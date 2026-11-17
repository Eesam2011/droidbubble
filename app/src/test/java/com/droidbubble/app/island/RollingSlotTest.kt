package com.droidbubble.app.island

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

/** D3.1: a rolling number with a slot template keeps one width whatever it shows. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RollingSlotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun widthsOf(values: List<String>, template: String, size: Float = 12f): List<Float> {
        val value = mutableStateOf(values.first())
        rule.setContent {
            Column {
                RollingNumberText(
                    value.value, Color.White, size.sp, FontWeight.SemiBold,
                    modifier = Modifier.testTag("slot"), slotTemplate = template,
                )
                RollingNumberText(template, Color.White, size.sp, FontWeight.SemiBold, modifier = Modifier.testTag("template"))
            }
        }
        val templateWidth = rule.onNodeWithTag("template").getBoundsInRoot().width.value
        return values.map { v ->
            rule.runOnIdle { value.value = v }
            rule.mainClock.advanceTimeBy(1_000)
            rule.onNodeWithTag("slot").getBoundsInRoot().width.value.also {
                assertTrue("$v is wider than its slot ($it < $templateWidth)", it + 0.01f >= templateWidth)
            }
        }
    }

    @Test
    fun percentages_neverChangeWidth() {
        val widths = widthsOf(listOf("0%", "7%", "42%", "99%", "100%", "1%"), "100%")
        assertTrue(widths.toString(), widths.all { it == widths.first() })
    }

    @Test
    fun bubbleNumeral_neverChangesWidth() {
        val widths = widthsOf(listOf("1", "42", "100", "8"), "100", size = 11f)
        assertTrue(widths.toString(), widths.all { it == widths.first() })
    }

    @Test
    fun withoutATemplate_theTextHugsItsValue() {
        rule.setContent {
            Column(horizontalAlignment = Alignment.Start) {
                RollingNumberText("7%", Color.White, 12.sp, FontWeight.SemiBold, modifier = Modifier.testTag("bare"))
                RollingNumberText("7%", Color.White, 12.sp, FontWeight.SemiBold, modifier = Modifier.testTag("slot"), slotTemplate = "100%")
            }
        }
        assertTrue(rule.onNodeWithTag("bare").getBoundsInRoot().width < rule.onNodeWithTag("slot").getBoundsInRoot().width)
    }

    @Test
    fun direction_followsTheNumber() {
        assertTrue(numericValue("42%") < numericValue("57%"))
        assertTrue(numericValue("9:59") < numericValue("10:00"))
        assertTrue(numericValue("100%") > numericValue("99%"))
        assertEquals(0L, numericValue("On"))
    }
}
