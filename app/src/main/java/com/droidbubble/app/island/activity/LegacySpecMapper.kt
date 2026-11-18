package com.droidbubble.app.island.activity

import com.droidbubble.app.island.CALL_WIGGLE
import com.droidbubble.app.island.LiveActivityKind
import com.droidbubble.app.island.LiveButtonStyle
import com.droidbubble.app.island.dismissSignature
import com.droidbubble.app.island.icon.IconEffect
import com.droidbubble.app.island.isTimerAlert
import com.droidbubble.app.island.layout.CompactFit
import com.droidbubble.app.island.liveCompactText
import com.droidbubble.app.island.parseLiveScore
import com.droidbubble.app.island.staticLiveTimeText
import com.droidbubble.app.island.trackingChipText
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState

/**
 * A1.2: today's rules, written down as specs. Pure; nothing reads the result until A1.3's store.
 *
 * The slots copy the compact `when`s: leading from `LiveActivityCompactIcon` and the message
 * avatar, trailing from `liveCompactText`, the live timer and the first name, minimal from the
 * split bubble. The tiers are A2.1's table.
 */
object LegacySpecMapper {

    /** Paused media drops out after this long (NotificationReceiverService.STALE_PAUSED_MEDIA_MS). */
    const val STALE_PAUSED_MEDIA_MS = 10 * 60 * 1000L

    /** A notification that is a plain download goes to [fromProgress]; everything else lands here. */
    fun fromNotification(n: NotificationInfo, previous: LiveActivitySpec? = null): LiveActivitySpec {
        if (n.hasProgress && n.kind == LiveActivityKind.NONE) return fromProgress(n, previous)
        val legacy = n.kind
        val kind = activityKind(n, legacy)
        val (leading, trailing, minimal) = compactSlots(n, legacy)
        return LiveActivitySpec(
            id = ActivityId.notification(n.key),
            kind = kind,
            source = ActivitySource.NOTIFICATION,
            packageName = n.packageName,
            tier = tierFor(kind, n),
            bucket = if (kind == ActivityKind.CALL_RINGING || kind == ActivityKind.CALL_ONGOING) OrderBucket.CALL else OrderBucket.NOTIFICATION,
            startedAt = previous?.startedAt ?: n.timestamp,
            lastSignificantAt = significantAt(n, previous),
            alerting = kind == ActivityKind.CALL_RINGING || kind == ActivityKind.ALERT,
            accent = accentFor(legacy),
            compactLeading = leading,
            compactTrailing = trailing,
            minimal = minimal,
            expanded = ExpandedSpec(presentationFor(n, legacy), controls = controls(n)),
            open = OpenTarget(n.contentIntent, n.packageName),
            stateSignature = dismissSignature(n),
            payload = ActivityPayload.Notification(n),
        )
    }

    fun fromProgress(n: NotificationInfo, previous: LiveActivitySpec? = null): LiveActivitySpec {
        val fraction = if (n.isIndeterminate) null else n.progressFraction
        return LiveActivitySpec(
            id = ActivityId.notification(n.key),
            kind = ActivityKind.PROGRESS,
            source = ActivitySource.NOTIFICATION,
            packageName = n.packageName,
            tier = Tier.T2_ONGOING,
            bucket = OrderBucket.PROGRESS,
            startedAt = previous?.startedAt ?: n.timestamp,
            lastSignificantAt = significantAt(n, previous),
            accent = AccentRole.NEUTRAL,
            compactLeading = Slot.Image(ImageRef.AppIcon(fallback = IslandIcon.DOWNLOAD)),
            compactTrailing = Slot.Progress(fraction),
            minimal = Slot.Progress(fraction),
            expanded = ExpandedSpec(ExpandedPresentation.PROGRESS_CARD, controls = controls(n)),
            open = OpenTarget(n.contentIntent, n.packageName),
            // A dismissed download stays gone while it ticks, until the app removes it (as before A1.4)
            stateSignature = 0,
            payload = ActivityPayload.Notification(n),
        )
    }

    fun fromMedia(m: MediaInfo, nowMs: Long, previous: LiveActivitySpec? = null): LiveActivitySpec = LiveActivitySpec(
        id = ActivityId.media(m.packageName),
        kind = ActivityKind.MEDIA,
        source = ActivitySource.MEDIA_SESSION,
        packageName = m.packageName,
        tier = Tier.T2_ONGOING,
        bucket = OrderBucket.MEDIA,
        startedAt = previous?.startedAt ?: nowMs,
        lastSignificantAt = if (previous != null && previous.stateSignature == m.isPlaying.hashCode()) previous.lastSignificantAt else nowMs,
        staleAt = if (m.isPlaying) null else nowMs + STALE_PAUSED_MEDIA_MS,
        accent = AccentRole.MEDIA,
        compactLeading = Slot.Custom(CustomSlot.MEDIA_ART),
        compactTrailing = Slot.Custom(CustomSlot.EQUALIZER),
        minimal = Slot.Custom(CustomSlot.MEDIA_ART),
        expanded = ExpandedSpec(ExpandedPresentation.MEDIA_CARD),
        open = OpenTarget(null, m.packageName),
        // A swiped-away paused session returns only when it plays again (IslandStateManager.updateMedia)
        stateSignature = m.isPlaying.hashCode(),
        payload = ActivityPayload.Media(m),
    )

