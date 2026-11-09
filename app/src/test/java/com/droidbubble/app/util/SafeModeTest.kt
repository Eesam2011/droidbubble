package com.droidbubble.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** B11.3: the crash-loop window, the pause and its expiry, and resume. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SafeModeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val minute = 60_000L

    @After
    fun tearDown() {
        context.getSharedPreferences("safe_mode", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun threeCrashesWithinTenMinutes_isALoop() {
        val now = 100 * minute
        assertTrue(SafeMode.isCrashLoop(listOf(now - 9 * minute, now - 5 * minute, now - minute), now))
        assertFalse("two aren't enough", SafeMode.isCrashLoop(listOf(now - 2 * minute, now - minute), now))
        assertFalse("too spread out", SafeMode.isCrashLoop(listOf(now - 30 * minute, now - 20 * minute, now - minute), now))
    }

    @Test
    fun lastPrimary_isTheNewestPrimaryBreadcrumb() {
        val crumbs = listOf(
            "1 state primary kind=TIMER pkg=com.google.android.deskclock keyHash=1",
            "2 state expand priority=current",
            "3 state primary kind=NONE pkg=com.whatsapp keyHash=2",
            "4 window spec=Gone",
        )
        assertEquals("NONE|com.whatsapp", SafeMode.lastPrimary(crumbs))
        assertNull(SafeMode.lastPrimary(listOf("1 state collapse")))
    }

    @Test
    fun crashLoop_pausesTheLastPrimaryForAnHour_thenExpires() {
        val now = 100 * minute
        repeat(3) { SafeMode.recordCrash(context, now - (3 - it) * minute, "NONE|com.example.crashy") }
        val pause = SafeMode.evaluateOnStart(context, now)
        assertNotNull(pause)
        assertEquals("com.example.crashy", pause!!.pkg)
        assertEquals("NONE", pause.kind)
        assertTrue(SafeMode.crashTimes(context).isEmpty())
        assertNotNull(SafeMode.activePause(context, now + 59 * minute))
        assertNull("expires after an hour", SafeMode.activePause(context, now + 61 * minute))
    }

    @Test
    fun resume_clearsThePause_andTwoCrashesDoNothing() {
        val now = 100 * minute
        repeat(3) { SafeMode.recordCrash(context, now - minute, "|com.example.crashy") }
        val pause = SafeMode.evaluateOnStart(context, now)!!
        assertNull("unknown kind pauses the whole package", pause.kind)
        assertTrue(pause.covers("com.example.crashy", "TIMER"))
        SafeMode.resume(context)
        assertNull(SafeMode.activePause(context, now))

        repeat(2) { SafeMode.recordCrash(context, now - minute, "NONE|com.other") }
        assertNull(SafeMode.evaluateOnStart(context, now))
    }
}
