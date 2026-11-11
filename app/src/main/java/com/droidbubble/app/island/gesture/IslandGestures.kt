package com.droidbubble.app.island.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

/**
 * One gesture detector for every island surface (M4.1). It waits for the touch slop, locks to the
 * axis the finger moved on, tracks velocity, and on release asks [resolveRelease] what to do.
 * Children keep first claim: only changes they left unconsumed are taken, at the Main pass, so a
 * slider or a button inside a card still gets its own drag.
 *
 * @param isRtl the layout direction (`LocalLayoutDirection`), for drags that start at the centre
 * @param onDrag the drag so far on the locked axis, already rubber-banded where it can't commit
 *   (write it into an Animatable with snapTo)
 * @param onThresholdCrossed fires when the drag crosses the commit distance either way (haptics)
 */
fun Modifier.islandGestures(
    surface: GestureSurface,
    thresholds: GestureThresholds = GestureThresholds(),
    canDismiss: () -> Boolean = { true },
    isRtl: Boolean = false,
    onDrag: (axis: Axis, offset: Offset) -> Unit = { _, _ -> },
    onThresholdCrossed: (axis: Axis, crossed: Boolean) -> Unit = { _, _ -> },
    onRelease: (IslandGestureResult) -> Unit,
): Modifier = pointerInput(surface, thresholds, isRtl) {
    val t = thresholds.toPx(this)
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Main)
        val afterSlop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            ?: return@awaitEachGesture
        val axis = lockAxis(afterSlop.position - down.position)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        tracker.addPosition(afterSlop.uptimeMillis, afterSlop.position)
        var total = afterSlop.position - down.position
        var crossed = false

        fun report() {
            val along = if (axis == Axis.HORIZONTAL) Offset(total.x, 0f) else Offset(0f, total.y)
            onDrag(axis, along)
            val distance = if (axis == Axis.HORIZONTAL) abs(total.x) else abs(total.y)
            val nowCrossed = distance >= t.commit
            if (nowCrossed != crossed) {
                crossed = nowCrossed
                onThresholdCrossed(axis, nowCrossed)
            }
        }
        report()

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                tracker.addPosition(change.uptimeMillis, change.position)
                break
            }
            if (change.isConsumed) continue
            total += change.positionChange()
            tracker.addPosition(change.uptimeMillis, change.position)
            change.consume()
            report()
        }

        val v = tracker.calculateVelocity()
        val velocity = if (axis == Axis.HORIZONTAL) Velocity(v.x, 0f) else Velocity(0f, v.y)
        val offset = if (axis == Axis.HORIZONTAL) Offset(total.x, 0f) else Offset(0f, total.y)
        onRelease(resolveRelease(surface, axis, offset, velocity, down.position.x, size.width / 2f, t, isRtl, canDismiss()))
    }
}
