package com.droidbubble.app.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for the island's visual language:
 * spacing scale, corner radii, color palette and type scale.
 * Views should read from here instead of hardcoding hex colors / magic numbers.
 */
object IslandTokens {

    /**
     * High contrast mode (Accessibility setting): secondary text, dim text and secondary
     * controls get brighter. Backed by Compose state, so flipping it restyles the island live.
     */
    var highContrast by mutableStateOf(false)

    // ---- Spacing scale (4pt grid) ----
    val SpaceXxs: Dp = 2.dp   // title -> subtitle
    val SpaceXs: Dp = 4.dp    // chip gaps, inner corners
    val SpaceSm: Dp = 8.dp    // header -> body
    val SpaceMd: Dp = 12.dp   // icon -> header text
    val SpaceLg: Dp = 16.dp   // section gaps
    val SpaceXl: Dp = 24.dp   // outer breathing room

    // ---- Corner radii ----
    val RadiusIcon: Dp = 10.dp
    val RadiusChip: Dp = 12.dp
    val RadiusButton: Dp = 14.dp
    val RadiusCard: Dp = 28.dp
    /** Corner radius the collapsed pill morphs toward (the squircle caps it at half the height). */
    val CompactCornerRadius: Dp = 22.dp

    // ---- Surfaces ----
    val CardBackground = Color.Black
    val SurfaceMuted = Color(0xFF212121)   // icon tiles, reply input
    val SurfaceRaised = Color(0xFF2C2C2E)  // chips, secondary buttons
    val SurfaceSunken = Color(0xFF1C1C1E)  // inner panels (forecast, transfer cards)
    val SurfaceDivider = Color(0xFF3A3A3C)

    // ---- Text ----
    val TextPrimary = Color.White
    val TextBody = Color(0xFFF4F4F5)
    val TextSecondary: Color get() = if (highContrast) Color(0xFFDADADF) else Color(0xFFA1A1AA)
    val TextTertiary: Color get() = if (highContrast) Color(0xFFC7C7CC) else Color(0xFF8E8E93)

    // ---- Accents ----
    val AccentBlue = Color(0xFF64D2FF)
    val AccentGreen = Color(0xFF30D158)
    val AccentOrange = Color(0xFFFF9F0A)
    val AccentPurple = Color(0xFFBF5AF2)
    val AccentRed = Color(0xFFFF453A)
    val AccentYellow = Color(0xFFFFD60A)

    // ---- Actions ----
    val ActionPrimaryContainer = Color(0xFFE5E5EA)
    val ActionPrimaryContent = Color.Black
    // Brighter than SurfaceRaised for contrast on black cards
    val ActionSecondaryContainer: Color get() = if (highContrast) Color(0xFF5A5A5E) else Color(0xFF3A3A3C)

    // ---- Semantic roles (F2.2: the values the views used inline, unchanged) ----
    /** Recording tint on cards. Same value as [AccentRed]. */
    val RecordingRed = AccentRed
    /** The recording outline and dot use the brighter system red. F2.3 merges it into [RecordingRed]. */
    val RecordingRedSystem = Color(0xFFFF3B30)
    /** The silent ringer's icon and label. */
    val RingerSilentRed = RecordingRedSystem
    val ChargingGreen = AccentGreen
    /** The charging pill's fill: the dark base, then the gradient that fills it. */
    val ChargingFillDark = Color(0xFF04190B)
    val ChargingFillFrom = Color(0xFF062B12)
    val ChargingFillTo = Color(0xFF0D3D1B)
    val FlashlightTint = Color(0xFFD2D6FF)
    /** Light controls: progress fills, equalizer bars, icons on dark tiles. Same value as [ActionPrimaryContainer]. */
    val ControlLight = ActionPrimaryContainer
    /** The unfilled part of a progress bar or ring. Same value as [SurfaceRaised]. */
    val ControlTrack = SurfaceRaised
    /** An icon that can't be used yet (the send button with no text). */
    val IconDisabled = Color(0xFF52525B)

