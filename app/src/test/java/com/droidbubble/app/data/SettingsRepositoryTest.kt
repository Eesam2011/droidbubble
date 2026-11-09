package com.droidbubble.app.data

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.ui.main.MainScreenViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * B3.1 / B3.2: the live-activities setting. A store without the key reads as on, the repository
 * round-trips it, and the settings screen's switch persists through the view model.
 *
 * One test method on purpose: the `preferencesDataStore` delegate is a process singleton bound to
 * the first test's files dir, so a second test touching it in the same JVM hits a stale, locked
 * file on Windows. Keep every DataStore check in this method.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** The view model's coroutine resumes on the main looper, so drive it while waiting. */
    private fun awaitStored(repository: SettingsRepository, predicate: (IslandSettings) -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (predicate(runBlocking { repository.settingsFlow.first() })) return
            if (System.currentTimeMillis() > deadline) fail("setting was not stored")
            Thread.sleep(20)
        }
    }

    @Test
    fun showLiveActivities_defaultsOn_roundTrips_andPersistsFromTheSwitch() {
        val repository = SettingsRepository(context)

        // Settings written before B3.1 have no key: live activities are on
        assertTrue(runBlocking { repository.settingsFlow.first() }.showLiveActivities)

        runBlocking { repository.updateSettings { it.copy(showLiveActivities = false) } }
        assertFalse(runBlocking { repository.settingsFlow.first() }.showLiveActivities)
        runBlocking { repository.updateSettings { it.copy(showLiveActivities = true) } }
        assertTrue(runBlocking { repository.settingsFlow.first() }.showLiveActivities)

        // B3.2: the Live Activities switch writes through MainScreenViewModel
        val viewModel = MainScreenViewModel(repository)
        viewModel.updateSettings { it.copy(showLiveActivities = false) }
        awaitStored(repository) { !it.showLiveActivities }
        viewModel.updateSettings { it.copy(showLiveActivities = true) }
        awaitStored(repository) { it.showLiveActivities }
    }
}
