package com.droidbubble.app.island.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** V3.2: compact text squeezes, then shortens, then drops; it never ends in "…". */
class CompactFitTest {

    /** A fake font: 7 dp per character, 92% when squeezed. */
    private fun width(t: String, squeeze: Boolean) = t.length * 7f * if (squeeze) CompactFit.SQUEEZE else 1f

    @Test fun fits() = assertEquals(CompactFit.Result("Sam"), CompactFit.fit("Sam", 40f, ::width))

    @Test fun squeezeFits() {
        // 8 chars: 56 plain, 51.5 squeezed
        assertEquals(CompactFit.Result("Samantha", CompactFit.SQUEEZE), CompactFit.fit("Samantha", 52f, ::width))
    }

    @Test fun firstNameOnly() {
        assertEquals("Sam", CompactFit.firstName("Sam Carter-Williams"))
        assertEquals("Emianlahzy", CompactFit.firstName("  Emianlahzy  "))
    }

    @Test fun dropped() {
        assertTrue(CompactFit.fit("Download", 30f, ::width).dropped)
        assertTrue(CompactFit.fit("Bartholomew", 30f, ::width).dropped)
    }

    @Test fun numbersAreNeverShortened() {
        assertNull(CompactFit.fit("12:34:56", 20f, ::width).text)
    }

    @Test fun rightToLeftText() {
        assertEquals(CompactFit.Result("سامي"), CompactFit.fit(CompactFit.firstName("سامي العلي"), 30f, ::width))
    }

    @Test fun noEllipsisInTheCompactPill() {
        val view = File("src/main/java/com/droidbubble/app/island/IslandView.kt").readText().replace("\r\n", "\n")
        val start = view.indexOf("fun CompactContent(")
        val body = view.substring(start, view.indexOf("\n}\n", start))
        assertTrue("CompactContent truncates with an ellipsis", !body.contains("Ellipsis"))
    }
}
