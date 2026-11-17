package com.droidbubble.app.island

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.droidbubble.app.island.harness.IslandFixtures
import com.droidbubble.app.island.harness.IslandHarness
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.KeylineMode
import com.droidbubble.app.model.ScreenRecordingState
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale

/**
 * The owner checkpoint image set (VD0.1): every state the Phase 4 + 6 design reviews (CP1–CP6)
 * look at, at font scale 1.0 and 1.3. The wallpaper is a mid-grey "photo", so the black island's
 * silhouette shows; a few states are also rendered on black and on a white photo.
 * Test-only: it never runs in the normal suite.
 *
 * ```
 * # record the baseline (committed under app/src/test/snapshots/checkpoints/baseline/)
 * bash tools/gradle-locked.sh :app:testDebugUnitTest --tests "*CheckpointScreens*" -Pcheckpoint=all -Pcheckpoint.record=true
 * # render one checkpoint's states and compare them with the baseline
 * bash tools/gradle-locked.sh :app:testDebugUnitTest --tests "*CheckpointScreens*" -Pcheckpoint=CP1
 * ```
 *
 * A comparison run writes `build/outputs/roborazzi/checkpoints/CP<n>/<state>@<fontScale>.png` and
 * an `index.html` contact sheet that puts each image next to its baseline, with the number of
 * pixels that changed.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CheckpointScreens(private val case: Case) {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val h = IslandHarness(rule)
    private val settings = mutableStateOf(case.settings)
    private val fontScale = mutableFloatStateOf(case.font)

    @Before
    fun setUp() {
        assumeTrue("checkpoint set not requested (-Pcheckpoint=CPn|all)", selected(case))
        IslandStateManager.resetForTest()
        IslandTokens.highContrast = case.highContrast
        h.freezeClock()
        h.setContent(settings, fontScale, case.backdrop.brush)
    }

    @After
    fun tearDown() {
        h.unfreezeClock()
        IslandTokens.highContrast = false
        IslandStateManager.resetForTest()
    }

    @Test
    fun render() {
        h.advanceFrozen(100)
        rule.runOnIdle(case.setUp)
        h.advanceFrozen(1500)
        val image = h.islandCrop()
        if (record) {
            write(image, File(BASELINE_DIR, case.fileName))
        } else {
            val outDir = File(OUT_ROOT, filter)
            val out = File(outDir, case.fileName)
            write(image, out)
            File(outDir, case.fileName + ".diff").writeText(diffAgainstBaseline(image))
            writeContactSheet(outDir)
        }
    }

    // ---- states ----

    enum class Backdrop(val label: String, val brush: Brush?) {
        BLACK("black", Brush.verticalGradient(listOf(Color.Black, Color.Black))),
        GREY("grey", photo(0xFF5E6670.toInt(), 0xFF8C8478.toInt(), 0x80B4BCC4.toInt())),
        WHITE("white", photo(0xFFF4F1EA.toInt(), 0xFFFFFFFF.toInt(), 0x70FFE6C2.toInt())),
    }

    class Case(
        val state: String,
        val cps: Set<Int>,
        val font: Float = 1f,
        val backdrop: Backdrop = Backdrop.GREY,
        val settings: IslandSettings = BASE,
        val highContrast: Boolean = false,
        val setUp: () -> Unit,
    ) {
        private val variant = if (backdrop == Backdrop.GREY) state else "${state}__${backdrop.label}"
        val fileName: String get() = "$variant@${String.format(Locale.US, "%.1f", font)}.png"
        override fun toString() = fileName
    }

    companion object {
        private val BASE = IslandSettings(showNotifications = true)

        private val filter: String = System.getProperty("island.checkpoint").orEmpty()
        private val record: Boolean = System.getProperty("island.checkpoint.record") == "true"
        private val OUT_ROOT = File(System.getProperty("island.checkpoint.out") ?: "build/outputs/roborazzi/checkpoints")
        private val BASELINE_DIR = File("src/test/snapshots/checkpoints/baseline")

        private fun selected(case: Case): Boolean = when {
            filter.isEmpty() -> false
            record || filter.equals("all", ignoreCase = true) -> true
            else -> filter.removePrefix("CP").toIntOrNull()?.let { it in case.cps } == true
        }

        // Which checkpoint looks at what: CP1 fit and corners, CP2 compact balance, CP3 cards and
        // color, CP4 icons. CP5 (motion) is judged on videos and CP6 on the settings editor.
        private val COMPACT = setOf(1, 2, 4)
        private val EXPANDED = setOf(1, 3, 4)
        private val ALERT = setOf(2, 4)

        private val ongoingCall get() = IslandFixtures.incomingCall.copy(
            isIncomingCall = false,
            text = "Ongoing call",
            liveTimerBaseMs = IslandClock.wallMs() - 65_000L,
        )

        private fun compact(): List<Case> = listOf(
            Case("compact_idle", COMPACT, settings = BASE.copy(alwaysShowPill = true)) {},
            Case("compact_media", COMPACT) { IslandStateManager.updateMedia(IslandFixtures.song) },
            Case("compact_message", COMPACT) { IslandStateManager.updateNotification(IslandFixtures.message()) },
            Case("compact_message_contact_photo", COMPACT) {
                IslandStateManager.updateNotification(IslandFixtures.message().copy(avatarBitmap = IslandFixtures.art))
            },
            // Owner: a long first name fades at its end; the pill doesn't keep growing (their 90 × 25 dp pill)
            Case("compact_message_long_name", COMPACT, settings = BASE.copy(widthDp = 90, heightDp = 25)) {
                IslandStateManager.updateNotification(IslandFixtures.message().copy(title = "Emianlahzy Hassan", avatarBitmap = IslandFixtures.art))
            },
            Case("compact_message_short_name", COMPACT, settings = BASE.copy(widthDp = 90, heightDp = 25)) {
                IslandStateManager.updateNotification(IslandFixtures.message().copy(title = "Sam Carter", avatarBitmap = IslandFixtures.art))
            },
            Case("compact_timer", COMPACT) { IslandStateManager.updateNotification(IslandFixtures.timer) },
            Case("compact_call", COMPACT) { IslandStateManager.updateNotification(ongoingCall) },
            Case("compact_recorder", COMPACT) { IslandStateManager.updateNotification(IslandFixtures.recorder) },
            Case("compact_screen_recording", COMPACT) {
                IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = true, durationSeconds = 42))
            },
            Case("compact_progress_42", COMPACT) { IslandStateManager.updateProgress(IslandFixtures.progress(42)) },
            Case("compact_flashlight", COMPACT) { IslandStateManager.updateFlashlightState(true, level = 3) },
            Case("compact_ride", COMPACT) { IslandStateManager.updateNotification(IslandFixtures.ride) },
            Case("compact_dual_media_message", COMPACT) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.updateNotification(IslandFixtures.message())
            },
            Case("compact_dual_media_download", COMPACT) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.updateProgress(IslandFixtures.progress(42))
            },
            Case("compact_minimized", COMPACT) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.minimizeIsland()
            },
            Case("compact_notch_media", COMPACT, settings = BASE.copy(islandStyle = IslandSettings.STYLE_NOTCH)) {
                IslandStateManager.updateMedia(IslandFixtures.song)
            },
            Case("alert_charging", ALERT) { IslandStateManager.triggerChargingEvent(80, 18.0) },
            Case("alert_unlock", ALERT) { IslandStateManager.triggerUnlockEvent() },
            Case("alert_ringer_silent", ALERT) { IslandStateManager.triggerRingerModeEvent(IslandStateManager.RingerMode.SILENT) },
            Case("alert_bluetooth", ALERT) { IslandStateManager.triggerBluetoothEvent() },
        )

        private fun expanded(): List<Case> = listOf(
            Case("expanded_media", EXPANDED) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.expand(null)
            },
            Case("expanded_media_art", EXPANDED, settings = BASE.copy(blurredAlbumArtBackground = true)) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.expand(null)
            },
            Case("expanded_message", EXPANDED) {
                IslandStateManager.updateNotification(IslandFixtures.message())
                IslandStateManager.expand(null)
            },
            Case("expanded_progress", EXPANDED) {
                IslandStateManager.updateProgress(IslandFixtures.progress(42))
                IslandStateManager.expand(IslandPriority.PROGRESS)
            },
            Case("expanded_flashlight", EXPANDED) {
                IslandStateManager.updateFlashlightState(true, level = 3)
                IslandStateManager.expand(IslandPriority.FLASHLIGHT)
            },
            Case("expanded_timer", EXPANDED) {
                IslandStateManager.updateNotification(IslandFixtures.timer)
                IslandStateManager.expand(IslandPriority.POP_NOTIFICATION)
            },
            Case("expanded_call", EXPANDED) {
                IslandStateManager.updateNotification(ongoingCall)
                IslandStateManager.expand(null)
            },
            Case("expanded_incoming_call", EXPANDED) { IslandStateManager.updateNotification(IslandFixtures.incomingCall) },
            Case("expanded_alarm", EXPANDED) { IslandStateManager.updateNotification(IslandFixtures.alarmRinging) },
            Case("expanded_times_up", EXPANDED) { IslandStateManager.updateNotification(IslandFixtures.timesUp) },
            Case("expanded_dual", EXPANDED) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.updateNotification(IslandFixtures.message())
                IslandStateManager.expand(null)
            },
        )

        /** CP1 judges the card corners at expandedScale 0.8, 1.1 (the default) and 1.3. */
        private fun scaled(): List<Case> = listOf(0.8f, 1.3f).flatMap { s ->
            val tag = "s" + String.format(Locale.US, "%.1f", s).replace('.', '_')
            val scaledSettings = BASE.copy(expandedScale = s)
            listOf(
                Case("expanded_media__$tag", setOf(1), settings = scaledSettings) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                    IslandStateManager.expand(null)
                },
                Case("expanded_message__$tag", setOf(1), settings = scaledSettings) {
                    IslandStateManager.updateNotification(IslandFixtures.message())
                    IslandStateManager.expand(null)
                },
                Case("expanded_progress__$tag", setOf(1), settings = scaledSettings) {
                    IslandStateManager.updateProgress(IslandFixtures.progress(42))
                    IslandStateManager.expand(IslandPriority.PROGRESS)
                },
                Case("expanded_flashlight__$tag", setOf(1), settings = scaledSettings) {
                    IslandStateManager.updateFlashlightState(true, level = 3)
                    IslandStateManager.expand(IslandPriority.FLASHLIGHT)
                },
            )
        }

        /** CP3: keylines and scrims on black and on bright wallpaper, and the high-contrast outline. */
        private fun backdrops(): List<Case> = listOf(Backdrop.BLACK, Backdrop.WHITE).flatMap { b ->
            listOf(
                Case("compact_media", setOf(3), backdrop = b) { IslandStateManager.updateMedia(IslandFixtures.song) },
                Case("compact_dual_media_message", setOf(3), backdrop = b) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                    IslandStateManager.updateNotification(IslandFixtures.message())
                },
                Case("expanded_media", setOf(3), backdrop = b) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                    IslandStateManager.expand(null)
                },
                Case("expanded_media_art", setOf(3), backdrop = b, settings = BASE.copy(blurredAlbumArtBackground = true)) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                    IslandStateManager.expand(null)
                },
                Case("expanded_message", setOf(3), backdrop = b) {
                    IslandStateManager.updateNotification(IslandFixtures.message())
                    IslandStateManager.expand(null)
                },
            )
        }

        /**
         * CP3 (V6.1): the keyline on (ALWAYS) on black and white wallpaper. OFF is the plain states
         * above; AUTO is ALWAYS or OFF once the overlay service has read the wallpaper.
         */
        private fun keylines(): List<Case> = listOf(Backdrop.BLACK, Backdrop.WHITE).flatMap { b ->
            val on = BASE.copy(keylineMode = KeylineMode.ALWAYS)
            listOf(
                Case("keyline_compact_dual_media_message", setOf(3), backdrop = b, settings = on) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                    IslandStateManager.updateNotification(IslandFixtures.message())
                },
                Case("keyline_compact_notch_media", setOf(3), backdrop = b,
                    settings = on.copy(islandStyle = IslandSettings.STYLE_NOTCH)) {
                    IslandStateManager.updateMedia(IslandFixtures.song)
                },
                Case("keyline_expanded_message", setOf(3), backdrop = b, settings = on) {
                    IslandStateManager.updateNotification(IslandFixtures.message())
                    IslandStateManager.expand(null)
                },
            )
        }

        private fun highContrast(): List<Case> = listOf(
            Case("hc_compact_media", setOf(3), settings = BASE.copy(keylineMode = KeylineMode.ALWAYS), highContrast = true) {
                IslandStateManager.updateMedia(IslandFixtures.song)
            },
            Case("hc_compact_notch_media", setOf(3), highContrast = true,
                settings = BASE.copy(keylineMode = KeylineMode.ALWAYS, islandStyle = IslandSettings.STYLE_NOTCH)) {
                IslandStateManager.updateMedia(IslandFixtures.song)
            },
            Case("hc_compact_dual_media_message", setOf(3), settings = BASE.copy(keylineMode = KeylineMode.ALWAYS), highContrast = true) {
                IslandStateManager.updateMedia(IslandFixtures.song)
                IslandStateManager.updateNotification(IslandFixtures.message())
            },
            Case("hc_expanded_message", setOf(3), settings = BASE.copy(keylineMode = KeylineMode.ALWAYS), highContrast = true) {
                IslandStateManager.updateNotification(IslandFixtures.message())
                IslandStateManager.expand(null)
            },
        )

        private fun atFontScales(cases: List<Case>, scales: List<Float>): List<Case> = cases.flatMap { c ->
            scales.map { f -> Case(c.state, c.cps, f, c.backdrop, c.settings, c.highContrast, c.setUp) }
        }

        val all: List<Case> by lazy {
            val both = listOf(1f, 1.3f)
            atFontScales(compact() + expanded() + backdrops(), both) +
                atFontScales(scaled() + keylines() + highContrast(), listOf(1f))
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = all.map { arrayOf<Any>(it) }

        /** A synthetic "photo": a vertical gradient with a soft highlight near the island. */
        private fun photo(top: Int, bottom: Int, glow: Int): Brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = ComposeShader(
                LinearGradient(0f, 0f, 0f, size.height, top, bottom, android.graphics.Shader.TileMode.CLAMP),
                RadialGradient(size.width * 0.3f, size.height * 0.04f, size.width * 0.4f, glow, glow and 0x00FFFFFF,
                    android.graphics.Shader.TileMode.CLAMP),
                PorterDuff.Mode.SRC_OVER,
            )
        }
    }

    // ---- files ----

    private fun write(image: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** "0", the number of changed pixels, "new" (no baseline) or "size WxH → WxH". */
    private fun diffAgainstBaseline(image: Bitmap): String {
        val baselineFile = File(BASELINE_DIR, case.fileName)
        if (!baselineFile.exists()) return "new"
        val base = BitmapFactory.decodeFile(baselineFile.path) ?: return "new"
        if (base.width != image.width || base.height != image.height) {
            return "size ${base.width}x${base.height} → ${image.width}x${image.height}"
        }
        val a = IntArray(base.width * base.height).also { base.getPixels(it, 0, base.width, 0, 0, base.width, base.height) }
        val b = IntArray(image.width * image.height).also { image.getPixels(it, 0, image.width, 0, 0, image.width, image.height) }
        return a.indices.count { a[it] != b[it] }.toString()
    }

    /** Rewritten after every state, so the last one leaves the complete sheet. */
    private fun writeContactSheet(dir: File) {
        val rows = dir.listFiles { f -> f.name.endsWith(".png") }.orEmpty().sortedBy { it.name }.joinToString("\n") { png ->
            val diff = File(dir, png.name + ".diff").takeIf { it.exists() }?.readText() ?: "?"
            val baseline = File(BASELINE_DIR, png.name).absoluteFile
            val changed = diff != "0"
            """<tr class="${if (changed) "changed" else "same"}"><th>${png.name}<br><small>${if (changed) "changed: $diff" else "unchanged"}</small></th>""" +
                """<td><img src="${baseline.toURI()}"></td><td><img src="${png.name}"></td></tr>"""
        }
        File(dir, "index.html").writeText(
            """
            <!doctype html><meta charset="utf-8"><title>Island checkpoint ${dir.name}</title>
            <style>
              body { background: #1c1c1e; color: #f2f2f7; font: 14px system-ui, sans-serif; margin: 16px; }
              table { border-collapse: collapse; } th, td { padding: 8px; border-bottom: 1px solid #3a3a3c; vertical-align: top; text-align: left; }
              img { max-width: 520px; background: repeating-conic-gradient(#2c2c2e 0 25%, #3a3a3c 0 50%) 0 0 / 16px 16px; }
              tr.changed th small { color: #ffd60a; } tr.same th small { color: #8e8e93; }
            </style>
            <h1>Checkpoint ${dir.name}</h1>
            <table><tr><th>State</th><th>Baseline</th><th>This build</th></tr>
            $rows
            </table>
            """.trimIndent()
        )
    }
}
