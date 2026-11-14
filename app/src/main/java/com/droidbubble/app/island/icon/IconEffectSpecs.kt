package com.droidbubble.app.island.icon

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.runtime.Immutable
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionTier

/** An icon's animated values. Each one rests at [rest]. */
enum class IconChannel(val rest: Float) {
    /** Uniform scale. */
    Scale(1f),
    /** Sideways offset, as a fraction of the icon's width. */
    OffsetX(0f),
    /** Vertical offset, as a fraction of the icon's height (negative = up). */
    OffsetY(0f),
    /** Turn about the pivot, in degrees. */
    Rotation(0f),
}

/** An effect after the motion tier and speed are applied (Convention 3). */
sealed interface ResolvedEffect {
    /** Velocity kicks on springs that rest at [IconChannel.rest]. A new kick adds to a running one. */
    @Immutable
    data class Kicks(val spec: SpringSpec<Float>, val kicks: List<Kick>) : ResolvedEffect

    /** Reduced motion: no transform, only opacity dipping to `1 − depth` and back over [spec]. */
    @Immutable
    data class OpacityDip(val spec: TweenSpec<Float>, val depth: Float) : ResolvedEffect

    @Immutable
    data class Kick(val channel: IconChannel, val velocity: Float)
}

/**
 * An indefinite effect after the tier is applied. A wave w goes 0 -> 1 -> 0 over [periodMillis]
 * on [easing] (like a reversing infinite transition); opacity is `minAlpha + (1 - minAlpha) * w`,
 * scale `scaleFrom + (scaleTo - scaleFrom) * w`. [turns]: a linear full turn per period instead.
 * [periodMillis] 0: no frames at all, the icon holds [minAlpha] and [scaleFrom].
 * [frameIntervalMillis] 0: every display frame.
 */
@Immutable
data class ResolvedLoop(
    val periodMillis: Int,
    val frameIntervalMillis: Int,
    val easing: Easing,
    val minAlpha: Float,
    val scaleFrom: Float,
    val scaleTo: Float,
    val turns: Boolean = false,
) {
    val isStatic: Boolean get() = periodMillis == 0

    companion object {
        fun still(alpha: Float = 1f, scale: Float = 1f) = ResolvedLoop(0, 0, LinearEasing, alpha, scale, scale)
    }
}

/**
 * Turns an [IconEffect] into springs and kicks for the island's current [IslandMotion].
 *
 * Kick sizes are chosen for a target peak, so they look the same at every animation speed: a
 * spring's peak travel is about `velocity / ω` times a damping factor, and speed scales ω, so the
 * velocity is scaled by speed too. All values are starting points to tune in the I4 catalogue.
 */
object IconEffectSpecs {
    /** Bounce: the icon dips to about 88 % before overshooting back. */
    const val BOUNCE_SCALE_VELOCITY = -4.3f
    /** Bounce: about 2 dp of drift on an 18 dp glyph (fractions of its height per second). */
    const val BOUNCE_OFFSET_VELOCITY = 4f
    /** Wiggle: about 14° at the first swing. */
    const val WIGGLE_ROTATION_VELOCITY = 263f
    /** Wiggle sideways: about 3 dp on an 18 dp glyph. */
    const val WIGGLE_OFFSET_VELOCITY = 3.1f
    /** Reduced motion: a bounce or wiggle becomes a dip to 60 % opacity. */
    const val REDUCED_DIP_DEPTH = 0.4f
    /**
     * Kicks that land on a moving icon add up, but the swing never gets bigger than one kick this
     * many times as strong: a burst of ten messages gives one lively bounce, not a wild one
     * (Convention 5).
     */
    const val MAX_STACKED_KICKS = 1.5f

    /** Reduced motion with system animations removed: a loop holds this opacity instead. */
    const val REMOVED_ANIMATIONS_ALPHA = 0.7f

    /**
     * Indefinite effects for [motion]'s tier:
     * - Full: as asked.
     * - Efficient (Battery Saver): the same, at most 30 frames a second.
     * - Reduced: a pulse stays (opacity isn't motion) but slows to 2 s; a breathe becomes that
     *   pulse at its resting size; a rotation stops. With [animationsRemoved] (system animator
     *   scale 0) every loop holds still.
     */
    fun resolveLoop(effect: IconEffect.Indefinite, motion: IslandMotion, animationsRemoved: Boolean = false): ResolvedLoop {
        val frame = if (motion.tier == MotionTier.EFFICIENT) IslandMotion.EFFICIENT_FRAME_MS else 0
        val wave = FastOutSlowInEasing
        if (motion.tier == MotionTier.REDUCED) {
            val minAlpha = when (effect) {
                is IconEffect.Pulse -> effect.minAlpha
                is IconEffect.Breathe -> effect.minAlpha
                is IconEffect.Rotate -> 1f
            }
            val restScale = (effect as? IconEffect.Breathe)?.scaleFrom ?: 1f
            if (animationsRemoved) return ResolvedLoop.still(if (minAlpha < 1f) REMOVED_ANIMATIONS_ALPHA else 1f, restScale)
            if (minAlpha >= 1f) return ResolvedLoop.still(1f, restScale)
            return ResolvedLoop(IslandMotion.Symbol.PULSE_REDUCED_PERIOD_MS, 0, wave, minAlpha, restScale, restScale)
        }
        return when (effect) {
            is IconEffect.Pulse -> ResolvedLoop(effect.periodMillis, frame, wave, effect.minAlpha, 1f, 1f)
            is IconEffect.Breathe -> ResolvedLoop(effect.periodMillis, frame, wave, effect.minAlpha, effect.scaleFrom, effect.scaleTo)
            is IconEffect.Rotate -> ResolvedLoop(effect.periodMillis, frame, LinearEasing, 1f, 1f, 1f, turns = true)
        }
    }

    fun resolve(effect: IconEffect.Discrete, motion: IslandMotion): ResolvedEffect {
        if (motion.tier == MotionTier.REDUCED) {
            return ResolvedEffect.OpacityDip(TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing), REDUCED_DIP_DEPTH)
        }
        val v = motion.speed
        return when (effect) {
            is IconEffect.Bounce -> ResolvedEffect.Kicks(
                motion.spring(IslandMotion.Symbol.bounce),
                listOf(
                    ResolvedEffect.Kick(IconChannel.Scale, BOUNCE_SCALE_VELOCITY * v),
                    ResolvedEffect.Kick(
                        IconChannel.OffsetY,
                        BOUNCE_OFFSET_VELOCITY * v * if (effect.direction == IconEffect.Direction.Up) -1f else 1f,
                    ),
                ),
            )
            is IconEffect.Wiggle -> ResolvedEffect.Kicks(
                motion.spring(IslandMotion.Symbol.wiggle),
                listOf(
                    when (effect.axis) {
                        IconEffect.WiggleAxis.Rotate -> ResolvedEffect.Kick(IconChannel.Rotation, WIGGLE_ROTATION_VELOCITY * v)
                        IconEffect.WiggleAxis.Horizontal -> ResolvedEffect.Kick(IconChannel.OffsetX, WIGGLE_OFFSET_VELOCITY * v)
                    },
                ),
            )
        }
    }
}
