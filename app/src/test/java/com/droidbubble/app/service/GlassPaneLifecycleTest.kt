package com.droidbubble.app.service

import android.content.Intent
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.flow.MutableStateFlow
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

/** B8.3: the glass pane windows (and their URGENT_DISPLAY threads) exist only with Liquid glass on. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GlassPaneLifecycleTest {

    private val settings = MutableStateFlow(IslandSettings())

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

    /** The panes add and remove their windows on their own threads: let those finish too. */
    private fun destroyAndDrain(controller: org.robolectric.android.controller.ServiceController<IslandOverlayService>) {
        controller.destroy()
        repeat(10) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(50)
        }
    }

    private fun idle() {
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    @Test
    fun glassOff_noPanes_andTurningItOnAndOffFollows() {
        settings.value = IslandSettings(liquidGlassCards = false)
        val controller = Robolectric.buildService(IslandOverlayService::class.java, Intent(IslandOverlayService.ACTION_START))
            .create().startCommand(0, 1)
        idle()
        val service = controller.get()
        assertNull("no panes with glass off", service.glassPanesForTest)

        settings.value = IslandSettings(liquidGlassCards = true)
        idle()
        assertNotNull("panes once glass is turned on", service.glassPanesForTest)

        settings.value = IslandSettings(liquidGlassCards = false)
        idle()
        assertNull("panes removed when glass is turned off", service.glassPanesForTest)
        destroyAndDrain(controller)
    }

    @Test
    fun glassOn_atStart_createsPanes() {
        settings.value = IslandSettings(liquidGlassCards = true)
        val controller = Robolectric.buildService(IslandOverlayService::class.java, Intent(IslandOverlayService.ACTION_START))
            .create().startCommand(0, 1)
        idle()
        assertNotNull(controller.get().glassPanesForTest)
        destroyAndDrain(controller)
    }
}
