package com.droidbubble.app.island

import com.droidbubble.app.model.NotificationInfo

private val DIGITS = Regex("""\d+""")

/**
 * What must change for a card the user dismissed to come back (B4.5). Phase 7 A1.2 reuses this as
 * `LiveActivitySpec.stateSignature`.
 *
 * - A message comes back on any new text: a new text is a new message.
 * - A live activity (ride, delivery, timer...) comes back only on a change of state: its kind,
 *   ringing or alert, paused or running, its buttons, or its words. Numbers are masked, so an ETA
 *   or a clock that ticks ("Arrives in 4 min" → "3 min") keeps it dismissed, while "Driver has
 *   arrived" brings it back.
 */
fun dismissSignature(n: NotificationInfo): Int =
    if (!n.isLiveActivity) {
        n.text.hashCode()
    } else {
        listOf(
            liveActivityKind(n),
            n.isIncomingCall,
            n.isAlert,
            n.liveTimerBaseMs == 0L,
            n.actions.map { it.title },
            n.title.replace(DIGITS, "#"),
            n.text.replace(DIGITS, "#"),
        ).hashCode()
    }
