package com.droidbubble.app.island.activity

import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A1.3: the store itself, and the shadow writes from IslandStateManager. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityStoreTest {

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        Dispatchers.resetMain()
    }

    private fun spec(id: String, signature: Int = 0, started: Long = 0L, staleAt: Long? = null, expiresAt: Long? = null) = LiveActivitySpec(
        id = ActivityId(id), kind = ActivityKind.MESSAGE, source = ActivitySource.TEST, packageName = "p", tier = Tier.T3_BANNER,
        bucket = OrderBucket.NOTIFICATION, startedAt = started, lastSignificantAt = started, staleAt = staleAt, expiresAt = expiresAt,
        accent = AccentRole.NEUTRAL, compactLeading = Slot.Empty, compactTrailing = Slot.Empty, minimal = Slot.Empty,
        expanded = ExpandedSpec(ExpandedPresentation.NONE), open = OpenTarget.None, stateSignature = signature,
        payload = ActivityPayload.System(GlanceInfo(id)),
    )

    // ---- the store ----

    @Test
    fun upsert_keepsStartedAt() {
        val store = ActivityStore()
        store.upsert(spec("a", started = 10L))
        store.upsert(spec("a", started = 99L))
        assertEquals(10L, store.get(ActivityId("a"))?.startedAt)
    }

    @Test
    fun dismissal_versusSignature() {
        val store = ActivityStore()
        store.upsert(spec("a", signature = 1))
        store.dismiss(ActivityId("a"))
        assertNull(store.get(ActivityId("a")))
        assertFalse("an identical repost stays dismissed", store.upsert(spec("a", signature = 1)))
        assertNull(store.get(ActivityId("a")))
        assertTrue("a changed signature brings it back", store.upsert(spec("a", signature = 2)))
        assertEquals(2, store.get(ActivityId("a"))?.stateSignature)
        assertFalse(store.isDismissed(ActivityId("a")))
    }

    @Test
    fun remove_clearsTheDismissal() {
        val store = ActivityStore()
        store.upsert(spec("a", signature = 1))
        store.dismiss(ActivityId("a"))
        store.remove(ActivityId("a"))
        assertTrue(store.upsert(spec("a", signature = 1)))
    }

    @Test
    fun expire() {
        val store = ActivityStore()
        store.upsert(spec("glance", expiresAt = 100L))
        store.upsert(spec("paused", staleAt = 200L))
        store.upsert(spec("forever"))
        store.expire(100L)
        assertEquals(listOf("paused", "forever"), store.activities.value.keys.map { it.value })
        store.expire(250L)
        assertEquals(listOf("forever"), store.activities.value.keys.map { it.value })
    }

    @Test
    fun insertionOrder_isStable() {
        val store = ActivityStore()
        listOf("c", "a", "b").forEach { store.upsert(spec(it)) }
        store.upsert(spec("a", signature = 5)) // an update keeps its place
        assertEquals(listOf("c", "a", "b"), store.activities.value.keys.map { it.value })
    }

    @Test
    fun threadCheck_runsOnEveryWrite() {
        var checks = 0
        val store = ActivityStore(threadCheck = { checks++ })
        store.upsert(spec("a"))
        store.dismiss(ActivityId("a"))
        store.remove(ActivityId("a"))
        store.expire(0L)
        assertEquals(4, checks)
    }

    // ---- shadow writes ----

    private val ongoingCall = NotificationInfo(
        key = "call", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Ongoing call",
        isCall = true, isOngoing = true, liveTimerBaseMs = 1L,
    )
    private val timer = NotificationInfo(
        key = "timer", packageName = "com.google.android.deskclock", appName = "Clock", title = "Timer", text = "",
        isOngoing = true, liveTimerBaseMs = 10_000L, liveTimerCountDown = true,
    )
    private fun message(text: String = "Hi") = NotificationInfo(
        key = "msg", packageName = "org.telegram.messenger", appName = "Telegram", title = "Ana", text = text,
    )

    private val ids get() = IslandStateManager.store.activities.value.keys.map { it.value }

    @Test
    fun shadow_holdsEveryActivity_whileTheSlotShowsOne() {
        IslandStateManager.updateNotification(ongoingCall)
        IslandStateManager.updateNotification(timer)
        IslandStateManager.updateNotification(message())
        assertEquals(listOf("n:call", "n:timer", "n:msg"), ids)
        assertEquals("msg", IslandStateManager.activeNotification.value?.key)
    }

    @Test
    fun shadow_removalRemovesTheEntry() {
        IslandStateManager.updateNotification(timer)
        IslandStateManager.updateNotification(message())
        IslandStateManager.removeNotification("msg")
        assertEquals(listOf("n:timer"), ids)
    }

    @Test
    fun shadow_swipeSurvivesIdenticalRepost_liftedByChange() {
        IslandStateManager.updateNotification(message())
        IslandStateManager.dismissCurrentNotification()
        assertEquals(emptyList<String>(), ids)
        IslandStateManager.updateNotification(message())
        assertEquals(emptyList<String>(), ids)
        IslandStateManager.updateNotification(message("Are you there?"))
        assertEquals(listOf("n:msg"), ids)
    }

    @Test
    fun shadow_otherSources() {
        IslandStateManager.updateFlashlightState(true, level = 2)
        IslandStateManager.updateRecordingState(com.droidbubble.app.model.ScreenRecordingState(isRecording = true))
        IslandStateManager.updateMedia(MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "A", isPlaying = true))
        val progress = NotificationInfo(key = "dl", packageName = "com.android.vending", appName = "Play", title = "Downloading", text = "", progress = 5, maxProgress = 100)
        IslandStateManager.updateNotification(progress) // routed to the progress slot
        assertEquals(listOf("torch", "rec:screen", "media:com.spotify.music", "n:dl"), ids)
        assertEquals(ActivityKind.PROGRESS, IslandStateManager.store.get(ActivityId("n:dl"))?.kind)

        IslandStateManager.updateFlashlightState(false)
        IslandStateManager.updateRecordingState(com.droidbubble.app.model.ScreenRecordingState())
        IslandStateManager.updateMedia(null)
        IslandStateManager.updateProgress(null)
        assertEquals(emptyList<String>(), ids)
    }

    @Test
    fun shadow_swipedPausedMedia_returnsOnlyWhenItPlays() {
        val paused = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "A", isPlaying = false)
        IslandStateManager.updateMedia(paused)
        IslandStateManager.dismissMedia()
        IslandStateManager.updateMedia(paused)
        assertEquals(emptyList<String>(), ids)
        IslandStateManager.updateMedia(paused.copy(isPlaying = true))
        assertEquals(listOf("media:com.spotify.music"), ids)
    }

    @Test
    fun shadow_listenerDisconnect_clearsNotificationsOnly() {
        IslandStateManager.updateNotification(timer)
        IslandStateManager.updateFlashlightState(true)
        IslandStateManager.updateNotification(null)
        assertEquals(listOf("torch"), ids)
    }
}
