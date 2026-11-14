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
    LaunchedEffect(target) { animateBodyTo(target, spec) }
    LaunchedEffect(targetX) { animateXTo(targetX, xSpec) }
}
