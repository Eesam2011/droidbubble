package com.droidbubble.app.island.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import com.droidbubble.app.island.gesture.GestureSurface.COMPACT
import com.droidbubble.app.island.gesture.GestureSurface.EXPANDED_CARD
import com.droidbubble.app.island.gesture.GestureSurface.MINIMAL
import com.droidbubble.app.island.gesture.IslandGestureResult.Cancel
import com.droidbubble.app.island.gesture.IslandGestureResult.Dismiss
import com.droidbubble.app.island.gesture.IslandGestureResult.Expand
import com.droidbubble.app.island.gesture.IslandGestureResult.Minimise
import com.droidbubble.app.island.gesture.IslandGestureResult.OpenShade
import com.droidbubble.app.island.gesture.IslandGestureResult.Restore
import com.droidbubble.app.island.gesture.IslandGestureResult.Tuck
import com.droidbubble.app.island.gesture.IslandGestureResult.TuckInstead
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.reflect.KClass

/** M4.1: what each released gesture does (05 §6). Density 1, so dp = px. */
class GestureResolverTest {

    private val t = GestureThresholds().toPx(Density(1f))
    private val centre = 90f // a 180 dp pill

    private class Row(
        val name: String,
        val surface: GestureSurface,
        val axis: Axis,
        val dx: Float = 0f,
        val dy: Float = 0f,
        val vx: Float = 0f,
        val vy: Float = 0f,
        val startX: Float = 90f,
        val rtl: Boolean = false,
        val canDismiss: Boolean = true,
        val expect: KClass<out IslandGestureResult>,
    )

    private val rows = listOf(
        // ---- expanded card, sideways ----
        Row("slow drag past 48 dp right dismisses", EXPANDED_CARD, Axis.HORIZONTAL, dx = 60f, vx = 100f, expect = Dismiss::class),
        Row("slow drag past 48 dp left dismisses", EXPANDED_CARD, Axis.HORIZONTAL, dx = -60f, vx = -100f, expect = Dismiss::class),
        Row("short fast fling dismisses", EXPANDED_CARD, Axis.HORIZONTAL, dx = 15f, vx = 1500f, expect = Dismiss::class),
        Row("slow short drag springs back", EXPANDED_CARD, Axis.HORIZONTAL, dx = 30f, vx = 200f, expect = Cancel::class),
        Row("a fling against the drag springs back", EXPANDED_CARD, Axis.HORIZONTAL, dx = 30f, vx = -1500f, expect = Cancel::class),
        Row("a ringing call flung sideways tucks", EXPANDED_CARD, Axis.HORIZONTAL, dx = 80f, vx = 900f, canDismiss = false, expect = TuckInstead::class),
        Row("a ringing call, short drag, springs back", EXPANDED_CARD, Axis.HORIZONTAL, dx = 20f, canDismiss = false, expect = Cancel::class),
        // ---- expanded card, vertical ----
        Row("slow 60 dp swipe up tucks", EXPANDED_CARD, Axis.VERTICAL, dy = -60f, vy = -100f, expect = Tuck::class),
        Row("quick flick up tucks", EXPANDED_CARD, Axis.VERTICAL, dy = -10f, vy = -1800f, expect = Tuck::class),
        Row("short pull up springs back", EXPANDED_CARD, Axis.VERTICAL, dy = -20f, vy = -200f, expect = Cancel::class),
        Row("pulling the card down only springs back", EXPANDED_CARD, Axis.VERTICAL, dy = 120f, vy = 2000f, expect = Cancel::class),
        // ---- compact pill, vertical ----
        Row("swipe down 40 dp expands", COMPACT, Axis.VERTICAL, dy = 40f, vy = 300f, expect = Expand::class),
        Row("swipe down 80 dp opens the shade", COMPACT, Axis.VERTICAL, dy = 80f, vy = 300f, expect = OpenShade::class),
        Row("short fast flick down expands", COMPACT, Axis.VERTICAL, dy = 10f, vy = 1500f, expect = Expand::class),
        Row("tiny pull down springs back", COMPACT, Axis.VERTICAL, dy = 10f, vy = 100f, expect = Cancel::class),
        Row("swipe up on the pill does nothing", COMPACT, Axis.VERTICAL, dy = -60f, vy = -1500f, expect = Cancel::class),
        // ---- compact pill, sideways: inward minimises ----
        Row("from the right half, leftward is inward", COMPACT, Axis.HORIZONTAL, dx = -60f, startX = 160f, expect = Minimise::class),
        Row("from the right half, rightward is outward", COMPACT, Axis.HORIZONTAL, dx = 60f, startX = 160f, expect = Cancel::class),
        Row("from the left half, rightward is inward", COMPACT, Axis.HORIZONTAL, dx = 60f, startX = 20f, expect = Minimise::class),
        Row("from the left half, leftward is outward (no swipe-to-hide)", COMPACT, Axis.HORIZONTAL, dx = -60f, startX = 20f, expect = Cancel::class),
        Row("near the centre in LTR, leftward minimises", COMPACT, Axis.HORIZONTAL, dx = -60f, startX = 92f, expect = Minimise::class),
        Row("near the centre in RTL, rightward minimises", COMPACT, Axis.HORIZONTAL, dx = 60f, startX = 92f, rtl = true, expect = Minimise::class),
        Row("near the centre in RTL, leftward springs back", COMPACT, Axis.HORIZONTAL, dx = -60f, startX = 92f, rtl = true, expect = Cancel::class),
        Row("an inward fling minimises from a short drag", COMPACT, Axis.HORIZONTAL, dx = -12f, vx = -1500f, startX = 160f, expect = Minimise::class),
        // ---- minimal ring ----
        Row("ring swiped outward (rightward from its right side) restores", MINIMAL, Axis.HORIZONTAL, dx = 60f, startX = 170f, expect = Restore::class),
        Row("ring from the centre, rightward in LTR restores", MINIMAL, Axis.HORIZONTAL, dx = 60f, startX = 90f, expect = Restore::class),
        Row("ring swiped inward springs back", MINIMAL, Axis.HORIZONTAL, dx = -60f, startX = 90f, expect = Cancel::class),
        Row("ring pulled down springs back", MINIMAL, Axis.VERTICAL, dy = 60f, expect = Cancel::class),
    )

