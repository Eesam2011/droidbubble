package com.droidbubble.app.island.shell

import android.accessibilityservice.AccessibilityService
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.droidbubble.app.island.IslandHapticEvent
import com.droidbubble.app.island.LocalIslandHaptics
import com.droidbubble.app.island.gesture.Axis
import com.droidbubble.app.island.gesture.GestureSurface
import com.droidbubble.app.island.gesture.GestureThresholds
import com.droidbubble.app.island.gesture.IslandGestureResult
import com.droidbubble.app.island.gesture.islandGestures
import com.droidbubble.app.island.gesture.rubberBand
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.service.IslandAccessibilityService
import kotlinx.coroutines.launch

/*
 * The compact pill's gestures (M4.4, 05 §5 and §6): pull down to expand, pull further to open the
 * system shade, swipe inward to minimise into the ring, swipe the ring outward to restore. Tap and
 * long-press stay on the pill's combinedClickable.
 */

/** Opens the notification shade through the accessibility service. Tests replace [open]. */
internal object ShadeOpener {
    private val system: () -> Boolean = {
        IslandAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
    }

    /** Opens the shade; false when it couldn't (no accessibility service), and the pull expands instead. */
    @VisibleForTesting
    var open: () -> Boolean = system

    @VisibleForTesting
    fun reset() {
        open = system
    }
}

/** What a released pill drag does. */
internal sealed interface PillRelease {
    /** Open the card, starting with the finger's downward speed (px/s). */
    data class Expand(val velocityY: Float) : PillRelease
    data object OpenShade : PillRelease
    data object Minimise : PillRelease
    data object Restore : PillRelease
    /** Nothing to do: the pill springs back. */
    data object SpringBack : PillRelease
}

/**
 * Pure: the resolver's verdict for the pill, given what the pill can do right now. A pull past the
 * shade threshold is [PillRelease.OpenShade] here; when the shade can't open, [shadeFallback].
 */
internal fun pillReleaseFor(result: IslandGestureResult, canExpand: Boolean, canMinimise: Boolean, canRestore: Boolean): PillRelease =
    when (result) {
        is IslandGestureResult.Expand -> if (canExpand) PillRelease.Expand(result.velocityY) else PillRelease.SpringBack
        IslandGestureResult.OpenShade -> PillRelease.OpenShade
        IslandGestureResult.Minimise -> if (canMinimise) PillRelease.Minimise else PillRelease.SpringBack
        IslandGestureResult.Restore -> if (canRestore) PillRelease.Restore else PillRelease.SpringBack
        else -> PillRelease.SpringBack
    }

/** Without the accessibility service a long pull expands, as a shorter one would. */
internal fun shadeFallback(canExpand: Boolean): PillRelease =
    if (canExpand) PillRelease.Expand(0f) else PillRelease.SpringBack

/**
 * Where the pill is drawn for a drag of [raw] px: rubber-banded both ways. Pulling up, toward the
 * screen edge, gives less ([upFraction] of the band).
 */
internal fun pillDragOffset(raw: Float, axis: Axis, bandPx: Float, upFraction: Float = DOWN_DRAG_BAND_FRACTION): Float =
    if (axis == Axis.VERTICAL && raw < 0f) rubberBand(raw, bandPx * upFraction) else rubberBand(raw, bandPx)

/**
 * The pill's drag gestures (M4.4). [dragX] and [dragY] (px) move the pill body under the finger,
 * rubber-banded, and spring back on release unless [follows] is off (the notch style stays fused
 * to the top edge).
 *
 * @param minimised the pill is the ring around the camera (the MINIMAL surface)
 * @param onExpand opens the card with the finger's downward velocity (px/s) from the pill's
 *   dragged-down offset (px)
 */
internal fun Modifier.pillSwipe(
    enabled: Boolean,
    minimised: Boolean,
    follows: Boolean,
    dragX: Animatable<Float, AnimationVector1D>,
    dragY: Animatable<Float, AnimationVector1D>,
    canExpand: Boolean,
    canMinimise: Boolean,
    onExpand: (velocityY: Float, draggedY: Float) -> Unit,
    onMinimise: () -> Unit,
    onRestore: () -> Unit,
): Modifier = if (!enabled) this else this.composed {
    val motion = LocalIslandMotion.current
    val haptics = LocalIslandHaptics.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thresholds = remember { GestureThresholds() }
    val bandPx = with(LocalDensity.current) { thresholds.rubberBandLength.toPx() }
    val latestFollows by rememberUpdatedState(follows)
    val latestCanExpand by rememberUpdatedState(canExpand)
    val latestCanMinimise by rememberUpdatedState(canMinimise)
    val latestOnExpand by rememberUpdatedState(onExpand)
    val latestOnMinimise by rememberUpdatedState(onMinimise)
    val latestOnRestore by rememberUpdatedState(onRestore)

    islandGestures(
        surface = if (minimised) GestureSurface.MINIMAL else GestureSurface.COMPACT,
        thresholds = thresholds,
        isRtl = isRtl,
        onDrag = { axis, offset ->
            if (latestFollows) {
                if (axis == Axis.HORIZONTAL) {
                    val x = pillDragOffset(offset.x, axis, bandPx)
                    scope.launch { dragX.snapTo(x) }
                } else {
                    val y = pillDragOffset(offset.y, axis, bandPx)
                    scope.launch { dragY.snapTo(y) }
                }
            }
        },
        // Threshold haptic while dragging (05 §6): on each boundary where letting go would act
        onThresholdCrossed = { axis, crossed ->
            val acts = if (axis == Axis.VERTICAL) latestCanExpand else (minimised || latestCanMinimise)
            if (crossed && acts) haptics.perform(IslandHapticEvent.THRESHOLD_CROSSED)
        },
        onRelease = { result ->
            val draggedY = dragY.value
            var release = pillReleaseFor(result, latestCanExpand, latestCanMinimise, canRestore = minimised)
            if (release == PillRelease.OpenShade) {
                if (ShadeOpener.open()) haptics.perform(IslandHapticEvent.GESTURE_COMMIT)
                else release = shadeFallback(latestCanExpand)
            }
            when (val r = release) {
                is PillRelease.Expand -> latestOnExpand(r.velocityY, draggedY)
                PillRelease.Minimise -> {
                    haptics.perform(IslandHapticEvent.MINIMISE)
                    latestOnMinimise()
                }
                PillRelease.Restore -> {
                    haptics.perform(IslandHapticEvent.RESTORE)
                    latestOnRestore()
                }
                PillRelease.OpenShade, PillRelease.SpringBack -> Unit
            }
            // The body always comes home; a cancelled drag carries the finger's velocity back
            if (latestFollows) {
                val carry = (result as? IslandGestureResult.Cancel)?.velocity
                val springBack = motion.spring<Float>(motion.interactive)
                scope.launch { dragX.animateTo(0f, springBack, carry?.x ?: 0f) }
                scope.launch { dragY.animateTo(0f, springBack, carry?.y ?: 0f) }
            }
        },
    )
}
