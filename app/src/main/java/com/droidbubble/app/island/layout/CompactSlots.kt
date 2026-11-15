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
     * Content sits against the camera on both sides (true) or against the pill's outer ends
     * (false, today's placement). One flag, for CP2.
     */
    const val SNUG_TO_CAMERA = true

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

    /** What the pill's two sides hold, as widths (dp); the text parts are measured by the caller. */
    data class Sides(val lead: Dp, val leadText: String?, val trail: Dp, val trailText: String?)

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
            IslandPriority.CALL -> Sides(glyph, null, 0.dp, "00:00")
            IslandPriority.SCREEN_RECORDING -> Sides(RECORDING_DOT, null, 0.dp, template(formatDuration(recording.durationSeconds)))
            IslandPriority.FLASHLIGHT -> Sides(FLASHLIGHT_GLOW, null, 0.dp, null)
            IslandPriority.MEDIA -> Sides(art, null, EQUALIZER, null)
            IslandPriority.PROGRESS -> {
                val icon = if (notification?.iconBitmap != null) art else glyph
                Sides(icon + PROGRESS_LABEL_GAP, notification?.appName ?: "Download", if (notification?.isIndeterminate == true) PROGRESS_SPINNER else 0.dp,
                    if (notification?.isIndeterminate == true) null else "100%")
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
                Sides(lead, null, 0.dp, text)
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

/**
 * The larger of the pill's two sides for what it shows now (V3.1), with the text measured in the
 * pill's own style. Recomputed only when the activity changes, never per tick.
 */
@Composable
fun rememberCompactSide(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    pillHeight: Dp,
): Dp {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val type = LocalIslandType.current
    val density = LocalDensity.current
    val sides = remember(priority, media, notification, recording.durationSeconds / 60, pillHeight) {
        CompactSlots.sides(priority, media, notification, recording, pillHeight)
    }
    return remember(sides, base, type, density) {
        fun measure(text: String?, weight: FontWeight): Dp = if (text == null) 0.dp else with(density) {
            val style = base.merge(TextStyle(fontSize = IslandType.CompactLabel, lineHeight = type.compactLabelLineHeight, fontWeight = weight))
            measurer.measure(text, style, maxLines = 1, softWrap = false).size.width.toDp()
        }
        val lead = sides.lead + measure(sides.leadText, FontWeight.Medium)
        val trail = sides.trail + measure(sides.trailText, FontWeight.SemiBold)
        max(lead.value, trail.value).dp
    }
}
