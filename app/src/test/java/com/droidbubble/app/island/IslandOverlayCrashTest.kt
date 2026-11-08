package com.droidbubble.app.island

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlin.random.Random

/**
 * Renders the real island overlay (IslandOverlayRoot) on the JVM and drives it through the
 * gestures and event sequences that used to crash it: swipes on the pill in every direction,
 * repeated swipes, swipes mid-animation, expand / collapse, call cards, flashlight blinking...
 * Any exception thrown during composition, layout or input fails the test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandOverlayCrashTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var settings by mutableStateOf(IslandSettings(showNotifications = true))

    private val art: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF884422.toInt()) }

    private val song = MediaInfo(
        packageName = "com.spotify.music",
        title = "Song",
        artist = "Artist",
        albumArt = art,
        isPlaying = true,
        durationMs = 200_000L,
        positionMs = 10_000L
    )

    private fun message(key: String = "msg", text: String = "Hello there") = NotificationInfo(
        key = key,
        packageName = "com.whatsapp",
        appName = "WhatsApp",
        title = "Sam",
        text = text,
        iconBitmap = art,
        actions = listOf(NotificationAction("Reply", null, remoteInputKey = "r"), NotificationAction("Mark as read", null))
    )

    private val incomingCall = NotificationInfo(
        key = "call",
        packageName = "com.google.android.dialer",
        appName = "Phone",
        title = "Essam medhat",
        text = "Incoming call",
        isCall = true,
        isIncomingCall = true,
        isOngoing = true
    )

    private val recorder = NotificationInfo(
        key = "rec",
        packageName = "com.google.android.apps.recorder",
        appName = "Recorder",
        title = "Recording",
        text = "00:12",
        isOngoing = true,
        liveTimerBaseMs = System.currentTimeMillis() - 12_000L,
        actions = listOf(NotificationAction("Pause", null), NotificationAction("Stop", null))
    )

    private val timer = NotificationInfo(
        key = "timer",
        packageName = "com.google.android.deskclock",
        appName = "Clock",
        title = "Timer",
        text = "Timer",
        isOngoing = true,
        liveTimerBaseMs = System.currentTimeMillis() + 31_000L,
        liveTimerCountDown = true,
        actions = listOf(NotificationAction("Pause", null), NotificationAction("Cancel", null))
    )

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        rule.mainClock.autoAdvance = true
        rule.setContent {
            IslandOverlayRoot(settings = settings)
        }
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
    }

    private fun settle(ms: Long = 1500L) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
        draw()
    }

    /** Runs a real draw pass (bitmaps, paths, blur effects, colors), not just layout. */
    private fun draw() {
        rule.runOnIdle {
            val root = rule.activity.window.decorView
            if (root.width > 0 && root.height > 0) {
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(android.graphics.Canvas(bitmap))
                bitmap.recycle()
            }
        }
    }

    private fun onState(block: () -> Unit) {
        rule.runOnIdle(block)
        settle(60)
    }

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** Horizontal swipe across the pill, starting from [startFraction] of its width (0 = left edge). */
    private fun swipePill(dx: Float, startFraction: Float = 0.5f, durationMs: Long = 120L) {
        if (!exists(IslandTestTags.PILL)) return
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].performTouchInput {
            val start = Offset(width * startFraction, height / 2f)
            swipe(start, start + Offset(dx, 0f), durationMs)
        }
    }

    private fun swipeUpOnCard() {
        if (!exists(IslandTestTags.CARD)) return
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            val start = Offset(width / 2f, 20f)
            swipe(start, start + Offset(0f, -200f), 150L)
        }
    }

    private fun swipeCard(dx: Float) {
        if (!exists(IslandTestTags.CARD)) return
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            swipe(center, center + Offset(dx, 0f), 150L)
        }
    }

    private fun tap(tag: String) {
        if (!exists(tag)) return
        rule.onAllNodesWithTag(tag)[0].performTouchInput { click() }
    }

    // --- Pill swipes (the reported crashes) -------------------------------------------------

    @Test
    fun swipeLeftTwice_onMediaPill_fromAlbumArt() {
        onState { IslandStateManager.updateMedia(song) }
        settle()
        swipePill(-300f, startFraction = 0.12f)
        settle(300)
        swipePill(-300f, startFraction = 0.12f)
        settle()
        assertTrue(IslandStateManager.userMinimized.value)
    }

    @Test
    fun swipeRightTwice_onMediaPill() {
        onState { IslandStateManager.updateMedia(song) }
        settle()
        swipePill(300f)
        settle(120) // second swipe lands while the pill is still shrinking away
        swipePill(300f)
        settle()
        assertTrue(IslandStateManager.userHidden.value)
    }

    @Test
    fun swipesInEveryDirection_whileAnimating_dualPill() {
        onState {
            IslandStateManager.updateMedia(song)
            IslandStateManager.updateNotification(message())
        }
        settle()
        repeat(4) {
            swipePill(-260f, startFraction = 0.1f, durationMs = 60)
            rule.mainClock.advanceTimeBy(16)
            swipePill(260f, startFraction = 0.1f, durationMs = 60)
            rule.mainClock.advanceTimeBy(48)
            swipePill(-260f, startFraction = 0.9f, durationMs = 60)
            rule.mainClock.advanceTimeBy(32)
        }
        settle()
        swipePill(300f)
        swipePill(300f)
        settle()
    }

    @Test
    fun minimizedPill_tapRestoresAndExpands() {
        onState { IslandStateManager.updateMedia(song) }
        settle()
        swipePill(-300f)
        settle()
        tap(IslandTestTags.PILL)
        settle()
        assertFalse(IslandStateManager.userMinimized.value)
        tap(IslandTestTags.PILL)
        settle()
        assertTrue(IslandStateManager.isExpanded.value)
        swipeUpOnCard()
        settle()
        assertFalse(IslandStateManager.isExpanded.value)
    }

    @Test
    fun expandedCards_swipeAwayAndCollapse() {
        onState {
            IslandStateManager.updateMedia(song)
            IslandStateManager.updateNotification(message())
        }
        settle()
        onState { IslandStateManager.expand(null) }
        settle(200) // mid-morph
        swipeCard(500f)
        settle(100)
        swipeUpOnCard()
        settle()
        onState { IslandStateManager.expand(null) }
        settle()
        swipeCard(-500f)
        settle()
    }

    // --- Calls, live activities and the flashlight -------------------------------------------

    @Test
    fun incomingCall_withBlinkingFlash_staysOnCallCard() {
        onState { IslandStateManager.updateNotification(incomingCall) }
        settle()
        assertTrue(IslandStateManager.isExpanded.value)
        repeat(12) { i ->
            onState { IslandStateManager.onTorchChanged(i % 2 == 0) }
            rule.mainClock.advanceTimeBy(150)
        }
        onState { IslandStateManager.triggerRingerModeEvent(IslandStateManager.RingerMode.VIBRATE) }
        settle()
        assertTrue(IslandStateManager.isExpanded.value)
        assertFalse(IslandStateManager.isFlashlightOn.value)
        assertTrue(IslandStateManager.activeNotification.value?.isIncomingCall == true)
    }

    @Test
    fun recorderTimerAndCallCards_renderAndSwipe() {
        for (card in listOf(recorder, timer, incomingCall.copy(isIncomingCall = false, liveTimerBaseMs = System.currentTimeMillis() - 5_000L))) {
            onState { IslandStateManager.updateNotification(card) }
            settle()
            onState { IslandStateManager.expand(IslandPriority.POP_NOTIFICATION) }
            settle()
            swipeCard(400f)
            settle()
            onState { IslandStateManager.removeNotification(card.key) }
            settle()
        }
    }

    @Test
    fun notchStyle_rendersAndSwipes() {
        onState { settings = settings.copy(islandStyle = IslandSettings.STYLE_NOTCH) }
        onState {
            IslandStateManager.updateMedia(song)
            IslandStateManager.updateNotification(recorder)
        }
        settle()
        swipePill(-300f)
        settle()
        swipePill(300f)
        settle()
        onState { IslandStateManager.restoreIsland(); IslandStateManager.expand(null) }
        settle()
        swipeUpOnCard()
        settle()
    }

    // --- Randomised event storm ------------------------------------------------------------

    @Test
    fun randomEventStorm_neverCrashes() {
        val rnd = Random(20261105)
        val actions: List<() -> Unit> = listOf(
            { onState { IslandStateManager.updateMedia(song.copy(isPlaying = rnd.nextBoolean())) } },
            { onState { IslandStateManager.updateMedia(null) } },
            { onState { IslandStateManager.updateNotification(message(text = "m${rnd.nextInt(5)}")) } },
            { onState { IslandStateManager.removeNotification("msg") } },
            { onState { IslandStateManager.updateNotification(incomingCall) } },
            { onState { IslandStateManager.removeNotification("call") } },
            { onState { IslandStateManager.updateNotification(recorder) } },
            { onState { IslandStateManager.updateNotification(timer) } },
            { onState { IslandStateManager.updateProgress(message("dl").copy(progress = rnd.nextInt(100), maxProgress = 100)) } },
            { onState { IslandStateManager.removeNotification("dl") } },
            { onState { IslandStateManager.onTorchChanged(rnd.nextBoolean()) } },
            { onState { IslandStateManager.updateFlashlightState(rnd.nextBoolean()) } },
            { onState { IslandStateManager.triggerChargingEvent(rnd.nextInt(101), 18.0) } },
            { onState { IslandStateManager.triggerUnlockEvent() } },
            { onState { IslandStateManager.triggerBluetoothEvent() } },
            { onState { IslandStateManager.triggerRingerModeEvent(IslandStateManager.RingerMode.SILENT) } },
            { onState { IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = rnd.nextBoolean(), durationSeconds = 9)) } },
            { onState { IslandStateManager.expand(null) } },
            { onState { IslandStateManager.collapse() } },
            { onState { IslandStateManager.setForegroundPackage(if (rnd.nextBoolean()) "com.whatsapp" else null) } },
            { onState { IslandStateManager.dismissCurrentNotification() } },
            { onState { IslandStateManager.dismissMedia() } },
            { swipePill(-300f, startFraction = rnd.nextFloat()) },
            { swipePill(300f, startFraction = rnd.nextFloat()) },
            { tap(IslandTestTags.PILL) },
            { tap(IslandTestTags.BUBBLE) },
            { swipeUpOnCard() },
            { swipeCard(if (rnd.nextBoolean()) 500f else -500f) },
            { onState { settings = settings.copy(islandStyle = if (rnd.nextBoolean()) IslandSettings.STYLE_NOTCH else IslandSettings.STYLE_PILL) } },
            { onState { settings = settings.copy(liquidGlassCards = rnd.nextBoolean()) } },
        )
        repeat(400) { step ->
            actions[rnd.nextInt(actions.size)]()
            rule.mainClock.advanceTimeBy(rnd.nextLong(0L, 700L))
            if (step % 3 == 0) draw()
        }
        settle(5000)
    }
}
