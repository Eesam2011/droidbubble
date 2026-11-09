package com.droidbubble.app.service

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** B10.2: a burst of media callbacks causes one refresh, and the last event is never lost. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MediaRefreshDebounceTest {

    @Before
    fun setUp() = IslandStateManager.resetForTest()

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    @Test
    fun twentyCallbacksIn50ms_runOnce_andALaterOneRunsAgain() {
        val service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        val looper = shadowOf(Looper.getMainLooper())

        repeat(20) {
            service.refreshMedia()
            looper.idleFor(Duration.ofMillis(2))
        }
        assertEquals("nothing runs inside the window", 0, service.mediaRefreshRunsForTest)
        looper.idleFor(Duration.ofMillis(200))
        assertEquals(1, service.mediaRefreshRunsForTest)

        // A trailing event after the run gets its own run
        service.refreshMedia()
        looper.idleFor(Duration.ofMillis(200))
        assertEquals(2, service.mediaRefreshRunsForTest)
    }
}
