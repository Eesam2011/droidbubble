package com.droidbubble.app.island

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bluetooth
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.ArrowUpward
import com.droidbubble.app.model.FpsInfo
import kotlinx.coroutines.flow.first
import com.droidbubble.app.model.HourlyWeather
import com.droidbubble.app.model.WeatherInfo
import com.droidbubble.app.model.NetworkSpeedInfo
import com.droidbubble.app.island.IslandStateManager.RingerMode
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IslandOverlayRoot(
    settings: IslandSettings,
    onBoundsChange: (widthPx: Int, heightPx: Int) -> Unit
) {
    val isExpanded by IslandStateManager.isExpanded.collectAsState()
    val isPopIn by IslandStateManager.isPopInActive.collectAsState()
    val media by IslandStateManager.activeMedia.collectAsState()
    val notification by IslandStateManager.activeNotification.collectAsState()
    val progressNotification by IslandStateManager.activeProgress.collectAsState()
    val recording by IslandStateManager.screenRecordingState.collectAsState()
    val charging by IslandStateManager.activeCharging.collectAsState()
    val isUnlocked by IslandStateManager.isUnlocked.collectAsState()
    val isBluetoothConnected by IslandStateManager.isBluetoothConnected.collectAsState()
    val activeRingerMode by IslandStateManager.activeRingerMode.collectAsState()
    val isFlashlightOn by IslandStateManager.isFlashlightOn.collectAsState()
    val flashlightLevel by IslandStateManager.flashlightLevel.collectAsState()
    val maxFlashlightLevel by IslandStateManager.maxFlashlightLevel.collectAsState()
    val isKeyboardFocused by IslandStateManager.isKeyboardFocused.collectAsState()
    val fpsInfo by IslandStateManager.fpsInfo.collectAsState()
    val weatherInfo by IslandStateManager.weatherInfo.collectAsState()
    val networkSpeedInfo by IslandStateManager.networkSpeedInfo.collectAsState()

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var lastRingerMode by remember { mutableStateOf(RingerMode.RING) }
    LaunchedEffect(activeRingerMode) {
        if (activeRingerMode != null) {
            lastRingerMode = activeRingerMode!!
        }
    }
    val displayRingerMode = activeRingerMode ?: lastRingerMode

    val idleBounceY = remember { Animatable(0f) }
    val idleBounceScale = remember { Animatable(1f) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val allowInOrientation = settings.isEnabled && (settings.showInLandscape || !isLandscape)

    if (!allowInOrientation || !settings.isEnabled) {
        return
    }

    val currentMedia = media
    val currentNotif = notification
    val currentProgress = progressNotification
    val mediaArt = currentMedia?.albumArt ?: currentMedia?.appIcon
    val mediaAccentColor = remember(mediaArt) { extractMediaAccentColor(mediaArt) }
    val blurredMediaArt = remember(mediaArt, settings.blurredAlbumArtBackground) {
        if (settings.blurredAlbumArtBackground && mediaArt != null) {
            // Stronger one-time blur: replaces the per-frame RenderEffect blur that used to sit on top
            createBlurredBitmap(mediaArt, blurRadius = 20)
        } else {
            null
        }
    }
    val isRecording = allowInOrientation && recording.isRecording && settings.showScreenRecording
    val isProgress = allowInOrientation && currentProgress != null && settings.showLiveProgress
    val isNotification = allowInOrientation && currentNotif != null && settings.showNotifications
    val isMedia = allowInOrientation && currentMedia != null && (!settings.dismissMediaOnPause || currentMedia.isPlaying) && (currentMedia.isPlaying || currentMedia.title.isNotBlank()) && settings.showMedia
    val isFlashlight = allowInOrientation && settings.showFlashlight && isFlashlightOn
    val isCharging = allowInOrientation && settings.showCharging && charging != null
    val isUnlock = allowInOrientation && isUnlocked && !isCharging
    val isBluetooth = allowInOrientation && settings.showBluetooth && isBluetoothConnected && !isCharging && !isUnlock
    val isRingerMode = allowInOrientation && settings.showRingerMode && activeRingerMode != null && !isCharging && !isUnlock && !isBluetooth

    val isFps = allowInOrientation && settings.showFps
    val isWeather = allowInOrientation && settings.showWeather
    val isNetworkSpeed = allowInOrientation && settings.showNetworkSpeed
    val hasPersistent = isFps || isWeather || isNetworkSpeed

    val isOccupied = allowInOrientation && (isRecording || isNotification || isMedia || isProgress || isCharging || isUnlock || isBluetooth || isFlashlight || isRingerMode || hasPersistent)

    val persistentList = buildList {
        if (isFps) add(IslandPriority.FPS)
        if (isWeather) add(IslandPriority.WEATHER)
        if (isNetworkSpeed) add(IslandPriority.NETWORK_SPEED)
    }
    val persistentPrimary = persistentList.firstOrNull()
    val persistentSecondary = if (persistentList.size > 1) persistentList[1] else null

    // Determine dual-island state (when 2+ activities occupy the island, temporarily overshadowed by charging, unlock, bluetooth, or ringer)
    val activeCount = (if (isRecording) 1 else 0) +
            (if (isNotification) 1 else 0) +
            (if (isMedia) 1 else 0) +
            (if (isProgress) 1 else 0) +
            (if (isFlashlight) 1 else 0)
    val ephemeralCount = activeCount
    val isDual = !isCharging && !isUnlock && !isBluetooth && !isRingerMode && (
        ephemeralCount >= 2 || (ephemeralCount == 0 && persistentList.size >= 2)
    )

    // Primary & Secondary activity assignment ordered dynamically by settings.featurePriorities:
    val orderedEphemeralPriorities = buildList {
        for (featureKey in settings.featurePriorities) {
            when (featureKey) {
                "MEDIA" -> if (isMedia) add(IslandPriority.MEDIA)
                "PROGRESS" -> if (isProgress) add(IslandPriority.PROGRESS)
                "RECORDING" -> if (isRecording) add(IslandPriority.SCREEN_RECORDING)
                "NOTIFICATION" -> if (isNotification) add(IslandPriority.POP_NOTIFICATION)
                "FLASHLIGHT" -> if (isFlashlight) add(IslandPriority.FLASHLIGHT)
            }
        }
        if (isFlashlight && "FLASHLIGHT" !in settings.featurePriorities) {
            add(IslandPriority.FLASHLIGHT)
        }
    }

    val basePrimaryPriority: IslandPriority = when {
        orderedEphemeralPriorities.isNotEmpty() -> orderedEphemeralPriorities.first()
        persistentPrimary != null -> persistentPrimary
        else -> IslandPriority.IDLE
    }

    val baseSecondaryPriority: IslandPriority? = when {
        !isDual -> null
        orderedEphemeralPriorities.size >= 2 -> {
            if (isPopIn && isNotification && basePrimaryPriority != IslandPriority.POP_NOTIFICATION) {
                IslandPriority.POP_NOTIFICATION
            } else {
                orderedEphemeralPriorities[1]
            }
        }
        ephemeralCount == 0 -> persistentSecondary
        else -> null
    }

    // Swiping the pill sideways swaps which of the two activities is shown in the pill and
    // which in the bubble (as on iOS). Resets once only one activity is left.
    var dualSwapped by remember { mutableStateOf(false) }
    LaunchedEffect(isDual) {
        if (!isDual) dualSwapped = false
    }
    val primaryPriority: IslandPriority =
        if (dualSwapped && baseSecondaryPriority != null) baseSecondaryPriority else basePrimaryPriority
    val secondaryPriority: IslandPriority? =
        if (dualSwapped && baseSecondaryPriority != null) basePrimaryPriority else baseSecondaryPriority

    // Dynamic non-customizable outline colors (around 15% opacity):
    val redRecordingOutline = Color(0xFFFF3B30).copy(alpha = 0.15f)
    val defaultTaskOutline = Color.White.copy(alpha = 0.15f)
    val mediaOutline = if (mediaAccentColor != Color.Transparent) {
        mediaAccentColor.copy(alpha = 0.15f)
    } else {
        defaultTaskOutline
    }
    val flashlightOutline = Color.White.copy(alpha = 0.25f)

    // Card 1 (Main Island):
    val primaryOutlineColor = when {
        isCharging -> Color(0xFF30D158).copy(alpha = 0.30f)
        primaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        primaryPriority == IslandPriority.MEDIA -> mediaOutline
        primaryPriority == IslandPriority.FLASHLIGHT -> flashlightOutline
        else -> defaultTaskOutline
    }

    // Card 2 / Auxiliary Bubble (Secondary Island):
    // Secondary islands have the color of their respective task (15% opacity white usually, or red for recording) instead of the media color
    val secondaryOutlineColor = when {
        secondaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        secondaryPriority == IslandPriority.MEDIA -> mediaOutline
        secondaryPriority == IslandPriority.FLASHLIGHT -> flashlightOutline
        else -> defaultTaskOutline
    }

    val speed = settings.animationSpeed.coerceIn(0.5f, 2.0f)
    val speedSq = speed * speed
    // Collapse uses a critically damped spring (no overshoot) instead of a fixed tween, so an
    // interrupted expand keeps its velocity and the card lands softly like iOS.
    val collapseStiffness = settings.expandStiffness * 1.4f * speedSq

    // Squash & stretch when the pill switches to a different activity (the iOS "breath")
    val swapSquash = remember { Animatable(0f) }
    val swapBlur = remember { Animatable(0f) }
    var lastCompactPriority by remember { mutableStateOf(primaryPriority) }
    LaunchedEffect(primaryPriority) {
        if (primaryPriority == lastCompactPriority) return@LaunchedEffect
        lastCompactPriority = primaryPriority
        if (isExpanded) return@LaunchedEffect
        launch {
            // Content blurs during the swap and sharpens as the new activity settles
            swapBlur.snapTo(1f)
            swapBlur.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
        }
        swapSquash.animateTo(0.06f, tween(110, easing = FastOutSlowInEasing))
        swapSquash.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f * speedSq))
    }

    // Press feedback: the pill and bubble sink slightly under the finger and spring back
    val pillInteraction = remember { MutableInteractionSource() }
    val pillPressed by pillInteraction.collectIsPressedAsState()
    val pillPressScale by animateFloatAsState(
        targetValue = if (pillPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "pillPress"
    )
    val bubbleInteraction = remember { MutableInteractionSource() }
    val bubblePressed by bubbleInteraction.collectIsPressedAsState()
    val bubblePressScale by animateFloatAsState(
        targetValue = if (bubblePressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
        label = "bubblePress"
    )

    // Animation Specs (dynamically scaled by settings.animationSpeed):
    val sizeAnimSpecDp = when {
        isCharging || isUnlock || isBluetooth || isRingerMode -> spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = settings.appearDamping,
            stiffness = settings.appearStiffness * speedSq
        )
        isExpanded -> spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = settings.expandDamping,
            stiffness = settings.expandStiffness * speedSq
        )
        else -> spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = collapseStiffness,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    }

    val sizeAnimSpecFloat = when {
        isCharging || isUnlock || isBluetooth || isRingerMode -> spring<Float>(
            dampingRatio = settings.appearDamping,
            stiffness = settings.appearStiffness * speedSq
        )
        isExpanded -> spring<Float>(
            dampingRatio = settings.expandDamping,
            stiffness = settings.expandStiffness * speedSq
        )
        else -> spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = collapseStiffness,
            visibilityThreshold = 0.002f
        )
    }

    // One spring drives the card's width, height, corner radius and drop position together,
    // so the pill-to-card morph moves as a single shape instead of four separate animations.
    val cardMorphAnimSpec: AnimationSpec<CardMorph> = when {
        isCharging || isUnlock || isBluetooth || isRingerMode -> spring(
            dampingRatio = settings.appearDamping,
            stiffness = settings.appearStiffness * speedSq,
            visibilityThreshold = CardMorph.VisibilityThreshold
        )
        isExpanded -> spring(
            dampingRatio = settings.expandDamping,
            stiffness = settings.expandStiffness * speedSq,
            visibilityThreshold = CardMorph.VisibilityThreshold
        )
        else -> spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = collapseStiffness,
            visibilityThreshold = CardMorph.VisibilityThreshold
        )
    }

    val positionAnimSpecDp = if (isExpanded) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = settings.expandDamping,
            stiffness = settings.expandStiffness * speedSq
        )
    } else {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = collapseStiffness,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    }

    // Dedicated spec for compact pill width:
    // When expanding: crisp spring shrinking into 0 width without overshoot
    // When emerging back: subtle horizontal bounce of a few pixels (dampingRatio = 0.63f)!
    val compactPillWidthAnimSpec = if (isExpanded) {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = 0.82f,
            stiffness = 380f * speedSq
        )
    } else {
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = settings.splitDamping,
            stiffness = settings.splitStiffness * speedSq
        )
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) 1f else 0f,
        animationSpec = sizeAnimSpecFloat,
        label = "expandProgress"
    )

    val chargingContentAlpha by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "chargingContentAlpha"
    )

    val unlockContentAlpha by animateFloatAsState(
        targetValue = if (isUnlock) 1f else 0f,
        animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "unlockContentAlpha"
    )

    var unlockCheckmarkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isUnlock) {
        if (isUnlock) {
            unlockCheckmarkVisible = false
            kotlinx.coroutines.delay((110L / speed).toLong())
            unlockCheckmarkVisible = true
        } else {
            unlockCheckmarkVisible = false
        }
    }

    val unlockCheckmarkAlpha by animateFloatAsState(
        targetValue = if (unlockCheckmarkVisible && isUnlock) 1f else 0f,
        animationSpec = tween(durationMillis = (150 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "unlockCheckmarkAlpha"
    )

    val unlockCheckmarkScale by animateFloatAsState(
        targetValue = if (unlockCheckmarkVisible && isUnlock) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.52f, // Gentle bounce: overshoots slightly by ~3-4px, then settles
            stiffness = 320f * speedSq
        ),
        label = "unlockCheckmarkScale"
    )

    val bluetoothContentAlpha by animateFloatAsState(
        targetValue = if (isBluetooth) 1f else 0f,
        animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "bluetoothContentAlpha"
    )

    var bluetoothCheckmarkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isBluetooth) {
        if (isBluetooth) {
            bluetoothCheckmarkVisible = false
            kotlinx.coroutines.delay((110L / speed).toLong())
            bluetoothCheckmarkVisible = true
        } else {
            bluetoothCheckmarkVisible = false
        }
    }

    val bluetoothCheckmarkAlpha by animateFloatAsState(
        targetValue = if (bluetoothCheckmarkVisible && isBluetooth) 1f else 0f,
        animationSpec = tween(durationMillis = (150 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "bluetoothCheckmarkAlpha"
    )

    val bluetoothCheckmarkScale by animateFloatAsState(
        targetValue = if (bluetoothCheckmarkVisible && isBluetooth) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.52f, // Gentle bounce: overshoots slightly by ~3-4px, then settles
            stiffness = 320f * speedSq
        ),
        label = "bluetoothCheckmarkScale"
    )

    // Ringer Mode (Silent, Vibrate, Ring) animations:
    val ringerContentAlpha by animateFloatAsState(
        targetValue = if (isRingerMode) 1f else 0f,
        animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "ringerContentAlpha"
    )

    var ringerSecondaryVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isRingerMode) {
        if (isRingerMode) {
            ringerSecondaryVisible = false
            kotlinx.coroutines.delay((110L / speed).toLong())
            ringerSecondaryVisible = true
        } else {
            ringerSecondaryVisible = false
        }
    }

    val ringerSecondaryAlpha by animateFloatAsState(
        targetValue = if (ringerSecondaryVisible && isRingerMode) 1f else 0f,
        animationSpec = tween(durationMillis = (150 / speed).roundToInt(), easing = FastOutSlowInEasing),
        label = "ringerSecondaryAlpha"
    )

    val ringerSecondaryScale by animateFloatAsState(
        targetValue = if (ringerSecondaryVisible && isRingerMode) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = 320f * speedSq
        ),
        label = "ringerSecondaryScale"
    )

    val bellShakeRotation = remember { Animatable(0f) }

    LaunchedEffect(isRingerMode, activeRingerMode) {
        if (isRingerMode && activeRingerMode == RingerMode.RING) {
            launch {
                if (settings.hapticsEnabled) {
                    try {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                            vm?.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        }
                        if (vibrator != null && vibrator.hasVibrator()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val timings = longArrayOf(0, 25, 100, 25, 100, 25)
                                val amplitudes = intArrayOf(0, 180, 0, 200, 0, 220)
                                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(longArrayOf(0, 25, 100, 25, 100, 25), -1)
                            }
                        } else {
                            for (i in 0 until 3) {
                                if (i > 0) delay(100)
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    } catch (_: Throwable) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                }
            }
            bellShakeRotation.snapTo(0f)
            val animDuration = (1000 / speed).roundToInt()
            val rotSpec = keyframes<Float> {
                durationMillis = animDuration
                0f at 0 using FastOutSlowInEasing
                -24f at (animDuration * 0.06f).roundToInt() using FastOutSlowInEasing
                24f at (animDuration * 0.13f).roundToInt() using FastOutSlowInEasing
                -18f at (animDuration * 0.22f).roundToInt() using FastOutSlowInEasing
                14f at (animDuration * 0.33f).roundToInt() using FastOutSlowInEasing
                -10f at (animDuration * 0.46f).roundToInt() using FastOutSlowInEasing
                6f at (animDuration * 0.60f).roundToInt() using FastOutSlowInEasing
                -3f at (animDuration * 0.75f).roundToInt() using FastOutSlowInEasing
                1f at (animDuration * 0.88f).roundToInt() using FastOutSlowInEasing
                0f at animDuration using FastOutSlowInEasing
            }
            bellShakeRotation.animateTo(0f, animationSpec = rotSpec)
        } else {
            bellShakeRotation.snapTo(0f)
        }
    }

    val vibrateShakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isRingerMode, activeRingerMode) {
        if (isRingerMode && activeRingerMode == RingerMode.VIBRATE) {
            vibrateShakeOffset.snapTo(0f)
            val vibDuration = (600 / speed).roundToInt()
            val vibSpec = keyframes<Float> {
                durationMillis = vibDuration
                0f at 0 using FastOutSlowInEasing
                -3f at (vibDuration * 0.08f).roundToInt() using FastOutSlowInEasing
                3f at (vibDuration * 0.16f).roundToInt() using FastOutSlowInEasing
                -3f at (vibDuration * 0.25f).roundToInt() using FastOutSlowInEasing
                3f at (vibDuration * 0.35f).roundToInt() using FastOutSlowInEasing
                -2f at (vibDuration * 0.48f).roundToInt() using FastOutSlowInEasing
                2f at (vibDuration * 0.62f).roundToInt() using FastOutSlowInEasing
                -1f at (vibDuration * 0.78f).roundToInt() using FastOutSlowInEasing
                0f at vibDuration using FastOutSlowInEasing
            }
            vibrateShakeOffset.animateTo(0f, animationSpec = vibSpec)
        } else {
            vibrateShakeOffset.snapTo(0f)
        }
    }

    val chargingPct = charging?.percentage ?: 0
    val targetChargingFraction = if (isCharging) (chargingPct.toFloat() / 100f).coerceIn(0f, 1f) else 0f
    val animatedChargingProgress by animateFloatAsState(
        targetValue = targetChargingFraction,
        animationSpec = if (isCharging) {
            tween(
                durationMillis = (750 / speed).roundToInt(),
                delayMillis = (60 / speed).roundToInt(),
                easing = FastOutSlowInEasing
            )
        } else {
            tween(
                durationMillis = (150 / speed).roundToInt(),
                easing = FastOutLinearInEasing
            )
        },
        label = "chargingProgress"
    )

    val chargingIslandWidth = (settings.widthDp + 20).dp.coerceAtLeast(140.dp)
    val unlockIslandWidth = (settings.widthDp * 0.82f).dp.coerceAtLeast(108.dp)
    val ringerIslandWidth = (settings.widthDp * 0.95f).dp.coerceAtLeast(130.dp)

    // Minimised Island Pill: sits docked at settings.topOffsetDp.dp.
    // When expanding, it animates from its compact width down to 0.dp.
    // When minimising, it animates from 0.dp back to its compact width.
    val targetCompactWidth = when {
        !isOccupied && !settings.alwaysShowPill && !hasPersistent -> 0.dp
        isExpanded -> 0.dp // Shrinks horizontally into 0 width while card expands!
        isCharging -> chargingIslandWidth
        isRingerMode -> ringerIslandWidth
        isUnlock || isBluetooth -> unlockIslandWidth
        // Dual mode keeps the main pill at full size; the secondary bubble sits beside it
        else -> settings.widthDp.dp
    }

    // Expanded Cards: Card 1 (and Card 2 if dual mode).
    // When expanded, Card 1 slides down to (settings.topOffsetDp + 52).dp, widening to 340.dp * scale.
    // When minimising, Card 1 shrinks back to 0.dp width (transitioning into the small circle at the punch hole) as it slides back up.
    val screenWidth = configuration.screenWidthDp.dp
    val expandedCardWidth = (340.dp * settings.expandedScale).coerceAtMost(screenWidth - 16.dp)
    val targetCard1Width = if (isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) {
        expandedCardWidth
    } else {
        0.dp
    }

    // Notification card height is measured from its real content (title, wrapped body, actions)
    val notifTextMeasurer = rememberTextMeasurer()
    val notifBaseTextStyle = LocalTextStyle.current
    val notifDensity = LocalDensity.current
    val notifBaseHeight = remember(currentNotif, isKeyboardFocused, settings.expandedScale, expandedCardWidth, notifBaseTextStyle, notifDensity) {
        measureNotificationCardHeight(
            notification = currentNotif,
            title = notificationDisplayTitle(currentNotif, null),
            scale = settings.expandedScale,
            cardWidth = expandedCardWidth,
            isReplying = isKeyboardFocused,
            textMeasurer = notifTextMeasurer,
            baseStyle = notifBaseTextStyle,
            density = notifDensity
        )
    }

    // Single height table shared by Card 1 and Card 2 (see cardHeightFor).
    // Content is laid out once at this final size and revealed by the growing card,
    // so it doesn't re-measure / reflow on every frame of the morph.
    val card1ContentHeight = cardHeightFor(primaryPriority, settings.expandedScale, notifBaseHeight)
    val targetCard1Height = if (isExpanded) {
        card1ContentHeight
    } else {
        settings.heightDp.dp
    }

    val targetCornerRadius = when {
        isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode -> expandedCornerRadiusFor(card1ContentHeight)
        else -> settings.cornerRadiusDp.dp
    }

    // Card 2 delayed appearance: starts a split frame after Card 1 when expanding
    var card2Expanded by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            kotlinx.coroutines.delay((45L / speed).toLong())
            card2Expanded = true
        } else {
            card2Expanded = false
        }
    }

    val showSecondaryCard = !isCharging && !isUnlock && !isBluetooth && !isRingerMode && activeCount >= 2 && secondaryPriority != null
    val targetCard2Height = if (card2Expanded && showSecondaryCard) {
        cardHeightFor(secondaryPriority, settings.expandedScale, notifBaseHeight)
    } else 0.dp
    val card2CornerRadius = expandedCornerRadiusFor(cardHeightFor(secondaryPriority, settings.expandedScale, notifBaseHeight))
    val targetCard2Alpha = if (card2Expanded && showSecondaryCard) 1f else 0f
    val targetCard2OffsetY = if (card2Expanded && showSecondaryCard) 0.dp else (-14).dp

    val bubbleSize = settings.heightDp.dp
    val compactMainHalf = settings.widthDp.dp / 2f
    val bubbleGap = 6.dp
    val restingBubbleCenterX = compactMainHalf + bubbleGap + (bubbleSize / 2f)

    val shouldEmergeSecondary = !isCharging && !isUnlock && !isBluetooth && !isRingerMode && isDual && !isExpanded && (isOccupied || hasPersistent)
    var secondaryBubbleEmerged by remember { mutableStateOf(false) }
    LaunchedEffect(shouldEmergeSecondary) {
        if (shouldEmergeSecondary) {
            // Subtle delay for a split fraction when the main island appears, so the main island establishes first
            kotlinx.coroutines.delay((60L / speed).toLong().coerceAtLeast(10L))
            secondaryBubbleEmerged = true
        } else {
            secondaryBubbleEmerged = false
        }
    }

    val targetBubbleOffsetX = when {
        secondaryBubbleEmerged && shouldEmergeSecondary -> restingBubbleCenterX
        else -> 0.dp
    }

    val bubbleOffsetAnimSpec = if (secondaryBubbleEmerged && shouldEmergeSecondary) {
        spring<Dp>(
            dampingRatio = settings.splitDamping, // Slight bounce! Gently overshoots by ~3-4dp and bounces back
            stiffness = settings.splitStiffness * speedSq
        )
    } else {
        spring<Dp>(
            dampingRatio = 0.85f, // Smooth retraction back inside the punch hole
            stiffness = 380f * speedSq
        )
    }

    val targetHorizontalOffset = if (isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) {
        0.dp
    } else if (!isCharging && !isUnlock && !isBluetooth && !isRingerMode) {
        settings.horizontalOffsetDp.dp
    } else {
        0.dp
    }

    val targetTopOffset = if (isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) {
        (settings.topOffsetDp + 52).dp // Sits way below initial position (comfortably below status bar) when expanded!
    } else {
        settings.topOffsetDp.dp // Docks right over the punch hole when minimised or charging/unlock/bluetooth/ringer
    }

    val targetCardSpacing = if (isExpanded && showSecondaryCard) 10.dp else 0.dp

    val animatedCompactWidth by animateDpAsState(targetValue = targetCompactWidth, animationSpec = compactPillWidthAnimSpec, label = "compactWidth")
    val animatedCardMorph by animateValueAsState(
        targetValue = CardMorph(targetCard1Width, targetCard1Height, targetCornerRadius, targetTopOffset),
        typeConverter = CardMorph.VectorConverter,
        animationSpec = cardMorphAnimSpec,
        label = "cardMorph"
    )
    val animatedCard1Width = animatedCardMorph.width.coerceAtLeast(0.dp)
    val animatedCard1Height = animatedCardMorph.height.coerceAtLeast(0.dp)
    val animatedCornerRadius = animatedCardMorph.corner.coerceAtLeast(0.dp)
    val animatedCard2Height by animateDpAsState(targetValue = targetCard2Height, animationSpec = sizeAnimSpecDp, label = "card2Height")
    val animatedCard2Alpha by animateFloatAsState(targetValue = targetCard2Alpha, animationSpec = sizeAnimSpecFloat, label = "card2Alpha")
    val animatedCard2OffsetY by animateDpAsState(targetValue = targetCard2OffsetY, animationSpec = sizeAnimSpecDp, label = "card2OffsetY")
    val animatedBubbleOffsetX by animateDpAsState(targetValue = targetBubbleOffsetX, animationSpec = bubbleOffsetAnimSpec, label = "bubbleOffsetX")
    val animatedHorizontalOffset by animateDpAsState(targetValue = targetHorizontalOffset, animationSpec = positionAnimSpecDp, label = "horizontalOffset")
    val animatedTopOffset = animatedCardMorph.top
    val animatedCardSpacing by animateDpAsState(targetValue = targetCardSpacing, animationSpec = sizeAnimSpecDp, label = "cardSpacing")

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            snapshotFlow { animatedCardMorph.width <= 0.5.dp && animatedCard2Height <= 0.5.dp }.first { it }
            IslandStateManager.notifyCollapseSettled()
        }
    }

    val cardContainerAlpha = if (isExpanded) 1f else ((expandProgress - 0.05f) / 0.95f).coerceIn(0f, 1f)

    var card1OffsetX by remember { mutableFloatStateOf(0f) }
    var card2OffsetX by remember { mutableFloatStateOf(0f) }

    val animatedCard1OffsetX by animateFloatAsState(
        targetValue = card1OffsetX,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f * speedSq),
        label = "card1OffsetX"
    )
    val animatedCard2OffsetX by animateFloatAsState(
        targetValue = card2OffsetX,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f * speedSq),
        label = "card2OffsetX"
    )

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            card1OffsetX = 0f
            card2OffsetX = 0f
        }
    }

    // Parentheses overshoot animation: curve shoots out with dynamic spring physics and settles
    val parenthesisExtraShoot = remember { Animatable(0f) }
    var wasExpanded by remember { mutableStateOf(isExpanded) }
    var wasOccupied by remember { mutableStateOf(isOccupied) }

    LaunchedEffect(isExpanded, isOccupied, settings.bubbleEffectEnabled) {
        val justMinimised = wasExpanded && !isExpanded
        val justAppeared = !wasOccupied && isOccupied && !isExpanded
        wasExpanded = isExpanded
        wasOccupied = isOccupied

        if (settings.bubbleEffectEnabled && (justMinimised || justAppeared)) {
            parenthesisExtraShoot.snapTo(0f)
            // Wait for minimised island pill to expand close to punch hole exit (~70ms at 1x speed)
            kotlinx.coroutines.delay((70L / speed).toLong().coerceAtLeast(10L))
            // Shoot outward from the main island with spring bounce and settle back to 0
            parenthesisExtraShoot.animateTo(
                targetValue = 0f,
                initialVelocity = 240f * speed,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 260f * speedSq
                )
            )
        } else {
            parenthesisExtraShoot.snapTo(0f)
        }
    }

    LaunchedEffect(charging?.timestamp) {
        if (charging != null && settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    LaunchedEffect(isExpanded, isOccupied, primaryPriority) {
        if (isExpanded) {
            if (!isOccupied || primaryPriority == IslandPriority.IDLE) {
                IslandStateManager.collapse()
            }
        }
    }

    // Light tap for expand / taps, a barely-there tick for collapse, a firm press only for long-press
    fun triggerHaptic() {
        if (settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    fun triggerCollapseHaptic() {
        if (settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun triggerLongPressHaptic() {
        if (settings.hapticsEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    // Completely idle and collapsed: nothing is visible on screen. Skip rendering the view hierarchy to preserve CPU & battery.
    if (!isOccupied && !settings.alwaysShowPill && animatedCompactWidth <= 0.1.dp && !isExpanded && expandProgress <= 0.001f) {
        return
    }

    // Outer container: full touch bounds when expanded to capture outside tap, or exact bounds when minimised
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // Tap outside backdrop when expanded (active during whole expand and collapse animation)
        if (expandProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerCollapseHaptic()
                        IslandStateManager.collapse()
                    }
            )
        }



        // Expanded Cards Container: Positioned at animated top and horizontal offset
        if (expandProgress > 0.001f || isExpanded) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = animatedHorizontalOffset.roundToPx(),
                            y = animatedTopOffset.roundToPx()
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Cards Stack (Card 1 + Card 2)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(animatedCardSpacing),
                    modifier = if (isExpanded && primaryPriority != IslandPriority.FLASHLIGHT) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -15f) {
                                    triggerCollapseHaptic()
                                    IslandStateManager.collapse()
                                }
                            }
                        }
                    } else Modifier
                ) {
                    // Card 1: Primary Expanded Card
                    if (animatedCard1Width > 1.dp && (expandProgress > 0.001f || isExpanded)) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedCard1OffsetX.roundToInt(), 0) }
                                .width(animatedCard1Width)
                                .height(animatedCard1Height)
                                .graphicsLayer {
                                    alpha = cardContainerAlpha
                                }
                                .clip(islandShape(animatedCornerRadius))
                                .background(if (animatedCard1Width > 2.dp) Color.Black else Color.Transparent)
                                .then(
                                    if (settings.showOutline && animatedCard1Width > 8.dp) {
                                        Modifier.border(
                                            width = settings.outlineThicknessDp.dp,
                                            color = primaryOutlineColor,
                                            shape = islandShape(animatedCornerRadius)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isExpanded) {
                                        Modifier.pointerInput(primaryPriority) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    val threshold = 160f
                                                    when (primaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissMedia()
                                                                if (activeCount <= 1) {
                                                                    IslandStateManager.collapse()
                                                                }
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                val dismissed = IslandStateManager.dismissCurrentNotification()
                                                                if (dismissed && activeCount <= 1) {
                                                                    IslandStateManager.collapse()
                                                                }
                                                            }
                                                        }
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                val dismissed = IslandStateManager.dismissProgress()
                                                                if (dismissed && activeCount <= 1) {
                                                                    IslandStateManager.collapse()
                                                                }
                                                            }
                                                        }
                                                        else -> Unit
                                                    }
                                                    card1OffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    card1OffsetX = 0f
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    when (primaryPriority) {
                                                        IslandPriority.SCREEN_RECORDING -> {
                                                            card1OffsetX += dragAmount * 0.18f
                                                        }
                                                        else -> {
                                                            card1OffsetX += dragAmount
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle ambient gradient background or blurred album art for media player or flashlight
                            if (isExpanded) {
                                if (primaryPriority == IslandPriority.MEDIA && settings.blurredAlbumArtBackground && mediaArt != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(islandShape(animatedCornerRadius))
                                    ) {
                                        Image(
                                            bitmap = (blurredMediaArt ?: mediaArt).asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (blurredMediaArt == null) {
                                                        Modifier.blur(radius = 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                                    } else Modifier
                                                )
                                                .graphicsLayer {
                                                    scaleX = 1.35f
                                                    scaleY = 1.35f
                                                    alpha = expandProgress * 0.75f
                                                }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.38f))
                                        )
                                        // Black gradient coming from the bottom
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.40f),
                                                            Color.Black.copy(alpha = 0.82f),
                                                            Color.Black
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                } else {
                                    val ambientBrush = when {
                                        primaryPriority == IslandPriority.MEDIA && mediaAccentColor != Color.Transparent -> {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    mediaAccentColor.copy(alpha = 0.35f * expandProgress),
                                                    mediaAccentColor.copy(alpha = 0.12f * expandProgress),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                        primaryPriority == IslandPriority.FLASHLIGHT -> {
                                            val blurpleTint = Color(0xFFD2D6FF)
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    blurpleTint.copy(alpha = 0.06f * expandProgress),
                                                    blurpleTint.copy(alpha = 0.20f * expandProgress)
                                                )
                                            )
                                        }
                                        primaryPriority == IslandPriority.SCREEN_RECORDING -> {
                                            val redTint = Color(0xFFFF453A)
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    redTint.copy(alpha = 0.16f * expandProgress),
                                                    redTint.copy(alpha = 0.05f * expandProgress),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                        else -> null
                                    }
                                    if (ambientBrush != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(ambientBrush)
                                        )
                                    }
                                }
                            }

                            // Expanded Card 1 Content (e.g. Media Player or Primary Notification)
                            val expandedAlpha = if (isExpanded) {
                                ((expandProgress - 0.30f) / 0.70f).coerceIn(0f, 1f)
                            } else {
                                ((expandProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                            }

                            if (!isCharging && expandedAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.TopCenter, unbounded = true)
                                        .requiredSize(expandedCardWidth, card1ContentHeight)
                                        // Inner padding lives in each view (islandCardContentPadding)
                                        .graphicsLayer {
                                            alpha = expandedAlpha
                                            scaleX = 0.94f + (0.06f * expandedAlpha)
                                            scaleY = 0.94f + (0.06f * expandedAlpha)
                                            renderEffect = morphBlurEffect(expandedAlpha, 10.dp.toPx())
                                        }
                                ) {
                                    ExpandedCardContent(
                                        priority = primaryPriority,
                                        media = currentMedia,
                                        notification = if (primaryPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                        recording = recording,
                                        flashlightLevel = flashlightLevel,
                                        maxFlashlightLevel = maxFlashlightLevel,
                                        fpsInfo = fpsInfo,
                                        weatherInfo = weatherInfo,
                                        networkSpeedInfo = networkSpeedInfo,
                                        scale = settings.expandedScale,
                                        onCollapse = {
                                            triggerCollapseHaptic()
                                            IslandStateManager.collapse()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Secondary Action Card (smoothly expands and collapses in dual mode!)
                    if (animatedCard2Height > 1.dp) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(animatedCard2OffsetX.roundToInt(), animatedCard2OffsetY.roundToPx()) }
                                .width(animatedCard1Width)
                                .height(animatedCard2Height)
                                .graphicsLayer {
                                    alpha = animatedCard2Alpha
                                    scaleX = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                                    scaleY = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                                }
                                .clip(islandShape(card2CornerRadius))
                                .background(Color.Black)
                                .then(
                                    if (settings.showOutline) {
                                        Modifier.border(
                                            width = settings.outlineThicknessDp.dp,
                                            color = secondaryOutlineColor,
                                            shape = islandShape(card2CornerRadius)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isExpanded && secondaryPriority != null) {
                                        Modifier.pointerInput(secondaryPriority) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    val threshold = 160f
                                                    when (secondaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissMedia()
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissCurrentNotification()
                                                            }
                                                        }
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                triggerHaptic()
                                                                IslandStateManager.dismissProgress()
                                                            }
                                                        }
                                                        else -> Unit
                                                    }
                                                    card2OffsetX = 0f
                                                },
                                                onDragCancel = {
                                                    card2OffsetX = 0f
                                                },
                                                onHorizontalDrag = { _, dragAmount ->
                                                    when (secondaryPriority) {
                                                        IslandPriority.PROGRESS,
                                                        IslandPriority.SCREEN_RECORDING -> {
                                                            card2OffsetX += dragAmount * 0.18f
                                                        }
                                                        else -> {
                                                            card2OffsetX += dragAmount
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            // Subtle ambient gradient background or blurred album art for secondary card if it's the media player or flashlight
                            if (isExpanded) {
                                if (secondaryPriority == IslandPriority.MEDIA && settings.blurredAlbumArtBackground && mediaArt != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(islandShape(card2CornerRadius))
                                    ) {
                                        Image(
                                            bitmap = (blurredMediaArt ?: mediaArt).asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (blurredMediaArt == null) {
                                                        Modifier.blur(radius = 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                                    } else Modifier
                                                )
                                                .graphicsLayer {
                                                    scaleX = 1.35f
                                                    scaleY = 1.35f
                                                    alpha = animatedCard2Alpha * 0.75f
                                                }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.38f))
                                        )
                                        // Black gradient coming from the bottom
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.40f),
                                                            Color.Black.copy(alpha = 0.82f),
                                                            Color.Black
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                } else {
                                    val ambientBrush = when {
                                        secondaryPriority == IslandPriority.MEDIA && mediaAccentColor != Color.Transparent -> {
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    mediaAccentColor.copy(alpha = 0.35f * animatedCard2Alpha),
                                                    mediaAccentColor.copy(alpha = 0.12f * animatedCard2Alpha),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                        secondaryPriority == IslandPriority.FLASHLIGHT -> {
                                            val blurpleTint = Color(0xFFD2D6FF)
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    blurpleTint.copy(alpha = 0.06f * animatedCard2Alpha),
                                                    blurpleTint.copy(alpha = 0.20f * animatedCard2Alpha)
                                                )
                                            )
                                        }
                                        secondaryPriority == IslandPriority.SCREEN_RECORDING -> {
                                            val redTint = Color(0xFFFF453A)
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    redTint.copy(alpha = 0.16f * animatedCard2Alpha),
                                                    redTint.copy(alpha = 0.05f * animatedCard2Alpha),
                                                    Color.Transparent
                                                )
                                            )
                                        }
                                        else -> null
                                    }
                                    if (ambientBrush != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(ambientBrush)
                                        )
                                    }
                                }
                            }

                            if (animatedCard2Alpha > 0.15f && secondaryPriority != null) {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.TopCenter, unbounded = true)
                                        .requiredSize(
                                            expandedCardWidth,
                                            cardHeightFor(secondaryPriority, settings.expandedScale, notifBaseHeight)
                                        )
                                        .graphicsLayer {
                                            renderEffect = morphBlurEffect(animatedCard2Alpha, 10.dp.toPx())
                                        }
                                        // Inner padding lives in each view (islandCardContentPadding)
                                ) {
                                    ExpandedCardContent(
                                        priority = secondaryPriority,
                                        media = currentMedia,
                                        notification = if (secondaryPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                        recording = recording,
                                        flashlightLevel = flashlightLevel,
                                        maxFlashlightLevel = maxFlashlightLevel,
                                        fpsInfo = fpsInfo,
                                        weatherInfo = weatherInfo,
                                        networkSpeedInfo = networkSpeedInfo,
                                        scale = settings.expandedScale,
                                        onCollapse = {
                                            triggerCollapseHaptic()
                                            IslandStateManager.collapse()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Minimised Island Container: Positioned strictly at camera cutout level (settings.topOffsetDp.dp)
        // Shrinks horizontally to 0 width while the card expands downwards, and blossoms from 0 width when minimising!
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = animatedHorizontalOffset.roundToPx(),
                        y = settings.topOffsetDp.dp.roundToPx()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Smoothly fade in over the first frames where it is barely a circle emerging from the punch hole,
            // and smoothly fade out when shrinking back into the punch hole.
            // Applies to both single and dual activities!
            val circleThreshold = (settings.heightDp.dp * 0.95f).coerceAtLeast(10.dp)
            val emergenceProgress = (animatedCompactWidth / circleThreshold).coerceIn(0f, 1f)
            val compactPillAlpha = emergenceProgress * emergenceProgress * (3f - 2f * emergenceProgress)

            // 1. Auxiliary Circular Island Bubble: Placed on the LAYER BELOW (rendered first so main pill covers it!)
            // Fixed circular shape & size; emerges and retracts purely along the X axis from the middle of the punch hole (x = 0)
            if (isDual || animatedBubbleOffsetX > 1.dp) {
                val bubbleOffsetXPx = with(LocalDensity.current) { animatedBubbleOffsetX.roundToPx() }
                val baseBubbleAlpha = if (isExpanded) {
                    ((1f - expandProgress * 4f).coerceIn(0f, 1f))
                } else if (!isOccupied && !hasPersistent) {
                    0f
                } else {
                    1f
                }
                // When emerging from the punch hole, the secondary bubble also fades in with the main island
                // so no opaque circle sits exposed at the punch hole during the initial frames!
                val bubbleAlpha = baseBubbleAlpha * compactPillAlpha

                if (bubbleAlpha > 0.01f) {
                    val emergedDistance = animatedBubbleOffsetX - (compactMainHalf - bubbleSize / 2f)
                    val contentAlpha = ((emergedDistance - bubbleSize * 0.35f) / (bubbleSize * 0.55f)).coerceIn(0f, 1f) * bubbleAlpha

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x = bubbleOffsetXPx, y = 0) }
                            .size(bubbleSize)
                            .graphicsLayer {
                                alpha = bubbleAlpha
                                scaleX = bubblePressScale
                                scaleY = bubblePressScale
                            }
                            .clip(CircleShape)
                            .background(Color.Black)
                            .then(
                                if (settings.showOutline && animatedBubbleOffsetX > compactMainHalf * 0.7f) {
                                    Modifier.border(
                                        width = settings.outlineThicknessDp.dp,
                                        color = secondaryOutlineColor,
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                            .combinedClickable(
                                interactionSource = bubbleInteraction,
                                indication = null,
                                onClick = {
                                    triggerHaptic()
                                    IslandStateManager.expand(secondaryPriority)
                                },
                                onLongClick = {
                                    triggerLongPressHaptic()
                                    openAppForPriority(
                                        context = context,
                                        priority = secondaryPriority,
                                        media = currentMedia,
                                        notification = currentNotif,
                                        progress = currentProgress,
                                        onDismissNotification = {
                                            IslandStateManager.dismissCurrentNotification()
                                        }
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (contentAlpha > 0.05f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = contentAlpha },
                                contentAlignment = Alignment.Center
                            ) {
                                AuxiliaryBubbleContent(
                                    priority = secondaryPriority,
                                    media = currentMedia,
                                    notification = if (secondaryPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                    recording = recording,
                                    fpsInfo = fpsInfo,
                                    weatherInfo = weatherInfo,
                                    networkSpeedInfo = networkSpeedInfo
                                )
                            }
                        }
                    }
                }
            }

            // Liquid split: while the secondary bubble separates from the pill, a black "neck" with
            // concave sides joins them and thins until it snaps, like two drops pulling apart.
            // Only drawn during the split itself, so it costs nothing at rest.
            if (isDual && !isExpanded && animatedBubbleOffsetX > 1.dp && animatedCompactWidth > settings.heightDp.dp) {
                val pillH = settings.heightDp.dp
                val pillRight = animatedCompactWidth / 2f
                val gap = (animatedBubbleOffsetX - bubbleSize / 2f) - pillRight
                val snapGap = 5.dp
                // 1 while the shapes overlap, 0 once the gap reaches snapGap
                val neck = (1f - gap / snapGap).coerceIn(0f, 1f)
                // Both ends tuck inside the round caps so only the concave waist is visible
                val neckStart = pillRight - pillH * 0.3f
                val neckEnd = animatedBubbleOffsetX - bubbleSize * 0.2f
                if (neck > 0.02f && neckEnd > neckStart && gap > -bubbleSize * 0.6f) {
                    val neckCenterX = (neckStart + neckEnd) / 2f
                    Canvas(
                        modifier = Modifier
                            .offset { IntOffset(x = neckCenterX.roundToPx(), y = 0) }
                            .size(neckEnd - neckStart, pillH)
                            .graphicsLayer { alpha = compactPillAlpha }
                    ) {
                        val w = size.width
                        val cy = size.height / 2f
                        val edgeHalf = size.height * 0.42f
                        val waistHalf = (size.height / 2f) * neck * 0.85f
                        // Quadratic control point that puts the curve's midpoint at the waist
                        val ctrlTop = cy - 2f * waistHalf + edgeHalf
                        val ctrlBottom = cy + 2f * waistHalf - edgeHalf
                        val neckPath = Path().apply {
                            moveTo(0f, cy - edgeHalf)
                            quadraticTo(w / 2f, ctrlTop, w, cy - edgeHalf)
                            lineTo(w, cy + edgeHalf)
                            quadraticTo(w / 2f, ctrlBottom, 0f, cy + edgeHalf)
                            close()
                        }
                        drawPath(neckPath, Color.Black)
                    }
                }
            }

            // 2. Main Compact Pill: Rendered on top of the bubble (covers it when sliding near x = 0)
            if (animatedCompactWidth > 0.5.dp) {
                Box(
                    modifier = Modifier
                        .width(animatedCompactWidth)
                        .height(settings.heightDp.dp)
                        .graphicsLayer {
                            alpha = compactPillAlpha
                            translationY = idleBounceY.value.dp.toPx()
                            scaleX = idleBounceScale.value * pillPressScale * (1f + swapSquash.value)
                            scaleY = idleBounceScale.value * pillPressScale * (1f - swapSquash.value * 0.5f)
                        }
                        .clip(islandShape(settings.cornerRadiusDp.dp))
                        .background(if (animatedCompactWidth > 2.dp) Color.Black else Color.Transparent)
                        .then(
                            if (settings.showOutline && animatedCompactWidth > 8.dp) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = primaryOutlineColor.copy(alpha = primaryOutlineColor.alpha * compactPillAlpha),
                                    shape = islandShape(settings.cornerRadiusDp.dp)
                                )
                            } else Modifier
                        )
                        .pointerInput(isDual) {
                            if (!isDual) return@pointerInput
                            var dragTotal = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragTotal += amount
                                },
                                onDragEnd = {
                                    if (kotlin.math.abs(dragTotal) > 32.dp.toPx() && !isExpanded) {
                                        triggerHaptic()
                                        dualSwapped = !dualSwapped
                                    }
                                }
                            )
                        }
                        .combinedClickable(
                            interactionSource = pillInteraction,
                            indication = null,
                            onClick = {
                                if (!isExpanded && (isOccupied || hasPersistent) && !isUnlock && !isBluetooth && !isRingerMode) {
                                    triggerHaptic()
                                    IslandStateManager.expand(primaryPriority)
                                } else if (!isExpanded && !isOccupied && !hasPersistent && settings.alwaysShowPill) {
                                    if (settings.hapticsEnabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                    }
                                    coroutineScope.launch {
                                        launch {
                                            idleBounceY.snapTo(0f)
                                            idleBounceY.animateTo(
                                                targetValue = 3.5f,
                                                animationSpec = tween(70, easing = FastOutSlowInEasing)
                                            )
                                            idleBounceY.animateTo(
                                                targetValue = -1.5f,
                                                animationSpec = tween(90, easing = FastOutSlowInEasing)
                                            )
                                            idleBounceY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            idleBounceScale.snapTo(1f)
                                            idleBounceScale.animateTo(
                                                targetValue = 0.95f,
                                                animationSpec = tween(70, easing = FastOutSlowInEasing)
                                            )
                                            idleBounceScale.animateTo(
                                                targetValue = 1.04f,
                                                animationSpec = tween(90, easing = FastOutSlowInEasing)
                                            )
                                            idleBounceScale.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isExpanded && !isUnlock && !isBluetooth && !isRingerMode) {
                                    triggerLongPressHaptic()
                                    openAppForPriority(
                                        context = context,
                                        priority = primaryPriority,
                                        media = currentMedia,
                                        notification = currentNotif,
                                        progress = currentProgress,
                                        onDismissNotification = {
                                            IslandStateManager.dismissCurrentNotification()
                                        }
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val compactAlpha = if (isExpanded) {
                        (1f - (expandProgress * 3.0f)).coerceIn(0f, 1f)
                    } else {
                        (1f - (expandProgress * 1.8f)).coerceIn(0f, 1f)
                    }

                    // Background darker green progress bar for charging status:
                    if (isCharging || chargingContentAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = chargingContentAlpha }
                                .background(Color(0xFF04190B))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = animatedChargingProgress.coerceIn(0.001f, 1f))
                                .align(Alignment.CenterStart)
                                .graphicsLayer { alpha = chargingContentAlpha }
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF062B12),
                                            Color(0xFF0D3D1B)
                                        )
                                    )
                                )
                        )
                    }

                    // 1. Compact Content or Charging Content: strictly clear 36dp center over punch-hole
                    if (isCharging || chargingContentAlpha > 0.01f) {
                        val wattage = charging?.wattage
                        val chargingLabel = if (wattage != null && wattage >= 1.0) {
                            if (wattage >= 10.0 || (wattage % 1.0) < 0.05 || (wattage % 1.0) > 0.95) {
                                "${wattage.roundToInt()}W"
                            } else {
                                String.format(Locale.US, "%.1fW", wattage)
                            }
                        } else {
                            "Charging"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                                .graphicsLayer { alpha = chargingContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Wattage (e.g. "33W") or "Charging" to save extreme space
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = chargingLabel,
                                    color = Color(0xFF30D158),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(36.dp))

                            // Right: Current battery percentage in green
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "${charging?.percentage ?: 100}%",
                                    color = Color(0xFF30D158),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    } else if (isUnlock || unlockContentAlpha > 0.01f) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .graphicsLayer { alpha = unlockContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Unlocked lock icon at the slight edge of the island
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unlocked",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(36.dp))

                            // Right: Checkmark at the slight edge of the island with smooth bounce growth animation
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (unlockCheckmarkAlpha > 0.01f) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = unlockCheckmarkAlpha
                                                scaleX = unlockCheckmarkScale
                                                scaleY = unlockCheckmarkScale
                                            }
                                            .size(19.dp)
                                    )
                                }
                            }
                        }
                    } else if (isBluetooth || bluetoothContentAlpha > 0.01f) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .graphicsLayer { alpha = bluetoothContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Bluetooth icon at the slight edge of the island
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = "Bluetooth",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(36.dp))

                            // Right: Checkmark at the slight edge of the island with smooth bounce growth animation
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (bluetoothCheckmarkAlpha > 0.01f) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Connected",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = bluetoothCheckmarkAlpha
                                                scaleX = bluetoothCheckmarkScale
                                                scaleY = bluetoothCheckmarkScale
                                            }
                                            .size(19.dp)
                                    )
                                }
                            }
                        }
                    } else if (isRingerMode || ringerContentAlpha > 0.01f) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .graphicsLayer { alpha = ringerContentAlpha },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Ringer icon (Bell, Slashed Bell, or Vibration)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val icon = when (displayRingerMode) {
                                    RingerMode.SILENT -> Icons.Default.NotificationsOff
                                    RingerMode.VIBRATE -> Icons.Default.Vibration
                                    RingerMode.RING -> Icons.Default.Notifications
                                }
                                val iconTint = if (displayRingerMode == RingerMode.SILENT) {
                                    Color(0xFFFF3B30) // Red icon for silent
                                } else {
                                    Color.White
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = displayRingerMode.name,
                                    tint = iconTint,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            if (displayRingerMode == RingerMode.RING) {
                                                transformOrigin = TransformOrigin(0.5f, 0.0f)
                                                rotationZ = bellShakeRotation.value
                                            } else if (displayRingerMode == RingerMode.VIBRATE) {
                                                transformOrigin = TransformOrigin.Center
                                                translationX = vibrateShakeOffset.value.dp.toPx()
                                            } else {
                                                transformOrigin = TransformOrigin.Center
                                            }
                                        }
                                        .size(19.dp)
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(36.dp))

                            // Right: Text label (Silent, Vibrate, Ring) with smooth bounce scaling & alpha
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (ringerSecondaryAlpha > 0.01f) {
                                    val labelText = when (displayRingerMode) {
                                        RingerMode.SILENT -> "Silent"
                                        RingerMode.VIBRATE -> "Vibrate"
                                        RingerMode.RING -> "Ring"
                                    }
                                    val labelColor = if (displayRingerMode == RingerMode.SILENT) {
                                        Color(0xFFFF3B30) // Red text for silent
                                    } else {
                                        Color.White
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = ringerSecondaryAlpha
                                            scaleX = ringerSecondaryScale
                                            scaleY = ringerSecondaryScale
                                        }
                                    ) {
                                        Text(
                                            text = labelText,
                                            color = labelColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    } else if (compactAlpha > 0.01f && animatedCompactWidth > 24.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = compactAlpha },
                            contentAlignment = Alignment.Center
                        ) {
                            // Switching activity: old content shrinks and blurs out while the new one
                            // springs in sharp, instead of swapping instantly
                            AnimatedContent(
                                targetState = primaryPriority,
                                transitionSpec = {
                                    (fadeIn(tween(200, delayMillis = 50)) +
                                        scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f))) togetherWith
                                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120))) using
                                        SizeTransform(clip = false)
                                },
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                                label = "compactContentSwap"
                            ) { shownPriority ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            renderEffect = morphBlurEffect(1f - swapBlur.value, 12.dp.toPx())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CompactContent(
                                        priority = shownPriority,
                                        media = currentMedia,
                                        notification = if (shownPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                        recording = recording,
                                        flashlightLevel = flashlightLevel,
                                        maxFlashlightLevel = maxFlashlightLevel,
                                        fpsInfo = fpsInfo,
                                        weatherInfo = weatherInfo,
                                        networkSpeedInfo = networkSpeedInfo,
                                        secondaryPriority = secondaryPriority,
                                        horizontalInset = ((settings.heightDp.dp - 20.dp) / 2f).coerceIn(6.dp, 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Parentheses Accents: Neat parentheses-shaped brackets on each side of the island
            // Shorter than the height of the island, smoothly positioning symmetrically around 1 or 2 activities, and neatly close to the edges.
            // Hidden while two activities share the island (main pill + secondary bubble)
            val targetParenthesisAlpha = if ((!isOccupied && !hasPersistent) || isDual) 0f else ((1f - expandProgress * 3.5f).coerceIn(0f, 1f))
            val parenthesisAlphaAnim by animateFloatAsState(
                targetValue = targetParenthesisAlpha,
                animationSpec = tween(durationMillis = (130 / speed).roundToInt(), easing = FastOutSlowInEasing),
                label = "parenthesisAlpha"
            )

            // Fades in a split second earlier as the island begins emerging from the punch hole
            val parenthesisEmergenceAlpha = (animatedCompactWidth / 14.dp).coerceIn(0f, 1f)
            val effectiveParenthesisAlpha = parenthesisAlphaAnim * parenthesisEmergenceAlpha

            if (settings.bubbleEffectEnabled && effectiveParenthesisAlpha > 0.01f && animatedCompactWidth > 3.dp) {
                val parenthesisHeight = (settings.heightDp.dp * 0.52f).coerceIn(16.dp, 21.dp)
                val extraShootDp = parenthesisExtraShoot.value.coerceAtLeast(0f).dp
                val parenthesisWidth = 5.0.dp + (extraShootDp * 0.20f)
                val parenthesisGap = 3.5.dp
                val parenthesisStroke = 1.8.dp
                val density = LocalDensity.current

                val pYPx = with(density) { ((settings.heightDp.dp - parenthesisHeight) / 2f).roundToPx() }
                val tipOffsetFromCenter = (parenthesisWidth - parenthesisStroke) / 2f

                // Left parenthesis `(`: tips facing right are at centerX + tipOffsetFromCenter.
                // Sits at islandLeft - parenthesisGap - extraShootDp.
                val islandLeftDp = -animatedCompactWidth / 2f
                val leftCenterXPx = with(density) {
                    (islandLeftDp - parenthesisGap - extraShootDp - tipOffsetFromCenter).roundToPx()
                }

                // Right parenthesis `)`: tips facing left are at centerX - tipOffsetFromCenter.
                // Sits at rightContentEdgeDp + parenthesisGap + extraShootDp.
                // When emerging, curve shoots out slightly further from the island and settles back.
                val bubbleRightEdge = animatedBubbleOffsetX + (bubbleSize / 2f)
                val rightContentEdgeDp = if (!isCharging && !isUnlock && !isBluetooth && !isRingerMode && animatedBubbleOffsetX > 1.dp) {
                    maxOf(animatedCompactWidth / 2f, bubbleRightEdge)
                } else {
                    animatedCompactWidth / 2f
                }
                val rightCenterXPx = with(density) {
                    (rightContentEdgeDp + parenthesisGap + extraShootDp + tipOffsetFromCenter).roundToPx()
                }

                val bracketColor = Color.Black

                // Left Parenthesis `(`
                ParenthesisBracket(
                    isLeft = true,
                    width = parenthesisWidth,
                    height = parenthesisHeight,
                    strokeWidth = parenthesisStroke,
                    color = bracketColor,
                    modifier = Modifier
                        .offset { IntOffset(x = leftCenterXPx, y = pYPx) }
                        .graphicsLayer { alpha = effectiveParenthesisAlpha }
                )

                // Right Parenthesis `)`
                ParenthesisBracket(
                    isLeft = false,
                    width = parenthesisWidth,
                    height = parenthesisHeight,
                    strokeWidth = parenthesisStroke,
                    color = bracketColor,
                    modifier = Modifier
                        .offset { IntOffset(x = rightCenterXPx, y = pYPx) }
                        .graphicsLayer { alpha = effectiveParenthesisAlpha }
                )
            }
        }
    }
}

fun launchAppOrIntent(
    context: android.content.Context,
    packageName: String?,
    fallbackPendingIntent: PendingIntent? = null
): Boolean {
    // 1. If it is an app, open it directly via its launcher intent with NEW_TASK
    if (!packageName.isNullOrBlank()) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                context.startActivity(launchIntent)
                return true
            }
        } catch (e: Throwable) {
            Log.e("IslandView", "Failed to launch app by package name: $packageName", e)
        }
    }

    // 2. If it is not a launchable app or launchIntent failed, try the PendingIntent with background activity start allowed
    if (fallbackPendingIntent != null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }.toBundle()
                fallbackPendingIntent.send(context, 0, null, null, null, null, options)
            } else {
                fallbackPendingIntent.send()
            }
            return true
        } catch (e: Throwable) {
            Log.e("IslandView", "Failed to send pendingIntent", e)
        }
    }

    return false
}

