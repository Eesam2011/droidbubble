package com.droidbubble.app.service

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.CoroutineDispatcher
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.coroutines.CoroutineContext

/**
 * B10.3: parsing runs on the parser dispatcher and results apply on main in post order. Here the
 * parser is a queue the test drains in any order, to make parses finish out of order.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ParserOrderingTest {

    private class QueueDispatcher : CoroutineDispatcher() {
        val tasks = ArrayList<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            tasks += block
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val parser = QueueDispatcher()
    private lateinit var service: NotificationReceiverService

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        NotificationReceiverService.parserDispatcherForTest = parser
        service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        service.setSettingsForTest(IslandSettings(showNotifications = true))
        service.importanceForTest = { 3 }
    }

    @After
    fun tearDown() {
        NotificationReceiverService.parserDispatcherForTest = null
        IslandStateManager.resetForTest()
    }

    /** The same notification key each time, with different text. */
    private fun message(text: String) = NotificationFixtures.build(
        context, "inline/chat",
        JSONObject()
            .put("pkg", "com.example.chat")
            .put("source", "synthetic (B10.3)")
            .put("importance", 3)
            .put("extras", JSONObject().put("android.title", "Sam").put("android.text", text))
    ).sbn

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun laterPostWins_whenItsParseFinishesFirst() {
        service.onNotificationPosted(message("first"))
        service.onNotificationPosted(message("second"))
        assertEquals(2, parser.tasks.size)
        // The second parse finishes first, then the slow first one
        parser.tasks[1].run()
        idleMain()
        parser.tasks[0].run()
        idleMain()
        assertEquals("second", IslandStateManager.activeNotification.value?.text)
    }

    @Test
    fun removedWhileParsing_staysRemoved() {
        service.onNotificationPosted(message("first"))
        service.onNotificationPosted(message("second"))
        service.onNotificationRemoved(message("second"))
        parser.tasks.forEach { it.run() }
        idleMain()
        assertNull(IslandStateManager.activeNotification.value)
    }

    @Test
    fun nothingAppliesBeforeTheParseRuns() {
        service.onNotificationPosted(message("first"))
        idleMain()
        assertNull("the parse hasn't run yet", IslandStateManager.activeNotification.value)
        parser.tasks.single().run()
        idleMain()
        assertEquals("first", IslandStateManager.activeNotification.value?.text)
    }
}