    fun fromRecording(r: ScreenRecordingState, nowMs: Long, previous: LiveActivitySpec? = null): LiveActivitySpec {
        val base = nowMs - r.durationSeconds * 1000L
        return LiveActivitySpec(
            id = ActivityId.SCREEN_RECORDING,
            kind = ActivityKind.SCREEN_RECORDING,
            source = ActivitySource.SCREEN_RECORDING,
            packageName = null,
            tier = Tier.T1_LIVE,
            bucket = OrderBucket.RECORDING,
            startedAt = previous?.startedAt ?: base,
            lastSignificantAt = if (previous != null && previous.stateSignature == r.isPaused.hashCode()) previous.lastSignificantAt else nowMs,
            accent = AccentRole.RECORDING,
            compactLeading = Slot.RecordingDot,
            compactTrailing = Slot.Timer(base, countDown = false, pausedText = null),
            minimal = Slot.RecordingDot,
            expanded = ExpandedSpec(ExpandedPresentation.RECORDING_ROW),
            open = OpenTarget.None,
            stateSignature = r.isPaused.hashCode(),
            payload = ActivityPayload.ScreenRecording(r),
        )
    }

    fun fromTorch(level: Int, max: Int, nowMs: Long, previous: LiveActivitySpec? = null): LiveActivitySpec = LiveActivitySpec(
        id = ActivityId.TORCH,
        kind = ActivityKind.FLASHLIGHT,
        source = ActivitySource.TORCH,
        packageName = null,
        tier = Tier.T2_ONGOING,
        bucket = OrderBucket.FLASHLIGHT,
        startedAt = previous?.startedAt ?: nowMs,
        lastSignificantAt = previous?.lastSignificantAt ?: nowMs,
        accent = AccentRole.NEUTRAL,
        compactLeading = Slot.Custom(CustomSlot.FLASHLIGHT_GLOW),
        // V3.4: the level on the empty side, or "On" without steps
        compactTrailing = Slot.Custom(CustomSlot.FLASHLIGHT_LEVEL),
        minimal = Slot.Custom(CustomSlot.FLASHLIGHT_GLOW),
        expanded = ExpandedSpec(ExpandedPresentation.FLASHLIGHT_CARD),
        open = OpenTarget.None,
        stateSignature = 0,
        payload = ActivityPayload.Torch(level, max),
    )

    // ---- notification rules ----

    internal fun activityKind(n: NotificationInfo, legacy: LiveActivityKind): ActivityKind = when (legacy) {
        LiveActivityKind.CALL -> if (n.isIncomingCall) ActivityKind.CALL_RINGING else ActivityKind.CALL_ONGOING
        LiveActivityKind.ALERT -> ActivityKind.ALERT
        LiveActivityKind.RECORDING -> ActivityKind.VOICE_RECORDING
        LiveActivityKind.TIMER -> ActivityKind.TIMER
        LiveActivityKind.STOPWATCH -> ActivityKind.STOPWATCH
        LiveActivityKind.ALARM -> ActivityKind.ALARM
        LiveActivityKind.TRACKING -> ActivityKind.TRACKING
        LiveActivityKind.SPORTS -> ActivityKind.SPORTS
        // A timer-looking ongoing notification that isn't a known kind still counts live
        LiveActivityKind.NONE -> ActivityKind.MESSAGE
    }

    /** A2.1's tier table for notification kinds. */
    internal fun tierFor(kind: ActivityKind, n: NotificationInfo): Tier = when (kind) {
        ActivityKind.CALL_RINGING, ActivityKind.ALERT -> Tier.T0_INTERRUPT
        ActivityKind.CALL_ONGOING, ActivityKind.NAVIGATION, ActivityKind.VOICE_RECORDING, ActivityKind.SCREEN_RECORDING,
        ActivityKind.TIMER, ActivityKind.STOPWATCH -> Tier.T1_LIVE
        // A Live Update with an ETA or a chip is live; without one it's ongoing
        ActivityKind.TRACKING -> if (hasEtaOrChip(n)) Tier.T1_LIVE else Tier.T2_ONGOING
        ActivityKind.MESSAGE -> if (n.liveTimerBaseMs > 0L) Tier.T1_LIVE else Tier.T3_BANNER
        else -> Tier.T2_ONGOING
    }

