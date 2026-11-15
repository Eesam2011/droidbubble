package com.droidbubble.app.island

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rounded rectangle with continuous ("squircle") corners, like Apple's: the curve eases into
 * the straight edges instead of meeting them abruptly as a circular arc does.
 *
 * Geometry follows Figma's corner smoothing. [smoothing] 0 = plain circular corners,
 * 0.6 ≈ iOS. When a radius reaches half the short side (a capsule or circle), smoothing
 * fades out automatically so pills stay perfectly round-ended.
 *
 * Each corner has its own radius (V2.1); start and end follow the layout direction. A shape
 * whose corners are all full capsule ends is returned as a plain rounded rect, which the
 * renderer clips by outline instead of by path (the squircle maths gives a pure circle there).
 */
@Immutable
class SquircleShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float = 0.6f,
) : Shape {

    constructor(radius: Dp, smoothing: Float = 0.6f) : this(radius, radius, radius, radius, smoothing)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(Rect(0f, 0f, w.coerceAtLeast(0f), h.coerceAtLeast(0f)))
        val budget = min(w, h) / 2f
        val radii = physicalRadii(layoutDirection, density, budget)
        if (radii.all { it <= 0f }) return Outline.Rectangle(Rect(0f, 0f, w, h))
        // Capsule fast path: every corner is a full round end
        if (radii.all { it >= budget - 0.5f }) {
            return Outline.Rounded(RoundRect(0f, 0f, w, h, CornerRadius(budget)))
        }
        return Outline.Generic(squirclePath(w, h, radii, smoothing))
    }

    /** Radii in px, clockwise from top-right (the path's corner order), each capped at half the short side. */
    internal fun physicalRadii(layoutDirection: LayoutDirection, density: Density, budget: Float): FloatArray {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val topLeft = if (ltr) topStart else topEnd
        val topRight = if (ltr) topEnd else topStart
        val bottomRight = if (ltr) bottomEnd else bottomStart
        val bottomLeft = if (ltr) bottomStart else bottomEnd
        val r = with(density) { floatArrayOf(topRight.toPx(), bottomRight.toPx(), bottomLeft.toPx(), topLeft.toPx()) }
        for (i in r.indices) r[i] = r[i].coerceIn(0f, budget)
        return r
    }

    override fun equals(other: Any?): Boolean =
        other is SquircleShape && other.topStart == topStart && other.topEnd == topEnd &&
            other.bottomEnd == bottomEnd && other.bottomStart == bottomStart && other.smoothing == smoothing

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return 31 * result + smoothing.hashCode()
    }

    internal companion object {
        /**
         * The squircle path for corner radii in px, clockwise from top-right. Each corner runs
         * the same smoothing maths on its own radius, so equal radii give the single-radius shape.
         */
        fun squirclePath(w: Float, h: Float, radii: FloatArray, smoothing: Float): Path {
            val budget = min(w, h) / 2f
            val path = Path()
            // Corners clockwise from top-right: corner point, incoming edge direction, outgoing edge direction
            val corners = listOf(
                Triple(Offset(w, 0f), Offset(1f, 0f), Offset(0f, 1f)),
                Triple(Offset(w, h), Offset(0f, 1f), Offset(-1f, 0f)),
                Triple(Offset(0f, h), Offset(-1f, 0f), Offset(0f, -1f)),
                Triple(Offset(0f, 0f), Offset(0f, -1f), Offset(1f, 0f))
            )
            val arcStartAngles = floatArrayOf(-45f, 45f, 135f, 225f)

            corners.forEachIndexed { i, (corner, inDir, outDir) ->
                val r = radii[i]
                if (r <= 0f) {
                    if (i == 0) path.moveTo(corner.x, corner.y) else path.lineTo(corner.x, corner.y)
                    return@forEachIndexed
                }
                // Length along each edge the corner occupies; shrink the smoothing if it doesn't fit
                var s = smoothing.coerceIn(0f, 1f)
                var p = (1f + s) * r
                if (p > budget) {
                    s = (budget / r - 1f).coerceAtLeast(0f)
                    p = budget
                }
                val arcMeasure = 90f * (1f - s)
                val arcSectionLength = sin(Math.toRadians(arcMeasure / 2.0)).toFloat() * r * sqrt(2f)
                val angleAlpha = (90f - arcMeasure) / 2f
                val p3ToP4 = r * tan(Math.toRadians(angleAlpha / 2.0)).toFloat()
                val angleBeta = Math.toRadians(45.0 * s)
                val c = p3ToP4 * cos(angleBeta).toFloat()
                val d = c * tan(angleBeta).toFloat()
                val b = (p - arcSectionLength - c - d) / 3f
                val a = 2f * b

                val start = corner - inDir * p
                if (i == 0) path.moveTo(start.x, start.y) else path.lineTo(start.x, start.y)

                // Bezier easing out of the straight edge into the arc
                val c1 = start + inDir * a
                val c2 = start + inDir * (a + b)
                val e1 = start + inDir * (a + b + c) + outDir * d
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, e1.x, e1.y)

                // Circular middle section centred on the corner's diagonal
                if (arcMeasure > 0.01f) {
                    val center = corner - inDir * r + outDir * r
                    path.arcTo(
                        rect = Rect(center, r),
                        startAngleDegrees = arcStartAngles[i] - arcMeasure / 2f,
                        sweepAngleDegrees = arcMeasure,
                        forceMoveTo = false
                    )
                }

                // Mirror Bezier easing back into the next straight edge
                val e2 = e1 + inDir * arcSectionLength + outDir * arcSectionLength
                val c3 = e2 + inDir * d + outDir * c
                val c4 = e2 + inDir * d + outDir * (b + c)
                val end = corner + outDir * p
                path.cubicTo(c3.x, c3.y, c4.x, c4.y, end.x, end.y)
            }
            path.close()
            return path
        }
    }
}

