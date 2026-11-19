package com.droidbubble.app.ui.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

/**
 * Owner review only: renders the phase 8 companion changes (C1.2–C1.6, C3.9) to PNGs.
 * Runs only with `-Pcheckpoint=PHASE8`; writes to the checkpoint output folder, PHASE8/.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h1400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Phase8Render {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val out by lazy { File(System.getProperty("island.checkpoint.out"), "PHASE8").apply { mkdirs() } }
    private val all = PermissionHelper.allPermissions(35)

    @Before
    fun only() = assumeTrue(System.getProperty("island.checkpoint") == "PHASE8")

    private fun show(content: @Composable () -> Unit) {
        rule.setContent {
            DroidbubbleTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) { content() }
            }
        }
        rule.waitForIdle()
    }

    private fun draw(view: View): Bitmap =
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }

    /** The content view, cropped to what was drawn in it (the Box's measured height). */
    private fun save(name: String) {
        val image = rule.runOnIdle {
            val content = rule.activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            draw(content)
        }
        File(out, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun section(items: List<PermissionItem>, skipped: Set<String> = emptySet(), help: Set<String> = emptySet()) {
        show {
            PermissionSection(
                items = items, skipped = skipped, showSkipped = false,
                onAllow = {}, onSkip = {}, onReviewSkipped = {},
                overline = ::setupStepLabel, restrictedHelp = help,
            )
        }
    }

    @Test
    fun c1_2_freshInstall() {
        section(all.filter { it.id != PermissionHelper.BLUETOOTH_CONNECT_ID })
        save("1_setup_fresh_install")
    }

    @Test
    fun c1_2_onlyOptionalLeft_oneSkipped() {
        section(all.filter { it.level != PermissionLevel.REQUIRED && it.id != PermissionHelper.BLUETOOTH_CONNECT_ID },
            skipped = setOf(PermissionHelper.BATTERY_ID))
        save("2_setup_finish_setup_one_skipped")
    }

    @Test
    fun c1_2_onlySkippedLeft() {
        section(all.filter { it.level == PermissionLevel.OPTIONAL && it.id != PermissionHelper.BLUETOOTH_CONNECT_ID },
            skipped = setOf(PermissionHelper.POST_NOTIFICATIONS_ID, PermissionHelper.RECORD_AUDIO_ID))
        save("3_setup_folded_to_one_line")
    }

    @Test
    fun c1_4_restrictedHelp() {
        section(all.filter { it.id == PermissionHelper.NOTIFICATION_LISTENER_ID || it.id == PermissionHelper.ACCESSIBILITY_ID },
            help = setOf(PermissionHelper.NOTIFICATION_LISTENER_ID))
        save("4_restricted_settings_help")
    }

    @Test
    fun c1_3_disclosureDialog() {
        show { AccessibilityDisclosureDialog(onContinue = {}, onDismiss = {}) }
        val image = rule.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog() ?: error("no dialog")
            val behind = draw(rule.activity.window.decorView)
            val front = draw(dialog.window!!.decorView)
            // The dialog over a dimmed screen, centred like the real one
            Bitmap.createBitmap(behind.width, behind.height, Bitmap.Config.ARGB_8888).also {
                val c = Canvas(it)
                c.drawBitmap(behind, 0f, 0f, null)
                c.drawColor(0x99000000.toInt())
                c.drawBitmap(front, ((behind.width - front.width) / 2f), ((behind.height - front.height) / 2f), null)
            }
        }
        File(out, "5_status_bar_layer_disclosure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun c1_5_bluetoothRow() {
        show {
            Column(Modifier.fillMaxWidth()) {
                BluetoothAlertRow(checked = true, needsPermission = true, onCheckedChange = {}, onAllow = {})
            }
        }
        save("6_bluetooth_row_needs_nearby_devices")
    }

    @Test
    fun c1_6_snackbar() {
        show {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                host.showSnackbar("Display over other apps is on. Next: Notification access", actionLabel = "Allow", duration = SnackbarDuration.Indefinite)
            }
            Column(Modifier.fillMaxWidth()) {
                PermissionSection(
                    items = all.filter { it.id == PermissionHelper.NOTIFICATION_LISTENER_ID || it.id == PermissionHelper.RECORD_AUDIO_ID },
                    skipped = emptySet(), showSkipped = false, onAllow = {}, onSkip = {}, onReviewSkipped = {},
                    overline = ::setupStepLabel,
                )
                SnackbarHost(host)
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        save("7_first_run_snackbar")
    }

    @Test
    fun c3_9_accessibilityPage() {
        show { AccessibilityPage(settings = IslandSettings(spokenAlerts = true), onUpdateSettings = {}, onBack = {}) }
        save("8_accessibility_page_spoken_alerts")
    }

    @Test
    fun c1_mainScreen() {
        val vm = MainScreenViewModel(SettingsRepository(ApplicationProvider.getApplicationContext()))
        rule.runOnIdle { vm.checkPermissions(rule.activity) }
        rule.setContent {
            DroidbubbleTheme(darkTheme = false, dynamicColor = false) {
                androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) { MainScreen(viewModel = vm) }
            }
        }
        rule.waitForIdle()
        save("0_main_screen_top")
    }
}
