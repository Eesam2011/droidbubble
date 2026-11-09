package com.droidbubble.app.island

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** B6.3: a card opens its own intent first; the app's launcher is only the fallback. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OpenTargetTest {

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun chatIntent(): PendingIntent = PendingIntent.getActivity(
        app, 7, Intent(app, MainActivity::class.java).setAction(CHAT), PendingIntent.FLAG_IMMUTABLE
    )

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    @Test
    fun ownIntent_isSent_andLauncherNotUsed() {
        assertTrue(openTarget(app, chatIntent(), app.packageName))
        assertEquals(CHAT, nextStarted()?.action)
        assertNull("the launcher must not start as well", nextStarted())
    }

    @Test
    fun noOwnIntent_opensTheLauncher() {
        assertTrue(openTarget(app, null, app.packageName))
        val started = nextStarted()
        assertEquals(Intent.ACTION_MAIN, started?.action)
        assertEquals(MainActivity::class.java.name, started?.component?.className)
    }

    @Test
    fun cancelledOwnIntent_fallsBackToTheLauncher() {
        val pi = chatIntent().also { it.cancel() }
        assertTrue(openTarget(app, pi, app.packageName))
        assertEquals(Intent.ACTION_MAIN, nextStarted()?.action)
    }

    @Test
    fun nothingToOpen_returnsFalse() {
        assertFalse(openTarget(app, null, null))
        assertFalse(openTarget(app, null, "com.example.not.installed"))
        assertNull(nextStarted())
    }

    private companion object {
        const val CHAT = "test.OPEN_CHAT"
    }
}
