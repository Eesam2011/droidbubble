package com.droidbubble.app.island.activity.adapters

import android.app.Notification
import android.app.PendingIntent
import com.droidbubble.app.island.activity.LegacySpecMapper
import com.droidbubble.app.island.activity.controls.ControlResolver
import com.droidbubble.app.island.activity.controls.ResolveContext
import com.droidbubble.app.island.isClockOrRecorderPackage
import com.droidbubble.app.island.isClockPackage
import com.droidbubble.app.island.isRecorderPackage
import com.droidbubble.app.island.isSportsPackage
import com.droidbubble.app.island.isTrackingPackage
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.allows
import com.droidbubble.app.model.gateKind
import com.droidbubble.app.model.parseTimerTextMillis
import com.droidbubble.app.service.hasWord

/**
 * A3.1: today's notification rules (the listener's old buildNotificationInfo plus
 * liveActivityKind), moved verbatim onto a [NotificationSnapshot]. Always last in the chain.
 */
object LegacyAdapter : NotificationAdapter {

    override val name = "legacy"

    // Notification.FLAG_PROMOTED_ONGOING (Android 16), as a literal so it compiles on every level
    const val FLAG_PROMOTED_ONGOING = 0x00040000

    override fun accepts(n: NotificationSnapshot) = true

