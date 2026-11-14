package com.droidbubble.app.island.icon

/*
 * Route decision (I2.1): shape morphs interpolate the corners of convex quads in this file and
 * round them with a round-join stroke. That can't twist or self-intersect, needs no dependency,
 * and is driven by IslandMotion springs, so it interrupts and follows the motion tier.
 * graphics-shapes `Morph` was tried first; its corner matching folded the play triangle into
 * slivers mid-way. AnimatedVectorDrawable (animation-graphics) would add a dependency, run on
 * timelines instead of springs and ignore the tiers.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import kotlin.math.hypot

/** The 24-unit box the morph shapes are drawn in, the same as the Material glyphs' viewport. */
private const val UNIT = 24f
/** A play / pause change drifts this far up at its midpoint (up/up: forward progression). */
private val MORPH_DRIFT = 1.dp

/**
 * One part of a morph: a convex quad (clockwise from the top-left, in a 24-unit box) with rounded
 * corners. A corner may repeat (a triangle's tip as two corners), which lets a triangle become a
 * bar. Drawn as [core] (the quad inset by [radius]) filled and stroked with a round join
 * [radius] wide, which gives the outline back with every corner rounded.
 */
@Immutable
class RoundedQuad(val corners: List<Offset>, val radius: Float) {
    val core: List<Offset> by lazy { insetConvex(corners, radius) }
}

/**
 * A glyph that morphs between two states (I2.1): part i of the start becomes part i of the end.
 * [start] and [end] are the real glyphs, drawn whenever the morph is at rest, so the settled look
 * is exactly the Material Symbols one.
 */
@Immutable
class MorphGlyph(val start: ImageVector, val end: ImageVector, val parts: List<Pair<RoundedQuad, RoundedQuad>>)

object MorphShapes {
    private fun quad(vararg xy: Float, radius: Float) =
        RoundedQuad(List(xy.size / 2) { Offset(xy[2 * it], xy[2 * it + 1]) }, radius)

    /** y on the play triangle's top edge (8,5 → 19,12) at [x]; the bottom edge mirrors it about y = 12. */
    private fun playTop(x: Float) = 5f + (x - 8f) * 7f / 11f

    /**
     * Play → pause. The play triangle (Material play_arrow: 8,5 · 19,12 · 8,19) is two
     * overlapping parts: a left quad up to x = 14.7 and the tip from x = 12.3 (its point counted
     * twice), so together they cover the whole triangle. They become the two pause bars (Material
     * pause: x 6.3–10.1 and 13.9–17.7, y 4.8–19.2): the tip's two points spread into the right bar's
     * right edge.
     */
    val PlayPause: MorphGlyph by lazy {
        MorphGlyph(
            start = IslandIcons.Play,
            end = IslandIcons.Pause,
            parts = listOf(
                quad(8f, 5f, 14.7f, playTop(14.7f), 14.7f, 24f - playTop(14.7f), 8f, 19f, radius = 1.2f) to
                    quad(6.3f, 4.8f, 10.1f, 4.8f, 10.1f, 19.2f, 6.3f, 19.2f, radius = 1.5f),
                quad(12.3f, playTop(12.3f), 19f, 12f, 19f, 12f, 12.3f, 24f - playTop(12.3f), radius = 1.2f) to
                    quad(13.9f, 4.8f, 17.7f, 4.8f, 17.7f, 19.2f, 13.9f, 19.2f, radius = 1.5f),
            ),
        )
    }
}

/**
 * [corners] (a convex polygon, clockwise on screen, corners may repeat) moved [d] inward: each
 * edge is pushed in by [d] and each corner lands where its two pushed edges meet. A repeated
 * corner stays repeated.
 */
internal fun insetConvex(corners: List<Offset>, d: Float): List<Offset> {
    val unique = corners.filterIndexed { i, c -> c != corners[(i + corners.size - 1) % corners.size] }
    val n = unique.size
    fun edgeLine(i: Int): Pair<Offset, Offset> {
        val a = unique[i]
        val b = unique[(i + 1) % n]
        val len = hypot(b.x - a.x, b.y - a.y)
        // Clockwise on a y-down screen: the inside is to the right of travel
        val normal = Offset(-(b.y - a.y) / len, (b.x - a.x) / len)
        return (a + normal * d) to (b + normal * d)
    }
    val moved = List(n) { i ->
        val (p1, p2) = edgeLine((i + n - 1) % n)
        val (p3, p4) = edgeLine(i)
        intersect(p1, p2, p3, p4)
    }
    return corners.map { c -> moved[unique.indexOf(c)] }
}

