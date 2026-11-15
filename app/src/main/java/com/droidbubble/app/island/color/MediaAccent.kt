package com.droidbubble.app.island.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlin.math.cbrt
import kotlin.math.pow
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
        val (l0, a0, b0) = toLab(argb)
        if (sqrt(a0 * a0 + b0 * b0) < MIN_CHROMA) return Color.Transparent
        val l = l0.coerceIn(MIN_L, MAX_L)
        // Out of gamut after the lightness change: pull a and b toward grey until it fits
        var k = 1.0
        while (k > 0.0) {
            val rgb = fromLab(l, a0 * k, b0 * k)
            if (rgb != null) {
                // Pulled too far toward grey: no accent rather than a muddy one
                if (sqrt((a0 * k).pow(2) + (b0 * k).pow(2)) < MIN_CHROMA) return Color.Transparent
                return Color(rgb[0].toFloat(), rgb[1].toFloat(), rgb[2].toFloat())
            }
            k -= 0.02
        }
        return Color.Transparent
    }

    // ---- sRGB <-> CIELAB (D65) ----
    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883

    private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun gam(c: Double) = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1 / 2.4) - 0.055

    fun toLab(argb: Int): Triple<Double, Double, Double> {
        val r = lin(((argb shr 16) and 0xff) / 255.0)
        val g = lin(((argb shr 8) and 0xff) / 255.0)
        val b = lin((argb and 0xff) / 255.0)
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / XN
        val y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / YN
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / ZN
        fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
        val fx = f(x); val fy = f(y); val fz = f(z)
        return Triple(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    /** sRGB 0..1 for a LAB colour, or null when it is outside the sRGB gamut. */
    fun fromLab(l: Double, a: Double, b: Double): DoubleArray? {
        val fy = (l + 16) / 116
        val fx = fy + a / 500
        val fz = fy - b / 200
        fun inv(t: Double) = if (t.pow(3) > 216.0 / 24389) t.pow(3) else (116 * t - 16) / (24389.0 / 27)
        val x = inv(fx) * XN
        val y = inv(fy) * YN
        val z = inv(fz) * ZN
        val r = 3.2406 * x - 1.5372 * y - 0.4986 * z
        val g = -0.9689 * x + 1.8758 * y + 0.0415 * z
        val bl = 0.0557 * x - 0.2040 * y + 1.0570 * z
        val eps = 1e-4
        if (listOf(r, g, bl).any { it < -eps || it > 1 + eps }) return null
        return doubleArrayOf(gam(r.coerceIn(0.0, 1.0)), gam(g.coerceIn(0.0, 1.0)), gam(bl.coerceIn(0.0, 1.0)))
    }
}
