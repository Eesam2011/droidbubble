package com.droidbubble.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** B3.1: the live-activities setting round-trips, and a store without the key reads as on. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun showLiveActivities_missingKeyIsOn_andRoundTrips() = runBlocking {
        val repository = SettingsRepository(context)
        // Settings written before B3.1 have no key: live activities are on
        assertTrue(repository.settingsFlow.first().showLiveActivities)

        repository.updateSettings { it.copy(showLiveActivities = false) }
        assertFalse(repository.settingsFlow.first().showLiveActivities)

        repository.updateSettings { it.copy(showLiveActivities = true) }
        assertTrue(repository.settingsFlow.first().showLiveActivities)
    }
}
