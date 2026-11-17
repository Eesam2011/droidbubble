package com.droidbubble.app.island.motion

/** D2.4: the two physical moments of the liquid split. */
enum class NeckMoment { SNAP, MERGE }

/**
 * D2.4: watches the gap between the pill and the bubble and reports each physical moment once:
 * [NeckMoment.SNAP] when a joined pair pulls past half the snap gap, [NeckMoment.MERGE] when a
 * split pair touches again (gap ≤ 0). A spring wobbling around either line can't repeat it,
 * because each moment needs the other one first. The first gap seen only sets the state (an
 * island already at rest is silent), and two moments closer than [throttleMs] give one.
 */
class CrossingLatch(private val snapGapDp: Float, private val throttleMs: Long = THROTTLE_MS) {
    private var split: Boolean? = null
    private var lastMs = Long.MIN_VALUE / 2

    fun update(gapDp: Float, nowMs: Long): NeckMoment? {
        val wasSplit = split
        if (wasSplit == null) {
            split = gapDp > 0f
            return null
        }
        val moment = when {
            !wasSplit && gapDp >= snapGapDp / 2f -> NeckMoment.SNAP.also { split = true }
            wasSplit && gapDp <= 0f -> NeckMoment.MERGE.also { split = false }
            else -> null
        } ?: return null
        if (nowMs - lastMs < throttleMs) return null
        lastMs = nowMs
        return moment
    }

    /** Forget the state, e.g. while a card is open (the bubble folds away under it). */
    fun reset() {
        split = null
    }

    companion object {
        const val THROTTLE_MS = 150L
    }
}
