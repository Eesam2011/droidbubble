package com.droidbubble.app.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Owner review only (C3.10): the open cards and the pill at font scale 1.0, 1.3 and 2.0, side by
 * side. Runs only with `-Pcheckpoint=C310`; writes to the checkpoint output folder, C310/.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontScaleRender {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(IslandSettings(showNotifications = true))
    private val font = mutableFloatStateOf(1f)
    private val out by lazy { File(System.getProperty("island.checkpoint.out"), "C310").apply { mkdirs() } }
    private val scales = listOf(1f, 1.3f, 2f)

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("island.checkpoint") == "C310")
        IslandStateManager.resetForTest()
        h.freezeClock()
        h.setContent(settings, font, backdrop = Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF8A9BB0), androidx.compose.ui.graphics.Color(0xFF5C6B7D))))
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandStateManager.resetForTest()
    }

    private fun shot(scale: Float, expand: Boolean, show: () -> Unit): Bitmap {
        rule.runOnIdle {
            IslandStateManager.resetForTest()
            font.floatValue = scale
        }
        h.advanceFrozen(300)
        rule.runOnIdle { show(); if (expand) IslandStateManager.expand(null) }
        h.advanceFrozen(2500)
        return h.islandCrop(toScreenTop = true)
    }

    /** The three scales side by side, each labelled. */
    private fun row(name: String, expand: Boolean, show: () -> Unit) {
        val shots = scales.map { shot(it, expand, show) }
        val label = 72
        val gap = 24
        val w = shots.sumOf { it.width } + gap * (shots.size + 1)
        val hgt = shots.maxOf { it.height } + label + gap
        val sheet = Bitmap.createBitmap(w, hgt, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        c.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f; color = Color.BLACK }
        var x = gap
        shots.forEachIndexed { i, b ->
            c.drawText("font ${scales[i]}x", x.toFloat(), 52f, paint)
            c.drawBitmap(b, x.toFloat(), label.toFloat(), null)
            x += b.width + gap
        }
        File(out, "$name.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun timerCard() = row("timer_card", expand = true) { IslandStateManager.updateNotification(IslandFixtures.timer) }

    @Test
    fun messageCard() = row("message_card", expand = true) { IslandStateManager.updateNotification(IslandFixtures.message()) }

    @Test
    fun mediaCard() = row("media_card", expand = true) { IslandStateManager.updateMedia(IslandFixtures.song) }

    @Test
    fun timerPill() = row("timer_pill", expand = false) { IslandStateManager.updateNotification(IslandFixtures.timer) }

    @Test
    fun downloadAndMediaPill() = row("media_download_pill", expand = false) {
        IslandStateManager.updateMedia(IslandFixtures.song)
        IslandStateManager.updateProgress(IslandFixtures.progress(40))
    }
}
