package com.droidbubble.app.island.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.sqrt

/**
 * V5.2: the song's accent colour, from Palette instead of a plain RGB average, then clamped to
 * read on black: lightness L* 70–80, and greyish covers (chroma below 20) give no accent at all
 * (Transparent, which callers already treat as "no accent").
 */
object MediaAccent {
    const val MIN_L = 70.0
    const val MAX_L = 80.0
    const val MIN_CHROMA = 20.0

    private val cache = java.util.WeakHashMap<Bitmap, Color>()

    fun extract(bitmap: Bitmap?): Color {
        if (bitmap == null || bitmap.isRecycled) return Color.Transparent
        synchronized(cache) { cache[bitmap]?.let { return it } }
        val color = try {
            val palette = Palette.from(bitmap).maximumColorCount(16).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch
            swatch?.let { clampAccent(it.rgb) } ?: Color.Transparent
        } catch (_: Exception) {
            Color.Transparent
        }
        synchronized(cache) { cache[bitmap] = color }
        return color
    }

    /** The clamp on an sRGB colour: pure, so it is unit-tested. */
    fun clampAccent(argb: Int): Color {
        val lab = toLab(argb)
        if (chroma(lab) < MIN_CHROMA) return Color.Transparent
        val l = lab[0].coerceIn(MIN_L, MAX_L)
        // Out of gamut after the lightness change: pull a and b toward grey until it fits
        var k = 1.0
        while (k > 0.0) {
            val target = doubleArrayOf(l, lab[1] * k, lab[2] * k)
            // Pulled too far toward grey: no accent rather than a muddy one
            if (chroma(target) < MIN_CHROMA) return Color.Transparent
            val rgb = ColorUtils.LABToColor(target[0], target[1], target[2])
            // In gamut when the clamped sRGB colour converts back to (nearly) the same LAB
            if (ColorUtils.distanceEuclidean(toLab(rgb), target) < 1.0) return Color(rgb)
            k -= 0.02
        }
        return Color.Transparent
    }

    fun toLab(argb: Int): DoubleArray = DoubleArray(3).also { ColorUtils.colorToLAB(argb, it) }

    private fun chroma(lab: DoubleArray) = sqrt(lab[1] * lab[1] + lab[2] * lab[2])
}
