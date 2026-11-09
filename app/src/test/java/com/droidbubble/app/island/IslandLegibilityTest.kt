package com.droidbubble.app.island

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B5.2 contrast and B9.2 bubble numeral rules. */
class IslandLegibilityTest {

    @After
    fun tearDown() {
        IslandTokens.highContrast = false
    }

    /** WCAG 2 contrast ratio. */
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun replyPlaceholder_reaches4_5to1_onTheInput() {
        IslandTokens.highContrast = false
        assertTrue("TextTertiary: ${contrast(IslandTokens.TextTertiary, IslandTokens.SurfaceMuted)}",
            contrast(IslandTokens.TextTertiary, IslandTokens.SurfaceMuted) >= 4.5f)
        // The old placeholder grey didn't
        assertTrue(contrast(Color(0xFF71717A), IslandTokens.SurfaceMuted) < 4.5f)
        IslandTokens.highContrast = true
        assertTrue(contrast(IslandTokens.TextTertiary, IslandTokens.SurfaceMuted) >= 7f)
    }

    @Test
    fun bubbleRing_followsTheBubble_within22to28dp() {
        assertEquals(26.dp, bubbleRingDiameter(36.dp))
        assertEquals(22.dp, bubbleRingDiameter(25.dp))
        assertEquals(28.dp, bubbleRingDiameter(48.dp))
    }

    @Test
    fun bubbleNumeral_showsOnlyWhenItFits() {
        // A 26 dp ring leaves a 21 dp opening; the digits' box must fit inside that circle.
        // 11 sp at font 1.0: digits about 7.9 dp tall, "42" about 13 dp, "100" about 18.5 dp wide
        assertTrue("42 at 1.0", bubbleNumeralFits(13.dp, 7.9.dp, 26.dp))
        assertTrue("100 at 1.0", bubbleNumeralFits(18.4.dp, 7.9.dp, 26.dp))
        // At font 1.5 the same "42" is about 19.8 by 11.9 dp: its corners would touch the ring
        assertFalse("42 at 1.5", bubbleNumeralFits(19.8.dp, 11.9.dp, 26.dp))
        // A small bubble's 22 dp ring (17 dp opening) takes "42" but not "100"
        assertTrue(bubbleNumeralFits(13.dp, 7.9.dp, 22.dp))
        assertFalse(bubbleNumeralFits(18.4.dp, 7.9.dp, 22.dp))
    }
}
