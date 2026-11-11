package com.droidbubble.app.island.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * `width()` / `height()` whose values are read in the **layout** phase (M2.2). An animated size
 * passed as a plain `Dp` is read in composition, which recomposes the whole shell on every frame
 * of a morph; read here, a frame only re-measures and redraws.
 *
 * Same result as `Modifier.width(w).height(h)`: the size is rounded to pixels and kept inside the
 * incoming constraints. A null lambda leaves that axis to the incoming constraints.
 */
fun Modifier.layoutSize(width: (() -> Dp)? = null, height: (() -> Dp)? = null): Modifier = layout { measurable, constraints ->
    val w = width?.invoke()?.coerceAtLeast(0.dp)?.roundToPx()?.let(constraints::constrainWidth)
    val h = height?.invoke()?.coerceAtLeast(0.dp)?.roundToPx()?.let(constraints::constrainHeight)
    val placeable = measurable.measure(
        Constraints(
            minWidth = w ?: constraints.minWidth,
            maxWidth = w ?: constraints.maxWidth,
            minHeight = h ?: constraints.minHeight,
            maxHeight = h ?: constraints.maxHeight,
        )
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