    @Test
    fun table() {
        assertTrue(rows.size >= 25)
        val failures = rows.mapNotNull { r ->
            val result = resolveRelease(
                r.surface, r.axis, Offset(r.dx, r.dy), Velocity(r.vx, r.vy),
                startX = r.startX, centreX = centre, thresholds = t, isRtl = r.rtl, canDismiss = r.canDismiss,
            )
            if (r.expect.isInstance(result)) null else "${r.name}: expected ${r.expect.simpleName}, got $result"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun dismiss_leavesInTheFlingDirection_withItsVelocity() {
        val left = resolveRelease(EXPANDED_CARD, Axis.HORIZONTAL, Offset(-70f, 0f), Velocity(-900f, 0f), 90f, centre, t, false, true)
        assertEquals(Dismiss(-1, -900f), left)
    }

    @Test
    fun cancel_carriesTheReleaseVelocity() {
        val result = resolveRelease(EXPANDED_CARD, Axis.HORIZONTAL, Offset(20f, 0f), Velocity(300f, 0f), 90f, centre, t, false, true)
        assertEquals(Cancel(Velocity(300f, 0f)), result)
    }

    @Test
    fun axisLock_followsTheLargerMovement() {
        assertEquals(Axis.HORIZONTAL, lockAxis(Offset(10f, 4f)))
        assertEquals(Axis.VERTICAL, lockAxis(Offset(4f, -10f)))
        assertEquals(Axis.VERTICAL, lockAxis(Offset(6f, 6f)))
    }

    @Test
    fun rubberBand_isMonotonicAndBounded() {
        val l = 120f
        var previous = rubberBand(0f, l)
        assertEquals(0f, previous, 0f)
        for (x in 1..2000) {
            val y = rubberBand(x.toFloat(), l)
            assertTrue("monotonic at $x", y > previous)
            assertTrue("bounded at $x", y < l)
            assertTrue("never ahead of the finger", y <= x)
            assertEquals("symmetric", -y, rubberBand(-x.toFloat(), l), 1e-4f)
            previous = y
        }
        assertTrue("1:1 at first", abs(rubberBand(1f, l) - 1f) < 0.01f)
    }
}
