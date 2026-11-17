package com.droidbubble.app.island.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.droidbubble.app.model.KeylineMode

/** V6.1: when the island's keyline shows. */
object Keyline {
    /** ALWAYS, AUTO over a dark wallpaper or in night mode, and always with high contrast. */
    fun visible(mode: KeylineMode, darkBackdrop: Boolean, highContrast: Boolean): Boolean =
        highContrast || mode == KeylineMode.ALWAYS || (mode == KeylineMode.AUTO && darkBackdrop)
}

/**
 * V6.1: the island's keyline, drawn over the content. The stroke is twice [width] and centred on
 * the edge, and the surface's own clip (which must be the same [shape]) cuts the outer half away,
 * so the line is [width] wide all round, squircle corners included. [shape] is read in the draw
 * phase, so a corner that animates every frame doesn't recompose the surface.
 */
fun Modifier.islandKeyline(color: () -> Color, width: Dp, shape: () -> Shape): Modifier =
    drawWithCache {
        val path = Path().apply { addOutline(shape().createOutline(size, layoutDirection, this@drawWithCache)) }
        val stroke = Stroke(width.toPx() * 2f)
        onDrawWithContent {
            drawContent()
            val c = color()
            if (c.alpha > 0f) drawPath(path, c, style = stroke)
        }
    }
