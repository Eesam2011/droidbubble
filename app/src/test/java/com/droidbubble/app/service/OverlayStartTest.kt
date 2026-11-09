package com.droidbubble.app.service

import android.content.Intent
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/**
 * B8.4: the service starts without reading settings on the main thread. The overlay isn't added
 * until the first real settings arrive, and then only if the island is enabled.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class OverlayStartTest {

    private val settings = MutableSharedFlow<IslandSettings>(replay = 1)

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandOverlayService.settingsFlowForTest = settings
        ShadowSettings.setCanDrawOverlays(true)
    }

    @After
    fun tearDown() {
        IslandOverlayService.settingsFlowForTest = null
        IslandStateManager.resetForTest()
    }

    private fun idle() = repeat(3) { shadowOf(Looper.getMainLooper()).idle() }

    @Test
    fun overlayWaitsForTheFirstSettings() {
        val controller = Robolectric.buildService(IslandOverlayService::class.java, Intent(IslandOverlayService.ACTION_START))
            .create().startCommand(0, 1)
        idle()
        val service = controller.get()
        assertNull("no overlay before the settings arrive", service.overlayViewForTest)

        settings.tryEmit(IslandSettings())
        idle()
        assertNotNull("overlay added with the first settings", service.overlayViewForTest)
        controller.destroy()
        idle()
    }

    @Test
    fun disabledIsland_neverShows() {
        val controller = Robolectric.buildService(IslandOverlayService::class.java, Intent(IslandOverlayService.ACTION_START))
            .create().startCommand(0, 1)
        settings.tryEmit(IslandSettings(isEnabled = false))
        idle()
        assertNull(controller.get().overlayViewForTest)
        controller.destroy()
        idle()
    }
}
