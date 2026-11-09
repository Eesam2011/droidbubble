package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.island.golden.IslandBoundsGolden
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandSettings
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The settled bounds and content matrix (S1.3): 19 island states × 6 settings. Each case records
 * the settled PILL, BUBBLE and CARD bounds (rounded to 0.5 dp) and which activity each content
 * slot draws, and compares them with [IslandBoundsGolden]. Later refactors (F1, M1, the V tasks)
 * must reproduce the table or list their diffs.
 *
 * Record mode rewrites the golden file:
 * `./gradlew :app:testDebugUnitTest --tests "*IslandCharacterizationTest" -Pisland.record=true`
 *
 * Columns use `showNotifications = true` as their base: with the fresh-install default (off),
 * every notification row would be empty. The fresh-install gate is covered by S3.2 and S5.3.
 * Only settled frames are compared, never mid-morph (rememberMorphSpeed reads the wall clock).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IslandCharacterizationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(BASE)
    private val fontScale = mutableFloatStateOf(1f)

    private class State(val name: String, val knownIssue: String? = null, val setUp: () -> Unit)
    private class Column(val name: String, val settings: IslandSettings, val highContrast: Boolean = false, val fontScale: Float = 1f)

    private val states = listOf(
        State("idle_alwaysShowPill") { /* the column turns alwaysShowPill on */ },
        State("media_playing") { IslandStateManager.updateMedia(IslandFixtures.song) },
        State("media_paused") { IslandStateManager.updateMedia(IslandFixtures.song.copy(isPlaying = false)) },
        State("message") { IslandStateManager.updateNotification(IslandFixtures.message()) },
        State("download_40") { IslandStateManager.updateProgress(IslandFixtures.progress(40)) },
        State("voice_recorder") { IslandStateManager.updateNotification(IslandFixtures.recorder) },
        State("timer_running") { IslandStateManager.updateNotification(IslandFixtures.timer) },
        State("incoming_call") { IslandStateManager.updateNotification(IslandFixtures.incomingCall) },
        State("incoming_call_torch_blink") {
            IslandStateManager.updateNotification(IslandFixtures.incomingCall)
            repeat(6) { IslandStateManager.onTorchChanged(it % 2 == 0) }
        },
        State("media_and_message") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.message())
        },
        State("media_recorder_download", knownIssue = "A2.4: the third activity is hidden") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.recorder)
            IslandStateManager.updateProgress(IslandFixtures.progress(40))
        },
        State("minimized") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.minimizeIsland()
        },
        State("hidden") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.hideIsland()
        },
        State("media_app_in_foreground") {
            IslandStateManager.setForegroundPackage(IslandFixtures.song.packageName)
            IslandStateManager.updateMedia(IslandFixtures.song)
        },
        State("charging_glance") { IslandStateManager.triggerChargingEvent(80, 18.0) },
        State("ringer_glance") { IslandStateManager.triggerRingerModeEvent(IslandStateManager.RingerMode.VIBRATE) },
        State("expanded_media") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.expand(null)
        },
        State("expanded_message") {
            IslandStateManager.updateNotification(IslandFixtures.message())
            IslandStateManager.expand(null)
        },
        State("expanded_dual") {
            IslandStateManager.updateMedia(IslandFixtures.song)
            IslandStateManager.updateNotification(IslandFixtures.message())
            IslandStateManager.expand(null)
        },
    )

    private val columns = listOf(
        Column("default", BASE),
        Column("notch", BASE.copy(islandStyle = IslandSettings.STYLE_NOTCH)),
        Column("size_220x40", BASE.copy(widthDp = 220, heightDp = 40)),
        Column("expandedScale_1_0", BASE.copy(expandedScale = 1.0f)),
        Column("high_contrast", BASE.copy(showOutline = true), highContrast = true),
        Column("font_1_3", BASE, fontScale = 1.3f),
    )

    @Before
    fun setUp() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
        rule.mainClock.autoAdvance = true
        h.setContent(settings, fontScale)
    }

    @After
    fun tearDown() {
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = false
    }

    @Test
    fun settledBoundsAndContent_matchGolden() {
        val recorded = linkedMapOf<String, String>()
        for (column in columns) {
            for (state in states) {
                recorded["${state.name}|${column.name}"] = record(state, column)
            }
        }
        if (System.getProperty("island.record") == "true") {
            writeGolden(recorded)
            return
        }
        val diffs = recorded.mapNotNull { (key, actual) ->
            val expected = IslandBoundsGolden.rows[key]
            if (expected == actual) null else "$key\n  expected: $expected\n  actual:   $actual"
        } + IslandBoundsGolden.rows.keys.filter { it !in recorded }.map { "$it missing from the run" }
        if (diffs.isNotEmpty()) fail("${diffs.size} cases differ from IslandBoundsGolden:\n" + diffs.joinToString("\n"))
    }

    private fun record(state: State, column: Column): String {
        rule.runOnIdle {
            IslandStateManager.resetForTest()
            IslandTokens.highContrast = column.highContrast
            settings.value = if (state.name == "idle_alwaysShowPill") column.settings.copy(alwaysShowPill = true) else column.settings
            fontScale.floatValue = column.fontScale
        }
        rule.mainClock.advanceTimeBy(100)
        rule.waitForIdle()
        rule.runOnIdle { state.setUp() }
        rule.mainClock.advanceTimeBy(1500)
        rule.waitForIdle()
        val bounds = listOf("pill" to IslandTestTags.PILL, "bubble" to IslandTestTags.BUBBLE, "card" to IslandTestTags.CARD)
            .joinToString(" ") { (label, tag) -> "$label=${format(h.bounds(tag))}" }
        val content = IslandTestTags.CONTENT_SLOTS.joinToString(" ") { slot -> "$slot:${h.contentOf(slot)?.name ?: "-"}" }
        val issue = state.knownIssue?.let { " knownIssue=${it.substringBefore(':')}" }.orEmpty()
        return "$bounds | $content$issue"
    }

    private fun format(r: DpRect?): String {
        if (r == null) return "-"
        fun half(v: Float) = String.format(Locale.US, "%.1f", (v * 2f).roundToInt() / 2f)
        return "${half(r.left.value)},${half(r.top.value)},${half(r.right.value - r.left.value)}x${half(r.bottom.value - r.top.value)}"
    }

    private fun writeGolden(rows: Map<String, String>) {
        val file = File("src/test/java/com/droidbubble/app/island/golden/IslandBoundsGolden.kt")
        val body = rows.entries.joinToString("\n") { (k, v) -> "        \"$k\" to \"$v\"," }
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.droidbubble.app.island.golden
            |
            |/**
            | * Recorded by IslandCharacterizationTest (S1.3). Format per case "<state>|<column>":
            | * `pill=left,top,WxH bubble=... card=... | pill:<priority> bubble:... card1:... card2:...`
            | * in dp, rounded to 0.5. `-` means absent. `knownIssue=<task>` marks a row a task will flip.
            | * Re-record: ./gradlew :app:testDebugUnitTest --tests "*IslandCharacterizationTest" -Pisland.record=true
            | */
            |object IslandBoundsGolden {
            |    val rows: Map<String, String> = mapOf(
            |$body
            |    )
            |}
            |""".trimMargin()
        )
        println("Recorded ${rows.size} cases into ${file.absolutePath}")
    }

    private companion object {
        val BASE = IslandSettings(showNotifications = true)
    }
}
