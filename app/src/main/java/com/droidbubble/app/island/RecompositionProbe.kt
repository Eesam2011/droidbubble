package com.droidbubble.app.island

/**
 * Test hook: called on every composition of [IslandOverlayRoot] (S4.1). Null in production, so
 * it costs one field read per composition.
 */
object RecompositionProbe {
    @JvmField
    var onRoot: (() -> Unit)? = null
}
