package com.droidbubble.app.service

import android.view.WindowManager
import com.droidbubble.app.model.IslandSettings
import kotlin.math.abs

/**
 * Everything the overlay window's size and touchability depend on. The service fills it from
 * [com.droidbubble.app.island.IslandStateManager] and its current settings. The activity flags
 * are the raw state (is there media, a notification...), before any `show*` setting is applied.
 */
data class WindowInputs(
    val settings: IslandSettings,
    val expanded: Boolean = false,
    val landscape: Boolean = false,
    val locked: Boolean = false,
    val call: Boolean = false,
    val media: Boolean = false,
    val notification: Boolean = false,
    val progress: Boolean = false,
    val recording: Boolean = false,
    val flashlight: Boolean = false,
    val charging: Boolean = false,
    val unlock: Boolean = false,
    val connection: Boolean = false,
    val ringer: Boolean = false,
    val userHidden: Boolean = false,
    val userMinimized: Boolean = false,
    /**
     * The app in front, and the app of each activity (B2.1). The island hides an app's own media,
     * progress and notification while that app is in front (except a ringing call), so they don't
     * hold the window either. A temporary copy of the view's rule (IslandView `isMedia` ...),
     * until F1's single scene derivation replaces both.
     */
    val foregroundPackage: String? = null,
    val mediaPackage: String? = null,
    val notificationPackage: String? = null,
    val progressPackage: String? = null,
    val incomingCall: Boolean = false,
) {
    private fun notInFront(pkg: String?) = foregroundPackage == null || pkg != foregroundPackage

    /** Media, notification and progress as the island draws them: not the foreground app's own. */
    val mediaShown: Boolean get() = media && notInFront(mediaPackage)
    val notificationShown: Boolean get() = notification && (incomingCall || notInFront(notificationPackage))
    val progressShown: Boolean get() = progress && notInFront(progressPackage)
}

/** What the overlay window should be. The service turns it into pixels and window flags. */
sealed interface WindowSpec {
    /** 1×1 and not touchable. */
    data object Gone : WindowSpec

    /** MATCH_PARENT × MATCH_PARENT and touchable, so a tap outside the card collapses it. */
    data object FullScreen : WindowSpec

    /** A strip at the top of the screen. [widthDp] null means MATCH_PARENT (left cutout). */
    data class Compact(val widthDp: Float?, val heightDp: Float, val touchable: Boolean) : WindowSpec
}

/** A [WindowSpec] in window pixels. Width and height may be `MATCH_PARENT`. */
data class WindowPx(val width: Int, val height: Int, val touchable: Boolean)

fun WindowSpec.toWindowPx(density: Float): WindowPx = when (this) {
    WindowSpec.Gone -> WindowPx(1, 1, touchable = false)
    WindowSpec.FullScreen -> WindowPx(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, touchable = true)
    is WindowSpec.Compact -> WindowPx(
        width = widthDp?.let { (it * density).toInt() } ?: WindowManager.LayoutParams.MATCH_PARENT,
        height = (heightDp * density).toInt(),
        touchable = touchable,
    )
}

/** Which kinds of content keep the compact window up, after the `show*` settings are applied. */
data class WindowOccupancy(
    /** A visible activity or system alert. */
    val occupied: Boolean,
    /** Weather or network speed is on (always shown). */
    val persistentWidgets: Boolean,
    /** Charging, unlock, connection or ringer: these interrupt the collapse latch. */
    val systemAlert: Boolean,
)

/** Whether the island may show at all: enabled, orientation and lock-screen rules. */
fun islandAllowed(i: WindowInputs): Boolean {
    val s = i.settings
    return s.isEnabled && (s.showInLandscape || !i.landscape) && (!s.hideOnLockScreen || !i.locked)
}

/** The service's occupancy rule, used by [compactWindowSpec] and by the service's collapse timing. */
fun windowOccupancy(i: WindowInputs): WindowOccupancy {
    val s = i.settings
    val charging = s.showCharging && i.charging
    val unlock = i.unlock
    val connection = (s.showBluetooth || s.showWifi) && i.connection
    val ringer = s.showRingerMode && i.ringer
    val flashlight = s.showFlashlight && i.flashlight
    val occupied = (s.showCalls && i.call) ||
        (s.showMedia && i.mediaShown) ||
        (s.showNotifications && i.notificationShown) ||
        (s.showLiveProgress && i.progressShown) ||
        (s.showScreenRecording && i.recording) ||
        flashlight || charging || unlock || connection || ringer
    return WindowOccupancy(
        occupied = occupied,
        persistentWidgets = s.showWeather || s.showNetworkSpeed,
        systemAlert = charging || unlock || connection || ringer,
    )
}

