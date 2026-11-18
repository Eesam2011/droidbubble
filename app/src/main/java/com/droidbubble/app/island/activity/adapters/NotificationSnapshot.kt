package com.droidbubble.app.island.activity.adapters

import android.app.PendingIntent
import android.graphics.Bitmap
import com.droidbubble.app.island.activity.controls.ActionSnapshot

/**
 * A3.1: everything the adapters may read from one posted notification, as plain data. Built by
 * [SnapshotReader] on the B10 parser thread, each field in its own `runCatching` (null or the
 * default when it can't be read). Adapters are pure functions of this.
 *
 * [title] and [text] are the raw `EXTRA_TITLE` / `EXTRA_TEXT` (not blank-filtered).
 *
 * The expensive parts are read on demand through [AdapterContext]: a custom view's Chronometer
 * (inflation) only when ChronometerReader asks for it, and [SnapshotDetails] (the app label,
 * bitmaps, the CallStyle person) only for a notification an adapter keeps.
 */
data class NotificationSnapshot(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val sdk: Int,
    val whenMs: Long,
    val showChronometer: Boolean,
    val chronometerCountDown: Boolean,
    val category: String?,
    val template: String?,
    val channelId: String?,
    val flags: Int,
    /** The sbn says ongoing (`StatusBarNotification.isOngoing`), besides FLAG_ONGOING_EVENT. */
    val sbnOngoing: Boolean,
    /** The channel's importance is LOW or below (silent). */
    val lowImportance: Boolean,
    val title: String?,
    val titleBig: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    /** `android.shortCriticalText` [M]. */
    val shortCriticalText: String?,
    val progress: Int,
    val progressMax: Int,
    /** Android 16 ProgressStyle: the sum of the segment lengths [M]; 0 when absent. */
    val progressSegmentsMax: Int,
    val indeterminate: Boolean,
    /** CallStyle `android.callType`: 0 unknown, 1 incoming, 2 ongoing, 3 screening. */
    val callType: Int,
    val hasFullScreenIntent: Boolean,
    val contentIntent: PendingIntent?,
    /** CallStyle's own buttons (`android.answerIntent` …), not in [actions]. */
    val answerIntent: PendingIntent?,
    val declineIntent: PendingIntent?,
    val hangUpIntent: PendingIntent?,
    /** Every action with a title, in order. */
    val actions: List<ActionSnapshot>,
)

/** A3.1: the parts read only for a notification an adapter keeps. */
data class SnapshotDetails(
    val appName: String,
    val iconBitmap: Bitmap?,
    val avatarBitmap: Bitmap?,
    /** CallStyle `android.callPerson`. */
    val callPersonName: String?,
    val callPersonUri: String?,
)