    // ---- Live activity accents (used through LiveActivityColors) ----
    val LiveRecordingRed = Color(0xFFFF5A47)
    val LiveTimerBlue = Color(0xFF5B9BFF)
    val LiveCallGreen = Color(0xFF34C759)
    val LiveAlarmYellow = Color(0xFFFFCC00)
    val LiveSportsWhite = Color(0xFFF2F2F7)

    // ---- Outline strengths: the alpha of the thin tinted outline around a task's island ----
    const val OutlineAlphaTask = 0.15f
    const val OutlineAlphaFlashlight = 0.25f
    const val OutlineAlphaCharging = 0.30f

    /** Outline drawn around the island in high contrast mode, so it stands out on dark wallpapers. */
    val HighContrastOutline = Color.White.copy(alpha = 0.55f)

    /**
     * The numbers that define the island's body (not what is inside a card). The view, the target
     * geometry and the overlay window all read them here, so they can't drift apart (F2.4).
     */
    object Shell {
        /** An open card sits this far below the pill's top offset, clear of the status bar. */
        val ExpandedDrop = 52.dp
        /** Width of an open card before `expandedScale`. */
        val ExpandedCardWidth = 340.dp
        /** An open card stays this far inside the screen in total (8 dp per side). */
        val ScreenGutter = 16.dp
        /** Gap between the pill and the secondary bubble. */
        val BubbleGap = 6.dp
        /** Gap between card 1 and card 2. */
        val CardSpacing = 10.dp
        /** Card 2 starts this far up, under card 1, and drops into place. */
        val Card2Rise = 14.dp
        /** The liquid neck between pill and bubble snaps once they are this far apart. */
        val NeckSnapGap = 5.dp
        /** A swipe up collapses only when it starts within the pill's height plus this. */
        val PillZoneExtra = 20.dp
        /** The empty middle of the pill, over the camera (V1 replaces it with the measured cutout). */
        val CameraZone = 36.dp
        /** While the island morphs, the window keeps this much room beside the pill. */
        val WindowSideMargin = 40.dp
        /** While the island morphs, the window keeps this much room below the pill. */
        val WindowBottomMargin = 12.dp
        /** The charging pill is this much wider than the pill, and at least [ChargingMinWidth]. */
        const val ChargingExtraWidthDp = 20
        val ChargingMinWidth = 140.dp
        /** The unlock and connection pills: a fraction of the pill's width, with a floor. */
        const val UnlockWidthFactor = 0.82f
        val UnlockMinWidth = 108.dp
        const val RingerWidthFactor = 0.95f
        val RingerMinWidth = 130.dp
    }

    /** Today's gesture thresholds. M4 replaces them with dp- and velocity-based ones. */
    object Gesture {
        /** A card dragged sideways further than this (in px) is dismissed. */
        const val LegacySwipeDismissPx = 160f
        /** A sideways swipe on the pill longer than this minimizes or restores it. */
        val LegacyPillSwipe = 32.dp
        /** A drag up faster than this many px per event collapses the card. */
        const val LegacySwipeUpPx = 15f
    }

    // ---- Type scale: moved to IslandType (F4.1). These aliases go in a later release. ----
    @Deprecated("Use IslandType", ReplaceWith("IslandType.HeaderTitle"))
    val TypeTitle get() = IslandType.HeaderTitle
    @Deprecated("Use IslandType", ReplaceWith("IslandType.HeaderSubtitle"))
    val TypeSubtitle get() = IslandType.HeaderSubtitle
    @Deprecated("Use IslandType", ReplaceWith("IslandType.Body"))
    val TypeBody get() = IslandType.Body
    @Deprecated("Use IslandType", ReplaceWith("IslandType.CompactLabel"))
    val TypeCaption get() = IslandType.CompactLabel
    @Deprecated("Use IslandType", ReplaceWith("IslandType.Micro"))
    val TypeMicro get() = IslandType.Micro
}
