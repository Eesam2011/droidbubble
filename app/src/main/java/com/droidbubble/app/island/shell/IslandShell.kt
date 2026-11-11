package com.droidbubble.app.island.shell

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
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.island.*
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTween
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandInputs
import com.droidbubble.app.island.scene.IslandMode
import com.droidbubble.app.island.scene.MeasuredHeights
import com.droidbubble.app.island.scene.deriveIslandScene
import com.droidbubble.app.island.scene.deriveIslandTargets
import com.droidbubble.app.island.shell.Follow
import com.droidbubble.app.island.shell.rememberIslandShellState
import com.droidbubble.app.island.shell.rememberWindowReady
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import com.droidbubble.app.model.allows
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/*
* The island's shell: the card stack, the pill body, the bubble, the liquid neck, the notch
* bridge and the parentheses (M2.1, moved out of IslandView.kt unchanged). The presentations
* (CompactContent, ExpandedCardContent...) stay in IslandView.kt.
 */

/** What the island's shapes are heading to, for the settled signal (F5.3). */
private data class SettleTargets(val mode: IslandMode, val compactWidth: Dp, val bubbleX: Dp, val card2Height: Dp)

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun IslandShell(
    settings: IslandSettings
) {
    com.droidbubble.app.util.IslandTraceCounters.rootComposed()
    RecompositionProbe.onRoot?.invoke()
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
    val connectionAlert by IslandStateManager.connectionAlert.collectAsState()
    val foregroundPackage by IslandStateManager.foregroundPackage.collectAsState()
    val userHidden by IslandStateManager.userHidden.collectAsState()
    val userMinimized by IslandStateManager.userMinimized.collectAsState()
    val expandedPriority by IslandStateManager.expandedPriority.collectAsState()
    val isDeviceLocked by IslandStateManager.isDeviceLocked.collectAsState()

    // Tapping the flashlight expands only the flashlight card (latched in the state manager, F1.5)
    val flashlightSolo by IslandStateManager.flashlightSolo.collectAsState()

    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalIslandHaptics.current
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
    // F1.2: one arbitration source. The local names below are aliases of the scene, so the rest of
    // this function reads the same values it always did.
    val sceneInputs = IslandInputs(
        isExpanded = isExpanded,
        expandedPriority = expandedPriority,
        isPopIn = isPopIn,
        media = currentMedia,
        notification = currentNotif,
        progress = currentProgress,
        recording = recording,
        charging = charging,
        isUnlocked = isUnlocked,
        isBluetoothConnected = isBluetoothConnected,
        connectionAlert = connectionAlert,
        ringerMode = activeRingerMode,
        isFlashlightOn = isFlashlightOn,
        foregroundPackage = foregroundPackage,
        userHidden = userHidden,
        userMinimized = userMinimized,
        isDeviceLocked = isDeviceLocked,
        flashlightSolo = flashlightSolo,
    )
    val sceneEnv = IslandEnv(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    val scene = remember(sceneInputs, settings, sceneEnv) { deriveIslandScene(sceneInputs, settings, sceneEnv) }
    // Locked with "hide on lock screen": the window is gone too (F1.4)
    if (scene.mode == IslandMode.OFF) return
    val isRecording = scene.visible.isRecording
    val isProgress = scene.visible.isProgress
    val isNotification = scene.visible.isNotification
    val isMedia = scene.visible.isMedia
    val isFlashlight = scene.visible.isFlashlight
    val isCharging = scene.visible.isCharging
    val isUnlock = scene.visible.isUnlock
    val isBluetooth = scene.visible.isBluetooth
    val isRingerMode = scene.visible.isRingerMode
    val isOccupied = scene.isOccupied
    val activeCount = scene.activeCount
    val isDual = scene.isDual
    val primaryPriority: IslandPriority = scene.primary
    val secondaryPriority: IslandPriority? = scene.secondary

    // Dynamic non-customizable outline colors (around 15% opacity):
    val redRecordingOutline = IslandTokens.RecordingRedSystem.copy(alpha = IslandTokens.OutlineAlphaTask)
    val defaultTaskOutline = Color.White.copy(alpha = IslandTokens.OutlineAlphaTask)
    val mediaOutline = if (mediaAccentColor != Color.Transparent) {
        mediaAccentColor.copy(alpha = IslandTokens.OutlineAlphaTask)
    } else {
        defaultTaskOutline
    }
    val flashlightOutline = Color.White.copy(alpha = IslandTokens.OutlineAlphaFlashlight)

    // Card 1 (Main Island):
    val primaryTaskOutline = when {
        isCharging -> IslandTokens.ChargingGreen.copy(alpha = IslandTokens.OutlineAlphaCharging)
        primaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        primaryPriority == IslandPriority.MEDIA -> mediaOutline
        primaryPriority == IslandPriority.FLASHLIGHT -> flashlightOutline
        else -> defaultTaskOutline
    }

    // Card 2 / Auxiliary Bubble (Secondary Island):
    // Secondary islands have the color of their respective task (15% opacity white usually, or red for recording) instead of the media color
    val secondaryTaskOutline = when {
        secondaryPriority == IslandPriority.SCREEN_RECORDING -> redRecordingOutline
        secondaryPriority == IslandPriority.MEDIA -> mediaOutline
        secondaryPriority == IslandPriority.FLASHLIGHT -> flashlightOutline
        else -> defaultTaskOutline
    }
    // B5.1: high contrast draws every island edge (pill, bubble, both cards) with the 55 % white
    // outline instead of the tasks' 15 % tints
    val primaryOutlineColor = if (IslandTokens.highContrast) IslandTokens.HighContrastOutline else primaryTaskOutline
    val secondaryOutlineColor = if (IslandTokens.highContrast) IslandTokens.HighContrastOutline else secondaryTaskOutline

    // F3.2: every animation spec comes from IslandMotion (the user's curves and speed included)
    val motion = LocalIslandMotion.current
    val speed = motion.speed

    // Squash & stretch when the pill switches to a different activity (the iOS "breath")
    // The leaving content's blur rides this squash (see the compact content swap), so it follows
    // the shape change instead of a separate timer
    val swapSquash = remember { Animatable(0f) }
    var lastCompactPriority by remember { mutableStateOf(primaryPriority) }
    // Battery Saver turns morph blurs off (steppedBlurEffect); start listening for it
    LaunchedEffect(Unit) { AudioPowerPolicy.ensure(context) }
    LaunchedEffect(primaryPriority) {
        if (primaryPriority == lastCompactPriority) return@LaunchedEffect
        lastCompactPriority = primaryPriority
        if (isExpanded) return@LaunchedEffect
        swapSquash.animateTo(0.06f, motion.tween(MotionTween.SWAP_SQUASH_IN))
        swapSquash.animateTo(0f, motion.spring(motion.swapSquashOut))
    }

    // Press feedback: the pill and bubble sink slightly under the finger and spring back
    val pillInteraction = remember { MutableInteractionSource() }
    val pillPressed by pillInteraction.collectIsPressedAsState()
    val pillPressScale by animateFloatAsState(
        targetValue = if (pillPressed) 0.95f else 1f,
        animationSpec = motion.spring(motion.press),
        label = "pillPress"
    )
    val bubbleInteraction = remember { MutableInteractionSource() }
    val bubblePressed by bubbleInteraction.collectIsPressedAsState()
    val bubblePressScale by animateFloatAsState(
        targetValue = if (bubblePressed) 0.9f else 1f,
        animationSpec = motion.spring(motion.press),
        label = "bubblePress"
    )

    // Shell specs. A glance (charging, unlock, connection, ringer) moves on the user's Appear
    // curve, the card opening on Expand, and everything closing on the critically damped
    // collapse, so an interrupted expand keeps its velocity and lands without overshoot.
    val glanceShowing = isCharging || isUnlock || isBluetooth || isRingerMode
    val shellToken = when {
        glanceShowing -> motion.appear
        isExpanded -> motion.expand
        else -> motion.collapse
    }
    val shellClosing = !glanceShowing && !isExpanded
    val sizeAnimSpecDp = motion.spring<Dp>(shellToken, if (shellClosing) Dp.VisibilityThreshold else null)
    val sizeAnimSpecFloat = motion.spring<Float>(shellToken, if (shellClosing) 0.002f else null)

    // One spring drives the card's width, height, corner radius and drop position together,
    // so the pill-to-card morph moves as a single shape instead of four separate animations.
    val cardMorphAnimSpec: AnimationSpec<IslandGeometry> = motion.spring(shellToken, IslandGeometry.VisibilityThreshold)

    // The island's horizontal position (dp as Float, with Dp's 0.1 visibility threshold)
    val positionAnimSpec = if (isExpanded) {
        motion.spring<Float>(motion.expand)
    } else {
        motion.spring<Float>(motion.collapse, Dp.VisibilityThreshold.value)
    }

    // The pill: shrinks away without overshoot while the card opens, and comes back (or appears,
    // hides, minimises) on the user's Split curve, with its small bounce
    val compactPillWidthAnimSpec = if (isExpanded) {
        motion.spring<Dp>(motion.pillShrinkOnExpand)
    } else {
        motion.spring<Dp>(motion.split)
    }

    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) 1f else 0f,
        animationSpec = sizeAnimSpecFloat,
        label = "expandProgress"
    )

    val chargingContentAlpha by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = motion.effects(),
        label = "chargingContentAlpha"
    )

    val unlockContentAlpha by animateFloatAsState(
        targetValue = if (isUnlock) 1f else 0f,
        animationSpec = motion.effects(),
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
        animationSpec = motion.tween(MotionTween.CHECKMARK_FADE),
        label = "unlockCheckmarkAlpha"
    )

    val unlockCheckmarkScale by animateFloatAsState(
        targetValue = if (unlockCheckmarkVisible && isUnlock) 1f else 0.4f,
        // Gentle bounce: overshoots slightly by ~3-4px, then settles
        animationSpec = motion.spring(motion.checkmarkPop),
        label = "unlockCheckmarkScale"
    )

    val bluetoothContentAlpha by animateFloatAsState(
        targetValue = if (isBluetooth) 1f else 0f,
        animationSpec = motion.effects(),
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
        animationSpec = motion.tween(MotionTween.CHECKMARK_FADE),
        label = "bluetoothCheckmarkAlpha"
    )

    val bluetoothCheckmarkScale by animateFloatAsState(
        targetValue = if (bluetoothCheckmarkVisible && isBluetooth) 1f else 0.4f,
        // Gentle bounce: overshoots slightly by ~3-4px, then settles
        animationSpec = motion.spring(motion.checkmarkPop),
        label = "bluetoothCheckmarkScale"
    )

    // Ringer Mode (Silent, Vibrate, Ring) animations:
    val ringerContentAlpha by animateFloatAsState(
        targetValue = if (isRingerMode) 1f else 0f,
        animationSpec = motion.effects(),
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
        animationSpec = motion.tween(MotionTween.CHECKMARK_FADE),
        label = "ringerSecondaryAlpha"
    )

    val ringerSecondaryScale by animateFloatAsState(
        targetValue = if (ringerSecondaryVisible && isRingerMode) 1f else 0.4f,
        animationSpec = motion.spring(motion.checkmarkPop),
        label = "ringerSecondaryScale"
    )

    val bellShakeRotation = remember { Animatable(0f) }

    LaunchedEffect(isRingerMode, activeRingerMode) {
        if (isRingerMode && activeRingerMode == RingerMode.RING) {
            haptics.perform(IslandHapticEvent.SYSTEM_ALERT_RINGER)
            bellShakeRotation.snapTo(0f)
            val animDuration = motion.scaled(1000)
            val rotSpec = motion.keyframes<Float> {
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
            val vibDuration = motion.scaled(600)
            val vibSpec = motion.keyframes<Float> {
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
        animationSpec = motion.tween(if (isCharging) MotionTween.CHARGING_FILL else MotionTween.CHARGING_DRAIN),
        label = "chargingProgress"
    )

    val expandedCardWidth = com.droidbubble.app.island.scene.expandedCardWidth(settings, sceneEnv)

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
    val progressBaseHeight = remember(currentProgress, settings.expandedScale, notifBaseTextStyle, notifDensity) {
        measureProgressCardHeight(currentProgress, settings.expandedScale, notifTextMeasurer, notifBaseTextStyle, notifDensity)
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

    val showSecondaryCard = scene.showSecondaryCard
    val targetCard2Alpha = if (card2Expanded && showSecondaryCard) 1f else 0f
    val targetCard2OffsetY = if (card2Expanded && showSecondaryCard) 0.dp else -IslandTokens.Shell.Card2Rise

    val bubbleSize = settings.heightDp.dp
    val compactMainHalf = settings.widthDp.dp / 2f

    val shouldEmergeSecondary = scene.emergesBubble
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

    // Out on the user's Split curve (a slight overshoot); back into the punch hole smoothly
    val bubbleOffsetAnimSpec = if (secondaryBubbleEmerged && shouldEmergeSecondary) {
        motion.spring<Dp>(motion.split)
    } else {
        motion.spring<Dp>(motion.bubbleRetract)
    }

    // F1.3: every target size comes from one pure function, shared with the window (F1.4)
    // The overlay window's size (state: it updates when the window is resized), known even while
    // nothing is drawn
    val rootSize = LocalWindowInfo.current.containerSize
    val targets = deriveIslandTargets(
        scene = scene,
        settings = settings,
        env = sceneEnv,
        measured = MeasuredHeights(notifBaseHeight, progressBaseHeight),
        card2Expanded = card2Expanded,
        bubbleEmerged = secondaryBubbleEmerged,
    )
    // F5.2: grow the window first. A bigger shape (the card opening, the pill appearing from
    // nothing) waits until the overlay window has really grown to hold it, at most 100 ms, so its
    // first frames are never clipped.
    val windowDensity = LocalDensity.current
    fun fitsWindow(widthDp: Dp, bottomDp: Dp): Boolean = with(windowDensity) {
        rootSize.width >= widthDp.roundToPx() && rootSize.height >= bottomDp.roundToPx()
    }
    val cardWanted = targets.card1.width > 0.dp
    val cardReady = rememberWindowReady(
        want = cardWanted,
        fits = fitsWindow(targets.card1.width, targets.card1.top + targets.card1.height),
    )
    val pillReady = rememberWindowReady(
        want = targets.compactWidth > 0.dp,
        fits = fitsWindow(targets.compactWidth, settings.topOffsetDp.dp + settings.heightDp.dp),
    )
    // Until then the shape holds where it is: the card docked at the pill, the pill at zero width
    val heldTargets = if (cardReady) targets else deriveIslandTargets(
        scene = scene.copy(isExpanded = false),
        settings = settings,
        env = sceneEnv,
        measured = MeasuredHeights(notifBaseHeight, progressBaseHeight),
        card2Expanded = card2Expanded,
        bubbleEmerged = secondaryBubbleEmerged,
    )
    val shellTargetCard1 = if (cardReady) targets.card1 else heldTargets.card1
    val shellTargetX = if (cardReady) targets.horizontalOffset else heldTargets.horizontalOffset
    val targetCompactWidth = if (pillReady) targets.compactWidth else 0.dp
    val targetCard1Width = shellTargetCard1.width
    val targetCard1Height = shellTargetCard1.height
    val card1ContentHeight = targets.card1ContentHeight
    val targetCard2Height = targets.card2Height
    val targetBubbleOffsetX = targets.bubbleRestX
    val targetHorizontalOffset = targets.horizontalOffset
    val targetCardSpacing = targets.cardSpacing

    val compactWidthState = animateDpAsState(targetValue = targetCompactWidth, animationSpec = compactPillWidthAnimSpec, label = "compactWidth")
    val animatedCompactWidthRaw by compactWidthState
    // The bouncy split spring overshoots below 0 when the pill shrinks away (swipe to hide, dual
    // pill retracting); a negative width throws in layout and crashed the island
    val animatedCompactWidth = animatedCompactWidthRaw.coerceAtLeast(0.dp)
    // M1.1: the body's shape and position live in one state holder (Animatables), so later
    // tasks can retarget with a velocity, snap, and read the velocity
    val shell = rememberIslandShellState(shellTargetCard1, shellTargetX)
    shell.Follow(shellTargetCard1, shellTargetX, cardMorphAnimSpec, positionAnimSpec)
    val animatedCardMorph = shell.geometry
    val animatedCard1Width = animatedCardMorph.width.coerceAtLeast(0.dp)
    val animatedCard1Height = animatedCardMorph.height.coerceAtLeast(0.dp)
    val animatedCornerRadius = animatedCardMorph.corner.coerceAtLeast(0.dp)
    val animatedCard2Height by animateDpAsState(targetValue = targetCard2Height, animationSpec = sizeAnimSpecDp, label = "card2Height")
    val animatedCard2Alpha by animateFloatAsState(targetValue = targetCard2Alpha, animationSpec = sizeAnimSpecFloat, label = "card2Alpha")
    val animatedCard2OffsetY by animateDpAsState(targetValue = targetCard2OffsetY, animationSpec = sizeAnimSpecDp, label = "card2OffsetY")
    val animatedBubbleOffsetX by animateDpAsState(targetValue = targetBubbleOffsetX, animationSpec = bubbleOffsetAnimSpec, label = "bubbleOffsetX")
    val animatedHorizontalOffset = shell.xDp

    // M2.2: coarse flags over the animated sizes. They change a handful of times per morph, not
    // every frame, so they don't recompose the shell on their own.
    val card1Shown by remember { derivedStateOf { shell.geometry.width > 1.dp } }
    val card1Filled by remember { derivedStateOf { shell.geometry.width > 2.dp } }
    val card1Outlined by remember { derivedStateOf { shell.geometry.width > 8.dp } }
    val pillShown by remember { derivedStateOf { compactWidthState.value.coerceAtLeast(0.dp) > 0.5.dp } }
    val pillFilled by remember { derivedStateOf { compactWidthState.value.coerceAtLeast(0.dp) > 2.dp } }
    val pillOutlined by remember { derivedStateOf { compactWidthState.value.coerceAtLeast(0.dp) > 8.dp } }
    val animatedCardSpacing by animateDpAsState(targetValue = targetCardSpacing, animationSpec = sizeAnimSpecDp, label = "cardSpacing")

    // F5.3: tell the window when every shape animation (card, pill, bubble, card 2) has come to
    // rest, and in which shape, so it shrinks only then and never clips a shape still drawn
    val settleTargets by rememberUpdatedState(SettleTargets(scene.mode, targetCompactWidth, targetBubbleOffsetX, targetCard2Height))
    LaunchedEffect(Unit) {
        snapshotFlow {
            val t = settleTargets
            val atRest = shell.isSettled &&
                abs((animatedCompactWidthRaw - t.compactWidth).value) < 0.5f &&
                abs((animatedBubbleOffsetX - t.bubbleX).value) < 0.5f &&
                abs((animatedCard2Height - t.card2Height).value) < 0.5f
            if (atRest) SettledShape(t.mode) else null
        }.collect { IslandStateManager.publishSettledShape(it) }
    }

    // How fast the card and the pill are changing size right now. Leaving content blurs with
    // these and the card stretches with them, so both peak mid-morph and vanish as it settles.
    val cardSpeed = rememberMorphSpeed(animatedCard1Width, animatedCard1Height, targetCard1Width, targetCard1Height)
    val pillSpeed = rememberMorphSpeed(animatedCompactWidth, 0.dp, targetCompactWidth.coerceAtLeast(0.dp), 0.dp)

    // On-screen rects of card 1 / card 2, for the liquid glass blur windows
    val glassCardBounds = remember { arrayOfNulls<androidx.compose.ui.geometry.Rect>(2) }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            snapshotFlow { shell.geometry.width <= 0.5.dp && animatedCard2Height <= 0.5.dp }.first { it }
            glassCardBounds[0] = null
            glassCardBounds[1] = null
            IslandStateManager.setGlassPanes(emptyList())
            IslandStateManager.notifyCollapseSettled()
        }
    }

    // Liquid glass: cards fade from black to clear and the overlay service blurs what's behind them
    val glassBlurAvailable by IslandStateManager.glassBlurAvailable.collectAsState()
    val cardBackgroundBrush: Brush = if (settings.liquidGlassCards) {
        // With a real blur behind it the card can go nearly clear; without one keep it readable
        val bottomAlpha = if (glassBlurAvailable) 0.3f else 0.72f
        Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = (bottomAlpha + 0.92f) / 2f), Color.Black.copy(alpha = bottomAlpha))
        )
    } else {
        SolidColor(Color.Black)
    }

    val cardContainerAlpha = if (isExpanded) 1f else ((expandProgress - 0.05f) / 0.95f).coerceIn(0f, 1f)

    var card1OffsetX by remember { mutableFloatStateOf(0f) }
    var card2OffsetX by remember { mutableFloatStateOf(0f) }

    val animatedCard1OffsetX by animateFloatAsState(
        targetValue = card1OffsetX,
        animationSpec = motion.spring(motion.cardDrag),
        label = "card1OffsetX"
    )
    val animatedCard2OffsetX by animateFloatAsState(
        targetValue = card2OffsetX,
        animationSpec = motion.spring(motion.cardDrag),
        label = "card2OffsetX"
    )

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            card1OffsetX = 0f
            card2OffsetX = 0f
        }
    }

    // Liquid glass blur: the cards' on-screen rects go to the overlay service every frame (from
    // layout and from each animated recomposition), so the blur windows under them move, grow and
    // fade in together with the cards instead of appearing after they settle.
    val glassDensity = LocalDensity.current
    val latestGlassEnabled by rememberUpdatedState(settings.liquidGlassCards)
    val latestGlassExpandProgress by rememberUpdatedState(expandProgress)
    val latestGlassCard2Alpha by rememberUpdatedState(if (showSecondaryCard) animatedCard2Alpha else 0f)
    val latestGlassCornerPx by rememberUpdatedState(with(glassDensity) { animatedCornerRadius.toPx() })
    val glassMotionInsetPx = with(glassDensity) { 3.dp.toPx() }
    val publishGlass: () -> Unit = remember {
        {
            val panes = if (!latestGlassEnabled) emptyList() else buildList {
                val strengths = floatArrayOf(
                    latestGlassExpandProgress.coerceIn(0f, 1f),
                    (latestGlassCard2Alpha * latestGlassExpandProgress).coerceIn(0f, 1f)
                )
                for (i in 0..1) {
                    val r = glassCardBounds[i] ?: continue
                    val strength = strengths[i]
                    if (strength < 0.03f || r.width < 8f || r.height < 8f) continue
                    // While moving, tuck the pane a few px inside the card so a one-frame lag of
                    // the blur window can never peek out past the card's edge
                    val inset = if (strength < 0.98f) glassMotionInsetPx else 0f
                    val w = (r.width - inset * 2f).roundToInt()
                    val h = (r.height - inset * 2f).roundToInt()
                    if (w < 4 || h < 4) continue
                    add(
                        IslandStateManager.GlassPane(
                            left = (r.left + inset).roundToInt(),
                            top = (r.top + inset).roundToInt(),
                            width = w,
                            height = h,
                            cornerRadiusPx = (latestGlassCornerPx - inset).coerceAtLeast(0f),
                            // Coarse steps: smooth to the eye, far fewer window updates
                            strength = (strength * 20f).roundToInt() / 20f
                        )
                    )
                }
            }
            IslandStateManager.setGlassPanes(panes)
        }
    }
    SideEffect { publishGlass() }
    LaunchedEffect(isExpanded, showSecondaryCard) {
        if (!isExpanded && !showSecondaryCard) glassCardBounds[1] = null
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
                animationSpec = motion.spring(motion.parenthesisShoot)
            )
        } else {
            parenthesisExtraShoot.snapTo(0f)
        }
    }

    LaunchedEffect(charging?.timestamp) {
        if (charging != null) haptics.perform(IslandHapticEvent.SYSTEM_ALERT_CHARGING)
    }

    LaunchedEffect(isExpanded, isOccupied, primaryPriority) {
        if (isExpanded) {
            if (!isOccupied || primaryPriority == IslandPriority.IDLE) {
                IslandStateManager.collapse()
            }
        }
    }

    // Auto-collapse: an expanded island folds back after 3s without any touch on it
    // (never while a finger is down or while typing a reply).
    // B8.5: touches reach this loop through a channel (pressed / released, sent only when that
    // changes), so composition never reads touch state and a slider drag doesn't recompose the root.
    val touchPresses = remember { Channel<Boolean>(Channel.CONFLATED) }
    val isCallRinging = currentNotif?.isIncomingCall == true
    LaunchedEffect(isExpanded, isKeyboardFocused, isCallRinging) {
        if (!isExpanded || isKeyboardFocused || isCallRinging) return@LaunchedEffect
        var pressed = false
        while (true) {
            if (pressed) {
                pressed = touchPresses.receive()
            } else {
                val next = withTimeoutOrNull(IslandStateManager.timings.autoCollapse) { touchPresses.receive() }
                if (next == null) {
                    IslandStateManager.collapse()
                    return@LaunchedEffect
                }
                pressed = next
            }
        }
    }

    // Completely idle and collapsed: nothing is visible on screen. Skip rendering the view hierarchy to preserve CPU & battery.
    if (!isOccupied && !settings.alwaysShowPill && animatedCompactWidth <= 0.1.dp && !isExpanded && expandProgress <= 0.001f) {
        return
    }

    // Outer container: full touch bounds when expanded to capture outside tap, or exact bounds when minimised
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Observe (never consume) every touch: a press holds off the auto-collapse and a
            // release restarts its 3s. Only changes of the pressed state are sent (B8.5).
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var wasPressed = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.any { it.pressed }
                        if (pressed != wasPressed) {
                            wasPressed = pressed
                            touchPresses.trySend(pressed)
                        }
                    }
                }
            },
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
                        haptics.perform(IslandHapticEvent.COLLAPSE)
                        IslandStateManager.collapse()
                    }
            )
        }



        // Expanded Cards Container: Positioned at animated top and horizontal offset
        if (expandProgress > 0.001f || isExpanded) {
            Box(
                modifier = Modifier
                    .offset {
                        // Read here, in the layout phase (M2.2)
                        IntOffset(
                            x = shell.xDp.roundToPx(),
                            y = shell.geometry.top.roundToPx()
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Cards Stack (Card 1 + Card 2)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(animatedCardSpacing),
                    modifier = if (isExpanded && primaryPriority != IslandPriority.FLASHLIGHT) {
                        // Swipe up to collapse only from the pill zone at the top of the island
                        // (where the pill morphed from), never from card content such as the
                        // media slider, buttons or the second card
                        Modifier.pointerInput(settings.heightDp) {
                            val pillZonePx = (settings.heightDp.dp + IslandTokens.Shell.PillZoneExtra).toPx()
                            var fromPillZone = false
                            var collapsed = false
                            detectVerticalDragGestures(
                                onDragStart = { start ->
                                    fromPillZone = start.y <= pillZonePx
                                    collapsed = false
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    if (fromPillZone && !collapsed && dragAmount < -IslandTokens.Gesture.LegacySwipeUpPx) {
                                        change.consume()
                                        collapsed = true
                                        haptics.perform(IslandHapticEvent.COLLAPSE)
                                        IslandStateManager.collapse()
                                    }
                                }
                            )
                        }
                    } else Modifier
                ) {
                    // Card 1: Primary Expanded Card
                    if (card1Shown && (expandProgress > 0.001f || isExpanded)) {
                        Box(
                            modifier = Modifier
                                .testTag(IslandTestTags.CARD)
                                .offset { IntOffset(animatedCard1OffsetX.roundToInt(), 0) }
                                .layoutSize(width = { shell.geometry.width }, height = { shell.geometry.height })
                                .onGloballyPositioned { glassCardBounds[0] = it.boundsInWindow(); publishGlass() }
                                .graphicsLayer {
                                    alpha = cardContainerAlpha
                                    // Liquid stretch: while the card grows it widens a touch and
                                    // flattens, and springs back as the morph slows. It hangs from
                                    // the top edge so it stays docked under the camera. Off for
                                    // glass cards, whose blur window behind follows the unscaled
                                    // bounds and would no longer line up.
                                    if (cardSpeed.growing && !settings.liquidGlassCards) {
                                        transformOrigin = TransformOrigin(0.5f, 0f)
                                        scaleX = 1f + 0.035f * cardSpeed.speed
                                        scaleY = 1f - 0.02f * cardSpeed.speed
                                    }
                                }
                                // M2.3: the corner is read in the draw phase; same as clip(islandShape(corner))
                                .graphicsLayer {
                                    shape = IslandShapeCache.get(shell.geometry.corner.coerceAtLeast(0.dp))
                                    clip = true
                                }
                                .background(if (card1Filled) cardBackgroundBrush else SolidColor(Color.Transparent))
                                .then(
                                    if (settings.showOutline && card1Outlined) {
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
                                                    val threshold = IslandTokens.Gesture.LegacySwipeDismissPx
                                                    when (primaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                haptics.perform(IslandHapticEvent.DISMISS)
                                                                IslandStateManager.dismissMedia()
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                if (currentNotif?.isIncomingCall == true || currentNotif?.isAlert == true) {
                                                                    // B6.2: a ringing call or alarm tucks back to the pill instead
                                                                    // of losing its Answer / Stop buttons. Decline and Stop end it.
                                                                    haptics.perform(IslandHapticEvent.COLLAPSE)
                                                                    IslandStateManager.collapse()
                                                                } else {
                                                                    haptics.perform(IslandHapticEvent.DISMISS)
                                                                    IslandStateManager.clearCurrentNotification()
                                                                }
                                                            }
                                                        }
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card1OffsetX) > threshold) {
                                                                haptics.perform(IslandHapticEvent.DISMISS)
                                                                IslandStateManager.dismissProgress()
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
                                    MediaArtBackdrop(
                                        art = blurredMediaArt ?: mediaArt,
                                        needsBlur = blurredMediaArt == null,
                                        alpha = expandProgress,
                                        glass = settings.liquidGlassCards,
                                        shape = islandShape(animatedCornerRadius)
                                    )
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
                                            val blurpleTint = IslandTokens.FlashlightTint
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    blurpleTint.copy(alpha = 0.06f * expandProgress),
                                                    blurpleTint.copy(alpha = 0.20f * expandProgress)
                                                )
                                            )
                                        }
                                        primaryPriority == IslandPriority.SCREEN_RECORDING -> {
                                            val redTint = IslandTokens.RecordingRed
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
                            // Arriving content waits until the card is ~60% of the way to its
                            // shape, so it never shows stretched while the card is still growing.
                            // Fade and scale come from expandProgress, which runs on the same
                            // spring as the card's size, so an interrupted morph reverses together.
                            val expandedAlpha = if (isExpanded) {
                                ((expandProgress - 0.60f) / 0.40f).coerceIn(0f, 1f)
                            } else {
                                ((expandProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                            }

                            if (!isCharging && expandedAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .testTag(IslandTestTags.content("card1", primaryPriority))
                                        .wrapContentSize(Alignment.TopCenter, unbounded = true)
                                        .requiredSize(expandedCardWidth, card1ContentHeight)
                                        // Inner padding lives in each view (islandCardContentPadding)
                                        .graphicsLayer {
                                            alpha = expandedAlpha
                                            scaleX = 0.94f + (0.06f * expandedAlpha)
                                            scaleY = 0.94f + (0.06f * expandedAlpha)
                                            // Only leaving content blurs: sharp on the way in, blurred
                                            // with the card's speed (or its fade) on the way out
                                            renderEffect = if (isExpanded) null else steppedBlurEffect(
                                                maxOf(1f - expandedAlpha, cardSpeed.speed), 10.dp.toPx()
                                            )
                                        }
                                ) {
                                    ExpandedCardContent(
                                        priority = primaryPriority,
                                        media = currentMedia,
                                        notification = if (primaryPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                        recording = recording,
                                        flashlightLevel = flashlightLevel,
                                        maxFlashlightLevel = maxFlashlightLevel,
                                        scale = settings.expandedScale,
                                        onCollapse = {
                                            haptics.perform(IslandHapticEvent.COLLAPSE)
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
                                .layoutSize(width = { shell.geometry.width })
                                .height(animatedCard2Height)
                                .onGloballyPositioned { glassCardBounds[1] = it.boundsInWindow(); publishGlass() }
                                .graphicsLayer {
                                    alpha = animatedCard2Alpha
                                    scaleX = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                                    scaleY = 0.92f + (0.08f * animatedCard2Alpha.coerceIn(0f, 1f))
                                }
                                // M2.3: the corner is read in the draw phase; same as clip(islandShape(corner))
                                .graphicsLayer {
                                    shape = IslandShapeCache.get(shell.geometry.corner.coerceAtLeast(0.dp))
                                    clip = true
                                }
                                .background(cardBackgroundBrush)
                                .then(
                                    if (settings.showOutline) {
                                        Modifier.border(
                                            width = settings.outlineThicknessDp.dp,
                                            color = secondaryOutlineColor,
                                            shape = islandShape(animatedCornerRadius)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isExpanded && secondaryPriority != null) {
                                        Modifier.pointerInput(secondaryPriority) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = {
                                                    val threshold = IslandTokens.Gesture.LegacySwipeDismissPx
                                                    when (secondaryPriority) {
                                                        IslandPriority.MEDIA -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                haptics.perform(IslandHapticEvent.DISMISS)
                                                                IslandStateManager.dismissMedia()
                                                            }
                                                        }
                                                        IslandPriority.POP_NOTIFICATION -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                if (currentNotif?.isIncomingCall == true || currentNotif?.isAlert == true) {
                                                                    // B6.2: a ringing call or alarm tucks back to the pill instead
                                                                    // of losing its Answer / Stop buttons. Decline and Stop end it.
                                                                    haptics.perform(IslandHapticEvent.COLLAPSE)
                                                                    IslandStateManager.collapse()
                                                                } else {
                                                                    haptics.perform(IslandHapticEvent.DISMISS)
                                                                    IslandStateManager.clearCurrentNotification()
                                                                }
                                                            }
                                                        }
                                                        IslandPriority.PROGRESS -> {
                                                            if (abs(card2OffsetX) > threshold) {
                                                                haptics.perform(IslandHapticEvent.DISMISS)
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
                                    MediaArtBackdrop(
                                        art = blurredMediaArt ?: mediaArt,
                                        needsBlur = blurredMediaArt == null,
                                        alpha = animatedCard2Alpha,
                                        glass = settings.liquidGlassCards,
                                        shape = islandShape(animatedCornerRadius)
                                    )
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
                                            val blurpleTint = IslandTokens.FlashlightTint
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    blurpleTint.copy(alpha = 0.06f * animatedCard2Alpha),
                                                    blurpleTint.copy(alpha = 0.20f * animatedCard2Alpha)
                                                )
                                            )
                                        }
                                        secondaryPriority == IslandPriority.SCREEN_RECORDING -> {
                                            val redTint = IslandTokens.RecordingRed
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
                                        .testTag(IslandTestTags.content("card2", secondaryPriority))
                                        .wrapContentSize(Alignment.TopCenter, unbounded = true)
                                        .requiredSize(
                                            expandedCardWidth,
                                            cardHeightFor(secondaryPriority, settings.expandedScale, notifBaseHeight, progressBaseHeight, notifDensity.fontScale)
                                        )
                                        .graphicsLayer {
                                            renderEffect = if (card2Expanded) null else steppedBlurEffect(
                                                maxOf(1f - animatedCard2Alpha, cardSpeed.speed), 10.dp.toPx()
                                            )
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
                                        scale = settings.expandedScale,
                                        onCollapse = {
                                            haptics.perform(IslandHapticEvent.COLLAPSE)
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
                        x = shell.xDp.roundToPx(),
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
                } else if (!isOccupied) {
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
                            .testTag(IslandTestTags.BUBBLE)
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
                                    haptics.perform(IslandHapticEvent.TAP_EXPAND)
                                    IslandStateManager.expand(secondaryPriority)
                                },
                                onLongClick = {
                                    haptics.perform(IslandHapticEvent.LONG_PRESS)
                                    openAppForPriority(
                                        context = context,
                                        priority = secondaryPriority,
                                        media = currentMedia,
                                        notification = currentNotif,
                                        progress = currentProgress
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (contentAlpha > 0.05f) {
                            Box(
                                modifier = Modifier
                                    .testTag(IslandTestTags.content("bubble", secondaryPriority))
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = contentAlpha },
                                contentAlignment = Alignment.Center
                            ) {
                                AuxiliaryBubbleContent(
                                    priority = secondaryPriority,
                                    media = currentMedia,
                                    notification = if (secondaryPriority == IslandPriority.PROGRESS) currentProgress else currentNotif,
                                    recording = recording,
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
                val snapGap = IslandTokens.Shell.NeckSnapGap
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

            // Notch style: a black bridge with concave "ears" joins the pill to the top edge of the
            // screen. Drawn under the pill and follows its width, so every morph still works.
            val compactPillShape = if (settings.isNotch) {
                notchPillShape(settings.heightDp.dp * 0.45f)
            } else {
                islandShape(settings.heightDp.dp / 2)
            }
            if (settings.isNotch && animatedCompactWidth > 0.5.dp) {
                val notchEar = (8.dp * compactPillAlpha).coerceAtLeast(0.dp)
                val bridgeHeight = settings.topOffsetDp.dp + settings.heightDp.dp / 2f
                Canvas(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = -settings.topOffsetDp.dp.roundToPx()) }
                        .size(animatedCompactWidth + notchEar * 2, bridgeHeight)
                        .graphicsLayer {
                            alpha = compactPillAlpha
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleX = pillPressScale * (1f + swapSquash.value)
                        }
                ) {
                    drawPath(notchBridgePath(size.width, size.height, notchEar.toPx()), Color.Black)
                }
            }

            // 2. Main Compact Pill: Rendered on top of the bubble (covers it when sliding near x = 0)
            if (pillShown) {
                Box(
                    modifier = Modifier
                        .testTag(IslandTestTags.PILL)
                        .layoutSize(width = { compactWidthState.value })
                        .height(settings.heightDp.dp)
                        .graphicsLayer {
                            alpha = compactPillAlpha
                            // A notch stays attached to the top edge: no bounce away from it
                            translationY = if (settings.isNotch) 0f else idleBounceY.value.dp.toPx()
                            if (settings.isNotch) transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleX = idleBounceScale.value * pillPressScale * (1f + swapSquash.value)
                            scaleY = idleBounceScale.value * pillPressScale * (1f - swapSquash.value * 0.5f)
                        }
                        .clip(compactPillShape)
                        .background(if (pillFilled) Color.Black else Color.Transparent)
                        .then(
                            // Deliberately no pill outline in notch style: the notch is fused to the screen edge
                            if (settings.showOutline && !settings.isNotch && pillOutlined) {
                                Modifier.border(
                                    width = settings.outlineThicknessDp.dp,
                                    color = primaryOutlineColor.copy(alpha = (primaryOutlineColor.alpha * compactPillAlpha).coerceIn(0f, 1f)),
                                    shape = compactPillShape
                                )
                            } else Modifier
                        )
                        // Swipe left: shrink the pill into the camera circle. On the camera circle, a
                        // right swipe (or a tap) brings it back. A right swipe on the full pill does
                        // nothing (B6.4, D6): it used to hide the island until the next event, a trap.
                        // The drag is still consumed so it doesn't leak to the card below.
                        .pointerInput(isExpanded, userMinimized) {
                            var dragTotal = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragTotal += amount
                                },
                                onDragEnd = {
                                    val threshold = IslandTokens.Gesture.LegacyPillSwipe.toPx()
                                    when {
                                        isExpanded -> Unit
                                        userMinimized && dragTotal > threshold -> {
                                            haptics.perform(IslandHapticEvent.RESTORE)
                                            IslandStateManager.restoreIsland()
                                        }
                                        dragTotal < -threshold && !userMinimized -> {
                                            haptics.perform(IslandHapticEvent.MINIMISE)
                                            IslandStateManager.minimizeIsland()
                                        }
                                    }
                                }
                            )
                        }
                        .combinedClickable(
                            interactionSource = pillInteraction,
                            indication = null,
                            onClick = {
                                if (userMinimized) {
                                    haptics.perform(IslandHapticEvent.RESTORE)
                                    IslandStateManager.restoreIsland()
                                } else if (!isExpanded && isOccupied && !isUnlock && !isBluetooth && !isRingerMode) {
                                    haptics.perform(IslandHapticEvent.TAP_EXPAND)
                                    IslandStateManager.expand(primaryPriority)
                                } else if (!isExpanded && !isOccupied && settings.alwaysShowPill) {
                                    haptics.perform(IslandHapticEvent.IDLE_TAP)
                                    coroutineScope.launch {
                                        launch {
                                            idleBounceY.snapTo(0f)
                                            idleBounceY.animateTo(
                                                targetValue = 3.5f,
                                                animationSpec = motion.tween(MotionTween.IDLE_TAP_DOWN)
                                            )
                                            idleBounceY.animateTo(
                                                targetValue = -1.5f,
                                                animationSpec = motion.tween(MotionTween.IDLE_TAP_UP)
                                            )
                                            idleBounceY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = motion.spring(motion.idleTapSettle)
                                            )
                                        }
                                        launch {
                                            idleBounceScale.snapTo(1f)
                                            idleBounceScale.animateTo(
                                                targetValue = 0.95f,
                                                animationSpec = motion.tween(MotionTween.IDLE_TAP_DOWN)
                                            )
                                            idleBounceScale.animateTo(
                                                targetValue = 1.04f,
                                                animationSpec = motion.tween(MotionTween.IDLE_TAP_UP)
                                            )
                                            idleBounceScale.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = motion.spring(motion.idleTapSettle)
                                            )
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isExpanded && !isUnlock && !isBluetooth && !isRingerMode) {
                                    haptics.perform(IslandHapticEvent.LONG_PRESS)
                                    openAppForPriority(
                                        context = context,
                                        priority = primaryPriority,
                                        media = currentMedia,
                                        notification = currentNotif,
                                        progress = currentProgress
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val compactAlpha = if (userMinimized && !isCharging && !isUnlock && !isBluetooth && !isRingerMode) 0f else if (isExpanded) {
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
                                .background(IslandTokens.ChargingFillDark)
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
                                            IslandTokens.ChargingFillFrom,
                                            IslandTokens.ChargingFillTo
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
                                    color = IslandTokens.ChargingGreen,
                                    fontSize = IslandType.CompactValue,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(IslandTokens.Shell.CameraZone))

                            // Right: Current battery percentage in green
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "${charging?.percentage ?: 100}%",
                                    color = IslandTokens.ChargingGreen,
                                    fontSize = IslandType.CompactValue,
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
                            Spacer(modifier = Modifier.width(IslandTokens.Shell.CameraZone))

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
                                    imageVector = if (connectionAlert == IslandStateManager.ConnectionAlert.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
                                    contentDescription = if (connectionAlert == IslandStateManager.ConnectionAlert.WIFI) "Wi-Fi" else "Bluetooth",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // Center: 36dp deadzone strictly over camera cutout
                            Spacer(modifier = Modifier.width(IslandTokens.Shell.CameraZone))

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
                                    IslandTokens.RingerSilentRed // Red icon for silent
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
                            Spacer(modifier = Modifier.width(IslandTokens.Shell.CameraZone))

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
                                        IslandTokens.RingerSilentRed // Red text for silent
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
                                            fontSize = IslandType.CompactLabel,
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
                                .graphicsLayer {
                                    alpha = compactAlpha
                                    // Pill content leaving for the card blurs out with the morph
                                    renderEffect = if (isExpanded) steppedBlurEffect(
                                        maxOf(1f - compactAlpha, cardSpeed.speed), 10.dp.toPx()
                                    ) else null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Switching activity: old content shrinks and blurs out while the new one
                            // springs in sharp, instead of swapping instantly
                            AnimatedContent(
                                targetState = primaryPriority,
                                transitionSpec = {
                                    (fadeIn(motion.tween(MotionTween.COMPACT_SWAP_FADE_IN)) +
                                        scaleIn(initialScale = 0.85f, animationSpec = motion.spring(motion.compactSwapScale))) togetherWith
                                        (fadeOut(motion.tween(MotionTween.COMPACT_SWAP_OUT)) +
                                            scaleOut(targetScale = 0.85f, animationSpec = motion.tween(MotionTween.COMPACT_SWAP_OUT))) using
                                        SizeTransform(clip = false)
                                },
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                                label = "compactContentSwap"
                            ) { shownPriority ->
                                Box(
                                    modifier = Modifier
                                        .testTag(IslandTestTags.content("pill", shownPriority))
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            // Only the leaving activity blurs, with the swap's squash
                                            // and the pill's width change; the new one stays sharp
                                            renderEffect = if (shownPriority != primaryPriority) steppedBlurEffect(
                                                maxOf(kotlin.math.abs(swapSquash.value) / 0.06f, pillSpeed.speed), 12.dp.toPx()
                                            ) else null
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
            val targetParenthesisAlpha = if (!isOccupied || isDual || userHidden || userMinimized || settings.isNotch) 0f else ((1f - expandProgress * 3.5f).coerceIn(0f, 1f))
            val parenthesisAlphaAnim by animateFloatAsState(
                targetValue = targetParenthesisAlpha,
                animationSpec = motion.effects(),
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
