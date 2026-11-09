package com.droidbubble.app.island

/**
 * Test hook: called on every composition of [IslandOverlayRoot] (S4.1). Null in production, so
 * it costs one field read per composition.
 */
object RecompositionProbe {
    @JvmField
    var onRoot: (() -> Unit)? = null

    /** Called on every composition of the expanded media card (B8.2). */
    @JvmField
    var onMediaCard: (() -> Unit)? = null
}
