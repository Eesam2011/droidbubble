package com.droidbubble.app.island.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
fun Modifier.islandKeyline(color: () -> Color, width: Dp, openTop: Boolean = false, shape: () -> Shape): Modifier =
    drawWithCache {
        val strokePx = width.toPx() * 2f
        // V6.2 [openTop] (the notch hangs from the screen edge): the shape is drawn taller and
        // raised, so its top edge and that edge's stroke land above the clip and only the sides
        // and bottom show. Needs a square top, as the notch pill has.
        val lift = if (openTop) strokePx * 2f else 0f
        val outline = shape().createOutline(Size(size.width, size.height + lift), layoutDirection, this@drawWithCache)
        val path = Path().apply { addOutline(outline); if (lift > 0f) translate(Offset(0f, -lift)) }
        val stroke = Stroke(strokePx)
        onDrawWithContent {
            drawContent()
            val c = color()
            if (c.alpha > 0f) drawPath(path, c, style = stroke)
        }
    }