    private fun hasEtaOrChip(n: NotificationInfo): Boolean =
        !n.shortCriticalText.isNullOrBlank() || trackingChipText(n.copy(progress = -1, maxProgress = 0)) != null

    private fun accentFor(kind: LiveActivityKind): AccentRole = when (kind) {
        LiveActivityKind.RECORDING -> AccentRole.RECORDING
        LiveActivityKind.TIMER -> AccentRole.TIMER
        LiveActivityKind.STOPWATCH -> AccentRole.STOPWATCH
        LiveActivityKind.CALL, LiveActivityKind.TRACKING -> AccentRole.SUCCESS
        LiveActivityKind.ALERT, LiveActivityKind.ALARM -> AccentRole.WARNING
        LiveActivityKind.SPORTS, LiveActivityKind.NONE -> AccentRole.NEUTRAL
    }

    private fun presentationFor(n: NotificationInfo, kind: LiveActivityKind): ExpandedPresentation = when (kind) {
        LiveActivityKind.CALL -> ExpandedPresentation.CALL
        LiveActivityKind.ALERT -> ExpandedPresentation.ALERT
        LiveActivityKind.RECORDING -> ExpandedPresentation.RECORDING_ROW
        LiveActivityKind.TIMER, LiveActivityKind.STOPWATCH, LiveActivityKind.ALARM -> ExpandedPresentation.TIMER_ROW
        LiveActivityKind.TRACKING -> ExpandedPresentation.TRACKING_ROW
        LiveActivityKind.SPORTS -> if (parseLiveScore(n) != null) ExpandedPresentation.SCORE_ROW else ExpandedPresentation.TRACKING_ROW
        LiveActivityKind.NONE -> ExpandedPresentation.NOTIFICATION_CARD
    }

    private fun controls(n: NotificationInfo): List<ActivityControl> =
        n.actions.map { ActivityControl(it.role, it.title, it.actionIntent, LiveButtonStyle.GLASS) }

    /** A state change moves it; a timer tick or an ETA tick (same signature) doesn't. */
    private fun significantAt(n: NotificationInfo, previous: LiveActivitySpec?): Long =
        if (previous != null && previous.stateSignature == dismissSignature(n)) previous.lastSignificantAt else n.timestamp

    /** Leading, trailing and minimal, the way the compact `when`s draw them today. */
    internal fun compactSlots(n: NotificationInfo, kind: LiveActivityKind): Triple<Slot, Slot, Slot> {
        val leading: Slot = when (kind) {
            LiveActivityKind.CALL -> Slot.Icon(IslandIcon.CALL, if (n.isIncomingCall) CALL_WIGGLE else null)
            LiveActivityKind.RECORDING -> Slot.Waveform
            LiveActivityKind.TIMER ->
                if (n.liveTimerCountDown || n.liveTimerBaseMs <= 0L) Slot.Ring(n.liveTimerBaseMs, n.key)
                else Slot.ClockFace(n.liveTimerBaseMs, countDown = false)
            LiveActivityKind.STOPWATCH ->
                if (n.liveTimerBaseMs > 0L) Slot.ClockFace(n.liveTimerBaseMs, n.liveTimerCountDown) else Slot.Icon(IslandIcon.TIMER)
            LiveActivityKind.ALERT -> Slot.Icon(if (isTimerAlert(n)) IslandIcon.TIMER else IslandIcon.ALARM, IconEffect.Wiggle())
            LiveActivityKind.ALARM -> Slot.Icon(IslandIcon.ALARM)
            LiveActivityKind.TRACKING -> Slot.Image(ImageRef.Avatar(fallback = IslandIcon.DELIVERY))
            LiveActivityKind.SPORTS -> Slot.Icon(IslandIcon.SPORTS)
            LiveActivityKind.NONE ->
                if (n.liveTimerBaseMs > 0L) Slot.ClockFace(n.liveTimerBaseMs, n.liveTimerCountDown)
                else Slot.Custom(CustomSlot.CONTACT_AVATAR)
        }
        val isTimedKind = kind != LiveActivityKind.NONE && kind != LiveActivityKind.CALL
        val text = liveCompactText(n, kind)
        val trailing: Slot = when {
            text != null -> Slot.Text(text, numeric = true)
            n.liveTimerBaseMs > 0L -> Slot.Timer(n.liveTimerBaseMs, n.liveTimerCountDown, pausedText = null)
            isTimedKind -> staticLiveTimeText(n)?.let { Slot.Text(it, numeric = true) } ?: Slot.Empty
            else -> Slot.Text(CompactFit.firstName(n.title))
        }
        // The bubble draws the recorder as a pulsing mic (slashed while paused), everything else like the pill
        val minimal: Slot = if (kind == LiveActivityKind.RECORDING) Slot.Icon(IslandIcon.MIC, IconEffect.Pulse()) else leading
        return Triple(leading, trailing, minimal)
    }
}
