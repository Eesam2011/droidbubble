package com.droidbubble.app.service

import com.droidbubble.app.island.IslandStateManager.ChargingInfo
import com.droidbubble.app.island.IslandStateManager.ConnectionAlert
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandInputs
import com.droidbubble.app.island.scene.deriveIslandScene
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The window table (S3.2, re-based on the scene in F1.4). The window is derived from the same
 * [deriveIslandScene] the view draws, so each row is "this state → this window".
 *
 * Defaults: pill 180 × 36 dp, top offset 12 dp, so the compact window is 60 dp tall.
 *
 * Rows marked `F1.4:` changed when the window moved onto the scene. Before, the service had its
 * own occupancy rule that disagreed with the view.
 */
class OverlayWindowMathTest {

    private data class Row(
        val name: String,
        val inputs: IslandInputs,
        val expected: WindowSpec,
        val settings: IslandSettings = IslandSettings(),
        val settled: Boolean = false,
        val env: IslandEnv = IslandEnv(),
    )

    private val d = IslandSettings()
    private val messagesOn = d.copy(showNotifications = true)

    private fun compact(w: Float?, h: Float = 60f, touchable: Boolean = true) = WindowSpec.Compact(w, h, touchable)

    private fun spec(inputs: IslandInputs, settings: IslandSettings = d, settled: Boolean = false, env: IslandEnv = IslandEnv()): WindowSpec {
        val scene = deriveIslandScene(inputs, settings, env)
        return compactWindowSpec(scene, windowTargets(scene, settings, env), settings, settled)
    }

    private companion object {
        const val SPOTIFY = "com.spotify.music"
        const val CHROME = "com.android.chrome"
        const val WHATSAPP = "com.whatsapp"
        const val DIALER = "com.google.android.dialer"

        val song = MediaInfo(packageName = SPOTIFY, title = "Song", artist = "Artist", isPlaying = true)
        val message = NotificationInfo(key = "msg", packageName = WHATSAPP, appName = "WhatsApp", title = "Sam", text = "Hello")
        val download = NotificationInfo(key = "dl", packageName = CHROME, appName = "Chrome", title = "file.zip", text = "1 of 2 MB", progress = 1, maxProgress = 2)
        val ringing = NotificationInfo(key = "call", packageName = DIALER, appName = "Phone", title = "Sam", text = "Incoming call", isCall = true, isIncomingCall = true, isOngoing = true)
        val timer = NotificationInfo(key = "timer", packageName = "com.google.android.deskclock", appName = "Clock", title = "Timer", text = "Timer", isOngoing = true, liveTimerBaseMs = 1_000L, liveTimerCountDown = true)
        val recording = ScreenRecordingState(isRecording = true)
        val landscape = IslandEnv(isLandscape = true)
    }

