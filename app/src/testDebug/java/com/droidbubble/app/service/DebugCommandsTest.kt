package com.droidbubble.app.service

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** B1.1: debug builds keep the test commands, reachable only from inside the app. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DebugCommandsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() = IslandStateManager.resetForTest()

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    @Test
    fun inAppBroadcast_simulateMedia_setsActiveMedia() {
        val receiver = DebugCommands.register(context)
        try {
            context.sendBroadcast(
                Intent(DebugCommands.ACTION_SIMULATE_MEDIA)
                    .setPackage(context.packageName)
                    .putExtra("title", "Test Song")
            )
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Test Song", IslandStateManager.activeMedia.value?.title)
        } finally {
            DebugCommands.unregister(context, receiver)
        }
    }

    @Test
    fun afterUnregister_broadcastIsIgnored() {
        DebugCommands.unregister(context, DebugCommands.register(context))
        context.sendBroadcast(Intent(DebugCommands.ACTION_SIMULATE_MEDIA).setPackage(context.packageName))
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(IslandStateManager.activeMedia.value)
    }

    @Test
    fun registersAllThirteenActions() {
        assertEquals(13, DebugCommands.ACTIONS.size)
    }
}
