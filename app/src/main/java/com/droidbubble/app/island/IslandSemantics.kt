package com.droidbubble.app.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * C3.4: what one island node (the pill or the bubble) says to TalkBack. [headline] is the
 * node's `contentDescription` and changes only on real events; [value] is its
 * `stateDescription` and may tick. Null labels mean "no such action".
 */
data class NodeSpeech(
    val subject: SpeechSubject,
    val headline: String,
    val value: String?,
    val clickLabel: String?,
    val longClickLabel: String?,
)

/** The glance on the pill, if one is showing (charging, unlock, a connection, the ringer). */
fun glanceSubject(
    isCharging: Boolean,
    chargingPercent: Int?,
    isUnlock: Boolean,
    isConnection: Boolean,
    connection: IslandStateManager.ConnectionAlert,
    isRinger: Boolean,
    ringer: IslandStateManager.RingerMode,
    connectionNames: (IslandStateManager.ConnectionAlert) -> String,
): SpeechSubject? = when {
    isUnlock -> SpeechSubject.Unlocked
    isConnection -> SpeechSubject.Connected(connectionNames(connection))
    isRinger -> SpeechSubject.Ringer(ringer)
    isCharging -> SpeechSubject.Charging(chargingPercent)
    else -> null
}

/** What the activity in [priority] shows, as a [SpeechSubject]; null for none (idle). */
fun activitySubject(
    priority: IslandPriority?,
    media: MediaInfo?,
    notification: NotificationInfo?,
    progress: NotificationInfo?,
    recording: ScreenRecordingState,
    flashlightPercent: Int,
    nowMs: Long,
): SpeechSubject? = when (priority) {
    IslandPriority.MEDIA -> media?.let { SpeechSubject.Media(it.title, it.artist, it.isPlaying) }
    IslandPriority.CALL, IslandPriority.POP_NOTIFICATION -> notification?.let {
        IslandSpeech.subjectFor(it, nowMs, paused = liveActivityControls(it).isPaused)
    }
    IslandPriority.PROGRESS -> progress?.let {
        val s = IslandSpeech.subjectFor(it, nowMs, paused = liveActivityControls(it).isPaused)
        // A plain download has no live kind: it reads as a download, not a message
        if (s is SpeechSubject.Message) SpeechSubject.Download(it.title, it.downloadPercent()) else s
    }
    IslandPriority.SCREEN_RECORDING -> SpeechSubject.ScreenRecording(recording.durationSeconds * 1000L, recording.isPaused)
    IslandPriority.FLASHLIGHT -> SpeechSubject.Flashlight(flashlightPercent)
    IslandPriority.IDLE, null -> null
}

private fun NotificationInfo.downloadPercent(): Int? =
    if (maxProgress > 0 && progress >= 0 && !isIndeterminate) (progressFraction * 100).toInt() else null

/**
 * The long-press target of [priority]: the same rule as `openAppForPriority` (only messages,
 * downloads and media open something). Messages with their own tap target open the conversation (B6.3).
 */
fun longClickTarget(priority: IslandPriority?, subject: SpeechSubject?, notification: NotificationInfo?): OpenTarget? = when (priority) {
    IslandPriority.POP_NOTIFICATION ->
        if (subject is SpeechSubject.Message && notification?.contentIntent != null) OpenTarget.CONVERSATION else OpenTarget.APP
    IslandPriority.PROGRESS, IslandPriority.MEDIA -> OpenTarget.APP
    else -> null
}

/**
 * Builds the [NodeSpeech] for one node. [glance] wins over the activity; a minimised island reads
 * as the ring. Pure, so the wiring is testable without a screen.
 */
fun nodeSpeech(
    speech: IslandSpeech,
    activity: SpeechSubject?,
    glance: SpeechSubject?,
    minimised: Boolean,
    appName: String?,
    target: OpenTarget?,
): NodeSpeech? {
    val subject = glance ?: activity?.let { if (minimised) SpeechSubject.Minimised(it) else it } ?: return null
    return NodeSpeech(
        subject = subject,
        headline = speech.headline(subject),
        value = speech.value(subject),
        clickLabel = speech.clickLabel(subject),
        longClickLabel = if (glance != null || minimised || target == null || appName.isNullOrBlank()) null
            else speech.longClickLabel(appName, target),
    )
}

/**
 * C3.4: a clock for spoken values that tick (a timer's remaining time). It only runs while a
 * screen reader is on, so without one the island doesn't recompose for it at all.
 */
@Composable
fun rememberSpeechClock(): Long {
    val readerOn by IslandA11y.screenReaderOn.collectAsState()
    val now = remember { mutableLongStateOf(IslandClock.wallMs()) }
    LaunchedEffect(readerOn) {
        while (readerOn) {
            now.longValue = IslandClock.wallMs()
            delay(1_000L)
        }
    }
    return if (readerOn) now.longValue else 0L
}

/** The app's label for [packageName] ("Spotify"), remembered per package. */
@Composable
fun rememberAppLabel(packageName: String?): String? {
    val context = LocalContext.current
    return remember(packageName) {
        packageName?.let { pkg ->
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull()
        }
    }
}

/**
 * C3.4: the node's spoken summary. An idle node ([speech] null) and a glance are cleared, so TalkBack
 * skips the idle pill and a glance reads its headline and value only, with no tap action. Other
 * nodes keep their click actions; their labels come through `combinedClickable`.
 */
