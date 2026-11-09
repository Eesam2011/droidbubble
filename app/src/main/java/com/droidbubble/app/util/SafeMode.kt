package com.droidbubble.app.util

import android.content.Context

/**
 * Crash-loop guard (B11.3). The crash handler records each crash and the island's last primary
 * activity. When the island service starts after 3 crashes within 10 minutes, that activity kind
 * from that package is paused for an hour, the user is told, and can resume it at once.
 *
 * Backed by SharedPreferences with commit(): synchronous, so it is safe inside the crash handler.
 */
object SafeMode {
    private const val PREFS = "safe_mode"
    private const val KEY_CRASH_TIMES = "crash_times"
    private const val KEY_LAST_PRIMARY = "last_primary"
    private const val KEY_PAUSED = "paused"
    private const val KEY_PAUSED_UNTIL = "paused_until"

    const val CRASHES = 3
    const val WINDOW_MS = 10 * 60_000L
    const val PAUSE_MS = 60 * 60_000L
    private const val KEPT_CRASHES = 5

    /** What is paused: [kind] null means every island card from [pkg]. */
    data class Pause(val kind: String?, val pkg: String, val untilMs: Long) {
        fun covers(pkg: String, kind: String?): Boolean =
            pkg == this.pkg && (this.kind == null || this.kind == kind)
    }

    /** Pure: do the recorded crash times call for safe mode now? */
    fun isCrashLoop(crashTimes: List<Long>, nowMs: Long): Boolean =
        crashTimes.count { nowMs - it in 0..WINDOW_MS } >= CRASHES

    /** Pure: "kind|pkg" from the newest `primary` breadcrumb, if any. */
    fun lastPrimary(breadcrumbs: List<String>): String? =
        breadcrumbs.lastOrNull { " primary " in it }?.let { line ->
            val kind = Regex("""kind=(\S+)""").find(line)?.groupValues?.get(1)
            val pkg = Regex("""pkg=(\S+)""").find(line)?.groupValues?.get(1) ?: return@let null
            "${kind.orEmpty()}|$pkg"
        }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Crash handler: the crash time (last [KEPT_CRASHES] kept) and the last primary activity. */
    fun recordCrash(context: Context, nowMs: Long, lastPrimary: String?) {
        val p = prefs(context)
        val times = (crashTimes(context) + nowMs).takeLast(KEPT_CRASHES)
        p.edit()
            .putString(KEY_CRASH_TIMES, times.joinToString(","))
            .apply { if (lastPrimary != null) putString(KEY_LAST_PRIMARY, lastPrimary) }
            .commit()
    }

    fun crashTimes(context: Context): List<Long> =
        prefs(context).getString(KEY_CRASH_TIMES, null)?.split(",")?.mapNotNull { it.toLongOrNull() }.orEmpty()

    /**
     * Island service start: after a crash loop, pause the last primary activity for an hour and
     * clear the crash times. Returns the new pause, or null.
     */
    fun evaluateOnStart(context: Context, nowMs: Long): Pause? {
        if (!isCrashLoop(crashTimes(context), nowMs)) return null
        val p = prefs(context)
        val primary = p.getString(KEY_LAST_PRIMARY, null)
        p.edit().remove(KEY_CRASH_TIMES).commit()
        val (kind, pkg) = primary?.split("|", limit = 2)?.takeIf { it.size == 2 && it[1].isNotBlank() } ?: return null
        val pause = Pause(kind.ifBlank { null }, pkg, nowMs + PAUSE_MS)
        p.edit().putString(KEY_PAUSED, "${pause.kind.orEmpty()}|${pause.pkg}").putLong(KEY_PAUSED_UNTIL, pause.untilMs).commit()
        return pause
    }

    /** The pause in force, or null once it expires or is resumed. */
    fun activePause(context: Context, nowMs: Long): Pause? {
        val p = prefs(context)
        val until = p.getLong(KEY_PAUSED_UNTIL, 0L)
        if (until <= nowMs) return null
        val (kind, pkg) = p.getString(KEY_PAUSED, null)?.split("|", limit = 2)?.takeIf { it.size == 2 } ?: return null
        return Pause(kind.ifBlank { null }, pkg, until)
    }

    /** The user tapped "Resume". */
    fun resume(context: Context) {
        prefs(context).edit().remove(KEY_PAUSED).remove(KEY_PAUSED_UNTIL).commit()
    }
}
