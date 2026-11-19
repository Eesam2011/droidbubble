package com.droidbubble.app.island

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.IslandA11yAction.COLLAPSE
import com.droidbubble.app.island.IslandA11yAction.DISMISS
import com.droidbubble.app.island.IslandA11yAction.HIDE
import com.droidbubble.app.island.IslandA11yAction.MINIMISE
import com.droidbubble.app.island.IslandA11yAction.PAUSE_ISLAND
import com.droidbubble.app.island.IslandA11yAction.TURN_OFF_FLASHLIGHT
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.SpeechSubject.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

/** C3.3: every row of the phase 8 semantics tables, ICU durations and locale formatting. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class IslandSpeechTest {

    private val res = ApplicationProvider.getApplicationContext<Context>().resources
    private val speech = IslandSpeech(res, Locale.US)

    private fun said(s: SpeechSubject) = speech.headline(s) to speech.value(s)

    // --- Activity table -------------------------------------------------------------------

    @Test
    fun media() {
        assertEquals("Now playing: Song, Artist" to "Playing", said(Media("Song", "Artist", isPlaying = true)))
        assertEquals("Now playing: Song" to "Paused", said(Media("Song", "", isPlaying = false)))
    }

    @Test
    fun message() {
        val long = "x".repeat(120)
        assertEquals("WhatsApp: Sam" to "Hello there", said(Message("WhatsApp", "Sam", "Hello there")))
        assertEquals(80, speech.value(Message("WhatsApp", "Sam", long))!!.length)
    }

    @Test
    fun calls() {
        assertEquals("Incoming call: Sam Carter" to null, said(IncomingCall("Sam Carter")))
        assertEquals("Call: Sam Carter" to "1 minute, 5 seconds", said(OngoingCall("Sam Carter", 65_000L)))
    }

    @Test
    fun timesUpAndAlarm() {
        assertEquals("Time's up: Tea" to null, said(TimesUp("Tea")))
        assertEquals("Time's up" to null, said(TimesUp(null)))
        assertEquals("Alarm: Wake up" to "7:00 AM", said(Alarm("Wake up", "7:00 AM")))
    }

    @Test
    fun timer() {
        assertEquals("Timer" to "4 minutes, 59 seconds left", said(Timer(null, 299_000L, paused = false)))
        assertEquals("Timer, Tea" to "Paused, 4 minutes, 59 seconds left", said(Timer("Tea", 299_000L, paused = true)))
    }

    @Test
    fun stopwatchRecorderScreenRecording() {
        assertEquals("Stopwatch" to "12 seconds", said(Stopwatch(12_000L, paused = false)))
        assertEquals("Recording" to "Paused", said(Recording(12_000L, paused = true)))
        assertEquals("Screen recording" to "1 hour, 2 minutes", said(ScreenRecording(3_720_000L, paused = false)))
    }

    @Test
    fun trackingScoreDownload() {
        assertEquals("Driver arriving" to "5 min, 20%", said(Tracking("Driver arriving", "5 min", 20)))
        assertEquals("Arsenal 2 - 1 Chelsea" to "67'", said(Score("Arsenal 2 - 1 Chelsea", "67'")))
        assertEquals("Downloading Droidbubble" to "40%", said(Download("Downloading Droidbubble", 40)))
    }

    @Test
    fun flashlightAndNetwork() {
        assertEquals("Flashlight on" to "Brightness 60%", said(Flashlight(60)))
        assertEquals("Network speed" to "Down 2.1 MB/s, up 300 KB/s", said(NetworkSpeed("2.1 MB/s", "300 KB/s")))
    }

    // --- Element table --------------------------------------------------------------------

    @Test
    fun glances() {
        assertEquals("Charging" to "54%", said(Charging(54)))
        assertEquals("Unlocked" to null, said(Unlocked))
        assertEquals("Pixel Buds connected" to null, said(Connected("Pixel Buds")))
        assertEquals("Silent mode", speech.headline(Ringer(RingerMode.SILENT)))
        assertEquals("Vibrate mode", speech.headline(Ringer(RingerMode.VIBRATE)))
        assertEquals("Ring mode", speech.headline(Ringer(RingerMode.RING)))
        for (g in listOf(Charging(54), Unlocked, Connected("Car"), Ringer(RingerMode.SILENT))) {
            assertNull("glances can't be tapped", speech.clickLabel(g))
            assertTrue(speech.availableActions(SpeechNode.PILL, g).isEmpty())
        }
    }

    @Test
    fun minimisedRing() {
        val ring = Minimised(Timer(null, 299_000L, false))
        assertEquals("Island minimised" to "Timer", said(ring))
        assertEquals("Restore", speech.clickLabel(ring))
        assertEquals(listOf(HIDE), speech.availableActions(SpeechNode.PILL, ring))
    }

    @Test
    fun clickAndLongClickLabels() {
        assertEquals("Expand", speech.clickLabel(Timer(null, 1_000L, false)))
        assertEquals("Open in Clock", speech.longClickLabel("Clock", OpenTarget.APP))
        assertEquals("Open conversation", speech.longClickLabel("WhatsApp", OpenTarget.CONVERSATION))
    }

    @Test
    fun cardAndBackdrop() {
        assertEquals("Island: Timer", speech.paneTitle(Timer(null, 1_000L, false)))
        assertEquals("Island: Incoming call", speech.paneTitle(IncomingCall("Sam Carter")))
        assertEquals("Island: WhatsApp", speech.paneTitle(Message("WhatsApp", "Sam", "Hi")))
        assertEquals("Close island", speech.closeIsland())
        assertEquals("Close", speech.closeLabel())
    }

    // --- Actions --------------------------------------------------------------------------

    @Test
    fun actionsPerNode() {
        val timer = Timer(null, 1_000L, false)
        assertEquals(listOf(MINIMISE, HIDE, DISMISS), speech.availableActions(SpeechNode.PILL, timer))
        assertEquals(listOf(MINIMISE, HIDE, DISMISS, PAUSE_ISLAND), speech.availableActions(SpeechNode.PILL, timer, pauseAvailable = true))
        assertEquals(listOf(DISMISS), speech.availableActions(SpeechNode.BUBBLE, timer))
        assertEquals(listOf(COLLAPSE, DISMISS), speech.availableActions(SpeechNode.CARD, timer))
        assertEquals(listOf(MINIMISE, HIDE, TURN_OFF_FLASHLIGHT), speech.availableActions(SpeechNode.PILL, Flashlight(60)))
        assertEquals("Hide until next update", speech.actionLabel(HIDE))
        assertEquals("Turn off flashlight", speech.actionLabel(TURN_OFF_FLASHLIGHT))
        assertEquals("Pause island for 1 hour", speech.actionLabel(PAUSE_ISLAND))
    }

    @Test
    fun noDismissForRingingCallAlarmTimesUpOrScreenRecording() {
        for (s in listOf(IncomingCall("Sam Carter"), Alarm("Wake up", null), TimesUp("Tea"), ScreenRecording(1L, false), Flashlight(10))) {
            for (node in SpeechNode.entries) {
                assertFalse("$s on $node", DISMISS in speech.availableActions(node, s))
            }
        }
    }

    // --- Announcements --------------------------------------------------------------------

    @Test
    fun announcements() {
        assertEquals(Announcement(Politeness.ASSERTIVE, "Incoming call: Sam Carter"), speech.announcement(IncomingCall("Sam Carter")))
        assertEquals(Announcement(Politeness.ASSERTIVE, "Alarm: Wake up"), speech.announcement(Alarm("Wake up", "7:00 AM")))
        assertEquals(Announcement(Politeness.POLITE, "WhatsApp: Sam"), speech.announcement(Message("WhatsApp", "Sam", "Hi")))
        assertEquals(Announcement(Politeness.POLITE, "Timer"), speech.announcement(Timer(null, 1_000L, false)))
        assertEquals(Announcement(Politeness.POLITE, "Car connected"), speech.announcement(Connected("Car")))
        assertNull(speech.announcement(Media("Song", "Artist", true)))
        assertNull(speech.announcement(Unlocked))
    }

    // --- From notifications ---------------------------------------------------------------

    @Test
    fun subjectsFromFixtures() {
        val now = IslandClock.wallMs()
        val timer = IslandFixtures.timer.copy(liveTimerBaseMs = now + 299_000L)
        assertEquals("Timer" to "4 minutes, 59 seconds left", said(IslandSpeech.subjectFor(timer, now)))
        assertEquals(IncomingCall("Sam Carter"), IslandSpeech.subjectFor(IslandFixtures.incomingCall, now))
        assertEquals("Time's up: Tea", speech.headline(IslandSpeech.subjectFor(IslandFixtures.timesUp, now)))
        assertEquals("Alarm: Wake up" to "7:00 AM", said(IslandSpeech.subjectFor(IslandFixtures.alarmRinging, now)))
        assertEquals("WhatsApp: Sam", speech.headline(IslandSpeech.subjectFor(IslandFixtures.message(), now)))
        assertEquals("Driver arriving" to "5 min, 20%", said(IslandSpeech.subjectFor(IslandFixtures.ride, now)))
    }

    // --- Formatting -----------------------------------------------------------------------

    @Test
    fun durationsAndPercentages() {
        assertEquals("0 seconds", speech.spokenDuration(0L))
        assertEquals("1 second", speech.spokenDuration(1_000L))
        assertEquals("4 minutes, 59 seconds", speech.spokenDuration(299_000L))
        assertEquals("54%", speech.spokenPercent(54))
    }

    @Test
    fun spanishLocale() {
        val es = IslandSpeech(res, Locale.forLanguageTag("es-ES"))
        val d = es.spokenDuration(299_000L)
        assertTrue(d, d.contains("4 minutos") && d.contains("59 segundos"))
        // Spanish puts a space before the sign
        assertEquals("54 %", es.spokenPercent(54).replace(' ', ' '))
    }
}
