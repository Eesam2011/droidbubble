package com.droidbubble.app.island.motion

/**
 * How much motion the island may use.
 * - [FULL]: everything.
 * - [EFFICIENT]: Battery Saver. No stretch or blur, and ambient loops run at 30 fps at most.
 * - [REDUCED]: "Reduce motion" or the system's "Remove animations". Shapes move without
 *   overshoot, content crossfades, and nothing bounces, shakes or loops.
 *
 * M5.1 resolves the tier; until then the island always runs [FULL].
 */
enum class MotionTier { FULL, EFFICIENT, REDUCED }
