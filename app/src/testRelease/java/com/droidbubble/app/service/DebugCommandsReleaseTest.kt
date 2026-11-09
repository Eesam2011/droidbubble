package com.droidbubble.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** B1.1: release builds register no debug receiver. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DebugCommandsReleaseTest {

    @Test
    fun register_returnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNull(DebugCommands.register(context))
    }
}
