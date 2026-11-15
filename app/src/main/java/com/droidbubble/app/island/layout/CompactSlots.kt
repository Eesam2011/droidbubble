package com.droidbubble.app.island.layout

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.IslandType
import com.droidbubble.app.island.LiveActivityKind
import com.droidbubble.app.island.LocalIslandType
import com.droidbubble.app.island.liveActivityKind
import com.droidbubble.app.island.liveCompactText
import com.droidbubble.app.island.staticLiveTimeText
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlin.math.max

/**
 * The compact pill hugs its content (V3.1): it is only as wide as the camera gap plus its larger
 * side twice (so the camera stays centred between balanced sides), plus the edge insets, clamped
 * between [MIN_WIDTH] and the user's width setting, which becomes the maximum.
 */
object CompactSlots {
    val MIN_WIDTH: Dp = 120.dp

    /**
     * Whether the pill shrinks to its content. Off: the owner wants the pill exactly the Width
     * setting (2026-11-15, after seeing V3.1). The width model stays for V3.3's bubble and as the
     * one switch to turn it back on.
     */
    const val HUG_CONTENT = false

    /**
     * Content sits against the camera on both sides (true) or against the pill's outer ends
     * (false, today's placement). One flag, for CP2.
     */
    const val SNUG_TO_CAMERA = false

    /**
     * V3.3: the bubble is a circle until its content (plus 6 dp each side) needs more, then an
     * oval up to 1.25 × its height. Nothing it shows today is wider than the circle (the
     * percentage sits inside its ring), so it stays round; the rule is for content that is.
     */
    fun bubbleWidth(contentWidth: Dp, height: Dp): Dp = (contentWidth + BUBBLE_PADDING * 2).coerceIn(height, height * 1.25f)
    val BUBBLE_PADDING: Dp = 6.dp

    /** The gap between the pill's edge and its content: today's rule. */
    fun inset(pillHeight: Dp): Dp = ((pillHeight - 20.dp) / 2f).coerceIn(6.dp, 14.dp)

    fun width(side: Dp, cameraZone: Dp, inset: Dp, wMin: Dp = MIN_WIDTH, wMax: Dp): Dp =
        (cameraZone + side * 2 + inset * 2).coerceIn(minOf(wMin, wMax), wMax)

    /**
     * A value measured at its widest form, so a ticking timer never changes the width: digits become
     * 0, and clock times take their full form (9:59 and 10:00 both measure as 00:00).
     */
    fun template(text: String): String {
        val t = text.trim()
        if (Regex("""^-?\d{1,2}:\d{2}$""").matches(t)) return (if (t.startsWith("-")) "-" else "") + "00:00"
        if (Regex("""^-?\d{1,2}:\d{2}:\d{2}$""").matches(t)) return (if (t.startsWith("-")) "-" else "") + "00:00:00"
        return text.map { if (it.isDigit()) '0' else it }.joinToString("")
    }

    /**
     * What the pill's two sides hold, as widths (dp); the text parts are measured by the caller.
     * A [trailValue] is a number (value size, never shortened) with an optional [trailUnit].
     */
    data class Sides(
        val lead: Dp,
        val leadText: String?,
        val trail: Dp,
        val trailText: String?,
        val trailValue: Boolean = false,
        val trailUnit: String? = null,
    )

    /**
     * The pill's two sides for an activity, mirroring CompactContent: icon, art and fixed widths
     * here, text left for measuring (already templated). Pure, so it is unit-tested.
     */
    fun sides(
        priority: IslandPriority,
        media: MediaInfo?,
        notification: NotificationInfo?,
        recording: ScreenRecordingState,
        pillHeight: Dp,
    ): Sides {
        val glyph = IslandTokens.Glyph.Compact
        val art = IslandTokens.Glyph.CompactArt
        return when (priority) {
            IslandPriority.CALL -> Sides(glyph, null, 0.dp, "00:00", trailValue = true)
            IslandPriority.SCREEN_RECORDING -> Sides(RECORDING_DOT, null, 0.dp, template(formatDuration(recording.durationSeconds)), trailValue = true)
            IslandPriority.FLASHLIGHT -> Sides(FLASHLIGHT_GLOW, null, 0.dp, null)
            IslandPriority.MEDIA -> Sides(art, null, EQUALIZER, null)
            IslandPriority.PROGRESS -> {
                val icon = if (notification?.iconBitmap != null) art else glyph
                val indeterminate = notification?.isIndeterminate == true
                Sides(icon, notification?.appName ?: "Download", if (indeterminate) PROGRESS_SPINNER else 0.dp,
                    if (indeterminate) null else "100", trailValue = true, trailUnit = if (indeterminate) null else "%")
            }
            IslandPriority.POP_NOTIFICATION -> {
                val kind = liveActivityKind(notification)
                val lead = when {
                    notification == null -> glyph
                    kind == LiveActivityKind.RECORDING -> RECORDER_WAVEFORM
                    kind == LiveActivityKind.TRACKING -> art
                    kind != LiveActivityKind.NONE -> glyph
                    notification.liveTimerBaseMs > 0L -> glyph
                    notification.avatarBitmap != null -> avatarFootprint(pillHeight)
                    notification.iconBitmap != null -> minOf(avatarFootprint(pillHeight), art)
                    else -> glyph
                }
                val timed = kind != LiveActivityKind.NONE && kind != LiveActivityKind.CALL
                val live = notification?.let { liveCompactText(it, kind) }
                    ?: notification?.takeIf { timed || it.liveTimerBaseMs > 0L }?.let { staticLiveTimeText(it) ?: "00:00" }
                val text = live?.let { template(it) } ?: notification?.title ?: "Notification"
                Sides(lead, null, 0.dp, text, trailValue = live != null)
            }
            IslandPriority.IDLE -> Sides(0.dp, null, 0.dp, null)
        }
    }

