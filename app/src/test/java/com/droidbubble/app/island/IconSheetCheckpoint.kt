package com.droidbubble.app.island

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * CP4's icon sheet (V7.2): every catalogue glyph at the pill (18), bubble (16), header (24) and
 * control (0.46 × 55) sizes, and each optically shifted glyph in a circle before and after its
 * shift, with crosshairs on the circle's centre. Renders only with -Pcheckpoint=CP4 or all.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w600dp-h1000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconSheetCheckpoint {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun render() {
        val filter = System.getProperty("island.checkpoint").orEmpty()
        assumeTrue("icon sheet not requested", filter.equals("CP4", true) || filter.equals("all", true))
        val glyphs = IslandIcons::class.java.declaredMethods
            .filter { it.returnType == ImageVector::class.java && it.parameterCount == 0 }
            .sortedBy { it.name }
            .map { it.name.removePrefix("get") to it.invoke(IslandIcons) as ImageVector }
        val sizes = listOf(
            IslandTokens.Glyph.Compact,
            IslandTokens.Glyph.Bubble,
            40.dp * IslandTokens.Glyph.HeaderRatio,
            55.dp * IslandTokens.Glyph.ControlRatio,
        )
        rule.setContent {
            Column(Modifier.fillMaxSize().background(Color(0xFF1C1C1E)).padding(12.dp)) {
                Text("Pill 18 · bubble 16 · header 24 · control 25 dp", color = Color.White, fontSize = 13.sp)
                val half = (glyphs.size + 1) / 2
                Row {
                    listOf(glyphs.take(half), glyphs.drop(half)).forEach { column ->
                        Column(Modifier.width(288.dp)) {
                            column.forEach { (name, v) ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, color = Color(0xFF8E8E93), fontSize = 11.sp, modifier = Modifier.width(96.dp))
                                    sizes.forEach { s ->
                                        Box(Modifier.size(36.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                                            Icon(v, null, tint = Color.White, modifier = Modifier.size(s))
                                        }
                                        Box(Modifier.width(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Optical centre: before | after (crosshairs on the circle's centre)", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                glyphs.map { it.second }.distinctBy { it.name }.filter { IslandIcons.opticalShiftOf(it) != null }.chunked(3).forEach { row ->
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { v ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CenterCrop(v, shifted = false)
                                CenterCrop(v, shifted = true)
                            }
                            Text(v.name.removePrefix("Rounded."), color = Color(0xFF8E8E93), fontSize = 10.sp)
                        }
                    }
                }
                }
            }
        }
        rule.waitForIdle()
        val image = rule.runOnIdle {
            val view = rule.activity.window.decorView
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(android.graphics.Canvas(it)) }
        }
        val out = File(System.getProperty("island.checkpoint.out") ?: "build/outputs/roborazzi/checkpoints", filter)
        out.mkdirs()
        File(out, "icon_sheet.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @androidx.compose.runtime.Composable
    private fun CenterCrop(v: ImageVector, shifted: Boolean, glyph: Dp = 32.dp) {
        Box(
            Modifier
                .size(56.dp)
                .background(Color(0xFF3A3A3C), CircleShape)
                .border(1.dp, Color(0xFF636366), CircleShape)
                .drawBehind {
                    val c = Color(0x99FF453A)
                    drawLine(c, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 1f)
                    drawLine(c, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1f)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(v, null, tint = Color.White, modifier = Modifier.size(glyph).let { if (shifted) it.opticalCenter(v) else it })
        }
    }
}
