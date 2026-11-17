package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * D2.1: when the pill switches activity its width breathes out about 6 % and back, and with
 * Reduce motion it doesn't move at all. Both activities here are icon-only pills of the same
 * width, so any widening is the bump.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AlertBumpTest(private val reduceMotion: Boolean) {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true, reduceMotion = reduceMotion)))
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    private fun pillWidth(): Float = h.bounds(IslandTestTags.PILL)!!.let { it.right - it.left }.value

    @Test
    fun activityChange_breathesOnlyWithMotion() {
        rule.runOnIdle { IslandStateManager.updateProgress(IslandFixtures.progress(0).copy(isIndeterminate = true)) }
        h.settle(2000)
        val before = pillWidth()

        rule.mainClock.autoAdvance = false
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        var widest = before
        repeat(60) {
            rule.mainClock.advanceTimeBy(16)
            widest = maxOf(widest, pillWidth())
        }
        rule.mainClock.autoAdvance = true
        h.settle(2000)
        val after = pillWidth()

        assertEquals("both pills have the same width", before, after, 0.5f)
        val peak = widest / after - 1f
        if (reduceMotion) {
            assertTrue("no bump with Reduce motion (peak ${peak * 100} %)", peak < 0.005f)
        } else {
            // 6 % ± 1 %, sampled every 16 ms so a frame may miss the very top
            assertTrue("bump peak ${peak * 100} %", peak in 0.045f..0.07f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "reduceMotion={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }
}
