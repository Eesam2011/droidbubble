package com.droidbubble.app.island.scene

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import com.droidbubble.app.island.IslandStateManager.ChargingInfo
import com.droidbubble.app.island.IslandStateManager.RingerMode
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.liveActivityCornerRadius
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** F1.3: the target-geometry table. Pure JUnit. */
class DeriveIslandTargetsTest {

    private val base = IslandSettings(showNotifications = true)
    private val env = IslandEnv()
    private val measured = MeasuredHeights(notification = 174.5.dp, progress = 150.dp)

    private val song = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "Artist", isPlaying = true)
    private val message = NotificationInfo(key = "msg", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Hello")

    private fun targets(
        inputs: IslandInputs,
        settings: IslandSettings = base,
        env: IslandEnv = this.env,
        card2Expanded: Boolean = true,
        bubbleEmerged: Boolean = true,
    ) = deriveIslandTargets(deriveIslandScene(inputs, settings, env), settings, env, measured, card2Expanded, bubbleEmerged)

    private fun assertDp(expected: Dp, actual: Dp) = assertEquals(expected.value, actual.value, 0.001f)

    @Test
    fun default_isA180x36Pill_withTheCardDockedBehindIt() {
        val t = targets(IslandInputs(media = song))
        assertDp(180.dp, t.compactWidth)
        assertEquals(IslandGeometry(0.dp, 36.dp, IslandTokens.CompactCornerRadius, 12.dp), t.card1)
        assertDp(0.dp, t.card2Height)
        assertDp(0.dp, t.bubbleRestX)
        assertDp(0.dp, t.cardSpacing)
        assertNull(t.alertWidth)
    }

    @Test
    fun idle_hasNoPill_unlessAlwaysShown() {
        assertDp(0.dp, targets(IslandInputs()).compactWidth)
        assertDp(180.dp, targets(IslandInputs(), base.copy(alwaysShowPill = true)).compactWidth)
    }

    @Test
    fun pillSizeFollowsTheSettings() {
        val t = targets(IslandInputs(media = song), base.copy(widthDp = 220, heightDp = 40, topOffsetDp = 20))
        assertDp(220.dp, t.compactWidth)
        assertDp(40.dp, t.card1.height)
        assertDp(20.dp, t.card1.top)
    }

    @Test
    fun expanded_isTheCardBelowTheStatusBar() {
        val t = targets(IslandInputs(media = song, isExpanded = true))
        assertDp(0.dp, t.compactWidth)
        assertDp(374.dp, t.card1.width)          // 340 × 1.1
        assertDp(214.5.dp, t.card1.height)       // media: 195 × 1.1
        assertDp(64.dp, t.card1.top)             // 12 + 52
        assertDp(liveActivityCornerRadius(1.1f, 1f), t.card1.corner)
        assertDp(214.5.dp, t.card1ContentHeight)
    }

    @Test
    fun expandedScale_scalesTheCard() {
        assertDp(272.dp, targets(IslandInputs(media = song, isExpanded = true), base.copy(expandedScale = 0.8f)).card1.width)
        // 340 × 1.4 = 476 does not fit a 411 dp screen: clamped to the screen minus 16
        assertDp(395.dp, targets(IslandInputs(media = song, isExpanded = true), base.copy(expandedScale = 1.4f)).card1.width)
    }

    @Test
    fun narrowScreen_clampsTheCardWidth() {
        val narrow = IslandEnv(screenWidthDp = 320)
        assertDp(304.dp, targets(IslandInputs(media = song, isExpanded = true), env = narrow).card1.width)
        assertDp(304.dp, expandedCardWidth(base, narrow))
    }

    @Test
    fun measuredCards_useTheMeasuredHeights() {
        assertDp(174.5.dp, targets(IslandInputs(notification = message, isExpanded = true)).card1.height)
        val download = NotificationInfo(key = "dl", packageName = "a", appName = "A", title = "t", text = "x", progress = 1, maxProgress = 2)
        assertDp(150.dp, targets(IslandInputs(progress = download, isExpanded = true)).card1.height)
    }

    @Test
    fun alertWidths_andTheirFloors() {
        val charging = IslandInputs(charging = ChargingInfo(80))
        val unlock = IslandInputs(isUnlocked = true)
        val bluetooth = IslandInputs(isBluetoothConnected = true)
        val ringer = IslandInputs(ringerMode = RingerMode.VIBRATE)

        assertDp(200.dp, targets(charging).compactWidth)            // 180 + 20
        assertDp(147.6.dp, targets(unlock).compactWidth)            // 180 × 0.82
        assertDp(147.6.dp, targets(bluetooth).compactWidth)
        assertDp(171.dp, targets(ringer).compactWidth)              // 180 × 0.95
        assertDp(200.dp, targets(charging).alertWidth!!)

        val small = base.copy(widthDp = 100)
        assertDp(140.dp, targets(charging, small).compactWidth)
        assertDp(108.dp, targets(unlock, small).compactWidth)
        assertDp(108.dp, targets(bluetooth, small).compactWidth)
        assertDp(130.dp, targets(ringer, small).compactWidth)
    }

    @Test
    fun alert_ignoresTheHorizontalOffset_andKeepsTheCardClosed() {
        val offset = base.copy(horizontalOffsetDp = 14)
        assertDp(14.dp, targets(IslandInputs(media = song), offset).horizontalOffset)
        assertDp(0.dp, targets(IslandInputs(media = song, charging = ChargingInfo(80)), offset).horizontalOffset)
        assertDp(0.dp, targets(IslandInputs(media = song, isExpanded = true), offset).horizontalOffset)

        val alertWhileExpanded = targets(IslandInputs(media = song, isExpanded = true, charging = ChargingInfo(80)))
        assertDp(0.dp, alertWhileExpanded.card1.width)
        assertDp(12.dp, alertWhileExpanded.card1.top)
        assertDp(IslandTokens.CompactCornerRadius, alertWhileExpanded.card1.corner)
    }

    @Test
    fun minimized_isACircle_andHidden_isGone() {
        assertDp(36.dp, targets(IslandInputs(media = song, userMinimized = true)).compactWidth)
        assertDp(0.dp, targets(IslandInputs(media = song, userHidden = true)).compactWidth)
        assertDp(44.dp, targets(IslandInputs(media = song, userMinimized = true), base.copy(heightDp = 44)).compactWidth)
    }

    @Test
    fun bubble_restsBesideThePill_onceEmerged() {
        val dual = IslandInputs(media = song, notification = message)
        assertDp(114.dp, targets(dual).bubbleRestX)                                  // 90 + 6 + 18
        assertDp(0.dp, targets(dual, bubbleEmerged = false).bubbleRestX)
        assertDp(0.dp, targets(dual.copy(userMinimized = true)).bubbleRestX)
        assertDp(0.dp, targets(dual.copy(charging = ChargingInfo(80))).bubbleRestX)
        assertDp(0.dp, targets(dual.copy(isExpanded = true)).bubbleRestX)
        assertDp(136.dp, targets(dual, base.copy(widthDp = 220, heightDp = 40)).bubbleRestX)
    }

    @Test
    fun card2_opensWithTheSecondActivity() {
        val dual = IslandInputs(media = song, notification = message, isExpanded = true)
        val open = targets(dual)
        assertDp(174.5.dp, open.card2Height)
        assertDp(10.dp, open.cardSpacing)
        val delayed = targets(dual, card2Expanded = false)
        assertDp(0.dp, delayed.card2Height)
        assertDp(10.dp, delayed.cardSpacing)
        assertDp(0.dp, targets(IslandInputs(media = song, isExpanded = true)).card2Height)
    }
}
