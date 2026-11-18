package com.droidbubble.app.island.activity

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.LiveActivityTester
import com.droidbubble.app.island.LiveActivityTester.Demo
import com.droidbubble.app.island.icon.IconEffect
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import com.droidbubble.app.service.NotificationReceiverService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** A1.2: today's rules as specs, over every Test-lab demo and the call, clock, recorder and progress fixtures. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LegacySpecMapperTest {

    private val now = 1_000_000_000L

    @Before
    fun setUp() {
        NotificationReceiverService.parserDispatcherForTest = kotlinx.coroutines.Dispatchers.Unconfined
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        NotificationReceiverService.parserDispatcherForTest = null
        IslandStateManager.resetForTest()
    }

    private fun spec(demo: Demo) = LegacySpecMapper.fromNotification(LiveActivityTester.demoNotification(demo, now))

    @Test
    fun everyDemo_kindAndTier() {
        val expected = mapOf(
            Demo.CALL to (ActivityKind.CALL_RINGING to Tier.T0_INTERRUPT),
            Demo.TIMER to (ActivityKind.TIMER to Tier.T1_LIVE),
            Demo.TIMES_UP to (ActivityKind.ALERT to Tier.T0_INTERRUPT),
            Demo.ALARM to (ActivityKind.ALERT to Tier.T0_INTERRUPT),
            Demo.SNOOZED to (ActivityKind.ALARM to Tier.T2_ONGOING),
            Demo.STOPWATCH to (ActivityKind.STOPWATCH to Tier.T1_LIVE),
            Demo.RECORDER to (ActivityKind.VOICE_RECORDING to Tier.T1_LIVE),
            Demo.DOWNLOAD to (ActivityKind.PROGRESS to Tier.T2_ONGOING),
            // A ride with an ETA chip is live; a flight saying "Boarding" has a chip too
            Demo.RIDE to (ActivityKind.TRACKING to Tier.T1_LIVE),
            Demo.DELIVERY to (ActivityKind.TRACKING to Tier.T1_LIVE),
            Demo.FLIGHT to (ActivityKind.TRACKING to Tier.T1_LIVE),
            Demo.SCORE to (ActivityKind.SPORTS to Tier.T2_ONGOING),
        )
        assertEquals("every demo has a row", Demo.entries.toSet(), expected.keys)
        for ((demo, kt) in expected) {
            val s = spec(demo)
            assertEquals("$demo kind", kt.first, s.kind)
            assertEquals("$demo tier", kt.second, s.tier)
            assertEquals("$demo id", ActivityId.notification(LiveActivityTester.demoNotification(demo, now).key), s.id)
        }
    }

    @Test
    fun demoSlots() {
        spec(Demo.CALL).let {
            assertEquals(Slot.Icon(IslandIcon.CALL, IconEffect.Wiggle(repeatEveryMs = 2_400L)), it.compactLeading)
            assertEquals(OrderBucket.CALL, it.bucket)
            assertEquals(ExpandedPresentation.CALL, it.expanded.presentation)
            assertEquals(AccentRole.SUCCESS, it.accent)
        }
        spec(Demo.TIMER).let {
            assertTrue(it.compactLeading is Slot.Ring)
            assertEquals(Slot.Timer(now + 5 * 60_000L, countDown = true, pausedText = null), it.compactTrailing)
            assertEquals(ExpandedPresentation.TIMER_ROW, it.expanded.presentation)
            assertEquals(AccentRole.TIMER, it.accent)
        }
        spec(Demo.STOPWATCH).let {
            assertEquals(Slot.ClockFace(now - 83_000L, countDown = false), it.compactLeading)
            assertEquals(AccentRole.STOPWATCH, it.accent)
        }
        spec(Demo.RECORDER).let {
            assertEquals(Slot.Waveform, it.compactLeading)
            assertEquals(Slot.Icon(IslandIcon.MIC, IconEffect.Pulse()), it.minimal)
            assertEquals(ExpandedPresentation.RECORDING_ROW, it.expanded.presentation)
        }
        spec(Demo.TIMES_UP).let {
            assertEquals(Slot.Icon(IslandIcon.TIMER, IconEffect.Wiggle()), it.compactLeading)
            assertEquals(Slot.Text("Time's up", numeric = true), it.compactTrailing)
            assertTrue(it.alerting)
        }
        spec(Demo.ALARM).let {
            assertEquals(Slot.Icon(IslandIcon.ALARM, IconEffect.Wiggle()), it.compactLeading)
            assertEquals(Slot.Text("7:00 AM", numeric = true), it.compactTrailing)
        }
        spec(Demo.DOWNLOAD).let {
            assertEquals(Slot.Image(ImageRef.AppIcon(IslandIcon.DOWNLOAD)), it.compactLeading)
            assertEquals(Slot.Progress(0f), it.compactTrailing)
            assertEquals(ExpandedPresentation.PROGRESS_CARD, it.expanded.presentation)
            assertEquals(OrderBucket.PROGRESS, it.bucket)
        }
        spec(Demo.RIDE).let {
            assertEquals(Slot.Image(ImageRef.Avatar(IslandIcon.DELIVERY)), it.compactLeading)
            assertEquals(Slot.Text("5 min", numeric = true), it.compactTrailing)
            assertEquals(ExpandedPresentation.TRACKING_ROW, it.expanded.presentation)
        }
        spec(Demo.SCORE).let {
            assertEquals(Slot.Icon(IslandIcon.SPORTS), it.compactLeading)
            assertEquals(Slot.Text("2–1", numeric = true), it.compactTrailing)
            assertEquals(ExpandedPresentation.SCORE_ROW, it.expanded.presentation)
        }
    }

    @Test
    fun message_isABannerWithAvatarAndFirstName() {
        val msg = NotificationInfo(key = "m", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam Carter", text = "Hi", timestamp = now)
        val s = LegacySpecMapper.fromNotification(msg)
        assertEquals(ActivityKind.MESSAGE, s.kind)
        assertEquals(Tier.T3_BANNER, s.tier)
        assertEquals(Slot.Custom(CustomSlot.CONTACT_AVATAR), s.compactLeading)
        assertEquals(Slot.Text("Sam"), s.compactTrailing)
        assertEquals(ExpandedPresentation.NOTIFICATION_CARD, s.expanded.presentation)
        // A message comes back on new text (B4.5)
        assertNotEquals(s.stateSignature, LegacySpecMapper.fromNotification(msg.copy(text = "Hello?")).stateSignature)
    }

    @Test
    fun stateSignature_ignoresTicks_keepsStartedAt() {
        val ride = LiveActivityTester.demoNotification(Demo.RIDE, now)
        val first = LegacySpecMapper.fromNotification(ride)
        val tick = LegacySpecMapper.fromNotification(ride.copy(shortCriticalText = "4 min", progress = 20, timestamp = now + 60_000L), first)
        assertEquals(first.stateSignature, tick.stateSignature)
        assertEquals(first.startedAt, tick.startedAt)
        assertEquals("an ETA tick isn't significant", first.lastSignificantAt, tick.lastSignificantAt)
        val arrived = LegacySpecMapper.fromNotification(ride.copy(title = "Driver has arrived", timestamp = now + 120_000L), tick)
        assertNotEquals(first.stateSignature, arrived.stateSignature)
        assertEquals(now + 120_000L, arrived.lastSignificantAt)
        assertEquals(first.startedAt, arrived.startedAt)
    }

    @Test
    fun otherSources() {
        val media = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "Artist", isPlaying = false)
        LegacySpecMapper.fromMedia(media, now).let {
            assertEquals(ActivityId("media:com.spotify.music"), it.id)
            assertEquals(Tier.T2_ONGOING, it.tier)
            assertEquals(now + LegacySpecMapper.STALE_PAUSED_MEDIA_MS, it.staleAt)
            assertEquals(Slot.Custom(CustomSlot.MEDIA_ART), it.compactLeading)
        }
        assertNull(LegacySpecMapper.fromMedia(media.copy(isPlaying = true), now).staleAt)
        LegacySpecMapper.fromRecording(ScreenRecordingState(isRecording = true, durationSeconds = 5), now).let {
            assertEquals(ActivityKind.SCREEN_RECORDING, it.kind)
            assertEquals(Tier.T1_LIVE, it.tier)
            assertEquals(Slot.RecordingDot, it.compactLeading)
            assertEquals(Slot.Timer(now - 5_000L, countDown = false, pausedText = null), it.compactTrailing)
        }
        LegacySpecMapper.fromTorch(3, 5, now).let {
            assertEquals(ActivityId.TORCH, it.id)
            assertEquals(OrderBucket.FLASHLIGHT, it.bucket)
            assertEquals(ExpandedPresentation.FLASHLIGHT_CARD, it.expanded.presentation)
        }
    }

    /** Fixture name to the kind and tier its spec gets, through the real listener. */
    private val fixtures = mapOf(
        "call/gphone_incoming_en" to (ActivityKind.CALL_RINGING to Tier.T0_INTERRUPT),
        "call/gphone_screening_en" to (ActivityKind.CALL_ONGOING to Tier.T1_LIVE),
        // A4: not yet understood as ringing (Arabic, Spanish, Turkish); A3.2 / A4.3 flip these
        "call/whatsapp_incoming_ar" to (ActivityKind.CALL_ONGOING to Tier.T1_LIVE),
        "call/whatsapp_video_es" to (ActivityKind.CALL_ONGOING to Tier.T1_LIVE),
        "call/samsung_incoming_tr" to (ActivityKind.CALL_ONGOING to Tier.T1_LIVE),
        "clock/gclock_alarm_ringing_es" to (ActivityKind.ALERT to Tier.T0_INTERRUPT),
        "clock/gclock_timesup_fr" to (ActivityKind.ALERT to Tier.T0_INTERRUPT),
        "clock/gclock_stopwatch_en" to (ActivityKind.STOPWATCH to Tier.T1_LIVE),
        "clock/gclock_timer_running_en" to (ActivityKind.TIMER to Tier.T1_LIVE),
        "clock/samsung_timer_ar" to (ActivityKind.TIMER to Tier.T1_LIVE),
        "recorder/grecorder_en" to (ActivityKind.VOICE_RECORDING to Tier.T1_LIVE),
        "recorder/miui_recorder_zh" to (ActivityKind.VOICE_RECORDING to Tier.T1_LIVE),
        "recorder/samsung_voicenote_ru" to (ActivityKind.VOICE_RECORDING to Tier.T1_LIVE),
        "progress/play_download_en" to (ActivityKind.PROGRESS to Tier.T2_ONGOING),
        "progress/chrome_download_hi" to (ActivityKind.PROGRESS to Tier.T2_ONGOING),
        "progress/uber_progressstyle_36_en" to (ActivityKind.TRACKING to Tier.T1_LIVE),
        // The Arabic ETA isn't read yet ("15 دقيقة"), so no chip: ongoing. A3.3 makes these live
        "progress/talabat_ongoing_ar" to (ActivityKind.TRACKING to Tier.T2_ONGOING),
        "progress/careem_ongoing_ar" to (ActivityKind.TRACKING to Tier.T2_ONGOING),
    )

    @Test
    fun fixtures_kindAndTier() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for ((name, kt) in fixtures) {
            IslandStateManager.resetForTest()
            val fixture = NotificationFixtures.load(context, name)
            val service = Robolectric.buildService(NotificationReceiverService::class.java).get()
            service.setSettingsForTest(IslandSettings(showNotifications = true))
            service.importanceForTest = { key -> if (key == fixture.key) fixture.importance else null }
            service.onNotificationPosted(fixture.sbn)
            shadowOf(Looper.getMainLooper()).idle()
            val info = IslandStateManager.activeNotification.value ?: IslandStateManager.activeProgress.value
            assertNotNull("$name posts a card", info)
            val s = LegacySpecMapper.fromNotification(info!!)
            assertEquals("$name kind", kt.first, s.kind)
            assertEquals("$name tier", kt.second, s.tier)
        }
    }
}
