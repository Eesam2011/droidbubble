package com.droidbubble.app.model

/**
 * Which settings toggle lets a notification onto the island (B3.1). "Incoming Notifications"
 * ([IslandSettings.showNotifications]) only governs message banners; calls, live activities and
 * progress each have their own switch.
 */
enum class NotificationGateKind { CALL, LIVE, PROGRESS, MESSAGE }

/**
 * A call wins, then progress, then a live activity, then a plain message.
 *
 * Progress before live: the listener keeps routing a live activity that carries a bar (a ride
 * with a progress bar) through "Live Progress" until Phase 7 A1. The view and the window math
 * pass `hasProgress = false` for the notification slot, because plain downloads already went to
 * the progress card, so there a ride with a bar counts as LIVE.
 */
fun gateKind(isCall: Boolean, isLive: Boolean, hasProgress: Boolean): NotificationGateKind = when {
    isCall -> NotificationGateKind.CALL
    hasProgress -> NotificationGateKind.PROGRESS
    isLive -> NotificationGateKind.LIVE
    else -> NotificationGateKind.MESSAGE
}

fun IslandSettings.allows(kind: NotificationGateKind): Boolean = when (kind) {
    NotificationGateKind.CALL -> showCalls
    NotificationGateKind.LIVE -> showLiveActivities
    NotificationGateKind.PROGRESS -> showLiveProgress
    NotificationGateKind.MESSAGE -> showNotifications
}
