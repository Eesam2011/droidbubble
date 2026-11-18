package com.droidbubble.app.island.activity

import android.app.Notification
import android.os.Bundle
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.activity.adapters.AdapterContext
import com.droidbubble.app.island.activity.adapters.AdapterOutcome
import com.droidbubble.app.island.activity.adapters.AdapterRegistry
import com.droidbubble.app.island.activity.adapters.LegacyAdapter
import com.droidbubble.app.island.activity.adapters.NotificationAdapter
import com.droidbubble.app.island.activity.adapters.NotificationSnapshot
import com.droidbubble.app.island.activity.adapters.SnapshotDetails
import com.droidbubble.app.island.activity.adapters.SnapshotReader
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.util.CrashLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A3.1: SnapshotReader, the adapter chain and LegacyAdapter. Parity with the old listener code is
 * FixtureCharacterizationTest's golden (every fixture through the real listener, unchanged).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AdapterChainTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun ctx(settings: IslandSettings = IslandSettings(showNotifications = true), foreground: String? = null) = AdapterContext(
        settings = settings,
        foregroundPackage = foreground,
        customViewTimer = { null },
        details = { SnapshotDetails(appName = "App", iconBitmap = null, avatarBitmap = null, callPersonName = null, callPersonUri = null) },
        stabilizeBase = { _, raw, _ -> raw },
    )

    private fun snapshot(name: String): NotificationSnapshot {
        val f = NotificationFixtures.load(context, name)
        return SnapshotReader.read(f.sbn, lowImportance = f.importance?.let { it < 3 } == true, iconName = { null })
    }

    // ---- SnapshotReader ----

    @Test
    fun reader_callFixture() {
        val s = snapshot("call/gphone_incoming_en")
        assertEquals("com.google.android.dialer", s.packageName)
        assertEquals(1, s.callType)
        assertTrue(s.template!!.contains("CallStyle"))
        assertTrue(s.answerIntent != null)
    }

    @Test
    fun reader_progressStyleSegments() {
        val s = snapshot("progress/uber_progressstyle_36_en")
        assertTrue(s.template!!.contains("ProgressStyle"))
        assertEquals("5 min", s.shortCriticalText)
        // A real segment list sums its lengths
        val extras = Bundle().apply {
            putString(Notification.EXTRA_TEMPLATE, "android.app.Notification\$ProgressStyle")
            putParcelableArrayList("android.progressSegments", arrayListOf(
                Bundle().apply { putInt("length", 30) }, Bundle().apply { putInt("length", 70) },
            ))
        }
        val n = Notification.Builder(context, "c").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        n.extras.putAll(extras)
        @Suppress("DEPRECATION")
        val sbn = StatusBarNotification("com.ubercab", "com.ubercab", 2, null, 0, 0, 0, n, UserHandle.getUserHandleForUid(0), 0L)
        assertEquals(100, SnapshotReader.read(sbn, lowImportance = false, iconName = { null }).progressSegmentsMax)
    }

    @Test
    fun reader_malformedExtra_givesDefaultAndBreadcrumb() {
        val extras = Bundle().apply {
            putString(Notification.EXTRA_TEMPLATE, "android.app.Notification\$ProgressStyle")
            putString("android.progressSegments", "not a list")
            putCharSequence(Notification.EXTRA_TITLE, "Driver arriving")
        }
        val n = Notification.Builder(context, "c").setSmallIcon(android.R.drawable.ic_dialog_info).build()
        n.extras.putAll(extras)
        @Suppress("DEPRECATION")
        val sbn = StatusBarNotification("com.ubercab", "com.ubercab", 1, null, 0, 0, 0, n, UserHandle.getUserHandleForUid(0), 0L)
        val s = SnapshotReader.read(sbn, lowImportance = false, iconName = { null })
        assertEquals(0, s.progressSegmentsMax)
        assertEquals("Driver arriving", s.title)
        assertTrue(CrashLog.breadcrumbs().any { "progressSegments unreadable" in it })
    }

    // ---- the chain ----

    private class Fixed(override val name: String, val accept: Boolean, val outcome: () -> AdapterOutcome?) : NotificationAdapter {
        override fun accepts(n: NotificationSnapshot) = accept
        override fun adapt(n: NotificationSnapshot, ctx: AdapterContext) = outcome()
    }

    @Test
    fun chain_firstAcceptingAdapterWins() {
        val s = snapshot("message/telegram_en")
        val chain = listOf(
            Fixed("skips", accept = false) { error("never asked") },
            Fixed("drops", accept = true) { AdapterOutcome.Drop },
            LegacyAdapter,
        )
        assertEquals(AdapterOutcome.Drop, AdapterRegistry.adapt(s, ctx(), chain))
    }

    @Test
    fun chain_aCrashingAdapterIsContained() {
        val s = snapshot("message/telegram_en")
        val chain = listOf(Fixed("boom", accept = true) { error("bad extra") }, LegacyAdapter)
        val outcome = AdapterRegistry.adapt(s, ctx(), chain)
        assertTrue(outcome is AdapterOutcome.Post)
        assertTrue(CrashLog.breadcrumbs().any { "boom failed" in it })
        assertNull(AdapterRegistry.adapt(s, ctx(), listOf(Fixed("boom", accept = true) { error("x") })))
    }

    @Test
    fun registry_legacyIsLast() {
        assertEquals(LegacyAdapter, AdapterRegistry.chain.last())
    }

    // ---- LegacyAdapter (the same kinds as LegacySpecMapperTest, from snapshots) ----

    @Test
    fun legacy_kindsFromSnapshots() {
        val expected = mapOf(
            "call/gphone_incoming_en" to ActivityKind.CALL_RINGING,
            "clock/gclock_timer_running_en" to ActivityKind.TIMER,
            "clock/gclock_timesup_fr" to ActivityKind.ALERT,
            "recorder/grecorder_en" to ActivityKind.VOICE_RECORDING,
            "progress/play_download_en" to ActivityKind.PROGRESS,
            "progress/uber_progressstyle_36_en" to ActivityKind.TRACKING,
            "message/telegram_en" to ActivityKind.MESSAGE,
        )
        for ((name, kind) in expected) {
            val outcome = AdapterRegistry.adapt(snapshot(name), ctx())
            assertTrue("$name is kept", outcome is AdapterOutcome.Post)
            assertEquals(name, kind, (outcome as AdapterOutcome.Post).spec.kind)
        }
    }

    @Test
    fun legacy_messageFromTheAppOnScreen_isIgnored_andMessagesOff_isDropped() {
        val s = snapshot("message/telegram_en")
        assertEquals(AdapterOutcome.Ignore, AdapterRegistry.adapt(s, ctx(foreground = s.packageName)))
        assertEquals(AdapterOutcome.Drop, AdapterRegistry.adapt(s, ctx(IslandSettings(showNotifications = false))))
    }

    @Test
    fun legacy_detailsReadOnlyForKeptNotifications() {
        var reads = 0
        val counting = AdapterContext(
            settings = IslandSettings(showNotifications = false), foregroundPackage = null, customViewTimer = { null },
            details = { reads++; SnapshotDetails("App", null, null, null, null) }, stabilizeBase = { _, raw, _ -> raw },
        )
        AdapterRegistry.adapt(snapshot("message/telegram_en"), counting)
        assertEquals(0, reads)
        assertFalse(AdapterRegistry.adapt(snapshot("message/telegram_en"), ctx()) is AdapterOutcome.Drop)
    }
}
