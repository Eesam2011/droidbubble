package com.droidbubble.app.island

import android.view.accessibility.AccessibilityManager
import org.junit.Assert.assertEquals
import org.junit.Test

/** F3.5: the island's timeouts and how the accessibility timeout scales them. Pure JUnit. */
class IslandTimingsTest {

    @Test
    fun unscaled_areTodaysValues() {
        val t = IslandTimings.Unscaled
        assertEquals(3_000L, t.autoCollapse)
        assertEquals(3_000L, t.notificationAuto)
        assertEquals(4_500L, t.popIn)
        assertEquals(2_200L, t.unlock)
        assertEquals(3_000L, t.connection)
        assertEquals(2_500L, t.ringer)
        assertEquals(4_000L, t.charging)
    }

    @Test
    fun everyTimeoutAsksTheRecommendation() {
        // "Time to take action: 1 minute": the system returns the longer of the two
        val oneMinute = IslandTimings { ms, _ -> maxOf(ms, 60_000) }
        for (value in listOf(oneMinute.autoCollapse, oneMinute.notificationAuto, oneMinute.popIn, oneMinute.unlock,
            oneMinute.connection, oneMinute.ringer, oneMinute.charging)) {
            assertEquals(60_000L, value)
        }
    }

    @Test
    fun flags_describeWhatEachTimeoutShows() {
        val seen = HashMap<Int, Int>()
        val t = IslandTimings { ms, flags -> seen[ms] = flags; ms }
        val icons = AccessibilityManager.FLAG_CONTENT_ICONS
        val text = AccessibilityManager.FLAG_CONTENT_TEXT
        val controls = AccessibilityManager.FLAG_CONTENT_CONTROLS

        t.autoCollapse
        assertEquals("an open card has buttons to reach", controls or text, seen[3_000])
        t.popIn
        assertEquals(text, seen[4_500])
        t.unlock
        assertEquals(icons, seen[2_200])
        t.ringer
        assertEquals(icons, seen[2_500])
        t.charging
        assertEquals(icons or text, seen[4_000])
    }

    @Test
    fun valuesAreReadEachTime_soASettingChangeApplies() {
        var factor = 1
        val t = IslandTimings { ms, _ -> ms * factor }
        assertEquals(3_000L, t.autoCollapse)
        factor = 2
        assertEquals(6_000L, t.autoCollapse)
    }
}
