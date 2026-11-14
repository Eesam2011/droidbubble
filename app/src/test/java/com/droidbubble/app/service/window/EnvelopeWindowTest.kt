package com.droidbubble.app.service.window

import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandInputs
import com.droidbubble.app.island.scene.MeasuredHeights
import com.droidbubble.app.island.scene.deriveIslandScene
import com.droidbubble.app.model.ExpandWindowStrategy
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.service.WindowSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * F5.4: the envelope window around the open cards (a trial behind a flag, decision D7).
 *
 * Defaults: pill 180 × 36 dp at 12 dp; the open card is 340 × 1.1 = 374 dp wide and starts
 * 12 + 52 = 64 dp from the top; a media card is 195 × 1.1 = 214.5 dp tall; 24 dp margin.
 * A 411 dp phone is narrower than 374 + 2 × 24, so there the envelope is full width and only the
 * height shrinks; the 600 dp rows show the width.
 */
class EnvelopeWindowTest {

    private data class Row(
        val name: String,
        val inputs: IslandInputs,
        val expected: WindowSpec.Compact?,
        val settings: IslandSettings = IslandSettings(),
        val env: IslandEnv = IslandEnv(),
        val measured: MeasuredHeights? = MeasuredHeights(notification = 120.dp, progress = 150.dp),
    )

    private val d = IslandSettings()
    private val messagesOn = d.copy(showNotifications = true)
    private val wide = IslandEnv(screenWidthDp = 600)

    private fun env(w: Float?, h: Float) = WindowSpec.Compact(w, h, touchable = true)

    private companion object {
        val song = MediaInfo(packageName = "com.spotify.music", title = "Song", artist = "Artist", isPlaying = true)
        val message = NotificationInfo(key = "msg", packageName = "com.whatsapp", appName = "WhatsApp", title = "Sam", text = "Hello")
        val download = NotificationInfo(key = "dl", packageName = "com.android.chrome", appName = "Chrome", title = "file.zip", text = "1 of 2 MB", progress = 1, maxProgress = 2)
    }

    private val rows = listOf(
        // 64 + 214.5 + 24
        Row("media, phone: full width, short", IslandInputs(isExpanded = true, media = song), env(null, 302.5f)),
        // (187 + 24) × 2
        Row("media, 600 dp screen", IslandInputs(isExpanded = true, media = song), env(422f, 302.5f), env = wide),
        // Card 2 below card 1: 64 + 214.5 + 10 + 150 + 24. The bubble (114 + 18 from the centre) is inside the card
        Row("dual media + download: both cards", IslandInputs(isExpanded = true, media = song, progress = download), env(422f, 462.5f), env = wide),
        // The measured notification height: 64 + 120 + 24
        Row("message", IslandInputs(isExpanded = true, notification = message), env(422f, 208f), messagesOn, wide),
        // Smaller cards fit a phone: 340 × 0.8 = 272 wide, (136 + 24) × 2; 195 × 0.8 = 156 tall
        Row("media at scale 0.8, phone", IslandInputs(isExpanded = true, media = song), env(320f, 244f), d.copy(expandedScale = 0.8f)),
        // The pill goes back to its offset as the card collapses: room for it on both sides
        Row("horizontal offset 20 dp", IslandInputs(isExpanded = true, media = song), env(462f, 302.5f), d.copy(horizontalOffsetDp = 20), wide),
        Row("left cutout: full width", IslandInputs(isExpanded = true, media = song), env(null, 302.5f), d.copy(isLeftCutout = true), wide),
        // Nothing measured yet: the caller keeps MATCH_PARENT
        Row("heights not published yet", IslandInputs(isExpanded = true, media = song), null, measured = null),
    )

    /** dp sums of float tokens (1.1 × 195): equal within a hundredth of a dp. */
    private fun same(a: WindowSpec.Compact?, b: WindowSpec.Compact?): Boolean {
        if (a == null || b == null) return a == b
        val w = if (a.widthDp == null || b.widthDp == null) a.widthDp == b.widthDp else kotlin.math.abs(a.widthDp!! - b.widthDp!!) < 0.01f
        return w && kotlin.math.abs(a.heightDp - b.heightDp) < 0.01f && a.touchable == b.touchable
    }

    @Test
    fun envelopeTable() {
        val failures = rows.mapNotNull { row ->
            val scene = deriveIslandScene(row.inputs, row.settings, row.env)
            val actual = EnvelopeWindow.envelopeFor(scene, row.settings, row.env, row.measured)
            if (same(actual, row.expected)) null else "${row.name}: expected ${row.expected}, got $actual"
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }

    @Test
    fun envelopeAlwaysHoldsTheOpenCardWithItsMargin() {
        val scene = deriveIslandScene(IslandInputs(isExpanded = true, media = song), d, wide)
        val e = EnvelopeWindow.envelopeFor(scene, d, wide, MeasuredHeights(0.dp))!!
        // F5.2's window-ready gate needs the card's width and bottom inside the window
        assertTrue(e.widthDp!! >= 374f + 2 * EnvelopeWindow.ENVELOPE_MARGIN_DP - 0.01f)
        assertTrue(e.heightDp >= 64f + 214.5f + EnvelopeWindow.ENVELOPE_MARGIN_DP - 0.01f)
    }

    @Test
    fun grownOnly_neverShrinksWhileOpen() {
        val mp = WindowManager.LayoutParams.MATCH_PARENT
        assertEquals(900, EnvelopeWindow.grownOnly(900, 600))
        assertEquals(900, EnvelopeWindow.grownOnly(600, 900))
        assertEquals(mp, EnvelopeWindow.grownOnly(mp, 600))
        assertEquals(mp, EnvelopeWindow.grownOnly(600, mp))
    }

    @Test
    fun outsideTap_collapsesOnlyAnOpenCard() {
        var expanded = true
        var collapses = 0
        val c = OutsideTapCollapser(isExpanded = { expanded }, collapse = { collapses++ })
        assertFalse(c.onAction(MotionEvent.ACTION_DOWN))
        assertFalse(c.onAction(MotionEvent.ACTION_UP))
        assertEquals(0, collapses)
        assertTrue(c.onAction(MotionEvent.ACTION_OUTSIDE))
        assertEquals(1, collapses)
        expanded = false
        assertTrue(c.onAction(MotionEvent.ACTION_OUTSIDE))
        assertEquals(1, collapses)
    }

    @Test
    fun defaultIsMatchParent() {
        assertFalse(IslandSettings().envelopeWindowTrial)
        assertEquals(ExpandWindowStrategy.MATCH_PARENT, IslandSettings().expandWindowStrategy)
        assertEquals(ExpandWindowStrategy.ENVELOPE, IslandSettings(envelopeWindowTrial = true).expandWindowStrategy)
    }

    @Test
    fun noFlagNoWatchOutside() {
        assertFalse(WindowState(width = 10, height = 10, touchable = true).watchOutside)
        assertNull(EnvelopeWindow.envelopeFor(deriveIslandScene(IslandInputs(isExpanded = true, media = song), d, IslandEnv()), d, IslandEnv(), null))
    }
}