fun openAppForPriority(
    context: android.content.Context,
    priority: IslandPriority?,
    media: MediaInfo?,
    notification: NotificationInfo?,
    progress: NotificationInfo?,
    onDismissNotification: (() -> Unit)? = null
) {
    when (priority) {
        IslandPriority.POP_NOTIFICATION -> {
            if (notification != null) {
                launchAppOrIntent(context, notification.packageName, notification.contentIntent)
                onDismissNotification?.invoke()
            }
        }
        IslandPriority.PROGRESS -> {
            if (progress != null) {
                launchAppOrIntent(context, progress.packageName, progress.contentIntent)
            }
        }
        IslandPriority.MEDIA -> {
            if (media != null) {
                launchAppOrIntent(context, media.packageName, media.sessionActivity)
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
    fpsInfo: FpsInfo = FpsInfo(),
    weatherInfo: WeatherInfo = WeatherInfo(),
    networkSpeedInfo: NetworkSpeedInfo = NetworkSpeedInfo()
) {
    when (priority) {
        IslandPriority.SCREEN_RECORDING -> {
            PulsingRecordingDot()
        }
        IslandPriority.POP_NOTIFICATION -> {
            if (notification?.iconBitmap != null) {
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
                        color = Color(0xFFE5E5EA),
                        trackColor = Color(0xFF2C2C2E)
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFE5E5EA),
                        trackColor = Color(0xFF2C2C2E)
                    )
                    Text(
                        text = "${(pct * 100).roundToInt()}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
        IslandPriority.FPS -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${fpsInfo.fps}",
                    color = Color(0xFF30D158),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        IslandPriority.WEATHER -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${weatherInfo.temperatureC}°",
                    color = Color(0xFFFF9F0A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        IslandPriority.NETWORK_SPEED -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color(0xFF64D2FF),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        else -> Unit
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
    fpsInfo: FpsInfo = FpsInfo(),
    weatherInfo: WeatherInfo = WeatherInfo(),
    networkSpeedInfo: NetworkSpeedInfo = NetworkSpeedInfo(),
    secondaryPriority: IslandPriority? = null,
    horizontalInset: Dp = 10.dp
) {
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
                    if (notification?.iconBitmap != null) {
                        Image(
                            bitmap = notification.iconBitmap.asImageBitmap(),
                            contentDescription = "Notification Icon",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFE5E5EA),
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
                                tint = Color(0xFFE5E5EA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = notification?.appName ?: "Download",
                                color = Color(0xFFE5E5EA),
                                fontSize = IslandTokens.TypeCaption,
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
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                IslandPriority.FPS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "FPS",
                            tint = if (fpsInfo.isSmooth) Color(0xFF30D158) else Color(0xFFFFD60A),
                            modifier = Modifier.size(15.dp)
                        )
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FPS",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IslandPriority.WEATHER -> {
                    val weatherIcon = when {
                        weatherInfo.condition.contains("Sun", ignoreCase = true) || weatherInfo.condition.contains("Clear", ignoreCase = true) -> Icons.Default.WbSunny
                        weatherInfo.condition.contains("Rain", ignoreCase = true) -> Icons.Default.WaterDrop
                        else -> Icons.Default.Cloud
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = "Weather",
                            tint = Color(0xFFFF9F0A),
                            modifier = Modifier.size(15.dp)
                        )
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = weatherInfo.condition,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IslandPriority.NETWORK_SPEED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (networkSpeedInfo.isWifi) Icons.Default.Wifi else Icons.Default.ArrowDownward,
                            contentDescription = "Speed",
                            tint = Color(0xFF64D2FF),
                            modifier = Modifier.size(14.dp)
                        )
                        if (!isCompactDual) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (networkSpeedInfo.isWifi) "Wi-Fi" else "5G",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IslandPriority.IDLE -> Unit
            }
        }

        // Camera punch-hole deadzone: STRICTLY 36dp EMPTY SPACE IN THE MIDDLE!
        // No UI elements will EVER enter or overlap the camera cutout area
        Spacer(modifier = Modifier.width(36.dp))

        // Trailing content: Pinned strictly to the right of the camera punch hole
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (priority) {
                IslandPriority.SCREEN_RECORDING -> {
                    val minutes = recording.durationSeconds / 60
                    val seconds = recording.durationSeconds % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        color = Color(0xFFE5E5EA),
                        fontSize = IslandTokens.TypeCaption,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.FLASHLIGHT -> Unit

                IslandPriority.POP_NOTIFICATION -> {
                    val liveTimer = rememberLiveTimerText(notification)
                    Text(
                        text = liveTimer ?: notification?.title ?: "Notification",
                        color = Color(0xFFE5E5EA),
                        fontSize = IslandTokens.TypeCaption,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IslandPriority.PROGRESS -> {
                    val pct = notification?.progressFraction ?: 0f
                    val isIndet = notification?.isIndeterminate == true
                    if (isIndet) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFE5E5EA),
                            trackColor = Color(0xFF2C2C2E)
                        )
                    } else {
                        Text(
                            text = "${(pct * 100).roundToInt()}%",
                            color = Color(0xFFE5E5EA),
                            fontSize = IslandTokens.TypeCaption,
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
                        lerp(Color(0xFFE5E5EA), compactAccent, 0.50f)
                    } else {
                        Color(0xFFE5E5EA)
                    }
                    EqualizerVisualizer(isPlaying = media?.isPlaying == true, barColor = compactEqTint)
                }

                IslandPriority.FPS -> {
                    Text(
                        text = "${fpsInfo.fps} FPS",
                        color = if (fpsInfo.isSmooth) Color(0xFF30D158) else Color(0xFFFFD60A),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.WEATHER -> {
                    Text(
                        text = "${weatherInfo.temperatureC}°",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IslandPriority.NETWORK_SPEED -> {
                    Text(
                        text = "↓ ${networkSpeedInfo.downloadSpeedFormatted}",
                        color = Color(0xFF64D2FF),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
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
    fpsInfo: FpsInfo = FpsInfo(),
    weatherInfo: WeatherInfo = WeatherInfo(),
    networkSpeedInfo: NetworkSpeedInfo = NetworkSpeedInfo(),
    scale: Float = 1.0f,
    onCollapse: () -> Unit
) {
    when (priority) {
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
        IslandPriority.FPS -> {
            ExpandedFpsView(fpsInfo = fpsInfo, onCollapse = onCollapse, scale = scale)
        }
        IslandPriority.WEATHER -> {
            ExpandedWeatherView(weatherInfo = weatherInfo, onCollapse = onCollapse, scale = scale)
        }
        IslandPriority.NETWORK_SPEED -> {
            ExpandedNetworkSpeedView(networkSpeedInfo = networkSpeedInfo, onCollapse = onCollapse, scale = scale)
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
    val view = LocalView.current

    // Current brightness level 1..4
    var currentLevel by remember(level) { mutableIntStateOf(level.coerceIn(1, 4)) }
    LaunchedEffect(level) {
        currentLevel = level.coerceIn(1, 4)
    }

    val currentLevelState = rememberUpdatedState(currentLevel)
    val onSetLevelState = rememberUpdatedState(onSetLevel)

    // Animated fraction for the light beam height (1 -> 0.25f, 4 -> 1.0f)
    val targetFraction = (currentLevel / 4f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 450f
        ),
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
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
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

                val blurpleTint = Color(0xFFD2D6FF)

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
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onTurnOff()
                    onCollapse()
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

    var livePosition by remember(media?.packageName, media?.title) {
        mutableLongStateOf(media?.computeCurrentPosition() ?: 0L)
    }

    LaunchedEffect(media?.isPlaying, media?.positionMs, media?.lastPositionUpdateTimeMs, isSeeking, isSeekPending) {
        if (!isSeeking && !isSeekPending) {
            livePosition = media?.computeCurrentPosition() ?: 0L
            if (media?.isPlaying == true) {
                while (isActive) {
                    kotlinx.coroutines.delay(250)
                    if (!isSeeking && !isSeekPending) {
                        livePosition = media?.computeCurrentPosition() ?: 0L
                    }
                }
            }
        }
    }

    val currentPosition = when {
        isSeeking -> if (hasValidDuration) (scrubFraction * duration).toLong().coerceIn(0L, duration) else livePosition
        else -> livePosition
    }

    val sliderProgress = when {
        isSeeking -> scrubFraction
        hasValidDuration -> (livePosition.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }

    val displayArt = media?.albumArt ?: media?.appIcon
    val mediaAccentColor = remember(displayArt) { extractMediaAccentColor(displayArt) }

    val controlsTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.40f)
    } else {
        Color(0xFFE5E5EA)
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
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.50f)
    } else {
        Color(0xFFE5E5EA)
    }

    val sliderActiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFFE5E5EA), mediaAccentColor, 0.55f)
    } else {
        Color(0xFFE5E5EA)
    }

    val sliderInactiveTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFF2C2C2E), mediaAccentColor, 0.22f)
    } else {
        Color(0xFF2C2C2E)
    }

    val durationTextTint = if (mediaAccentColor != Color.Transparent) {
        lerp(Color(0xFF8E8E93), mediaAccentColor, 0.35f)
    } else {
        Color(0xFF8E8E93)
    }

    // Time labels use tabular digits and a fixed width sized for the longest possible value,
    // so the seek bar keeps its length while the time ticks or is scrubbed.
    val timeLabelStyle = LocalTextStyle.current.merge(
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    )
    val timeTextMeasurer = rememberTextMeasurer()
    val timeLabelTemplate = formatDuration(duration).replace(Regex("[0-9]"), "0")
    val timeLabelWidth = with(LocalDensity.current) {
        remember(timeLabelTemplate, timeLabelStyle) {
            maxOf(
                timeTextMeasurer.measure(timeLabelTemplate, timeLabelStyle).size.width,
                timeTextMeasurer.measure("--:--", timeLabelStyle).size.width
            ).toDp()
        }
    }

    val thumbWidth by animateDpAsState(
        targetValue = if (isSeeking) 5.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "seekThumbWidth"
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (isSeeking) 18.dp else 14.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "seekThumbHeight"
    )

    val artSize = (54.dp * scale).coerceIn(40.dp, 64.dp)
    val titleSize = (17 * scale).coerceIn(14f, 19f).sp
    val artistSize = (13 * scale).coerceIn(11.5f, 15f).sp
    val sliderGapTop = (IslandTokens.SpaceSm * scale).coerceIn(4.dp, 10.dp)
    val sliderGapBottom = (IslandTokens.SpaceXs * scale).coerceIn(2.dp, 8.dp)
    val prevNextSize = (42.dp * scale).coerceIn(34.dp, 48.dp)
    val prevNextIcon = (24.dp * scale).coerceIn(18.dp, 28.dp)
    val playBtnSize = (50.dp * scale).coerceIn(40.dp, 58.dp)
    val playBtnIcon = (30.dp * scale).coerceIn(22.dp, 36.dp)
    val controlsGap = (20.dp * scale).coerceIn(12.dp, 26.dp)

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
                            launchAppOrIntent(context, media?.packageName, media?.sessionActivity)
                            onCollapse()
                        },
                    placeholder = {
                        Icon(
                            imageVector = Icons.Default.Radio,
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
            Text(
                text = formatDuration(currentPosition),
                color = durationTextTint,
                style = timeLabelStyle,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier.width(timeLabelWidth)
            )

            Spacer(modifier = Modifier.width(IslandTokens.SpaceMd))

            Slider(
                value = sliderProgress,
                enabled = hasValidDuration,
                onValueChange = { frac ->
                    isSeeking = true
                    scrubFraction = frac
                },
                onValueChangeFinished = {
                    if (hasValidDuration) {
                        val seekTargetMs = (scrubFraction * duration).toLong().coerceIn(0L, duration)
                        livePosition = seekTargetMs
                        isSeekPending = true
                        isSeeking = false
                        IslandStateManager.onSeekTo?.invoke(seekTargetMs)
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(650)
                            isSeekPending = false
                        }
                    } else {
                        isSeeking = false
                    }
                },
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

            Text(
                text = if (hasValidDuration) formatDuration(duration) else "--:--",
                color = durationTextTint,
                style = timeLabelStyle,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(timeLabelWidth)
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
                IconButton(
                    onClick = { IslandStateManager.onSkipPrevious?.invoke() },
                    modifier = Modifier.size(prevNextSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
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
                            (fadeIn(tween(140)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f))) togetherWith
                                (fadeOut(tween(100)) + scaleOut(targetScale = 0.6f, animationSpec = tween(100)))
                        },
                        label = "playPauseIcon"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
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
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next",
                        tint = controlsTint,
                        modifier = Modifier.size(prevNextIcon)
                    )
                }
            }


        }
    }
}

@Composable
fun ExpandedNotificationView(notification: NotificationInfo?, onCollapse: () -> Unit, scale: Float = 1.0f) {
    val context = LocalContext.current
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    val replyAction = remember(notification) {
        notification?.actions?.firstOrNull { it.isReply || it.title.contains("Reply", ignoreCase = true) }
    }

    fun openNotification() {
        IslandStateManager.setKeyboardFocused(false)
        val pendingIntent = notification?.contentIntent
        var launched = false
        if (pendingIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }.toBundle()
                    pendingIntent.send(context, 0, null, null, null, null, options)
                } else {
                    pendingIntent.send()
                }
                launched = true
            } catch (e: Exception) {
                Log.e("ExpandedNotification", "Failed to send contentIntent", e)
            }
        }
        if (!launched && !notification?.packageName.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
            }
        }
        onCollapse()
    }

    fun sendReply(text: String) {
        if (text.isBlank() || notification == null) return
        IslandStateManager.onSendReply?.invoke(notification.key, text.trim())
        IslandStateManager.setKeyboardFocused(false)
        IslandStateManager.dismissCurrentNotification()
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
                    // App icon (tap to open app)
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                            .background(IslandTokens.SurfaceRaised)
                            .clickable {
                                launchAppOrIntent(context, notification?.packageName, notification?.contentIntent)
                                onCollapse()
                            },
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
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = IslandTokens.ActionPrimaryContainer,
                                modifier = Modifier.size((24.dp * scale).coerceIn(18.dp, 28.dp))
                            )
                        }
                    }
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
                // Smart reply choices chips
                val smartChoices = remember(replyAction) {
                    val choices = replyAction?.remoteInputChoices ?: emptyList()
                    if (choices.isNotEmpty()) choices else listOf("OK", "Sure", "Thanks!")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    smartChoices.take(3).forEach { choice ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2C2C2E))
                                .clickable { sendReply(choice) }
                                .padding(horizontal = 10.dp, vertical = metrics.replyChipVerticalPadding)
                        ) {
                            Text(
                                text = choice,
                                color = Color(0xFFE5E5EA),
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
                        .background(Color(0xFF212121))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    IslandStateManager.setKeyboardFocused(true)
                                }
                            },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFFE5E5EA)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendReply(replyText) }),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = replyAction?.remoteInputLabel ?: "Type a reply...",
                                    color = Color(0xFF71717A),
                                    fontSize = 14.sp
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
                            tint = if (replyText.isNotBlank()) Color.White else Color(0xFF52525B),
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
                            tint = Color(0xFF71717A),
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
                                    runCatching { action.actionIntent?.send() }
                                    IslandStateManager.dismissCurrentNotification()
                                    onCollapse()
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
    val percentageText = if (isIndet) "" else "${(progressFraction * 100).roundToInt()}%"

    fun openProgressApp() {
        launchAppOrIntent(context, notification?.packageName, notification?.contentIntent)
        onCollapse()
    }

    val iconSize = (40.dp * scale).coerceIn(32.dp, 48.dp)
    val iconGap = (12.dp * scale).coerceIn(8.dp, 16.dp)
    val titleSize = (17 * scale).coerceIn(14f, 19f).sp
    val appNameSize = (13 * scale).coerceIn(11.5f, 15f).sp
    val vertGap = (IslandTokens.SpaceSm * scale).coerceIn(6.dp, 12.dp)
    val bodySize = (15 * scale).coerceIn(13f, 17f).sp
    val bodyLineHeight = (20 * scale).coerceIn(17f, 22f).sp
    val buttonHeight = (42.dp * scale).coerceIn(36.dp, 48.dp)
    val buttonFontSize = (13.5f * scale).coerceIn(12f, 15f).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.PROGRESS, scale),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Header + Detail text + Progress bar
        Column(modifier = Modifier.fillMaxWidth()) {
            IslandHeader(
                title = notification?.title?.ifBlank { "Downloading" } ?: "Downloading",
                subtitle = notification?.appName ?: "",
                titleSize = titleSize,
                subtitleSize = appNameSize,
                subtitleSpacing = (IslandTokens.SpaceXxs * scale).coerceIn(1.dp, 3.dp),
                titleMarquee = true,
                icon = {
                    // App icon (tap to open app)
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                            .background(IslandTokens.SurfaceMuted)
                            .clickable {
                                openProgressApp()
                            },
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
                },
                trailing = {
                    if (!isIndet) {
                        InfoChip(text = percentageText)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = IslandTokens.ActionPrimaryContainer,
                            trackColor = IslandTokens.SurfaceRaised
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(vertGap))

            val rawProgressText = notification?.text ?: "In progress..."
            val displayProgressText = remember(rawProgressText) {
                var s = rawProgressText.trim()
                while (s.startsWith("•") || s.startsWith("·") || s.startsWith("-")) {
                    s = s.substring(1).trim()
                }
                while (s.endsWith("•") || s.endsWith("·") || s.endsWith("-")) {
                    s = s.substring(0, s.length - 1).trim()
                }
                s.ifBlank { "In progress..." }
            }

            Text(
                text = displayProgressText,
                color = Color(0xFFF4F4F5),
                fontSize = bodySize,
                lineHeight = bodyLineHeight,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = iconSize + iconGap)
                    .clickable { openProgressApp() }
            )

            Spacer(modifier = Modifier.height(vertGap))

            // Linear Progress Bar
            if (isIndet) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE5E5EA),
                    trackColor = Color(0xFF2C2C2E)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE5E5EA),
                    trackColor = Color(0xFF2C2C2E)
                )
            }
        }

        Spacer(modifier = Modifier.height((IslandTokens.SpaceLg * scale).coerceIn(12.dp, 24.dp)))

        val actionItems = remember(notification) {
            val list = mutableListOf<IslandActionItem>()
            // Drop redundant actions: tapping the icon/body already opens the app
            val notifActions = (notification?.actions ?: emptyList()).filterNot {
                it.title.toString().equals("Open", ignoreCase = true)
            }
            notifActions.take(2).forEach { action ->
                list.add(
                    IslandActionItem(
                        title = action.title,
                        isPrimary = false,
                        onClick = {
                            runCatching { action.actionIntent?.send() }
                        }
                    )
                )
            }
            list
        }

        IslandActionButtonRow(
            items = actionItems,
            height = buttonHeight,
            fontSize = buttonFontSize
        )
    }
}

