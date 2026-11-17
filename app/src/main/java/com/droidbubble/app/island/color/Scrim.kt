package com.droidbubble.app.island.color

import android.graphics.Bitmap

/**
 * V5.3: how dark the scrim behind the song card's text must be so white text keeps 4.5:1 or
 * better on bright covers. Art drawn at [artAlpha] over black has luminance `artAlpha × Y`; a
 * black scrim of alpha `a` brings it to `(1 − a)·Y1`, and at 0.18 or less white text is 4.57:1.
 */
object Scrim {
    const val TARGET_Y = 0.18f
    /** Solid card: the scrim it always had (dark covers look unchanged). */
    const val MIN_SOLID = 0.38f
    const val MAX = 0.85f

    fun alphaFor(luminance: Float, artAlpha: Float, min: Float = MIN_SOLID): Float {
        val y1 = luminance * artAlpha
        if (y1 <= TARGET_Y) return min
        return (1f - TARGET_Y / y1).coerceIn(min, MAX)
    }

    /** The brightest of the 8 × 8 cells in the top and bottom 40% of the art, as linear luminance. */
    data class Bands(val top: Float, val bottom: Float)

    private val cache = java.util.WeakHashMap<Bitmap, Bands>()

    fun bands(art: Bitmap): Bands {
        synchronized(cache) { cache[art]?.let { return it } }
        val result = try {
            val small = Bitmap.createScaledBitmap(art, 8, 8, true)
            val px = IntArray(64)
            small.getPixels(px, 0, 8, 0, 0, 8, 8)
            if (small != art) small.recycle()
            bandsOf(px)
        } catch (_: Exception) {
            Bands(0f, 0f)
        }
        synchronized(cache) { cache[art] = result }
        return result
    }

    /** [bands] on an 8 × 8 grid of ARGB pixels, row by row: rows 0–2 are the top 40%, rows 5–7 the bottom. */
    fun bandsOf(px: IntArray): Bands {
        fun y(p: Int) = androidx.core.graphics.ColorUtils.calculateLuminance(p).toFloat()
        val top = (0 until 24).maxOf { y(px[it]) }
        val bottom = (40 until 64).maxOf { y(px[it]) }
        return Bands(top, bottom)
    }
}
