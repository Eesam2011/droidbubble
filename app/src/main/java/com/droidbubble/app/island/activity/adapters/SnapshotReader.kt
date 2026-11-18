package com.droidbubble.app.island.activity.adapters

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.droidbubble.app.island.activity.controls.ActionSnapshot
import com.droidbubble.app.util.CrashLog

/**
 * A3.1: a posted notification as a [NotificationSnapshot]. Runs on the B10 parser thread. Every
 * field is read in its own `runCatching`: a malformed extra gives its default and a breadcrumb,
 * never a crash or a lost card.
 */
object SnapshotReader {

    private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
    private const val EXTRA_PROGRESS_SEGMENTS = "android.progressSegments"

    /**
     * [lowImportance] comes from the ranking (main thread); [iconName] resolves an action icon's
     * resource entry name (the listener caches it per package and resId).
     */
    fun read(
        sbn: StatusBarNotification,
        lowImportance: Boolean,
        iconName: (Notification.Action) -> String?,
        sdk: Int = Build.VERSION.SDK_INT,
    ): NotificationSnapshot {
        val n = sbn.notification
        val extras: Bundle = n.extras ?: Bundle()
        fun <T> field(name: String, default: T, read: () -> T): T = runCatching(read).getOrElse {
            CrashLog.breadcrumb("snapshot", "$name unreadable: ${it.javaClass.simpleName}")
            default
        }
        fun text(key: String): String? = field(key, null) { extras.getCharSequence(key)?.toString() }
        fun intent(key: String): PendingIntent? = field(key, null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(key, PendingIntent::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(key) as? PendingIntent
            }
        }
        val template = field("template", null) { extras.getString(Notification.EXTRA_TEMPLATE) }
        return NotificationSnapshot(
            key = sbn.key,
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            sdk = sdk,
            whenMs = n.`when`,
            showChronometer = field("showChronometer", false) { extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) },
            chronometerCountDown = field("chronometerCountDown", false) { extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false) },
            category = n.category,
            template = template,
            channelId = field("channelId", null) { n.channelId },
            flags = n.flags,
            sbnOngoing = field("sbnOngoing", false) { sbn.isOngoing },
            lowImportance = lowImportance,
            title = text(Notification.EXTRA_TITLE),
            titleBig = text(Notification.EXTRA_TITLE_BIG),
            text = text(Notification.EXTRA_TEXT),
            bigText = text(Notification.EXTRA_BIG_TEXT),
            subText = text(Notification.EXTRA_SUB_TEXT),
            shortCriticalText = text(EXTRA_SHORT_CRITICAL_TEXT),
            progress = field("progress", -1) { extras.getInt(Notification.EXTRA_PROGRESS, -1) },
            progressMax = field("progressMax", 0) { extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) },
            progressSegmentsMax = if (template?.contains("ProgressStyle") == true) {
                field("progressSegments", 0) {
                    @Suppress("DEPRECATION")
                    val segments = extras.getParcelableArrayList<Bundle>(EXTRA_PROGRESS_SEGMENTS)
                    // Bundle returns null for a value of the wrong type instead of throwing
                    if (segments == null && extras.containsKey(EXTRA_PROGRESS_SEGMENTS)) error("not a list of bundles")
                    segments?.sumOf { it.getInt("length", 0) } ?: 0
                }
            } else 0,
            indeterminate = field("indeterminate", false) { extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) },
            callType = field("callType", 0) { extras.getInt("android.callType", 0) },
            hasFullScreenIntent = n.fullScreenIntent != null,
            contentIntent = n.contentIntent,
            answerIntent = intent("android.answerIntent"),
            declineIntent = intent("android.declineIntent"),
            hangUpIntent = intent("android.hangUpIntent"),
            actions = field("actions", emptyList()) {
                n.actions.orEmpty().filterNotNull().filter { !it.title.isNullOrBlank() }.map { a ->
                    val remoteInput = runCatching { a.remoteInputs?.firstOrNull() }.getOrNull()
                    ActionSnapshot(
                        title = a.title.toString(),
                        semanticAction = runCatching { a.semanticAction }.getOrDefault(0),
                        iconResName = runCatching { iconName(a) }.getOrNull(),
                        hasRemoteInput = remoteInput != null,
                        intent = a.actionIntent,
                        remoteInputKey = remoteInput?.resultKey,
                        remoteInputLabel = remoteInput?.label?.toString(),
                        remoteInputChoices = remoteInput?.choices?.map { it.toString() }.orEmpty(),
                    )
                }
            },
        )
    }
}
