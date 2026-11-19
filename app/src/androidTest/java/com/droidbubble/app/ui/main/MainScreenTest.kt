package com.droidbubble.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.theme.DroidbubbleTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * C3.1: the real main screen with a real ViewModel. Replaces the stale template that called
 * `MainScreen(FAKE_DATA)` and no longer compiled.
 */
class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        val context = composeTestRule.activity.applicationContext
        val viewModel = MainScreenViewModel(SettingsRepository(context))
        composeTestRule.setContent {
            DroidbubbleTheme { MainScreen(viewModel = viewModel) }
        }
    }

    @Test
    fun enableIslandSwitch_existsAndIsToggleable() {
        composeTestRule.onNodeWithText("Enable Island").assertExists()
        composeTestRule.onNodeWithTag(MainScreenTestTags.ENABLE_SWITCH).assertIsToggleable()
    }
}
