package com.droidbubble.app.island

import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTween
import android.graphics.Bitmap
import android.content.Context
import android.app.PendingIntent
import android.app.ActivityOptions
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.parseTimerTextMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Shared header used by every expanded island card:
 * icon tile + title/subtitle column + optional trailing content (chip, spinner, visualizer).
 */
@Composable
fun IslandHeader(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    titleSize: TextUnit = IslandType.HeaderTitle,
    subtitleSize: TextUnit = IslandType.HeaderSubtitle,
    titleLineHeight: TextUnit = TextUnit.Unspecified,
    subtitleLineHeight: TextUnit = TextUnit.Unspecified,
    subtitleSpacing: Dp = IslandTokens.SpaceXxs,
    iconSpacing: Dp = IslandTokens.SpaceMd,
    titleMarquee: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Spacer(modifier = Modifier.width(iconSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = IslandTokens.TextPrimary,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (titleMarquee) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier
            )
            Spacer(modifier = Modifier.height(subtitleSpacing))
            Text(
                text = subtitle,
                color = IslandTokens.TextSecondary,
                fontSize = subtitleSize,
                lineHeight = subtitleLineHeight,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        trailing?.invoke()
    }
}

/**
 * Small rounded info chip pinned to the top-right of a header,
 * with an optional leading status dot.
 */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = IslandTokens.SurfaceRaised,
    contentColor: Color = IslandTokens.TextPrimary,
    dotColor: Color? = null,
    fontSize: TextUnit = IslandType.CompactLabel
) {
    Surface(
        shape = islandShape(IslandTokens.RadiusChip),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = IslandTokens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Chip with fully custom content (e.g. temperature + H/L range). */
@Composable
fun InfoChip(
    modifier: Modifier = Modifier,
    containerColor: Color = IslandTokens.SurfaceRaised,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        shape = islandShape(IslandTokens.RadiusChip),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = IslandTokens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

data class IslandActionItem(
    val title: String,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Canonical expanded-card height per priority: base height scaled by the card's
 * `expandedScale`, clamped to a per-priority minimum. Single source of truth used
 * by BOTH the primary (Card 1) and secondary (Card 2) slots so the tables can't drift.
 */
fun cardHeightFor(priority: IslandPriority?, scale: Float, notifHeight: Dp, progressHeight: Dp? = null, fontScale: Float = 1f, mediaHeight: Dp? = null): Dp {
    // V4.1: media cards are measured from their rows (see measureMediaCardHeight)
    if (priority == IslandPriority.MEDIA && mediaHeight != null) return mediaHeight
    // Notification cards are measured from their content (see measureNotificationCardHeight)
    if (priority == IslandPriority.POP_NOTIFICATION) return notifHeight
    // So are download cards (see measureProgressCardHeight)
    if (priority == IslandPriority.PROGRESS && progressHeight != null) return progressHeight
    // Screen recording uses the one-row native recorder design
    if (priority == IslandPriority.SCREEN_RECORDING) return liveActivityCardHeight(LiveActivityKind.RECORDING, scale, fontScale)
    val (baseH, minH) = when (priority) {
        IslandPriority.SCREEN_RECORDING -> 115.dp to 100.dp
        IslandPriority.PROGRESS -> 182.dp to 155.dp
        IslandPriority.FLASHLIGHT -> 205.dp to 165.dp
        else -> 195.dp to 155.dp // media, idle, charging...
    }
    // Larger system font sizes get proportionally taller fixed-layout cards, so text isn't cut off
    val textGrowth = 1f + (fontScale.coerceIn(1f, 1.5f) - 1f) * 0.5f
    return (baseH * scale * textGrowth).coerceAtLeast(minH)
}

/** V2.2: the two card insets of the one margin system ([CardGeometry]). */
enum class CardInsetKind { ROOMY, TIGHT }

/** Tall cards are roomy (R/2, so a corner-nested element is concentric); the one-row flashlight card is tight. */
fun cardInsetKind(priority: IslandPriority): CardInsetKind =
    if (priority == IslandPriority.FLASHLIGHT) CardInsetKind.TIGHT else CardInsetKind.ROOMY

/** The inset of an expanded card's content from the card edge, the same on all four sides. */
fun cardInsetDp(priority: IslandPriority, scale: Float, fontScale: Float): Dp {
    val g = com.droidbubble.app.island.layout.CardGeometry(scale, fontScale)
    return if (cardInsetKind(priority) == CardInsetKind.TIGHT) g.insetTight else g.insetRoomy
}

/**
 * Inner content padding for expanded cards (V2.2: one margin system). Each view applies it at its
 * own root so the card container stays layout-agnostic.
 */
fun Modifier.cardInset(priority: IslandPriority, scale: Float, fontScale: Float): Modifier =
    padding(cardInsetDp(priority, scale, fontScale))

/**
 * Sizes of the expanded notification card. Shared by [ExpandedNotificationView] and
 * [measureNotificationCardHeight] so the card is always exactly as tall as its content.
 */
class NotificationCardMetrics(scale: Float, fontScale: Float = 1f, type: IslandType = IslandType(scale)) {
    val horizontalPadding = cardInsetDp(IslandPriority.POP_NOTIFICATION, scale, fontScale)
    val verticalPadding = horizontalPadding
    val iconSize = (40.dp * scale).coerceIn(32.dp, 48.dp)
    val iconGap = (IslandTokens.SpaceMd * scale).coerceIn(8.dp, 16.dp)
    val titleSize = type.title
    val titleLineHeight = type.titleLineHeight
    val appNameSize = type.secondary
    val appNameLineHeight = type.secondaryLineHeight
    val subtitleSpacing = (IslandTokens.SpaceXxs * scale).coerceIn(1.dp, 3.dp)
    val bodySize = type.body
    val bodyLineHeight = type.bodyLineHeight
    val bodyMaxLines = if (scale < 0.88f) 2 else 3
    val headerBodyGap = (IslandTokens.SpaceSm * scale).coerceIn(6.dp, 10.dp)
    val actionsGap = (IslandTokens.SpaceMd * scale).coerceIn(10.dp, 14.dp)
    val buttonHeight = (42.dp * scale).coerceIn(44.dp, 50.dp) // 44dp+ visual, inside a 48dp touch row
    val buttonFontSize = type.button
    val buttonLineHeight = type.buttonLineHeight
    val replyChipFontSize = type.replyChip
    val replyChipLineHeight = type.replyChipLineHeight
    val replyInputSize = type.replyInput
    val replyInputLineHeight = type.replyInputLineHeight
    val replyChipVerticalPadding = 5.dp
    val replyRowSpacing = 6.dp
    val replyInputHeight = 42.dp
}

/** Sizes of the expanded download / progress card, shared by the view and its height measurement. */
class ProgressCardMetrics(scale: Float, fontScale: Float = 1f, type: IslandType = IslandType(scale)) {
    val verticalPadding = cardInsetDp(IslandPriority.PROGRESS, scale, fontScale)
    val iconSize = (40.dp * scale).coerceIn(32.dp, 48.dp)
    val iconGap = (IslandTokens.SpaceMd * scale).coerceIn(8.dp, 16.dp)
    val titleSize = type.title
    val titleLineHeight = type.titleLineHeight
    val appNameSize = type.secondary
    val appNameLineHeight = type.secondaryLineHeight
    val subtitleSpacing = (IslandTokens.SpaceXxs * scale).coerceIn(1.dp, 3.dp)
    val gap = (IslandTokens.SpaceMd * scale).coerceIn(9.dp, 14.dp)
    val bodySize = type.body
    val bodyLineHeight = type.bodyLineHeight
    val barGap = (IslandTokens.SpaceSm * scale).coerceIn(6.dp, 10.dp)
    val barHeight = 6.dp
    val actionsGap = (IslandTokens.SpaceMd * scale).coerceIn(10.dp, 14.dp)
    val buttonHeight = (42.dp * scale).coerceIn(44.dp, 50.dp)
    val buttonFontSize = type.button
    val buttonLineHeight = type.buttonLineHeight
}

/**
 * Sizes of the expanded media card (V4.1), shared by [ExpandedMediaView] and
 * [measureMediaCardHeight] so the card is exactly as tall as its rows.
 */
class MediaCardMetrics(scale: Float, fontScale: Float = 1f) {
    val inset = cardInsetDp(IslandPriority.MEDIA, scale, fontScale)
    val artSize = (54.dp * scale).coerceIn(40.dp, 64.dp)
    val subtitleSpacing = 3.dp
    /**
     * Song info to seek bar, and seek bar to controls: the room the old fixed-height layout gave
     * them, kept by the owner (2026-11-15). The card is still measured from its rows.
     */
    /** The room the owner asked back between the info, the bar and the controls (whole dp, so no rounding). */
    private val restoredRoom = (11f * scale).roundToInt().coerceIn(8, 15).dp
    val sliderGapTop = (IslandTokens.SpaceSm * scale).coerceIn(4.dp, 10.dp) + restoredRoom
    val sliderGapBottom = (IslandTokens.SpaceXs * scale).coerceIn(2.dp, 8.dp) + restoredRoom
    /** The seek bar's track row (the thumb is at most 18 dp, inside it while seeking). */
    val sliderHeight = 20.dp
    val prevNextSize = (42.dp * scale).coerceIn(34.dp, 48.dp)
    val playBtnSize = (50.dp * scale).coerceIn(40.dp, 58.dp)
    val sideControlSize = (36.dp * scale).coerceIn(30.dp, 42.dp)
    val controlsHeight get() = maxOf(playBtnSize, prevNextSize, sideControlSize)
}

/**
 * V4.1: the media card's height from its rows: header (cover beside title and artist), the seek
 * row, the controls, and the inset all round. No slack: the rows used to be spread over a fixed
 * table height (195 × scale) with SpaceBetween.
 */
fun measureMediaCardHeight(
    scale: Float,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    density: Density
): Dp = with(density) {
    val m = MediaCardMetrics(scale, density.fontScale)
    val type = IslandType(scale, density.fontScale)
    fun lineHeight(style: TextStyle): Dp = textMeasurer.measure(text = "Ag", style = baseStyle.merge(style), maxLines = 1).size.height.toDp()
    // The weights IslandHeader draws them in
    val title = lineHeight(TextStyle(fontSize = type.title, lineHeight = type.titleLineHeight, fontWeight = FontWeight.SemiBold))
    val artist = lineHeight(TextStyle(fontSize = type.secondary, lineHeight = type.secondaryLineHeight, fontWeight = FontWeight.Medium))
    val header = maxOf(m.artSize, title + m.subtitleSpacing + artist)
    val slider = maxOf(m.sliderHeight, lineHeight(type.timeLabel))
    val content = m.inset * 2 + header + m.sliderGapTop + slider + m.sliderGapBottom + m.controlsHeight
    // Never shorter than a capsule of the card's own radius
    content.coerceAtLeast(liveActivityCornerRadius(scale, density.fontScale) * 2)
}

/** Detail line of a download ("12 MB of 40 MB"), without stray leading / trailing bullets. */
fun cleanProgressText(raw: String): String {
    var s = raw.trim()
    while (s.startsWith("•") || s.startsWith("·") || s.startsWith("-")) s = s.substring(1).trim()
    while (s.endsWith("•") || s.endsWith("·") || s.endsWith("-")) s = s.substring(0, s.length - 1).trim()
    return s.ifBlank { "In progress…" }
}

/** Buttons on the download card: "Open" is dropped since tapping the card opens the app. */
fun progressActions(notification: NotificationInfo?): List<NotificationAction> =
    (notification?.actions ?: emptyList())
        .filterNot { it.title.equals("Open", ignoreCase = true) }
        .take(2)

/**
 * Exact height of the expanded download card: padding, header (icon or two text lines, whichever
 * is taller), one detail line, the bar and, only when the app gives some, a row of buttons.
 */
fun measureProgressCardHeight(
    notification: NotificationInfo?,
    scale: Float,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    density: Density
): Dp = with(density) {
    val m = ProgressCardMetrics(scale, density.fontScale)
    fun lineHeight(style: TextStyle): Dp = textMeasurer.measure(text = "Ag", style = style, maxLines = 1).size.height.toDp()
    val title = lineHeight(baseStyle.merge(TextStyle(fontSize = m.titleSize, lineHeight = m.titleLineHeight, fontWeight = FontWeight.SemiBold)))
    val subtitle = lineHeight(baseStyle.merge(TextStyle(fontSize = m.appNameSize, lineHeight = m.appNameLineHeight, fontWeight = FontWeight.Medium)))
    val header = maxOf(m.iconSize, title + m.subtitleSpacing + subtitle)
    val body = lineHeight(baseStyle.merge(TextStyle(fontSize = m.bodySize, lineHeight = m.bodyLineHeight)))
    val actions = if (progressActions(notification).isNotEmpty()) m.actionsGap + m.buttonHeight else 0.dp
    m.verticalPadding * 2 + header + m.gap + body + m.barGap + m.barHeight + actions + 1.dp
}

/** Notification actions shown as buttons: "Open" is redundant since tapping the card opens it. */
fun visibleNotificationActions(notification: NotificationInfo?): List<NotificationAction> =
    (notification?.actions ?: emptyList())
        .filterNot { it.title.equals("Open", ignoreCase = true) }
        // Timers show all their controls (Pause, +1:00, Stop) like the clock notification
        .take(if ((notification?.liveTimerBaseMs ?: 0L) > 0L) 3 else 2)

/**
 * Exact height of the expanded notification card for [notification] at [cardWidth]:
 * padding + header + body (wrapped to its real line count) + actions/reply row.
 * [baseStyle] must be the LocalTextStyle the card's Text composables inherit.
 */
fun measureNotificationCardHeight(
    notification: NotificationInfo?,
    title: String,
    scale: Float,
    cardWidth: Dp,
    isReplying: Boolean,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    density: Density
): Dp = with(density) {
    val m = NotificationCardMetrics(scale, density.fontScale)
    // Call card: one row (photo, name, round answer / decline buttons)
    // Calls, recorders, timers: one native row (photo / time, round buttons)
    liveActivityKind(notification).takeIf { it != LiveActivityKind.NONE }?.let { return@with liveActivityCardHeight(it, scale, density.fontScale) }
    val innerWidth = (cardWidth - m.horizontalPadding * 2).coerceAtLeast(1.dp)

    fun textHeight(text: String, style: TextStyle, width: Dp, maxLines: Int): Dp =
        textMeasurer.measure(
            text = text,
            style = baseStyle.merge(style),
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width.roundToPx().coerceAtLeast(1))
        ).size.height.toDp()

    val titleHeight = textHeight(title, TextStyle(fontSize = m.titleSize, lineHeight = m.titleLineHeight, fontWeight = FontWeight.SemiBold), innerWidth, 1)
    val subtitleHeight = textHeight(
        notification?.appName ?: "",
        TextStyle(fontSize = m.appNameSize, lineHeight = m.appNameLineHeight, fontWeight = FontWeight.Medium),
        innerWidth,
        1
    )
    var height = m.verticalPadding * 2 + maxOf(m.iconSize, titleHeight + m.subtitleSpacing + subtitleHeight)

    val body = notification?.text.orEmpty()
    if (body.isNotBlank()) {
        height += m.headerBodyGap + textHeight(
            body,
            TextStyle(fontSize = m.bodySize, lineHeight = m.bodyLineHeight, fontWeight = FontWeight.Normal),
            innerWidth - m.iconSize - m.iconGap,
            m.bodyMaxLines
        )
    }

    if (isReplying) {
        // B6.1: the chip row exists only when the app supplied reply choices
        val chipRow = if (replyChoices(notification).isNotEmpty()) {
            textHeight("OK", TextStyle(fontSize = m.replyChipFontSize, lineHeight = m.replyChipLineHeight, fontWeight = FontWeight.Medium), innerWidth, 1) +
                m.replyChipVerticalPadding * 2 + m.replyRowSpacing
        } else 0.dp
        height += m.actionsGap + chipRow + m.replyInputHeight
    } else if (visibleNotificationActions(notification).isNotEmpty()) {
        height += m.actionsGap + m.buttonHeight
    }

    // Round up so sub-pixel text metrics never clip the last line
    height + 1.dp
}

/**
 * Ticking timer text ("01:23", "1:02:03") for notifications that carry a live timer
 * (e.g. a voice recorder), or null when the notification has none.
 */
@Composable
fun rememberLiveTimerText(notification: NotificationInfo?): String? {
    val base = notification?.liveTimerBaseMs ?: 0L
    val countDown = notification?.liveTimerCountDown == true
    var now by remember { mutableLongStateOf(IslandClock.wallMs()) }
    LaunchedEffect(base, countDown) {
        if (base <= 0L) return@LaunchedEffect
        while (true) {
            now = IslandClock.wallMs()
            // Wake right as the next whole second of the timer flips
            delay(1000L - Math.floorMod(now - base, 1000L))
        }
    }
    if (base <= 0L) return null
    // `now` is only as fresh as the last tick: when the base moves (a notification update), the
    // first frame would mix the new base with a stale clock and show the previous second, so
    // the digits rolled back and then forward again. Read the clock here too.
    val t = maxOf(now, IslandClock.wallMs())
    val millis = if (countDown) base - t else t - base
    return formatTimerText(millis)
}

fun formatTimerText(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Title to show for a notification: a live timer replaces a timer-looking title
 * (e.g. a recorder posting "00:00") so it keeps counting.
 */
fun notificationDisplayTitle(notification: NotificationInfo?, liveTimer: String?): String {
    val title = notification?.title ?: "Notification"
    return if (liveTimer != null && parseTimerTextMillis(title) != null) liveTimer else title
}

/**
 * Equal-width segmented action row with squarish inner corners.
 * Renders nothing when [items] is empty.
 */
@Composable
fun IslandActionButtonRow(
    items: List<IslandActionItem>,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    fontSize: TextUnit = IslandType.CompactLabel,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    if (items.isEmpty()) return
    val total = items.size
    val geometry = com.droidbubble.app.island.layout.LocalCardGeometry.current

    Row(
        modifier = modifier.fillMaxWidth(),
        // 6dp apart: still reads as one segmented control, but a finger can't hit two buttons
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            // V2.3: outer corners concentric with the card (a near-capsule), inner corners small
            val outer = geometry.inner(geometry.insetRoomy)
            val inner = IslandTokens.SpaceXs
            val shape = when {
                total <= 1 -> islandShape(outer)
                index == 0 -> islandShape(topStart = outer, topEnd = inner, bottomEnd = inner, bottomStart = outer)
                index == total - 1 -> islandShape(topStart = inner, topEnd = outer, bottomEnd = outer, bottomStart = inner)
                else -> islandShape(inner)
            }

            FilledTonalButton(
                onClick = item.onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                shape = shape,
                contentPadding = PaddingValues(horizontal = IslandTokens.SpaceSm, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (item.isPrimary) IslandTokens.ActionPrimaryContainer else IslandTokens.ActionSecondaryContainer,
                    contentColor = if (item.isPrimary) IslandTokens.ActionPrimaryContent else IslandTokens.TextBody
                )
            ) {
                Text(
                    text = item.title,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = if (item.isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Identity of the playing track; the cover flips when this changes. */
fun mediaTrackKey(media: com.droidbubble.app.model.MediaInfo?): String? =
    media?.let { "${it.packageName}|${it.title}|${it.artist}" }

/**
 * Album cover that does a 3D card flip when [trackKey] changes (new song). Art that arrives
 * late for the same song swaps in silently. Only animates during the ~0.4s flip itself.
 */
@Composable
fun FlippingCoverArt(
    art: Bitmap?,
    trackKey: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Cover Art",
    placeholder: @Composable () -> Unit = {}
) {
    var shownArt by remember { mutableStateOf(art) }
    var shownKey by remember { mutableStateOf(trackKey) }
    // True during the first half of a flip, while the old cover is still facing the viewer
    var isTurningAway by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    // I3.4, Reduced motion: the old cover fading out over the new one instead of a turn
    var fadingArt by remember { mutableStateOf<Bitmap?>(null) }
    val fade = remember { Animatable(0f) }
    val latestArt by rememberUpdatedState(art)
    val scope = rememberCoroutineScope()

    val motion = LocalIslandMotion.current
    LaunchedEffect(trackKey) {
        if (trackKey == shownKey) return@LaunchedEffect
        shownKey = trackKey
        // Separate scope so a late art update can't cancel the flip halfway through
        scope.launch {
            if (motion.tier == MotionTier.REDUCED) {
                rotation.snapTo(0f)
                fadingArt = shownArt
                shownArt = latestArt
                fade.snapTo(1f)
                fade.animateTo(0f, motion.tween(MotionTween.ART_FLIP_OUT))
                fadingArt = null
                return@launch
            }
            // I3.4: both halves follow the island's speed, like every symbol effect
            isTurningAway = true
            rotation.animateTo(90f, motion.tween(MotionTween.ART_FLIP_OUT.millis, MotionTween.ART_FLIP_OUT.easing))
            shownArt = latestArt
            isTurningAway = false
            rotation.snapTo(-90f)
            rotation.animateTo(0f, motion.spring(IslandMotion.Symbol.replace))
        }
    }
    LaunchedEffect(art) {
        if (!isTurningAway) shownArt = art
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = shownArt
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            placeholder()
        }
        val old = fadingArt
        if (old != null) {
            Image(
                bitmap = old.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }
            )
        }
    }
}

/** The old name of [IslandGeometry]; kept for one release (F1.3). */
typealias CardMorph = IslandGeometry

/** Blur radii come in steps this size, so a morph reuses a few effects instead of one per frame. */
private const val BLUR_STEP_PX = 0.5f
private val blurEffectCache = HashMap<Int, RenderEffect>()

/**
 * Blur at [strength] (0..1) of [maxRadiusPx], rounded to [BLUR_STEP_PX] steps and cached, so
 * an animated blur allocates nothing frame to frame. Null when it would be invisible (under one
 * step), before Android 12, or with Battery Saver on: content then just fades, which costs
 * nothing extra on the GPU. Reading Battery Saver here also redraws the layer when it changes.
 */
fun steppedBlurEffect(strength: Float, maxRadiusPx: Float): RenderEffect? {
    if (!IslandApi.hasRenderEffect) return null
    if (AudioPowerPolicy.lowPower) return null
    val steps = (strength.coerceIn(0f, 1f) * maxRadiusPx / BLUR_STEP_PX).roundToInt()
    if (steps < 1) return null
    return blurEffectCache.getOrPut(steps) {
        val radius = steps * BLUR_STEP_PX
        BlurEffect(radius, radius, TileMode.Decal)
    }
}

/**
 * Notification picture: the sender's photo (WhatsApp contact, SMS sender...) as a circle with a
 * small app-icon badge, like iOS communication notifications; otherwise the app icon.
 */
@Composable
fun NotificationAvatar(
    notification: NotificationInfo?,
    size: Dp,
    modifier: Modifier = Modifier,
    showAppBadge: Boolean = true,
    fallback: @Composable () -> Unit = {}
) {
    val avatar = notification?.avatarBitmap
    val appIcon = notification?.iconBitmap
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            avatar != null -> {
                Image(
                    bitmap = avatar.asImageBitmap(),
                    contentDescription = "Sender",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
                if (showAppBadge && appIcon != null) {
                    val badge = size * 0.42f
                    Image(
                        bitmap = appIcon.asImageBitmap(),
                        contentDescription = notification.appName,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = badge * 0.18f, y = badge * 0.18f)
                            .size(badge)
                            .clip(CircleShape)
                            .border(1.5.dp, IslandTokens.CardBackground, CircleShape)
                    )
                }
            }
            appIcon != null -> {
                Image(
                    bitmap = appIcon.asImageBitmap(),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(islandShape(size * 0.25f))
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(islandShape(size * 0.25f))
                        .background(IslandTokens.SurfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    fallback()
                }
            }
        }
    }
}

/** Sends a notification PendingIntent, allowing it to open an activity from the overlay. */
fun sendIslandPendingIntent(context: Context, intent: PendingIntent?): Boolean {
    if (intent == null) return false
    return runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic().apply {
                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }.toBundle()
            intent.send(context, 0, null, null, null, null, options)
        } else {
            intent.send()
        }
    }.isSuccess
}

/**
 * Small clock for running timers: the hand steps once per second with a short ease, so it
 * reads as smooth motion while only animating ~0.3s of every second.
 */
@Composable
fun TimerClockIcon(baseMs: Long, countDown: Boolean, size: Dp, color: Color, modifier: Modifier = Modifier) {
    val angle = remember { Animatable(0f) }
    val motion = LocalIslandMotion.current
    LaunchedEffect(baseMs, countDown) {
        while (true) {
            val now = IslandClock.wallMs()
            val seconds = kotlin.math.abs(now - baseMs) / 1000L
            var target = (seconds % 60L) * 6f * (if (countDown) -1f else 1f)
            // Keep turning the same way instead of spinning back at the full minute
            val current = angle.value
            while (target - current > 180f) target -= 360f
            while (current - target > 180f) target += 360f
            angle.animateTo(target, motion.tween(MotionTween.CLOCK_HAND))
            delay(1000L - Math.floorMod(IslandClock.wallMs() - baseMs, 1000L))
        }
    }
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.09f
        val radius = this.size.minDimension / 2f - stroke / 2f
        drawCircle(color = color, radius = radius, style = Stroke(width = stroke))
        rotate(angle.value) {
            drawLine(
                color = color,
                start = center,
                end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius * 0.68f),
                strokeWidth = stroke * 1.1f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = color, radius = stroke * 0.9f)
    }
}

/**
 * B6.1: the smart-reply choices the app itself supplied for its reply action, or none. Shared by
 * the reply row and the card height, so the card never reserves room for a missing chip row.
 */
fun replyChoices(notification: NotificationInfo?): List<String> =
    notification?.actions
        ?.firstOrNull { it.isReply || it.title.contains("Reply", ignoreCase = true) }
        ?.remoteInputChoices
        .orEmpty()
