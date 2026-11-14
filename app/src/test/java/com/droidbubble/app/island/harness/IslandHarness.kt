package com.droidbubble.app.island.harness

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.droidbubble.app.island.IslandClock
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandTestTags
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Renders the real island overlay ([IslandOverlayRoot]) under Robolectric and drives it: settle,
 * draw, state changes, gestures and node bounds. Tests own the compose rule and pass it in:
 *
 * ```
 * @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
 * private val h = IslandHarness(rule)
 * ```
 *
 * Use with `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")` and
 * `@GraphicsMode(NATIVE)`, and call `IslandStateManager.resetForTest()` before and after.
 */
class IslandHarness(
    val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
) {

    /**
     * Renders the island. With [fontScale], the island sees that font scale, the way the service
     * passes the phone's font size through `LocalDensity` (`IslandOverlayService`).
     */
    fun setContent(
        settings: MutableState<IslandSettings>,
        fontScale: MutableFloatState? = null,
        backdrop: Brush? = null,
    ) {
        rule.setContent {
            if (backdrop == null) {
                Island(settings, fontScale)
            } else {
                // The wallpaper behind the island (VD0.1 checkpoints): keylines and scrims are judged on it
                Box(Modifier.fillMaxSize().background(backdrop)) { Island(settings, fontScale) }
            }
        }
    }

    @Composable
    private fun Island(settings: MutableState<IslandSettings>, fontScale: MutableFloatState?) {
        if (fontScale == null) {
            IslandOverlayRoot(settings = settings.value)
        } else {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.floatValue)) {
                IslandOverlayRoot(settings = settings.value)
            }
        }
    }

    /**
     * Freezes the island's wall clock at [atMs] and stops the compose clock from advancing on its
     * own, so ambient loops (equalizer, recording dot, timers) only move when the test advances
     * time. Undo with [unfreezeClock] in `@After`.
     */
    fun freezeClock(atMs: Long = FROZEN_WALL_MS) {
        IslandClock.wallMs = { atMs }
        rule.mainClock.autoAdvance = false
    }

    fun unfreezeClock() {
        IslandClock.wallMs = System::currentTimeMillis
    }

    /** Advances a frozen compose clock by [ms] and lets composition, layout and drawing catch up. */
    fun advanceFrozen(ms: Long) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
    }

    fun settle(ms: Long = 1500L) {
        rule.mainClock.advanceTimeBy(ms)
        rule.waitForIdle()
        draw()
    }

    /** Runs a real draw pass (bitmaps, paths, blur effects, colors), not just layout. */
    fun draw() {
        rule.runOnIdle {
            val root = rule.activity.window.decorView
            if (root.width > 0 && root.height > 0) {
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                bitmap.recycle()
            }
        }
    }

    /**
     * Draws the window and crops it to the island's union bounds (pill, bubble, cards and their
     * content roots) plus 8 dp, so window size changes don't create noise in screenshots.
     */
    fun islandCrop(): Bitmap {
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
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        if (rects.isEmpty()) return root
        // boundsInRoot is in px; the margin is 8 dp
        val margin = 8f * rule.density.density
        val left = floor(rects.minOf { it.left } - margin).toInt().coerceIn(0, root.width - 1)
        val top = floor(rects.minOf { it.top } - margin).toInt().coerceIn(0, root.height - 1)
        val right = ceil(rects.maxOf { it.right } + margin).toInt().coerceIn(left + 1, root.width)
        val bottom = ceil(rects.maxOf { it.bottom } + margin).toInt().coerceIn(top + 1, root.height)
        return Bitmap.createBitmap(root, left, top, right - left, bottom - top)
    }

    fun onState(block: () -> Unit) {
        rule.runOnIdle(block)
        settle(60)
    }

    fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** Bounds of the first node with [tag], relative to the compose root; null if it isn't there. */
    fun bounds(tag: String): DpRect? =
        if (exists(tag)) rule.onAllNodesWithTag(tag)[0].getBoundsInRoot() else null

    /**
     * Which activity the content slot [slot] (one of [IslandTestTags.CONTENT_SLOTS]) is drawing, or
     * null if the slot shows nothing. Reads the unmerged tree, because the clickable pill and card
     * merge their children's tags away. Ask at settle: mid-transition a slot can hold two activities.
     */
    fun contentOf(slot: String): IslandPriority? {
        require(slot in IslandTestTags.CONTENT_SLOTS) { "Unknown slot $slot" }
        val prefix = "island_${slot}_"
        val found = rule.onAllNodes(
            SemanticsMatcher("content tag of $slot") { node ->
                node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
            },
            useUnmergedTree = true
        ).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag].removePrefix(prefix) }
            .distinct()
        check(found.size <= 1) { "Slot $slot shows several activities: $found" }
        val name = found.firstOrNull() ?: return null
        return if (name == "NONE") null else IslandPriority.valueOf(name)
    }

    /** Horizontal swipe across the pill, starting from [startFraction] of its width (0 = left edge). */
    fun swipePill(dx: Float, startFraction: Float = 0.5f, durationMs: Long = 120L) {
        if (!exists(IslandTestTags.PILL)) return
        rule.onAllNodesWithTag(IslandTestTags.PILL)[0].performTouchInput {
            val start = Offset(width * startFraction, height / 2f)
            swipe(start, start + Offset(dx, 0f), durationMs)
        }
    }

    fun swipeUpOnCard() {
        if (!exists(IslandTestTags.CARD)) return
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            val start = Offset(width / 2f, 20f)
            swipe(start, start + Offset(0f, -200f), 150L)
        }
    }

    fun swipeCard(dx: Float) {
        if (!exists(IslandTestTags.CARD)) return
        rule.onAllNodesWithTag(IslandTestTags.CARD)[0].performTouchInput {
            swipe(center, center + Offset(dx, 0f), 150L)
        }
    }

    fun tap(tag: String) {
        if (!exists(tag)) return
        rule.onAllNodesWithTag(tag)[0].performTouchInput { click() }
    }

    fun longPress(tag: String) {
        if (!exists(tag)) return
        rule.onAllNodesWithTag(tag)[0].performTouchInput { longClick() }
    }

    companion object {
        /** The frozen wall clock screenshot tests use: 2027-01-15 08:00 UTC. */
        const val FROZEN_WALL_MS = 1_800_000_000_000L
    }
}