    /** A contact photo fills the pill's end with an even ring; this is how much of the side it takes. */
    fun avatarFootprint(pillHeight: Dp): Dp {
        val ring = (pillHeight * 0.13f).coerceIn(3.dp, 6.dp)
        val reach = (inset(pillHeight) - ring).coerceAtLeast(0.dp)
        return (pillHeight - ring * 2 - reach).coerceAtLeast(0.dp)
    }

    private fun formatDuration(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)

    // Fixed pieces of the pill's content (their sizes in CompactContent)
    val RECORDING_DOT: Dp = 11.dp
    val FLASHLIGHT_GLOW: Dp = 26.dp
    val EQUALIZER: Dp = 13.dp
    val RECORDER_WAVEFORM: Dp = 26.dp
    val PROGRESS_SPINNER: Dp = 14.dp
    val PROGRESS_LABEL_GAP: Dp = 6.dp
}

/** The compact text roles (V3.2): one family on both sides of the camera. */
object CompactText {
    fun label(type: com.droidbubble.app.island.IslandType) =
        TextStyle(fontSize = IslandType.CompactLabel, lineHeight = type.compactLabelLineHeight, fontWeight = FontWeight.SemiBold)

    fun value(type: com.droidbubble.app.island.IslandType) =
        TextStyle(fontSize = IslandType.CompactValue, lineHeight = type.compactLabelLineHeight, fontWeight = FontWeight.SemiBold)

    fun unit(type: com.droidbubble.app.island.IslandType) =
        TextStyle(fontSize = IslandType.CompactUnit, lineHeight = type.compactLabelLineHeight, fontWeight = FontWeight.Medium)

    /** The download pill's app name and percentage: the smaller size it had before V3.2 (owner). */
    fun small(type: com.droidbubble.app.island.IslandType, weight: FontWeight = FontWeight.SemiBold) =
        TextStyle(fontSize = IslandType.CompactSmall, lineHeight = type.compactLabelLineHeight, fontWeight = weight)
}

/**
 * The larger of the pill's two sides for what it shows now (V3.1), with the text measured in the
 * pill's own style and fitted the way the pill fits it (V3.2), within [maxSide]. Recomputed only
 * when the activity changes, never per tick.
 */
@Composable
fun rememberCompactSide(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    pillHeight: Dp,
    maxSide: Dp,
): Dp {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val type = LocalIslandType.current
    val density = LocalDensity.current
    val sides = remember(priority, media, notification, recording.durationSeconds / 60, pillHeight) {
        CompactSlots.sides(priority, media, notification, recording, pillHeight)
    }
    return remember(sides, base, type, density, maxSide) {
        fun width(text: String, style: TextStyle, squeeze: Boolean = false) =
            CompactFit.measure(measurer, density, text, base.merge(style), squeeze).dp
        fun fitted(text: String?, available: Dp): Dp {
            if (text == null) return 0.dp
            val r = CompactFit.fit(text, available.value) { t, sq -> width(t, CompactText.label(type), sq).value }
            return if (r.text == null) 0.dp else width(r.text, CompactText.label(type), r.scaleX < 1f)
        }
        val leadText = fitted(sides.leadText, maxSide - sides.lead - CompactSlots.PROGRESS_LABEL_GAP)
        val lead = sides.lead + if (leadText > 0.dp) CompactSlots.PROGRESS_LABEL_GAP + leadText else 0.dp
        val trail = sides.trail + when {
            sides.trailText == null -> 0.dp
            sides.trailValue -> width(sides.trailText, CompactText.value(type)) + (sides.trailUnit?.let { width(it, CompactText.unit(type)) } ?: 0.dp)
            else -> fitted(sides.trailText, maxSide - sides.trail)
        }
        max(lead.value, trail.value).dp
    }
}

/** V3.5: the glance pills' widest content, measured in their own styles. */
@Composable
fun rememberAlertSides(): com.droidbubble.app.island.scene.AlertSides {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val density = LocalDensity.current
    return remember(base, density) {
        fun w(text: String, size: androidx.compose.ui.unit.TextUnit) = CompactFit.measure(
            measurer, density, text, base.merge(TextStyle(fontSize = size, fontWeight = FontWeight.SemiBold)), squeeze = false
        )
        val ringer = listOf("Ring", "Vibrate", "Silent").maxOf { w(it, IslandType.CompactLabel) }
        val wattage = listOf("0.0W", "000W").maxOf { w(it, IslandType.CompactValue) }
        val bolt = IslandTokens.Glyph.Compact.value - CHARGING_BOLT_SHRINK + CHARGING_BOLT_GAP
        val charging = maxOf(bolt + wattage, w("100%", IslandType.CompactValue))
        com.droidbubble.app.island.scene.AlertSides(
            ringer = maxOf(IslandTokens.Glyph.Compact.value, ringer).dp,
            charging = charging.dp,
        )
    }
}

/** The charging bolt: the compact glyph size minus this, then this gap before the wattage. */
const val CHARGING_BOLT_SHRINK = 4f
const val CHARGING_BOLT_GAP = 3f
