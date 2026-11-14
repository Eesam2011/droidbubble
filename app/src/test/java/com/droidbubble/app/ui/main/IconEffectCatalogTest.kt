package com.droidbubble.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.model.IslandSettings
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** I4.1: the catalogue composes in every tier, with the tier override and without. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class IconEffectCatalogTest {

    @get:Rule
    val rule = createComposeRule()

    private var override by mutableStateOf<MotionTier?>(null)

    @Test
    fun everyTier_rendersEveryRow() {
        rule.mainClock.autoAdvance = false
        rule.setContent { IconEffectCatalog(IslandSettings(), override, onTierOverride = { override = it }) }
        for (t in listOf(null) + MotionTier.entries) {
            override = t
            repeat(20) { rule.mainClock.advanceTimeByFrame() }
            rule.waitForIdle()
            IconEffectCatalogNames.indices.forEach { rule.onNodeWithTag("icon_catalog_zoom_$it").assertExists() }
        }
    }
}

/**
 * I4.1 tuning clips: renders each effect's 2× tile frame by frame (30 fps, 1.0× speed, Full tier)
 * into `<dir>/<index>/<frame>.png`; tools/icon-clips.py turns them into animated clips.
 * Skipped unless `-PiconClips=<dir>` is given.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconEffectClips {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var trigger by mutableIntStateOf(0)
    private var on by mutableStateOf(false)
    private var level by mutableFloatStateOf(0f)

    @Test
    fun renderClips() {
        val dir = System.getProperty("island.iconClips")
        assumeTrue("pass -PiconClips=<dir>", !dir.isNullOrBlank())
        val names = IconEffectCatalogNames
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalIslandMotion provides IslandMotion.from(IslandSettings(animationSpeed = 1f), MotionTier.FULL)) {
                // Every tile at once (3 a row fits the test window), all driven together
                Column { names.indices.chunked(3).forEach { row -> Row { row.forEach { IconEffectCatalogTile(it, trigger, on, level) } } } }
            }
        }
        File(dir!!, "names.txt").apply { parentFile.mkdirs() }.writeText(names.joinToString(separator = System.lineSeparator()))
        repeat(30) { rule.mainClock.advanceTimeByFrame() }
        val outs = names.indices.map { File(dir, "$it").apply { mkdirs() } }
        // 3 s: at 0.25 s fire / switch on / level up; at 1.6 s fire again / switch off; on again at 2 s
        for (f in 0 until 90) {
            when (f) {
                8 -> { trigger++; on = true; level = 0.34f }
                20 -> level = 0.67f
                32 -> level = 1f
                48 -> { trigger++; on = false; level = 0.3f }
                60 -> on = true
            }
            // 33 ms a frame: two 60 Hz frames
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            // Draw the window ourselves: captureToImage waits for a frame the paused clock never gives
            val full = rule.runOnIdle {
                val view = rule.activity.window.decorView
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
            }
            for (i in names.indices) {
                val r = rule.onNodeWithTag("icon_catalog_zoom_$i").fetchSemanticsNode().boundsInRoot
                val crop = Bitmap.createBitmap(full, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
                File(outs[i], "%03d.png".format(f)).outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
