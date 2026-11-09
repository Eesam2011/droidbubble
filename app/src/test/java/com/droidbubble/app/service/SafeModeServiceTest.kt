package com.droidbubble.app.service

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.util.SafeMode
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

/** B11.3: while safe mode pauses a package, its notifications never reach the island. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SafeModeServiceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        NotificationReceiverService.parserDispatcherForTest = kotlinx.coroutines.Dispatchers.Unconfined
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        NotificationReceiverService.parserDispatcherForTest = null
        IslandStateManager.resetForTest()
        context.getSharedPreferences("safe_mode", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun message(pkg: String, name: String) = NotificationFixtures.build(
        context, name,
        JSONObject().put("pkg", pkg).put("source", "synthetic (B11.3)").put("importance", 3)
            .put("extras", JSONObject().put("android.title", "Sam").put("android.text", "Hi"))
    ).sbn

    @Test
    fun pausedPackage_isSkipped_othersStillShow() {
        val now = System.currentTimeMillis()
        repeat(3) { SafeMode.recordCrash(context, now - 60_000L, "NONE|com.example.crashy") }
        SafeMode.evaluateOnStart(context, now)

        val service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        service.setSettingsForTest(IslandSettings(showNotifications = true))
        service.importanceForTest = { 3 }

        service.onNotificationPosted(message("com.example.crashy", "inline/crashy"))
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(IslandStateManager.activeNotification.value)

        service.onNotificationPosted(message("com.example.fine", "inline/fine"))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("com.example.fine", IslandStateManager.activeNotification.value?.packageName)
    }
}
