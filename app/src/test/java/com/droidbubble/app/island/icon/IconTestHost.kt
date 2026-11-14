package com.droidbubble.app.island.icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.model.IslandSettings

/**
 * Counts how often a host recomposes (Convention 2: an effect must recompose it 0 times per
 * frame). Call [hit] from a `SideEffect` written directly in the scope being measured: a
 * composable of its own would be its own scope and count nothing.
 */
class IconRecompositionProbe {
    var count = 0
        private set

    fun hit() {
        count++
    }

    fun reset() {
        count = 0
    }
}

/** The island's motion at [tier] and [speed], for icon tests. */
@Composable
fun WithIslandMotion(tier: MotionTier = MotionTier.FULL, speed: Float = 1f, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalIslandMotion provides IslandMotion.from(IslandSettings(animationSpeed = speed), tier),
        content = content,
    )
}

/** Steps the paused clock one 60 Hz frame at a time and samples [read] after each. */
fun ComposeContentTestRule.sampleFrames(frames: Int, read: () -> Float): List<Float> = List(frames) {
    mainClock.advanceTimeByFrame()
    waitForIdle()
    read()
}
