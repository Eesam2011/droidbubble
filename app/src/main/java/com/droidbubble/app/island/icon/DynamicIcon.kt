package com.droidbubble.app.island.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.opticalCenter
import kotlinx.coroutines.delay
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.ui.platform.LocalContext
import com.droidbubble.app.island.motion.readAnimatorScale

/** The last trigger an icon has seen, so only a change fires (Convention 4). */
private class TriggerMemory(var last: Any?)

/**
 * Applies [effect] to any content: a glyph, an avatar, album art (phase 5, I1).
 *
 * - A discrete effect fires each time [trigger] changes. The value it starts with never fires:
 *   an icon that appears in a state doesn't animate into it.
 * - [IconEffect.Wiggle] with `repeatEveryMs` swings again at that interval while [active].
 * - An indefinite effect runs while [active] and drops back to rest when it turns false.
 * - Values are read only in the layer, so an effect recomposes nothing; only a changed
 *   [trigger] recomposes the caller, as any argument does.
 *
 * [state] is for tests and for hosts that drive several effects on one layer.
 */
fun Modifier.iconEffect(
    effect: IconEffect?,
    trigger: Any? = null,
    active: Boolean = true,
    pivot: TransformOrigin = TransformOrigin.Center,
    state: IconEffectState? = null,
): Modifier = if (effect == null) this else composed {
    val s = state ?: remember { IconEffectState() }
    val motion = rememberUpdatedState(LocalIslandMotion.current)
    // Effects run on this scope, not the LaunchedEffect's: a new trigger must not cancel the
    // running spring before the next kick has read its velocity
    val scope = rememberCoroutineScope()
    val memory = remember { TriggerMemory(trigger) }
    if (effect is IconEffect.Discrete) {
        LaunchedEffect(trigger) {
            if (memory.last != trigger) {
                memory.last = trigger
                s.fire(IconEffectSpecs.resolve(effect, motion.value), scope)
            }
        }
    }
    if (effect is IconEffect.Indefinite) {
        val removed = rememberAnimationsRemoved()
        val loop = IconEffectSpecs.resolveLoop(effect, motion.value, removed)
        LaunchedEffect(loop, active) {
            if (!active) {
                s.setLoop(null)
                return@LaunchedEffect
            }
            s.setLoop(loop)
            if (loop.isStatic) return@LaunchedEffect
            // Frames only while composed: this ends with the composition (Convention 6)
            val start = withInfiniteAnimationFrameMillis { it }
            while (true) {
                if (loop.frameIntervalMillis > 0) delay(loop.frameIntervalMillis.toLong())
                withInfiniteAnimationFrameMillis { now ->
                    s.setLoopPhase(((now - start) % loop.periodMillis) / loop.periodMillis.toFloat())
                }
            }
        }
    }
    val repeatMs = (effect as? IconEffect.Wiggle)?.repeatEveryMs
    if (repeatMs != null && active) {
        LaunchedEffect(effect) {
            while (true) {
                delay(repeatMs)
                s.fire(IconEffectSpecs.resolve(effect, motion.value), scope)
            }
        }
    }
    graphicsLayer {
        transformOrigin = pivot
        s.applyTo(this)
    }
}

/** True when the system animator scale is 0 ("Remove animations"). */
@Composable
private fun rememberAnimationsRemoved(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { readAnimatorScale(resolver) <= 0f }
}

/**
 * An island glyph with an optional effect. Same size, tint and optical centring as a plain
 * `Icon`, so wiring an effect never moves the glyph.
 */
@Composable
fun DynamicIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = IslandTokens.Glyph.Compact,
    effect: IconEffect? = null,
    trigger: Any? = null,
    active: Boolean = true,
    pivot: TransformOrigin = TransformOrigin.Center,
) {
    Icon(
        icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(size)
            .iconEffect(effect, trigger, active, pivot)
            .opticalCenter(icon),
    )
}
