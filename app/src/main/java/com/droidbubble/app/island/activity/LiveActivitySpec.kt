package com.droidbubble.app.island.activity

import android.app.PendingIntent
import com.droidbubble.app.island.LiveButtonStyle
import com.droidbubble.app.island.activity.controls.ControlRole
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState

/**
 * A1.2: one immutable description of any activity the island can show. The ids are
 * "n:<sbn.key>", "media:<pkg>", "rec:screen", "torch", "sys:<glance>", "bt:<addr>".
 */
@JvmInline value class ActivityId(val value: String) {
    companion object {
        fun notification(key: String) = ActivityId("n:$key")
        fun media(packageName: String) = ActivityId("media:$packageName")
        val SCREEN_RECORDING = ActivityId("rec:screen")
        val TORCH = ActivityId("torch")
    }
}

/** A2.1's tiers: T0 interrupts, T1 live, T2 ongoing, T3 banner, T4 glance overlay, T5 ambient. */
enum class Tier { T0_INTERRUPT, T1_LIVE, T2_ONGOING, T3_BANNER, T4_GLANCE, T5_AMBIENT }

enum class ActivitySource { NOTIFICATION, MEDIA_SESSION, SCREEN_RECORDING, TORCH, SYSTEM, WIDGET, PINNED, TEST }

enum class ActivityKind {
    CALL_RINGING, CALL_ONGOING, ALERT, NAVIGATION, VOICE_RECORDING, SCREEN_RECORDING, TIMER, STOPWATCH, ALARM,
    TRACKING, SPORTS, MEDIA, PROGRESS, FLASHLIGHT, VPN, PRIVACY, PINNED, MESSAGE, GLANCE, WIDGET
}

/**
 * The user's drag-order bucket (`settings.featurePriorities`). The first six are today's stored
 * keys; A2.2 adds LIVE and SYSTEM.
 */
enum class OrderBucket(val storedKey: String) {
    CALL("CALL"), MEDIA("MEDIA"), PROGRESS("PROGRESS"), RECORDING("RECORDING"),
    NOTIFICATION("NOTIFICATION"), FLASHLIGHT("FLASHLIGHT"), LIVE("LIVE"), SYSTEM("SYSTEM");

    companion object {
        fun fromStored(key: String): OrderBucket? = entries.firstOrNull { it.storedKey == key }
    }
}

/**
 * The F2 / V5.1 colour role an activity's accent comes from, never a Color literal. Presentation
 * maps it: RECORDING → IslandTokens.Recording, TIMER → Timer, STOPWATCH → Stopwatch,
 * SUCCESS → Success, WARNING → Warning, INFO → Info, NEUTRAL → TextBody, MEDIA → the album accent.
 */
enum class AccentRole { RECORDING, TIMER, STOPWATCH, SUCCESS, WARNING, INFO, NEUTRAL, MEDIA }

/** Where tapping the activity goes: the notification's own intent first, the app's launcher second (B6). */
data class OpenTarget(val contentIntent: PendingIntent?, val packageName: String?) {
    companion object {
        val None = OpenTarget(null, null)
    }
}

/** A5: lock-screen visibility. Filled by A5.1; public until then. */
data class PrivacySpec(
    val visibility: Int = VISIBILITY_PUBLIC,
    val publicTitle: String? = null,
    val publicText: String? = null,
) {
    companion object {
        const val VISIBILITY_PUBLIC = 1
        const val VISIBILITY_PRIVATE = 0
        const val VISIBILITY_SECRET = -1
        val Public = PrivacySpec()
    }
}

enum class ExpandedPresentation {
    CALL, ALERT, TIMER_ROW, RECORDING_ROW, TRACKING_ROW, NAVIGATION_ROW, SCORE_ROW,
    PROGRESS_CARD, NOTIFICATION_CARD, MEDIA_CARD, FLASHLIGHT_CARD, NONE
}

data class ActivityControl(
    val role: ControlRole,
    val label: String,
    val intent: PendingIntent?,
    val weight: LiveButtonStyle,
)

data class ExpandedSpec(
    val presentation: ExpandedPresentation,
    val leading: Slot = Slot.Empty,
    val trailing: Slot = Slot.Empty,
    val center: Slot = Slot.Empty,
    /** A3.5's journey bar. */
    val bottom: Slot = Slot.Empty,
    val controls: List<ActivityControl> = emptyList(),
)

/** A7.1 fills this in; until then a glance is only its kind. */
data class GlanceInfo(val kind: String, val text: String? = null)

/** Back-reference for the bespoke renderers while the presentations migrate (A1.5–A1.8). */
sealed interface ActivityPayload {
    data class Notification(val info: NotificationInfo) : ActivityPayload
    data class Media(val info: MediaInfo) : ActivityPayload
    data class ScreenRecording(val state: ScreenRecordingState) : ActivityPayload
    data class Torch(val level: Int, val max: Int) : ActivityPayload
    data class System(val glance: GlanceInfo) : ActivityPayload
}

data class LiveActivitySpec(
    val id: ActivityId,
    val kind: ActivityKind,
    val source: ActivitySource,
    /** Null for system sources. */
    val packageName: String?,
    /** The adapter's default; A2.5 may override it per app. */
    val tier: Tier,
    val bucket: OrderBucket,
    /** Tiebreak inside a tier, 0..1. */
    val relevance: Float = 0f,
    /** First seen, wall ms. The store keeps the first value across updates. */
    val startedAt: Long,
    /** Last change of state, never a clock tick. */
    val lastSignificantAt: Long,
    /** For example paused media + 10 min; null = never. */
    val staleAt: Long? = null,
    /** Glances (A7.1). */
    val expiresAt: Long? = null,
    /** This update may peek-expand (T0 always does). */
    val alerting: Boolean = false,
    val accent: AccentRole,
    val compactLeading: Slot,
    val compactTrailing: Slot,
    /** The split bubble. */
    val minimal: Slot,
    val expanded: ExpandedSpec,
    val open: OpenTarget,
    val privacy: PrivacySpec = PrivacySpec.Public,
    /** What must change for a dismissed activity to come back (B4.5's dismissSignature for notifications). */
    val stateSignature: Int,
    val payload: ActivityPayload,
)
