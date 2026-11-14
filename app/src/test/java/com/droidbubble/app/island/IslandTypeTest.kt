package com.droidbubble.app.island

import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F4.1: the Legacy roles are exactly the sizes the cards used inline. F4.2: the Trimmed line boxes. */
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

    private fun assertSp(message: String, expected: Float, actual: TextUnit) {
        assertTrue("$message is sp", actual.isSp)
        assertEquals(message, expected, actual.value, 0.001f)
    }

    /** F4.2: the line heights of 01 §6 at expandedScale 1.0. */
    @Test
    fun trimmed_lineHeights_atScale1() {
        val t = IslandType(1f)
        assertEquals(IslandType.Mode.Trimmed, t.mode)
        assertSp("compactValue", 16f, t.compactValueLineHeight)
        assertSp("compactLabel", 16f, t.compactLabelLineHeight)
        assertSp("bubbleValue", 14f, t.bubbleValueLineHeight)
        assertSp("bubble numeral", 14f, t.bubbleNumeral.lineHeight)
        assertSp("hero", 44f, t.heroLineHeight)
        assertSp("heroSmall", 32f, t.heroSmallLineHeight)
        for (card in IslandType.Card.values()) assertSp("title $card", 20f, t.titleLineHeight(card))
        assertSp("body", 20f, t.bodyLineHeight)
        assertSp("secondary", 16f, t.secondaryLineHeight)
        assertSp("button", 18f, t.buttonLineHeight)
        assertSp("caption", 14f, t.captionLineHeight)
        assertSp("reply chip", 16f, t.replyChipLineHeight)
        assertSp("reply input", 20f, t.replyInputLineHeight)
        assertSp("time label", 14f, t.timeLabel.lineHeight)
        assertSp("idle title", 20f, t.idleTitleLineHeight)
        assertSp("idle caption", 14f, t.idleCaptionLineHeight)
    }

    /** Sizes stay today's: only the boxes change. */
    @Test
    fun trimmed_keepsTheSizes() {
        for (scale in scales) {
            val trimmed = IslandType(scale, 1.2f, IslandType.Mode.Trimmed)
            val legacy = IslandType(scale, 1.2f, IslandType.Mode.Legacy)
            for (card in IslandType.Card.values()) assertEquals(legacy.title(card), trimmed.title(card))
            assertEquals(legacy.secondary, trimmed.secondary)
            assertEquals(legacy.body, trimmed.body)
            assertEquals(legacy.button, trimmed.button)
            assertEquals(legacy.caption, trimmed.caption)
            assertEquals(legacy.hero, trimmed.hero)
            assertEquals(legacy.heroSmall, trimmed.heroSmall)
            assertEquals(legacy.timeLabel.fontSize, trimmed.timeLabel.fontSize)
        }
    }

    /** Expanded line heights scale with their size, so the ratio stays at every scale. */
    @Test
    fun trimmed_lineHeights_scaleWithTheSize() {
        for (scale in scales) {
            val t = IslandType(scale)
            assertSp("title $scale", t.title(IslandType.Card.MEDIA).value * 20f / 17f, t.titleLineHeight(IslandType.Card.MEDIA))
            assertSp("live title $scale", t.title(IslandType.Card.LIVE).value * 20f / 16f, t.titleLineHeight(IslandType.Card.LIVE))
            assertSp("secondary $scale", t.secondary.value * 16f / 13f, t.secondaryLineHeight)
            assertSp("button $scale", t.button.value * 18f / 13.5f, t.buttonLineHeight)
            assertSp("caption $scale", t.caption.value * 14f / 12f, t.captionLineHeight)
            assertSp("hero $scale", t.hero.value * 44f / 38f, t.heroLineHeight)
            assertSp("heroSmall $scale", t.heroSmall.value * 32f / 28f, t.heroSmallLineHeight)
            // Compact roles never scale
            assertSp("compact $scale", 16f, t.compactLabelLineHeight)
        }
    }

    @Test
    fun trimmed_baseAndLineHeightStyles() {
        val t = IslandType(1.1f)
        val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
        assertEquals(trim, t.lineHeightStyle)
        assertEquals(trim, t.base.lineHeightStyle)
        assertSp("base", 20f, t.base.lineHeight)
        assertEquals("tnum", t.base.fontFeatureSettings)
        assertEquals(0.sp, t.base.letterSpacing)
        val hero = t.heroLineHeightStyle!!
        assertEquals(LineHeightStyle.Trim.Both, hero.trim)
        assertEquals(LineHeightStyle.Mode.Minimum, hero.mode)
    }

    /** Legacy still reproduces F4.1: nothing set, so text keeps the inherited box. */
    @Test
    fun legacy_setsNoLineHeights() {
        val t = IslandType(1.1f, mode = IslandType.Mode.Legacy)
        assertNull(t.lineHeightStyle)
        assertNull(t.heroLineHeightStyle)
        assertTrue(t.base.lineHeight.isUnspecified)
        assertNull(t.base.lineHeightStyle)
        for (lh in listOf(
            t.titleLineHeight(IslandType.Card.NOTIFICATION), t.secondaryLineHeight, t.buttonLineHeight,
            t.captionLineHeight, t.heroLineHeight, t.heroSmallLineHeight, t.compactLabelLineHeight,
            t.compactValueLineHeight, t.bubbleValueLineHeight, t.replyChipLineHeight, t.replyInputLineHeight,
            t.idleTitleLineHeight, t.idleCaptionLineHeight, t.timeLabel.lineHeight, t.bubbleNumeral.lineHeight,
        )) assertTrue(lh.isUnspecified)
    }

    @Test
    fun metricClasses_readTheLineHeights() {
        for (scale in scales) {
            val t = IslandType(scale)
            val n = NotificationCardMetrics(scale)
            assertEquals(t.titleLineHeight(IslandType.Card.NOTIFICATION), n.titleLineHeight)
            assertEquals(t.secondaryLineHeight, n.appNameLineHeight)
            assertEquals(t.buttonLineHeight, n.buttonLineHeight)
            val p = ProgressCardMetrics(scale)
            assertEquals(t.titleLineHeight(IslandType.Card.PROGRESS), p.titleLineHeight)
            val live = LiveLayout(scale, 1.3f)
            assertEquals(IslandType(scale, 1.3f).heroLineHeight, live.heroLineHeight)
            assertEquals(t.heroLineHeightStyle, live.heroLineHeightStyle)
        }
    }

    @Test
    fun withScale_keepsTheMode() {
        val t = IslandType(1.1f, 1.2f, IslandType.Mode.Trimmed)
        assertEquals(IslandType.Mode.Trimmed, t.withScale(0.9f).mode)
        assertEquals(1.2f, t.withScale(0.9f).fontScale, 0f)
        assertEquals(t, t.withScale(1.1f))
    }
}
