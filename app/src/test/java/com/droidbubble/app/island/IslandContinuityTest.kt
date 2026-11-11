package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/**
 * M1.2: the continuity harness. Drives the real island frame by frame (60 Hz, frozen clock)
 * through plain and interrupted transitions and records its silhouette: the union of the pill and
 * card bounds, every frame. Two rules:
 * - **no jump**: the silhouette never moves further than [MAX_STEP_DP] in one frame
 * - **no narrowing on expand**: while the island opens it only grows (03 P0-1: today the pill
 *   shrinks before the card has grown, so the silhouette narrows, and collapse looks like
 *   "vanish and re-pop")
 *
 * Today (two bodies) only the interrupted expand, the reopen after an interrupted collapse and the
 * charging glance pass. The other cases are @Ignore'd until M1.3, with today's numbers in the
 * reason; run them by hand to see each frame.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandContinuityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        h.freezeClock()
        h.setContent(settings)
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    // ---- recorder ----

    /** The union of every pill and card node, or null when nothing is drawn. */
    private fun silhouette(): DpRect? {
        val rects = listOf(IslandTestTags.PILL, IslandTestTags.CARD).flatMap { tag ->
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().map { node ->
                with(rule.density) {
                    val b = node.boundsInRoot
                    DpRect(b.left.toDp(), b.top.toDp(), b.right.toDp(), b.bottom.toDp())
                }
            }
        }.filter { it.right > it.left }
        if (rects.isEmpty()) return null
        return DpRect(rects.minOf { it.left }, rects.minOf { it.top }, rects.maxOf { it.right }, rects.maxOf { it.bottom })
    }

    /** Advances [frames] frames of 16 ms; [before] runs on the main thread before frame [at]. */
    private fun record(frames: Int, at: Int = -1, before: () -> Unit = {}): List<DpRect?> = (0 until frames).map { i ->
        if (i == at) rule.runOnIdle(before)
        h.advanceFrozen(FRAME_MS)
        silhouette()
    }

    private fun DpRect.w(): Dp = right - left

    private fun List<DpRect?>.assertNoJumpOver(maxStep: Dp = MAX_STEP_DP) {
        zipWithNext().forEachIndexed { i, (a, b) ->
            if (a == null || b == null) return@forEachIndexed
            val step = maxOf(
                kotlin.math.abs((b.left - a.left).value), kotlin.math.abs((b.right - a.right).value),
                kotlin.math.abs((b.top - a.top).value), kotlin.math.abs((b.bottom - a.bottom).value),
            )
            assertTrue("frame $i → ${i + 1}: an edge moved $step dp (${a.str()} → ${b.str()})", step <= maxStep.value)
        }
    }

    private fun List<DpRect?>.assertMonotonicWidth(from: Int, to: Int) {
        val widths = subList(from, to).map { it?.w()?.value ?: 0f }
        val narrowest = widths.withIndex().drop(1).minByOrNull { (i, w) -> w - widths.subList(0, i).max() }
        println("silhouette widths: " + widths.joinToString { String.format(Locale.US, "%.0f", it) })
        widths.zipWithNext().forEachIndexed { i, (a, b) ->
            assertTrue("frame ${from + i + 1}: width fell $a → $b dp (narrowest ${narrowest?.value})", b >= a - 0.5f)
        }
    }

    private fun DpRect?.str() = this?.let { String.format(Locale.US, "%.1f,%.1f %.1fx%.1f", left.value, top.value, (right - left).value, (bottom - top).value) } ?: "-"

    private fun showMediaPill() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.advanceFrozen(1500)
    }

    // ---- no jump ----

    @Ignore("fails until M1.3: the pill and the card are two bodies, so the silhouette's top jumps 52 dp when the pill pops back at the camera while the card is still down")
    @Test
    fun expand_noJump() {
        showMediaPill()
        record(45, at = 0) { IslandStateManager.expand(null) }.assertNoJumpOver()
    }

    @Ignore("fails until M1.3: the pill and the card are two bodies, so the silhouette's top jumps 52 dp when the pill pops back at the camera while the card is still down")
    @Test
    fun collapse_noJump() {
        showMediaPill()
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.advanceFrozen(1500)
        record(45, at = 0) { IslandStateManager.collapse() }.assertNoJumpOver()
    }

    @Test
    fun expandInterruptedByCollapse_noJump() {
        for (at in listOf(5, 10, 15)) { // about 30 %, 60 %, 90 % of the expand at speed 0.7
            rule.runOnIdle { IslandStateManager.resetForTest() }
            h.advanceFrozen(500)
            showMediaPill()
            rule.runOnIdle { IslandStateManager.expand(null) }
            record(60, at = at) { IslandStateManager.collapse() }.assertNoJumpOver()
        }
    }

    @Ignore("fails until M1.3: the pill and the card are two bodies, so the silhouette's top jumps 52 dp when the pill pops back at the camera while the card is still down")
    @Test
    fun collapseInterruptedByExpand_noJump() {
        showMediaPill()
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.advanceFrozen(1500)
        rule.runOnIdle { IslandStateManager.collapse() }
        record(60, at = 8) { IslandStateManager.expand(null) }.assertNoJumpOver()
    }

    @Test
    fun chargingDuringExpand_noJump() {
        showMediaPill()
        rule.runOnIdle { IslandStateManager.expand(null) }
        record(60, at = 6) { IslandStateManager.triggerChargingEvent(80, 18.0) }.assertNoJumpOver()
    }

    @Ignore("fails until M1.3: the pill and the card are two bodies, so the silhouette's top jumps 52 dp when the pill pops back at the camera while the card is still down")
    @Test
    fun dualSplitDuringCollapse_noJump() {
        rule.runOnIdle {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.message())
            IslandStateManager.expand(null)
        }
        h.advanceFrozen(1500)
        record(60, at = 0) { IslandStateManager.collapse() }.assertNoJumpOver()
    }

    // ---- no narrowing ----

    @Ignore("fails until M1.3: the pill shrinks before the card grows (two-body morph); today the silhouette narrows 180 → 123 dp")
    @Test
    fun expand_neverNarrows() {
        showMediaPill()
        record(45, at = 0) { IslandStateManager.expand(null) }.assertMonotonicWidth(0, 45)
    }

    @Test
    fun collapseInterruptedByExpand_neverNarrowsOnceReopening() {
        showMediaPill()
        rule.runOnIdle { IslandStateManager.expand(null) }
        h.advanceFrozen(1500)
        rule.runOnIdle { IslandStateManager.collapse() }
        val frames = record(60, at = 8) { IslandStateManager.expand(null) }
        // From the frame the reopen turns the shape around, it only grows
        val turn = (9 until frames.size).first { (frames[it]?.w()?.value ?: 0f) > (frames[it - 1]?.w()?.value ?: 0f) }
        frames.assertMonotonicWidth(turn, frames.size)
    }

    private companion object {
        const val FRAME_MS = 16L
        /** At 60 Hz the fastest default spring moves an edge about 25 dp a frame; a jump is far more. */
        val MAX_STEP_DP = 40.dp
    }
}
