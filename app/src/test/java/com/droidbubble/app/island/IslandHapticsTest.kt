package com.droidbubble.app.island

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F3.3: the haptics switch is always respected, and Legacy maps each event to what it felt like before. */
class IslandHapticsTest {

    private val performed = ArrayList<Int>()
    private fun haptics(enabled: Boolean = true, sdk: Int = 34, map: HapticMap = HapticMap.Legacy) =
        IslandHaptics(perform = { performed += it }, enabled = { enabled }, map = map, sdk = sdk)

    @Test
    fun switchOff_nothingVibrates() {
        val h = haptics(enabled = false)
        IslandHapticEvent.entries.forEach(h::perform)
        assertTrue(performed.isEmpty())
    }

    @Test
    fun switchIsReadOnEveryEvent() {
        var on = false
        val h = IslandHaptics(perform = { performed += it }, enabled = { on }, sdk = 34)
        h.perform(IslandHapticEvent.TAP_EXPAND)
        on = true
        h.perform(IslandHapticEvent.TAP_EXPAND)
        assertEquals(listOf(HapticFeedbackConstants.CONTEXT_CLICK), performed)
    }

    @Test
    fun legacy_matchesTheOldCallSites() {
        val expected = mapOf(
            IslandHapticEvent.TAP_EXPAND to HapticFeedbackConstants.CONTEXT_CLICK,
            IslandHapticEvent.DISMISS to HapticFeedbackConstants.CONTEXT_CLICK,
            IslandHapticEvent.MINIMISE to HapticFeedbackConstants.CONTEXT_CLICK,
            IslandHapticEvent.RESTORE to HapticFeedbackConstants.CONTEXT_CLICK,
            IslandHapticEvent.HIDE to HapticFeedbackConstants.CONTEXT_CLICK,
            IslandHapticEvent.COLLAPSE to HapticFeedbackConstants.CLOCK_TICK,
            IslandHapticEvent.LONG_PRESS to HapticFeedbackConstants.LONG_PRESS,
            IslandHapticEvent.SYSTEM_ALERT_CHARGING to HapticFeedbackConstants.LONG_PRESS,
            // The flashlight slider and its off button
            IslandHapticEvent.SEGMENT_TICK to HapticFeedbackConstants.CLOCK_TICK,
            IslandHapticEvent.TOGGLE_OFF to HapticFeedbackConstants.CONFIRM,
            IslandHapticEvent.IDLE_TAP to HapticFeedbackConstants.CONFIRM,
            // The ringer switch: one tick, no raw vibrator pattern
            IslandHapticEvent.SYSTEM_ALERT_RINGER to HapticFeedbackConstants.CLOCK_TICK,
        )
        for (sdk in listOf(30, 34)) {
            for ((event, constant) in expected) {
                assertEquals("$event at API $sdk", constant, HapticMap.Legacy.constantFor(event, sdk))
            }
        }
    }

    @Test
    fun legacy_api29_fallsBackWhereConfirmIsMissing() {
        // CONFIRM and REJECT arrived in API 30
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, HapticMap.Legacy.constantFor(IslandHapticEvent.IDLE_TAP, 29))
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, HapticMap.Legacy.constantFor(IslandHapticEvent.TOGGLE_OFF, 29))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, HapticMap.Legacy.constantFor(IslandHapticEvent.REJECT, 29))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, HapticMap.Legacy.constantFor(IslandHapticEvent.TAP_EXPAND, 29))
    }

    @Test
    fun legacy_newEventsAreSilent() {
        for (event in listOf(IslandHapticEvent.THRESHOLD_CROSSED, IslandHapticEvent.NECK_SNAP, IslandHapticEvent.MERGE, IslandHapticEvent.SEGMENT_FREQUENT_TICK)) {
            assertNull(HapticMap.Legacy.constantFor(event, 34))
        }
        val h = haptics()
        h.perform(IslandHapticEvent.NECK_SNAP)
        assertTrue(performed.isEmpty())
    }

    @Test
    fun none_neverPerforms() {
        IslandHapticEvent.entries.forEach(IslandHaptics.None::perform)
    }
}
