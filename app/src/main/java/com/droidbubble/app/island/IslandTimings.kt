package com.droidbubble.app.island

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import com.droidbubble.app.model.IslandPriority

/**
 * Every island timeout, in one place (F3.5). Each one goes through Android's "Time to take
 * action" accessibility setting ([AccessibilityManager.getRecommendedTimeoutMillis]), so people
 * who need longer get longer. With that setting at its default the values are exactly these.
 * F3.6 (05 R6): an open card stays long enough to read, media longest, and a ringing call or
 * alarm stays until it is answered or stopped.
 */
class IslandTimings(private val recommend: (originalMs: Int, flags: Int) -> Int) {

    /**
     * How long an expanded card stays open without a touch, for the card that is open.
     * Null means it stays until the user closes it: a ringing call or alarm ([isAlarm]).
     */
    fun autoCollapseFor(priority: IslandPriority, isAlarm: Boolean): Long? = when {
        isAlarm -> null
        priority == IslandPriority.MEDIA -> scaled(8_000, CONTROLS or TEXT)
        else -> scaled(6_000, CONTROLS or TEXT)
    }

    /** "Auto-dismiss notifications": how long a message stays. */
    val notificationAuto: Long get() = scaled(3_000, TEXT)

    /** How long a new notification holds the bubble before the island returns to compact. */
    val popIn: Long get() = scaled(4_500, TEXT)

    val unlock: Long get() = scaled(2_200, ICONS)
    val connection: Long get() = scaled(3_000, ICONS or TEXT)
    val ringer: Long get() = scaled(2_500, ICONS)
    val charging: Long get() = scaled(4_000, ICONS or TEXT)

    private fun scaled(ms: Int, flags: Int): Long = recommend(ms, flags).toLong()

    companion object {
        private const val ICONS = AccessibilityManager.FLAG_CONTENT_ICONS
        private const val TEXT = AccessibilityManager.FLAG_CONTENT_TEXT
        private const val CONTROLS = AccessibilityManager.FLAG_CONTENT_CONTROLS

        /** Timeouts scaled by the phone's accessibility timeout setting (API 29+). */
        fun from(context: Context): IslandTimings {
            val am = context.getSystemService(AccessibilityManager::class.java)
            return IslandTimings { ms, flags ->
                if (am != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) am.getRecommendedTimeoutMillis(ms, flags) else ms
            }
        }

        /** The plain values, for tests and before the service starts. */
        val Unscaled = IslandTimings { ms, _ -> ms }
    }
}
