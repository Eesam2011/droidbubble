package com.droidbubble.app.island.layout

import com.droidbubble.app.island.MediaCardMetrics
import com.droidbubble.app.island.NotificationCardMetrics
import com.droidbubble.app.island.ProgressCardMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

/** V4.2: the tall cards share one rhythm for icon gaps, title spacing and buttons. */
class CardRhythmTest {
    private val scales = listOf(0.8f, 1f, 1.1f, 1.3f)

    @Test
    fun cardsShareTheRhythm() {
        for (s in scales) {
            val r = CardRhythm(s)
            val n = NotificationCardMetrics(s)
            val p = ProgressCardMetrics(s)
            val m = MediaCardMetrics(s)
            assertEquals(r.iconGap, n.iconGap); assertEquals(r.iconGap, p.iconGap); assertEquals(r.iconGap, m.iconGap)
            assertEquals(r.titleToSubtitle, n.subtitleSpacing); assertEquals(r.titleToSubtitle, p.subtitleSpacing)
            assertEquals(r.tile, n.iconSize); assertEquals(r.tile, p.iconSize)
            assertEquals(r.bodyToActions, n.actionsGap); assertEquals(r.bodyToActions, p.actionsGap)
            assertEquals(r.buttonHeight, n.buttonHeight); assertEquals(r.buttonHeight, p.buttonHeight)
        }
    }

    @Test
    fun theRulesClamp() {
        assertEquals(8f, CardRhythm(0.5f).iconGap.value, 0.01f)
        assertEquals(16f, CardRhythm(2f).iconGap.value, 0.01f)
        assertEquals(44f, CardRhythm(0.8f).buttonHeight.value, 0.01f)
        assertEquals(50f, CardRhythm(1.5f).buttonHeight.value, 0.01f)
    }
}
