package com.droidbubble.app.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The island's text sizes, by role, on the system font (F4). Every size the island draws comes
 * from here, so a change to the type scale is a change to this one file.
 *
 * - Compact and bubble roles are fixed: the pill around the camera can't grow with the card scale.
 * - Expanded roles scale with `expandedScale` and are clamped.
 *
 * [Mode.Trimmed] (F4.2, the default) gives every role its own line height (01 §6) and trims the
 * box to the glyphs with `LineHeightStyle(Center, Trim.Both)`. Expanded line heights scale with
 * their size. [Mode.Legacy] keeps the old line boxes: line heights are not set, so text keeps
 * the 24 sp line box it inherits from Material's bodyLarge. (Sizes are the same in both modes.)
 */
@Immutable
class IslandType(
    val expandedScale: Float,
    val fontScale: Float = 1f,
    val mode: Mode = Mode.Trimmed,
) {
    enum class Mode { Legacy, Trimmed }

    private val s = expandedScale

    /** How much the phone's font size grows the live-activity rows (see [LiveLayout]). */
    private val textGrowth = fontScale.coerceIn(1f, IslandFontScale.CARD_MAX)
    private val rowGrowth = 1f + (textGrowth - 1f) * 0.6f

    private val trimmed = mode == Mode.Trimmed

    /**
     * A role's line height: [lineHeight] sp for a role drawn at [baseSize] sp, kept in that ratio
     * as the size scales and clamps. Unspecified in [Mode.Legacy], so the inherited box stays.
     */
    private fun lh(size: TextUnit, baseSize: Float, lineHeight: Float): TextUnit =
        if (trimmed) (size.value * lineHeight / baseSize).sp else TextUnit.Unspecified

    /** A fixed line height in [Mode.Trimmed], unspecified in [Mode.Legacy]. */
    private fun fixed(lineHeight: Float): TextUnit = if (trimmed) lineHeight.sp else TextUnit.Unspecified

    /** How every role's box sits on its glyphs. Null in [Mode.Legacy] (the inherited style stays). */
    val lineHeightStyle: LineHeightStyle? = if (trimmed) TrimBoth else null

    /** Heroes also never get a box shorter than the font, so a rolling digit is never cut. */
    val heroLineHeightStyle: LineHeightStyle? = if (trimmed) HeroTrim else null

    /**
     * The style every island Text starts from: tabular digits, the font's own tracking and, in
     * [Mode.Trimmed], body's 20 sp trimmed line box instead of bodyLarge's 24 sp one.
     * `IslandOverlayRoot` merges it into `LocalTextStyle`.
     */
    val base: TextStyle = if (trimmed) {
        TextStyle(fontFeatureSettings = "tnum", letterSpacing = 0.sp, lineHeight = 20.sp, lineHeightStyle = TrimBoth)
    } else {
        TextStyle(fontFeatureSettings = "tnum", letterSpacing = 0.sp)
    }

    // ---- Compact pill and bubble line heights: never scaled ----
    val compactValueLineHeight: TextUnit = fixed(16f)
    val compactLabelLineHeight: TextUnit = fixed(16f)
    val bubbleValueLineHeight: TextUnit = fixed(14f)

    /** The bubble's numeral, with its own line box. */
    val bubbleNumeral: TextStyle = BubbleNumeral.copy(lineHeight = bubbleValueLineHeight)

    // ---- Expanded cards: scale with expandedScale ----

    /**
     * Every card's title (the sender, the song, the activity name). F4.3: one size for all cards,
     * 17.6 sp at the default scale 1.1 (the notification, progress and media titles were 18.7).
     */
    val title: TextUnit = (16 * s).coerceIn(13.5f, 18f).sp
    val titleLineHeight: TextUnit = lh(title, 16f, 20f)

    /** The line under a title: the app name, the artist, a live activity's label. */
    val secondary: TextUnit = (13 * s).coerceIn(11.5f, 15f).sp
    val secondaryLineHeight: TextUnit = lh(secondary, 13f, 16f)

    /** Message and download text. */
    val body: TextUnit = (15 * s).coerceIn(13f, 17f).sp
    val bodyLineHeight: TextUnit = (20 * s).coerceIn(17f, 22f).sp

    /** Labels on a card's buttons. */
    val button: TextUnit = (13.5f * s).coerceIn(12f, 15f).sp
    val buttonLineHeight: TextUnit = lh(button, 13.5f, 18f)

    /** Small text on a live activity (a status, a detail). */
    val caption: TextUnit = (12 * s).coerceIn(10.5f, 13.5f).sp
    val captionLineHeight: TextUnit = lh(caption, 12f, 14f)

    /** A live activity's big value (time, ETA, score). Grows with the row, not with the font setting, so it always fits. */
    val hero: TextUnit = ((38 * s).coerceIn(31f, 44f) * rowGrowth / textGrowth).sp
    val heroSmall: TextUnit = ((28 * s).coerceIn(23f, 32f) * rowGrowth / textGrowth).sp
    val heroLineHeight: TextUnit = lh(hero, 38f, 44f)
    val heroSmallLineHeight: TextUnit = lh(heroSmall, 28f, 32f)

    // ---- Expanded views that ignored the scale until F4.3: they now scale like the rest ----

    /** A suggested reply chip: the caption size, with an eyebrow's 16 sp line. */
    val replyChip: TextUnit = caption
    val replyChipLineHeight: TextUnit = lh(replyChip, 12f, 16f)

    /** The reply field and its placeholder (body's line ratio). */
    val replyInput: TextUnit = (14 * s).coerceIn(12f, 16f).sp
    val replyInputLineHeight: TextUnit = lh(replyInput, 14f, 20f)

    /** The media card's elapsed and total time: tabular digits, so the seek bar never shifts. Caption's line ratio. */
    val timeLabel: TextStyle = TextStyle(
        fontSize = (11 * s).coerceIn(9.5f, 12.5f).sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
        lineHeight = if (trimmed) ((11 * s).coerceIn(9.5f, 12.5f) * 14f / 11f).sp else TextUnit.Unspecified,
    )

    /** The empty card's title (the body size, body's line), and its caption and button (the caption role). */
    val idleTitle: TextUnit = body
    val idleCaption: TextUnit = caption
    val idleTitleLineHeight: TextUnit = lh(idleTitle, 15f, 20f)
    val idleCaptionLineHeight: TextUnit = captionLineHeight

    fun withScale(scale: Float): IslandType = if (scale == expandedScale) this else IslandType(scale, fontScale, mode)

    companion object {
        // ---- Compact pill and bubble: never scaled ----

        /** A compact value next to the camera: timers, percentages, wattage (V3.2: 14 sp, weight 600). */
        val CompactValue: TextUnit = 14.sp

        /** A compact label: names, app names, the ringer mode (V3.2: 13 sp, weight 600, both sides). */
        val CompactLabel: TextUnit = 13.sp

        /** A unit beside a compact value: %, W (V3.2: 11 sp, weight 500, secondary colour). */
        val CompactUnit: TextUnit = 11.sp

        /** The download pill keeps its smaller text, app name and percentage (owner, 2026-11-15). */
        val CompactSmall: TextUnit = 12.sp

        /** The bubble's numeral (B9.2: 11 sp is the floor). */
        val BubbleValue: TextUnit = 11.sp
        val BubbleNumeral: TextStyle = TextStyle(fontSize = BubbleValue, fontWeight = FontWeight.Bold)

        /** Card header defaults, before a card passes its own scaled sizes. */
        val HeaderTitle: TextUnit = 17.sp
        val HeaderSubtitle: TextUnit = 13.sp
        val Body: TextUnit = 15.sp
        val Micro: TextUnit = 11.sp

        private val TrimBoth = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
        private val HeroTrim = LineHeightStyle(
            LineHeightStyle.Alignment.Center,
            LineHeightStyle.Trim.Both,
            LineHeightStyle.Mode.Minimum,
        )
    }
}

/** `IslandOverlayRoot` provides it for the card scale and font size in use. */
val LocalIslandType = staticCompositionLocalOf { IslandType(expandedScale = 1f) }

/** The island's type at [scale] (a card may be drawn at another scale than the island's own). */
@Composable
fun rememberIslandType(scale: Float): IslandType {
    val base = LocalIslandType.current
    val fontScale = LocalDensity.current.fontScale
    return remember(base, scale, fontScale) { IslandType(scale, fontScale, base.mode) }
}
