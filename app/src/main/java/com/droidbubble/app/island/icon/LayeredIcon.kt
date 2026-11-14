package com.droidbubble.app.island.icon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.opticalCenter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** How a [LayeredIcon]'s layers light up (I1.4). */
enum class VariableMode {
    /** Layers lit follow a level, 0..1: a download, a battery, a torch's brightness. */
    Level,
    /** Layers light one by one and stay lit, then start over: connecting, building up. */
    Cumulative,
    /** One layer lit at a time, moving along: searching, an unknown amount of work. */
    Iterative,
}

/** Which way the layers are counted: the first layer lit is the bottom one, or the top one. */
enum class LayerOrder { BottomUp, TopDown }

/** How many layers are lit at [level] 0..1 out of [layers]: any level above 0 lights at least one. */
fun litLayers(level: Float, layers: Int): Int = ceil(level.coerceIn(0f, 1f) * layers).toInt().coerceIn(0, layers)

/**
 * An island glyph drawn as [layers] horizontal bands whose emphasis follows a level or a loop
 * (I1.4, "variable color"). The bands cut the real glyph, so its geometry is exactly the V7
 * Material Symbols Rounded shape; a lit band draws at full [tint], an unlit one at
 * [IslandTokens.LayerInactiveAlpha].
 *
 * - [VariableMode.Level]: [level] is a lambda read in the draw phase and in a snapshot flow, so a
 *   level that ticks ten times a second recomposes nothing. Each band eases to its new emphasis on
 *   the quick effects spec, the next band [IslandMotion.Symbol.LAYER_STAGGER_MS] later (by layer).
 * - [VariableMode.Cumulative] / [VariableMode.Iterative]: a loop of
 *   [IslandMotion.Symbol.VARIABLE_STEP_MS] a step while [active]; [reversing] runs back down
 *   instead of starting over.
 * - Reduced motion: a level change is a 150 ms fade with no stagger, and loops hold every band at
 *   [IslandTokens.LayerLoopStillAlpha]. Battery Saver: loops step half as often.
 */
@Composable
fun LayeredIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = IslandTokens.Glyph.Compact,
    layers: Int = 3,
    order: LayerOrder = LayerOrder.BottomUp,
    level: () -> Float = { 1f },
    mode: VariableMode = VariableMode.Level,
    active: Boolean = true,
    reversing: Boolean = false,
    inactiveAlpha: Float = IslandTokens.LayerInactiveAlpha,
) {
    val motion = LocalIslandMotion.current
    val painter = rememberVectorPainter(icon)
    val levelState = rememberUpdatedState(level)
    val alphas = remember(layers) {
        val lit = litLayers(level(), layers)
        List(layers) { Animatable(if (mode != VariableMode.Level || it < lit) 1f else inactiveAlpha) }
    }
    val reduced = motion.tier == MotionTier.REDUCED
    if (mode == VariableMode.Level) {
        LaunchedEffect(alphas, reduced) {
            snapshotFlow { litLayers(levelState.value(), layers) }.distinctUntilChanged().collect { lit ->
                coroutineScope {
                    val spec: FiniteAnimationSpec<Float> = if (reduced) reducedFade() else motion.effects()
                    // Bands that light go first-to-last, bands that dim last-to-first
                    alphas.forEachIndexed { i, a ->
                        val target = if (i < lit) 1f else inactiveAlpha
                        if (a.targetValue == target) return@forEachIndexed
                        val rank = if (target == 1f) i else layers - 1 - i
                        launch {
                            if (!reduced) delay(rank.toLong() * IslandMotion.Symbol.LAYER_STAGGER_MS)
                            a.animateTo(target, spec)
                        }
                    }
                }
            }
        }
    } else {
        LaunchedEffect(alphas, mode, active, reduced, reversing) {
            if (!active) {
                alphas.forEach { it.snapTo(1f) }
                return@LaunchedEffect
            }
            if (reduced) {
                alphas.forEach { it.snapTo(IslandTokens.LayerLoopStillAlpha) }
                return@LaunchedEffect
            }
            val step = IslandMotion.Symbol.VARIABLE_STEP_MS.toLong() * if (motion.tier == MotionTier.EFFICIENT) 2 else 1
            // Cumulative: 0..N lit, Iterative: band 0..N-1 lit; reversing walks back instead of restarting
            val count = if (mode == VariableMode.Cumulative) layers + 1 else layers
            val sequence = if (reversing) (0 until count) + (count - 2 downTo 1) else (0 until count).toList()
            while (true) {
                for (k in sequence) {
                    alphas.forEachIndexed { i, a ->
                        val lit = if (mode == VariableMode.Cumulative) i < k else i == k
                        a.snapTo(if (lit) 1f else inactiveAlpha)
                    }
                    delay(step)
                }
            }
        }
    }
    val described = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Canvas(modifier.then(described).size(size).opticalCenter(icon)) {
        val band = this.size.height / layers
        val filter = ColorFilter.tint(tint)
        for (i in 0 until layers) {
            // Band i counted from the bottom (BottomUp) or the top (TopDown)
            val top = if (order == LayerOrder.BottomUp) this.size.height - band * (i + 1) else band * i
            clipRect(top = top, bottom = top + band) {
                with(painter) { draw(this@Canvas.size, alpha = alphas[i].value, colorFilter = filter) }
            }
        }
    }
}

private fun reducedFade(): TweenSpec<Float> = TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing)