@Composable
fun ExpandedRecordingView(recording: ScreenRecordingState, onCollapse: () -> Unit, scale: Float = 1.0f) {
    val minutes = recording.durationSeconds / 60
    val seconds = recording.durationSeconds % 60
    val timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val timerSize = (22 * scale).coerceIn(18f, 26f).sp
    val buttonHeight = (42.dp * scale).coerceIn(36.dp, 48.dp)
    val buttonFontSize = (13.5f * scale).coerceIn(12f, 15f).sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.SCREEN_RECORDING, scale),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            PulsingRecordingDot(modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(IslandTokens.SpaceMd))
            Text(
                text = timerText,
                color = Color.White,
                fontSize = timerSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leftShape = RoundedCornerShape(
                topStart = 14.dp,
                bottomStart = 14.dp,
                topEnd = 4.dp,
                bottomEnd = 4.dp
            )
            FilledTonalButton(
                onClick = {
                    IslandStateManager.onTogglePauseRecording?.invoke()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                shape = leftShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IslandTokens.ActionSecondaryContainer,
                    contentColor = Color(0xFFF4F4F5)
                )
            ) {
                Icon(
                    imageVector = if (recording.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (recording.isPaused) "Resume" else "Pause",
                    fontSize = buttonFontSize,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val rightShape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = 14.dp,
                bottomEnd = 14.dp
            )
            FilledTonalButton(
                onClick = {
                    IslandStateManager.onStopRecording?.invoke()
                    onCollapse()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                shape = rightShape,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IslandTokens.ActionSecondaryContainer,
                    contentColor = Color(0xFFFF453A)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stop",
                    fontSize = buttonFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF453A)
                )
            }
        }
    }
}

