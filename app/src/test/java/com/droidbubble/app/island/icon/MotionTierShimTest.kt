package com.droidbubble.app.island.icon

import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.motion.rememberMotionTier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * I0.1: the icon engine reads the tier M5.1 already resolves (no separate shim is needed).
 * "Remove animations" gives Reduced; 0.5x stays Full (owner decision, 2026-11-12).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MotionTierShimTest {

    @get:Rule
    val rule = createComposeRule()

    private fun tierWith(scale: Float): MotionTier {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        val seen = mutableStateOf<MotionTier?>(null)
        rule.setContent { seen.value = rememberMotionTier(reduceMotionSetting = false) }
        rule.waitForIdle()
        return seen.value!!
    }

    @Test
    fun removeAnimations_isReduced() = assertEquals(MotionTier.REDUCED, tierWith(0f))

    @Test
    fun halfSpeedAnimations_stayFull() = assertEquals(MotionTier.FULL, tierWith(0.5f))

    // Battery Saver → Efficient is covered by MotionTierTest (AudioPowerPolicy.lowPower is set by its receiver only)
}
