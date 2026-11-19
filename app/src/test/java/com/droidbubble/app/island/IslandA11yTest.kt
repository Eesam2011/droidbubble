package com.droidbubble.app.island

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** C3.2: screen-reader detection ignores Droidbubble's own accessibility service. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class IslandA11yTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val am = context.getSystemService(AccessibilityManager::class.java)

    @After
    fun tearDown() {
        IslandA11y.unregister()
        IslandA11y.setScreenReaderOnForTest(false)
        IslandStateManager.timings = IslandTimings.Unscaled
    }

    private fun service(pkg: String, feedback: Int) = AccessibilityServiceInfo().apply {
        feedbackType = feedback
        val ri = ResolveInfo().apply { serviceInfo = ServiceInfo().apply { packageName = pkg; name = "$pkg.Service" } }
        ReflectionHelpers.setField(this, "mResolveInfo", ri)
    }

    @Test
    fun pureRule() {
        assertFalse(IslandA11y.isScreenReader(false, emptyList(), "me"))
        assertFalse("our own service alone", IslandA11y.isScreenReader(false, listOf("me"), "me"))
        assertTrue(IslandA11y.isScreenReader(false, listOf("me", "com.google.android.marvin.talkback"), "me"))
        assertTrue(IslandA11y.isScreenReader(true, emptyList(), "me"))
    }

    @Test
    fun onlyOurOwnServiceOn_isNotAScreenReader() {
        shadowOf(am).setEnabled(true)
        shadowOf(am).setEnabledAccessibilityServiceList(
            listOf(service(context.packageName, AccessibilityServiceInfo.FEEDBACK_GENERIC))
        )
        IslandA11y.register(context)
        assertFalse(IslandA11y.screenReaderOn.value)
    }

    @Test
    fun talkBackService_isAScreenReader() {
        shadowOf(am).setEnabled(true)
        shadowOf(am).setEnabledAccessibilityServiceList(
            listOf(
                service(context.packageName, AccessibilityServiceInfo.FEEDBACK_GENERIC),
                service("com.google.android.marvin.talkback", AccessibilityServiceInfo.FEEDBACK_SPOKEN),
            )
        )
        IslandA11y.register(context)
        assertTrue(IslandA11y.screenReaderOn.value)
    }

    @Test
    fun touchExplorationFlipsTheFlow() {
        IslandA11y.register(context)
        assertFalse(IslandA11y.screenReaderOn.value)
        shadowOf(am).setTouchExplorationEnabled(true)
        assertTrue(IslandA11y.screenReaderOn.value)
        shadowOf(am).setTouchExplorationEnabled(false)
        assertFalse(IslandA11y.screenReaderOn.value)
    }

    @Test
    fun recommendedTimeoutGoesThroughIslandTimings() {
        assertEquals(3_000L, IslandA11y.recommendedTimeoutMs(3_000))
        IslandStateManager.timings = IslandTimings { ms, _ -> ms * 4 }
        assertEquals(12_000L, IslandA11y.recommendedTimeoutMs(3_000))
    }
}
