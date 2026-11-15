package com.droidbubble.app.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** V2.1: the per-corner squircle, its RTL mapping and the capsule fast path. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SquircleShapeTest {

    private val density = Density(3f)
    private val size = Size(600f, 300f)

    /** Fills the outline into a bitmap, [k] times larger, and returns its alpha values. */
    private fun raster(outline: Outline, k: Float = 1f): Pair<IntArray, Int> {
        val w = (size.width * k).toInt()
        val h = (size.height * k).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { scale(k, k) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (outline) {
            is Outline.Generic -> canvas.drawPath(outline.path.asAndroidPath(), paint)
            is Outline.Rounded -> outline.roundRect.let { r -> canvas.drawRoundRect(r.left, r.top, r.right, r.bottom, r.topLeftCornerRadius.x, r.topLeftCornerRadius.y, paint) }
            is Outline.Rectangle -> outline.rect.let { canvas.drawRect(it.left, it.top, it.right, it.bottom, paint) }
        }
        return IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }.map { it ushr 24 }.toIntArray() to w
    }

    private fun outline(shape: SquircleShape, dir: LayoutDirection = LayoutDirection.Ltr) = shape.createOutline(size, dir, density)

    @Test
    fun singleRadiusEqualsFourEqualCorners() {
        val one = raster(outline(SquircleShape(30.dp))).first
        val four = raster(outline(SquircleShape(30.dp, 30.dp, 30.dp, 30.dp))).first
        assertTrue(one.contentEquals(four))
        assertEquals(SquircleShape(30.dp), SquircleShape(30.dp, 30.dp, 30.dp, 30.dp))
    }

    @Test
    fun startAndEndMirrorInRtl() {
        val rtl = raster(outline(SquircleShape(30.dp, 0.dp, 10.dp, 0.dp), LayoutDirection.Rtl)).first
        val ltr = raster(outline(SquircleShape(0.dp, 30.dp, 0.dp, 10.dp), LayoutDirection.Ltr)).first
        assertTrue(rtl.contentEquals(ltr))
    }

    @Test
    fun aZeroCornerIsSquare() {
        val (alpha, w) = raster(outline(SquircleShape(0.dp, 30.dp, 30.dp, 30.dp)))
        assertEquals("top-left pixel filled", 255, alpha[0])
        assertTrue("top-right pixel empty", alpha[w - 1] < 10)
    }

    @Test
    fun zeroRadiusIsARectangle() {
        assertTrue(outline(SquircleShape(0.dp)) is Outline.Rectangle)
    }

    @Test
    fun capsuleFastPathMatchesTheSquirclePath() {
        val fast = outline(SquircleShape(200.dp))
        assertTrue("a capsule takes the fast path", fast is Outline.Rounded)
        val budget = size.minDimension / 2f
        val generic = Outline.Generic(SquircleShape.squirclePath(size.width, size.height, FloatArray(4) { budget }, 0.6f))
        // Edge distance from the centre along 16 rays, at 8× resolution: within 0.5 px
        val k = 8f
        val (a, w) = raster(fast, k)
        val (b, _) = raster(generic, k)
        val h = a.size / w
        for (i in 0 until 16) {
            val angle = Math.toRadians(i * 22.5)
            fun edge(alpha: IntArray): Float {
                var t = 0f
                while (true) {
                    val x = (w / 2f + cos(angle) * t).toInt(); val y = (h / 2f + sin(angle) * t).toInt()
                    if (x !in 0 until w || y !in 0 until h || alpha[y * w + x] < 128) return t / k
                    t += 1f
                }
            }
            val d = abs(edge(a) - edge(b))
            assertTrue("ray $i differs by $d px", d <= 0.5f)
        }
    }
}