/**
 * Shape for the island surfaces. Android 13+ clips arbitrary paths with anti-aliasing, so it
 * gets the continuous squircle corners; older versions would clip a path with jagged edges,
 * so they keep regular rounded corners.
 */
fun islandShape(radius: Dp): Shape =
    if (IslandApi.hasSmoothPathClip) SquircleShape(radius) else RoundedCornerShape(radius)

/** [islandShape] with a radius per corner (V2.1); start and end follow the layout direction. */
fun islandShape(topStart: Dp, topEnd: Dp, bottomEnd: Dp, bottomStart: Dp): Shape =
    if (IslandApi.hasSmoothPathClip) SquircleShape(topStart, topEnd, bottomEnd, bottomStart)
    else RoundedCornerShape(topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart)

/**
 * [islandShape] instances by exact corner radius (M2.3), so a surface that is redrawn at the same
 * corner reuses one shape instead of allocating per frame. Keys are exact (no rounding), so the
 * drawn edge is the same as [islandShape]'s to the pixel. Main thread only.
 */
object IslandShapeCache {
    private const val MAX = 64
    private val shapes = object : LinkedHashMap<Float, Shape>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Float, Shape>?) = size > MAX
    }

    fun get(radius: Dp): Shape = shapes.getOrPut(radius.value) { islandShape(radius) }
}

/**
 * Black bridge that joins a notch-style island to the top edge of the screen: a bar as wide as
 * the pill plus an inward-curving "ear" on each side, like a phone notch. [ear] is the radius
 * of those concave corners.
 */
fun notchBridgePath(width: Float, height: Float, ear: Float): Path {
    val e = ear.coerceIn(0f, min(width / 2f, height))
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(width, 0f)
        quadraticTo(width - e, 0f, width - e, e)
        lineTo(width - e, height)
        lineTo(e, height)
        lineTo(e, e)
        quadraticTo(e, 0f, 0f, 0f)
        close()
    }
}

/** Notch-style pill: square top (it hangs from the screen edge), round bottom corners (V2.3: squircle family). */
fun notchPillShape(bottomRadius: Dp): Shape = islandShape(0.dp, 0.dp, bottomRadius, bottomRadius)
