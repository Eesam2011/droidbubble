package com.droidbubble.app.island

import androidx.compose.ui.unit.Density

/**
 * C3.10: how far the phone's font size setting grows the island's text.
 *
 * - Open cards follow it up to [CARD_MAX] (2.0, Android's largest setting).
 * - The compact pill and bubble stop at [COMPACT_MAX]: they sit around the camera and can't grow
 *   without limit.
 *
 * Compose's `Density(density, fontScale)` already applies Android 14's non-linear font curve (big
 * text grows less than small text; checked in C3.10 step 0), so the caps only clamp the scale.
 */
object IslandFontScale {
    const val MIN = 0.85f
    const val COMPACT_MAX = 1.3f
    const val CARD_MAX = 2.0f

    /** The scale the island and its open cards use for the phone's [system] font scale. */
    fun card(system: Float): Float = system.coerceIn(MIN, CARD_MAX)

    /** The scale the compact pill and bubble use. */
    fun compact(system: Float): Float = system.coerceIn(MIN, COMPACT_MAX)

    /** [density] with its font scale capped for the compact pill and bubble; itself when already within. */
    fun compactDensity(density: Density): Density =
        if (density.fontScale <= COMPACT_MAX) density else Density(density.density, COMPACT_MAX)
}
