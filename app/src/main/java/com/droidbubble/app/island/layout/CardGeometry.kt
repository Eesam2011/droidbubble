package com.droidbubble.app.island.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandShapeCache

/**
 * The open card's margin and radius system (V2.1): one card radius [R], two named insets, and
 * the concentric radius for anything nested inside. Same numbers as `LiveLayout`, which now reads
 * them from here.
 *
 * Concentric: an element inset by `i` from a card with radius `R` shares the card's corner centre
 * when its own radius is `R − i`. Square-ish elements are capped at 0.42 of their short side so
 * tiles and art stay rounded squares instead of turning into circles.
 */
@Immutable
class CardGeometry(scale: Float, fontScale: Float = 1f) {
    /**
     * How much the phone's font size setting grows the cards. Text follows the setting fully;
     * controls and rows grow 60% as much, so big text still fits without the card ballooning.
     */
    private val textGrowth = fontScale.coerceIn(1f, com.droidbubble.app.island.IslandFontScale.CARD_MAX)
    val rowGrowth = 1f + (textGrowth - 1f) * 0.6f

    /** Inset of a one-row live card from its edges. */
    val m: Dp = (14.dp * scale).coerceIn(12.dp, 18.dp)
    val gap: Dp = (10.dp * scale).coerceIn(10.dp, 14.dp)
    /** Round controls and avatars: never smaller than the 48 dp minimum touch target. */
    val control: Dp = (50.dp * scale).coerceIn(48.dp, 58.dp) * rowGrowth
    /** The card's corner radius: concentric with a control inset by [m]. */
    val R: Dp = control / 2 + m

    /** One-row live cards and the flashlight card. */
    val insetTight: Dp get() = m
    /** Every tall card: a corner-nested element here gets a radius of exactly `R/2`. */
    val insetRoomy: Dp get() = R / 2

    /** The concentric radius for an element inset by [inset], never below 6 dp. */
    fun inner(inset: Dp): Dp = (R - inset).coerceAtLeast(MIN_INNER)

    /** [inner] for a [w] × [h] element: square-ish ones are capped at 0.42 of the short side, bars may become capsules. */
    fun radiusFor(inset: Dp, w: Dp, h: Dp): Dp {
        val short = minOf(w, h)
        val long = maxOf(w, h)
        val cap = if (long < short * 1.5f) short * SQUARE_CAP else short / 2
        return minOf(inner(inset), cap)
    }

    companion object {
        val MIN_INNER = 6.dp
        /** Square-ish elements stop at this fraction of their short side. */
        const val SQUARE_CAP = 0.42f
        /** The song cover in the media card stops lower: less round than a tile (owner, CP1 2026-11-15). */
        const val COVER_CAP = 0.27f
    }
}

/** The card geometry for the island's current scale and font size, provided by `IslandOverlayRoot`. */
val LocalCardGeometry = staticCompositionLocalOf { CardGeometry(1.1f) }

/** Clips to the concentric squircle for a [w] × [h] element inset by [inset] from the card edge (V2.1). */
fun Modifier.concentricClip(inset: Dp, w: Dp, h: Dp): Modifier = composed {
    clip(IslandShapeCache.get(LocalCardGeometry.current.radiusFor(inset, w, h)))
}
