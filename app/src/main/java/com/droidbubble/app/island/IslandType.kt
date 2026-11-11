package com.droidbubble.app.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * The island's text sizes, by role, on the system font (F4). Every size the island draws comes
 * from here, so a change to the type scale is a change to this one file.
 *
 * - Compact and bubble roles are fixed: the pill around the camera can't grow with the card scale.
 * - Expanded roles scale with `expandedScale` and are clamped.
 *
 * [Mode.Legacy] reproduces today exactly: line heights are not set, so text keeps the 24 sp line
 * box it inherits from the overlay's base style (Material's bodyLarge). F4.2 adds [Mode.Trimmed].
 */
@Immutable
class IslandType(
    val expandedScale: Float,
    val fontScale: Float = 1f,
    val mode: Mode = Mode.Legacy,
) {
    enum class Mode { Legacy, Trimmed }

    /** Card families whose title size still differs today. F4.3 merges them. */
    enum class Card { NOTIFICATION, PROGRESS, MEDIA, LIVE }

    private val s = expandedScale

    /** How much the phone's font size grows the live-activity rows (see [LiveLayout]). */
    private val textGrowth = fontScale.coerceIn(1f, 1.5f)
    private val rowGrowth = 1f + (textGrowth - 1f) * 0.6f

    // ---- Expanded cards: scale with expandedScale ----

    /** A card's title (the sender, the song, the activity name). */
    fun title(card: Card): TextUnit = when (card) {
        Card.LIVE -> (16 * s).coerceIn(13.5f, 18f).sp
        else -> (17 * s).coerceIn(14f, 19f).sp
    }

    /** The line under a title: the app name, the artist, a live activity's label. */
    val secondary: TextUnit = (13 * s).coerceIn(11.5f, 15f).sp

    /** Message and download text. */
    val body: TextUnit = (15 * s).coerceIn(13f, 17f).sp
    val bodyLineHeight: TextUnit = (20 * s).coerceIn(17f, 22f).sp

    /** Labels on a card's buttons. */
    val button: TextUnit = (13.5f * s).coerceIn(12f, 15f).sp

    /** Small text on a live activity (a status, a detail). */
    val caption: TextUnit = (12 * s).coerceIn(10.5f, 13.5f).sp

    /** A live activity's big value (time, ETA, score). Grows with the row, not with the font setting, so it always fits. */
    val hero: TextUnit = ((38 * s).coerceIn(31f, 44f) * rowGrowth / textGrowth).sp
    val heroSmall: TextUnit = ((28 * s).coerceIn(23f, 32f) * rowGrowth / textGrowth).sp

    // ---- Expanded, not scaled yet (F4.3 scales them) ----

    /** A suggested reply chip. */
    val replyChip: TextUnit = 12.sp

    /** The reply field and its placeholder. */
    val replyInput: TextUnit = 14.sp

    /** The media card's elapsed and total time: tabular digits, so the seek bar never shifts. */
    val timeLabel: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

    /** The empty card's title, caption and button. */
    val idleTitle: TextUnit = 15.sp
    val idleCaption: TextUnit = 12.sp

    fun withScale(scale: Float): IslandType = if (scale == expandedScale) this else IslandType(scale, fontScale, mode)

    companion object {
        // ---- Compact pill and bubble: never scaled ----

        /** A compact value next to the camera: charging wattage and percentage. */
        val CompactValue: TextUnit = 13.sp

        /** A compact label: chips, the ringer mode, short status text. */
        val CompactLabel: TextUnit = 12.sp

        /** The bubble's numeral (B9.2: 11 sp is the floor). */
        val BubbleValue: TextUnit = 11.sp
        val BubbleNumeral: TextStyle = TextStyle(fontSize = BubbleValue, fontWeight = FontWeight.Bold)

        /** Card header defaults, before a card passes its own scaled sizes. */
        val HeaderTitle: TextUnit = 17.sp
        val HeaderSubtitle: TextUnit = 13.sp
        val Body: TextUnit = 15.sp
        val Micro: TextUnit = 11.sp
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
