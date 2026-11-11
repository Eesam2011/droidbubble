package com.droidbubble.app.island.gesture

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

/** Which part of the island the finger is on. */
enum class GestureSurface { COMPACT, MINIMAL, EXPANDED_CARD }

enum class Axis { HORIZONTAL, VERTICAL }

/** Gesture distances in dp (05 R2, 05 §6). */
@Immutable
data class GestureThresholds(
    /** A drag this far commits (dismiss, tuck, minimise, restore). */
    val commit: Dp = 48.dp,
    /** A pull down on the pill between this and [shadeAt] expands it. */
    val expandMin: Dp = 24.dp,
    /** Past this, the pull opens the notification shade instead. */
    val shadeAt: Dp = 64.dp,
    /** A release at least this fast (dp per second) commits however short the drag. */
    val flingVelocity: Dp = 1000.dp,
    /** Past a limit the shape follows the finger less and less: x' = x / (1 + |x| / L). */
    val rubberBandLength: Dp = 120.dp,
    /** A drag starting this close to the pill's centre has no "inward" side of its own. */
    val centreZone: Dp = 8.dp,
) {
    fun toPx(density: Density): GestureThresholdsPx = with(density) {
        GestureThresholdsPx(
            commit = commit.toPx(),
            expandMin = expandMin.toPx(),
            shadeAt = shadeAt.toPx(),
            flingVelocity = flingVelocity.toPx(),
            rubberBandLength = rubberBandLength.toPx(),
            centreZone = centreZone.toPx(),
        )
    }
}

@Immutable
data class GestureThresholdsPx(
    val commit: Float,
    val expandMin: Float,
    val shadeAt: Float,
    val flingVelocity: Float,
    val rubberBandLength: Float,
    val centreZone: Float,
)

/** What a released gesture does. */
sealed interface IslandGestureResult {
    /** Pill pulled down: open the card, starting with the finger's speed. */
    data class Expand(val velocityY: Float) : IslandGestureResult
    /** Pill pulled down a long way: open the system shade. */
    data object OpenShade : IslandGestureResult
    /** Card swiped up: back to the pill, carrying the swipe's speed. */
    data class Tuck(val velocityY: Float) : IslandGestureResult
    /** Card flung sideways: dismiss it, leaving in [direction] (−1 left, +1 right). */
    data class Dismiss(val direction: Int, val velocityX: Float) : IslandGestureResult
    /** A ringing call or alarm flung sideways: back to the pill instead (B6.2). */
    data class TuckInstead(val velocityX: Float) : IslandGestureResult
    /** Pill swiped inward: shrink into the ring around the camera. */
    data object Minimise : IslandGestureResult
    /** Ring swiped outward: back to the pill. */
    data object Restore : IslandGestureResult
    /** Not far or fast enough: spring back with this velocity. */
    data class Cancel(val velocity: Velocity) : IslandGestureResult
}

/** The axis a drag locks to once it passes the touch slop. */
fun lockAxis(delta: Offset): Axis = if (abs(delta.x) > abs(delta.y)) Axis.HORIZONTAL else Axis.VERTICAL

/** Rubber band: follows the finger 1:1 at first, then less and less, never past [lengthPx]. */
fun rubberBand(x: Float, lengthPx: Float): Float = x / (1f + abs(x) / lengthPx)

/**
 * The sign of "inward" on the x axis: toward the pill's centre from where the drag started. A drag
 * starting within [GestureThresholdsPx.centreZone] of the centre uses the layout direction's
 * end → start instead (left in LTR, right in RTL), which keeps today's swipe-left-minimises.
 */
fun inwardSign(startX: Float, centreX: Float, thresholds: GestureThresholdsPx, isRtl: Boolean): Float = when {
    abs(startX - centreX) <= thresholds.centreZone -> if (isRtl) 1f else -1f
    startX > centreX -> -1f
    else -> 1f
}

/**
 * Pure: what a released drag does (M4.1). [offset] is the drag's total movement, [velocity] the
 * release velocity (px and px/s). Nothing here touches Android or Compose state, so every row of
 * 05 §6 is a table test.
 */
fun resolveRelease(
    surface: GestureSurface,
    axis: Axis,
    offset: Offset,
    velocity: Velocity,
    startX: Float,
    centreX: Float,
    thresholds: GestureThresholdsPx,
    isRtl: Boolean,
    canDismiss: Boolean,
): IslandGestureResult {
    val t = thresholds
    val cancel = IslandGestureResult.Cancel(velocity)

    /** Far enough, or flung fast enough in the same direction as [direction]. */
    fun commits(distance: Float, speed: Float, direction: Float): Boolean =
        distance * direction >= t.commit || speed * direction >= t.flingVelocity

    return when (surface) {
        GestureSurface.EXPANDED_CARD -> when (axis) {
            Axis.HORIZONTAL -> {
                val direction = if (offset.x != 0f) sign(offset.x) else sign(velocity.x)
                if (direction == 0f || !commits(offset.x, velocity.x, direction)) cancel
                else if (canDismiss) IslandGestureResult.Dismiss(direction.toInt(), velocity.x)
                else IslandGestureResult.TuckInstead(velocity.x)
            }
            // Up tucks; down only rubber-bands and springs back
            Axis.VERTICAL -> if (commits(offset.y, velocity.y, -1f)) IslandGestureResult.Tuck(velocity.y) else cancel
        }

        GestureSurface.COMPACT -> when (axis) {
            Axis.VERTICAL -> when {
                offset.y >= t.shadeAt -> IslandGestureResult.OpenShade
                offset.y >= t.expandMin || velocity.y >= t.flingVelocity -> IslandGestureResult.Expand(velocity.y)
                else -> cancel
            }
            Axis.HORIZONTAL -> {
                val inward = inwardSign(startX, centreX, t, isRtl)
                // Inward minimises; outward does nothing on the pill (D6: no swipe-to-hide)
                if (commits(offset.x, velocity.x, inward)) IslandGestureResult.Minimise else cancel
            }
        }

        GestureSurface.MINIMAL -> when (axis) {
            Axis.HORIZONTAL -> {
                val outward = -inwardSign(startX, centreX, t, isRtl)
                if (commits(offset.x, velocity.x, outward)) IslandGestureResult.Restore else cancel
            }
            Axis.VERTICAL -> cancel
        }
    }
}
