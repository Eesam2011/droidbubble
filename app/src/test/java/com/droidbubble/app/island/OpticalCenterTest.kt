package com.droidbubble.app.island

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import kotlin.math.abs

/** V7.2: glyphs sit on their box's centre, and the pill and bubble use two glyph sizes only. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OpticalCenterTest {

    private val catalogue: List<ImageVector> =
        IslandIcons::class.java.declaredMethods
            .filter { it.returnType == ImageVector::class.java && it.parameterCount == 0 }
            .map { it.invoke(IslandIcons) as ImageVector }

    /** The play button's glyph tops out at 36 dp; check at 48 dp for margin. */
    private val largestDp = 48f

    @Test
    fun shiftedGlyphsAreCentredWithinHalfADp() {
        val shifted = catalogue.distinctBy { it.name }.filter { IslandIcons.opticalShiftOf(it) != null }
        assertEquals("every glyph with an optical shift is in the catalogue", 6, shifted.size)
        for (glyph in shifted) {
            val (cx, cy) = GlyphCentroidProbe.measure(glyph)
            val (fx, fy) = IslandIcons.opticalShiftOf(glyph)!!
            val errX = abs(cx + fx) * largestDp
            val errY = abs(cy + fy) * largestDp
            assertTrue(String.format(Locale.US, "%s is %.2f dp, %.2f dp off centre", glyph.name, errX, errY), errX <= 0.5f && errY <= 0.5f)
        }
    }

    @Test
    fun playIsCentredInItsCircle() {
        val play = IslandIcons.Play
        val (cx, _) = GlyphCentroidProbe.measure(play)
        val shift = IslandIcons.opticalShiftOf(play).also { assertNotNull("play has no optical shift", it) }!!.first
        assertTrue("play centroid ${abs(cx + shift) * 36f} dp off", abs(cx + shift) * 36f <= 0.5f)
    }

    @Test
    fun pillAndBubbleGlyphsUseTheTwoSizeTokens() {
        val island = File("src/main/java/com/droidbubble/app/island")
        val bodies = listOf(
            File(island, "IslandView.kt") to listOf("fun AuxiliaryBubbleContent(", "fun CompactContent("),
            File(island, "LiveActivities.kt") to listOf("fun LiveActivityCompactIcon("),
        )
        val offenders = mutableListOf<String>()
        for ((file, fns) in bodies) {
            val text = file.readText().replace("\r\n", "\n")
            for (fn in fns) {
                val start = text.indexOf(fn).also { check(it >= 0) { "$fn not found in ${file.name}" } }
                val end = text.indexOf("\n}\n", start)
                val body = text.substring(start, end)
                Regex("""(?<![A-Za-z])Icon\(""").findAll(body).forEach { m ->
                    val size = Regex("""Modifier\.size\(([^)]*)\)""").find(body, m.range.last)?.groupValues?.get(1).orEmpty()
                    if (!(size.startsWith("IslandTokens.Glyph.Compact") || size.startsWith("IslandTokens.Glyph.Bubble") || size == "glyph")) {
                        offenders += "${file.name} $fn: Icon sized '$size'"
                    }
                }
            }
        }
        // The alert pills (unlock, connection, ringer) draw their glyphs at the compact size
        val shell = File(island, "shell/IslandShell.kt").readText()
        if (Regex("""\.size\(19\.dp\)""").containsMatchIn(shell)) offenders += "IslandShell.kt: an alert glyph at 19 dp"
        assertEquals(emptyList<String>(), offenders)
    }
}
