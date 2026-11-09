package com.droidbubble.app.ui.main

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** B3.2: the settings screen's Live Activities switch persists through the view model. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MainScreenViewModelSettingsTest {

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
    fun liveActivitiesSwitch_persists() {
        val repository = SettingsRepository(context)
        val viewModel = MainScreenViewModel(repository)

        viewModel.updateSettings { it.copy(showLiveActivities = false) }
        awaitStored(repository) { !it.showLiveActivities }

        viewModel.updateSettings { it.copy(showLiveActivities = true) }
        awaitStored(repository) { it.showLiveActivities }
    }
}
