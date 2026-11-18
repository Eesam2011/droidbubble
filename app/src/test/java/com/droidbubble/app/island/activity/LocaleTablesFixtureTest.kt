package com.droidbubble.app.island.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.activity.controls.LocaleTables
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A4.2: LocaleTablesTest's table lists every button of those fixtures (read from the JSON), and
 * Robolectric gives the same roles as the plain JVM.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LocaleTablesFixtureTest {

    @Test
    fun tableMatchesTheFixtures_andRobolectricAgrees() {
        val table = LocaleTablesTest()
        for ((name, roles) in table.fixtureRoles) {
            val stream = javaClass.classLoader!!.getResourceAsStream("fixtures/notifications/$name.json")!!
            val json = JSONObject(stream.bufferedReader().use { it.readText() })
            val actions = json.getJSONArray("actions")
            val titles = (0 until actions.length()).map { actions.getJSONObject(it).getString("title") }.toSet()
            assertEquals("$name: the table lists every button", titles, roles.keys)
            assertEquals("$name is a call", json.optString("category") == "call", name in table.callFixtures)
            for ((title, role) in roles) assertEquals("$name: $title", role, LocaleTables.match(title, name in table.callFixtures))
        }
    }
}
