package com.droidbubble.app.island

import com.droidbubble.app.island.icon.isSameAppNudge
import com.droidbubble.app.model.NotificationInfo

/** D2.3: "same app, new event" for the activity already shown, by app. */
data class IslandPulse(val id: String)

/**
 * D2.3: turns notification updates into [IslandPulse]s: the same app again with something new
 * ([isSameAppNudge]: never a first appearance, never a live activity's tick), at most one per
 * [throttleMs] per app, so a burst reads as one beat.
 */
class PulseDetector(private val throttleMs: Long = THROTTLE_MS) {
    private val last = HashMap<String, Long>()

    fun onUpdate(previous: NotificationInfo?, next: NotificationInfo?, nowMs: Long): IslandPulse? {
        if (next == null || !isSameAppNudge(previous, next)) return null
        val id = next.packageName
        val before = last[id]
        if (before != null && nowMs - before < throttleMs) return null
        last[id] = nowMs
        return IslandPulse(id)
    }

    fun reset() = last.clear()

    companion object {
        const val THROTTLE_MS = 600L
    }
}
