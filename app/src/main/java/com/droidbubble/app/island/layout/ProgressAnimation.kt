package com.droidbubble.app.island.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.droidbubble.app.island.motion.LocalIslandMotion

/** D3.3: how a progress ring or bar moves to a new value. */
object ProgressAnimationPolicy {
    /** A drop of more than this is a new download (or a restart), not progress: it snaps. */
    const val BACKWARD_SNAP = 0.2f

    /** True when [to] should be shown at once instead of springing from [from]. */
    fun snaps(from: Float, to: Float): Boolean = from - to > BACKWARD_SNAP
}

/**
 * D3.3: [target] (0–1) on the progress spring; a new download snaps back instead of draining.
 * Read `.value` in the draw phase (an indicator's `progress = { … }` lambda) so a moving ring
 * doesn't recompose.
 */
@Composable
fun rememberProgressAnimatable(target: Float): Animatable<Float, *> {
    val motion = LocalIslandMotion.current
    val value = remember { Animatable(target.coerceIn(0f, 1f)) }
    LaunchedEffect(target) {
        val to = target.coerceIn(0f, 1f)
        if (ProgressAnimationPolicy.snaps(value.value, to)) value.snapTo(to)
        else value.animateTo(to, motion.spring(motion.progress, geometry = false))
    }
    return value
}
