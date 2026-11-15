package com.droidbubble.app.island.liquid

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandTokens
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The liquid body's maths (epic D1.1), mirrored from [LiquidBodyShader]'s AGSL so it can be tested
 * without a GPU. The pill and the bubble are signed-distance round boxes joined by a smooth
 * minimum: while they are close, the union grows a neck; past a gap of `k/2` the neck snaps.
 *
 * All values are px. Distances are negative inside a shape.
 */
object LiquidSdf {
    /** The largest smoothing radius. The neck snaps at a gap of `kMax/2`, today's snap gap. */
    val K_MAX: Dp = IslandTokens.Shell.NeckSnapGap * 2
    /** How far the pill swells, at most, when the bubble merges back. */
    val MERGE_BULGE: Dp = 3.dp
    /** Anti-alias half-width of the edge, px. */
    const val AA_PX = 0.75f

    /** A capsule-like round box: centre, half extents and corner radius, px. */
    data class Body(val cx: Float, val cy: Float, val halfW: Float, val halfH: Float, val r: Float)

    /** Signed distance from ([px], [py]), relative to the box centre, to a round box. */
    fun sdRoundBox(px: Float, py: Float, halfW: Float, halfH: Float, r: Float): Float {
        val qx = abs(px) - halfW + r
        val qy = abs(py) - halfH + r
        val outside = sqrt(max(qx, 0f) * max(qx, 0f) + max(qy, 0f) * max(qy, 0f))
        return outside + min(max(qx, qy), 0f) - r
    }

    /** Polynomial (quadratic) smooth minimum; `k <= 0` is the hard union. */
    fun smin(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return min(a, b)
        val h = max(k - abs(a - b), 0f) / k
        return min(a, b) - h * h * k * 0.25f
    }

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** The union's distance at ([x], [y]): what the shader fills. [bulge] widens the pill on both sides. */
    fun d(x: Float, y: Float, pill: Body, bubble: Body, k: Float, bulge: Float = 0f): Float {
        val dp = sdRoundBox(x - pill.cx, y - pill.cy, pill.halfW + bulge, pill.halfH, pill.r)
        val db = sdRoundBox(x - bubble.cx, y - bubble.cy, bubble.halfW, bubble.halfH, bubble.r)
        return smin(dp, db, k)
    }

    /** True while the smooth union still bridges two facing edges [gapPx] apart. */
    fun neckPresent(gapPx: Float, kPx: Float): Boolean = gapPx < kPx / 2f

    /**
     * Smoothing radius while splitting: full until the neck snaps (gap `kMax/2`), then falling to 0
     * by `1.5·kMax` so the facing sides stop bulging. It only falls once the gap is past `k/2`,
     * so a snapped neck can't re-form.
     */
    fun splitK(gapPx: Float, kMaxPx: Float): Float =
        kMaxPx * (1f - smoothstep(kMaxPx / 2f, 1.5f * kMaxPx, gapPx))

    /** Smoothing radius while merging: rises from 0 to `kMax` as the gap closes from `1.5·kMax` to 0. */
    fun mergeK(gapPx: Float, kMaxPx: Float): Float =
        kMaxPx * (1f - smoothstep(0f, 1.5f * kMaxPx, gapPx))
}
