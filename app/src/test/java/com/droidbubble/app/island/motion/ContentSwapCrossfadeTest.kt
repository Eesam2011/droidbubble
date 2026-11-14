package com.droidbubble.app.island.motion

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import com.droidbubble.app.model.IslandSettings
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M3.3: a content swap fades the old content out and the new one in on the same
 * `motion.effects()` spec, so at no moment is neither visible (alpha sum at least 0.9), in
 * every tier, profile and speed.
 */
class ContentSwapCrossfadeTest {

    private fun valueAt(spec: FiniteAnimationSpec<Float>, from: Float, to: Float, ms: Long): Float {
        val v = spec.vectorize(Float.VectorConverter)
        return v.getValueFromNanos(ms * 1_000_000, AnimationVector1D(from), AnimationVector1D(to), AnimationVector1D(0f)).value
    }

    @Test
    fun alphaSum_neverDips() {
        for (tier in MotionTier.values()) for (profile in IslandMotion.Profile.values()) for (speed in listOf(0.5f, 1f, 2f)) {
            val m = IslandMotion.from(IslandSettings(animationSpeed = speed), tier, profile)
            val spec = m.effects<Float>()
            for (ms in 0L..600L step 8) {
                val sum = valueAt(spec, 0f, 1f, ms) + valueAt(spec, 1f, 0f, ms)
                assertTrue("$tier $profile ${speed}x at $ms ms: $sum", sum >= 0.9f)
            }
        }
    }
}
