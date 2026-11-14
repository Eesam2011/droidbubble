package com.droidbubble.app.island.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import com.droidbubble.app.island.scene.ContentKey
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/*
 * The expanded cards' gestures (M4.2): a card follows the finger sideways and, on release, flies
 * off in the fling's direction, tucks back to the pill (a ringing call or alarm), or springs back
 * carrying the finger's velocity. The decision is the pure resolver's (M4.1); what each kind of
 * card does with it is [cardReleaseFor], also pure.
 */

/** What a sideways swipe can do to a card. */
internal enum class CardSwipeKind {
    /** Media, progress and ordinary notifications: a swipe dismisses the card. */
    DISMISS,
    /** A ringing call or alarm: a swipe tucks the island back to the pill and keeps the card (B6.2). */
    TUCK,
    /** Everything else (recording, flashlight, an ongoing call): the card moves and springs back. */
    INERT,
}

internal fun cardSwipeKind(priority: IslandPriority?, notification: NotificationInfo?): CardSwipeKind = when (priority) {
    IslandPriority.MEDIA, IslandPriority.PROGRESS -> CardSwipeKind.DISMISS
    IslandPriority.POP_NOTIFICATION ->
        if (notification?.isIncomingCall == true || notification?.isAlert == true) CardSwipeKind.TUCK else CardSwipeKind.DISMISS
    else -> CardSwipeKind.INERT
}

/** The screen-recording card barely moves under the finger: it can't be swiped away. */
internal const val RECORDING_DRAG_RESISTANCE = 0.18f

/**
 * Where the card is drawn for a finger [rawX] px from where it started: 1:1 for a card that can
 * be dismissed, rubber-banded for one that can't, and the recording card's fixed resistance.
 */
internal fun cardDragX(rawX: Float, kind: CardSwipeKind, priority: IslandPriority?, rubberBandLengthPx: Float): Float = when {
    priority == IslandPriority.SCREEN_RECORDING -> rawX * RECORDING_DRAG_RESISTANCE
    kind == CardSwipeKind.DISMISS -> rawX
    else -> rubberBand(rawX, rubberBandLengthPx)
}

/**
 * The finger's velocity [vx] as the card's own (the derivative of [cardDragX] at [rawX]), so a
 * banded card springs back at the speed it was actually moving, not the finger's.
 */
internal fun cardDragVelocity(rawX: Float, vx: Float, kind: CardSwipeKind, priority: IslandPriority?, rubberBandLengthPx: Float): Float = when {
    priority == IslandPriority.SCREEN_RECORDING -> vx * RECORDING_DRAG_RESISTANCE
    kind == CardSwipeKind.DISMISS -> vx
    else -> {
        val d = 1f + abs(rawX) / rubberBandLengthPx
        vx / (d * d)
    }
}

/** What a released card does. */
internal sealed interface CardRelease {
    /** Leave the screen toward [direction] (−1 left, +1 right), then the store dismisses it. */
    data class FlingOut(val direction: Int, val velocityX: Float) : CardRelease
    /** Collapse the island back to the pill; the card itself springs back to the centre as it closes. */
    data class TuckBack(val velocityX: Float) : CardRelease
    /** Spring back to rest with the finger's velocity. */
    data class SpringBack(val velocityX: Float) : CardRelease
}

/** Pure: the resolver's sideways verdict, for a card of [kind]. */
internal fun cardReleaseFor(result: IslandGestureResult, kind: CardSwipeKind): CardRelease = when (result) {
    is IslandGestureResult.Dismiss -> when (kind) {
        CardSwipeKind.DISMISS -> CardRelease.FlingOut(result.direction, result.velocityX)
        CardSwipeKind.TUCK -> CardRelease.TuckBack(result.velocityX)
        CardSwipeKind.INERT -> CardRelease.SpringBack(result.velocityX)
    }
    is IslandGestureResult.TuckInstead ->
        if (kind == CardSwipeKind.INERT) CardRelease.SpringBack(result.velocityX) else CardRelease.TuckBack(result.velocityX)
    is IslandGestureResult.Cancel -> CardRelease.SpringBack(result.velocity.x)
    else -> CardRelease.SpringBack(0f)
}

