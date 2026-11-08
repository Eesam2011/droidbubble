package com.droidbubble.app.island.harness

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandTestTags
import com.droidbubble.app.model.IslandSettings
import org.junit.Rule

open class IslandHarness(
    @get:Rule val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>
) {
    fun setContent(settings: MutableState<IslandSettings>) {
        rule.mainClock.autoAdvance = true
        rule.setContent { IslandOverlayRoot(settings = settings.value, onBoundsChange = { _, _ -> }) }
    }

    fun bounds(tag: String): DpRect? =
        if (exists(tag)) rule.onAllNodesWithTag(tag)[0].getBoundsInRoot() else null

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
                root.draw(android.graphics.Canvas(bitmap))
                bitmap.recycle()
            }
        }
    }

    fun onState(block: () -> Unit) {
        rule.runOnIdle(block)
        settle(60)
    }

    fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

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
