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
    val showCharging: Boolean = true,
    val showRingerMode: Boolean = true,
    val showInLandscape: Boolean = false,
    val animationSpeed: Float = 0.7f,
    val blockedPackages: Set<String> = emptySet(),
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
    val featurePriorities: List<String> = DEFAULT_FEATURE_PRIORITIES
)
