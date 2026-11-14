package com.droidbubble.app.island.icon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.steppedBlurEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Which way a replaced glyph moves (I1.3). The new glyph always rises in from slightly below.
 * - [DownUp]: the old one sinks away. A reaction to input (a toggle, a mode change).
 * - [UpUp]: the old one rises away. Forward progression (play becoming pause).
 */
enum class ReplaceDirection { DownUp, UpUp }

/** How far a replaced glyph travels while it fades: 4 dp. */
private val REPLACE_TRAVEL = 4.dp
/** A replaced glyph shrinks to this as it leaves, and grows from it as it arrives. */
private const val REPLACE_MIN_SCALE = 0.6f
/** An appearing glyph grows from this. */
private const val APPEAR_MIN_SCALE = 0.5f
/** An appearing glyph starts this blurred (Full tier only). */
private val APPEAR_BLUR = 4.dp

/** One glyph in a [DynamicIconSwitch]: 0 = gone, 1 = fully in. */
@Stable
internal class SwitchSlot<T>(val value: T, start: Float) {
    val progress = Animatable(start)
    var leaving by mutableStateOf(false)
}

/**
 * Shows [content] for [target]; a new target replaces the old glyph instead of cutting to it
 * (I1.3). The old glyph shrinks to 60 %, fades and drifts 4 dp away on the quick effects spec;
 * the new one springs in from 60 % and 4 dp below on [IslandMotion.Symbol.replace]. A target that
 * changes again mid-way retargets from the current frame: a glyph that is still leaving comes
 * back from where it is. Reduced motion: a 150 ms crossfade, no size or offset change.
 *
 * Values are read in the layer, so it recomposes once per target change, never per frame. The
 * slot is as large as its largest child, so a replace never moves what is around it.
 */
@Composable
fun <T> DynamicIconSwitch(
    target: T,
    modifier: Modifier = Modifier,
    direction: ReplaceDirection = ReplaceDirection.DownUp,
    content: @Composable (T) -> Unit,
) {
    val slots = remember { mutableStateListOf(SwitchSlot(target, 1f)) }
    DynamicIconSwitch(target, slots, modifier, direction, content)
}

/** [slots] is the glyphs on screen, the newest last; tests pass their own to read it. */
@Composable
internal fun <T> DynamicIconSwitch(
    target: T,
    slots: MutableList<SwitchSlot<T>>,
    modifier: Modifier = Modifier,
    direction: ReplaceDirection = ReplaceDirection.DownUp,
    content: @Composable (T) -> Unit,
) {
    val motion = LocalIslandMotion.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(target) {
        val current = slots.lastOrNull { !it.leaving }
        if (current?.value == target) return@LaunchedEffect
        val reduced = motion.tier == MotionTier.REDUCED
        val outSpec: FiniteAnimationSpec<Float> = if (reduced) reducedFade() else motion.effects()
        val inSpec: FiniteAnimationSpec<Float> = if (reduced) reducedFade() else motion.spring(IslandMotion.Symbol.replace)
        for (slot in slots) if (!slot.leaving && slot.value != target) {
            slot.leaving = true
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                slot.progress.animateTo(0f, outSpec)
                slots.remove(slot)
            }
        }
        // A glyph that is still leaving comes back from where it is
        val back = slots.firstOrNull { it.value == target }
        val incoming = back ?: SwitchSlot(target, 0f).also { slots.add(it) }
        incoming.leaving = false
        scope.launch(start = CoroutineStart.UNDISPATCHED) { incoming.progress.animateTo(1f, inSpec) }
    }
    val reduced = motion.tier == MotionTier.REDUCED
    Box(modifier, contentAlignment = Alignment.Center) {
        for (slot in slots) key(slot) {
            Box(
                Modifier.graphicsLayer {
                    val p = slot.progress.value
                    alpha = p.coerceIn(0f, 1f)
                    if (!reduced) {
                        val s = REPLACE_MIN_SCALE + (1f - REPLACE_MIN_SCALE) * p
                        scaleX = s
                        scaleY = s
                        val travel = REPLACE_TRAVEL.toPx() * (1f - p)
                        // Arriving: from below. Leaving: down for DownUp, up for UpUp
                        translationY = if (!slot.leaving || direction == ReplaceDirection.DownUp) travel else -travel
                    }
                },
            ) { content(slot.value) }
        }
    }
}

/**
 * Shows [content] while [visible], appearing and disappearing instead of popping (I1.3): it
 * grows from 50 % with its opacity, sharpening from a 4 dp blur in the Full tier, on
 * [IslandMotion.Symbol.appear], and leaves on the quick effects spec. Reduced motion: a 150 ms
 * fade. Layout is kept while it shows; the island's shape owns any size change.
 */
@Composable
fun DynamicIconVisibility(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val motion = LocalIslandMotion.current
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    var present by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        val reduced = motion.tier == MotionTier.REDUCED
        if (visible) {
            present = true
            progress.animateTo(1f, if (reduced) reducedFade() else motion.spring(IslandMotion.Symbol.appear))
        } else {
            progress.animateTo(0f, if (reduced) reducedFade() else motion.effects())
            present = false
        }
    }
    if (!present) return
    val reduced = motion.tier == MotionTier.REDUCED
    val blur = motion.allowsBlur
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            if (!reduced) {
                val s = APPEAR_MIN_SCALE + (1f - APPEAR_MIN_SCALE) * p
                scaleX = s
                scaleY = s
                if (blur) renderEffect = steppedBlurEffect(1f - p.coerceIn(0f, 1f), APPEAR_BLUR.toPx())
            }
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun reducedFade(): TweenSpec<Float> = TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing)