    override fun adapt(n: NotificationSnapshot, ctx: AdapterContext): AdapterOutcome {
        val packageName = n.packageName
        val settings = ctx.settings
        // Live progress tracking (Google Play Store, downloads, transfers, navigation)
        val template = n.template.orEmpty()
        val isProgressStyle = template.contains("ProgressStyle")
        val progress = n.progress
        // Android 16 ProgressStyle: the max is the sum of the segment lengths
        val maxProgress = n.progressMax.takeIf { it > 0 } ?: if (isProgressStyle) n.progressSegmentsMax else 0
        val isIndeterminate = n.indeterminate
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0 || n.sbnOngoing
        // Android 16 "Live Updates": promoted ongoing notifications (rides, deliveries, navigation...)
        val isPromoted = (n.sdk >= 36 && (n.flags and FLAG_PROMOTED_ONGOING) != 0) || (isProgressStyle && isOngoing)
        val shortCriticalText = n.shortCriticalText?.takeIf { it.isNotBlank() }
        val rawWords = "${n.title} ${n.text}".lowercase()
        // Ringing alarm or expired timer: the clock app posts it as an alarm with a full-screen intent
        val isAlert = isClockPackage(packageName) && (
            (n.category == Notification.CATEGORY_ALARM && n.hasFullScreenIntent) ||
                "time's up" in rawWords || "time’s up" in rawWords || "time is up" in rawWords
            )
        // Android 14 foreground-service notifications can lack FLAG_ONGOING_EVENT, so check both.
        val isForegroundServiceNotif = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        val ongoingLike = isOngoing || isForegroundServiceNotif
        // B4.2: a ride, sports or recorder app only counts when its notification is ongoing; an
        // upcoming alarm isn't always ongoing, so the clock clause stays as it is.
        val isLiveCandidate = isAlert || isPromoted ||
            (ongoingLike && (isTrackingPackage(packageName) || isSportsPackage(packageName) || isRecorderPackage(packageName))) ||
            (isClockPackage(packageName) && ("alarm" in rawWords || "snooz" in rawWords))
        val isNavCategory = n.category == Notification.CATEGORY_NAVIGATION
        val isNavApp = packageName == "com.google.android.apps.maps" || packageName == "com.waze" || packageName.contains("maps")
        val isNavigation = (isNavCategory || isNavApp) && isOngoing
        val hasProgress = maxProgress > 0 || isIndeterminate || (progress >= 0 && maxProgress > 0) || isNavigation

        val callType = n.callType
        val isCall = n.category == Notification.CATEGORY_CALL || template.contains("CallStyle")

        // Persistent notifications ("App is running", VPN...) are skipped unless they are live
        // activities. Clock apps draw the countdown in a custom view (A3.7's gate).
        val customTimer = if (ChronometerReader.wantsCustomViewTimer(
                ongoingOrForeground = ongoingLike,
                hasProgress = hasProgress,
                isCall = isCall,
                showsChronometer = n.showChronometer,
                clockOrRecorderPackage = isClockOrRecorderPackage(packageName),
                category = n.category,
            )
        ) ctx.customViewTimer() else null
        val looksLikeTimer = n.showChronometer || parseTimerTextMillis(n.title) != null || customTimer != null
        // B3.1: each kind has its own toggle
        if (!settings.allows(gateKind(isCall = isCall, isLive = looksLikeTimer || isLiveCandidate, hasProgress = hasProgress))) {
            return AdapterOutcome.Drop
        }
        if (ongoingLike && !hasProgress && !isCall && !looksLikeTimer && !isLiveCandidate) return AdapterOutcome.Drop
        // Silent / low-importance channels don't pop the island (calls and live activities always do)
        if (!isCall && !looksLikeTimer && !hasProgress && !isLiveCandidate && n.lowImportance) return AdapterOutcome.Drop

        val title = n.title?.takeIf { it.isNotBlank() } ?: n.titleBig?.takeIf { it.isNotBlank() } ?: customTimer?.label
        val text = n.text?.takeIf { it.isNotBlank() } ?: n.bigText?.takeIf { it.isNotBlank() }
            ?: n.subText?.takeIf { it.isNotBlank() } ?: customTimer?.timeText
        if (title.isNullOrBlank() && text.isNullOrBlank()) return AdapterOutcome.Drop
        // Not from the app on screen, except calls and live activities (see the listener's notes:
        // a countdown is never reposted, so dropping its first post would lose it)
        if (!hasProgress && !isCall && !looksLikeTimer && !isLiveCandidate && packageName == ctx.foregroundPackage) {
            return AdapterOutcome.Ignore
        }

        val details = ctx.details(isCall)

        // Answer / decline: CallStyle extras first, then the notification's own buttons
        fun actionIntent(vararg words: String): PendingIntent? =
            n.actions.firstOrNull { it.title.hasWord(*words) }?.intent
        val answerIntent = if (isCall) n.answerIntent ?: actionIntent("answer", "accept") else null
        val declineIntent = if (isCall) {
            n.declineIntent ?: n.hangUpIntent ?: actionIntent("decline", "reject", "hang up", "end")
        } else null
        val isIncomingCall = isCall && (callType == 1 || (callType == 0 && answerIntent != null))
        val callerNumber = details.callPersonUri?.takeIf { it.startsWith("tel:") }?.removePrefix("tel:")
            ?: n.text?.takeIf { t -> t.any { it.isDigit() } && t.count { it.isDigit() } >= 5 }

        // Live timers: a chronometer from `when`, a custom view, or a static "00:00" title (A3.7)
        val timer = ChronometerReader.read(
            ChronometerReader.Inputs(
                showsChronometer = n.showChronometer,
                chronometerCountDown = n.chronometerCountDown,
                whenMs = n.whenMs,
                postTime = n.postTime,
                title = title,
                text = text,
                ongoingOrForeground = ongoingLike,
                customTimer = customTimer,
            )
        )
        // Same start time on every update, so the island clock never steps back or stalls
        val liveTimerBaseMs = ctx.stabilizeBase("${n.key}|${timer.countDown}", timer.rawBaseMs, timer.baseFromText)

        // A4.1: each button's role, from CallStyle intents, the semantic action and the icon name
        // before its (language-dependent) title
        val resolveContext = ResolveContext(
            isCall = isCall,
            answer = if (isCall) n.answerIntent else null,
            decline = if (isCall) n.declineIntent else null,
            hangUp = if (isCall) n.hangUpIntent else null,
        )
        val actions = n.actions.filter { it.title.isNotBlank() }.map { a ->
            NotificationAction(
                title = a.title,
                actionIntent = a.intent,
                remoteInputKey = a.remoteInputKey,
                remoteInputLabel = a.remoteInputLabel,
                remoteInputChoices = a.remoteInputChoices,
                semanticAction = a.semanticAction,
                iconResName = a.iconResName,
                role = ControlResolver.resolve(a, resolveContext),
            )
        }

        val info = NotificationInfo(
            key = n.key,
            packageName = packageName,
            appName = details.appName,
            title = details.callPersonName?.takeIf { it.isNotBlank() } ?: title ?: details.appName,
            text = text ?: "",
            iconBitmap = details.iconBitmap,
            avatarBitmap = details.avatarBitmap,
            contentIntent = n.contentIntent,
            actions = actions.take(3),
            timestamp = n.postTime,
            progress = progress,
            maxProgress = maxProgress,
            isIndeterminate = isIndeterminate || (isNavigation && maxProgress <= 0),
            // Android 14 lets foreground-service notifications be swiped, dropping the ongoing
            // flag; they are still live activities (recorders, timers)
            isOngoing = ongoingLike,
            liveTimerBaseMs = liveTimerBaseMs,
            liveTimerCountDown = timer.countDown,
            isAutoCancel = (n.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            isCall = isCall,
            isIncomingCall = isIncomingCall,
            callerNumber = callerNumber,
            answerIntent = answerIntent,
            declineIntent = declineIntent,
            category = n.category,
            isAlert = isAlert,
            isPromoted = isPromoted,
            shortCriticalText = shortCriticalText,
        )
        // A1.1: classify here, on the parser thread (B10), so the regexes never run on main
        info.kind
        return AdapterOutcome.Post(info, hasProgress, LegacySpecMapper.fromNotification(info, ctx.previous))
    }
}
