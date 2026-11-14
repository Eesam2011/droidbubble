package com.droidbubble.app.island

import com.droidbubble.app.island.motion.MotionTween
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.filled.Air
import kotlinx.coroutines.flow.first
import com.droidbubble.app.model.allows
import com.droidbubble.app.island.IslandStateManager.RingerMode
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Call
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.rememberMotionTier
import com.droidbubble.app.island.shell.Follow
import com.droidbubble.app.island.shell.IslandShell
import androidx.compose.ui.platform.LocalWindowInfo
import com.droidbubble.app.island.shell.rememberWindowReady
import com.droidbubble.app.island.shell.rememberIslandShellState
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandInputs
import com.droidbubble.app.island.scene.IslandMode
import com.droidbubble.app.island.scene.MeasuredHeights
import com.droidbubble.app.island.scene.deriveIslandTargets
import com.droidbubble.app.island.scene.deriveIslandScene
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


private val blurredBitmapCache = java.util.WeakHashMap<Bitmap, Bitmap>()

fun createBlurredBitmap(src: Bitmap, blurRadius: Int = 14, scaleFraction: Float = 0.20f): Bitmap? {
    if (src.isRecycled) return null
    synchronized(blurredBitmapCache) {
        blurredBitmapCache[src]?.let { if (!it.isRecycled) return it }
    }
    return try {
        val w = (src.width * scaleFraction).toInt().coerceAtLeast(16)
        val h = (src.height * scaleFraction).toInt().coerceAtLeast(16)
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val mutable = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (scaled != src && scaled != mutable && !scaled.isRecycled) {
            scaled.recycle()
        }
        fastBoxBlur(mutable, blurRadius.coerceIn(3, 25))
        synchronized(blurredBitmapCache) {
            blurredBitmapCache[src] = mutable
        }
        mutable
    } catch (_: Throwable) {
        null
    }
}

private fun fastBoxBlur(bitmap: Bitmap, radius: Int) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val wm = width - 1
    val hm = height - 1
    val wh = width * height
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    val vmin = IntArray(maxOf(width, height))

    var yw = 0
    var yi = 0

    for (y in 0 until height) {
        var rsum = 0; var gsum = 0; var bsum = 0
        for (i in -radius..radius) {
            val p = pixels[yi + minOf(wm, maxOf(i, 0))]
            rsum += (p shr 16) and 0xff
            gsum += (p shr 8) and 0xff
            bsum += p and 0xff
        }
        for (x in 0 until width) {
            r[yi] = rsum / div
            g[yi] = gsum / div
            b[yi] = bsum / div

            if (y == 0) {
                vmin[x] = minOf(x + radius + 1, wm)
            }
            val p1 = pixels[yw + vmin[x]]
            val p2 = pixels[yw + maxOf(x - radius, 0)]

            rsum += ((p1 shr 16) and 0xff) - ((p2 shr 16) and 0xff)
            gsum += ((p1 shr 8) and 0xff) - ((p2 shr 8) and 0xff)
            bsum += (p1 and 0xff) - (p2 and 0xff)
            yi++
        }
        yw += width
    }

    for (x in 0 until width) {
        var rsum = 0; var gsum = 0; var bsum = 0
        for (i in -radius..radius) {
            yi = maxOf(0, minOf(hm, i)) * width + x
            rsum += r[yi]
            gsum += g[yi]
            bsum += b[yi]
        }
        yi = x
        for (y in 0 until height) {
            pixels[yi] = (0xff shl 24) or ((rsum / div) shl 16) or ((gsum / div) shl 8) or (bsum / div)
            if (x == 0) {
                vmin[y] = minOf(y + radius + 1, hm) * width
            }
            val p1 = x + vmin[y]
            val p2 = x + maxOf(y - radius, 0) * width

            rsum += r[p1] - r[p2]
            gsum += g[p1] - g[p2]
            bsum += b[p1] - b[p2]

            yi += width
        }
    }

    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
}

private val accentColorCache = java.util.WeakHashMap<Bitmap, Color>()

fun extractMediaAccentColor(bitmap: Bitmap?): Color {
    if (bitmap == null) return Color.Transparent
    synchronized(accentColorCache) {
        accentColorCache[bitmap]?.let { return it }
    }
    return try {
        // Sample with a small 16x16 scaled bitmap for ultra-fast, smooth calculation without JNI pixel bottlenecks
        val scaled = if (bitmap.width > 16 || bitmap.height > 16) {
            Bitmap.createScaledBitmap(bitmap, 16, 16, false)
        } else {
            bitmap
        }
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled != bitmap && !scaled.isRecycled) {
            scaled.recycle()
        }

        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0L

        for (pixel in pixels) {
            val a = (pixel shr 24) and 0xff
            if (a > 60) {
                redSum += (pixel shr 16) and 0xff
                greenSum += (pixel shr 8) and 0xff
                blueSum += pixel and 0xff
                count++
            }
        }

        val color = if (count > 0L) {
            val avgR = (redSum / count).toInt().coerceIn(0, 255)
            val avgG = (greenSum / count).toInt().coerceIn(0, 255)
            val avgB = (blueSum / count).toInt().coerceIn(0, 255)
            Color(avgR, avgG, avgB)
        } else {
            Color.Transparent
        }

        synchronized(accentColorCache) {
            accentColorCache[bitmap] = color
        }
        color
    } catch (_: Exception) {
        Color.Transparent
    }
}

/** Semantics tags for the island's touch targets (used by the crash tests). */
object IslandTestTags {
    const val PILL = "island_pill"
    const val BUBBLE = "island_bubble"
    const val CARD = "island_card"
    /** Card 2, the second activity's card in dual mode (not part of the S1 golden). */
    const val CARD2 = "island_card2"

    /** Content slots that tests can ask "which activity is drawn here?" (see [content]). */
    val CONTENT_SLOTS = listOf("pill", "bubble", "card1", "card2")

    /** Tag on the content root of [slot] while it shows [priority]. Semantics only: no layout or drawing. */
    fun content(slot: String, priority: IslandPriority?) = "island_${slot}_${priority?.name ?: "NONE"}"
    /** M3.3: one keyed child of a content swap; old and new are both present mid-transition. */
    fun swapContent(slot: String, priority: IslandPriority?) = "island_swap_${slot}_${priority?.name ?: "NONE"}"
}

/**
 * The island. Provides what every part of it shares (haptics, motion), then draws it.
 */
@Composable
fun IslandOverlayRoot(
    settings: IslandSettings
) {
    val view = LocalView.current
    // F3.3: every island haptic goes through here, and none fires with the haptics switch off
    val hapticsEnabled = rememberUpdatedState(settings.hapticsEnabled)
    val haptics = remember(view) { IslandHaptics(view, enabled = { hapticsEnabled.value }) }
    // F3.1: one motion object, rebuilt only when the speed, a curve or the motion tier (M5.1) changes
    val tier = rememberMotionTier(settings.reduceMotion)
    val motion = remember(
        settings.animationSpeed, settings.expandDamping, settings.expandStiffness,
        settings.appearDamping, settings.appearStiffness, settings.splitDamping, settings.splitStiffness, tier,
    ) { IslandMotion.from(settings, tier) }
    // F4.1: text sizes by role, for the card scale and the phone's font size
    val fontScale = LocalDensity.current.fontScale
    val type = remember(settings.expandedScale, fontScale) { IslandType(settings.expandedScale, fontScale) }
    CompositionLocalProvider(
        LocalIslandHaptics provides haptics,
        LocalIslandMotion provides motion,
        LocalIslandType provides type,
    ) {
        IslandShell(settings)
    }
}

