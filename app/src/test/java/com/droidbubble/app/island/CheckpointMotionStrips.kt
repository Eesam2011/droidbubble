package com.droidbubble.app.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * CP5 frame strips (D2.1–D2.4): the new motion, frame by frame, for the owner's review when no
 * phone is at hand for a screen recording. Each strip is one PNG: frames left to right, the
 * time under each. Test-only; runs with `-Pcheckpoint=CP5` (or `all`).
 *
 * ```
 * bash tools/gradle-locked.sh :app:testDebugUnitTest --tests "*CheckpointMotionStrips*" -Pcheckpoint=CP5
 * ```
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointMotionStrips {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)

    @Before
    fun setUp() {
        val filter = System.getProperty("island.checkpoint").orEmpty()
        assumeTrue("CP5 strips not requested", filter.equals("CP5", true) || filter.equals("all", true))
        IslandStateManager.resetForTest()
        h.setContent(mutableStateOf(IslandSettings(showNotifications = true)))
    }

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    /** The top of the window, centred: [heightDp] tall, 360 dp wide. */
    private fun frame(heightDp: Int): Bitmap = rule.runOnIdle {
        val view = rule.activity.window.decorView
        val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(full).apply { drawColor(AndroidColor.rgb(94, 102, 112)) })
        val d = rule.density.density
        val w = (360 * d).toInt().coerceAtMost(full.width)
        Bitmap.createBitmap(full, (full.width - w) / 2, 0, w, (heightDp * d).toInt().coerceAtMost(full.height))
    }

    /** Runs [act], then grabs a frame at each of [timesMs] (ms after [act]). */
    private fun strip(name: String, timesMs: List<Long>, heightDp: Int = 70, act: () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.runOnIdle(act)
        var now = 0L
        val frames = timesMs.map { t ->
            rule.mainClock.advanceTimeBy(t - now)
            now = t
            t to frame(heightDp)
        }
        rule.mainClock.autoAdvance = true
        save(name, frames)
    }

    private fun save(name: String, frames: List<Pair<Long, Bitmap>>) {
        val label = 44
        val w = frames.first().second.width
        val hgt = frames.first().second.height
        val cols = minOf(frames.size, 6)
        val rows = (frames.size + cols - 1) / cols
        val sheet = Bitmap.createBitmap(cols * w, rows * (hgt + label), Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet).apply { drawColor(AndroidColor.rgb(40, 40, 44)) }
        val paint = Paint().apply { color = AndroidColor.WHITE; textSize = 30f; isAntiAlias = true }
        frames.forEachIndexed { i, (t, bmp) ->
            val x = (i % cols) * w.toFloat()
            val y = (i / cols) * (hgt + label).toFloat()
            c.drawBitmap(bmp, x, y, null)
            c.drawText("$t ms", x + 16f, y + hgt + 32f, paint)
        }
        val out = File(System.getProperty("island.checkpoint.out") ?: "build/outputs/roborazzi/checkpoints", "CP5/$name.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val every33 = (0L..495L step 33L).toList()

    /** D2.1: a download with unknown progress, then a song takes the pill (same width): the breath. */
    @Test
    fun alertBump() {
        rule.runOnIdle { IslandStateManager.updateProgress(IslandFixtures.progress(0).copy(isIndeterminate = true)) }
        h.settle(2_000)
        strip("d2.1-alert-bump", every33) { IslandStateManager.updateMedia(IslandFixtures.song) }
    }

    /** D2.3: the same chat posts again: the 3 % pulse with the icon's bounce. */
    @Test
    fun sameAppPulse() {
        rule.runOnIdle { IslandStateManager.updateNotification(IslandFixtures.message(text = "Hi")) }
        h.settle(2_000)
        strip("d2.3-same-app-pulse", every33) { IslandStateManager.updateNotification(IslandFixtures.message(text = "Are you there?")) }
    }

    /** D2.2: a new message peeks open into its card and recedes on its own. */
    @Test
    fun bannerPeek() {
        IslandStateManager.peekBanners = true
        val times = listOf(0L, 100, 200, 350, 600, 1_000, 2_000, 2_500, 2_650, 2_800, 3_000, 3_400)
        strip("d2.2-banner-peek", times, heightDp = 230) { IslandStateManager.updateNotification(IslandFixtures.message()) }
    }

    /** D2.4: a message arrives during a song: the bubble splits off (the neck snap ticks here). */
    @Test
    fun split() {
        rule.runOnIdle { IslandStateManager.updateMedia(IslandFixtures.song) }
        h.settle(2_000)
        strip("d2.4-split", every33) { IslandStateManager.updateNotification(IslandFixtures.message()) }
    }
}
