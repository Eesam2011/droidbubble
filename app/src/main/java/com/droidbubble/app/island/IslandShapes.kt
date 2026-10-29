package com.droidbubble.app.island

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
 * 0.6 ≈ iOS. When the radius reaches half the short side (a capsule or circle), smoothing
 * fades out automatically so pills stay perfectly round-ended.
 */
@Immutable
class SquircleShape(private val radius: Dp, private val smoothing: Float = 0.6f) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val budget = min(w, h) / 2f
        val r = with(density) { radius.toPx() }.coerceIn(0f, budget)
        if (r <= 0f || w <= 0f || h <= 0f) return Outline.Rectangle(Rect(0f, 0f, w, h))

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
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is SquircleShape && other.radius == radius && other.smoothing == smoothing

    override fun hashCode(): Int = 31 * radius.hashCode() + smoothing.hashCode()
}

/**
 * Shape for the island surfaces. Android 13+ clips arbitrary paths with anti-aliasing, so it
 * gets the continuous squircle corners; older versions would clip a path with jagged edges,
 * so they keep regular rounded corners.
 */
fun islandShape(radius: Dp): Shape =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) SquircleShape(radius) else RoundedCornerShape(radius)

/**
 * Expanded-card corner radius that follows the card's size, so short cards (notifications)
 * and tall cards (media, weather) both look concentric with their content.
 */
fun expandedCornerRadiusFor(cardHeight: Dp): Dp = (cardHeight * 0.2f).coerceIn(26.dp, 40.dp)