private fun intersect(p1: Offset, p2: Offset, p3: Offset, p4: Offset): Offset {
    val d1 = p2 - p1
    val d2 = p4 - p3
    val denom = d1.x * d2.y - d1.y * d2.x
    val t = ((p3.x - p1.x) * d2.y - (p3.y - p1.y) * d2.x) / denom
    return p1 + d1 * t
}

/**
 * Part i at progress [p]: corners and radius each go straight from start to end. The spring's
 * small overshoot is clamped away: past the start, a tip's two points would cross.
 */
internal fun lerpQuad(a: RoundedQuad, b: RoundedQuad, p: Float): Pair<List<Offset>, Float> {
    val t = p.coerceIn(0f, 1f)
    return List(a.core.size) { i -> a.core[i] + (b.core[i] - a.core[i]) * t } to (a.radius + (b.radius - a.radius) * t)
}

/**
 * [glyph] at its start shape (`atEnd` false) or its end shape, melting from one into the other
 * on [IslandMotion.Symbol.morph] when [atEnd] changes (I2.1). A change mid-morph reverses from
 * the current shape. Reduced motion: a 150 ms crossfade of the two glyphs, no shape in between.
 *
 * Progress is read only in the draw phase: a toggle recomposes once, the morph frames not at all.
 */
@Composable
fun MorphIcon(
    glyph: MorphGlyph,
    atEnd: Boolean,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = IslandTokens.Glyph.Compact,
) {
    val motion = LocalIslandMotion.current
    val reduced = motion.tier == MotionTier.REDUCED
    val progress = remember { Animatable(if (atEnd) 1f else 0f) }
    LaunchedEffect(atEnd) {
        val target = if (atEnd) 1f else 0f
        if (progress.targetValue == target && !progress.isRunning) return@LaunchedEffect
        progress.animateTo(
            target,
            if (reduced) TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing) else motion.spring(IslandMotion.Symbol.morph),
        )
    }
    val startPainter = rememberVectorPainter(glyph.start)
    val endPainter = rememberVectorPainter(glyph.end)
    val described = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(
        modifier.then(described).size(size).drawWithCache {
            val path = Path()
            val filter = ColorFilter.tint(tint)
            val startShift = IslandIcons.opticalShiftOf(glyph.start)?.first ?: 0f
            val endShift = IslandIcons.opticalShiftOf(glyph.end)?.first ?: 0f
            onDrawBehind {
                // The draw area (the `size` parameter shadows DrawScope.size here)
                val box = this.size
                val p = progress.value
                val atRest = !progress.isRunning && (p == 0f || p == 1f)
                if (atRest || reduced) {
                    // The real glyphs; in Reduced motion they crossfade
                    val pc = p.coerceIn(0f, 1f)
                    if (pc < 1f) translate(box.width * startShift, 0f) {
                        with(startPainter) { draw(box, alpha = 1f - pc, colorFilter = filter) }
                    }
                    if (pc > 0f) translate(box.width * endShift, 0f) {
                        with(endPainter) { draw(box, alpha = pc, colorFilter = filter) }
                    }
                    return@onDrawBehind
                }
                val shift = startShift + (endShift - startShift) * p.coerceIn(0f, 1f)
                val drift = -MORPH_DRIFT.toPx() * 4f * p * (1f - p)
                translate(box.width * shift, drift) {
                    scale(box.width / UNIT, box.height / UNIT, pivot = Offset.Zero) {
                        for ((a, b) in glyph.parts) {
                            val (core, radius) = lerpQuad(a, b, p)
                            path.rewind()
                            path.moveTo(core[0].x, core[0].y)
                            for (i in 1 until core.size) path.lineTo(core[i].x, core[i].y)
                            path.close()
                            drawPath(path, tint)
                            drawPath(path, tint, style = Stroke(width = radius * 2f, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        },
    )
}
