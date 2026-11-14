package com.droidbubble.app.island

import android.view.HapticFeedbackConstants
import android.view.HapticFeedbackConstants as H
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F3.3: the haptics switch is always respected, and Legacy maps each event to what it felt like before.
 * F3.4: Droid, the default, follows 05 §6.
 */
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
        assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY), performed)
    }

    @Test
    fun droidIsTheDefault() {
        haptics(map = HapticMap.Droid).perform(IslandHapticEvent.TAP_EXPAND)
        IslandHaptics(perform = { performed += it }, enabled = { true }, sdk = 34).perform(IslandHapticEvent.TAP_EXPAND)
        assertEquals(performed[0], performed[1])
    }

    /** 05 §6 / F3.4 table: event → (API 34+, API 30–33, API 29). */
    @Test
    fun droid_matchesTheSpecTable() {
        val table = mapOf(
            IslandHapticEvent.TAP_EXPAND to listOf(H.VIRTUAL_KEY, H.VIRTUAL_KEY, H.VIRTUAL_KEY),
            IslandHapticEvent.RESTORE to listOf(H.VIRTUAL_KEY, H.VIRTUAL_KEY, H.VIRTUAL_KEY),
            IslandHapticEvent.COLLAPSE to listOf(H.CLOCK_TICK, H.CLOCK_TICK, H.CLOCK_TICK),
            IslandHapticEvent.LONG_PRESS to listOf(H.LONG_PRESS, H.LONG_PRESS, H.LONG_PRESS),
            IslandHapticEvent.THRESHOLD_CROSSED to listOf(H.GESTURE_THRESHOLD_ACTIVATE, H.CLOCK_TICK, H.CLOCK_TICK),
            IslandHapticEvent.GESTURE_COMMIT to listOf(H.GESTURE_END, H.GESTURE_END, H.CONTEXT_CLICK),
            IslandHapticEvent.DISMISS to listOf(H.GESTURE_END, H.GESTURE_END, H.CONTEXT_CLICK),
            IslandHapticEvent.MINIMISE to listOf(H.GESTURE_END, H.GESTURE_END, H.CONTEXT_CLICK),
            IslandHapticEvent.HIDE to listOf(H.GESTURE_END, H.GESTURE_END, H.CONTEXT_CLICK),
            IslandHapticEvent.TOGGLE_ON to listOf(H.TOGGLE_ON, H.CONTEXT_CLICK, H.CONTEXT_CLICK),
            IslandHapticEvent.TOGGLE_OFF to listOf(H.TOGGLE_OFF, H.CONTEXT_CLICK, H.CONTEXT_CLICK),
            IslandHapticEvent.SEGMENT_TICK to listOf(H.SEGMENT_TICK, H.CLOCK_TICK, H.CLOCK_TICK),
            IslandHapticEvent.SEGMENT_FREQUENT_TICK to listOf(H.SEGMENT_FREQUENT_TICK, null, null),
            IslandHapticEvent.CONFIRM to listOf(H.CONFIRM, H.CONFIRM, H.KEYBOARD_TAP),
            IslandHapticEvent.REJECT to listOf(H.REJECT, H.REJECT, H.LONG_PRESS),
            IslandHapticEvent.NECK_SNAP to listOf(H.SEGMENT_FREQUENT_TICK, H.CLOCK_TICK, H.CLOCK_TICK),
            IslandHapticEvent.MERGE to listOf(H.SEGMENT_FREQUENT_TICK, H.CLOCK_TICK, H.CLOCK_TICK),
            IslandHapticEvent.SYSTEM_ALERT_CHARGING to listOf(null, null, null),
            IslandHapticEvent.SYSTEM_ALERT_RINGER to listOf(null, null, null),
            IslandHapticEvent.IDLE_TAP to listOf(H.CONFIRM, H.CONFIRM, H.KEYBOARD_TAP),
        )
        assertEquals("every event is in the table", IslandHapticEvent.entries.toSet(), table.keys)
        for ((event, expected) in table) {
            for ((sdks, constant) in listOf(listOf(34, 36), listOf(30, 33), listOf(29)).zip(expected)) {
                for (sdk in sdks) assertEquals("$event at API $sdk", constant, HapticMap.Droid.constantFor(event, sdk))
            }
        }
    }

    @Test
    fun droid_systemAlertsAreSilent() {
        val h = haptics(map = HapticMap.Droid)
        h.perform(IslandHapticEvent.SYSTEM_ALERT_CHARGING)
        h.perform(IslandHapticEvent.SYSTEM_ALERT_RINGER)
        assertTrue(performed.isEmpty())
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
