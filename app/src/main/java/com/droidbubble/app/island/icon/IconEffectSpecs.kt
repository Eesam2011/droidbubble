package com.droidbubble.app.island.icon

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