/**
 * The window the service settles on after a state change (`updateWindowState`). Only the "what
 * size" decision lives here: when to apply it (the collapse latch, the delayed hide) stays in the
 * service. Behaviour is today's, inconsistencies included (06 P1-1; F5 changes them).
 */
fun compactWindowSpec(i: WindowInputs): WindowSpec {
    if (!islandAllowed(i)) return WindowSpec.Gone
    if (i.expanded) return WindowSpec.FullScreen

    val s = i.settings
    val occ = windowOccupancy(i)
    val charging = s.showCharging && i.charging
    val unlock = i.unlock
    val connection = (s.showBluetooth || s.showWifi) && i.connection
    val ringer = s.showRingerMode && i.ringer
    val flashlight = s.showFlashlight && i.flashlight

    val shouldShowCompact = (occ.occupied || s.alwaysShowPill || occ.persistentWidgets) && !i.userHidden
    if (!shouldShowCompact) return WindowSpec.Gone

    val activeCount = (if (s.showScreenRecording && i.recording) 1 else 0) +
        (if (s.showNotifications && i.notificationShown) 1 else 0) +
        (if (s.showMedia && i.mediaShown) 1 else 0) +
        (if (s.showLiveProgress && i.progressShown) 1 else 0) +
        (if (flashlight) 1 else 0)
    val isDual = !charging && !unlock && !connection && !ringer && activeCount >= 2

    val totalWidthDp = if (charging) {
        val chargingPillWidth = (s.widthDp + 20f).coerceAtLeast(140f)
        chargingPillWidth + 80f
    } else if (ringer) {
        val pillWidth = (s.widthDp * 0.95f).coerceAtLeast(130f)
        pillWidth + 80f
    } else if (unlock || connection) {
        val pillWidth = (s.widthDp * 0.82f).coerceAtLeast(108f)
        pillWidth + 80f
    } else if (i.userMinimized) {
        s.heightDp + 24f
    } else if (isDual) {
        val mainHalf = s.widthDp / 2f
        val rightEdge = mainHalf + 8f + s.heightDp + 40f
        (rightEdge * 2f).coerceAtLeast(s.widthDp + 80f)
    } else {
        s.widthDp + 80f
    }

    // userHidden is already excluded above, so touchability is just "is something there"
    val touchable = occ.occupied || occ.persistentWidgets
    return WindowSpec.Compact(
        widthDp = if (s.isLeftCutout) null else totalWidthDp + 2f * abs(s.horizontalOffsetDp),
        heightDp = (s.topOffsetDp + s.heightDp + 12).toFloat(),
        touchable = touchable,
    )
}

/**
 * The window `initOverlay` creates before the first state update. Kept separate on purpose,
 * because it differs from [compactWindowSpec] (06 P1-1, F5 merges them):
 * - occupancy ignores the `show*` toggles (any raw activity counts) and `userHidden`
 * - the width uses `widthDp * 0.90` and a charging edge, and ignores the unlock, ringer,
 *   minimized and dual widths
 */
fun initialWindowSpec(i: WindowInputs): WindowSpec {
    val s = i.settings
    val allowed = islandAllowed(i)
    val rawOccupied = i.call || i.recording || i.notification || i.media || i.progress ||
        i.charging || i.unlock || i.connection || i.flashlight || i.ringer
    val occupied = rawOccupied && allowed
    val persistentWidgets = (s.showWeather || s.showNetworkSpeed) && allowed
    val shouldShowCompact = (occupied || s.alwaysShowPill || persistentWidgets) && allowed
    if (!shouldShowCompact) return WindowSpec.Gone

    val mainHalf = (s.widthDp * 0.90f).coerceAtLeast(124f) / 2f
    val dualRightEdge = mainHalf + 8f + s.heightDp + 44f
    val chargingEdge = ((s.widthDp + 20f).coerceAtLeast(140f) + 80f) / 2f
    val totalWidthDp = (maxOf(dualRightEdge, chargingEdge) * 2f).coerceAtLeast(s.widthDp + 100f)

    return WindowSpec.Compact(
        widthDp = if (s.isLeftCutout) null else totalWidthDp + 2f * abs(s.horizontalOffsetDp),
        heightDp = (s.topOffsetDp + s.heightDp + 12).toFloat(),
        touchable = occupied || persistentWidgets,
    )
}