/**
 * Long-press on the pill or the bubble (B6.3): opens the card's own target, like the body tap.
 * A notification is then treated as opened, so an auto-cancel one clears and others keep their card.
 */
fun openAppForPriority(
    context: android.content.Context,
    priority: IslandPriority?,
    media: MediaInfo?,
    notification: NotificationInfo?,
    progress: NotificationInfo?,
) {
    when (priority) {
        IslandPriority.POP_NOTIFICATION -> {
            if (notification != null) {
                openTarget(context, notification.contentIntent, notification.packageName)
                IslandStateManager.onNotificationOpened()
            }
        }
        IslandPriority.PROGRESS -> {
            if (progress != null) {
                openTarget(context, progress.contentIntent, progress.packageName)
            }
        }
        IslandPriority.MEDIA -> {
            if (media != null) {
                openTarget(context, media.sessionActivity, media.packageName)
            }
        }
        else -> {}
    }
}

@Composable
fun ParenthesisBracket(
    isLeft: Boolean,
    width: Dp,
    height: Dp,
    strokeWidth: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val strokePx = strokeWidth.toPx()
        val path = Path()

        if (isLeft) {
            // Left parenthesis `(`: curves outward to the left
            path.moveTo(w - strokePx / 2f, strokePx / 2f)
            path.cubicTo(
                x1 = strokePx / 2f, y1 = h * 0.22f,
                x2 = strokePx / 2f, y2 = h * 0.78f,
                x3 = w - strokePx / 2f, y3 = h - strokePx / 2f
            )
        } else {
            // Right parenthesis `)`: curves outward to the right
            path.moveTo(strokePx / 2f, strokePx / 2f)
            path.cubicTo(
                x1 = w - strokePx / 2f, y1 = h * 0.22f,
                x2 = w - strokePx / 2f, y2 = h * 0.78f,
                x3 = strokePx / 2f, y3 = h - strokePx / 2f
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun AuxiliaryBubbleContent(
    priority: IslandPriority?,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
) {
    // Keep showing the last activity while the bubble animates away, instead of a generic bell
    val notification = rememberLastNonNull(notification)
    when (priority) {
        IslandPriority.SCREEN_RECORDING -> {
            PulsingRecordingDot()
        }
        IslandPriority.POP_NOTIFICATION -> {
            val liveKind = liveActivityKind(notification)
            if (notification != null && liveKind == LiveActivityKind.RECORDING) {
                Icon(Icons.Rounded.Mic, contentDescription = "Recording", tint = LiveActivityColors.RecordingRed, modifier = Modifier.size(17.dp))
            } else if (notification != null && liveKind != LiveActivityKind.NONE) {
                LiveActivityCompactIcon(liveKind, notification)
            } else if (notification != null && notification.liveTimerBaseMs > 0L) {
                TimerClockIcon(
                    baseMs = notification.liveTimerBaseMs,
                    countDown = notification.liveTimerCountDown,
                    size = 18.dp,
                    color = IslandTokens.AccentOrange
                )
            } else if (notification?.avatarBitmap != null) {
                // The sender's photo fills the split bubble, with the same even ring as the pill
                CompactContactAvatar(notification = notification, centered = true)
            } else if (notification?.iconBitmap != null) {
                Image(
                    bitmap = notification.iconBitmap.asImageBitmap(),
                    contentDescription = "Notification Icon",
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IslandPriority.PROGRESS -> {
            val pct = notification?.progressFraction ?: 0f
            val isIndet = notification?.isIndeterminate == true
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isIndet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = IslandTokens.ControlLight,
                        trackColor = IslandTokens.ControlTrack
                    )
                } else {
                    // B9.2: a readable numeral (11 sp bold, never smaller) in a ring sized by the
                    // bubble; when the number doesn't fit at this font scale, the ring shows alone
                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                        val ring = bubbleRingDiameter(minOf(maxWidth, maxHeight))
                        val label = "${(pct * 100).roundToInt()}"
                        val numeralStyle = LocalTextStyle.current.merge(IslandType.BubbleNumeral.copy(color = Color.White))
                        val measurer = rememberTextMeasurer()
                        val density = LocalDensity.current
                        val labelWidth = with(density) { measurer.measure(label, numeralStyle).size.width.toDp() }
                        // Digits are about 0.72 em tall: the box that must sit inside the circle
                        val digitHeight = with(density) { IslandType.BubbleValue.toDp() } * 0.72f
                        CircularProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.size(ring),
                            strokeWidth = BUBBLE_RING_STROKE,
                            color = IslandTokens.ControlLight,
                            trackColor = IslandTokens.ControlTrack
                        )
                        if (bubbleNumeralFits(labelWidth, digitHeight, ring)) {
                            Text(text = label, style = numeralStyle, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
        IslandPriority.MEDIA -> {
            val displayArt = media?.albumArt ?: media?.appIcon
            if (displayArt != null) {
                FlippingCoverArt(
                    art = displayArt,
                    trackKey = mediaTrackKey(media),
                    contentDescription = "Media Art",
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                )
            } else {
                EqualizerVisualizer(isPlaying = media?.isPlaying == true)
            }
        }
        IslandPriority.FLASHLIGHT -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        else -> Unit
    }
}

/**
 * A contact photo in a compact slot, as large as its space allows. Its two limits are the pill's
 * outer edge and the camera gap: it fills the pill's rounded end with an even ring of black all
 * round (concentric with the end cap) and never crosses into the camera gap. Both limits set
 * one diameter and the photo is always laid out square, so when the pill narrows during a morph
 * it shrinks as a circle instead of being squeezed into an oval (a plain `size()` gave way only
 * in width).
 *
 * [edgeReach] is the row padding between the slot and the pill's edge, which the photo may use.
 * [centered] fills a round bubble instead of the pill's leading end.
 */
@Composable
private fun CompactContactAvatar(
    notification: NotificationInfo,
    edgeReach: Dp = 0.dp,
    centered: Boolean = false,
    maxDiameter: Dp = Dp.Infinity
) {
    BoxWithConstraints(contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
        val slotHeight = if (constraints.hasBoundedHeight) maxHeight else 30.dp
        val ring = (slotHeight * 0.13f).coerceIn(3.dp, 6.dp)
        val reach = if (centered) 0.dp else (edgeReach - ring).coerceAtLeast(0.dp)
        val slotWidth = if (!constraints.hasBoundedWidth) slotHeight else if (centered) maxWidth - ring * 2 else maxWidth + reach
        val diameter = minOf(slotHeight - ring * 2, slotWidth, maxDiameter).coerceAtLeast(0.dp)
        NotificationAvatar(
            notification = notification,
            size = diameter,
            showAppBadge = false,
            modifier = Modifier
                .offset(x = -reach)
                .wrapContentWidth(if (centered) Alignment.CenterHorizontally else Alignment.Start, unbounded = true)
                .requiredSize(diameter)
        )
    }
}

@Composable
fun CompactContent(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    isCompactDual: Boolean = false,
    flashlightLevel: Int = 1,
    maxFlashlightLevel: Int = 1,
    secondaryPriority: IslandPriority? = null,
    horizontalInset: Dp = 10.dp
) {
    // Keep showing the last activity while the pill animates away, instead of a generic bell
    val notification = rememberLastNonNull(notification)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isCompactDual) IslandTokens.SpaceSm else horizontalInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Leading content: Pinned strictly to the left of the camera punch hole
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            when (priority) {
                IslandPriority.CALL -> Icon(
                    Icons.Default.Call, contentDescription = "Active call",
                    tint = LiveActivityColors.CallGreen, modifier = Modifier.size(18.dp)
                )
                IslandPriority.SCREEN_RECORDING -> {
                    PulsingRecordingDot()
                }

                IslandPriority.FLASHLIGHT -> {
                    Box(
                        modifier = Modifier.offset(x = (-4).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.50f),
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                IslandPriority.POP_NOTIFICATION -> {
                    val liveKind = liveActivityKind(notification)
                    if (notification != null && liveKind != LiveActivityKind.NONE) {
                        LiveActivityCompactIcon(liveKind, notification)
                    } else if (notification != null && notification.liveTimerBaseMs > 0L) {
                        TimerClockIcon(
                            baseMs = notification.liveTimerBaseMs,
                            countDown = notification.liveTimerCountDown,
                            size = 18.dp,
                            color = IslandTokens.AccentOrange
                        )
                    } else if (notification?.avatarBitmap != null || notification?.iconBitmap != null) {
                        CompactContactAvatar(
                            notification = notification,
                            edgeReach = if (isCompactDual) IslandTokens.SpaceSm else horizontalInset,
                            // A contact photo fills the pill's end; a bare app icon stays icon-sized
                            maxDiameter = if (notification.avatarBitmap != null) Dp.Infinity else 20.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = IslandTokens.ControlLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IslandPriority.PROGRESS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (notification?.iconBitmap != null) {
                            Image(
                                bitmap = notification.iconBitmap.asImageBitmap(),
                                contentDescription = "Download Icon",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = IslandTokens.ControlLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = notification?.appName ?: "Download",
                                color = IslandTokens.ControlLight,
                                fontSize = IslandType.CompactLabel,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IslandPriority.MEDIA -> {
                    FlippingCoverArt(
                        art = media?.albumArt ?: media?.appIcon,
                        trackKey = mediaTrackKey(media),
                        contentDescription = "Media Art",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        placeholder = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = IslandTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }



                IslandPriority.IDLE -> Unit
            }
        }

        // Camera punch-hole deadzone: STRICTLY 36dp EMPTY SPACE IN THE MIDDLE!
        // No UI elements will EVER enter or overlap the camera cutout area
        Spacer(modifier = Modifier.width(IslandTokens.Shell.CameraZone))

        // Trailing content: Pinned strictly to the right of the camera punch hole
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (priority) {
                IslandPriority.CALL -> {
                    val call by IslandStateManager.activeCallState.collectAsState()
                    RollingNumberText(
                        text = String.format(Locale.US, "%02d:%02d", call.durationSeconds / 60, call.durationSeconds % 60),
                        color = LiveActivityColors.CallGreen,
                        fontSize = IslandType.CompactLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IslandPriority.SCREEN_RECORDING -> {
                    val minutes = recording.durationSeconds / 60
                    val seconds = recording.durationSeconds % 60
                    RollingNumberText(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        color = LiveActivityColors.RecordingRed,
                        fontSize = IslandType.CompactLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.FLASHLIGHT -> Unit

                IslandPriority.POP_NOTIFICATION -> {
                    val liveKind = liveActivityKind(notification)
                    val isTimedKind = liveKind != LiveActivityKind.NONE && liveKind != LiveActivityKind.CALL
                    // Rides show the ETA, scores the score, alerts "Time's up" / the alarm time
                    val liveTimer = notification?.let { liveCompactText(it, liveKind) }
                        ?: rememberLiveTimerText(notification)
                        ?: notification?.takeIf { isTimedKind }?.let { staticLiveTimeText(it) }
                    // Recorder red, timer blue, call timer green; names and alerts stay white.
                    // Live values roll digit by digit with steady-width numbers.
                    RollingNumberText(
                        text = liveTimer ?: notification?.title ?: "Notification",
                        color = if (liveKind != LiveActivityKind.NONE && liveTimer != null) LiveActivityColors.accentFor(liveKind) else IslandTokens.ControlLight,
                        fontSize = IslandType.CompactLabel,
                        fontWeight = FontWeight.SemiBold,
                        countDown = notification?.liveTimerCountDown == true || liveKind == LiveActivityKind.TRACKING
                    )
                }

                IslandPriority.PROGRESS -> {
                    val pct = notification?.progressFraction ?: 0f
                    val isIndet = notification?.isIndeterminate == true
                    if (isIndet) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = IslandTokens.ControlLight,
                            trackColor = IslandTokens.ControlTrack
                        )
                    } else {
                        Text(
                            text = "${(pct * 100).roundToInt()}%",
                            color = IslandTokens.ControlLight,
                            fontSize = IslandType.CompactLabel,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IslandPriority.MEDIA -> {
                    val displayArt = media?.albumArt ?: media?.appIcon
                    val compactAccent = remember(displayArt) { extractMediaAccentColor(displayArt) }
                    val compactEqTint = if (compactAccent != Color.Transparent) {
                        lerp(IslandTokens.ControlLight, compactAccent, 0.50f)
                    } else {
                        IslandTokens.ControlLight
                    }
                    EqualizerVisualizer(isPlaying = media?.isPlaying == true, barColor = compactEqTint)
                }



                IslandPriority.IDLE -> Unit
            }
        }
    }
}

@Composable
fun ExpandedCardContent(
    priority: IslandPriority,
    media: MediaInfo?,
    notification: NotificationInfo?,
    recording: ScreenRecordingState,
    flashlightLevel: Int = 1,
    maxFlashlightLevel: Int = 1,
    scale: Float = 1.0f,
    onCollapse: () -> Unit
) {
    when (priority) {
        IslandPriority.CALL -> {
            val call by IslandStateManager.activeCallState.collectAsState()
            Text(
                text = call.callerName.ifBlank { "Active call" },
                color = IslandTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IslandPriority.SCREEN_RECORDING -> {
            ExpandedRecordingView(recording, onCollapse, scale)
        }
        IslandPriority.POP_NOTIFICATION -> {
            ExpandedNotificationView(notification, onCollapse, scale)
        }
        IslandPriority.PROGRESS -> {
            ExpandedProgressView(notification, onCollapse, scale)
        }
        IslandPriority.MEDIA -> {
            ExpandedMediaView(media, onCollapse, scale)
        }
        IslandPriority.FLASHLIGHT -> {
            ExpandedFlashlightView(
                level = flashlightLevel,
                onSetLevel = { newLevel ->
                    IslandStateManager.setFlashlightBrightness(newLevel)
                },
                onTurnOff = {
                    IslandStateManager.turnOffFlashlight()
                },
                onCollapse = onCollapse,
                scale = scale
            )
        }
        IslandPriority.IDLE -> {
            LaunchedEffect(Unit) {
                onCollapse()
            }
            ExpandedIdleView(onCollapse, scale)
        }
    }
}

@Composable
fun ExpandedFlashlightView(
    level: Int,
    onSetLevel: (Int) -> Unit,
    onTurnOff: () -> Unit,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val haptics = LocalIslandHaptics.current

    // Current brightness level 1..4
    var currentLevel by remember(level) { mutableIntStateOf(level.coerceIn(1, 4)) }
    LaunchedEffect(level) {
        currentLevel = level.coerceIn(1, 4)
    }

    val currentLevelState = rememberUpdatedState(currentLevel)
    val onSetLevelState = rememberUpdatedState(onSetLevel)

    val motion = LocalIslandMotion.current
    // Animated fraction for the light beam height (1 -> 0.25f, 4 -> 1.0f)
    val targetFraction = (currentLevel / 4f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = motion.spring(motion.flashlightBeam),
        label = "flashlightBeamFraction"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.FLASHLIGHT, scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper section: The light-beam triangular blur vertical slider
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val h = size.height.toFloat()
                        if (h > 0) {
                            val relY = (down.position.y / h).coerceIn(0f, 1f)
                            val newLevel = (((1f - relY) * 3.999f).toInt() + 1).coerceIn(1, 4)
                            if (newLevel != currentLevelState.value) {
                                currentLevel = newLevel
                                haptics.perform(IslandHapticEvent.SEGMENT_TICK)
                                onSetLevelState.value(newLevel)
                            }
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            val currentH = size.height.toFloat()
                            if (currentH > 0) {
                                val relY = (change.position.y / currentH).coerceIn(0f, 1f)
                                val newLevel = (((1f - relY) * 3.999f).toInt() + 1).coerceIn(1, 4)
                                if (newLevel != currentLevelState.value) {
                                    currentLevel = newLevel
                                    haptics.perform(IslandHapticEvent.SEGMENT_TICK)
                                    onSetLevelState.value(newLevel)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, bottom = 0.dp)
                    .blur(radius = 8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                val w = size.width
                val h = size.height
                val centerX = w / 2f

                val blurpleTint = IslandTokens.FlashlightTint

                // Geometry:
                // Maximum widths at full brightness (level 4):
                val maxSecTopHalfW = (w * 0.18f).coerceAtMost(52.dp.toPx())
                val secBaseHalfW = 7.dp.toPx()

                val maxPrimTopHalfW = (w * 0.09f).coerceAtMost(26.dp.toPx())
                val primBaseHalfW = 3.5.dp.toPx()

                // Width expands dynamically with brightness, maxing out at current width for level 4!
                val widthFactor = 0.35f + 0.65f * animatedFraction
                val currentSecTopHalfW = maxSecTopHalfW * widthFactor
                val currentPrimTopHalfW = maxPrimTopHalfW * widthFactor

                // 1. Background guide cone: subtle blurple tint, fades away at the top!
                val guideOuterPath = Path().apply {
                    moveTo(centerX - secBaseHalfW - 3.dp.toPx(), h)
                    lineTo(centerX - maxSecTopHalfW - 6.dp.toPx(), 0f)
                    quadraticTo(centerX, -4.dp.toPx(), centerX + maxSecTopHalfW + 6.dp.toPx(), 0f)
                    lineTo(centerX + secBaseHalfW + 3.dp.toPx(), h)
                    close()
                }
                drawPath(
                    path = guideOuterPath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent, // Fades away at the top!
                            0.20f to blurpleTint.copy(alpha = 0.03f),
                            0.60f to blurpleTint.copy(alpha = 0.05f),
                            1.0f to Color.Transparent
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                val guidePath = Path().apply {
                    moveTo(centerX - secBaseHalfW, h)
                    lineTo(centerX - maxSecTopHalfW, 0f)
                    quadraticTo(centerX, -4.dp.toPx(), centerX + maxSecTopHalfW, 0f)
                    lineTo(centerX + secBaseHalfW, h)
                    close()
                }
                drawPath(
                    path = guidePath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent, // Fades away at the top!
                            0.18f to blurpleTint.copy(alpha = 0.08f),
                            0.55f to blurpleTint.copy(alpha = 0.13f),
                            1.0f to blurpleTint.copy(alpha = 0.04f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                // 2. Active illuminated beams filling up to animatedFraction (1..4)
                val activeH = h * animatedFraction
                val activeTopY = h - activeH
                val activeSecTopHalfW = secBaseHalfW + (currentSecTopHalfW - secBaseHalfW) * animatedFraction
                val activePrimTopHalfW = primBaseHalfW + (currentPrimTopHalfW - primBaseHalfW) * animatedFraction

                // Dynamic fade distance at top: fades away at top, but fade reduces when less bright
                val fadeLength = (28.dp.toPx() * animatedFraction).coerceAtMost(activeH * 0.40f).coerceAtLeast(4.dp.toPx())
                val fadeStop = (fadeLength / activeH).coerceIn(0.04f, 0.45f)

                // Diffused outer atmospheric halo
                val haloSpread = 5.dp.toPx() * animatedFraction
                val haloPath = Path().apply {
                    moveTo(centerX - secBaseHalfW - 2.dp.toPx(), h)
                    lineTo(centerX - activeSecTopHalfW - haloSpread, activeTopY)
                    quadraticTo(centerX, activeTopY - 4.dp.toPx(), centerX + activeSecTopHalfW + haloSpread, activeTopY)
                    lineTo(centerX + secBaseHalfW + 2.dp.toPx(), h)
                    close()
                }
                drawPath(
                    path = haloPath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            fadeStop to Color.White.copy(alpha = 0.12f * animatedFraction),
                            1.0f to Color.Transparent
                        ),
                        startY = activeTopY,
                        endY = h
                    )
                )

                // 3. Secondary beam: slightly less bright, same width as now (realistic spill beam)
                val secBeamPath = Path().apply {
                    moveTo(centerX - secBaseHalfW, h)
                    lineTo(centerX - activeSecTopHalfW, activeTopY)
                    quadraticTo(centerX, activeTopY - 3.dp.toPx(), centerX + activeSecTopHalfW, activeTopY)
                    lineTo(centerX + secBaseHalfW, h)
                    close()
                }
                val secIntensity = 0.32f + 0.22f * animatedFraction
                drawPath(
                    path = secBeamPath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent, // Fades away at the top!
                            fadeStop to Color.White.copy(alpha = 0.35f * secIntensity),
                            0.60f to Color.White.copy(alpha = 0.24f * secIntensity),
                            1.0f to Color.White.copy(alpha = 0.08f * secIntensity)
                        ),
                        startY = activeTopY,
                        endY = h
                    )
                )

                // 4. Primary core beam: half as thin again, brilliant hotspot, fades away at the top
                val primBeamPath = Path().apply {
                    moveTo(centerX - primBaseHalfW, h)
                    lineTo(centerX - activePrimTopHalfW, activeTopY)
                    quadraticTo(centerX, activeTopY - 2.dp.toPx(), centerX + activePrimTopHalfW, activeTopY)
                    lineTo(centerX + primBaseHalfW, h)
                    close()
                }
                val primIntensity = 0.70f + 0.30f * animatedFraction
                drawPath(
                    path = primBeamPath,
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent, // Fades away at the top!
                            fadeStop to Color.White.copy(alpha = 0.96f * primIntensity),
                            0.65f to Color.White.copy(alpha = 0.78f * primIntensity),
                            1.0f to Color.White.copy(alpha = 0.45f * primIntensity)
                        ),
                        startY = activeTopY,
                        endY = h
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom section: Large centered flashlight icon (NO dark circle background, NO radial glow)
        // Tapping on this icon turns the flashlight off!
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.perform(IslandHapticEvent.TOGGLE_OFF)
                    onTurnOff()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOn,
                contentDescription = "Turn off flashlight",
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedMediaView(media: MediaInfo?, onCollapse: () -> Unit, scale: Float = 1.0f) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var isSeekPending by remember { mutableStateOf(false) }

    val rawDuration = media?.durationMs ?: 0L
    val hasValidDuration = rawDuration > 0L
    val duration = if (hasValidDuration) rawDuration else 0L

    RecompositionProbe.onMediaCard?.invoke()

    // B8.2: the position is held outside snapshot state and pushed into the slider each frame
    // while playing, which re-lays out the slider only. The card recomposes once a second, for
    // the time label (secondTick), instead of on every frame.
    val livePosition = remember(media?.packageName, media?.title) { longArrayOf(media?.computeCurrentPosition() ?: 0L) }
    var secondTick by remember(media?.packageName, media?.title) { mutableLongStateOf(livePosition[0] / 1000L) }
    val sliderState = remember { SliderState(value = 0f) }

    // Players re-report their position with small clock drift; accepting tiny backward steps
    // made the time and bar twitch. Only real jumps (> 1.5s, i.e. a seek) may go backwards.
    fun stablePosition(previous: Long): Long {
        val fresh = media?.computeCurrentPosition() ?: 0L
        return if (media?.isPlaying == true && fresh < previous && previous - fresh < 1500L) previous else fresh
    }

    fun showPosition(positionMs: Long) {
        livePosition[0] = positionMs
        if (hasValidDuration) sliderState.value = (positionMs.toFloat() / duration).coerceIn(0f, 1f)
        val second = positionMs / 1000L
        if (second != secondTick) secondTick = second
    }

    LaunchedEffect(media?.isPlaying, media?.positionMs, media?.lastPositionUpdateTimeMs, isSeeking, isSeekPending, duration) {
        if (isSeeking || isSeekPending) return@LaunchedEffect
        showPosition(stablePosition(livePosition[0]))
        if (media?.isPlaying == true) {
            while (isActive) {
                // The infinite-animation variant: an endless loop that idle waits (tests) don't wait on
                withInfiniteAnimationFrameMillis { showPosition(stablePosition(livePosition[0])) }
            }
        }
    }

    val latestMedia by rememberUpdatedState(media)

    val currentPosition = when {
        isSeeking || isSeekPending -> if (hasValidDuration) (scrubFraction * duration).toLong().coerceIn(0L, duration) else secondTick * 1000L
        else -> secondTick * 1000L
    }

    val displayArt = media?.albumArt ?: media?.appIcon
    val mediaAccentColor = remember(displayArt) { extractMediaAccentColor(displayArt) }

    val controlsTint = if (mediaAccentColor != Color.Transparent) {
        lerp(IslandTokens.ControlLight, mediaAccentColor, 0.40f)
    } else {
        IslandTokens.ControlLight
    }

    val playButtonBg = if (mediaAccentColor != Color.Transparent) {
        lerp(Color.White, mediaAccentColor, 0.28f)
    } else {
        Color.White
    }

    val playButtonIconTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color.Black, mediaAccentColor, 0.15f)
    } else {
        Color.Black
    }

    val eqTint = if (mediaAccentColor != Color.Transparent) {
        lerp(IslandTokens.ControlLight, mediaAccentColor, 0.50f)
    } else {
        IslandTokens.ControlLight
    }

    val sliderActiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(IslandTokens.ControlLight, mediaAccentColor, 0.55f)
    } else {
        IslandTokens.ControlLight
    }

    val sliderInactiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(IslandTokens.ControlTrack, mediaAccentColor, 0.22f)
    } else {
        IslandTokens.ControlTrack
    }

    val durationTextTint = if (mediaAccentColor != Color.Transparent) {
        lerp(IslandTokens.TextTertiary, mediaAccentColor, 0.35f)
    } else {
        IslandTokens.TextTertiary
    }

    // Time labels use tabular digits and a fixed width sized for the longest possible value,
    // so the seek bar keeps its length while the time ticks or is scrubbed.
    val type = rememberIslandType(scale)
    val timeLabelStyle = LocalTextStyle.current.merge(type.timeLabel)
    // F4.4: both labels sit in a NumericSlot as wide as the track's full length
    val timeLabelTemplate = formatDuration(duration)
    val timeLabelAlsoFits = remember { listOf("--:--") }

    val motion = LocalIslandMotion.current
    val thumbWidth by animateDpAsState(
        targetValue = if (isSeeking) 5.dp else 3.dp,
        animationSpec = motion.spring(motion.seekThumb),
        label = "seekThumbWidth"
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (isSeeking) 18.dp else 14.dp,
        animationSpec = motion.spring(motion.seekThumb),
        label = "seekThumbHeight"
    )

    val artSize = (54.dp * scale).coerceIn(40.dp, 64.dp)
    val titleSize = type.title(IslandType.Card.MEDIA)
    val artistSize = type.secondary
    val sliderGapTop = (IslandTokens.SpaceSm * scale).coerceIn(4.dp, 10.dp)
    val sliderGapBottom = (IslandTokens.SpaceXs * scale).coerceIn(2.dp, 8.dp)
    val prevNextSize = (42.dp * scale).coerceIn(34.dp, 48.dp)
    val prevNextIcon = (24.dp * scale).coerceIn(18.dp, 28.dp)
    val playBtnSize = (50.dp * scale).coerceIn(40.dp, 58.dp)
    val playBtnIcon = (30.dp * scale).coerceIn(22.dp, 36.dp)
    val controlsGap = (20.dp * scale).coerceIn(12.dp, 26.dp)
    val sideControlSize = (36.dp * scale).coerceIn(30.dp, 42.dp)
    val sideControlIcon = (20.dp * scale).coerceIn(16.dp, 24.dp)
    val sideControlGap = (10.dp * scale).coerceIn(6.dp, 14.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.MEDIA, scale),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header: Album cover / app icon + Title & Artist + Equalizer
        IslandHeader(
            title = media?.title?.ifBlank { "No Media Playing" } ?: "No Media Playing",
            subtitle = media?.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
            titleSize = titleSize,
            subtitleSize = artistSize,
            subtitleSpacing = 3.dp,
            titleMarquee = true,
            icon = {
                // Album cover (larger, tap to open app and collapse island); flips on song change
                FlippingCoverArt(
                    art = displayArt,
                    trackKey = mediaTrackKey(media),
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(IslandTokens.RadiusChip))
                        .background(IslandTokens.SurfaceMuted)
                        .clickable {
                            openTarget(context, media?.sessionActivity, media?.packageName)
                            onCollapse()
                        },
                    placeholder = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = IslandTokens.TextSecondary,
                            modifier = Modifier.size((28.dp * scale).coerceIn(20.dp, 32.dp))
                        )
                    }
                )
            },
            trailing = {
                Spacer(modifier = Modifier.width(IslandTokens.SpaceMd))
                EqualizerVisualizer(isPlaying = media?.isPlaying == true, barColor = eqTint)
            }
        )

        Spacer(modifier = Modifier.height(sliderGapTop))

        // Progress Bar & Duration on Left & Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = IslandTokens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumericSlot(
                text = formatDuration(currentPosition),
                style = timeLabelStyle,
                template = timeLabelTemplate,
                align = Alignment.Start,
                color = durationTextTint,
                alsoFits = timeLabelAlsoFits,
            )

            Spacer(modifier = Modifier.width(IslandTokens.SpaceMd))

            sliderState.onValueChange = { frac ->
                isSeeking = true
                scrubFraction = frac
                sliderState.value = frac
            }
            sliderState.onValueChangeFinished = {
                    if (hasValidDuration) {
                        val seekTargetMs = (scrubFraction * duration).toLong().coerceIn(0L, duration)
                        // Until the player confirms the seek, keep showing where the user let go
                        showPosition(seekTargetMs)
                        isSeekPending = true
                        isSeeking = false
                        IslandStateManager.onSeekTo?.invoke(seekTargetMs)
                        coroutineScope.launch {
                            // Players often send one more update with the OLD position before the
                            // seek lands, which made the bar jump back and then forward. Hold the
                            // released position until the reported one matches (max 2.5s).
                            val startedAt = android.os.SystemClock.uptimeMillis()
                            while (true) {
                                kotlinx.coroutines.delay(100)
                                val elapsed = android.os.SystemClock.uptimeMillis() - startedAt
                                val expected = seekTargetMs + if (latestMedia?.isPlaying == true) elapsed else 0L
                                val reported = latestMedia?.computeCurrentPosition() ?: break
                                if (elapsed >= 500L && abs(reported - expected) < 1500L) break
                                if (elapsed >= 2500L) break
                            }
                            showPosition(latestMedia?.computeCurrentPosition() ?: seekTargetMs)
                            isSeekPending = false
                        }
                    } else {
                        isSeeking = false
                    }
            }
            Slider(
                state = sliderState,
                enabled = hasValidDuration,
                thumb = {
                    if (hasValidDuration) {
                        Box(
                            modifier = Modifier
                                .width(thumbWidth)
                                .height(thumbHeight)
                                .clip(RoundedCornerShape(thumbWidth / 2))
                                .background(sliderActiveTint)
                        )
                    }
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        drawStopIndicator = null,
                        drawTick = { _, _ -> },
                        colors = SliderDefaults.colors(
                            activeTrackColor = sliderActiveTint,
                            inactiveTrackColor = sliderInactiveTint,
                            disabledActiveTrackColor = sliderInactiveTint.copy(alpha = 0.5f),
                            disabledInactiveTrackColor = sliderInactiveTint.copy(alpha = 0.3f)
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
            )

            Spacer(modifier = Modifier.width(IslandTokens.SpaceMd))

            NumericSlot(
                text = if (hasValidDuration) formatDuration(duration) else "--:--",
                style = timeLabelStyle,
                template = timeLabelTemplate,
                align = Alignment.End,
                color = durationTextTint,
                alsoFits = timeLabelAlsoFits,
            )
        }

        Spacer(modifier = Modifier.height(sliderGapBottom))

        // Playback Controls (moved upward, no big gap)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Centered Controls: Previous, Play/Pause, Next
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle: highlighted when on
                IconButton(
                    onClick = { IslandStateManager.onToggleShuffle?.invoke() },
                    modifier = Modifier.size(sideControlSize)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (media?.isShuffleOn == true) sliderActiveTint else controlsTint.copy(alpha = 0.45f),
                        modifier = Modifier.size(sideControlIcon)
                    )
                }

                Spacer(modifier = Modifier.width(sideControlGap))

                IconButton(
                    onClick = { IslandStateManager.onSkipPrevious?.invoke() },
                    modifier = Modifier.size(prevNextSize)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = controlsTint,
                        modifier = Modifier.size(prevNextIcon)
                    )
                }

                Spacer(modifier = Modifier.width(controlsGap))

                IconButton(
                    onClick = { IslandStateManager.onPlayPause?.invoke() },
                    modifier = Modifier
                        .size(playBtnSize)
                        .clip(CircleShape)
                        .background(playButtonBg)
                ) {
                    AnimatedContent(
                        targetState = media?.isPlaying == true,
                        transitionSpec = {
                            (fadeIn(motion.tween(MotionTween.PLAY_PAUSE_IN)) +
                                scaleIn(initialScale = 0.6f, animationSpec = motion.spring(motion.playPauseScale))) togetherWith
                                (fadeOut(motion.tween(MotionTween.PLAY_PAUSE_OUT)) +
                                    scaleOut(targetScale = 0.6f, animationSpec = motion.tween(MotionTween.PLAY_PAUSE_OUT)))
                        },
                        label = "playPauseIcon"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = playButtonIconTint,
                            modifier = Modifier.size(playBtnIcon)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(controlsGap))

                IconButton(
                    onClick = { IslandStateManager.onSkipNext?.invoke() },
                    modifier = Modifier.size(prevNextSize)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = controlsTint,
                        modifier = Modifier.size(prevNextIcon)
                    )
                }

                Spacer(modifier = Modifier.width(sideControlGap))

                // Repeat: off -> all -> one
                IconButton(
                    onClick = { IslandStateManager.onCycleRepeat?.invoke() },
                    modifier = Modifier.size(sideControlSize)
                ) {
                    Icon(
                        imageVector = if (media?.repeatMode == MediaInfo.REPEAT_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if ((media?.repeatMode ?: MediaInfo.REPEAT_OFF) != MediaInfo.REPEAT_OFF) sliderActiveTint else controlsTint.copy(alpha = 0.45f),
                        modifier = Modifier.size(sideControlIcon)
                    )
                }
            }


        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedNotificationView(notification: NotificationInfo?, onCollapse: () -> Unit, scale: Float = 1.0f) {
    // Native one-row designs for calls, voice recorders, timers and stopwatches
    val liveKind = liveActivityKind(notification)
    if (notification != null && liveKind == LiveActivityKind.CALL) {
        LiveCallView(notification, onCollapse, scale)
        return
    }
    if (notification != null && liveKind != LiveActivityKind.NONE) {
        LiveActivityNotificationView(notification, liveKind, onCollapse, scale)
        return
    }
    val context = LocalContext.current
    var isReplying by remember(notification?.key) { mutableStateOf(false) }
    var replyText by remember(notification?.key) { mutableStateOf("") }
    val replyFocusRequester = remember { FocusRequester() }
    var replyFieldHadFocus by remember(notification?.key) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Keyboard closed with Back / swipe while replying: release focus so the island can
    // auto-collapse again and stops taking the Back key from the app underneath
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible && isReplying) {
            imeWasVisible = false
            focusManager.clearFocus()
            IslandStateManager.setKeyboardFocused(false)
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isReplying) {
        if (isReplying) {
            // The island window only becomes focusable once the service reacts to the keyboard
            // flag, so wait a moment, then focus the field and raise the keyboard
            delay(180)
            runCatching { replyFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    val replyAction = remember(notification) {
        notification?.actions?.firstOrNull { it.isReply || it.title.contains("Reply", ignoreCase = true) }
    }

    fun openNotification() {
        IslandStateManager.setKeyboardFocused(false)
        openTarget(context, notification?.contentIntent, notification?.packageName)
        IslandStateManager.onNotificationOpened()
        onCollapse()
    }

    fun sendReply(text: String) {
        if (text.isBlank() || notification == null) return
        IslandStateManager.onSendReply?.invoke(notification.key, text.trim())
        IslandStateManager.setKeyboardFocused(false)
        isReplying = false
        // Folds back to the pill; the card itself stays until the notification is cleared
        onCollapse()
    }

    val metrics = remember(scale) { NotificationCardMetrics(scale) }
    val iconSize = metrics.iconSize
    val iconGap = metrics.iconGap
    val liveTimer = rememberLiveTimerText(notification)
    val displayTitle = notificationDisplayTitle(notification, liveTimer)

    // Laid out top-down; the card height is measured from this same content
    // (measureNotificationCardHeight), so there is no leftover gap and buttons never squeeze.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.POP_NOTIFICATION, scale)
    ) {
        // Top section: Header + Body text
        Column(modifier = Modifier.fillMaxWidth()) {
            IslandHeader(
                title = displayTitle,
                subtitle = notification?.appName ?: "",
                titleSize = metrics.titleSize,
                subtitleSize = metrics.appNameSize,
                subtitleSpacing = metrics.subtitleSpacing,
                iconSpacing = iconGap,
                titleMarquee = true,
                // A live timer that isn't already the title (e.g. a call) shows as a chip
                trailing = if (liveTimer != null && displayTitle != liveTimer) {
                    { InfoChip(text = liveTimer, dotColor = IslandTokens.AccentRed) }
                } else null,
                // Dismiss X removed: swipe the card away or tap outside to collapse.
                // Tapping the icon or body already opens the notification.
                icon = {
                    // Sender picture (WhatsApp contact, SMS sender...) with a small app badge,
                    // or the app icon for regular alerts. Tap to open.
                    NotificationAvatar(
                        notification = notification,
                        size = iconSize,
                        modifier = Modifier.clickable {
                            openTarget(context, notification?.contentIntent, notification?.packageName)
                            IslandStateManager.onNotificationOpened()
                            onCollapse()
                        },
                        fallback = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = IslandTokens.ActionPrimaryContainer,
                                modifier = Modifier.size((24.dp * scale).coerceIn(18.dp, 28.dp))
                            )
                        }
                    )
                }
            )

            if (!notification?.text.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(metrics.headerBodyGap))

                Text(
                    text = notification?.text ?: "",
                    color = IslandTokens.TextBody,
                    fontSize = metrics.bodySize,
                    lineHeight = metrics.bodyLineHeight,
                    fontWeight = FontWeight.Normal,
                    maxLines = metrics.bodyMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = iconSize + iconGap)
                        .clickable { openNotification() }
                )
            }
        }

        // Action Section: Either Inline Reply Row or Action Buttons
        if (isReplying) {
            Spacer(modifier = Modifier.height(metrics.actionsGap))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(metrics.replyRowSpacing)
            ) {
                // B6.1: smart reply chips only when the app supplied them (RemoteInput choices). The
                // card used to invent "OK", "Sure" and "Thanks!", which sent on one tap.
                val smartChoices = remember(notification) { replyChoices(notification) }

                if (smartChoices.isNotEmpty()) Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    smartChoices.take(3).forEach { choice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(IslandTokens.SurfaceRaised)
                                .clickable { sendReply(choice) }
                                .padding(horizontal = 10.dp, vertical = metrics.replyChipVerticalPadding)
                        ) {
                            Text(
                                text = choice,
                                color = IslandTokens.ControlLight,
                                fontSize = metrics.replyChipFontSize,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Inline Reply Text Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(metrics.replyInputHeight)
                        .clip(RoundedCornerShape(metrics.replyInputHeight / 2))
                        .background(IslandTokens.SurfaceMuted)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(replyFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    replyFieldHadFocus = true
                                    IslandStateManager.setKeyboardFocused(true)
                                } else if (replyFieldHadFocus) {
                                    replyFieldHadFocus = false
                                    IslandStateManager.setKeyboardFocused(false)
                                }
                            },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = metrics.replyInputSize
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(IslandTokens.ControlLight),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendReply(replyText) }),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = replyAction?.remoteInputLabel ?: "Type a reply...",
                                    // B5.2: TextTertiary on the input reaches 4.5:1 (#71717A was about 3.3:1)
                                    color = IslandTokens.TextTertiary,
                                    fontSize = metrics.replyInputSize
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(
                        onClick = { sendReply(replyText) },
                        enabled = replyText.isNotBlank(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reply",
                            // Disabled Send keeps its dimmer grey: disabled controls are exempt from contrast
                            tint = if (replyText.isNotBlank()) Color.White else IslandTokens.IconDisabled,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isReplying = false
                            IslandStateManager.setKeyboardFocused(false)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = IslandTokens.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Action Buttons: Taller, dynamically dividing available width equally, spaced by 4dp,
            // with inner corners facing adjacent buttons being squarish
            val actionItems = remember(notification, replyAction) {
                val list = mutableListOf<IslandActionItem>()
                visibleNotificationActions(notification).forEach { action ->
                    val isThisReply = action == replyAction || action.isReply || action.title.contains("Reply", ignoreCase = true)
                    list.add(
                        IslandActionItem(
                            title = action.title,
                            isPrimary = false,
                            onClick = {
                                if (isThisReply) {
                                    isReplying = true
                                    IslandStateManager.setKeyboardFocused(true)
                                } else {
                                    sendIslandPendingIntent(context, action.actionIntent)
                                    // The card stays while the notification is in the shade: it only ends
                                    // when swiped away or when the app / user clears the notification
                                    IslandStateManager.onNotificationActionPressed()
                                }
                            }
                        )
                    )
                }
                list
            }

            if (actionItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(metrics.actionsGap))
                IslandActionButtonRow(
                    items = actionItems,
                    height = metrics.buttonHeight,
                    fontSize = metrics.buttonFontSize
                )
            }
        }
    }
}

@Composable
fun ExpandedProgressView(notification: NotificationInfo?, onCollapse: () -> Unit, scale: Float = 1.0f) {
    val context = LocalContext.current
    val progressFraction = notification?.progressFraction ?: 0f
    val isIndet = notification?.isIndeterminate == true
    val m = remember(scale) { ProgressCardMetrics(scale) }
    val motion = LocalIslandMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = motion.spring(motion.downloadProgress),
        label = "download_progress"
    )

    fun openProgressApp() {
        openTarget(context, notification?.contentIntent, notification?.packageName)
        onCollapse()
    }

    // Header, detail line, bar and (optional) buttons stacked from the top; the card's height is
    // measured from exactly these pieces (measureProgressCardHeight), so there's no empty band.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.PROGRESS, scale)
    ) {
        IslandHeader(
            title = notification?.title?.ifBlank { "Downloading" } ?: "Downloading",
            subtitle = notification?.appName ?: "",
            titleSize = m.titleSize,
            subtitleSize = m.appNameSize,
            subtitleSpacing = m.subtitleSpacing,
            iconSpacing = m.iconGap,
            titleMarquee = true,
            icon = {
                // App icon (tap to open app)
                Box(
                    modifier = Modifier
                        .size(m.iconSize)
                        .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                        .background(IslandTokens.SurfaceMuted)
                        .clickable { openProgressApp() },
                    contentAlignment = Alignment.Center
                ) {
                    if (notification?.iconBitmap != null) {
                        Image(
                            bitmap = notification.iconBitmap.asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = IslandTokens.ActionPrimaryContainer,
                            modifier = Modifier.size((24.dp * scale).coerceIn(18.dp, 28.dp))
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(m.gap))

        val rawProgressText = notification?.text ?: ""
        val displayProgressText = remember(rawProgressText) { cleanProgressText(rawProgressText) }
        Text(
            text = displayProgressText,
            color = IslandTokens.TextBody,
            fontSize = m.bodySize,
            lineHeight = m.bodyLineHeight,
            fontWeight = FontWeight.Normal,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openProgressApp() }
        )

        Spacer(modifier = Modifier.height(m.barGap))

        if (isIndet) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(m.barHeight)
                    .clip(CircleShape),
                color = IslandTokens.ControlLight,
                trackColor = IslandTokens.ControlTrack
            )
        } else {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(m.barHeight)
                    .clip(CircleShape),
                color = IslandTokens.ControlLight,
                trackColor = IslandTokens.ControlTrack,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }

        val actionItems = remember(notification) {
            progressActions(notification).map { action ->
                IslandActionItem(
                    title = action.title,
                    isPrimary = false,
                    onClick = { runCatching { action.actionIntent?.send() } }
                )
            }
        }
        if (actionItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(m.actionsGap))
            IslandActionButtonRow(
                items = actionItems,
                height = m.buttonHeight,
                fontSize = m.buttonFontSize
            )
        }
    }
}

@Composable
fun ExpandedRecordingView(recording: ScreenRecordingState, onCollapse: () -> Unit, scale: Float = 1.0f) {
    // Same native design as voice recorders: red time, waveform, pause / resume and stop
    LiveRecordingRow(
        timeText = formatTimerText(recording.durationSeconds * 1000L),
        isPaused = recording.isPaused,
        scale = scale,
        onOpen = onCollapse,
        onPauseResume = IslandStateManager.onTogglePauseRecording?.let { toggle -> { toggle() } },
        onStop = { IslandStateManager.onStopRecording?.invoke() }
    )
}

@Composable
fun ExpandedIdleView(onCollapse: () -> Unit, scale: Float = 1.0f) {
    val type = rememberIslandType(scale)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.IDLE, scale),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Droidbubble Island",
            color = Color.White,
            fontSize = type.idleTitle,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ready for Media, Notifications & Recording",
            color = IslandTokens.TextSecondary,
            fontSize = type.idleCaption
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = onCollapse,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = IslandTokens.ActionSecondaryContainer,
                contentColor = Color.White
            )
        ) {
            Text("Collapse", fontSize = type.idleCaption)
        }
    }
}

@Composable
fun PulsingRecordingDot(modifier: Modifier = Modifier) {
    val motion = LocalIslandMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = motion.tween(MotionTween.RECORDING_PULSE),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // B8.1: the pulse is read in the layer and draw phases only, so the dot never recomposes
    Box(
        modifier = modifier
            .size(11.dp)
            .graphicsLayer {
                val a = alpha
                scaleX = 1f + a * 0.12f
                scaleY = 1f + a * 0.12f
                this.alpha = a
            }
            .drawBehind { drawCircle(IslandTokens.RecordingRedSystem) }
    )
}

/**
 * Three-bar equalizer (bass / mids / treble) driven by the real audio the phone is playing.
 * Falls back to a gentle animation when output capture isn't available (no mic permission, or
 * the device blocks it). Frames only run while playing.
 */
@Composable
fun EqualizerVisualizer(isPlaying: Boolean, barColor: Color = IslandTokens.ControlLight) {
    OutputAudioBandsEffect(active = isPlaying)
    val idle = remember { floatArrayOf(0.3f, 0.5f, 0.2f) }
    val shown = remember { idle.copyOf() }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            idle.copyInto(shown)
            frame++
            return@LaunchedEffect
        }
        val start = withInfiniteAnimationFrameMillis { it }
        // B8.1: at most ~30 steps a second (15 in Battery Saver) instead of one per vsync, so a
        // playing pill no longer holds an adaptive display at its top refresh rate. The audio
        // data itself arrives at 20 Hz or less (AudioLevels).
        val lastBands = FloatArray(shown.size) { -1f }
        while (true) {
            delay(if (AudioPowerPolicy.lowPower) EQ_STEP_LOW_POWER_MS else EQ_STEP_MS)
            withInfiniteAnimationFrameMillis { now ->
                val t = (now - start) / 1000f
                val real = OutputAudioBands.hasSignal
                // A real signal that hasn't changed since the last step needs no redraw
                if (real && shown.indices.all { OutputAudioBands.bands[it] == lastBands[it] }) return@withInfiniteAnimationFrameMillis
                if (real) for (i in shown.indices) lastBands[i] = OutputAudioBands.bands[i]
                for (i in shown.indices) {
                    val target = if (real) {
                        0.14f + 0.86f * OutputAudioBands.bands[i]
                    } else {
                        0.55f + 0.4f * kotlin.math.sin(t * EQ_FAKE_SPEED[i] + i * 1.7f)
                    }
                    // Fast attack, slower release, like a real level meter
                    val k = if (target > shown[i]) 0.5f else 0.16f
                    shown[i] += (target - shown[i]) * k
                }
                frame++
            }
        }
    }
    Canvas(modifier = Modifier.width(13.dp).height(16.dp)) {
        frame // redraw on every animation step
        val barW = 3.dp.toPx()
        val gap = 2.dp.toPx()
        val radius = androidx.compose.ui.geometry.CornerRadius(barW / 2f)
        for (i in shown.indices) {
            val h = (size.height * shown[i].coerceIn(0.12f, 1f))
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * (barW + gap), (size.height - h) / 2f),
                size = androidx.compose.ui.geometry.Size(barW, h),
                cornerRadius = radius
            )
        }
    }
}

