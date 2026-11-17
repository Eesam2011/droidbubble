package com.droidbubble.app.island.layout

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.KeylineMode

/** V6.2: the island's surfaces that draw the outline. */
enum class OutlineSurface {
    PILL, NOTCH, BUBBLE, CARD1, CARD2;

    /** The main activity's surfaces; the bubble and card 2 show the second one. */
    val isPrimary: Boolean get() = this == PILL || this == NOTCH || this == CARD1
}

/** V6.2: one surface's line at full emergence. */
data class OutlineSpec(val color: Color, val width: Dp) {
    /** The line while the surface emerges: [emergence] 0 hides it, 1 shows it at full strength. */
    fun colorAt(emergence: Float): Color = color.copy(alpha = color.alpha * emergence.coerceIn(0f, 1f))
}

/**
 * V6.2: the one rule for every island outline (pill, notch, bubble and both cards): the same
 * width everywhere, the activity's tint, and with high contrast a 55 % white line at least
 * 1.5 dp wide. AUTO is resolved by the overlay service, so here the mode is OFF or ALWAYS.
 */
object IslandOutline {
    fun resolve(
        surface: OutlineSurface,
        priority: IslandPriority?,
        charging: Boolean,
        mediaAccent: Color,
        keylineMode: KeylineMode,
        highContrast: Boolean,
        thickness: Dp,
    ): OutlineSpec? {
        if (highContrast) return OutlineSpec(IslandTokens.HighContrastOutline, max(thickness, IslandTokens.HighContrastOutlineWidth))
        if (keylineMode != KeylineMode.ALWAYS) return null
        return OutlineSpec(tint(surface, priority, charging, mediaAccent), thickness)
    }

    private fun tint(surface: OutlineSurface, priority: IslandPriority?, charging: Boolean, mediaAccent: Color): Color {
        val task = IslandTokens.OutlineAlphaTask
        return when {
            charging && surface.isPrimary -> IslandTokens.ChargingGreen.copy(alpha = IslandTokens.OutlineAlphaCharging)
            priority == IslandPriority.SCREEN_RECORDING -> IslandTokens.RecordingRed.copy(alpha = task)
            priority == IslandPriority.MEDIA && mediaAccent != Color.Transparent -> mediaAccent.copy(alpha = task)
            priority == IslandPriority.FLASHLIGHT -> Color.White.copy(alpha = IslandTokens.OutlineAlphaFlashlight)
            else -> Color.White.copy(alpha = task)
        }
    }

    /**
     * The bubble's line fades in as the liquid neck snaps ([gap] between pill and bubble, 0 to
     * the snap gap), so no second edge is drawn while the two still touch.
     */
    fun bubbleEmergence(gap: Dp, snapGap: Dp = IslandTokens.Shell.NeckSnapGap): Float =
        if (snapGap.value <= 0f) 1f else (gap / snapGap).coerceIn(0f, 1f)
}
