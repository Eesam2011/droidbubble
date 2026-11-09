package com.droidbubble.app.service

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.util.CrashLog
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * B11.2: a notification that makes the listener throw is skipped and logged as non-fatal, and the
 * next notification is processed normally.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NotificationGuardTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var service: NotificationReceiverService

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        CrashLog.resetForTest()
        CrashLog.clear(context)
        CrashLog.attach(context)
        service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        service.setSettingsForTest(IslandSettings(showNotifications = true))
    }

    @After
    fun tearDown() {
        CrashLog.clear(context)
        CrashLog.resetForTest()
        IslandStateManager.resetForTest()
    }

    private fun post(fixture: NotificationFixtures.Fixture) {
        service.onNotificationPosted(fixture.sbn)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun aThrowingNotification_isSkippedAndLogged_andTheNextOneWorks() {
        val bad = NotificationFixtures.load(context, "message/telegram_en")
        val good = NotificationFixtures.load(context, "message/whatsapp_ar")
        // Fault injection: reading the bad notification's ranking throws inside the parser
        service.importanceForTest = { key ->
            if (key == bad.key) throw IllegalStateException("ranking exploded") else good.importance
        }

        post(bad)
        val logged = CrashLog.readNonFatal(context)
        assertEquals(1, logged.size)
        assertTrue(logged.single().contains("posted:${bad.sbn.packageName} java.lang.IllegalStateException"))

        post(good)
        assertEquals(good.key, IslandStateManager.activeNotification.value?.key)
    }

    @Test
    fun wrongTypedExtras_doNotStopTheListener() {
        // android.progress and android.title with the wrong types: Bundle's typed getters return
        // defaults for these, but whatever the parser does, the next notification must still work
        val malformed = NotificationFixtures.build(
            context, "malformed/wrong_types",
            JSONObject(
                """
                {
                  "pkg": "com.example.broken",
                  "source": "synthetic (wrong extra types, B11.2)",
                  "importance": 3,
                  "flags": ["ONGOING_EVENT"],
                  "extras": {
                    "android.title": 42,
                    "android.text": true,
                    "android.progress": "half",
                    "android.progressMax": "all",
                    "android.chronometerBase": "soon"
                  }
                }
                """.trimIndent()
            )
        )
        val good = NotificationFixtures.load(context, "message/whatsapp_ar")
        service.importanceForTest = { key -> if (key == good.key) good.importance else malformed.importance }

        post(malformed)
        post(good)
        assertEquals(good.key, IslandStateManager.activeNotification.value?.key)
    }
}
