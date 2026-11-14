package com.droidbubble.app.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

/**
 * Where a glyph's ink sits in its box (V7.2): rasterises the vector's filled paths at [px] and
 * returns the alpha centroid's offset from the box centre, as fractions of the box size
 * (+x right, +y down). Blurring first (the research's "blur test") doesn't move a centroid, so it
 * is measured directly. Needs Robolectric `@GraphicsMode(NATIVE)`.
 */
object GlyphCentroidProbe {

    fun measure(vector: ImageVector, px: Int = 480): Pair<Float, Float> {
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; style = Paint.Style.FILL }
        val viewport = Matrix().apply { setScale(px / vector.viewportWidth, px / vector.viewportHeight) }
        // Groups may translate and scale their paths (Material Symbols shift theirs down by 960)
        fun draw(node: VectorNode, parent: Matrix) {
            when (node) {
                is VectorGroup -> {
                    val m = Matrix(parent).apply {
                        preTranslate(node.translationX, node.translationY)
                        preScale(node.scaleX, node.scaleY)
                    }
                    node.forEach { draw(it, m) }
                }
                is VectorPath -> {
                    val path = node.pathData.toPath().asAndroidPath()
                    path.fillType = if (node.pathFillType == PathFillType.EvenOdd) android.graphics.Path.FillType.EVEN_ODD else android.graphics.Path.FillType.WINDING
                    path.transform(parent)
                    paint.alpha = (node.fillAlpha * 255).toInt()
                    canvas.drawPath(path, paint)
                }
            }
        }
        draw(vector.root, viewport)
        val pixels = IntArray(px * px).also { bitmap.getPixels(it, 0, px, 0, 0, px, px) }
        var sum = 0.0
        var sx = 0.0
        var sy = 0.0
        for (y in 0 until px) for (x in 0 until px) {
            val a = (pixels[y * px + x] ushr 24) and 0xFF
            if (a == 0) continue
            sum += a; sx += a * (x + 0.5); sy += a * (y + 0.5)
        }
        check(sum > 0) { "${vector.name} drew nothing" }
        return ((sx / sum / px) - 0.5).toFloat() to ((sy / sum / px) - 0.5).toFloat()
    }
}
