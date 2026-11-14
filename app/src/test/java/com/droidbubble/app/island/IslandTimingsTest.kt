package com.droidbubble.app.island

import android.view.accessibility.AccessibilityManager
import com.droidbubble.app.model.IslandPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** F3.5: the island's timeouts and how the accessibility timeout scales them. F3.6: the retuned values. Pure JUnit. */
class IslandTimingsTest {

    @Test
    fun unscaled_areTodaysValues() {
        val t = IslandTimings.Unscaled
        assertEquals(3_000L, t.notificationAuto)
        assertEquals(4_500L, t.popIn)
        assertEquals(2_200L, t.unlock)
        assertEquals(3_000L, t.connection)
        assertEquals(2_500L, t.ringer)
        assertEquals(4_000L, t.charging)
    }

    @Test
    fun autoCollapse_byOpenCard() {
        val t = IslandTimings.Unscaled
        // F3.6 (05 R6): was 3 s for every card
        assertEquals(8_000L, t.autoCollapseFor(IslandPriority.MEDIA, isAlarm = false))
        for (p in IslandPriority.entries - IslandPriority.MEDIA) {
            assertEquals("$p", 6_000L, t.autoCollapseFor(p, isAlarm = false))
        }
        // A ringing call or alarm stays until it is answered or stopped
        for (p in IslandPriority.entries) {
            assertNull("$p", t.autoCollapseFor(p, isAlarm = true))
        }
    }

    @Test
    fun everyTimeoutAsksTheRecommendation() {
        // "Time to take action: 1 minute": the system returns the longer of the two
        val oneMinute = IslandTimings { ms, _ -> maxOf(ms, 60_000) }
        for (value in listOf(oneMinute.autoCollapseFor(IslandPriority.MEDIA, false),
            oneMinute.autoCollapseFor(IslandPriority.POP_NOTIFICATION, false), oneMinute.notificationAuto, oneMinute.popIn, oneMinute.unlock,
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

        t.autoCollapseFor(IslandPriority.POP_NOTIFICATION, isAlarm = false)
        assertEquals("an open card has buttons to reach", controls or text, seen[6_000])
        t.autoCollapseFor(IslandPriority.MEDIA, isAlarm = false)
        assertEquals(controls or text, seen[8_000])
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
        assertEquals(6_000L, t.autoCollapseFor(IslandPriority.POP_NOTIFICATION, false))
        factor = 2
        assertEquals(12_000L, t.autoCollapseFor(IslandPriority.POP_NOTIFICATION, false))
    }
}
