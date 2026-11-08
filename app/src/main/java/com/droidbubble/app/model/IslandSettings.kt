package com.droidbubble.app.model

val DEFAULT_FEATURE_PRIORITIES = listOf(
    "MEDIA",
    "PROGRESS",
    "RECORDING",
    "NOTIFICATION",
    "FLASHLIGHT"
)

data class IslandSettings(
    val isEnabled: Boolean = true,
    val widthDp: Int = 180,
    val heightDp: Int = 36,
    val cornerRadiusDp: Int = 22,
    val topOffsetDp: Int = 12,
    val horizontalOffsetDp: Int = 0,
    // Expanded cards fade from black to clear over a blur of what's behind them
    val liquidGlassCards: Boolean = false,
    val showOutline: Boolean = false, // off by default: a borderless black island blends into the camera like iOS
    val outlineColorArgb: Long = 0x33FFFFFF, // Sleek translucent silver
    val outlineThicknessDp: Float = 1.5f,
    val expandedScale: Float = 1.1f,
    val hapticsEnabled: Boolean = true,
    val showMedia: Boolean = true,
    val showNotifications: Boolean = false,
    val showLiveProgress: Boolean = true,
    val showScreenRecording: Boolean = true,
    val showFlashlight: Boolean = true,
    val showBluetooth: Boolean = true,
    val showWifi: Boolean = true,
    val showCharging: Boolean = true,
    val showRingerMode: Boolean = true,
    val showInLandscape: Boolean = false,
    // Accessibility: brighter secondary text, stronger controls and an outline around the island
    val highContrast: Boolean = false,
    // Accessibility: read "Time's up", alarms and the caller's name aloud
    val spokenAlerts: Boolean = false,
    val animationSpeed: Float = 0.7f,
    val blockedPackages: Set<String> = emptySet(),
    // Apps whose notifications take over the main pill (e.g. WhatsApp while music plays)
    val priorityApps: Set<String> = emptySet(),
    val restartOnBoot: Boolean = true,
    val bubbleEffectEnabled: Boolean = true,
    val dismissMediaOnPause: Boolean = false,
    val alwaysShowPill: Boolean = false,
    val showFps: Boolean = false,
    val showWeather: Boolean = false,
    val showNetworkSpeed: Boolean = false,
    val autoDismissNotifications: Boolean = false,
    val blurredAlbumArtBackground: Boolean = false,
    val expandDamping: Float = 0.8f,
    val expandStiffness: Float = 380f,
    val appearDamping: Float = 0.6f,
    val appearStiffness: Float = 280f,
    val splitDamping: Float = 0.63f,
    val splitStiffness: Float = 320f,
    val featurePriorities: List<String> = DEFAULT_FEATURE_PRIORITIES,
    // "PILL": floating capsule around the camera. "NOTCH": attached to the top edge of the screen.
    val islandStyle: String = STYLE_PILL
) {
    val isNotch: Boolean get() = islandStyle == STYLE_NOTCH

    companion object {
        const val STYLE_PILL = "PILL"
        const val STYLE_NOTCH = "NOTCH"

        /** Unknown / missing stored values fall back to the pill. */
        fun normalizeStyle(value: String?): String = if (value == STYLE_NOTCH) STYLE_NOTCH else STYLE_PILL
    }
}
