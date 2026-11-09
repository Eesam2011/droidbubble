package com.droidbubble.app.service

import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NotificationGateKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The window table (S3.2). Every row is today's behaviour, bugs included. Rows marked
 * `// BUG <task>` encode a known bug with today's value: the task that fixes it flips the row.
 *
 * Defaults: pill 180 × 36 dp, top offset 12 dp, so the compact window is 60 dp tall.
 */
class OverlayWindowMathTest {

    private data class Row(val name: String, val inputs: WindowInputs, val expected: WindowSpec)

    private val d = IslandSettings()
    private val messagesOn = d.copy(showNotifications = true)

    private fun compact(w: Float?, h: Float = 60f, touchable: Boolean = true) = WindowSpec.Compact(w, h, touchable)

    private companion object {
        const val SPOTIFY = "com.spotify.music"
        const val CHROME = "com.android.chrome"
        const val WHATSAPP = "com.whatsapp"
        const val DIALER = "com.google.android.dialer"
    }

    private val compactRows = listOf(
        Row("idle", WindowInputs(d), WindowSpec.Gone),
        Row("idle + alwaysShowPill", WindowInputs(d.copy(alwaysShowPill = true)), compact(260f, touchable = false)),

        // Single activities: pill + 40 dp each side
        Row("call", WindowInputs(d, call = true), compact(260f)),
        Row("media", WindowInputs(d, media = true), compact(260f)),
        Row("message, notifications on", WindowInputs(messagesOn, notification = true), compact(260f)),
        Row("message, defaults (notifications off)", WindowInputs(d, notification = true), WindowSpec.Gone),
        Row("progress", WindowInputs(d, progress = true), compact(260f)),
        Row("recording", WindowInputs(d, recording = true), compact(260f)),
        Row("flashlight", WindowInputs(d, flashlight = true), compact(260f)),
        Row("persistent widgets only", WindowInputs(d.copy(showWeather = true)), compact(260f)),
        Row("media hidden by showMedia = false", WindowInputs(d.copy(showMedia = false), media = true), WindowSpec.Gone),

        // System glances
        Row("charging", WindowInputs(d, charging = true), compact(280f)),           // max(180 + 20, 140) + 80
        Row("ringer", WindowInputs(d, ringer = true), compact(251f)),               // 180 × 0.95 + 80
        Row("unlock", WindowInputs(d, unlock = true), compact(227.6f)),             // 180 × 0.82 + 80
        Row("connection", WindowInputs(d, connection = true), compact(227.6f)),
        Row("charging wins over dual", WindowInputs(d, media = true, progress = true, charging = true), compact(280f)),

        // Dual, minimized, hidden
        Row("dual media + progress", WindowInputs(d, media = true, progress = true), compact(348f)),   // (90 + 8 + 36 + 40) × 2
        Row("dual media + message", WindowInputs(messagesOn, media = true, notification = true), compact(348f)),
        Row("minimized", WindowInputs(d, media = true, userMinimized = true), compact(60f)),          // height + 24
        Row("hidden", WindowInputs(d, media = true, userHidden = true), WindowSpec.Gone),

        // Placement settings
        Row("left cutout", WindowInputs(d.copy(isLeftCutout = true), media = true), compact(null)),
        Row("horizontal offset +20", WindowInputs(d.copy(horizontalOffsetDp = 20), media = true), compact(300f)),
        Row("horizontal offset -20", WindowInputs(d.copy(horizontalOffsetDp = -20), media = true), compact(300f)),
        Row("size 220 × 40", WindowInputs(d.copy(widthDp = 220, heightDp = 40), media = true), compact(300f, h = 64f)),

        // Orientation and lock screen
        Row("landscape, not allowed", WindowInputs(d, landscape = true, media = true), WindowSpec.Gone),
        Row("landscape, allowed", WindowInputs(d.copy(showInLandscape = true), landscape = true, media = true), compact(260f)),
        Row("locked + hideOnLockScreen", WindowInputs(d.copy(hideOnLockScreen = true), locked = true, media = true), WindowSpec.Gone),
        Row("locked, shown on lock screen", WindowInputs(d, locked = true, media = true), compact(260f)),
        Row("disabled", WindowInputs(d.copy(isEnabled = false), media = true), WindowSpec.Gone),

        // Expanded
        Row("expanded", WindowInputs(d, expanded = true, media = true), WindowSpec.FullScreen),
        Row("expanded but not allowed (landscape)", WindowInputs(d, expanded = true, landscape = true, media = true), WindowSpec.Gone),

        // B2.1: an app's own activities hide while it is in front, and the window goes with them
        Row("media, its app in the foreground", WindowInputs(d, media = true, mediaPackage = SPOTIFY, foregroundPackage = SPOTIFY), WindowSpec.Gone),
        Row("media, another app in the foreground", WindowInputs(d, media = true, mediaPackage = SPOTIFY, foregroundPackage = CHROME), compact(260f)),
        Row("media, foreground unknown", WindowInputs(d, media = true, mediaPackage = SPOTIFY), compact(260f)),
        Row("media app in front + a message from another app: single width, touchable",
            WindowInputs(messagesOn, media = true, mediaPackage = SPOTIFY, notification = true, notificationPackage = WHATSAPP, foregroundPackage = SPOTIFY),
            compact(260f)),
        Row("message, its app in the foreground", WindowInputs(messagesOn, notification = true, notificationPackage = WHATSAPP, foregroundPackage = WHATSAPP), WindowSpec.Gone),
        Row("ringing call, the phone app in the foreground",
            WindowInputs(messagesOn, notification = true, notificationPackage = DIALER, incomingCall = true, foregroundPackage = DIALER),
            compact(260f)),
        Row("progress, its app in the foreground", WindowInputs(d, progress = true, progressPackage = CHROME, foregroundPackage = CHROME), WindowSpec.Gone),

        // B2.2: settled, the window hugs the drawn pill: 12 dp per side, 6 dp below (54 dp tall)
        Row("media, settled", WindowInputs(d, media = true, settled = true), compact(204f, h = 54f)),
        Row("charging, settled", WindowInputs(d, charging = true, settled = true), compact(224f, h = 54f)),
        Row("dual, settled", WindowInputs(messagesOn, media = true, notification = true, settled = true), compact(288f, h = 54f)),
        Row("minimized, settled", WindowInputs(d, media = true, userMinimized = true, settled = true), compact(60f, h = 54f)),
        Row("left cutout, settled", WindowInputs(d.copy(isLeftCutout = true), media = true, settled = true), compact(null, h = 54f)),
        Row("expanded ignores settled", WindowInputs(d, media = true, expanded = true, settled = true), WindowSpec.FullScreen),
        Row("idle, settled", WindowInputs(d, settled = true), WindowSpec.Gone),
        // Weather in the pill and network speed in the bubble, with nothing else active: a dual pill
        Row("weather + network, settled", WindowInputs(d.copy(showWeather = true, showNetworkSpeed = true), settled = true), compact(288f, h = 54f)),
        Row("weather + network + media, settled: single", WindowInputs(d.copy(showWeather = true, showNetworkSpeed = true), media = true, settled = true), compact(204f, h = 54f)),

        // B3.1: each notification kind has its own toggle; "Incoming Notifications" is messages only
        Row("timer, defaults (showNotifications = false)", WindowInputs(d, notification = true, notificationGate = NotificationGateKind.LIVE), compact(260f)),
        Row("ringing call, defaults", WindowInputs(d, notification = true, notificationGate = NotificationGateKind.CALL), compact(260f)),
        Row("message, defaults", WindowInputs(d, notification = true, notificationGate = NotificationGateKind.MESSAGE), WindowSpec.Gone),
        Row("ringing call, Phone Calls off",
            WindowInputs(d.copy(showCalls = false), notification = true, notificationGate = NotificationGateKind.CALL), WindowSpec.Gone),
        Row("timer, Live Activities off",
            WindowInputs(d.copy(showLiveActivities = false), notification = true, notificationGate = NotificationGateKind.LIVE), WindowSpec.Gone),
    )

