package com.droidbubble.app.island.icon

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.GraphicsLayerScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * The animated values of one icon. They are only read in [applyTo], from a `graphicsLayer`
 * block, so an effect redraws the layer without recomposing anything (Convention 2).
 */
@Stable
class IconEffectState {
    private val scale = Animatable(IconChannel.Scale.rest)
    private val offsetX = Animatable(IconChannel.OffsetX.rest)
    private val offsetY = Animatable(IconChannel.OffsetY.rest)
    private val rotation = Animatable(IconChannel.Rotation.rest)
    /** Reduced motion's dip: 0 → 1 over the dip, opacity lowest at 0.5. */
    private val dip = Animatable(0f)
    private var dipDepth = 0f

    /** The running loop (null: none) and where in its period it is, 0..1. Read in the layer only. */
    private var loop by mutableStateOf<ResolvedLoop?>(null)
    private var loopPhase by mutableFloatStateOf(0f)

    /** Starts holding [l] at its first frame, or stops any loop with null. */
    internal fun setLoop(l: ResolvedLoop?) {
        loop = l
        loopPhase = 0f
    }

    internal fun setLoopPhase(phase: Float) {
        loopPhase = phase
    }

    /** The loop's wave right now, 0..1 (0 when no loop runs). */
    val loopWave: Float get() {
        val l = loop ?: return 0f
        if (l.isStatic || l.turns) return 0f
        val t = loopPhase * 2f
        return if (t < 1f) l.easing.transform(t) else l.easing.transform(2f - t)
    }

    fun value(channel: IconChannel): Float = animatable(channel).value
    fun velocity(channel: IconChannel): Float = animatable(channel).velocity
    val alpha: Float get() {
        val dipped = 1f - dipDepth * (1f - abs(2f * dip.value - 1f))
        val l = loop ?: return dipped
        return dipped * (l.minAlpha + (1f - l.minAlpha) * loopWave)
    }

    /** Scale, including a breathing loop. */
    val totalScale: Float get() {
        val l = loop ?: return scale.value
        return scale.value * (l.scaleFrom + (l.scaleTo - l.scaleFrom) * loopWave)
    }

    /** Rotation in degrees, including a turning loop. */
    val totalRotation: Float get() {
        val l = loop
        val turn = if (l != null && l.turns && !l.isStatic) loopPhase * 360f else 0f
        return rotation.value + turn
    }
    val isRunning: Boolean get() = IconChannel.entries.any { animatable(it).isRunning } || dip.isRunning

    private fun animatable(channel: IconChannel) = when (channel) {
        IconChannel.Scale -> scale
        IconChannel.OffsetX -> offsetX
        IconChannel.OffsetY -> offsetY
        IconChannel.Rotation -> rotation
    }

    /**
     * Plays [effect] in [scope]. A kick on a channel that is still moving starts from its current
     * value with its current velocity plus the kick (capped), so a second event mid-effect adds to
     * the motion instead of restarting it.
     */
    fun fire(effect: ResolvedEffect, scope: CoroutineScope) {
        when (effect) {
            is ResolvedEffect.Kicks -> for (kick in effect.kicks) {
                val a = animatable(kick.channel)
                // Read now: the new animateTo cancels the running one, and with it its velocity
                val start = stackedVelocity(a.value - kick.channel.rest, a.velocity, kick.velocity, sqrt(effect.spec.stiffness))
                // Undispatched: the animation takes over (and its velocity becomes readable) right
                // now, so a kick that lands in the next frame sees it
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    a.animateTo(kick.channel.rest, effect.spec, initialVelocity = start)
                }
            }
            is ResolvedEffect.OpacityDip -> scope.launch(start = CoroutineStart.UNDISPATCHED) {
                dipDepth = effect.depth
                dip.snapTo(0f)
                dip.animateTo(1f, effect.spec)
                dip.snapTo(0f)
            }
        }
    }

    internal companion object {
        /**
         * The velocity after a kick lands on a spring at offset [x] moving at [v].
         * - Already moving or pushed the kick's way: nothing changes (the icon is already doing
         *   what the kick asks, so a burst of events doesn't drive it deeper).
         * - Otherwise kicks add up, but the spring's energy (½ω²x² + ½v²) is capped at that of one
         *   kick [IconEffectSpecs.MAX_STACKED_KICKS] times as strong.
         */
        fun stackedVelocity(x: Float, v: Float, kick: Float, omega: Float): Float {
            if (v != 0f && sign(v) == sign(kick) || x != 0f && sign(x) == sign(kick)) return v
            val next = v + kick
            val maxEnergy = (kick * IconEffectSpecs.MAX_STACKED_KICKS).let { it * it }
            val potential = (omega * x).let { it * it }
            if (potential + next * next <= maxEnergy) return next
            val room = (maxEnergy - potential).coerceAtLeast(0f)
            return sign(next) * sqrt(room)
        }
    }

    /** Applies the values to a layer. Call only from `graphicsLayer { }`. */
    fun applyTo(layer: GraphicsLayerScope) = with(layer) {
        val s = totalScale
        scaleX = s
        scaleY = s
        translationX = offsetX.value * size.width
        translationY = offsetY.value * size.height
        rotationZ = totalRotation
        alpha = this@IconEffectState.alpha
    }
}
