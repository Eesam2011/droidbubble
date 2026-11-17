package com.droidbubble.app.island

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * F2.2: the color tokens hold the exact values the views used inline, and the documented
 * duplicates really are the same color. F2.3 merged the near-duplicates.
 */
class IslandTokensTest {

    @Test
    fun roles_keepTheInlineValues() {
        assertEquals(Color(0xFFFF453A), IslandTokens.RecordingRed)
        assertEquals(Color(0xFFFF453A), IslandTokens.RingerSilentRed)
        assertEquals(Color(0xFFF2F2F7), IslandTokens.TextBody)
        assertEquals(Color(0xFF30D158), IslandTokens.ChargingGreen)
        assertEquals(Color(0xFF04190B), IslandTokens.ChargingFillDark)
        assertEquals(Color(0xFF062B12), IslandTokens.ChargingFillFrom)
        assertEquals(Color(0xFF0D3D1B), IslandTokens.ChargingFillTo)
        assertEquals(Color(0xFFD2D6FF), IslandTokens.FlashlightTint)
        assertEquals(Color(0xFFE5E5EA), IslandTokens.ControlLight)
        assertEquals(Color(0xFF2C2C2E), IslandTokens.ControlTrack)
        assertEquals(Color(0xFF52525B), IslandTokens.IconDisabled)
        assertEquals(Color(0xFF212121), IslandTokens.SurfaceMuted)
    }

    @Test
    fun documentedDuplicates_areTheSameColor() {
        assertEquals(IslandTokens.AccentRed, IslandTokens.RecordingRed)
        assertEquals(IslandTokens.AccentGreen, IslandTokens.ChargingGreen)
        assertEquals(IslandTokens.ActionPrimaryContainer, IslandTokens.ControlLight)
        assertEquals(IslandTokens.SurfaceRaised, IslandTokens.ControlTrack)
    }

    @Test
    fun liveActivityColors_keepTheirValues() {
        assertEquals(Color(0xFFFF453A), LiveActivityColors.RecordingRed)
        assertEquals(Color(0xFF5B9BFF), LiveActivityColors.TimerBlue)
        assertEquals(Color(0xFFFF9F0A), LiveActivityColors.StopwatchOrange)
        assertEquals(Color(0xFF30D158), LiveActivityColors.CallGreen)
        assertEquals(Color(0xFFFF453A), LiveActivityColors.CallRed)
        assertEquals(Color(0xFF3A3A3C), LiveActivityColors.AvatarPlaceholder)
        assertEquals(Color(0xFFFFD60A), LiveActivityColors.AlarmYellow) // V5.1: one Warning yellow
        assertEquals(Color(0xFF30D158), LiveActivityColors.TrackingGreen)
        assertEquals(Color(0xFFF2F2F7), LiveActivityColors.SportsWhite)
    }

    /** F2.3: one red, one green and one gray family. Each merge was below ΔE2000 5. */
    @Test
    fun mergedNearDuplicates_shareOneToken() {
        assertEquals(IslandTokens.RecordingRed, IslandTokens.RingerSilentRed)
        assertEquals(IslandTokens.RecordingRed, LiveActivityColors.RecordingRed)
        assertEquals(IslandTokens.AccentRed, LiveActivityColors.CallRed)
        assertEquals(IslandTokens.AccentGreen, LiveActivityColors.CallGreen)
        assertEquals(IslandTokens.AccentGreen, LiveActivityColors.TrackingGreen)
        assertEquals(IslandTokens.TextBody, LiveActivityColors.SportsWhite)
    }

    /** F2.3 leaves the high contrast values (B5) alone. */
    @Test
    fun highContrastValues_unchanged() {
        val was = IslandTokens.highContrast
        try {
            IslandTokens.highContrast = true
            assertEquals(Color(0xFFDADADF), IslandTokens.TextSecondary)
            assertEquals(Color(0xFFC7C7CC), IslandTokens.TextTertiary)
            assertEquals(Color(0xFF5A5A5E), IslandTokens.ActionSecondaryContainer)
        } finally {
            IslandTokens.highContrast = was
        }
    }

    @Test
    fun outlineStrengths() {
        assertEquals(0.15f, IslandTokens.OutlineAlphaTask, 0f)
        assertEquals(0.25f, IslandTokens.OutlineAlphaFlashlight, 0f)
        assertEquals(0.30f, IslandTokens.OutlineAlphaCharging, 0f)
    }
}
