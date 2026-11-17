package com.droidbubble.app.island

import com.droidbubble.app.model.NotificationInfo

/**
 * D2.2: when a new message banner peeks open into its card before receding. Pure, so the rules
 * are tested without the island.
 */
object PeekPolicy {
    /** The same app peeks at most once in this long; a burst of messages opens the card once. */
    const val COOLDOWN_MS = 5_000L

    /**
     * A new, plain message peeks. Calls, alarms and live activities have their own behaviour; and
     * nothing peeks while the phone is locked (the card would show the message on the lock
     * screen), while TalkBack is on (the card would close under its focus; C3 brings the focus
     * signal), while a card is already open, or within [COOLDOWN_MS] of that app's last peek.
     */
    fun shouldPeek(
        notification: NotificationInfo,
        isNew: Boolean,
        nowMs: Long,
        lastPeekMs: Long?,
        locked: Boolean,
        touchExploration: Boolean,
        expanded: Boolean,
    ): Boolean = isNew &&
        !notification.isCall && !notification.isIncomingCall && !notification.isAlert &&
        !notification.isLiveActivity && !notification.isOngoing &&
        !locked && !touchExploration && !expanded &&
        (lastPeekMs == null || nowMs - lastPeekMs >= COOLDOWN_MS)
}
