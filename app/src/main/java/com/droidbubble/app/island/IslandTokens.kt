package com.droidbubble.app.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    /** Outline drawn around the island in high contrast mode, so it stands out on dark wallpapers. */
    val HighContrastOutline = Color.White.copy(alpha = 0.55f)

    // ---- Type scale (base sizes; views may scale by settings.expandedScale) ----
    val TypeTitle = 17.sp
    val TypeSubtitle = 13.sp
    val TypeBody = 15.sp
    val TypeCaption = 12.sp
    val TypeMicro = 11.sp
}