    private val rows = listOf(
        Row("idle", IslandInputs(), WindowSpec.Gone),
        Row("idle + alwaysShowPill", IslandInputs(), compact(260f, touchable = false), d.copy(alwaysShowPill = true)),

        // Single activities: pill + 40 dp each side
        Row("media", IslandInputs(media = song), compact(260f)),
        Row("message, notifications on", IslandInputs(notification = message), compact(260f), messagesOn),
        Row("message, defaults (notifications off)", IslandInputs(notification = message), WindowSpec.Gone),
        Row("progress", IslandInputs(progress = download), compact(260f)),
        Row("recording", IslandInputs(recording = recording), compact(260f)),
        Row("flashlight", IslandInputs(isFlashlightOn = true), compact(260f)),
        Row("media hidden by showMedia = false", IslandInputs(media = song), WindowSpec.Gone, d.copy(showMedia = false)),
        // F1.4: was a touchable 260 dp strip. The view draws nothing for the tracked call state
        // (the call's card is its notification), so the strip only blocked touches.
        Row("ongoing call only, no card", IslandInputs(isCallActive = true), WindowSpec.Gone),
        Row("ongoing call + its notification", IslandInputs(isCallActive = true, notification = ringing.copy(isIncomingCall = false)), compact(260f)),

        // System glances
        Row("charging", IslandInputs(charging = ChargingInfo(80)), compact(280f)),            // max(180 + 20, 140) + 80
        Row("ringer", IslandInputs(ringerMode = RingerMode.VIBRATE), compact(251f)),          // 180 × 0.95 + 80
        Row("unlock", IslandInputs(isUnlocked = true), compact(227.6f)),                      // 180 × 0.82 + 80
        Row("connection", IslandInputs(isBluetoothConnected = true), compact(227.6f)),
        Row("charging wins over dual", IslandInputs(media = song, progress = download, charging = ChargingInfo(80)), compact(280f)),
        // F1.4: was 227.6. The window took "Bluetooth or Wi-Fi on"; the view asks the alert's own toggle.
        Row("Wi-Fi alert with the Wi-Fi toggle off", IslandInputs(isBluetoothConnected = true, connectionAlert = ConnectionAlert.WIFI), WindowSpec.Gone, d.copy(showWifi = false)),

        // Dual, minimized, hidden
        Row("dual media + progress", IslandInputs(media = song, progress = download), compact(348f)),   // (90 + 8 + 36 + 40) × 2
        Row("dual media + message", IslandInputs(media = song, notification = message), compact(348f), messagesOn),
        Row("minimized", IslandInputs(media = song, userMinimized = true), compact(60f)),               // height + 24
        Row("hidden", IslandInputs(media = song, userHidden = true), WindowSpec.Gone),

        // Placement settings
        Row("left cutout", IslandInputs(media = song), compact(null), d.copy(isLeftCutout = true)),
        Row("horizontal offset +20", IslandInputs(media = song), compact(300f), d.copy(horizontalOffsetDp = 20)),
        Row("horizontal offset -20", IslandInputs(media = song), compact(300f), d.copy(horizontalOffsetDp = -20)),
        Row("size 220 × 40", IslandInputs(media = song), compact(300f, h = 64f), d.copy(widthDp = 220, heightDp = 40)),

        // Orientation and lock screen
        Row("landscape, not allowed", IslandInputs(media = song), WindowSpec.Gone, env = landscape),
        Row("landscape, allowed", IslandInputs(media = song), compact(260f), d.copy(showInLandscape = true), env = landscape),
        Row("locked + hideOnLockScreen", IslandInputs(media = song, isDeviceLocked = true), WindowSpec.Gone, d.copy(hideOnLockScreen = true)),
        Row("unlocked + hideOnLockScreen", IslandInputs(media = song), compact(260f), d.copy(hideOnLockScreen = true)),
        Row("locked, shown on lock screen", IslandInputs(media = song, isDeviceLocked = true), compact(260f)),
        Row("disabled", IslandInputs(media = song), WindowSpec.Gone, d.copy(isEnabled = false)),

        // Expanded
        Row("expanded", IslandInputs(media = song, isExpanded = true), WindowSpec.FullScreen),
        Row("expanded but not allowed (landscape)", IslandInputs(media = song, isExpanded = true), WindowSpec.Gone, env = landscape),
        Row("expanded but locked + hideOnLockScreen", IslandInputs(media = song, isExpanded = true, isDeviceLocked = true), WindowSpec.Gone, d.copy(hideOnLockScreen = true)),

        // B2.1: an app's own activities hide while it is in front, and the window goes with them
        Row("media, its app in the foreground", IslandInputs(media = song, foregroundPackage = SPOTIFY), WindowSpec.Gone),
        Row("media, another app in the foreground", IslandInputs(media = song, foregroundPackage = CHROME), compact(260f)),
        Row("media app in front + a message from another app: single width, touchable",
            IslandInputs(media = song, notification = message, foregroundPackage = SPOTIFY), compact(260f), messagesOn),
        Row("message, its app in the foreground", IslandInputs(notification = message, foregroundPackage = WHATSAPP), WindowSpec.Gone, messagesOn),
        Row("ringing call, the phone app in the foreground", IslandInputs(notification = ringing, foregroundPackage = DIALER), compact(260f)),
        Row("progress, its app in the foreground", IslandInputs(progress = download, foregroundPackage = CHROME), WindowSpec.Gone),
        Row("media app in front, always show pill: the empty pill, not touchable",
            IslandInputs(media = song, foregroundPackage = SPOTIFY), compact(260f, touchable = false), d.copy(alwaysShowPill = true)),

        // B2.2: settled, the window hugs the drawn pill: 12 dp per side, 6 dp below (54 dp tall)
        Row("media, settled", IslandInputs(media = song), compact(204f, h = 54f), settled = true),
        Row("charging, settled", IslandInputs(charging = ChargingInfo(80)), compact(224f, h = 54f), settled = true),
        Row("dual, settled", IslandInputs(media = song, notification = message), compact(288f, h = 54f), messagesOn, settled = true),
        Row("minimized, settled", IslandInputs(media = song, userMinimized = true), compact(60f, h = 54f), settled = true),
        Row("left cutout, settled", IslandInputs(media = song), compact(null, h = 54f), d.copy(isLeftCutout = true), settled = true),
        Row("expanded ignores settled", IslandInputs(media = song, isExpanded = true), WindowSpec.FullScreen, settled = true),
        Row("idle, settled", IslandInputs(), WindowSpec.Gone, settled = true),

        // B3.1: each notification kind has its own toggle; "Incoming Notifications" is messages only
        Row("timer, defaults (showNotifications = false)", IslandInputs(notification = timer), compact(260f)),
        Row("ringing call, defaults", IslandInputs(notification = ringing), compact(260f)),
        Row("ringing call, Phone Calls off", IslandInputs(notification = ringing), WindowSpec.Gone, d.copy(showCalls = false)),
        Row("timer, Live Activities off", IslandInputs(notification = timer), WindowSpec.Gone, d.copy(showLiveActivities = false)),
    )

