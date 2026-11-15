package com.droidbubble.app.island

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** V5.1: one colour per meaning, each readable on the black island. */
class SemanticColorTest {

    @Test
    fun eachKindMapsToItsRole() {
        assertEquals(IslandTokens.Recording, LiveActivityColors.accentFor(LiveActivityKind.RECORDING))
        assertEquals(IslandTokens.Timer, LiveActivityColors.accentFor(LiveActivityKind.TIMER))
        assertEquals(IslandTokens.Stopwatch, LiveActivityColors.accentFor(LiveActivityKind.STOPWATCH))
        assertEquals(IslandTokens.Success, LiveActivityColors.accentFor(LiveActivityKind.CALL))
        assertEquals(IslandTokens.Warning, LiveActivityColors.accentFor(LiveActivityKind.ALARM))
        assertEquals(IslandTokens.Warning, LiveActivityColors.accentFor(LiveActivityKind.ALERT))
        assertEquals(IslandTokens.Success, LiveActivityColors.accentFor(LiveActivityKind.TRACKING))
    }

    @Test
    fun theDuplicatesAreMerged() {
        assertEquals(Color(0xFFFF453A), IslandTokens.Recording)
        assertEquals(Color(0xFF30D158), IslandTokens.Success)
        assertEquals(Color(0xFFFFD60A), IslandTokens.Warning)
        assertEquals(IslandTokens.Warning, LiveActivityColors.AlarmYellow)
    }

    @Test
    fun everyRoleReadsOnBlack() {
        val roles = mapOf(
            "Recording" to IslandTokens.Recording, "Success" to IslandTokens.Success, "Warning" to IslandTokens.Warning,
            "Info" to IslandTokens.Info, "Timer" to IslandTokens.Timer, "Stopwatch" to IslandTokens.Stopwatch,
        )
        for ((name, c) in roles) {
            val contrast = (c.luminance() + 0.05f) / 0.05f
            assertTrue("$name contrast $contrast", contrast >= 4.5f)
        }
    }
}
