package com.droidbubble.app.island.scene

import com.droidbubble.app.island.IslandStateManager.ChargingInfo
import com.droidbubble.app.island.IslandStateManager.ConnectionAlert
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.golden.IslandBoundsGolden
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandPriority.FLASHLIGHT
import com.droidbubble.app.model.IslandPriority.IDLE
import com.droidbubble.app.model.IslandPriority.MEDIA
import com.droidbubble.app.model.IslandPriority.POP_NOTIFICATION
import com.droidbubble.app.model.IslandPriority.PROGRESS
import com.droidbubble.app.model.IslandPriority.SCREEN_RECORDING
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** F1.1: the arbitration table. Pure JUnit: no Android, no Compose. */
class DeriveIslandSceneTest {

    private val base = IslandSettings(showNotifications = true)
    private val env = IslandEnv()

    private val song = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "Artist", isPlaying = true)
    private val message = NotificationInfo(key = "msg", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Hello")
    private val call = NotificationInfo(
        key = "call", packageName = "com.google.android.dialer", appName = "Phone", title = "Sam Carter",
        text = "Incoming call", isCall = true, isIncomingCall = true, isOngoing = true,
    )
    private val recorder = NotificationInfo(
        key = "rec", packageName = "com.google.android.apps.recorder", appName = "Recorder", title = "Recording",
        text = "00:12", isOngoing = true, liveTimerBaseMs = 1_000L,
    )
    private val download = NotificationInfo(
        key = "dl", packageName = "com.android.vending", appName = "Google Play", title = "Downloading",
        text = "12 MB of 40 MB", progress = 40, maxProgress = 100,
    )
    private val recordingOn = ScreenRecordingState(isRecording = true)

    private fun scene(inputs: IslandInputs, settings: IslandSettings = base, env: IslandEnv = this.env) =
        deriveIslandScene(inputs, settings, env)

    private class Row(
        val name: String,
        val inputs: IslandInputs,
        val settings: IslandSettings? = null,
        val mode: IslandMode,
        val primary: IslandPriority,
        val secondary: IslandPriority? = null,
        val alert: SystemAlert? = null,
    )

    private val rows = listOf(
        // ---- nothing ----
        Row("nothing", IslandInputs(), mode = IslandMode.IDLE, primary = IDLE),
        Row("nothing, always show pill", IslandInputs(), base.copy(alwaysShowPill = true), IslandMode.IDLE_PILL, IDLE),
        Row("always show pill, minimized", IslandInputs(userMinimized = true), base.copy(alwaysShowPill = true), IslandMode.MINIMAL, IDLE),
        Row("always show pill, hidden", IslandInputs(userHidden = true), base.copy(alwaysShowPill = true), IslandMode.IDLE, IDLE),
        // ---- every single activity ----
        Row("media", IslandInputs(media = song), mode = IslandMode.COMPACT, primary = MEDIA),
        Row("paused media with a title", IslandInputs(media = song.copy(isPlaying = false)), mode = IslandMode.COMPACT, primary = MEDIA),
        Row("paused media without a title", IslandInputs(media = song.copy(isPlaying = false, title = "")), mode = IslandMode.IDLE, primary = IDLE),
        Row("paused media, dismiss on pause", IslandInputs(media = song.copy(isPlaying = false)), base.copy(dismissMediaOnPause = true), IslandMode.IDLE, IDLE),
        Row("message", IslandInputs(notification = message), mode = IslandMode.COMPACT, primary = POP_NOTIFICATION),
        Row("progress", IslandInputs(progress = download), mode = IslandMode.COMPACT, primary = PROGRESS),
        Row("screen recording", IslandInputs(recording = recordingOn), mode = IslandMode.COMPACT, primary = SCREEN_RECORDING),
        Row("flashlight", IslandInputs(isFlashlightOn = true), mode = IslandMode.COMPACT, primary = FLASHLIGHT),
        // ---- settings gates ----
        Row("media off", IslandInputs(media = song), base.copy(showMedia = false), IslandMode.IDLE, IDLE),
        Row("message with notifications off", IslandInputs(notification = message), base.copy(showNotifications = false), IslandMode.IDLE, IDLE),
        Row("recorder with notifications off still shows (live)", IslandInputs(notification = recorder), base.copy(showNotifications = false), IslandMode.COMPACT, POP_NOTIFICATION),
        Row("recorder with live activities off", IslandInputs(notification = recorder), base.copy(showLiveActivities = false), IslandMode.IDLE, IDLE),
        Row("call with calls off", IslandInputs(notification = call), base.copy(showCalls = false), IslandMode.IDLE, IDLE),
        Row("progress off", IslandInputs(progress = download), base.copy(showLiveProgress = false), IslandMode.IDLE, IDLE),
        Row("recording off", IslandInputs(recording = recordingOn), base.copy(showScreenRecording = false), IslandMode.IDLE, IDLE),
        Row("flashlight off", IslandInputs(isFlashlightOn = true), base.copy(showFlashlight = false), IslandMode.IDLE, IDLE),
        // ---- pairs (default order: MEDIA, PROGRESS, RECORDING, NOTIFICATION, FLASHLIGHT) ----
        Row("media + message", IslandInputs(media = song, notification = message), mode = IslandMode.DUAL, primary = MEDIA, secondary = POP_NOTIFICATION),
        Row("media + progress", IslandInputs(media = song, progress = download), mode = IslandMode.DUAL, primary = MEDIA, secondary = PROGRESS),
        Row("media + recording", IslandInputs(media = song, recording = recordingOn), mode = IslandMode.DUAL, primary = MEDIA, secondary = SCREEN_RECORDING),
        Row("media + flashlight", IslandInputs(media = song, isFlashlightOn = true), mode = IslandMode.DUAL, primary = MEDIA, secondary = FLASHLIGHT),
        Row("progress + recording", IslandInputs(progress = download, recording = recordingOn), mode = IslandMode.DUAL, primary = PROGRESS, secondary = SCREEN_RECORDING),
        Row("progress + message", IslandInputs(progress = download, notification = message), mode = IslandMode.DUAL, primary = PROGRESS, secondary = POP_NOTIFICATION),
        Row("recording + message", IslandInputs(recording = recordingOn, notification = message), mode = IslandMode.DUAL, primary = SCREEN_RECORDING, secondary = POP_NOTIFICATION),
        Row("message + flashlight", IslandInputs(notification = message, isFlashlightOn = true), mode = IslandMode.DUAL, primary = POP_NOTIFICATION, secondary = FLASHLIGHT),
        Row("three activities: the third is not shown", IslandInputs(media = song, notification = recorder, progress = download), mode = IslandMode.DUAL, primary = MEDIA, secondary = PROGRESS),
        // ---- promotion and reordering ----
        Row("a call goes first", IslandInputs(media = song, notification = call), mode = IslandMode.DUAL, primary = POP_NOTIFICATION, secondary = MEDIA),
        Row("a priority app goes first", IslandInputs(media = song, notification = message), base.copy(priorityApps = setOf("com.whatsapp")), IslandMode.DUAL, POP_NOTIFICATION, MEDIA),
        Row("reordered: notification before media", IslandInputs(media = song, notification = message), base.copy(featurePriorities = listOf("NOTIFICATION", "MEDIA")), IslandMode.DUAL, POP_NOTIFICATION, MEDIA),
        Row("flashlight missing from the stored order goes last", IslandInputs(media = song, isFlashlightOn = true), base.copy(featurePriorities = listOf("MEDIA", "PROGRESS", "RECORDING", "NOTIFICATION")), IslandMode.DUAL, MEDIA, FLASHLIGHT),
        Row("flashlight alone, missing from the stored order", IslandInputs(isFlashlightOn = true), base.copy(featurePriorities = listOf("MEDIA")), IslandMode.COMPACT, FLASHLIGHT),
        Row("pop-in swaps the notification into the bubble", IslandInputs(media = song, progress = download, notification = message, isPopIn = true), mode = IslandMode.DUAL, primary = MEDIA, secondary = POP_NOTIFICATION),
        Row("pop-in changes nothing when the notification is primary", IslandInputs(media = song, notification = call, isPopIn = true), mode = IslandMode.DUAL, primary = POP_NOTIFICATION, secondary = MEDIA),
        // ---- the foreground app hides its own cards ----
        Row("media app in front", IslandInputs(media = song, foregroundPackage = "com.spotify.music"), mode = IslandMode.IDLE, primary = IDLE),
        Row("message app in front", IslandInputs(notification = message, foregroundPackage = "com.whatsapp"), mode = IslandMode.IDLE, primary = IDLE),
        Row("progress app in front", IslandInputs(progress = download, foregroundPackage = "com.android.vending"), mode = IslandMode.IDLE, primary = IDLE),
        Row("a ringing call shows over its own app", IslandInputs(notification = call, foregroundPackage = "com.google.android.dialer"), mode = IslandMode.COMPACT, primary = POP_NOTIFICATION),
        Row("another app in front", IslandInputs(media = song, foregroundPackage = "com.android.chrome"), mode = IslandMode.COMPACT, primary = MEDIA),
        // ---- system alerts cover dual, in the order charging, unlock, connection, ringer ----
        Row("charging", IslandInputs(charging = ChargingInfo(80)), mode = IslandMode.SYSTEM_ALERT, primary = IDLE, alert = SystemAlert.CHARGING),
        Row("charging over dual", IslandInputs(media = song, notification = message, charging = ChargingInfo(80)), mode = IslandMode.SYSTEM_ALERT, primary = MEDIA, alert = SystemAlert.CHARGING),
        Row("unlock over dual", IslandInputs(media = song, notification = message, isUnlocked = true), mode = IslandMode.SYSTEM_ALERT, primary = MEDIA, alert = SystemAlert.UNLOCK),
        Row("bluetooth over dual", IslandInputs(media = song, notification = message, isBluetoothConnected = true), mode = IslandMode.SYSTEM_ALERT, primary = MEDIA, alert = SystemAlert.CONNECTION),
        Row("ringer over dual", IslandInputs(media = song, notification = message, ringerMode = RingerMode.VIBRATE), mode = IslandMode.SYSTEM_ALERT, primary = MEDIA, alert = SystemAlert.RINGER),
        Row("charging beats unlock", IslandInputs(charging = ChargingInfo(80), isUnlocked = true), mode = IslandMode.SYSTEM_ALERT, primary = IDLE, alert = SystemAlert.CHARGING),
        Row("unlock beats ringer", IslandInputs(isUnlocked = true, ringerMode = RingerMode.SILENT), mode = IslandMode.SYSTEM_ALERT, primary = IDLE, alert = SystemAlert.UNLOCK),
        Row("charging off", IslandInputs(charging = ChargingInfo(80)), base.copy(showCharging = false), IslandMode.IDLE, IDLE),
        Row("wifi alert follows the Wi-Fi toggle", IslandInputs(isBluetoothConnected = true, connectionAlert = ConnectionAlert.WIFI), base.copy(showWifi = false), IslandMode.IDLE, IDLE),
        Row("bluetooth alert follows the Bluetooth toggle", IslandInputs(isBluetoothConnected = true), base.copy(showBluetooth = false), IslandMode.IDLE, IDLE),
        Row("ringer off", IslandInputs(ringerMode = RingerMode.SILENT), base.copy(showRingerMode = false), IslandMode.IDLE, IDLE),
        // ---- user gestures ----
        Row("minimized", IslandInputs(media = song, userMinimized = true), mode = IslandMode.MINIMAL, primary = MEDIA),
        Row("hidden", IslandInputs(media = song, userHidden = true), mode = IslandMode.IDLE, primary = MEDIA),
        // ---- expanded ----
        Row("expanded media", IslandInputs(media = song, isExpanded = true), mode = IslandMode.EXPANDED, primary = MEDIA),
        Row("expanded dual", IslandInputs(media = song, notification = message, isExpanded = true), mode = IslandMode.EXPANDED, primary = MEDIA, secondary = POP_NOTIFICATION),
        Row("flashlight solo", IslandInputs(media = song, isFlashlightOn = true, isExpanded = true, expandedPriority = FLASHLIGHT, flashlightSolo = true), mode = IslandMode.EXPANDED, primary = FLASHLIGHT),
        Row("flashlight solo latch without a flashlight", IslandInputs(media = song, notification = message, flashlightSolo = true), mode = IslandMode.DUAL, primary = MEDIA, secondary = POP_NOTIFICATION),
    )

    @Test
    fun table() {
        assertTrue("at least 30 rows", rows.size >= 30)
        val failures = rows.mapNotNull { row ->
            val s = scene(row.inputs, row.settings ?: base)
            val actual = "mode=${s.mode} primary=${s.primary} secondary=${s.secondary} alert=${s.alert}"
            val expected = "mode=${row.mode} primary=${row.primary} secondary=${row.secondary} alert=${row.alert}"
            if (actual == expected) null else "${row.name}\n  expected: $expected\n  actual:   $actual"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun landscape_isOff_unlessAllowed() {
        val landscape = IslandEnv(isLandscape = true)
        assertEquals(IslandScene.Off, scene(IslandInputs(media = song), env = landscape))
        assertEquals(IslandMode.COMPACT, scene(IslandInputs(media = song), base.copy(showInLandscape = true), landscape).mode)
    }

    @Test
    fun disabled_isOff() {
        assertEquals(IslandScene.Off, scene(IslandInputs(media = song), base.copy(isEnabled = false)))
    }

    @Test
    fun locked_isOff_onlyWithHideOnLockScreen() {
        val locked = IslandInputs(media = song, isDeviceLocked = true)
        assertEquals(IslandScene.Off, scene(locked, base.copy(hideOnLockScreen = true)))
        assertEquals(IslandMode.COMPACT, scene(locked).mode)
        assertEquals(IslandMode.COMPACT, scene(IslandInputs(media = song), base.copy(hideOnLockScreen = true)).mode)
    }

    @Test
    fun anOngoingCall_hasNoCardOfItsOwn() {
        val s = scene(IslandInputs(isCallActive = true))
        assertEquals(IslandMode.IDLE, s.mode)
        assertFalse(s.isOccupied || s.wantsTouch)
    }

    @Test
    fun dualAndCounts() {
        val dual = scene(IslandInputs(media = song, notification = message))
        assertTrue(dual.isDual)
        assertEquals(2, dual.activeCount)
        assertTrue(dual.showSecondaryCard)

        val covered = scene(IslandInputs(media = song, notification = message, charging = ChargingInfo(80)))
        assertFalse("a glance covers dual", covered.isDual)
        assertNull(covered.secondary)
        assertFalse(covered.showSecondaryCard)
        assertEquals(2, covered.activeCount)
        assertTrue(covered.visible.isCharging && covered.visible.isMedia && covered.visible.isNotification)

        val solo = scene(IslandInputs(media = song, isFlashlightOn = true, isExpanded = true, flashlightSolo = true))
        assertFalse("flashlight solo shows one card", solo.showSecondaryCard)
    }

    @Test
    fun alertFlags_areExclusive() {
        val all = scene(IslandInputs(charging = ChargingInfo(50), isUnlocked = true, isBluetoothConnected = true, ringerMode = RingerMode.RING)).visible
        assertTrue(all.isCharging)
        assertFalse(all.isUnlock || all.isBluetooth || all.isRingerMode)
    }

    @Test
    fun wantsTouch_followsWhatIsDrawn() {
        assertFalse(scene(IslandInputs()).wantsTouch)
        assertFalse("the empty pill is not touchable", scene(IslandInputs(), base.copy(alwaysShowPill = true)).wantsTouch)
        assertTrue(scene(IslandInputs(media = song)).wantsTouch)
        assertTrue(scene(IslandInputs(media = song, userMinimized = true)).wantsTouch)
        assertFalse(scene(IslandInputs(media = song, userHidden = true)).wantsTouch)
        assertFalse("the foreground app's card is hidden", scene(IslandInputs(media = song, foregroundPackage = song.packageName)).wantsTouch)
        assertTrue(scene(IslandInputs(media = song, isExpanded = true)).wantsTouch)
        assertTrue(scene(IslandInputs(charging = ChargingInfo(80))).wantsTouch)
    }

    /**
     * The S1 matrix (IslandCharacterizationTest) records which activity each slot draws. The same
     * 19 states, as inputs, must give the scene that S1 saw on screen.
     */
    @Test
    fun matchesTheS1ContentGolden() {
        val alwaysPill = base.copy(alwaysShowPill = true)
        val s1 = listOf(
            Triple("idle_alwaysShowPill", IslandInputs(), alwaysPill),
            Triple("media_playing", IslandInputs(media = song), base),
            Triple("media_paused", IslandInputs(media = song.copy(isPlaying = false)), base),
            Triple("message", IslandInputs(notification = message), base),
            Triple("download_40", IslandInputs(progress = download), base),
            Triple("voice_recorder", IslandInputs(notification = recorder), base),
            Triple("timer_running", IslandInputs(notification = recorder.copy(key = "timer", liveTimerCountDown = true)), base),
            // A ringing call opens the card by itself
            Triple("incoming_call", IslandInputs(notification = call, isExpanded = true), base),
            // The blinking torch is filtered out before it reaches isFlashlightOn
            Triple("incoming_call_torch_blink", IslandInputs(notification = call, isExpanded = true), base),
            Triple("media_and_message", IslandInputs(media = song, notification = message), base),
            // updateProgress routes the recorder to the notification slot; the download is the hidden third
            Triple("media_recorder_download", IslandInputs(media = song, notification = recorder), base),
            Triple("minimized", IslandInputs(media = song, userMinimized = true), base),
            Triple("hidden", IslandInputs(media = song, userHidden = true), base),
            Triple("media_app_in_foreground", IslandInputs(media = song, foregroundPackage = song.packageName), base),
            Triple("charging_glance", IslandInputs(charging = ChargingInfo(80, 18.0)), base),
            Triple("ringer_glance", IslandInputs(ringerMode = RingerMode.VIBRATE), base),
            Triple("expanded_media", IslandInputs(media = song, isExpanded = true), base),
            Triple("expanded_message", IslandInputs(notification = message, isExpanded = true), base),
            Triple("expanded_dual", IslandInputs(media = song, notification = message, isExpanded = true), base),
        )
        val failures = s1.mapNotNull { (state, inputs, settings) ->
            val golden = IslandBoundsGolden.rows.getValue("$state|default").substringAfter("| ").substringBefore(" knownIssue")
            val actual = slots(scene(inputs, settings))
            if (golden == actual) null else "$state\n  S1 saw:  $golden\n  scene:   $actual"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    /** What each content slot draws for a scene, in the S1 golden's format. */
    private fun slots(s: IslandScene): String {
        val pill = if (s.mode in setOf(IslandMode.COMPACT, IslandMode.DUAL, IslandMode.IDLE_PILL)) s.primary.name else "-"
        val bubble = if (s.mode == IslandMode.DUAL) s.secondary?.name ?: "-" else "-"
        val card1 = if (s.mode == IslandMode.EXPANDED) s.primary.name else "-"
        val card2 = if (s.mode == IslandMode.EXPANDED && s.showSecondaryCard) s.secondary?.name ?: "-" else "-"
        return "pill:$pill bubble:$bubble card1:$card1 card2:$card2"
    }
}
