package com.droidbubble.app.island.activity

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A1.3: every activity the island knows about, by id, with dismissal memory. Main thread only.
 *
 * - [upsert] keeps the first `startedAt` across updates and the entry's place in the order.
 * - [dismiss] (a user swipe) drops the entry and remembers its `stateSignature`: a repost with the
 *   same signature is ignored, a changed one brings it back (B4.5).
 * - [remove] (the source ended) also forgets the dismissal.
 *
 * In shadow mode (A1.3) IslandStateManager writes every change here as well as to its old
 * single slots, and nothing reads the store; A1.4 turns the old slots into projections of it.
 */
class ActivityStore(
    private val now: () -> Long = System::currentTimeMillis,
    /** Debug builds check the thread (IslandStateManager passes a main-looper check). */
    private val threadCheck: () -> Unit = {},
) {
    private val _activities = MutableStateFlow<Map<ActivityId, LiveActivitySpec>>(emptyMap())
    val activities: StateFlow<Map<ActivityId, LiveActivitySpec>> = _activities.asStateFlow()

    /** id -> stateSignature at dismissal. */
    private val dismissed = HashMap<ActivityId, Int>()

    /** False when the entry is dismissed with an equal signature (nothing changes). */
    fun upsert(spec: LiveActivitySpec): Boolean {
        threadCheck()
        val signatureAtDismissal = dismissed[spec.id]
        if (signatureAtDismissal != null) {
            if (signatureAtDismissal == spec.stateSignature) return false
            dismissed.remove(spec.id)
        }
        val current = _activities.value
        val previous = current[spec.id]
        val stored = if (previous != null && previous.startedAt != spec.startedAt) spec.copy(startedAt = previous.startedAt) else spec
        if (previous == stored) return true
        _activities.value = LinkedHashMap(current).apply { put(spec.id, stored) }
        return true
    }

    fun remove(id: ActivityId) {
        threadCheck()
        dismissed.remove(id)
        drop(id)
    }

    fun dismiss(id: ActivityId) {
        threadCheck()
        val spec = _activities.value[id] ?: return
        dismissed[id] = spec.stateSignature
        drop(id)
    }

    /** "Dismiss media on pause" and similar: forget a dismissal without touching the entry. */
    fun undismiss(id: ActivityId) {
        threadCheck()
        dismissed.remove(id)
    }

    fun isDismissed(id: ActivityId): Boolean = id in dismissed

    fun get(id: ActivityId): LiveActivitySpec? = _activities.value[id]

    /** Drops entries whose `expiresAt` or `staleAt` is at or before [atMs]. */
    fun expire(atMs: Long = now()) {
        threadCheck()
        val current = _activities.value
        val keep = current.filterValues { s ->
            (s.expiresAt == null || s.expiresAt > atMs) && (s.staleAt == null || s.staleAt > atMs)
        }
        if (keep.size != current.size) _activities.value = LinkedHashMap(keep)
    }

    @VisibleForTesting
    fun clear() {
        dismissed.clear()
        _activities.value = emptyMap()
    }

    private fun drop(id: ActivityId) {
        val current = _activities.value
        if (id !in current) return
        _activities.value = LinkedHashMap(current).apply { remove(id) }
    }
}
