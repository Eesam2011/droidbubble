package com.droidbubble.app.island.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.sqrt

/** V5.2: the accent is bright and clean enough to read on black, or there is none. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MediaAccentTest {

    private fun cover(vararg colors: Int): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val band = 64 / colors.size
        for (y in 0 until 64) for (x in 0 until 64) bmp.setPixel(x, y, colors[(x / band).coerceAtMost(colors.size - 1)])
        return bmp
    }

    private fun assertGoodOrNone(c: Color, what: String) {
        if (c == Color.Transparent) return
        val (l, a, b) = MediaAccent.toLab(c.toArgb())
        assertTrue("$what L* $l", l in 69.5..80.5)
        assertTrue("$what C* ${sqrt(a * a + b * b)}", sqrt(a * a + b * b) >= 19.5)
        assertTrue("$what contrast", (c.luminance() + 0.05f) / 0.05f >= 4.5f)
    }

    @Test
    fun fixtureCovers() {
        val fixtures = mapOf(
            "dark" to cover(0xFF1A0B2E.toInt(), 0xFF2B0F0F.toInt()),
            "pastel" to cover(0xFFF8C8DC.toInt(), 0xFFC8E6F8.toInt()),
            "saturated" to cover(0xFFFF0040.toInt(), 0xFF0050FF.toInt()),
            "two-tone" to cover(0xFF0A4D2C.toInt(), 0xFFF2B705.toInt()),
        )
        for ((name, bmp) in fixtures) assertGoodOrNone(MediaAccent.extract(bmp), name)
    }

    @Test
    fun greyscaleGivesNoAccent() {
        assertEquals(Color.Transparent, MediaAccent.extract(cover(0xFF101010.toInt(), 0xFF808080.toInt(), 0xFFEEEEEE.toInt())))
    }

    @Test
    fun theClampLiftsDarkAndCalmsBright() {
        for (argb in listOf(0xFF400010.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFF00.toInt(), 0xFF5B2C06.toInt())) {
            assertGoodOrNone(MediaAccent.clampAccent(argb), Integer.toHexString(argb))
        }
        // A strongly coloured dark red keeps an accent
        assertTrue(MediaAccent.clampAccent(0xFF8B0000.toInt()) != Color.Transparent)
    }
}
