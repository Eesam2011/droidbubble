package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** F4.4: a NumericSlot keeps one width whatever value it shows. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumericSlotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")

    @Test
    fun template_mapsDigitsToZero() {
        assertEquals("00:00", slotTemplate("12:34"))
        assertEquals("000%", slotTemplate("100%"))
        assertEquals("000 KB/s", slotTemplate("512 KB/s"))
        assertEquals("--:--", slotTemplate("--:--"))
        assertEquals("", slotTemplate(""))
    }

    @Test
    fun differentValues_shareOneWidth() {
        rule.setContent {
            Column {
                NumericSlot("1:05", style, template = "10:59", modifier = Modifier.testTag("a"), align = Alignment.Start)
                NumericSlot("10:59", style, template = "10:59", modifier = Modifier.testTag("b"))
                NumericSlot("--:--", style, template = "10:59", modifier = Modifier.testTag("c"), alsoFits = listOf("--:--"))
                Text("1:05", style = style, maxLines = 1, modifier = Modifier.testTag("plain"))
            }
        }
        val a = rule.onNodeWithTag("a").getBoundsInRoot().width
        val b = rule.onNodeWithTag("b").getBoundsInRoot().width
        val c = rule.onNodeWithTag("c").getBoundsInRoot().width
        val plain = rule.onNodeWithTag("plain").getBoundsInRoot().width
        assertEquals(a, b)
        assertEquals(a, c)
        assertTrue("the slot is wider than the short value alone ($a vs $plain)", a > plain)
    }

    @Test
    fun customContent_keepsTheSlotWidth() {
        rule.setContent {
            Column {
                NumericSlot("7%", style, template = "100%", modifier = Modifier.testTag("custom")) { Text(it, style = style, maxLines = 1) }
                NumericSlot("100%", style, template = "100%", modifier = Modifier.testTag("text"))
            }
        }
        assertEquals(
            rule.onNodeWithTag("text").getBoundsInRoot().width,
            rule.onNodeWithTag("custom").getBoundsInRoot().width,
        )
    }
}
