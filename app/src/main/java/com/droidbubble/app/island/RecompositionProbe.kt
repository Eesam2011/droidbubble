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

    /** M2.6: called on every composition of the pill's content (CompactContent). */
    @JvmField
    var onCompactContent: (() -> Unit)? = null

    /** M5.2: called each time a motion blur effect is handed to a layer (motionBlur). */
    @JvmField
    var onMotionBlur: (() -> Unit)? = null
}
