package com.droidbubble.app.island.activity.controls

import android.app.PendingIntent

/** A4.1: what a notification button does, independent of the language its title is in. */
enum class ControlRole {
    ANSWER, DECLINE, HANG_UP, MUTE, UNMUTE, SPEAKER, PAUSE, RESUME, STOP, CANCEL,
    SNOOZE, ADD_TIME, DISMISS, REPLY, MARK_READ, OPEN, UNKNOWN
}

/**
 * A4.1: what the resolver may read from one `Notification.Action`. A3.1's snapshot builds these;
 * until then NRS builds them while it maps the actions.
 */
data class ActionSnapshot(
    val title: String,
    /** `Action.getSemanticAction()` (API 28); 0 = none. */
    val semanticAction: Int = 0,
    /** Entry name of the action icon's resource in the posting package ("ic_pause_24"), or null. */
    val iconResName: String? = null,
    /** The action carries a RemoteInput (an inline reply field). */
    val hasRemoteInput: Boolean = false,
    val intent: PendingIntent? = null,
    /** A3.1: the RemoteInput's result key, label and canned choices, for the inline reply. */
    val remoteInputKey: String? = null,
    val remoteInputLabel: String? = null,
    val remoteInputChoices: List<String> = emptyList(),
)
