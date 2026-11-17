package com.droidbubble.app.island

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.layout.IslandOutline
import com.droidbubble.app.island.layout.OutlineSurface
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.KeylineMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** V6.2: every surface's outline comes from one rule. */
class OutlineResolverTest {

    private fun resolve(
        surface: OutlineSurface,
        priority: IslandPriority? = null,
        charging: Boolean = false,
        accent: Color = Color.Transparent,
        mode: KeylineMode = KeylineMode.ALWAYS,
        highContrast: Boolean = false,
        thickness: Float = 1f,
    ) = IslandOutline.resolve(surface, priority, charging, accent, mode, highContrast, thickness.dp)

    @Test
    fun off_drawsNothing() {
        for (s in OutlineSurface.entries) assertNull(resolve(s, mode = KeylineMode.OFF))
    }

    @Test
    fun highContrast_isTheSameLineOnEverySurface_notchIncluded() {
        for (s in OutlineSurface.entries) {
            for (p in listOf(null, IslandPriority.MEDIA, IslandPriority.SCREEN_RECORDING)) {
                val spec = resolve(s, p, charging = true, accent = Color.Red, mode = KeylineMode.OFF, highContrast = true)!!
                assertEquals(IslandTokens.HighContrastOutline, spec.color)
                assertEquals(0.55f, spec.color.alpha, 1e-3f)
                assertEquals(1.5.dp, spec.width)
            }
        }
        // A thicker chosen line stays thicker
        assertEquals(3.dp, resolve(OutlineSurface.PILL, highContrast = true, thickness = 3f)!!.width)
    }

    @Test
    fun everySurface_usesTheChosenWidth() {
        for (s in OutlineSurface.entries) assertEquals(2.5.dp, resolve(s, thickness = 2.5f)!!.width)
    }

    @Test
    fun tints_followTheActivity() {
        val task = IslandTokens.OutlineAlphaTask
        assertEquals(Color.White.copy(alpha = task), resolve(OutlineSurface.PILL)!!.color)
        assertEquals(IslandTokens.RecordingRed.copy(alpha = task), resolve(OutlineSurface.BUBBLE, IslandPriority.SCREEN_RECORDING)!!.color)
        assertEquals(Color.Blue.copy(alpha = task), resolve(OutlineSurface.CARD2, IslandPriority.MEDIA, accent = Color.Blue)!!.color)
        assertEquals(Color.White.copy(alpha = task), resolve(OutlineSurface.CARD1, IslandPriority.MEDIA)!!.color)
        assertEquals(Color.White.copy(alpha = IslandTokens.OutlineAlphaFlashlight), resolve(OutlineSurface.NOTCH, IslandPriority.FLASHLIGHT)!!.color)
    }

    @Test
    fun charging_tintsOnlyTheMainSurfaces() {
        val green = IslandTokens.ChargingGreen.copy(alpha = IslandTokens.OutlineAlphaCharging)
        for (s in listOf(OutlineSurface.PILL, OutlineSurface.NOTCH, OutlineSurface.CARD1)) {
            assertEquals(green, resolve(s, IslandPriority.MEDIA, charging = true)!!.color)
        }
        for (s in listOf(OutlineSurface.BUBBLE, OutlineSurface.CARD2)) {
            assertEquals(Color.White.copy(alpha = IslandTokens.OutlineAlphaTask), resolve(s, charging = true)!!.color)
        }
    }

    @Test
    fun emergence_fadesTheLine() {
        val spec = resolve(OutlineSurface.PILL, highContrast = true)!!
        assertEquals(0f, spec.colorAt(0f).alpha, 0f)
        assertEquals(0.55f * 0.5f, spec.colorAt(0.5f).alpha, 1e-3f)
        assertEquals(0.55f, spec.colorAt(2f).alpha, 1e-3f)
    }

    @Test
    fun bubbleLine_waitsForTheNeckToSnap() {
        val snap = IslandTokens.Shell.NeckSnapGap
        assertEquals(0f, IslandOutline.bubbleEmergence((-4).dp), 0f) // still overlapping the pill
        assertEquals(0f, IslandOutline.bubbleEmergence(0.dp), 0f)
        assertEquals(0.5f, IslandOutline.bubbleEmergence(snap / 2f), 1e-4f)
        assertEquals(1f, IslandOutline.bubbleEmergence(snap), 0f)
        // At rest the bubble sits a full snap gap away, so its line is at full strength
        assertEquals(1f, IslandOutline.bubbleEmergence(IslandTokens.Shell.BubbleGap), 0f)
    }
}
