package com.droidbubble.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.NotificationInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** B11.1: breadcrumb ring, non-fatal log rotation, report format and privacy. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CrashLogTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var now = 1_000L

    @Before
    fun setUp() {
        CrashLog.resetForTest()
        CrashLog.clear(context)
        CrashLog.clock = { now++ }
        CrashLog.attach(context)
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        CrashLog.clock = android.os.SystemClock::elapsedRealtime
        CrashLog.clear(context)
        CrashLog.resetForTest()
        IslandStateManager.resetForTest()
    }

    @Test
    fun ring_keepsTheLast64_oldestFirst() {
        repeat(100) { CrashLog.breadcrumb("t", "e$it") }
        val crumbs = CrashLog.breadcrumbs()
        assertEquals(CrashLog.MAX_BREADCRUMBS, crumbs.size)
        assertTrue(crumbs.first().endsWith("t e36"))
        assertTrue(crumbs.last().endsWith("t e99"))
    }

    @Test
    fun nonFatal_rotatesToTheLast20_andLeavesABreadcrumb() {
        repeat(25) { CrashLog.nonFatal("posted:pkg$it", IllegalStateException("boom $it")) }
        val entries = CrashLog.readNonFatal(context)
        assertEquals(CrashLog.MAX_NONFATAL, entries.size)
        assertTrue(entries.first().contains("posted:pkg5 java.lang.IllegalStateException"))
        assertTrue(entries.last().contains("posted:pkg24 java.lang.IllegalStateException"))
        // The message can quote content, so it isn't stored
        assertFalse(entries.any { "boom" in it })
        assertTrue(CrashLog.breadcrumbs().last().contains("posted:pkg24 nonfatal java.lang.IllegalStateException"))
    }

    @Test
    fun report_hasTheTraceThenTheBreadcrumbs() {
        CrashLog.breadcrumb("state", "expand priority=MEDIA")
        CrashLog.breadcrumb("state", "collapse")
        val report = CrashLog.report(Thread.currentThread(), RuntimeException("crash"))
        assertTrue(report.startsWith("Droidbubble crash at "))
        val trace = report.indexOf("java.lang.RuntimeException: crash")
        val crumbs = report.indexOf("Breadcrumbs (oldest first, elapsedRealtime ms):")
        assertTrue(trace in 0 until crumbs)
        assertTrue(report.indexOf("expand priority=MEDIA") < report.indexOf("state collapse"))
    }

    @Test
    fun islandEvents_leaveNoUserTextInTheReport() {
        IslandStateManager.updateNotification(
            NotificationInfo(key = "0|com.whatsapp|1|null|10", packageName = "com.whatsapp", appName = "WhatsApp",
                title = "SECRET_SENDER", text = "SECRET_BODY")
        )
        IslandStateManager.expand(null)
        IslandStateManager.collapse()
        val report = CrashLog.report(Thread.currentThread(), RuntimeException("x"))
        assertTrue(report.contains("notification kind=NONE keyHash="))
        assertTrue(report.contains("state expand priority=current"))
        assertFalse(report.contains("SECRET_SENDER"))
        assertFalse(report.contains("SECRET_BODY"))
        assertFalse(report.contains("0|com.whatsapp|1"))
    }
}
