package com.droidbubble.app.island.icon

/*
 * Route decision (I2.1): shape morphs use androidx.graphics:graphics-shapes `Morph`, which is
 * already on the classpath through material3. They are springs driven by IslandMotion, so they
 * interrupt and follow the motion tier. AnimatedVectorDrawable (animation-graphics) would add a
 * dependency, run on timelines instead of springs and ignore the tiers; it is only for a glyph
 * that can't be built from convex polygons and strokes, and none of the island's glyphs needs it.
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier

/** The 24-unit box the morph shapes are drawn in, the same as the Material glyphs' viewport. */
private const val UNIT = 24f
/** Corners use the island's squircle smoothing, so the glyph belongs to the same family. */
private const val SMOOTHING = 0.6f
/** A play / pause change drifts this far up at its midpoint (up/up: forward progression). */
private val MORPH_DRIFT = 1.dp

/**
 * A glyph that morphs between two states (I2.1). Each part is a pair of convex rounded polygons
 * in a 24-unit box; part i of the start shape becomes part i of the end shape. [start] and [end]
 * are the real glyphs, drawn whenever the morph is at rest, so the settled look is exactly the
 * Material Symbols one.
 */
@Immutable
class MorphGlyph(val start: ImageVector, val end: ImageVector, val parts: List<Pair<RoundedPolygon, RoundedPolygon>>) {
    val morphs: List<Morph> by lazy { parts.map { (a, b) -> Morph(a, b) } }
}

object MorphShapes {
    private fun polygon(vararg xy: Float, radius: Float) =
        RoundedPolygon(xy, rounding = CornerRounding(radius, SMOOTHING))

    /**
     * Play -> pause. The play triangle (Material play_arrow: 8,5 · 19,12 · 8,19) is cut at
     * x = 13.5 into a left quad and a right triangle; those become the two pause bars (Material
     * pause: x 6.3-10.1 and 13.9-17.7, y 4.8-19.2). Vertices go clockwise from the top-left.
     */
    val PlayPause: MorphGlyph by lazy {
        MorphGlyph(
            start = IslandIcons.Play,
            end = IslandIcons.Pause,
            parts = listOf(
                polygon(8f, 5f, 13.5f, 8.5f, 13.5f, 15.5f, 8f, 19f, radius = 1.2f) to
                    polygon(6.3f, 4.8f, 10.1f, 4.8f, 10.1f, 19.2f, 6.3f, 19.2f, radius = 1.9f),
                polygon(13.5f, 8.5f, 19f, 12f, 13.5f, 15.5f, radius = 1.2f) to
                    polygon(13.9f, 4.8f, 17.7f, 4.8f, 17.7f, 19.2f, 13.9f, 19.2f, radius = 1.9f),
            ),
        )
    }
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
            val path = android.graphics.Path()
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
                    if (pc < 1f) translate(this.size.width * startShift, 0f) {
                        with(startPainter) { draw(box, alpha = 1f - pc, colorFilter = filter) }
                    }
                    if (pc > 0f) translate(this.size.width * endShift, 0f) {
                        with(endPainter) { draw(box, alpha = pc, colorFilter = filter) }
                    }
                    return@onDrawBehind
                }
                val shift = startShift + (endShift - startShift) * p.coerceIn(0f, 1f)
                val drift = -MORPH_DRIFT.toPx() * 4f * p * (1f - p)
                translate(this.size.width * shift, drift) {
                    scale(this.size.width / UNIT, this.size.height / UNIT, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        for (m in glyph.morphs) {
                            path.rewind()
                            m.toPath(p, path)
                            drawPath(path.asComposePath(), tint)
                        }
                    }
                }
            }
        },
    )
}
