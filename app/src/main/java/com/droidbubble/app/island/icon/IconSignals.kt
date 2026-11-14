package com.droidbubble.app.island.icon

import androidx.compose.runtime.Immutable
import com.droidbubble.app.model.NotificationInfo

/**
 * Event counters for the island's discrete icon effects (I3.1). Each counter goes up by one per
 * real event, so a view can use it as an effect trigger: a changed value fires once, an unchanged
 * value never does, and the first value it sees never fires (an icon that starts in a state
 * doesn't animate).
 */
@Immutable
data class IconSignals(
    /** A regular (not live) notification from the same app as the one showing: a new one, or the same one with new text. */
    val sameAppNudge: Int = 0,
    /** An incoming call started ringing. */
    val ringStart: Int = 0,
    /** An alarm or an expired timer ("Time's up") started ringing. */
    val alertStart: Int = 0,
    /** A Bluetooth or Wi-Fi connection glance was shown. */
    val connectionAppear: Int = 0,
    /** A charging glance was shown. */
    val chargeStart: Int = 0,
)

/**
 * True when [next] is "the same app again": [previous] is showing, both come from one package,
 * and [next] is a new notification or the same one with new text. Live activities never count:
 * timers and recorders repost every second, which would bounce on every tick.
 */
fun isSameAppNudge(previous: NotificationInfo?, next: NotificationInfo?): Boolean =
    previous != null && next != null &&
        !next.isLiveActivity && !previous.isLiveActivity &&
        previous.packageName == next.packageName &&
        (previous.key != next.key || previous.text != next.text || previous.title != next.title)
