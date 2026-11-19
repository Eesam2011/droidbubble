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

/** C3.9: Spoken Alerts stays quiet while a screen reader is on. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@org.robolectric.annotation.Config(sdk = [35])
class SpokenAlertsScreenReaderTest {

    private val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    private val timesUp = NotificationInfo(
        key = "t", packageName = "com.google.android.deskclock", appName = "Clock", title = "Time's up", text = "Tea",
        isOngoing = true, isAlert = true
    )

    @org.junit.Before
    fun ringerNormal() {
        context.getSystemService(android.media.AudioManager::class.java).ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
    }

    @Test
    fun screenReaderOn_phraseStillComputed_butNothingSpoken() {
        val said = mutableListOf<String>()
        val alerts = SpokenAlerts(context, screenReaderOn = { true }, speakOverride = { said += it })
        assertEquals("Time's up", SpokenAlerts.phraseFor(timesUp))
        alerts.onNotification(timesUp)
        assertEquals(emptyList<String>(), said)
    }

    @Test
    fun screenReaderOff_speaks() {
        val said = mutableListOf<String>()
        SpokenAlerts(context, screenReaderOn = { false }, speakOverride = { said += it }).onNotification(timesUp)
        assertEquals(listOf("Time's up"), said)
    }

    @Test
    fun talkBackTurnedOffMidAlert_theSameAlertIsNotSpokenLate() {
        val said = mutableListOf<String>()
        var reader = true
        val alerts = SpokenAlerts(context, screenReaderOn = { reader }, speakOverride = { said += it })
        alerts.onNotification(timesUp)
        reader = false
        alerts.onNotification(timesUp)
        assertEquals(emptyList<String>(), said)
    }
}
