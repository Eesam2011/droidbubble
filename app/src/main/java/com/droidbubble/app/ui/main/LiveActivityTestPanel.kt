package com.droidbubble.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.LiveActivityTester

/**
 * Settings card that shows fake live activities on the island (call, timer, Time's up, alarm,
 * recorder, download, ride, live update, flight, score) so each design can be checked without the
 * real apps. Their buttons work too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveActivityTestPanel(islandRunning: Boolean) {
    SettingsCardItem(position = GroupPosition.SINGLE) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (islandRunning) {
                    "Tap one to show it on the island. Buttons on the island work like the real thing."
                } else {
                    "Turn on the island first, then tap one to show it."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveActivityTester.Demo.entries.forEach { demo ->
                    FilledTonalButton(
                        onClick = { LiveActivityTester.show(demo) },
                        enabled = islandRunning,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(demo.label, fontWeight = FontWeight.Medium)
                    }
                }
                OutlinedButton(
                    onClick = { LiveActivityTester.clear() },
                    enabled = islandRunning,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * F5.4 (decision D7): debug builds only. Turns on the envelope window trial: an open card gets a
 * window around the cards instead of the whole screen, and a tap outside collapses it.
 */
@Composable
fun EnvelopeWindowTrialSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsCardItem(position = GroupPosition.SINGLE) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text("Envelope window (trial)", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("Debug only. The open card's window wraps the cards instead of the whole screen.") },
            trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
        )
    }
}
