package com.droidbubble.app.island.shell

import com.droidbubble.app.island.IslandGeometry

/**
 * M3.1: how far the card's body is from its closed rect (0) to its open rect (1), from the body's
 * own geometry rather than a separate spring. Height leads (the drop is what the eye follows),
 * then the top, then the width. An axis that doesn't change between the two rects is left out.
 * Clamped to 0..1, so a spring's overshoot never pushes content past its endpoints.
 */
fun contentProgress(g: IslandGeometry, closed: IslandGeometry, open: IslandGeometry): Float {
    var sum = 0f
    var weights = 0f
    fun axis(value: Float, from: Float, to: Float, weight: Float) {
        if (kotlin.math.abs(to - from) < 0.5f) return
        sum += weight * ((value - from) / (to - from)).coerceIn(0f, 1f)
        weights += weight
    }
    axis(g.height.value, closed.height.value, open.height.value, HEIGHT_WEIGHT)
    axis(g.top.value, closed.top.value, open.top.value, TOP_WEIGHT)
    axis(g.width.value, closed.width.value, open.width.value, WIDTH_WEIGHT)
    return if (weights == 0f) 0f else (sum / weights).coerceIn(0f, 1f)
}

private const val HEIGHT_WEIGHT = 0.5f
private const val TOP_WEIGHT = 0.3f
private const val WIDTH_WEIGHT = 0.2f

/**
 * M3.1: every content alpha, scale and blur as a pure function of [contentProgress] `p`. The
 * same curve whichever way `p` is moving, so an interrupted morph never jumps. Values from
 * 03 §6: the shape leads, the content follows.
 */
object IslandChoreography {
    /** Incoming content fades in over p 0.45 → 0.90 (trailing items may start later). */
    fun incomingAlpha(p: Float, start: Float = 0.45f): Float = smoothstep(start, 0.90f, p)

    /** Incoming content grows from 0.92 to full size with its fade, anchored top-centre. */
    // Written from 1 down so a settled card is scaled by exactly 1f (0.92f + 0.08f is not)
    fun incomingScale(p: Float, start: Float = 0.45f): Float = 1f - 0.08f * (1f - incomingAlpha(p, start))

    /** The compact (pill) content is gone by p 0.35. */
    fun compactAlpha(p: Float): Float = 1f - smoothstep(0f, 0.35f, p)

    /** Only leaving content blurs: none on the way in, fading out with the content on the way out. */
    fun leavingBlurFraction(p: Float, contracting: Boolean): Float = if (contracting) 1f - incomingAlpha(p) else 0f

    const val LEAVING_BLUR_MAX_DP = 8f
    const val INCOMING_BLUR_MAX_DP = 4f
    const val LEAVING_SCALE = 0.96f
}

internal fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
