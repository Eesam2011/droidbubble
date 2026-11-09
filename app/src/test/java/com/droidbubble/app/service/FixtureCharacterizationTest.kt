package com.droidbubble.app.service

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.fixtures.NotificationFixtures
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.liveActivityControls
import com.droidbubble.app.island.liveActivityKind
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.service.golden.FixtureGolden
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * Every notification fixture (S5) posted into the real NotificationReceiverService, with the
 * resulting island state recorded per settings column and compared with [FixtureGolden] (S5.3).
 * Phase 1 heuristic fixes show up as row diffs here.
 *
 * Columns: `messages_on` (Incoming Notifications on) and `defaults` (a fresh install).
 * Record mode rewrites the golden file:
 * `./gradlew :app:testDebugUnitTest --tests "*FixtureCharacterizationTest" -Pisland.record=true`
 *
 * The service is built without `onCreate`, so the DataStore settings collector can't overwrite
 * the test settings, and a fresh one is used per fixture (it keeps call and recording state).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FixtureCharacterizationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val columns = listOf(
        "messages_on" to IslandSettings(showNotifications = true),
        "defaults" to IslandSettings(),
    )

    /** Rows that encode a known bug, with the task that fixes it. That task flips the row. */
    private val knownIssues = buildMap {
        // A4: non-English call buttons and screen-recording texts aren't understood
        put("call/whatsapp_incoming_ar|messages_on", "A4")
        put("call/whatsapp_video_es|messages_on", "A4")
        put("call/samsung_incoming_tr|messages_on", "A4")
        // Since B3.1 the defaults column shows calls and live activities too, so the same bugs show there
        put("call/whatsapp_incoming_ar|defaults", "A4")
        put("call/whatsapp_video_es|defaults", "A4")
        put("call/samsung_incoming_tr|defaults", "A4")
        put("screenrec/samsung_es|messages_on", "A4")
        put("screenrec/samsung_es|defaults", "A4")
        put("screenrec/xiaomi_id|messages_on", "A4")
        put("screenrec/xiaomi_id|defaults", "A4")
    }

    @Before
    fun setUp() {
        NotificationReceiverService.parserDispatcherForTest = kotlinx.coroutines.Dispatchers.Unconfined
        IslandStateManager.resetForTest()
    }

    @After
    fun tearDown() {
        NotificationReceiverService.parserDispatcherForTest = null
        IslandStateManager.resetForTest()
    }

    @Test
    fun everyFixture_matchesGolden() {
        val recorded = linkedMapOf<String, String>()
        for (name in NotificationFixtures.allNames()) {
            for ((column, settings) in columns) {
                val key = "$name|$column"
                val issue = knownIssues[key]?.let { " knownIssue=$it" }.orEmpty()
                recorded[key] = post(name, settings) + issue
            }
        }
        if (System.getProperty("island.record") == "true") {
            writeGolden(recorded)
            return
        }
        val diffs = recorded.mapNotNull { (key, actual) ->
            val expected = FixtureGolden.rows[key]
            if (expected == actual) null else "$key\n  expected: $expected\n  actual:   $actual"
        } + FixtureGolden.rows.keys.filter { it !in recorded }.map { "$it missing from the run" }
        if (diffs.isNotEmpty()) fail("${diffs.size} rows differ from FixtureGolden:\n" + diffs.joinToString("\n"))
    }

    private fun post(name: String, settings: IslandSettings): String {
        IslandStateManager.resetForTest()
        val fixture = NotificationFixtures.load(context, name)
        val service = Robolectric.buildService(NotificationReceiverService::class.java).get()
        service.setSettingsForTest(settings)
        service.importanceForTest = { key -> if (key == fixture.key) fixture.importance else null }
        service.onNotificationPosted(fixture.sbn)
        shadowOf(Looper.getMainLooper()).idle()

        val notification = IslandStateManager.activeNotification.value?.takeIf { it.key == fixture.key }
        val progress = IslandStateManager.activeProgress.value?.takeIf { it.key == fixture.key }
        val info = notification ?: progress
        val flow = when {
            notification != null -> "notification"
            progress != null -> "progress"
            IslandStateManager.activeCallState.value.isActive -> "ongoing_call"
            IslandStateManager.screenRecordingState.value.isRecording -> "screen_recording"
            else -> "none"
        }
        if (info == null) return "flow=$flow"
        val controls = liveActivityControls(info)
        return "flow=$flow kind=${liveActivityKind(info)} incoming=${info.isIncomingCall} " +
            "answer=${info.answerIntent != null} decline=${info.declineIntent != null} " +
            "pause=${controls.pauseResume != null} stop=${controls.stop != null}"
    }

    private fun writeGolden(rows: Map<String, String>) {
        val file = File("src/test/java/com/droidbubble/app/service/golden/FixtureGolden.kt")
        val body = rows.entries.joinToString("\n") { (k, v) -> "        \"$k\" to \"$v\"," }
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.droidbubble.app.service.golden
            |
            |/**
            | * Recorded by FixtureCharacterizationTest (S5.3). Key "<fixture>|<column>". Value: which island
            | * flow received the notification, its live-activity kind, call flags and pause/stop controls.
            | * `knownIssue=<task>` marks a row that task will change.
            | * Re-record: ./gradlew :app:testDebugUnitTest --tests "*FixtureCharacterizationTest" -Pisland.record=true
            | */
            |object FixtureGolden {
            |    val rows: Map<String, String> = mapOf(
            |$body
            |    )
            |}
            |""".trimMargin()
        )
        println("Recorded ${rows.size} rows into ${file.absolutePath}")
    }
}
