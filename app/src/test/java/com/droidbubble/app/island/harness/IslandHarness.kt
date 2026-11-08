package com.droidbubble.app.island.harness

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandTestTags
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings

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

    fun setContent(settings: MutableState<IslandSettings>) {
        rule.setContent { IslandOverlayRoot(settings = settings.value) }
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
}
