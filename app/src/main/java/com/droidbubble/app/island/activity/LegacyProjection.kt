package com.droidbubble.app.island.activity

import com.droidbubble.app.model.NotificationInfo

/** A1.4: the two old single slots, as specs. */
data class LegacySlots(val notification: LiveActivitySpec?, val progress: LiveActivitySpec?) {
    companion object {
        val Empty = LegacySlots(null, null)
    }
}

private val LiveActivitySpec.info: NotificationInfo? get() = (payload as? ActivityPayload.Notification)?.info

private fun LiveActivitySpec.isNotificationCard() = source == ActivitySource.NOTIFICATION && kind != ActivityKind.PROGRESS && info != null
private fun LiveActivitySpec.isProgressCard() = source == ActivitySource.NOTIFICATION && kind == ActivityKind.PROGRESS && info != null

/**
 * A1.4: today's single-slot choice, computed from the store. A2.2 replaces it with arbitrate().
 *
 * [all] is the store in insertion order (oldest first); [changed] is the entry that was just
 * posted or updated, or null after a removal or dismissal.
 *
 * Notification slot:
 * 1. A ringing call or alert keeps the slot against a newcomer that is neither a call nor an alert.
 * 2. A live re-post while a one-off card shows waits behind it; any other newcomer takes the slot,
 *    so a one-off alert covers a live activity (today's behaviour; A2.3 reverses it).
 * 3. When the shown entry is gone, the newest remaining *live* entry returns. Unlike the old
 *    single stash, every live entry is still in the store, so none is lost.
 *
 * Progress slot: the entry just updated; when the shown one is gone, the newest remaining one.
 */
fun projectLegacySlots(all: Collection<LiveActivitySpec>, previous: LegacySlots, changed: ActivityId? = null): LegacySlots {
    val byId = all.associateBy { it.id }
    return LegacySlots(
        notification = projectNotification(all, byId, previous.notification?.id, changed),
        progress = projectProgress(all, byId, previous.progress?.id, changed),
    )
}

private fun projectNotification(
    all: Collection<LiveActivitySpec>,
    byId: Map<ActivityId, LiveActivitySpec>,
    shownId: ActivityId?,
    changed: ActivityId?,
): LiveActivitySpec? {
    val current = shownId?.let(byId::get)
    val newcomer = changed?.let(byId::get)?.takeIf { it.isNotificationCard() }
    if (newcomer == null) {
        // Nothing posted: keep what shows, else the newest live entry left (rule 3)
        return current ?: all.lastOrNull { it.isNotificationCard() && it.info!!.isLiveActivity }
    }
    if (current == null || current.id == newcomer.id) return newcomer
    val cur = current.info!!
    val new = newcomer.info!!
    // Rule 1: a ringing call or alarm isn't covered by anything but another call or alarm
    if ((cur.isIncomingCall || cur.isAlert) && !new.isCall && !new.isAlert) return current
    // Rule 2: a live activity re-posting behind a one-off card waits for it
    if (new.isLiveActivity && !cur.isLiveActivity && !new.isIncomingCall) return current
    return newcomer
}

private fun projectProgress(
    all: Collection<LiveActivitySpec>,
    byId: Map<ActivityId, LiveActivitySpec>,
    shownId: ActivityId?,
    changed: ActivityId?,
): LiveActivitySpec? {
    changed?.let(byId::get)?.takeIf { it.isProgressCard() }?.let { return it }
    return shownId?.let(byId::get) ?: all.lastOrNull { it.isProgressCard() }
}
