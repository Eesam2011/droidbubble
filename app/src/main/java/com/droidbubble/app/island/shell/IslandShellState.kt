package com.droidbubble.app.island.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import com.droidbubble.app.island.gesture.rubberBand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The island's body as animation state (M1.1): its shape ([body]: width, height, corner, top) as
 * one 4-D spring, and its horizontal position ([x], dp) beside it. Compose's animation vectors
 * stop at four dimensions, so x is a second `Animatable`; [animateTo] moves both together.
 *
 * Unlike `animate*AsState`, this exposes what gestures and the one-body morph need: retarget with
 * a given velocity, snap, the current velocity and whether it is moving.
 */
@Stable
class IslandShellState(initial: IslandGeometry, initialX: Dp) {
    // No visibility threshold here: the spring specs carry their own (IslandGeometry.VisibilityThreshold)
    val body = Animatable(initial, IslandGeometry.VectorConverter)
    val x = Animatable(initialX.value)

    /** Read these inside layout or draw lambdas where possible (M2): reading them in composition recomposes every frame. */
    val geometry: IslandGeometry get() = body.value
    val velocity: IslandGeometry get() = body.velocity
    val xDp: Dp get() = x.value.dp
    val isSettled: Boolean get() = !body.isRunning && !x.isRunning

    /**
     * How fast the shape is changing size, 0 to 1 (M2.4): the spring's own velocity, so stretch and
     * blur peak mid-morph and are gone the moment it settles. Read it in draw-phase lambdas.
     */
    fun morphSpeed(fullSpeedDpPerSec: Float = FULL_SPEED_DP_PER_SEC): Float =
        ((abs(velocity.width.value) + abs(velocity.height.value)) / fullSpeedDpPerSec).coerceIn(0f, 1f)

    /** True while the shape is getting bigger toward [target]. */
    fun isGrowing(target: IslandGeometry): Boolean =
        (target.width + target.height) > (geometry.width + geometry.height)

    /** Moves the shape to [target]; velocity carries over unless [initialVelocity] replaces it (a gesture hand-off, M4). */
    suspend fun animateBodyTo(
        target: IslandGeometry,
        spec: AnimationSpec<IslandGeometry>,
        initialVelocity: IslandGeometry = body.velocity,
    ) {
        body.animateTo(target, spec, initialVelocity)
    }

    suspend fun animateXTo(targetX: Dp, spec: AnimationSpec<Float>) {
        x.animateTo(targetX.value, spec)
    }

    /** Moves the shape and the position together; both keep their velocity. */
    suspend fun animateTo(
        target: IslandGeometry,
        targetX: Dp,
        spec: AnimationSpec<IslandGeometry>,
        xSpec: AnimationSpec<Float>,
        initialVelocity: IslandGeometry = body.velocity,
    ) = coroutineScope {
        launch { animateXTo(targetX, xSpec) }
        animateBodyTo(target, spec, initialVelocity)
    }

    suspend fun snapTo(target: IslandGeometry, targetX: Dp = xDp) {
        body.snapTo(target)
        x.snapTo(targetX.value)
    }

    // M4.3: a finger's velocity waiting for the next retarget (a tuck or an expand by gesture).
    // Not snapshot state: only read by Follow's effects, never in composition.
    private var handOffVelocity: IslandGeometry? = null
    private var handOffAtNanos = 0L

    /**
     * The next retarget (see [Follow]) starts with [velocity] instead of the body's own: a gesture
     * hands its speed to the spring it triggers. Forgotten if no retarget comes within
     * [HAND_OFF_TTL_NANOS], so a refused gesture never pushes a later, unrelated morph.
     */
    fun handOff(velocity: IslandGeometry, nowNanos: Long = System.nanoTime()) {
        handOffVelocity = velocity
        handOffAtNanos = nowNanos
    }

    /** The waiting hand-off velocity, if still fresh; taking it clears it. */
    fun takeHandOff(nowNanos: Long = System.nanoTime()): IslandGeometry? {
        val v = handOffVelocity ?: return null
        handOffVelocity = null
        return if (nowNanos - handOffAtNanos <= HAND_OFF_TTL_NANOS) v else null
    }