private val EQ_FAKE_SPEED = floatArrayOf(7.0f, 8.3f, 6.0f)
private const val EQ_STEP_MS = 33L
private const val EQ_STEP_LOW_POWER_MS = 66L

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
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
 * [value], or the last non-null value it had. Lets content keep showing what it was showing
 * while its container animates out after the data went away, instead of flashing a placeholder.
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { arrayOfNulls<Any>(1) }
    if (value != null) last[0] = value
    @Suppress("UNCHECKED_CAST")
    return value ?: last[0] as T?
}

/**
 * Blurred album art behind the music card. On a solid card it's a strong wash with a black fade
 * at the bottom. With liquid glass it becomes a light colour tint instead, with no black scrim and
 * no solid fade, so the blurred screen behind the card still shows through like the other cards.
 */
@Composable
internal fun MediaArtBackdrop(art: Bitmap, needsBlur: Boolean, alpha: Float, glass: Boolean, shape: Shape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
    ) {
        Image(
            bitmap = art.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (needsBlur) Modifier.blur(radius = 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier)
                .graphicsLayer {
                    scaleX = 1.35f
                    scaleY = 1.35f
                    this.alpha = alpha * (if (glass) 0.42f else 0.75f)
                }
        )
        if (!glass) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f))
            )
        }
        // Gradient from the bottom keeps the controls readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (glass) {
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.32f))
                        } else {
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.82f),
                                Color.Black
                            )
                        }
                    )
                )
        )
    }
}

/** B9.2: the bubble's progress ring stroke. */
val BUBBLE_RING_STROKE = 2.dp

/** B9.2: the progress ring in a bubble of [bubble] size: 5 dp in from each side, 22 to 28 dp. */
fun bubbleRingDiameter(bubble: Dp): Dp = (bubble - 10.dp).coerceIn(22.dp, 28.dp)

/**
 * B9.2: the numeral's box ([labelWidth] by [digitHeight]) fits inside the ring's opening (its
 * diagonal no longer than the inner diameter, with 0.5 dp to spare per side). Comparing the width
 * alone let the corners of a wide number touch the ring at large font scales.
 */
fun bubbleNumeralFits(labelWidth: Dp, digitHeight: Dp, ring: Dp): Boolean {
    val inner = (ring - BUBBLE_RING_STROKE * 2 - 1.dp).value
    return kotlin.math.hypot(labelWidth.value, digitHeight.value) <= inner
}
