package com.droidbubble.app.island.shell

import androidx.compose.ui.geometry.Rect
import com.droidbubble.app.island.IslandStateManager.GlassPane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M2.5: the glass panes are a pure function of the cards' rects and strengths. */
class GlassPublisherTest {

    private val card = Rect(20f, 100f, 1100f, 700f)

    @Test
    fun strength_isRoundedToSteps_ofAFifth() {
        assertEquals(0.5f, quantizeGlassStrength(0.51f), 0f)
        assertEquals(0.55f, quantizeGlassStrength(0.53f), 0f)
        assertEquals(1f, quantizeGlassStrength(0.99f), 0f)
        assertEquals(0f, quantizeGlassStrength(-0.2f), 0f)
        assertEquals(1f, quantizeGlassStrength(1.3f), 0f)
    }

    @Test
    fun settledCard_coversTheWholeRect() {
        val panes = glassPanes(arrayOf(card, null), floatArrayOf(1f, 0f), cornerPx = 90f, motionInsetPx = 9f)
        assertEquals(listOf(GlassPane(20, 100, 1080, 600, 90f, 1f)), panes)
    }

    @Test
    fun movingCard_isTuckedInside_byTheInset() {
        val pane = glassPanes(arrayOf(card, null), floatArrayOf(0.5f, 0f), cornerPx = 90f, motionInsetPx = 9f).single()
        assertEquals(GlassPane(29, 109, 1062, 582, 81f, 0.5f), pane)
    }

    @Test
    fun faintOrTinyCards_publishNothing() {
        assertTrue(glassPanes(arrayOf(card, card), floatArrayOf(0.02f, 0f), 90f, 9f).isEmpty())
        assertTrue(glassPanes(arrayOf(Rect(0f, 0f, 7f, 300f), null), floatArrayOf(1f, 0f), 90f, 9f).isEmpty())
        // 20 px wide, minus 9 px on each side, is too narrow
        assertTrue(glassPanes(arrayOf(Rect(0f, 0f, 20f, 300f), null), floatArrayOf(0.5f, 0f), 90f, 9f).isEmpty())
    }

    @Test
    fun twoCards_giveTwoPanes_inOrder() {
        val second = Rect(20f, 730f, 1100f, 1200f)
        val panes = glassPanes(arrayOf(card, second), floatArrayOf(1f, 1f), 90f, 9f)
        assertEquals(listOf(100, 730), panes.map { it.top })
    }

    @Test
    fun corner_neverGoesNegative() {
        assertEquals(0f, glassPanes(arrayOf(card, null), floatArrayOf(0.5f, 0f), cornerPx = 4f, motionInsetPx = 9f).single().cornerRadiusPx, 0f)
    }
}
