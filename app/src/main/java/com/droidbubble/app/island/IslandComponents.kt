package com.droidbubble.app.island

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
    titleSize: TextUnit = IslandTokens.TypeTitle,
    subtitleSize: TextUnit = IslandTokens.TypeSubtitle,
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
    fontSize: TextUnit = IslandTokens.TypeCaption
) {
    Surface(
        shape = RoundedCornerShape(IslandTokens.RadiusChip),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
        shape = RoundedCornerShape(IslandTokens.RadiusChip),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
fun cardHeightFor(priority: IslandPriority?, scale: Float, notifHeight: Dp): Dp {
    // Notification cards are measured from their content (see measureNotificationCardHeight)
    if (priority == IslandPriority.POP_NOTIFICATION) return notifHeight
    val (baseH, minH) = when (priority) {
        IslandPriority.SCREEN_RECORDING -> 115.dp to 100.dp
        IslandPriority.PROGRESS -> 182.dp to 155.dp
        IslandPriority.FLASHLIGHT -> 205.dp to 165.dp
        IslandPriority.FPS -> 185.dp to 150.dp
        IslandPriority.WEATHER -> 195.dp to 155.dp
        IslandPriority.NETWORK_SPEED -> 185.dp to 150.dp
        else -> 195.dp to 155.dp // media, idle, charging...
    }
    return (baseH * scale).coerceAtLeast(minH)
}

/**
 * Inner content padding for expanded cards. Each view applies this at its own root
 * so the card container stays layout-agnostic. Flashlight is the only card with
 * tighter insets; notification/progress get slightly less vertical room.
 */
fun Modifier.islandCardContentPadding(priority: IslandPriority, scale: Float): Modifier =
    padding(
        horizontal = islandCardHorizontalPadding(priority, scale),
        vertical = islandCardVerticalPadding(priority, scale)
    )

fun islandCardHorizontalPadding(priority: IslandPriority, scale: Float): Dp =
    ((if (priority == IslandPriority.FLASHLIGHT) 16.dp else 20.dp) * scale).coerceIn(14.dp, 24.dp)

fun islandCardVerticalPadding(priority: IslandPriority, scale: Float): Dp =
    ((if (priority == IslandPriority.FLASHLIGHT) 12.dp
        else if (priority == IslandPriority.POP_NOTIFICATION || priority == IslandPriority.PROGRESS) 18.dp
        else 20.dp) * scale).coerceIn(10.dp, 22.dp)

/**
 * Sizes of the expanded notification card. Shared by [ExpandedNotificationView] and
 * [measureNotificationCardHeight] so the card is always exactly as tall as its content.
 */
class NotificationCardMetrics(scale: Float) {
    val horizontalPadding = islandCardHorizontalPadding(IslandPriority.POP_NOTIFICATION, scale)
    val verticalPadding = islandCardVerticalPadding(IslandPriority.POP_NOTIFICATION, scale)
    val iconSize = (40.dp * scale).coerceIn(32.dp, 48.dp)
    val iconGap = (12.dp * scale).coerceIn(8.dp, 16.dp)
    val titleSize = (17 * scale).coerceIn(14f, 19f).sp
    val appNameSize = (13 * scale).coerceIn(11.5f, 15f).sp
    val subtitleSpacing = (IslandTokens.SpaceXxs * scale).coerceIn(1.dp, 3.dp)
    val bodySize = (15 * scale).coerceIn(13f, 17f).sp
    val bodyLineHeight = (20 * scale).coerceIn(17f, 22f).sp
    val bodyMaxLines = if (scale < 0.88f) 2 else 3
    val headerBodyGap = (IslandTokens.SpaceSm * scale).coerceIn(6.dp, 10.dp)
    val actionsGap = (IslandTokens.SpaceMd * scale).coerceIn(10.dp, 14.dp)
    val buttonHeight = (38.dp * scale).coerceIn(34.dp, 44.dp)
    val buttonFontSize = (13.5f * scale).coerceIn(12f, 15f).sp
    val replyChipFontSize = 12.sp
    val replyChipVerticalPadding = 5.dp
    val replyRowSpacing = 6.dp
    val replyInputHeight = 42.dp
}

/** Notification actions shown as buttons: "Open" is redundant since tapping the card opens it. */
fun visibleNotificationActions(notification: NotificationInfo?): List<NotificationAction> =
    (notification?.actions ?: emptyList())
        .filterNot { it.title.equals("Open", ignoreCase = true) }
        .take(2)

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
    val m = NotificationCardMetrics(scale)
    val innerWidth = (cardWidth - m.horizontalPadding * 2).coerceAtLeast(1.dp)

    fun textHeight(text: String, style: TextStyle, width: Dp, maxLines: Int): Dp =
        textMeasurer.measure(
            text = text,
            style = baseStyle.merge(style),
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            constraints = Constraints(maxWidth = width.roundToPx().coerceAtLeast(1))
        ).size.height.toDp()

    val titleHeight = textHeight(title, TextStyle(fontSize = m.titleSize, fontWeight = FontWeight.SemiBold), innerWidth, 1)
    val subtitleHeight = textHeight(
        notification?.appName ?: "",
        TextStyle(fontSize = m.appNameSize, fontWeight = FontWeight.Medium),
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
        val chipHeight = textHeight("OK", TextStyle(fontSize = m.replyChipFontSize, fontWeight = FontWeight.Medium), innerWidth, 1) +
            m.replyChipVerticalPadding * 2
        height += m.actionsGap + chipHeight + m.replyRowSpacing + m.replyInputHeight
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
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(base, countDown) {
        if (base <= 0L) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            // Wake right as the next whole second of the timer flips
            delay(1000L - Math.floorMod(now - base, 1000L))
        }
    }
    if (base <= 0L) return null
    val millis = if (countDown) base - now else now - base
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
    fontSize: TextUnit = IslandTokens.TypeCaption
) {
    if (items.isEmpty()) return
    val total = items.size

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(IslandTokens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val shape = when {
                total <= 1 -> RoundedCornerShape(IslandTokens.RadiusButton)
                index == 0 -> RoundedCornerShape(
                    topStart = IslandTokens.RadiusButton,
                    bottomStart = IslandTokens.RadiusButton,
                    topEnd = IslandTokens.SpaceXs,
                    bottomEnd = IslandTokens.SpaceXs
                )
                index == total - 1 -> RoundedCornerShape(
                    topStart = IslandTokens.SpaceXs,
                    bottomStart = IslandTokens.SpaceXs,
                    topEnd = IslandTokens.RadiusButton,
                    bottomEnd = IslandTokens.RadiusButton
                )
                else -> RoundedCornerShape(IslandTokens.SpaceXs)
            }

            FilledTonalButton(
                onClick = item.onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(height),
                shape = shape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (item.isPrimary) IslandTokens.ActionPrimaryContainer else IslandTokens.ActionSecondaryContainer,
                    contentColor = if (item.isPrimary) IslandTokens.ActionPrimaryContent else IslandTokens.TextBody
                )
            ) {
                Text(
                    text = item.title,
                    fontSize = fontSize,
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
    val latestArt by rememberUpdatedState(art)
    val scope = rememberCoroutineScope()

    LaunchedEffect(trackKey) {
        if (trackKey == shownKey) return@LaunchedEffect
        shownKey = trackKey
        // Separate scope so a late art update can't cancel the flip halfway through
        scope.launch {
            isTurningAway = true
            rotation.animateTo(90f, tween(durationMillis = 150, easing = FastOutLinearInEasing))
            shownArt = latestArt
            isTurningAway = false
            rotation.snapTo(-90f)
            rotation.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 380f))
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
    }
}

/** Geometry of the expanded card, animated as one 4D spring (see cardMorphAnimSpec). */
@Immutable
data class CardMorph(val width: Dp, val height: Dp, val corner: Dp, val top: Dp) {
    companion object {
        val VectorConverter = TwoWayConverter<CardMorph, AnimationVector4D>(
            convertToVector = { AnimationVector4D(it.width.value, it.height.value, it.corner.value, it.top.value) },
            convertFromVector = { CardMorph(it.v1.dp, it.v2.dp, it.v3.dp, it.v4.dp) }
        )

        // Stop the spring once it's within 0.1dp (same as Dp's default) instead of running extra frames
        val VisibilityThreshold = CardMorph(0.1.dp, 0.1.dp, 0.1.dp, 0.1.dp)
    }
}

/**
 * Blur for content that is fading in/out during a morph (iOS blurs content as the island
 * changes shape). Returns null once content is fully visible or hidden, so static frames
 * pay nothing; Android 12+ only, older versions just fade.
 */
fun morphBlurEffect(visibility: Float, maxRadiusPx: Float): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    if (visibility >= 0.99f || visibility <= 0.01f) return null
    val radius = (1f - visibility) * maxRadiusPx
    return if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
}
