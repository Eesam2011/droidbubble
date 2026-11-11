package com.droidbubble.app.island.scene

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.notificationSlotGate
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.allows

/**
 * What the island shows for a given state: which activities are visible, which one is primary,
 * which one is secondary, and which glance covers them. Pure, so the view and the window can
 * share one answer (F1).
 *
 * The rules are the ones `IslandOverlayRoot` had inline (F1.1 lifted them unchanged).
 */
fun deriveIslandScene(inputs: IslandInputs, settings: IslandSettings, env: IslandEnv): IslandScene {
    val allowInOrientation = settings.isEnabled && (settings.showInLandscape || !env.isLandscape)
    if (!allowInOrientation) return IslandScene.Off

    val currentMedia = inputs.media
    val currentNotif = inputs.notification
    val currentProgress = inputs.progress
    val foregroundPackage = inputs.foregroundPackage

    val isRecording = inputs.recording.isRecording && settings.showScreenRecording
    val isProgress = currentProgress != null && settings.showLiveProgress &&
        currentProgress.packageName != foregroundPackage
    // A ringing call always shows, even if the phone app's call screen is the foreground window
    // B3.1: messages need "Incoming Notifications", calls and live activities their own toggles
    val notifGate = currentNotif?.let { notificationSlotGate(it) }
    val isNotification = currentNotif != null && notifGate != null && settings.allows(notifGate) &&
        (currentNotif.packageName != foregroundPackage || currentNotif.isIncomingCall)
    val isMedia = currentMedia != null && currentMedia.packageName != foregroundPackage &&
        (!settings.dismissMediaOnPause || currentMedia.isPlaying) &&
        (currentMedia.isPlaying || currentMedia.title.isNotBlank()) && settings.showMedia
    val isFlashlight = settings.showFlashlight && inputs.isFlashlightOn
    val isCharging = settings.showCharging && inputs.charging != null
    val isUnlock = inputs.isUnlocked && !isCharging
    val connectionAlertEnabled =
        if (inputs.connectionAlert == IslandStateManager.ConnectionAlert.WIFI) settings.showWifi else settings.showBluetooth
    val isBluetooth = connectionAlertEnabled && inputs.isBluetoothConnected && !isCharging && !isUnlock
    val isRingerMode = settings.showRingerMode && inputs.ringerMode != null && !isCharging && !isUnlock && !isBluetooth

    val isOccupied = isRecording || isNotification || isMedia || isProgress || isCharging || isUnlock ||
        isBluetooth || isFlashlight || isRingerMode

    // Dual: two or more activities share the island, unless a glance covers them
    val activeCount = (if (isRecording) 1 else 0) +
        (if (isNotification) 1 else 0) +
        (if (isMedia) 1 else 0) +
        (if (isProgress) 1 else 0) +
        (if (isFlashlight) 1 else 0)
    val isDual = !isCharging && !isUnlock && !isBluetooth && !isRingerMode && activeCount >= 2

    // Primary and secondary follow settings.featurePriorities; calls and priority apps go first
    val ordered = buildList {
        if (isNotification && (currentNotif?.isCall == true || currentNotif?.packageName in settings.priorityApps)) {
            add(IslandPriority.POP_NOTIFICATION)
        }
        for (featureKey in settings.featurePriorities) {
            when (featureKey) {
                "MEDIA" -> if (isMedia) add(IslandPriority.MEDIA)
                "PROGRESS" -> if (isProgress) add(IslandPriority.PROGRESS)
                "RECORDING" -> if (isRecording) add(IslandPriority.SCREEN_RECORDING)
                "NOTIFICATION" -> if (isNotification && IslandPriority.POP_NOTIFICATION !in this) add(IslandPriority.POP_NOTIFICATION)
                "FLASHLIGHT" -> if (isFlashlight) add(IslandPriority.FLASHLIGHT)
            }
        }
        if (isFlashlight && "FLASHLIGHT" !in settings.featurePriorities) {
            add(IslandPriority.FLASHLIGHT)
        }
    }

    val basePrimary: IslandPriority = ordered.firstOrNull() ?: IslandPriority.IDLE
    val baseSecondary: IslandPriority? = when {
        !isDual -> null
        ordered.size >= 2 -> {
            if (inputs.isPopIn && isNotification && basePrimary != IslandPriority.POP_NOTIFICATION) {
                IslandPriority.POP_NOTIFICATION
            } else {
                ordered[1]
            }
        }
        else -> null
    }

    val isFlashlightSolo = inputs.flashlightSolo && isFlashlight
    val primary = if (isFlashlightSolo) IslandPriority.FLASHLIGHT else basePrimary
    val secondary = if (isFlashlightSolo) null else baseSecondary

    val alert = when {
        isCharging -> SystemAlert.CHARGING
        isUnlock -> SystemAlert.UNLOCK
        isBluetooth -> SystemAlert.CONNECTION
        isRingerMode -> SystemAlert.RINGER
        else -> null
    }
    val showSecondaryCard = alert == null && activeCount >= 2 && secondary != null

    // Derived only: the same order the view picks the pill's width in
    val mode = when {
        inputs.isExpanded && alert == null -> IslandMode.EXPANDED
        alert != null -> IslandMode.SYSTEM_ALERT
        inputs.userHidden -> IslandMode.IDLE
        !isOccupied && !settings.alwaysShowPill -> IslandMode.IDLE
        inputs.userMinimized -> IslandMode.MINIMAL
        !isOccupied -> IslandMode.IDLE_PILL
        isDual -> IslandMode.DUAL
        else -> IslandMode.COMPACT
    }

    return IslandScene(
        mode = mode,
        primary = primary,
        secondary = secondary,
        alert = alert,
        visible = VisibleActivities(
            isRecording = isRecording,
            isProgress = isProgress,
            isNotification = isNotification,
            isMedia = isMedia,
            isFlashlight = isFlashlight,
            isCharging = isCharging,
            isUnlock = isUnlock,
            isBluetooth = isBluetooth,
            isRingerMode = isRingerMode,
        ),
        activeCount = activeCount,
        isDual = isDual,
        isOccupied = isOccupied,
        showSecondaryCard = showSecondaryCard,
        wantsTouch = inputs.isExpanded || (isOccupied && !inputs.userHidden),
    )
}
