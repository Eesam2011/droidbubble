package com.droidbubble.app.fixtures

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NotificationFixturesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    /** Every file loads, has a source, and its JSON fields read back from the built notification. */
    @Test
    fun everyFixture_roundTrips() {
        val names = NotificationFixtures.allNames()
        assertTrue("no fixtures found", names.isNotEmpty())
        for (name in names) {
            val raw = JSONObject(javaClass.classLoader!!.getResourceAsStream("fixtures/notifications/$name.json")!!.bufferedReader().readText())
            val f = NotificationFixtures.load(context, name)
            val n = f.notification
            assertEquals(name, raw.getString("pkg"), f.sbn.packageName)
            assertTrue("$name: source", f.source.isNotBlank())
            raw.optJSONObject("extras")?.optString("android.title")?.takeIf { it.isNotEmpty() }?.let {
                assertEquals("$name: title", it, n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            }
            if (raw.has("category")) assertEquals("$name: category", raw.getString("category"), n.category)
            raw.optJSONArray("flags")?.let { flags ->
                for (i in 0 until flags.length()) {
                    val flag = when (flags.getString(i)) {
                        "ONGOING_EVENT" -> Notification.FLAG_ONGOING_EVENT
                        "FOREGROUND_SERVICE" -> Notification.FLAG_FOREGROUND_SERVICE
                        else -> 0
                    }
                    assertTrue("$name: flag ${flags.getString(i)}", n.flags and flag == flag)
                }
            }
            val actions = raw.optJSONArray("actions")
            val titles = n.actions?.map { it.title.toString() }.orEmpty()
            assertEquals("$name: actions", (0 until (actions?.length() ?: 0)).map { actions!!.getJSONObject(it).getString("title") }, titles)
            assertTrue("$name: under 2 KB", raw.toString().length < 2048)
        }
    }

    @Test
    fun timesAndTypesFollowTheFormat() {
        val f = NotificationFixtures.load(context, "clock/gclock_timer_paused_de")
        val n = f.notification
        assertEquals(NotificationFixtures.NOW_MS - 31_000L, n.`when`)
        assertEquals(NotificationFixtures.NOW_MS, f.sbn.postTime)
        assertEquals(3, f.importance)
        assertEquals("de-DE", f.locale)
        assertEquals("Pausiert", n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
        assertFalse(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals("android.app.Notification\$DecoratedCustomViewStyle", n.extras.getString(Notification.EXTRA_TEMPLATE))
        assertEquals("Fortsetzen", n.actions.single().title.toString())
    }
}