fun Modifier.islandNodeSemantics(speech: NodeSpeech?, actions: List<CustomAccessibilityAction> = emptyList()): Modifier = when {
    speech == null -> clearAndSetSemantics {}
    speech.subject.isGlance -> clearAndSetSemantics {
        contentDescription = speech.headline
        speech.value?.let { stateDescription = it }
    }
    else -> semantics {
        contentDescription = speech.headline
        speech.value?.let { stateDescription = it }
        if (actions.isNotEmpty()) customActions = actions
    }
}

/** C3.7: how long the pill stays a live region after a real event. */
const val LIVE_REGION_WINDOW_MS = 4_000L

/**
 * C3.7: the pill's live region. TalkBack hears an event once: the node becomes a polite live
 * region when its [announcement] changes, and stops being one [LIVE_REGION_WINDOW_MS] later, so a
 * ticking value (the timer's seconds in stateDescription) is never announced. None while
 * expanded, for assertive events (the card's pane title carries those), and for events
 * [IslandSpeech.announcement] keeps quiet (media, unlock, the minimised ring).
 *
 * A modifier node, so the window opening and closing updates semantics only and never
 * recomposes the island. Put it before [islandNodeSemantics], which may clear what follows it.
 */
fun Modifier.islandLiveRegion(announcement: Announcement?, isExpanded: Boolean): Modifier =
    this then LiveRegionElement(announcement, isExpanded)

private data class LiveRegionElement(val announcement: Announcement?, val isExpanded: Boolean) :
    ModifierNodeElement<LiveRegionNode>() {
    override fun create() = LiveRegionNode(announcement, isExpanded)
    override fun update(node: LiveRegionNode) = node.update(announcement, isExpanded)
}

private class LiveRegionNode(
    private var announcement: Announcement?,
    private var isExpanded: Boolean,
) : Modifier.Node(), SemanticsModifierNode {
    private var fresh = false
    private var window: Job? = null

    override fun onAttach() = openWindow()

    override fun onDetach() {
        window = null
        fresh = false
    }

    fun update(announcement: Announcement?, isExpanded: Boolean) {
        val changed = announcement?.text != this.announcement?.text
        this.announcement = announcement
        this.isExpanded = isExpanded
        if (changed && isAttached) openWindow() else invalidateSemantics()
    }

    private fun openWindow() {
        window?.cancel()
        fresh = announcement != null
        invalidateSemantics()
        if (fresh) {
            window = coroutineScope.launch {
                delay(LIVE_REGION_WINDOW_MS)
                fresh = false
                invalidateSemantics()
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        liveRegionFor(announcement, fresh, isExpanded)?.let { liveRegion = it }
    }
}

/** Pure part of [islandLiveRegion]. */
fun liveRegionFor(announcement: Announcement?, fresh: Boolean, isExpanded: Boolean): LiveRegionMode? = when {
    announcement == null || !fresh || isExpanded -> null
    announcement.politeness == Politeness.ASSERTIVE -> null
    else -> LiveRegionMode.Polite
}

/**
 * C3.5: what the card's sideways swipe does for [priority], as a function; null when the swipe
 * doesn't dismiss (a ringing call or alarm tucks instead, B6.2; calls, recording and the
 * flashlight have their own buttons).
 */
fun dismissFor(priority: IslandPriority?, notification: NotificationInfo?): (() -> Unit)? = when (priority) {
    IslandPriority.MEDIA -> { { IslandStateManager.dismissMedia() } }
    IslandPriority.POP_NOTIFICATION ->
        if (notification?.isIncomingCall == true || notification?.isAlert == true) null
        else { { IslandStateManager.clearCurrentNotification() } }
    IslandPriority.PROGRESS -> { { IslandStateManager.dismissProgress() } }
    else -> null
}

/**
 * C3.5: TalkBack's Actions menu for [node]: every swipe-only action gets a labelled alternative
 * with the same effect. The list (and its exclusions) comes from [IslandSpeech.availableActions].
 */
fun islandCustomActions(
    speech: IslandSpeech,
    node: SpeechNode,
    nodeSpeech: NodeSpeech?,
    priority: IslandPriority?,
    notification: NotificationInfo?,
): List<CustomAccessibilityAction> {
    val subject = nodeSpeech?.subject ?: return emptyList()
    val dismiss = dismissFor(priority, notification)
    return speech.availableActions(node, subject).mapNotNull { action ->
        val run: () -> Unit = when (action) {
            IslandA11yAction.MINIMISE -> IslandStateManager::minimizeIsland
            IslandA11yAction.HIDE -> IslandStateManager::hideIsland
            IslandA11yAction.DISMISS -> dismiss ?: return@mapNotNull null
            IslandA11yAction.TURN_OFF_FLASHLIGHT -> IslandStateManager::turnOffFlashlight
            IslandA11yAction.COLLAPSE -> IslandStateManager::collapse
            IslandA11yAction.PAUSE_ISLAND -> return@mapNotNull null // C4.1
        }
        CustomAccessibilityAction(speech.actionLabel(action)) { run(); true }
    }
}

/**
 * C3.6: false inside the pill's and the bubble's content. Their node already reads one summary
 * (C3.4), so numbers drawn inside them ([RollingNumberText]) stay silent instead of being read a
 * second time. The open card's numbers are read (as durations, for clocks).
 */
val LocalNumbersSpoken = androidx.compose.runtime.staticCompositionLocalOf { true }
