package com.droidbubble.app.island

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** F4.1: the Legacy roles are exactly the sizes the cards used inline. */
class IslandTypeTest {

    private val scales = listOf(0.8f, 1.0f, 1.1f, 1.4f)

    @Test
    fun expandedRoles_matchTheOldFormulas() {
        for (scale in scales) {
            val t = IslandType(scale)
            assertEquals("title $scale", (17 * scale).coerceIn(14f, 19f).sp, t.title(IslandType.Card.NOTIFICATION))
            assertEquals((17 * scale).coerceIn(14f, 19f).sp, t.title(IslandType.Card.PROGRESS))
            assertEquals((17 * scale).coerceIn(14f, 19f).sp, t.title(IslandType.Card.MEDIA))
            assertEquals((16 * scale).coerceIn(13.5f, 18f).sp, t.title(IslandType.Card.LIVE))
            assertEquals((13 * scale).coerceIn(11.5f, 15f).sp, t.secondary)
            assertEquals((15 * scale).coerceIn(13f, 17f).sp, t.body)
            assertEquals((20 * scale).coerceIn(17f, 22f).sp, t.bodyLineHeight)
            assertEquals((13.5f * scale).coerceIn(12f, 15f).sp, t.button)
            assertEquals((12 * scale).coerceIn(10.5f, 13.5f).sp, t.caption)
        }
    }

    @Test
    fun heroes_growWithTheRow_notTheFont() {
        for (scale in scales) {
            for (font in listOf(1f, 1.3f, 1.5f, 2f)) {
                val textGrowth = font.coerceIn(1f, 1.5f)
                val rowGrowth = 1f + (textGrowth - 1f) * 0.6f
                val t = IslandType(scale, font)
                assertEquals(((38 * scale).coerceIn(31f, 44f) * rowGrowth / textGrowth).sp, t.hero)
                assertEquals(((28 * scale).coerceIn(23f, 32f) * rowGrowth / textGrowth).sp, t.heroSmall)
            }
        }
    }

    @Test
    fun metricClasses_readTheRoles() {
        for (scale in scales) {
            val t = IslandType(scale)
            val n = NotificationCardMetrics(scale)
            assertEquals(t.title(IslandType.Card.NOTIFICATION), n.titleSize)
            assertEquals(t.secondary, n.appNameSize)
            assertEquals(t.body, n.bodySize)
            assertEquals(t.button, n.buttonFontSize)
            val p = ProgressCardMetrics(scale)
            assertEquals(t.title(IslandType.Card.PROGRESS), p.titleSize)
            assertEquals(t.bodyLineHeight, p.bodyLineHeight)
            val live = LiveLayout(scale, 1.3f)
            assertEquals(IslandType(scale, 1.3f).hero, live.heroSize)
            assertEquals(t.title(IslandType.Card.LIVE), live.titleSize)
        }
    }

    @Test
    fun compactRoles_neverScale() {
        assertEquals(13.sp, IslandType.CompactValue)
        assertEquals(12.sp, IslandType.CompactLabel)
        assertEquals(11.sp, IslandType.BubbleValue)
        val t = IslandType(1.4f)
        assertEquals(12.sp, t.replyChip)
        assertEquals(14.sp, t.replyInput)
        assertEquals(11.sp, t.timeLabel.fontSize)
        assertEquals(15.sp, t.idleTitle)
    }

    @Test
    fun withScale_keepsTheMode() {
        val t = IslandType(1.1f, 1.2f, IslandType.Mode.Trimmed)
        assertEquals(IslandType.Mode.Trimmed, t.withScale(0.9f).mode)
        assertEquals(1.2f, t.withScale(0.9f).fontScale, 0f)
        assertEquals(t, t.withScale(1.1f))
    }
}