@Composable
fun ExpandedIdleView(onCollapse: () -> Unit, scale: Float = 1.0f) {
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
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ready for Media, Notifications & Recording",
            color = Color(0xFFA1A1AA),
            fontSize = 12.sp
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
            Text("Collapse", fontSize = 12.sp)
        }
    }
}

@Composable
fun PulsingRecordingDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(11.dp)
            .scale(1f + (alpha * 0.12f))
            .clip(CircleShape)
            .background(Color(0xFFFF3B30).copy(alpha = alpha))
    )
}

@Composable
fun EqualizerVisualizer(isPlaying: Boolean, barColor: Color = Color(0xFFE5E5EA)) {
    if (!isPlaying) {
        // Zero CPU usage when paused: render static equalizer bars without triggering infinite frame recompositions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * 0.3f).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * 0.5f).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16 * 0.2f).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "eq_bars")

    val h1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    val currentH1 = if (isPlaying) h1 else 0.3f
    val currentH2 = if (isPlaying) h2 else 0.5f
    val currentH3 = if (isPlaying) h3 else 0.2f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH1).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH2).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * currentH3).dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
    }
}

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

@Composable
fun CellularSignalBars(
    level: Int,
    modifier: Modifier = Modifier
) {
    val activeLevel = level.coerceIn(0, 4)
    Row(
        modifier = modifier.height(11.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        val barHeights = listOf(3.5.dp, 6.dp, 8.5.dp, 11.dp)
        for (i in 0 until 4) {
            val isLit = activeLevel > i || (activeLevel == 0 && i == 0)
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeights[i])
                    .clip(RoundedCornerShape(0.8.dp))
                    .background(if (isLit) Color.White else Color.White.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun ExpandedFpsView(
    fpsInfo: FpsInfo,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val targetHz = fpsInfo.refreshRateHz.coerceAtLeast(60)
    val fpsFraction = (fpsInfo.fps.toFloat() / targetHz).coerceIn(0f, 1f)
    val isSmooth = fpsInfo.isSmooth
    val statusColor = if (isSmooth) Color(0xFF30D158) else Color(0xFFFFD60A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.FPS, scale)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Feature icon + Title / Subtitle + Top-Right Status Chip
        IslandHeader(
            title = "Display Frame Rate",
            subtitle = "${targetHz} Hz Refresh Target",
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                        .background(IslandTokens.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "FPS",
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailing = {
                InfoChip(
                    text = "${fpsInfo.fps} FPS",
                    dotColor = statusColor
                )
            }
        )

        // Center section: Minimal horizontal frame timing progress bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frame Pacing",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isSmooth) "Smooth" else "Drop Detected",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fpsFraction)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }

        // Bottom section: Clean inline metadata
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "Latency: %.1f ms", fpsInfo.frameTimeMs),
                color = Color(0xFFA1A1AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${targetHz} Hz Display",
                color = Color(0xFFA1A1AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExpandedWeatherView(
    weatherInfo: WeatherInfo,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    val weatherIcon = when {
        weatherInfo.condition.contains("Sun", ignoreCase = true) || weatherInfo.condition.contains("Clear", ignoreCase = true) -> Icons.Default.WbSunny
        weatherInfo.condition.contains("Rain", ignoreCase = true) || weatherInfo.condition.contains("Drizzle", ignoreCase = true) || weatherInfo.condition.contains("Shower", ignoreCase = true) -> Icons.Default.WaterDrop
        weatherInfo.condition.contains("Snow", ignoreCase = true) -> Icons.Default.AcUnit
        weatherInfo.condition.contains("Thunder", ignoreCase = true) -> Icons.Default.Bolt
        else -> Icons.Default.Cloud
    }
    val iconTint = if (weatherIcon == Icons.Default.WbSunny) Color(0xFFFFD60A) else Color(0xFF64D2FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.WEATHER, scale)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Feature icon + City / Condition + Temp & H/L Badge
        IslandHeader(
            title = weatherInfo.cityName.ifBlank { "Local Weather" },
            subtitle = weatherInfo.condition.ifBlank { "Partly Cloudy" },
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                        .background(IslandTokens.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "Weather",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailing = {
                InfoChip {
                    Text(
                        text = "${weatherInfo.temperatureC}°",
                        color = IslandTokens.TextPrimary,
                        fontSize = IslandTokens.TypeBody,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "H:${weatherInfo.highC}° L:${weatherInfo.lowC}°",
                        color = IslandTokens.TextSecondary,
                        fontSize = IslandTokens.TypeMicro,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )

        // Center section: Minimal 5-hour micro forecast strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E))
                .padding(vertical = 7.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val forecast = if (weatherInfo.hourlyForecast.isNotEmpty()) {
                weatherInfo.hourlyForecast.take(5)
            } else {
                listOf(
                    HourlyWeather("Now", weatherInfo.temperatureC, weatherInfo.condition),
                    HourlyWeather("+1h", weatherInfo.temperatureC, weatherInfo.condition),
                    HourlyWeather("+2h", weatherInfo.temperatureC, weatherInfo.condition),
                    HourlyWeather("+3h", weatherInfo.temperatureC, weatherInfo.condition),
                    HourlyWeather("+4h", weatherInfo.temperatureC, weatherInfo.condition)
                )
            }
            forecast.forEach { hour ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hour.timeLabel,
                        color = Color(0xFFA1A1AA),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${hour.tempC}°",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom section: Clean inline stats: Humidity, Wind, UV
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Humidity: ${weatherInfo.humidityPct}%",
                color = IslandTokens.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "•",
                color = IslandTokens.SurfaceDivider,
                fontSize = IslandTokens.TypeMicro
            )
            Text(
                text = "Wind: ${weatherInfo.windKmh} km/h",
                color = IslandTokens.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "•",
                color = IslandTokens.SurfaceDivider,
                fontSize = IslandTokens.TypeMicro
            )
            Text(
                text = "UV Index: ${weatherInfo.uvIndex}",
                color = IslandTokens.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExpandedNetworkSpeedView(
    networkSpeedInfo: NetworkSpeedInfo,
    onCollapse: () -> Unit,
    scale: Float = 1.0f
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .islandCardContentPadding(IslandPriority.NETWORK_SPEED, scale)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Feature icon + Network label / Latency + Top-right online chip
        IslandHeader(
            title = networkSpeedInfo.networkLabel.ifBlank { "Network Traffic" },
            subtitle = if (networkSpeedInfo.linkDownMbps > 0) "Link ~${networkSpeedInfo.linkDownMbps} Mbps" else "Live traffic",
            icon = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(IslandTokens.RadiusIcon))
                        .background(IslandTokens.SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (networkSpeedInfo.isWifi) Icons.Default.Wifi else Icons.Default.SignalCellularAlt,
                        contentDescription = "Network",
                        tint = IslandTokens.AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            trailing = {
                InfoChip(
                    text = "Online",
                    dotColor = IslandTokens.AccentGreen
                )
            }
        )

        // Center section: Dual minimal cards for Download & Upload (Apple style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Download Card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1C1E)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Download",
                            tint = Color(0xFF64D2FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = networkSpeedInfo.downloadSpeedFormatted,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
            }

            // Upload Card
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1C1E)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Upload",
                            tint = Color(0xFFBF5AF2),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Upload",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = networkSpeedInfo.uploadSpeedFormatted,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
            }
        }

        // Bottom section: Clean session transfer summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Session Transfer",
                color = Color(0xFFA1A1AA),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${networkSpeedInfo.sessionTotalMb} MB Total",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

