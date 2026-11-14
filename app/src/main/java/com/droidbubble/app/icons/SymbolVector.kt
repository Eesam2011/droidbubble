package com.droidbubble.app.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Builds a Material Symbols glyph from its SVG path (V7.3). Symbols are drawn in a 960-unit box
 * whose top edge is at y = -960, so the path is shifted down by 960. The fill colour doesn't
 * matter: `Icon` tints it.
 */
internal fun symbolVector(name: String, sizeDp: Int, pathData: String, autoMirror: Boolean = false): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = sizeDp.dp,
        defaultHeight = sizeDp.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = autoMirror,
    )
        .addGroup(translationY = 960f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .clearGroup()
        .build()
