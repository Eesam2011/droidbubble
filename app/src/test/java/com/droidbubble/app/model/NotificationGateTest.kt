package com.droidbubble.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** B3.1: which toggle lets a notification through. */
class NotificationGateTest {

    @Test
    fun gateKind_truthTable() {
        // isCall, isLive, hasProgress -> kind
        val table = listOf(
            Triple(true, true, true) to NotificationGateKind.CALL,
            Triple(true, false, false) to NotificationGateKind.CALL,
            Triple(false, true, true) to NotificationGateKind.PROGRESS,
            Triple(false, false, true) to NotificationGateKind.PROGRESS,
            Triple(false, true, false) to NotificationGateKind.LIVE,
            Triple(false, false, false) to NotificationGateKind.MESSAGE,
        )
        for ((input, expected) in table) {
            val (call, live, progress) = input
            assertEquals("call=$call live=$live progress=$progress", expected, gateKind(call, live, progress))
        }
    }

    @Test
    fun freshInstall_allowsCallsLiveAndProgress_butNotMessages() {
        val fresh = IslandSettings()
        assertTrue(fresh.allows(NotificationGateKind.CALL))
        assertTrue(fresh.allows(NotificationGateKind.LIVE))
        assertTrue(fresh.allows(NotificationGateKind.PROGRESS))
        assertFalse(fresh.allows(NotificationGateKind.MESSAGE))
    }

    @Test
    fun eachToggle_governsOnlyItsKind() {
        val all = IslandSettings(showNotifications = true)
        assertFalse(all.copy(showCalls = false).allows(NotificationGateKind.CALL))
        assertTrue(all.copy(showCalls = false).allows(NotificationGateKind.LIVE))
        assertFalse(all.copy(showLiveActivities = false).allows(NotificationGateKind.LIVE))
        assertTrue(all.copy(showLiveActivities = false).allows(NotificationGateKind.MESSAGE))
        assertFalse(all.copy(showLiveProgress = false).allows(NotificationGateKind.PROGRESS))
        assertFalse(all.copy(showNotifications = false).allows(NotificationGateKind.MESSAGE))
        assertTrue(all.copy(showNotifications = false).allows(NotificationGateKind.LIVE))
    }
}
