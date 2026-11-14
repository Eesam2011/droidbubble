package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * F4.2: a hero's box holds its glyphs, so `RollingNumberText`, which clips each rolling digit to
 * its box, never cuts the tops or bottoms of a digit, at rest or mid-roll.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandHeroLineBoxTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** What the overlay inherits from Material's bodyLarge: a 24 sp line, centred, not trimmed. */
    private val BodyLarge24 = TextStyle(
        lineHeight = 24.sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    @Test
    fun trimmedHero_boxHoldsTheGlyphs_atRestAndMidRoll() {
        val layout = LiveLayout(1.1f)
        val type = IslandType(1.1f)
        val text = mutableStateOf("9:59")
        var glyphHeight = 0
        rule.setContent {
            // The same base the overlay gives every Text, over bodyLarge's 24 sp line
            CompositionLocalProvider(LocalTextStyle provides BodyLarge24.merge(type.base)) {
                val measurer = rememberTextMeasurer()
                // The font's own height for the digits, with no line height applied
                glyphHeight = measurer.measure(
                    "10:00",
                    TextStyle(fontSize = layout.heroSize, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                ).size.height
                RollingNumberText(
                    text = text.value,
                    color = Color.White,
                    fontSize = layout.heroSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("hero"),
                    lineHeight = layout.heroLineHeight,
                    lineHeightStyle = layout.heroLineHeightStyle,
                )
            }
        }
        val settled = rule.onNodeWithTag("hero").fetchSemanticsNode().size.height
        assertTrue("hero box $settled px must hold the $glyphHeight px glyphs", settled >= glyphHeight)

        // Roll to the next value and stop halfway: the box (the roll's clip) doesn't change
        rule.mainClock.autoAdvance = false
        text.value = "10:00"
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(150)
        val midRoll = rule.onNodeWithTag("hero").fetchSemanticsNode().size.height
        assertTrue("mid-roll box $midRoll px must hold the $glyphHeight px glyphs", midRoll >= glyphHeight)
        assertEquals(settled, midRoll)
    }

    /** Documents the bug F4.2 fixes: the inherited 24 sp box was shorter than a hero's digits. */
    @Test
    fun legacyHero_boxWasShorterThanTheGlyphs() {
        val layout = LiveLayout(1.1f)
        val legacy = IslandType(1.1f, mode = IslandType.Mode.Legacy)
        var glyphHeight = 0
        rule.setContent {
            CompositionLocalProvider(LocalTextStyle provides BodyLarge24.merge(legacy.base)) {
                val measurer = rememberTextMeasurer()
                glyphHeight = measurer.measure(
                    "10:00",
                    TextStyle(fontSize = layout.heroSize, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                ).size.height
                RollingNumberText(
                    text = "10:00",
                    color = Color.White,
                    fontSize = layout.heroSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("hero"),
                    lineHeight = legacy.heroLineHeight,
                    lineHeightStyle = legacy.heroLineHeightStyle,
                )
            }
        }
        val box = rule.onNodeWithTag("hero").fetchSemanticsNode().size.height
        assertTrue("legacy box $box px vs glyphs $glyphHeight px", box < glyphHeight)
    }
}
