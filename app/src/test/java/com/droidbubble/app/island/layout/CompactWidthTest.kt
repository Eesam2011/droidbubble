package com.droidbubble.app.island.layout

import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** V3.1: the pill's width model, its sides per activity and tick stability. Robolectric: the fixtures hold bitmaps. */
@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@Config(sdk = [35])
class CompactWidthTest {

    private val h = 36.dp
    private val rec = ScreenRecordingState()

    @Test
    fun widthIsTheGapPlusTwiceTheLargerSidePlusInsets_clamped() {
        val inset = CompactSlots.inset(h)
        assertEquals(8f, inset.value, 0f)
        assertEquals(120f, CompactSlots.width(20.dp, 36.dp, inset, wMax = 180.dp).value, 0f)  // 92 → min 120
        assertEquals(140f, CompactSlots.width(44.dp, 36.dp, inset, wMax = 180.dp).value, 0f)  // 36 + 88 + 16
        assertEquals(180f, CompactSlots.width(90.dp, 36.dp, inset, wMax = 180.dp).value, 0f)  // the setting is the maximum
        assertEquals(100f, CompactSlots.width(10.dp, 36.dp, inset, wMax = 100.dp).value, 0f)  // a setting under the minimum wins
    }

    @Test
    fun sidesPerActivity() {
        val media = CompactSlots.sides(IslandPriority.MEDIA, IslandFixtures.song, null, rec, h)
        assertEquals(20f, media.lead.value, 0f); assertEquals(13f, media.trail.value, 0f)
        val call = CompactSlots.sides(IslandPriority.CALL, null, null, rec, h)
        assertEquals(18f, call.lead.value, 0f); assertEquals("00:00", call.trailText)
        val msg = CompactSlots.sides(IslandPriority.POP_NOTIFICATION, null, IslandFixtures.message(), rec, h)
        assertEquals("Sam", msg.trailText)
        assertEquals(20f, msg.lead.value, 0.01f) // every leading picture is the 20 dp art size
        val dl = CompactSlots.sides(IslandPriority.PROGRESS, null, IslandFixtures.progress(42), rec, h)
        assertEquals("Google Play", dl.leadText); assertEquals("100", dl.trailText); assertEquals("%", dl.trailUnit)
        val torch = CompactSlots.sides(IslandPriority.FLASHLIGHT, null, null, rec, h)
        assertNull(torch.trailText)
        val idle = CompactSlots.sides(IslandPriority.IDLE, null, null, rec, h)
        assertEquals(0f, idle.lead.value, 0f)
    }

    @Test
    fun bubbleIsACircleUntilItsContentNeedsMore() {
        // V3.3: content + 6 dp each side, between H and 1.25 H
        assertEquals(36f, CompactSlots.bubbleWidth(16.dp, 36.dp).value, 0f)
        assertEquals(36f, CompactSlots.bubbleWidth(24.dp, 36.dp).value, 0f)
        assertEquals(40f, CompactSlots.bubbleWidth(28.dp, 36.dp).value, 0f)
        assertEquals(45f, CompactSlots.bubbleWidth(60.dp, 36.dp).value, 0f)
        assertEquals(25f, CompactSlots.bubbleWidth(13.dp, 25.dp).value, 0f)
    }

    @Test
    fun tickingValuesKeepTheirTemplate() {
        assertEquals(CompactSlots.template("9:59"), CompactSlots.template("10:00"))
        assertEquals(CompactSlots.template("0:59:59"), CompactSlots.template("1:00:00"))
        assertEquals(CompactSlots.template("-0:05"), CompactSlots.template("-12:30"))
        assertEquals("0 min", CompactSlots.template("5 min"))
        assertEquals("Sam", CompactSlots.template("Sam"))
    }

    @Test
    fun timerNotification_measuresItsTemplate_notTheLiveValue() {
        val timer = CompactSlots.sides(IslandPriority.POP_NOTIFICATION, null, IslandFixtures.timer, rec, h)
        assertEquals(timer.trailText, CompactSlots.template(timer.trailText!!))
    }
}

/** The bare app icon in a message pill is the smaller of the avatar footprint and the 20 dp art cap. */
private fun CompactSlots.minOfArt(h: androidx.compose.ui.unit.Dp) = minOf(avatarFootprint(h), com.droidbubble.app.island.IslandTokens.Glyph.CompactArt).value
