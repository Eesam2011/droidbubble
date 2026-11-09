package com.droidbubble.app.service

import java.util.concurrent.ConcurrentHashMap

/**
 * B10.4: a running clock or recorder app reposts its custom-view notification about once a second,
 * and reading the timer means inflating its RemoteViews each time. A running timer's reading is
 * reused while its buttons stay the same (pause and resume swap them), re-read at least every
 * [maxAgeMs] as a safety net. Written on the parser thread, cleared from main on removal.
 */
class CustomTimerCache<T>(private val maxAgeMs: Long = 30_000L) {
    private class Entry<T>(val actionsSignature: List<String>, val value: T, val running: Boolean, val atMs: Long)

    private val entries = ConcurrentHashMap<String, Entry<T>>()

    /** The cached reading for [key], if it is a running timer with the same buttons and fresh enough. */
    fun lookup(key: String, actionsSignature: List<String>, nowMs: Long): T? {
        val entry = entries[key] ?: return null
        if (!entry.running || entry.actionsSignature != actionsSignature || nowMs - entry.atMs >= maxAgeMs) return null
        return entry.value
    }

    fun store(key: String, actionsSignature: List<String>, value: T, running: Boolean, nowMs: Long) {
        if (entries.size > 64) entries.clear()
        entries[key] = Entry(actionsSignature, value, running, nowMs)
    }

    fun remove(key: String) {
        entries.remove(key)
    }
}
