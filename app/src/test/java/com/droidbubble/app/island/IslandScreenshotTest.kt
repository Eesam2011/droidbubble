package com.droidbubble.app.island

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NotificationAction
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The screenshot baseline (S2.2): the 16 island states every polish task is checked against.
 * Each image is cropped to the union of the pill, bubble and card bounds plus 8 dp, so window
 * size changes don't create noise. Names are `<state>__<variant>.png`.
 *
 * `./gradlew :app:recordRoborazziDebug` records; `./gradlew :app:verifyRoborazziDebug` compares;
 * `./gradlew :app:compareRoborazziDebug` writes the diff images a PR should attach.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(BASE)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        h.freezeClock()
        h.setContent(settings)
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandTokens.highContrast = false
        IslandStateManager.resetForTest()
    }

    // ---- compact ----

    @Test fun idlePill() = shot("idle_pill", BASE.copy(alwaysShowPill = true)) {}

    @Test fun mediaPill() = shot("media_pill") { IslandStateManager.updateMedia(IslandFixtures.song) }

    @Test fun messagePill() = shot("message_pill") { IslandStateManager.updateNotification(IslandFixtures.message()) }

    @Test fun timerPill() = shot("timer_pill") { IslandStateManager.updateNotification(IslandFixtures.timer) }

    @Test fun recorderPill() = shot("recorder_pill") { IslandStateManager.updateNotification(IslandFixtures.recorder) }

    @Test fun downloadPill() = shot("download_pill") { IslandStateManager.updateProgress(IslandFixtures.progress(40)) }

    @Test fun dualPill() = shot("dual_pill") {
        IslandStateManager.updateMedia(IslandFixtures.song)
        IslandStateManager.updateNotification(IslandFixtures.message())
    }

    @Test fun minimizedRing() = shot("minimized_ring") {
        IslandStateManager.updateMedia(IslandFixtures.song)
        IslandStateManager.minimizeIsland()
    }

    @Test fun notchMedia() = shot("notch_media", BASE.copy(islandStyle = IslandSettings.STYLE_NOTCH)) {
        IslandStateManager.updateMedia(IslandFixtures.song)
    }

    @Test fun chargingGlance() = shot("charging_glance") { IslandStateManager.triggerChargingEvent(80, 18.0) }

    // ---- expanded ----

    @Test fun expandedMedia() = shot("expanded_media") {
        IslandStateManager.updateMedia(IslandFixtures.song)
        IslandStateManager.expand(null)
    }

    @Test fun expandedMessageReplyOpen() = shot("expanded_message_reply", after = ::openReply) {
        IslandStateManager.updateNotification(IslandFixtures.message())
        IslandStateManager.expand(null)
    }

    @Test fun expandedMessageReplyWithAppChoices() = shot("expanded_message_reply_choices", after = ::openReply) {
        IslandStateManager.updateNotification(
            IslandFixtures.message().copy(
                actions = listOf(
                    NotificationAction("Reply", null, remoteInputKey = "r", remoteInputChoices = listOf("On my way")),
                    NotificationAction("Mark as read", null)
                )
            )
        )
        IslandStateManager.expand(null)
    }

    @Test fun expandedIncomingCall() = shot("expanded_incoming_call") {
        IslandStateManager.updateNotification(IslandFixtures.incomingCall)
    }

    @Test fun expandedTimer() = shot("expanded_timer") {
        IslandStateManager.updateNotification(IslandFixtures.timer)
        IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
    }

    @Test fun expandedDual() = shot("expanded_dual") {
        IslandStateManager.updateMedia(IslandFixtures.song)
        IslandStateManager.updateNotification(IslandFixtures.message())
        IslandStateManager.expand(null)
    }

    @Test fun expandedMessageHighContrast() = shot("expanded_message", BASE.copy(showOutline = true), variant = "hc", highContrast = true) {
        IslandStateManager.updateNotification(IslandFixtures.message())
        IslandStateManager.expand(null)
    }

    // ---- helpers ----

    private fun openReply() {
        val reply = rule.onAllNodesWithText("Reply")
        assertNotNull("no Reply button on the message card", reply.fetchSemanticsNodes().firstOrNull())
        reply[0].performClick()
        h.advanceFrozen(1000)
    }

    private fun shot(
        state: String,
        s: IslandSettings = BASE,
        variant: String = "default",
        highContrast: Boolean = false,
        after: () -> Unit = {},
        setUp: () -> Unit,
    ) {
        rule.runOnIdle {
            IslandTokens.highContrast = highContrast
            settings.value = s
        }
        h.advanceFrozen(100)
        rule.runOnIdle(setUp)
        h.advanceFrozen(1500)
        after()
        capture("src/test/screenshots/island/${state}__$variant.png")
    }

    /** Crops the root to the island's union bounds plus 8 dp and saves it through Roborazzi. */
    private fun capture(path: String) {
        // Every node of each tag: dual mode has two cards (and a pill plus a bubble)
        // plus the content roots: card 2 overflows card 1's node and only carries its content tag
        val contentRoots = rule.onAllNodes(
            SemanticsMatcher("island content root") { node ->
                node.config.getOrNull(SemanticsProperties.TestTag)?.let { tag ->
                    IslandTestTags.CONTENT_SLOTS.any { tag.startsWith("island_${it}_") }
                } == true
            },
            useUnmergedTree = true
        ).fetchSemanticsNodes().map { it.boundsInRoot }
        val rects = listOf(IslandTestTags.PILL, IslandTestTags.BUBBLE, IslandTestTags.CARD)
            .flatMap { tag -> rule.onAllNodesWithTag(tag).fetchSemanticsNodes().map { it.boundsInRoot } } + contentRoots
        // Draw the window ourselves: captureToImage waits for a new frame, which never comes
        // while the clock is frozen
        val root = rule.runOnIdle {
            val view = rule.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(android.graphics.Canvas(it)) }
        }
        val crop: Bitmap = if (rects.isEmpty()) root else {
            // boundsInRoot is in px; the margin is 8 dp
            val margin = 8f * rule.density.density
            val left = floor(rects.minOf { it.left } - margin).toInt().coerceIn(0, root.width - 1)
            val top = floor(rects.minOf { it.top } - margin).toInt().coerceIn(0, root.height - 1)
            val right = ceil(rects.maxOf { it.right } + margin).toInt().coerceIn(left + 1, root.width)
            val bottom = ceil(rects.maxOf { it.bottom } + margin).toInt().coerceIn(top + 1, root.height)
            Bitmap.createBitmap(root, left, top, right - left, bottom - top)
        }
        crop.captureRoboImage(path)
    }

    private companion object {
        val BASE = IslandSettings(showNotifications = true)
    }
}
