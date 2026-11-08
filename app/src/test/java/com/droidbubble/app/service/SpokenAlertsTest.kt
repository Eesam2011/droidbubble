package com.droidbubble.app.service

import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the Spoken Alerts setting says for each kind of island alert. */
class SpokenAlertsTest {

    private fun n(
        pkg: String = "com.example",
        title: String = "",
        text: String = "",
        call: Boolean = false,
        incoming: Boolean = false,
        alert: Boolean = false,
        actions: List<String> = emptyList()
    ) = NotificationInfo(
        key = "k", packageName = pkg, appName = "App", title = title, text = text,
        isOngoing = true, isCall = call, isIncomingCall = incoming, isAlert = alert,
        actions = actions.map { NotificationAction(it, null) }
    )

    @Test
    fun incomingCallsSayTheCaller() {
        assertEquals("Call from Sam", SpokenAlerts.phraseFor(n(title = "Sam", call = true, incoming = true)))
        assertEquals("Incoming call", SpokenAlerts.phraseFor(n(call = true, incoming = true)))
        // An answered call is not announced again
        assertNull(SpokenAlerts.phraseFor(n(title = "Sam", call = true)))
    }

    @Test
    fun timersAndAlarms() {
        val clock = "com.google.android.deskclock"
        assertEquals("Time's up", SpokenAlerts.phraseFor(n(pkg = clock, title = "Time's up", alert = true, actions = listOf("+1:00", "Stop"))))
        assertEquals("Time's up. Tea", SpokenAlerts.phraseFor(n(pkg = clock, title = "Tea", text = "Time's up", alert = true, actions = listOf("+1:00", "Stop"))))
        assertEquals("Alarm. Wake up", SpokenAlerts.phraseFor(n(pkg = clock, title = "Wake up", text = "7:00 AM", alert = true, actions = listOf("Snooze", "Dismiss"))))
    }

    @Test
    fun ordinaryNotificationsStayQuiet() {
        assertNull(SpokenAlerts.phraseFor(n(pkg = "com.whatsapp", title = "Sam", text = "Hi")))
    }
}
