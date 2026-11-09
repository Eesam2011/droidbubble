package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.droidbubble.app.model.NetworkSpeedInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * B9.1: the network speed digits keep the island-wide tabular figures ("tnum", provided by the
 * service through LocalTextStyle). A `style = TextStyle(...)` used to replace that style.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NetworkSpeedTnumTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun featureSettingsOf(text: String): String? {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single().layoutInput.style.fontFeatureSettings
    }

    @Test
    fun speedValuesAndLabels_keepTabularFigures() {
        rule.setContent {
            CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFeatureSettings = "tnum")) {
                ExpandedNetworkSpeedView(
                    networkSpeedInfo = NetworkSpeedInfo(downloadSpeedFormatted = "1.2 MB/s", uploadSpeedFormatted = "345 KB/s"),
                    onCollapse = {}
                )
            }
        }
        assertEquals("tnum", featureSettingsOf("1.2 MB/s"))
        assertEquals("tnum", featureSettingsOf("345 KB/s"))
    }
}
