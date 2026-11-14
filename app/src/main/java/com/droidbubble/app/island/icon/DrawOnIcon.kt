package com.droidbubble.app.island.icon

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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier

/** Glyph geometry is in the same 24-unit box as the Material glyphs. */
private const val UNIT = 24f

/**
 * A stroke that draws itself on (I2.2): a centre line in a 24-unit box, its stroke [width], and
 * the real glyph ([glyph]) that is drawn instead once the stroke is complete, so the settled look
 * is exactly the Material Symbols one.
 */
@Immutable
class IconStrokePath(val glyph: ImageVector, val width: Float, val points: List<Offset>) {
    fun path(): Path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
}

object IconStrokes {
    /** Material check, compact size: short stroke down-right, long stroke up-right. */
    val Check: IconStrokePath by lazy {
        IconStrokePath(IslandIcons.Compact.Done, width = 2.4f, points = listOf(Offset(5.6f, 12.6f), Offset(9.6f, 16.5f), Offset(18.4f, 7.7f)))
    }
}

/**
 * [stroke] drawing itself from its first point to its last when [drawn] turns true (I2.2), on
 * [IslandMotion.Symbol.draw]. The first [delayFraction] of the spring passes before the line
 * starts, which staggers it after whatever appears with it without a wall-clock delay. Turning
 * [drawn] false clears it at once (the pill around it is fading anyway). Reduced motion: the
 * whole glyph fades in over 150 ms.
 */
@Composable
fun DrawOnIcon(
    stroke: IconStrokePath,
    drawn: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = IslandTokens.Glyph.Compact,
    delayFraction: Float = 0f,
) {
    val motion = LocalIslandMotion.current
    val reduced = motion.tier == MotionTier.REDUCED
    val progress = remember { Animatable(if (drawn) 1f else 0f) }
    LaunchedEffect(drawn) {
        if (!drawn) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        if (progress.value >= 1f) return@LaunchedEffect
        progress.animateTo(
            1f,
            if (reduced) TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing) else motion.spring(IslandMotion.Symbol.draw),
        )
    }
    val painter = rememberVectorPainter(stroke.glyph)
    val described = if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier
    Box(
        modifier.then(described).size(size).drawWithCache {
            val full = stroke.path()
            val measure = PathMeasure().apply { setPath(full, false) }
            val length = measure.length
            val partial = Path()
            val filter = ColorFilter.tint(tint)
            val line = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            onDrawBehind {
                val box = this.size
                val p = progress.value
                if (p <= 0f) return@onDrawBehind
                if (reduced || (p >= 1f && !progress.isRunning)) {
                    with(painter) { draw(box, alpha = if (reduced) p.coerceIn(0f, 1f) else 1f, colorFilter = filter) }
                    return@onDrawBehind
                }
                val d = delayFraction.coerceIn(0f, 0.9f)
                val drawnPart = ((p - d) / (1f - d)).coerceIn(0f, 1f)
                if (drawnPart <= 0f) return@onDrawBehind
                partial.reset()
                measure.getSegment(0f, length * drawnPart, partial, true)
                scale(box.width / UNIT, box.height / UNIT, pivot = Offset.Zero) {
                    drawPath(partial, tint, style = line)
                }
            }
        },
    )
}

/** The length drawn at progress [p] of a [length]-long stroke whose line starts at [delayFraction]. */
internal fun drawnLength(length: Float, p: Float, delayFraction: Float = 0f): Float {
    val d = delayFraction.coerceIn(0f, 0.9f)
    return length * ((p - d) / (1f - d)).coerceIn(0f, 1f)
}
