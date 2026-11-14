package com.droidbubble.app.island

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every glyph the island draws, in one family: Material Rounded (V7.1). Island code names icons by
 * meaning through this object and never reaches for `Icons.*` itself (IslandIconsTest checks
 * both), so the family stays consistent and the dynamic-icon engine (epic I) has one seam to
 * replace glyphs at.
 */
object IslandIcons {
    // ---- activities ----
    val Notification: ImageVector get() = Icons.Rounded.Notifications
    val Call: ImageVector get() = Icons.Rounded.Call
    val CallEnd: ImageVector get() = Icons.Rounded.CallEnd
    val Flashlight: ImageVector get() = Icons.Rounded.FlashlightOn
    val Download: ImageVector get() = Icons.Rounded.Download
    val MusicNote: ImageVector get() = Icons.Rounded.MusicNote
    val Mic: ImageVector get() = Icons.Rounded.Mic
    val Timer: ImageVector get() = Icons.Rounded.Timer
    val Alarm: ImageVector get() = Icons.Rounded.Alarm
    val Delivery: ImageVector get() = Icons.Rounded.LocalShipping
    val Sports: ImageVector get() = Icons.Rounded.SportsSoccer

    // ---- system alerts ----
    val Unlocked: ImageVector get() = Icons.Rounded.LockOpen
    val Bluetooth: ImageVector get() = Icons.Rounded.Bluetooth
    val Wifi: ImageVector get() = Icons.Rounded.Wifi
    val RingerOn: ImageVector get() = Icons.Rounded.Notifications
    val RingerSilent: ImageVector get() = Icons.Rounded.NotificationsOff
    val RingerVibrate: ImageVector get() = Icons.Rounded.Vibration
    val Done: ImageVector get() = Icons.Rounded.Check

    // ---- controls ----
    val Play: ImageVector get() = Icons.Rounded.PlayArrow
    val Pause: ImageVector get() = Icons.Rounded.Pause
    val SkipNext: ImageVector get() = Icons.Rounded.SkipNext
    val SkipPrevious: ImageVector get() = Icons.Rounded.SkipPrevious
    val Shuffle: ImageVector get() = Icons.Rounded.Shuffle
    val Repeat: ImageVector get() = Icons.Rounded.Repeat
    val RepeatOne: ImageVector get() = Icons.Rounded.RepeatOne
    /** Mirrors in right-to-left layouts, so the arrow points the way text flows. */
    val Send: ImageVector get() = Icons.AutoMirrored.Rounded.Send
    val Close: ImageVector get() = Icons.Rounded.Close
}