    @Test
    fun morphHold_scalesWithAnimationSpeed() {
        assertEquals(1200L, morphHoldMs(1f))
        assertEquals(1500L, morphHoldMs(0.8f))
        assertEquals(4000L, morphHoldMs(0.1f)) // clamped at 0.3x
    }

    @Test
    fun compactWindowSpec_table() {
        val failures = compactRows.mapNotNull { row ->
            val actual = compactWindowSpec(row.inputs)
            if (sameSpec(row.expected, actual)) null else "${row.name}: expected ${row.expected}, got $actual"
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    // ---- initialWindowSpec versus compactWindowSpec: the drift F5 removes (06 P1-1) ----

    @Test
    fun initial_usesItsOwnWidth_forEveryActivity() {
        // mainHalf = 162 / 2 = 81; dualRightEdge = 81 + 8 + 36 + 44 = 169; chargingEdge = 140 → 338
        for (inputs in listOf(
            WindowInputs(d, media = true),
            WindowInputs(d, unlock = true),
            WindowInputs(d, ringer = true),
            WindowInputs(d, media = true, progress = true),
        )) {
            assertSpec(compact(338f), initialWindowSpec(inputs))
        }
        assertSpec(compact(227.6f), compactWindowSpec(WindowInputs(d, unlock = true)))
    }

    @Test
    fun initial_ignoresShowToggles_andUserHidden() {
        // A message with notifications off: the first window is a touchable strip, the next update removes it
        assertSpec(compact(338f), initialWindowSpec(WindowInputs(d, notification = true)))
        assertSpec(WindowSpec.Gone, compactWindowSpec(WindowInputs(d, notification = true)))
        // userHidden is ignored by the initial window
        assertSpec(compact(338f), initialWindowSpec(WindowInputs(d, media = true, userHidden = true)))
    }

    @Test
    fun initial_respectsAllowedRules() {
        assertSpec(WindowSpec.Gone, initialWindowSpec(WindowInputs(d)))
        assertSpec(WindowSpec.Gone, initialWindowSpec(WindowInputs(d, landscape = true, media = true)))
        assertSpec(compact(338f, touchable = false), initialWindowSpec(WindowInputs(d.copy(alwaysShowPill = true))))
        assertSpec(compact(null), initialWindowSpec(WindowInputs(d.copy(isLeftCutout = true), media = true)))
    }

    @Test
    fun occupancy_flagsSystemAlerts() {
        assertTrue(windowOccupancy(WindowInputs(d, charging = true)).systemAlert)
        assertTrue(windowOccupancy(WindowInputs(d, unlock = true)).systemAlert)
        assertEquals(false, windowOccupancy(WindowInputs(d, media = true)).systemAlert)
        // A disabled glance is not an alert
        assertEquals(false, windowOccupancy(WindowInputs(d.copy(showCharging = false), charging = true)).systemAlert)
    }

    @Test
    fun toWindowPx_convertsAtDensity() {
        assertEquals(WindowPx(780, 180, true), compact(260f).toWindowPx(3f))
        assertEquals(WindowPx(-1, 180, true), compact(null).toWindowPx(3f))
        assertEquals(WindowPx(1, 1, false), WindowSpec.Gone.toWindowPx(3f))
        assertEquals(WindowPx(-1, -1, true), WindowSpec.FullScreen.toWindowPx(3f))
    }

    private fun sameSpec(e: WindowSpec, a: WindowSpec): Boolean = when {
        e is WindowSpec.Compact && a is WindowSpec.Compact ->
            e.touchable == a.touchable && kotlin.math.abs(e.heightDp - a.heightDp) < 0.01f &&
                (if (e.widthDp == null) a.widthDp == null else a.widthDp != null && kotlin.math.abs(e.widthDp - a.widthDp) < 0.01f)
        else -> e == a
    }

    private fun assertSpec(e: WindowSpec, a: WindowSpec) {
        assertTrue("expected $e, got $a", sameSpec(e, a))
    }
}