    @Test
    fun morphHold_scalesWithAnimationSpeed() {
        assertEquals(1200L, morphHoldMs(1f))
        assertEquals(1500L, morphHoldMs(0.8f))
        assertEquals(4000L, morphHoldMs(0.1f)) // clamped at 0.3x
    }

    @Test
    fun compactWindowSpec_table() {
        val failures = rows.mapNotNull { row ->
            val actual = spec(row.inputs, row.settings, row.settled, row.env)
            if (sameSpec(row.expected, actual)) null else "${row.name}: expected ${row.expected}, got $actual"
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    /**
     * F1.4: the window is touchable exactly when the scene says something touchable is drawn.
     * This is the rule that keeps status-bar taps working over the foreground app (06 P0-4).
     */
    @Test
    fun touchable_followsTheScene() {
        val cases = listOf(
            IslandInputs(),
            IslandInputs(media = song),
            IslandInputs(media = song, foregroundPackage = SPOTIFY),
            IslandInputs(media = song, userMinimized = true),
            IslandInputs(media = song, userHidden = true),
            IslandInputs(isCallActive = true),
            IslandInputs(charging = ChargingInfo(50)),
            IslandInputs(media = song, isExpanded = true),
        )
        for (settings in listOf(d, d.copy(alwaysShowPill = true))) {
            for (inputs in cases) {
                val scene = deriveIslandScene(inputs, settings, IslandEnv())
                val px = compactWindowSpec(scene, windowTargets(scene, settings, IslandEnv()), settings, settled = true).toWindowPx(3f)
                assertEquals("$inputs alwaysShowPill=${settings.alwaysShowPill}", scene.wantsTouch, px.touchable)
            }
        }
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

}
