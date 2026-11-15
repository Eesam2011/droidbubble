package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** V4.1: the media card's measured height equals the height its rows really take. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaHeightParityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val config = mutableStateOf(1.1f to 1f)
    private var measured = 0.dp
    private var started = false

    /** One setContent per test (the rule allows no more); the scale and font size are state. */
    private fun measuredAndLaidOut(scale: Float, fontScale: Float): Pair<Dp, Dp> {
        rule.runOnIdle { config.value = scale to fontScale }
        if (!started) {
            started = true
            rule.setContent {
                val (s, f) = config.value
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, f)) {
                    val measurer = rememberTextMeasurer()
                    measured = measureMediaCardHeight(s, measurer, LocalTextStyle.current, LocalDensity.current)
                    // An unbounded height: the card's rows take exactly what they need
                    Box(Modifier.width(IslandTokens.Shell.ExpandedCardWidth * s).verticalScroll(rememberScrollState())) {
                        Box(Modifier.testTag("media")) { ExpandedMediaView(IslandFixtures.song, onCollapse = {}, scale = s) }
                    }
                }
            }
        }
        rule.waitForIdle()
        val bounds = rule.onNodeWithTag("media").getBoundsInRoot()
        return measured to (bounds.bottom - bounds.top)
    }

    @Test
    fun measuredEqualsLaidOut() {
        for (s in listOf(0.8f, 1.1f, 1.3f)) for (f in listOf(1f, 1.3f, 1.5f)) {
            val (m, laid) = measuredAndLaidOut(s, f)
            assertEquals("scale $s font $f", m.value, laid.value, 0.5f)
        }
    }

    @Test
    fun asTallAsBefore_atTheDefaultScale() {
        val (m, _) = measuredAndLaidOut(1.1f, 1f)
        // Owner: spacing as before, so the card is about the old table (195 × 1.1 = 214.5 dp)
        assertEquals("media card $m", 214.5f, m.value, 2f)
    }
}