    /**
     * M4.3: a drag displaces the body from [base] (the shape when the drag began) by [dy]; see
     * [dragGeometry]. Snaps, so the shape is exactly under the finger; the target is untouched and
     * a release springs back to it or on to a new one.
     */
    suspend fun dragBy(base: IslandGeometry, dy: Dp, minHeight: Dp, band: Dp) {
        body.snapTo(dragGeometry(base, dy, minHeight, band))
    }

    /** M4.3: lets go of a drag: springs to [target] starting with [velocity] (from [gestureVelocity] or a single axis). */
    suspend fun release(target: IslandGeometry, spec: AnimationSpec<IslandGeometry>, velocity: IslandGeometry) {
        handOffVelocity = null
        body.animateTo(target, spec, velocity)
    }
}

/** A hand-off older than this is dropped (the gesture's retarget never came). */
const val HAND_OFF_TTL_NANOS = 500_000_000L

/** A finger faster than this (dp per second) is handed to the spring at this speed. */
const val MAX_HAND_OFF_DP_PER_SEC = 6000f

/** Pulling the card down only gives a little: its band is this fraction of the band upward. */
const val DOWN_DRAG_BAND_FRACTION = 0.33f

private val ZeroGeometry = IslandGeometry(0.dp, 0.dp, 0.dp, 0.dp)

/**
 * M4.3, pure: the body under a vertical drag of [dy] from [base], rubber-banded over [band]. Up
 * (negative) shortens it from the bottom, never below [minHeight], as if tucking back into the
 * pill; down lowers its top a little (a shorter band), and is only a give: it never commits.
 */
fun dragGeometry(base: IslandGeometry, dy: Dp, minHeight: Dp, band: Dp): IslandGeometry =
    if (dy.value < 0f) {
        val banded = rubberBand(dy.value, band.value)
        base.copy(height = (base.height + banded.dp).coerceAtLeast(minOf(minHeight, base.height)))
    } else {
        base.copy(top = base.top + rubberBand(dy.value, band.value * DOWN_DRAG_BAND_FRACTION).dp)
    }

/**
 * M4.3, pure: a finger's vertical speed [velocityY] (dp per second) as the body's velocity along its
 * morph from [from] to [to]: every value moves in proportion, scaled so the height changes at the
 * finger's speed. Only speed in the direction of the morph carries over; against it, nothing.
 */
fun gestureVelocity(velocityY: Float, from: IslandGeometry, to: IslandGeometry): IslandGeometry {
    val dh = (to.height - from.height).value
    if (abs(dh) < 0.5f) return ZeroGeometry
    val v = velocityY.coerceIn(-MAX_HAND_OFF_DP_PER_SEC, MAX_HAND_OFF_DP_PER_SEC)
    val f = (v / dh).coerceAtLeast(0f)
    return IslandGeometry(
        width = (to.width - from.width) * f,
        height = (to.height - from.height) * f,
        corner = (to.corner - from.corner) * f,
        top = (to.top - from.top) * f,
    )
}

/** A morph this fast (width + height, dp per second) counts as full speed for stretch and blur. */
const val FULL_SPEED_DP_PER_SEC = 1800f

/** [IslandShellState.morphSpeed] for a one-dimensional size, such as the pill's width. */
fun morphSpeedOf(velocityDpPerSec: Float, fullSpeedDpPerSec: Float = FULL_SPEED_DP_PER_SEC): Float =
    (abs(velocityDpPerSec) / fullSpeedDpPerSec).coerceIn(0f, 1f)

@Composable
fun rememberIslandShellState(initial: IslandGeometry, initialX: Dp): IslandShellState =
    remember { IslandShellState(initial, initialX) }

/**
 * Drives [shell] toward its targets. The shape and the position each retarget only when their own
 * target changes, with the spec in force at that moment, as `animate*AsState` did, so velocity
 * carries over an interruption.
 */
@Composable
fun IslandShellState.Follow(
    target: IslandGeometry,
    targetX: Dp,
    spec: AnimationSpec<IslandGeometry>,
    xSpec: AnimationSpec<Float>,
) {
    // A gesture's hand-off (M4.3, M4.4) replaces the body's own velocity on this retarget
    LaunchedEffect(target) { animateBodyTo(target, spec, takeHandOff() ?: body.velocity) }
    LaunchedEffect(targetX) { animateXTo(targetX, xSpec) }
}
