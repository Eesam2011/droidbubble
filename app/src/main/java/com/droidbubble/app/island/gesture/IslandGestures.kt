package com.droidbubble.app.island.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity

/**
 * One gesture detector for every island surface (M4.1). It waits for the touch slop, locks to the
 * axis the finger moved on, tracks velocity, and on release asks [resolveRelease] what to do.
 *
 * Children keep first claim: a child that consumes the movement (a slider, a seek bar) keeps the
 * drag, because only movement left unconsumed at the Main pass is taken. The down itself may be
 * consumed (a `combinedClickable` on the same node does that), so it is not required to be free.
 *
 * The callbacks are read through `rememberUpdatedState`, so they can capture values that change
 * between compositions (the current activity, the settings) without restarting the detector.
 *
 * @param isRtl the layout direction (`LocalLayoutDirection`), for drags that start at the centre
 * @param claims whether this surface takes a drag that locked to `axis` and started at `start`
 *   (local px); a drag it doesn't claim is left alone for an ancestor (M4.3: card 1 claims vertical
 *   drags only from the pill zone at its top)
 * @param onDrag the drag so far on the locked axis, in px, as the finger moved it. Callers apply
 *   their own rubber band ([rubberBand]) where the surface can't follow 1:1.
 * @param onThresholdCrossed fires each time letting go here (slowly) would start doing something
 *   else: `true` when it would now commit (dismiss, tuck, expand, open the shade, minimise,
 *   restore), `false` when it would now spring back. Used for the threshold haptic (05 §6).
 * @param onRelease what the gesture does; a gesture cut short (the pointer cancelled, the detector
 *   restarted) always reports [IslandGestureResult.Cancel], so the surface springs back.
 */
fun Modifier.islandGestures(
    surface: GestureSurface,
    thresholds: GestureThresholds = GestureThresholds(),
    canDismiss: () -> Boolean = { true },
    isRtl: Boolean = false,
    claims: (axis: Axis, start: Offset) -> Boolean = { _, _ -> true },
    onDrag: (axis: Axis, offset: Offset) -> Unit = { _, _ -> },
    onThresholdCrossed: (axis: Axis, crossed: Boolean) -> Unit = { _, _ -> },
    onRelease: (IslandGestureResult) -> Unit,
): Modifier = composed {
    val latestCanDismiss by rememberUpdatedState(canDismiss)
    val latestClaims by rememberUpdatedState(claims)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnThreshold by rememberUpdatedState(onThresholdCrossed)
    val latestOnRelease by rememberUpdatedState(onRelease)

    pointerInput(surface, thresholds, isRtl) {
        val t = thresholds.toPx(this)
        val slop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)

            // Touch slop, then axis lock. A consumed move means a child took the drag.
            var slopChange: PointerInputChange? = null
            while (slopChange == null) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                if ((change.position - down.position).getDistance() >= slop) slopChange = change
            }
            val afterSlop: PointerInputChange = slopChange ?: return@awaitEachGesture
            val axis = lockAxis(afterSlop.position - down.position)
            if (!latestClaims(axis, down.position)) return@awaitEachGesture
            afterSlop.consume()

            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            tracker.addPosition(afterSlop.uptimeMillis, afterSlop.position)
            var total = afterSlop.position - down.position
            var committing = false
            var lastPreview: IslandGestureResult? = null
            var released = false

            fun along(offset: Offset) = if (axis == Axis.HORIZONTAL) Offset(offset.x, 0f) else Offset(0f, offset.y)

            fun report() {
                latestOnDrag(axis, along(total))
                // What a slow release here would do; a change of outcome is a threshold crossing
                val preview = resolveRelease(
                    surface, axis, along(total), Velocity.Zero, down.position.x, size.width / 2f, t, isRtl, latestCanDismiss(),
                )
                val previewCommits = preview !is IslandGestureResult.Cancel
                val changed = lastPreview != null && preview::class != lastPreview!!::class
                if (previewCommits != committing || (changed && previewCommits)) {
                    committing = previewCommits
                    latestOnThreshold(axis, previewCommits)
                }
                lastPreview = preview
            }

            try {
                report()
                var lifted = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        tracker.addPosition(change.uptimeMillis, change.position)
                        lifted = true
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
                released = true
                latestOnRelease(
                    if (!lifted) IslandGestureResult.Cancel(velocity)
                    else resolveRelease(surface, axis, along(total), velocity, down.position.x, size.width / 2f, t, isRtl, latestCanDismiss())
                )
            } finally {
                // Cut short (the detector restarted mid-drag): never leave the surface displaced
                if (!released) latestOnRelease(IslandGestureResult.Cancel(Velocity.Zero))
            }
        }
    }
}
