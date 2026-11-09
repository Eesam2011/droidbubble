package com.droidbubble.app.util

import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** B5.3: the island follows Android's "High contrast text" as it changes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HighTextContrastTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resolver = context.contentResolver
    private val scope = CoroutineScope(Dispatchers.Main)

    @After
    fun tearDown() {
        scope.cancel()
        Settings.Secure.putInt(resolver, HIGH_TEXT_CONTRAST_KEY, 0)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun emitsTheCurrentValue_thenEachChange() {
        Settings.Secure.putInt(resolver, HIGH_TEXT_CONTRAST_KEY, 0)
        val seen = mutableListOf<Boolean>()
        scope.launch { highTextContrastFlow(resolver).collect { seen += it } }
        idle()
        assertEquals(listOf(false), seen)

        Settings.Secure.putInt(resolver, HIGH_TEXT_CONTRAST_KEY, 1)
        resolver.notifyChange(Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST_KEY), null)
        idle()
        assertEquals(listOf(false, true), seen)

        // A repeat notification with the same value emits nothing
        resolver.notifyChange(Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST_KEY), null)
        idle()
        Settings.Secure.putInt(resolver, HIGH_TEXT_CONTRAST_KEY, 0)
        resolver.notifyChange(Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST_KEY), null)
        idle()
        assertEquals(listOf(false, true, false), seen)
    }
}
