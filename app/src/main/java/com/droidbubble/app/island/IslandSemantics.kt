package com.droidbubble.app.island

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlinx.coroutines.delay

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
fun Modifier.islandNodeSemantics(speech: NodeSpeech?): Modifier = when {
    speech == null -> clearAndSetSemantics {}
    speech.subject.isGlance -> clearAndSetSemantics {
        contentDescription = speech.headline
        speech.value?.let { stateDescription = it }
    }
    else -> semantics {
        contentDescription = speech.headline
        speech.value?.let { stateDescription = it }
    }
}
