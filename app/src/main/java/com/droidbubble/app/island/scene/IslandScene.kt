package com.droidbubble.app.island.scene

import androidx.compose.runtime.Immutable
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState

/** Everything arbitration reads. A plain snapshot: no flows, no Compose. */
@Immutable
data class IslandInputs(
    val isExpanded: Boolean = false,
    val expandedPriority: IslandPriority? = null,
    val isPopIn: Boolean = false,
    val media: MediaInfo? = null,
    val notification: NotificationInfo? = null,
    val progress: NotificationInfo? = null,
    val recording: ScreenRecordingState = ScreenRecordingState(),
    val charging: IslandStateManager.ChargingInfo? = null,
    val isUnlocked: Boolean = false,
    /** "A connection alert is showing"; [connectionAlert] says which one. */
    val isBluetoothConnected: Boolean = false,
    val connectionAlert: IslandStateManager.ConnectionAlert = IslandStateManager.ConnectionAlert.BLUETOOTH,
    val ringerMode: IslandStateManager.RingerMode? = null,
    val isFlashlightOn: Boolean = false,
    /** An ongoing call tracked by the service. The window counts it; the view does not (F1.4). */
    val isCallActive: Boolean = false,
    val foregroundPackage: String? = null,
    val userHidden: Boolean = false,
    val userMinimized: Boolean = false,
    /** Only the window reads it today, for `hideOnLockScreen` (F1.4). */
    val isDeviceLocked: Boolean = false,
    /** The view's latch: the flashlight was tapped, so only its card expands. */
    val flashlightSolo: Boolean = false,
)

@Immutable
data class IslandEnv(
    val isLandscape: Boolean = false,
    val screenWidthDp: Int = 411,
    val fontScale: Float = 1f,
)

enum class IslandMode {
    /** Disabled, or landscape without "show in landscape". Nothing is composed. */
    OFF,
    /** Nothing to show (or swiped away): no pill. */
    IDLE,
    /** Nothing to show, but "always show pill" keeps the empty pill. */
    IDLE_PILL,
    /** Minimized into the circle around the camera. */
    MINIMAL,
    COMPACT,
    /** Pill plus the secondary bubble. */
    DUAL,
    /** Charging, unlock, connection or ringer glance. */
    SYSTEM_ALERT,
    EXPANDED,
}

enum class SystemAlert { CHARGING, UNLOCK, CONNECTION, RINGER }

/** What is visible, after the `show*` settings, the foreground-app rule and the alert order. */
@Immutable
data class VisibleActivities(
    val isRecording: Boolean = false,
    val isProgress: Boolean = false,
    val isNotification: Boolean = false,
    val isMedia: Boolean = false,
    val isFlashlight: Boolean = false,
    val isCharging: Boolean = false,
    val isUnlock: Boolean = false,
    val isBluetooth: Boolean = false,
    val isRingerMode: Boolean = false,
)

@Immutable
data class IslandScene(
    val mode: IslandMode,
    /** IDLE when nothing is showing. */
    val primary: IslandPriority,
    /** The bubble's (compact) or card 2's (expanded) activity; null unless two activities share the island. */
    val secondary: IslandPriority?,
    /** The glance covering the activities, if any. */
    val alert: SystemAlert?,
    val visible: VisibleActivities,
    /** Activities that can be primary or secondary (system alerts don't count). */
    val activeCount: Int,
    val isDual: Boolean,
    val isOccupied: Boolean,
    val showSecondaryCard: Boolean,
    /** True when something touchable is on screen: an open card, or a pill that shows something. */
    val wantsTouch: Boolean,
) {
    companion object {
        val Off = IslandScene(
            mode = IslandMode.OFF,
            primary = IslandPriority.IDLE,
            secondary = null,
            alert = null,
            visible = VisibleActivities(),
            activeCount = 0,
            isDual = false,
            isOccupied = false,
            showSecondaryCard = false,
            wantsTouch = false,
        )
    }
}