/**
 * Card [offsetX] (px) follows sideways drags on this card (M4.2).
 *
 * @param offscreenPx how far the card must travel to be off screen
 * @param onDismiss removes the card from the store once it has left; false if nothing was
 *   removed, and the card springs back instead
 * @param onTuck collapses the island (calls and alarms)
 */
internal fun Modifier.cardSwipe(
    enabled: Boolean,
    offsetX: Animatable<Float, AnimationVector1D>,
    kind: CardSwipeKind,
    priority: IslandPriority?,
    offscreenPx: () -> Float,
    onDismiss: () -> Boolean,
    onTuck: () -> Unit,
): Modifier = if (!enabled) this else this.composed {
    val motion = LocalIslandMotion.current
    val haptics = LocalIslandHaptics.current
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thresholds = remember { GestureThresholds() }
    val bandPx = with(LocalDensity.current) { thresholds.rubberBandLength.toPx() }
    val latestKind by rememberUpdatedState(kind)
    val latestPriority by rememberUpdatedState(priority)
    val latestOffscreen by rememberUpdatedState(offscreenPx)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnTuck by rememberUpdatedState(onTuck)
    val lastRawX = remember { floatArrayOf(0f) }

    islandGestures(
        surface = GestureSurface.EXPANDED_CARD,
        thresholds = thresholds,
        canDismiss = { latestKind != CardSwipeKind.TUCK },
        isRtl = isRtl,
        claims = { axis, _ -> axis == Axis.HORIZONTAL },
        onDrag = { _, offset ->
            lastRawX[0] = offset.x
            val x = cardDragX(offset.x, latestKind, latestPriority, bandPx)
            scope.launch { offsetX.snapTo(x) }
        },
        // Threshold haptic while dragging (05 §6), only where letting go would do something
        onThresholdCrossed = { _, crossed ->
            if (crossed && latestKind != CardSwipeKind.INERT) haptics.perform(IslandHapticEvent.THRESHOLD_CROSSED)
        },
        onRelease = { result ->
            val springBack = motion.spring<Float>(motion.interactive)
            when (val release = cardReleaseFor(result, latestKind)) {
                is CardRelease.FlingOut -> {
                    haptics.perform(IslandHapticEvent.DISMISS)
                    scope.launch {
                        // Straight off the screen with the fling's speed: no spring back first
                        offsetX.animateTo(
                            targetValue = release.direction * latestOffscreen(),
                            animationSpec = motion.spring(motion.dismiss, visibilityThreshold = 1f),
                            initialVelocity = release.velocityX,
                        )
                        if (!latestOnDismiss()) offsetX.animateTo(0f, springBack)
                    }
                }
                is CardRelease.TuckBack -> {
                    haptics.perform(IslandHapticEvent.COLLAPSE)
                    latestOnTuck()
                    val v = cardDragVelocity(lastRawX[0], release.velocityX, latestKind, latestPriority, bandPx)
                    scope.launch { offsetX.animateTo(0f, springBack, v) }
                }
                is CardRelease.SpringBack -> {
                    val v = cardDragVelocity(lastRawX[0], release.velocityX, latestKind, latestPriority, bandPx)
                    scope.launch { offsetX.animateTo(0f, springBack, v) }
                }
            }
            lastRawX[0] = 0f
        },
    )
}

/**
 * Brings dismissed cards back to the centre, but only once nobody can see the jump (M4.2): after
 * the island has fully closed, when the card starts showing another activity, or when the island
 * expands again with a card still parked off screen.
 */
@Composable
internal fun ResetCardOffsets(
    isExpanded: Boolean,
    primaryKey: ContentKey,
    secondaryKey: ContentKey,
    card1: Animatable<Float, AnimationVector1D>,
    card2: Animatable<Float, AnimationVector1D>,
    cardClosed: () -> Boolean,
) {
    val latestClosed by rememberUpdatedState(cardClosed)
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            if (!card1.isRunning) card1.snapTo(0f)
            if (!card2.isRunning) card2.snapTo(0f)
        } else {
            snapshotFlow { latestClosed() }.first { it }
            card1.snapTo(0f)
            card2.snapTo(0f)
        }
    }
    LaunchedEffect(primaryKey) { if (!card1.isRunning) card1.snapTo(0f) }
    LaunchedEffect(secondaryKey) { if (!card2.isRunning) card2.snapTo(0f) }
}
