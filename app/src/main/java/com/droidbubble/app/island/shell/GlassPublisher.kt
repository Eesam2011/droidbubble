package com.droidbubble.app.island.shell

import androidx.compose.ui.geometry.Rect
import com.droidbubble.app.island.IslandStateManager.GlassPane
import kotlin.math.roundToInt

/**
 * M2.5: the liquid-glass blur windows behind the cards, as a pure function of the cards'
 * on-screen rects and their fade-in strengths. The shell calls it from the cards' layout
 * callbacks and when the strengths change, never from composition.
 */
internal fun glassPanes(
    bounds: Array<Rect?>,
    strengths: FloatArray,
    cornerPx: Float,
    motionInsetPx: Float,
): List<GlassPane> = buildList {
    for (i in bounds.indices) {
        val r = bounds[i] ?: continue
        val strength = strengths.getOrElse(i) { 0f }.coerceIn(0f, 1f)
        if (strength < 0.03f || r.width < 8f || r.height < 8f) continue
        // While moving, tuck the pane a few px inside the card so a one-frame lag of the blur
        // window can never peek out past the card's edge
        val inset = if (strength < 0.98f) motionInsetPx else 0f
        val w = (r.width - inset * 2f).roundToInt()
        val h = (r.height - inset * 2f).roundToInt()
        if (w < 4 || h < 4) continue
        add(
            GlassPane(
                left = (r.left + inset).roundToInt(),
                top = (r.top + inset).roundToInt(),
                width = w,
                height = h,
                cornerRadiusPx = (cornerPx - inset).coerceAtLeast(0f),
                strength = quantizeGlassStrength(strength),
            )
        )
    }
}

/** Coarse steps of 0.05: smooth to the eye, far fewer window updates. */
internal fun quantizeGlassStrength(strength: Float): Float = (strength.coerceIn(0f, 1f) * 20f).roundToInt() / 20f
