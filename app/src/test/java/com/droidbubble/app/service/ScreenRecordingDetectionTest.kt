package com.droidbubble.app.service

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * B4.4: recording text alone doesn't start the recording pill. From an unknown app it also needs
 * a running chronometer (or a recording channel or a recorder package, covered by S5.3).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ScreenRecordingDetectionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() = IslandStateManager.resetForTest()

    @After
    fun tearDown() = IslandStateManager.resetForTest()

    private fun recordingAfterPosting(chronometer: Boolean, text: String = "Screen recording in progress"): Boolean {
        val extras = JSONObject()
            .put("android.title", "Screen recording")
            .put("android.text", text)
        if (chronometer) extras.put("android.showChronometer", true)
        val fixture = NotificationFixtures.build(
            context, "inline/unknown_recorder",
            JSONObject()
                .put("pkg", "com.example.capture")
                .put("source", "synthetic (B4.4)")
                .put("importance", 3)
                .put("flags", org.json.JSONArray().put("ONGOING_EVENT"))
                .put("when", 0)
                .put("extras", extras)
        )
        val service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        service.setSettingsForTest(IslandSettings())
        service.importanceForTest = { fixture.importance }
        service.onNotificationPosted(fixture.sbn)
        shadowOf(Looper.getMainLooper()).idle()
        return IslandStateManager.screenRecordingState.value.isRecording
    }

    @Test
    fun unknownApp_recordingTextAndChronometer_isRecording() {
        assertTrue(recordingAfterPosting(chronometer = true))
    }

    @Test
    fun unknownApp_recordingTextOnly_isNotRecording() {
        assertFalse(recordingAfterPosting(chronometer = false))
    }

    @Test
    fun idleRecorder_tapToStart_isNotRecording() {
        assertFalse(recordingAfterPosting(chronometer = true, text = "Screen recorder: tap to start"))
    }
}
