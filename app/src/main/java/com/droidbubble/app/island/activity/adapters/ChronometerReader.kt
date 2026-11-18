package com.droidbubble.app.island.activity.adapters

import com.droidbubble.app.model.parseTimerTextMillis

/**
 * A3.7: the timer-base rules, moved out of NotificationReceiverService so they are pure and
 * testable. NRS still reads the Android bits (extras, the inflated custom view) and passes them in;
 * A3.1's ChronometerAdapter will call the same functions from a NotificationSnapshot.
 *
 * Nothing here changes behaviour: these are the rules NRS used, verbatim, plus [isOvertime].
 */
object ChronometerReader {

    /** `Notification.CATEGORY_STOPWATCH` (API 31), as a string so it reads on every level. */
    const val CATEGORY_STOPWATCH = "stopwatch"
    const val CATEGORY_ALARM = "alarm"

    /**
     * Whether to inflate the notification's custom view to look for a Chronometer. Clock apps
     * (AOSP / Google Clock) draw the countdown in a custom view, so the notification has no title,
     * text or chronometer extras at all.
     */
    fun wantsCustomViewTimer(
        ongoingOrForeground: Boolean,
        hasProgress: Boolean,
        isCall: Boolean,
        showsChronometer: Boolean,
        clockOrRecorderPackage: Boolean,
        category: String?,
    ): Boolean = ongoingOrForeground && !hasProgress && !isCall && !showsChronometer &&
        (clockOrRecorderPackage || category == CATEGORY_ALARM || category == CATEGORY_STOPWATCH)

    /** A timer read from a notification's custom RemoteViews. */
    data class CustomViewTimer(
        val baseWallMs: Long,
        val countDown: Boolean,
        val running: Boolean,
        val timeText: String?,
        val label: String?,
    )

    /**
     * What an inflated custom view's Chronometer says. [chronoBaseElapsed] is
     * `Chronometer.getBase()` on the elapsedRealtime clock; [labels] are the visible TextViews.
     * Chronometer exposes no "started" flag: a running timer offers Pause, a paused one Resume / Start.
     */
    fun fromCustomView(
        chronoBaseElapsed: Long,
        elapsedNow: Long,
        wallNow: Long,
        isCountDown: Boolean,
        chronoText: String?,
        labels: List<String>,
        actionTitles: List<String>,
    ): CustomViewTimer {
        val baseWall = wallNow + (chronoBaseElapsed - elapsedNow)
        val timeText = chronoText?.replace("−", "")?.replace("-", "")?.trim()?.takeIf { it.isNotEmpty() }
        val titles = actionTitles.map { it.lowercase() }
        val offersPause = titles.any { "pause" in it || "stop" in it && "stopwatch" !in it }
        val offersResume = titles.any { "resume" in it || "start" in it || "continue" in it }
        val saysPaused = labels.any { it.contains("paused", ignoreCase = true) }
        val running = !saysPaused && (offersPause || !offersResume)
        val label = labels.firstOrNull { parseTimerTextMillis(it) == null }
            ?: if (isCountDown) "Timer" else "Stopwatch"
        return CustomViewTimer(baseWall, isCountDown, running, timeText, label)
    }

    /** The parts of a notification the base is worked out from. */
    data class Inputs(
        val showsChronometer: Boolean,
        val chronometerCountDown: Boolean,
        val whenMs: Long,
        val postTime: Long,
        val title: String?,
        val text: String?,
        val ongoingOrForeground: Boolean,
        val customTimer: CustomViewTimer?,
    )

    /** The raw base, before LiveTimerBaseStabilizer, and whether it came from static text. */
    data class Reading(val rawBaseMs: Long, val countDown: Boolean, val baseFromText: Boolean, val paused: Boolean)

    fun read(i: Inputs): Reading {
        val timerTitleMs = parseTimerTextMillis(i.title)
        val paused = i.text?.contains("pause", ignoreCase = true) == true
        val chronometer = i.showsChronometer && i.whenMs > 0L
        val custom = i.customTimer
        val raw = when {
            chronometer -> i.whenMs
            custom != null -> if (custom.running) custom.baseWallMs else 0L
            timerTitleMs != null && !paused && i.ongoingOrForeground -> i.postTime - timerTitleMs
            else -> 0L
        }
        // A paused countdown keeps the flag so it still gets the timer design, not the stopwatch one
        val countDown = (i.showsChronometer && i.chronometerCountDown) || custom?.countDown == true
        return Reading(raw, countDown, baseFromText = !chronometer && custom == null, paused = paused)
    }

    /** A countdown whose end has passed while the app still posts it ("Time's up" overtime). */
    fun isOvertime(baseMs: Long, countDown: Boolean, nowMs: Long): Boolean =
        countDown && baseMs > 0L && nowMs > baseMs
}
