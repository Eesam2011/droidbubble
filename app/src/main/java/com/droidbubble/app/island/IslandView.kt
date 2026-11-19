package com.droidbubble.app.island

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.droidbubble.app.R
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
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.flow.first
import com.droidbubble.app.model.allows
import com.droidbubble.app.island.IslandStateManager.RingerMode
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.icon.IconEffect
import com.droidbubble.app.island.icon.iconEffect
import com.droidbubble.app.island.icon.SlashOverlay
import com.droidbubble.app.island.icon.DynamicIconVisibility
import com.droidbubble.app.island.icon.LayeredIcon
import com.droidbubble.app.island.icon.VariableMode
import com.droidbubble.app.island.icon.MorphIcon
import com.droidbubble.app.island.icon.MorphShapes
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

/** V5.2: the song's accent, from Palette with lightness and chroma clamps (see [com.droidbubble.app.island.color.MediaAccent]). */
fun extractMediaAccentColor(bitmap: Bitmap?): Color = com.droidbubble.app.island.color.MediaAccent.extract(bitmap)

/** Semantics tags for the island's touch targets (used by the crash tests). */
object IslandTestTags {
    const val PILL = "island_pill"
    const val BUBBLE = "island_bubble"
    const val CARD = "island_card"
    /** Card 2, the second activity's card in dual mode (not part of the S1 golden). */
    const val CARD2 = "island_card2"
    /** V1.3: the pill's two sides, either side of the camera gap. */
    const val COMPACT_LEADING = "island_compact_leading"
    const val COMPACT_TRAILING = "island_compact_trailing"

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
        settings.animationSpeed, settings.expandDuration, settings.expandBounce, settings.collapseDuration,
        settings.collapseBounce, settings.appearDuration, settings.appearBounce, settings.splitDuration,
        settings.splitBounce, tier,
    ) { IslandMotion.from(settings, tier) }
    // F4.1: text sizes by role, for the card scale and the phone's font size
    val fontScale = LocalDensity.current.fontScale
    val type = remember(settings.expandedScale, fontScale) { IslandType(settings.expandedScale, fontScale) }
    // V2.1: the card margin and radius system, for nested corners
    val cardGeometry = remember(settings.expandedScale, fontScale) {
        com.droidbubble.app.island.layout.CardGeometry(settings.expandedScale, fontScale)
    }
    CompositionLocalProvider(
        LocalIslandHaptics provides haptics,
        LocalIslandMotion provides motion,
        LocalIslandType provides type,
        com.droidbubble.app.island.layout.LocalCardGeometry provides cardGeometry,
        // F4.2: every Text starts from the island's base (trimmed 20 sp box), not bodyLarge's 24 sp one
        LocalTextStyle provides LocalTextStyle.current.merge(type.base),
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
            val liveKind = notification?.kind ?: LiveActivityKind.NONE
            if (notification != null && liveKind == LiveActivityKind.RECORDING) {
                // I2.3: the mic is slashed while the recorder is paused
                val paused = remember(notification) { liveActivityControls(notification).isPaused }
                // I3.2: and pulses while it records
                SlashOverlay(
                    slashed = paused,
                    slashTint = LiveActivityColors.RecordingRed,
                    modifier = Modifier.iconEffect(IconEffect.Pulse(), active = !paused),
                ) {
                    Icon(IslandIcons.Compact.Mic, contentDescription = if (paused) "Recording paused" else "Recording", tint = LiveActivityColors.RecordingRed, modifier = Modifier.size(IslandTokens.Glyph.Bubble).opticalCenter(IslandIcons.Compact.Mic))
                }
            } else if (notification != null && liveKind != LiveActivityKind.NONE) {
                LiveActivityCompactIcon(liveKind, notification, glyph = IslandTokens.Glyph.Bubble, art = IslandTokens.Glyph.BubbleArt)
            } else if (notification != null && notification.liveTimerBaseMs > 0L) {
                TimerClockIcon(
                    baseMs = notification.liveTimerBaseMs,
                    countDown = notification.liveTimerCountDown,
                    size = IslandTokens.Glyph.Bubble,
                    color = IslandTokens.Stopwatch
                )
            } else {
                // I3.2: the same app posting again bounces its picture
                val nudge = IslandStateManager.iconSignals.collectAsState().value.sameAppNudge
                Box(Modifier.iconEffect(IconEffect.Bounce(), trigger = nudge), contentAlignment = Alignment.Center) {
                    if (notification?.avatarBitmap != null) {
                        // The sender's photo fills the split bubble, with the same even ring as the pill
                        CompactContactAvatar(notification = notification, centered = true)
                    } else if (notification?.iconBitmap != null) {
                        Image(
                            bitmap = notification.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(IslandTokens.Glyph.BubbleArt)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = IslandIcons.Compact.Notification,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(IslandTokens.Glyph.Bubble).opticalCenter(IslandIcons.Compact.Notification)
                        )
                    }
                }
            }
        }
        IslandPriority.PROGRESS -> {
            val pct = notification?.progressFraction ?: 0f
            // D3.3: the ring springs to each value (read in the draw phase), a new download
            // snaps it back, and spinner and ring cross-fade when the mode changes
            val ringProgress = com.droidbubble.app.island.layout.rememberProgressAnimatable(pct)
            Crossfade(
                targetState = notification?.isIndeterminate == true,
                animationSpec = LocalIslandMotion.current.tween(MotionTween.PROGRESS_MODE_FADE),
                modifier = Modifier.fillMaxSize(),
                label = "bubbleProgressMode",
            ) { isIndet ->
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
                            val numeralStyle = LocalTextStyle.current.merge(LocalIslandType.current.bubbleNumeral.copy(color = Color.White))
                            val measurer = rememberTextMeasurer()
                            val density = LocalDensity.current
                            val labelWidth = with(density) { measurer.measure(label, numeralStyle).size.width.toDp() }
                            // Digits are about 0.72 em tall: the box that must sit inside the circle
                            val digitHeight = with(density) { IslandType.BubbleValue.toDp() } * 0.72f
                            CircularProgressIndicator(
                                progress = { ringProgress.value },
                                modifier = Modifier.size(ring),
                                strokeWidth = BUBBLE_RING_STROKE,
                                color = IslandTokens.ControlLight,
                                trackColor = IslandTokens.ControlTrack
                            )
                            if (bubbleNumeralFits(labelWidth, digitHeight, ring)) {
                                // D3.1: rolls as the download counts, centred in a "100" slot
                                RollingNumberText(
                                    text = label,
                                    color = Color.White,
                                    fontSize = numeralStyle.fontSize,
                                    fontWeight = numeralStyle.fontWeight ?: FontWeight.Bold,
                                    lineHeight = numeralStyle.lineHeight,
                                    slotTemplate = "100",
                                    slotAlign = Alignment.CenterHorizontally,
                                )
                            }
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
                    contentDescription = null,
                    modifier = Modifier
                        .size(IslandTokens.Glyph.BubbleArt)
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
                val torchLevelState = IslandStateManager.flashlightLevel.collectAsState()
                val torchMaxState = IslandStateManager.maxFlashlightLevel.collectAsState()
                DynamicIconVisibility(visible = true, appearOnEnter = true) {
                    LayeredIcon(
                        IslandIcons.Compact.Flashlight,
                        Color.White,
                        contentDescription = null,
                        size = IslandTokens.Glyph.Bubble,
                        level = { torchLevel(torchLevelState.value, torchMaxState.value) },
                    )
                }
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
    horizontalInset: Dp = 10.dp,
    /** V3.2: the most one side may hold; text past it squeezes, shortens or drops, never "…". */
    maxSide: Dp = Dp.Infinity,
) {
    val compactType = LocalIslandType.current
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
            modifier = Modifier.weight(1f).testTag(IslandTestTags.COMPACT_LEADING),
            contentAlignment = Alignment.CenterStart
        ) {
            when (priority) {
                IslandPriority.CALL -> Icon(
                    IslandIcons.Compact.Call, contentDescription = null,
                    tint = LiveActivityColors.CallGreen, modifier = Modifier.size(IslandTokens.Glyph.Compact)
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
                        // I3.3: the torch appears as it turns on; its glyph lights with the brightness level
                        DynamicIconVisibility(visible = true, appearOnEnter = true) {
                            LayeredIcon(
                                IslandIcons.Compact.Flashlight,
                                Color.White,
                                contentDescription = null,
                                level = { torchLevel(flashlightLevel, maxFlashlightLevel) },
                            )
                        }
                    }
                }

                IslandPriority.POP_NOTIFICATION -> {
                    val liveKind = notification?.kind ?: LiveActivityKind.NONE
                    if (notification != null && liveKind != LiveActivityKind.NONE) {
                        LiveActivityCompactIcon(liveKind, notification)
                    } else if (notification != null && notification.liveTimerBaseMs > 0L) {
                        TimerClockIcon(
                            baseMs = notification.liveTimerBaseMs,
                            countDown = notification.liveTimerCountDown,
                            size = IslandTokens.Glyph.Compact,
                            color = IslandTokens.Stopwatch
                        )
                    } else {
                        // I3.2: the same app posting again bounces its picture
                        val nudge = IslandStateManager.iconSignals.collectAsState().value.sameAppNudge
                        Box(Modifier.iconEffect(IconEffect.Bounce(), trigger = nudge), contentAlignment = Alignment.Center) {
                            if (notification?.avatarBitmap != null || notification?.iconBitmap != null) {
                                CompactContactAvatar(
                                    notification = notification,
                                    // One layout for every leading picture (owner, CP2 2026-11-15): a contact
                                    // photo and an app icon sit exactly where the song cover does, the same
                                    // size, with the same gap on every side
                                    edgeReach = 0.dp,
                                    maxDiameter = IslandTokens.Glyph.CompactArt
                                )
                            } else {
                                Icon(
                                    imageVector = IslandIcons.Compact.Notification,
                                    contentDescription = null,
                                    tint = IslandTokens.ControlLight,
                                    modifier = Modifier.size(IslandTokens.Glyph.Compact).opticalCenter(IslandIcons.Compact.Notification)
                                )
                            }
                        }
                    }
                }

                IslandPriority.PROGRESS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (notification?.iconBitmap != null) {
                            Image(
                                bitmap = notification.iconBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(IslandTokens.Glyph.CompactArt)
                                    .clip(CircleShape)
                            )
                        } else {
                            // I3.3: the download glyph fills with the progress; unknown progress scans
                            val fraction = rememberUpdatedState(notification?.progressFraction ?: 0f)
                            LayeredIcon(
                                IslandIcons.Compact.Download,
                                IslandTokens.ControlLight,
                                level = { fraction.value },
                                mode = if (notification?.isIndeterminate == true) VariableMode.Iterative else VariableMode.Level,
                                reversing = true,
                            )
                        }
                        // Icon and percentage only, no app name (owner, 2026-11-15)
                    }
                }

                IslandPriority.MEDIA -> {
                    FlippingCoverArt(
                        art = media?.albumArt ?: media?.appIcon,
                        trackKey = mediaTrackKey(media),
                        contentDescription = null,
                        modifier = Modifier
                            .size(IslandTokens.Glyph.CompactArt)
                            .clip(islandShape(IslandTokens.Glyph.CompactArt * com.droidbubble.app.island.layout.CardGeometry.SQUARE_CAP)),
                        placeholder = {
                            Icon(
                                imageVector = IslandIcons.Compact.MusicNote,
                                contentDescription = null,
                                tint = IslandTokens.TextSecondary,
                                modifier = Modifier.size(IslandTokens.Glyph.Compact)
                            )
                        }
                    )
                }



                IslandPriority.IDLE -> Unit
            }
        }

        // Camera punch-hole deadzone: STRICTLY 36dp EMPTY SPACE IN THE MIDDLE!
        // No UI elements will EVER enter or overlap the camera cutout area
        com.droidbubble.app.island.layout.CameraGap()

        // Trailing content: Pinned strictly to the right of the camera punch hole. Text keeps a
        // little more room from the pill's round end than an icon does
        val trailingText = when (priority) {
            IslandPriority.CALL, IslandPriority.SCREEN_RECORDING, IslandPriority.FLASHLIGHT, IslandPriority.POP_NOTIFICATION -> true
            IslandPriority.PROGRESS -> notification?.isIndeterminate != true
            else -> false
        }
        val trailingPad = if (trailingText && !isCompactDual) com.droidbubble.app.island.layout.CompactSlots.TEXT_END_PAD else 0.dp
        Box(
            modifier = Modifier.weight(1f).testTag(IslandTestTags.COMPACT_TRAILING).padding(end = trailingPad),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (priority) {
                IslandPriority.CALL -> {
                    val call by IslandStateManager.activeCallState.collectAsState()
                    RollingNumberText(
                        text = String.format(Locale.US, "%02d:%02d", call.durationSeconds / 60, call.durationSeconds % 60),
                        color = LiveActivityColors.CallGreen,
                        fontSize = IslandType.CompactValue,
                        lineHeight = LocalIslandType.current.compactLabelLineHeight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IslandPriority.SCREEN_RECORDING -> {
                    val minutes = recording.durationSeconds / 60
                    val seconds = recording.durationSeconds % 60
                    RollingNumberText(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        color = LiveActivityColors.RecordingRed,
                        fontSize = IslandType.CompactValue,
                        lineHeight = LocalIslandType.current.compactLabelLineHeight,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.FLASHLIGHT -> {
                    // V3.4: the torch level on the empty side, so the pill is balanced; it rolls as
                    // the strength changes. A torch without steps says "On". Small type, like the
                    // download pill (owner, 2026-11-15).
                    if (maxFlashlightLevel > 1) {
                        val pct = (100f * flashlightLevel.coerceIn(1, maxFlashlightLevel) / maxFlashlightLevel).roundToInt()
                        RollingNumberText(
                            text = "$pct%",
                            color = Color.White,
                            fontSize = IslandType.CompactSmall,
                            lineHeight = LocalIslandType.current.compactLabelLineHeight,
                            fontWeight = FontWeight.SemiBold,
                            slotTemplate = "100%",
                        )
                    } else {
                        Text(
                            text = "On",
                            color = IslandTokens.TextSecondary,
                            style = com.droidbubble.app.island.layout.CompactText.small(compactType),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IslandPriority.POP_NOTIFICATION -> {
                    val liveKind = notification?.kind ?: LiveActivityKind.NONE
                    val isTimedKind = liveKind != LiveActivityKind.NONE && liveKind != LiveActivityKind.CALL
                    // Rides show the ETA, scores the score, alerts "Time's up" / the alarm time
                    val liveTimer = notification?.let { liveCompactText(it, liveKind) }
                        ?: rememberLiveTimerText(notification)
                        ?: notification?.takeIf { isTimedKind }?.let { staticLiveTimeText(it) }
                    // Recorder red, timer blue, call timer green; names and alerts stay white.
                    // Live values roll digit by digit with steady-width numbers.
                    if (liveTimer != null) {
                        RollingNumberText(
                            text = liveTimer,
                            color = if (liveKind != LiveActivityKind.NONE) LiveActivityColors.accentFor(liveKind) else IslandTokens.ControlLight,
                            fontSize = IslandType.CompactValue,
                            lineHeight = LocalIslandType.current.compactLabelLineHeight,
                            fontWeight = FontWeight.SemiBold,
                            countDown = notification?.liveTimerCountDown == true || liveKind == LiveActivityKind.TRACKING
                        )
                    } else {
                        // The first name only. It squeezes a little if that's enough; a longer one
                        // fades out at NAME_MAX instead of widening the pill (never "…")
                        val name = notification?.title?.let { com.droidbubble.app.island.layout.CompactFit.firstName(it) } ?: "Notification"
                        val nameRoom = minOf(maxSide - trailingPad, com.droidbubble.app.island.layout.CompactSlots.NAME_MAX)
                        val nameStyle = com.droidbubble.app.island.layout.CompactText.label(compactType)
                        val fit = com.droidbubble.app.island.layout.rememberCompactFit(name, nameRoom, nameStyle)
                        if (fit.text != null) {
                            Text(
                                text = fit.text,
                                color = IslandTokens.ControlLight,
                                style = com.droidbubble.app.island.layout.CompactFit.squeezed(nameStyle, fit),
                                maxLines = 1,
                                softWrap = false
                            )
                        } else if (nameRoom > 0.dp) {
                            FadingName(name, nameRoom, nameStyle)
                        }
                    }
                }

                IslandPriority.PROGRESS -> {
                    val pct = notification?.progressFraction ?: 0f
                    // D3.3: spinner and percentage cross-fade when the app starts reporting a value
                    Crossfade(
                        targetState = notification?.isIndeterminate == true,
                        animationSpec = LocalIslandMotion.current.tween(MotionTween.PROGRESS_MODE_FADE),
                        label = "compactProgressMode",
                    ) { isIndet ->
                        if (isIndet) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = IslandTokens.ControlLight,
                                trackColor = IslandTokens.ControlTrack
                            )
                        } else {
                            // The download pill keeps its smaller "42%" (owner, 2026-11-15); D3.1: it
                            // rolls, in a "100%" slot so the pill never shifts as it counts
                            RollingNumberText(
                                text = "${(pct * 100).roundToInt()}%",
                                color = IslandTokens.ControlLight,
                                fontSize = IslandType.CompactSmall,
                                lineHeight = compactType.compactLabelLineHeight,
                                fontWeight = FontWeight.SemiBold,
                                slotTemplate = "100%",
                            )
                    }
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
    // I2.3: tapping the torch draws the slash first; the torch turns off as it completes
    var turningOff by remember { mutableStateOf(false) }

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
            .cardInset(IslandPriority.FLASHLIGHT, scale, LocalDensity.current.fontScale),
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
                    .padding(top = IslandTokens.SpaceXs, bottom = 0.dp)
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

        Spacer(modifier = Modifier.height(IslandTokens.SpaceXs))

        // Bottom section: Large centered flashlight icon (NO dark circle background, NO radial glow)
        // Tapping on this icon turns the flashlight off!
        Box(
            modifier = Modifier
                .padding(bottom = IslandTokens.SpaceSm)
                .size(64.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!turningOff) {
                        haptics.perform(IslandHapticEvent.TOGGLE_OFF)
                        turningOff = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // I2.3: the slash draws across the torch, and the torch turns off once it is most of
            // the way (not after a delay), so the slash finishes as the card leaves
            SlashOverlay(slashed = turningOff, slashTint = Color.White, onSlashed = onTurnOff) {
                Icon(
                    imageVector = IslandIcons.Flashlight,
                    contentDescription = "Turn off flashlight",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
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

    // V4.1: the sizes the card's measured height is computed from
    val mediaFontScale = LocalDensity.current.fontScale
    val mediaMetrics = remember(scale, mediaFontScale) { MediaCardMetrics(scale, mediaFontScale) }
    val artSize = mediaMetrics.artSize
    val titleSize = type.title
    val artistSize = type.secondary
    val sliderGapTop = mediaMetrics.sliderGapTop
    val sliderGapBottom = mediaMetrics.sliderGapBottom
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
            .cardInset(IslandPriority.MEDIA, scale, LocalDensity.current.fontScale),
        // V4.1: rows stacked; the card is measured to exactly their height
        verticalArrangement = Arrangement.Top
    ) {
        // Header: Album cover / app icon + Title & Artist + Equalizer
        IslandHeader(
            title = media?.title?.ifBlank { "No Media Playing" } ?: "No Media Playing",
            subtitle = media?.artist?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
            titleSize = titleSize,
            subtitleSize = artistSize,
            titleLineHeight = type.titleLineHeight,
            subtitleLineHeight = type.secondaryLineHeight,
            subtitleSpacing = mediaMetrics.subtitleSpacing,
            iconSpacing = mediaMetrics.iconGap,
            titleMarquee = true,
            icon = {
                // Album cover (larger, tap to open app and collapse island); flips on song change
                FlippingCoverArt(
                    art = displayArt,
                    trackKey = mediaTrackKey(media),
                    modifier = Modifier
                        .size(artSize)
                        .clip(islandShape(com.droidbubble.app.island.layout.LocalCardGeometry.current.let {
                            minOf(it.radiusFor(it.insetRoomy, artSize, artSize), artSize * com.droidbubble.app.island.layout.CardGeometry.COVER_CAP)
                        }))
                        .background(IslandTokens.SurfaceMuted)
                        .clickable {
                            openTarget(context, media?.sessionActivity, media?.packageName)
                            onCollapse()
                        },
                    placeholder = {
                        Icon(
                            imageVector = IslandIcons.MusicNote,
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
                                .clip(islandShape(thumbWidth / 2))
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
                val onLabel = stringResource(R.string.speech_on)
                val offLabel = stringResource(R.string.speech_off)
                IconButton(
                    onClick = { IslandStateManager.onToggleShuffle?.invoke() },
                    // C3.6: "Shuffle, On"
                    modifier = Modifier
                        .size(sideControlSize)
                        .semantics { stateDescription = if (media?.isShuffleOn == true) onLabel else offLabel }
                ) {
                    Icon(
                        imageVector = IslandIcons.Shuffle,
                        contentDescription = stringResource(R.string.speech_shuffle),
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
                        imageVector = IslandIcons.SkipPrevious,
                        contentDescription = "Previous",
                        tint = controlsTint,
                        modifier = Modifier.size(prevNextIcon).opticalCenter(IslandIcons.SkipPrevious)
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
                    // I2.1: play melts into pause instead of swapping
                    MorphIcon(
                        glyph = MorphShapes.PlayPause,
                        atEnd = media?.isPlaying == true,
                        tint = playButtonIconTint,
                        // C3.6: says what a tap does
                        contentDescription = stringResource(if (media?.isPlaying == true) R.string.speech_pause else R.string.speech_play),
                        size = playBtnIcon,
                    )
                }

                Spacer(modifier = Modifier.width(controlsGap))

                IconButton(
                    onClick = { IslandStateManager.onSkipNext?.invoke() },
                    modifier = Modifier.size(prevNextSize)
                ) {
                    Icon(
                        imageVector = IslandIcons.SkipNext,
                        contentDescription = "Next",
                        tint = controlsTint,
                        modifier = Modifier.size(prevNextIcon).opticalCenter(IslandIcons.SkipNext)
                    )
                }

                Spacer(modifier = Modifier.width(sideControlGap))

                // Repeat: off -> all -> one
                IconButton(
                    onClick = { IslandStateManager.onCycleRepeat?.invoke() },
                    modifier = Modifier
                        .size(sideControlSize)
                        .semantics {
                            stateDescription = if ((media?.repeatMode ?: MediaInfo.REPEAT_OFF) != MediaInfo.REPEAT_OFF) onLabel else offLabel
                        }
                ) {
                    Icon(
                        imageVector = if (media?.repeatMode == MediaInfo.REPEAT_ONE) IslandIcons.RepeatOne else IslandIcons.Repeat,
                        contentDescription = stringResource(R.string.speech_repeat),
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
    val liveKind = notification?.kind ?: LiveActivityKind.NONE
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

    val fontScale = LocalDensity.current.fontScale
    val metrics = remember(scale, fontScale) { NotificationCardMetrics(scale, fontScale) }
    val iconSize = metrics.iconSize
    val iconGap = metrics.iconGap
    val liveTimer = rememberLiveTimerText(notification)
    val displayTitle = notificationDisplayTitle(notification, liveTimer)

    // Laid out top-down; the card height is measured from this same content
    // (measureNotificationCardHeight), so there is no leftover gap and buttons never squeeze.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .cardInset(IslandPriority.POP_NOTIFICATION, scale, LocalDensity.current.fontScale)
    ) {
        // Top section: Header + Body text
        Column(modifier = Modifier.fillMaxWidth()) {
            IslandHeader(
                title = displayTitle,
                subtitle = notification?.appName ?: "",
                titleSize = metrics.titleSize,
                subtitleSize = metrics.appNameSize,
                titleLineHeight = metrics.titleLineHeight,
                subtitleLineHeight = metrics.appNameLineHeight,
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
                                imageVector = IslandIcons.Notification,
                                contentDescription = null,
                                tint = IslandTokens.ActionPrimaryContainer,
                                modifier = Modifier.size(iconSize * IslandTokens.Glyph.HeaderRatio).opticalCenter(IslandIcons.Notification)
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
                                .clip(islandShape(IslandTokens.RadiusChip))
                                .background(IslandTokens.SurfaceRaised)
                                .clickable { sendReply(choice) }
                                .padding(horizontal = 10.dp, vertical = metrics.replyChipVerticalPadding)
                        ) {
                            Text(
                                text = choice,
                                color = IslandTokens.ControlLight,
                                fontSize = metrics.replyChipFontSize,
                                lineHeight = metrics.replyChipLineHeight,
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
                        .clip(islandShape(metrics.replyInputHeight / 2))
                        .background(IslandTokens.SurfaceMuted)
                        .padding(horizontal = IslandTokens.SpaceMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val replyFieldLabel = stringResource(R.string.speech_reply_to, notification?.title.orEmpty())
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            // C3.6: "Reply to Sam, edit box"
                            .semantics { contentDescription = replyFieldLabel }
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
                                    fontSize = metrics.replyInputSize,
                                    lineHeight = metrics.replyInputLineHeight
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
                            imageVector = IslandIcons.Send,
                            contentDescription = stringResource(R.string.speech_send_reply),
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
                            imageVector = IslandIcons.Close,
                            contentDescription = stringResource(R.string.speech_cancel_reply),
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
                    fontSize = metrics.buttonFontSize,
                    lineHeight = metrics.buttonLineHeight
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
    val progressFontScale = LocalDensity.current.fontScale
    val m = remember(scale, progressFontScale) { ProgressCardMetrics(scale, progressFontScale) }
    // D3.3: the progress spring, read in the draw phase; a new download snaps back
    val animatedProgress = com.droidbubble.app.island.layout.rememberProgressAnimatable(progressFraction)

    fun openProgressApp() {
        openTarget(context, notification?.contentIntent, notification?.packageName)
        onCollapse()
    }

    // Header, detail line, bar and (optional) buttons stacked from the top; the card's height is
    // measured from exactly these pieces (measureProgressCardHeight), so there's no empty band.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .cardInset(IslandPriority.PROGRESS, scale, LocalDensity.current.fontScale)
    ) {
        IslandHeader(
            title = notification?.title?.ifBlank { "Downloading" } ?: "Downloading",
            subtitle = notification?.appName ?: "",
            titleSize = m.titleSize,
            subtitleSize = m.appNameSize,
            titleLineHeight = m.titleLineHeight,
            subtitleLineHeight = m.appNameLineHeight,
            subtitleSpacing = m.subtitleSpacing,
            iconSpacing = m.iconGap,
            titleMarquee = true,
            icon = {
                // App icon (tap to open app)
                Box(
                    modifier = Modifier
                        .size(m.iconSize)
                        .clip(islandShape(com.droidbubble.app.island.layout.LocalCardGeometry.current.let { it.radiusFor(it.insetRoomy, m.iconSize, m.iconSize) }))
                        .background(IslandTokens.SurfaceMuted)
                        .clickable { openProgressApp() },
                    contentAlignment = Alignment.Center
                ) {
                    if (notification?.iconBitmap != null) {
                        Image(
                            bitmap = notification.iconBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = IslandIcons.Download,
                            contentDescription = null,
                            tint = IslandTokens.ActionPrimaryContainer,
                            modifier = Modifier.size(m.iconSize * IslandTokens.Glyph.HeaderRatio).opticalCenter(IslandIcons.Download)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(m.gap))

        val rawProgressText = notification?.text ?: ""
        val displayProgressText = remember(rawProgressText) { cleanProgressText(rawProgressText) }
        // D3.2: the sizes roll as the download counts ("12 MB" → "13 MB"), in the line's own
        // tracking so it looks the same at rest; a line too long for the card is cut as before
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openProgressApp() }
        ) {
            val detailStyle = rollingNumberStyle(
                IslandTokens.TextBody, m.bodySize, FontWeight.Normal, m.bodyLineHeight, letterSpacing = TextUnit.Unspecified,
            )
            val measurer = rememberTextMeasurer()
            val maxPx = constraints.maxWidth
            val fits = remember(displayProgressText, detailStyle, maxPx) {
                measurer.measure(displayProgressText, detailStyle, maxLines = 1, softWrap = false).size.width <= maxPx
            }
            if (fits) {
                RollingNumberText(
                    text = displayProgressText,
                    color = IslandTokens.TextBody,
                    fontSize = m.bodySize,
                    fontWeight = FontWeight.Normal,
                    lineHeight = m.bodyLineHeight,
                    letterSpacing = TextUnit.Unspecified,
                    maxRollLength = DETAIL_ROLL_MAX,
                )
            } else {
                Text(
                    text = displayProgressText,
                    style = detailStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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
                progress = { animatedProgress.value },
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
                fontSize = m.buttonFontSize,
                lineHeight = m.buttonLineHeight
            )
        }
    }
}

/** D3.2: the longest download detail line that rolls; longer ones change plainly. */
private const val DETAIL_ROLL_MAX = 40

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
            .cardInset(IslandPriority.IDLE, scale, LocalDensity.current.fontScale),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Droidbubble Island",
            color = Color.White,
            fontSize = type.idleTitle,
            lineHeight = type.idleTitleLineHeight,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(IslandTokens.SpaceXs))
        Text(
            text = "Ready for Media, Notifications & Recording",
            color = IslandTokens.TextSecondary,
            fontSize = type.idleCaption,
            lineHeight = type.idleCaptionLineHeight
        )
        Spacer(modifier = Modifier.height(IslandTokens.SpaceMd))
        FilledTonalButton(
            onClick = onCollapse,
            shape = islandShape(com.droidbubble.app.island.layout.LocalCardGeometry.current.let { it.inner(it.insetRoomy) }),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = IslandTokens.ActionSecondaryContainer,
                contentColor = Color.White
            )
        ) {
            Text("Collapse", fontSize = type.idleCaption, lineHeight = type.idleCaptionLineHeight)
        }
    }
}

@Composable
fun PulsingRecordingDot(modifier: Modifier = Modifier) {
    // I1.2: the engine's breathe, read in the layer only. Same pace and character as before:
    // opacity 0.35 <-> 1 with the dot 12 % larger at full opacity, 650 ms each way
    Box(
        modifier = modifier
            .size(11.dp)
            .iconEffect(RECORDING_DOT_BREATHE)
            .drawBehind { drawCircle(IslandTokens.RecordingRed) }
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

private const val RECORDING_DOT_MIN_ALPHA = 0.35f
private const val RECORDING_DOT_GROWTH = 0.12f
private val RECORDING_DOT_BREATHE = IconEffect.Breathe(
    minAlpha = RECORDING_DOT_MIN_ALPHA,
    scaleFrom = 1f + RECORDING_DOT_GROWTH * RECORDING_DOT_MIN_ALPHA,
    scaleTo = 1f + RECORDING_DOT_GROWTH,
    periodMillis = MotionTween.RECORDING_PULSE.millis * 2,
)

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
    // V5.3: the scrim darkens only as much as the cover's brightest parts need for 4.5:1 text
    val bands = remember(art) { com.droidbubble.app.island.color.Scrim.bands(art) }
    val artAlpha = if (glass) 0.42f else 0.75f
    val minScrim = if (glass) 0f else com.droidbubble.app.island.color.Scrim.MIN_SOLID
    val topScrim = com.droidbubble.app.island.color.Scrim.alphaFor(bands.top, artAlpha, minScrim)
    val bottomScrim = com.droidbubble.app.island.color.Scrim.alphaFor(bands.bottom, artAlpha, minScrim)
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
                    this.alpha = alpha * artAlpha
                }
        )
        if (topScrim > 0f || bottomScrim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = topScrim), Color.Black.copy(alpha = bottomScrim))))
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

/**
 * A name too long for its [room]: the start is shown and the end fades to clear over the last
 * 14 dp, so it still reads as the person's name without an ellipsis or a wider pill.
 */
@Composable
internal fun FadingName(name: String, room: Dp, style: androidx.compose.ui.text.TextStyle) {
    Text(
        text = name,
        color = IslandTokens.ControlLight,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .width(room)
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val fade = (FADING_NAME_EDGE.toPx() / size.width).coerceIn(0f, 1f)
                drawRect(
                    Brush.horizontalGradient(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }
    )
}

val FADING_NAME_EDGE = 14.dp

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

/**
 * I3.3: the torch glyph's lit fraction. A torch without brightness steps ([max] 1 or less) is
 * always fully lit, exactly as before.
 */
fun torchLevel(level: Int, max: Int): Float = if (max <= 1) 1f else (level.toFloat() / max).coerceIn(0f, 1f)
