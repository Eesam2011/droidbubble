package com.droidbubble.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandIcons
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.SquircleShape
import com.droidbubble.app.island.icon.DrawOnIcon
import com.droidbubble.app.island.icon.DynamicIcon
import com.droidbubble.app.island.icon.DynamicIconSwitch
import com.droidbubble.app.island.icon.DynamicIconVisibility
import com.droidbubble.app.island.icon.IconEffect
import com.droidbubble.app.island.icon.IconStrokes
import com.droidbubble.app.island.icon.LayeredIcon
import com.droidbubble.app.island.icon.MorphIcon
import com.droidbubble.app.island.icon.MorphShapes
import com.droidbubble.app.island.icon.MuteGlyph
import com.droidbubble.app.island.icon.ReplaceDirection
import com.droidbubble.app.island.icon.SlashOverlay
import com.droidbubble.app.island.icon.VariableMode
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier
import com.droidbubble.app.island.motion.MotionToken
import com.droidbubble.app.island.motion.rememberMotionTier
import com.droidbubble.app.island.motion.toPhysical
import com.droidbubble.app.model.IslandSettings
import java.util.Locale

/** One catalogue entry: how it is driven, and what it draws at a given glyph size. */
private sealed interface CatalogDrive {
    /** A button fires it: [trigger] counts presses. */
    data object Fire : CatalogDrive
    /** A switch holds it on or off. */
    data object Hold : CatalogDrive
    /** A slider sets a level. */
    data object Level : CatalogDrive
}

private class CatalogEntry(
    val name: String,
    val token: MotionToken.Perceptual?,
    val note: String,
    val drive: CatalogDrive,
    val draw: @Composable (glyph: Dp, trigger: Int, on: Boolean, level: Float) -> Unit,
)

private val White = Color.White

private val entries: List<CatalogEntry> = listOf(
    CatalogEntry("Bounce", IslandMotion.Symbol.bounce, "Something happened", CatalogDrive.Fire) { g, t, _, _ ->
        DynamicIcon(IslandIcons.Compact.Notification, null, White, size = g, effect = IconEffect.Bounce(), trigger = t)
    },
    CatalogEntry("Wiggle (turn)", IslandMotion.Symbol.wiggle, "Look here: bell, phone", CatalogDrive.Fire) { g, t, _, _ ->
        DynamicIcon(IslandIcons.Compact.RingerOn, null, White, size = g, effect = IconEffect.Wiggle(), trigger = t, pivot = TransformOrigin(0.5f, 0f))
    },
    CatalogEntry("Wiggle (sideways)", IslandMotion.Symbol.wiggle, "Look here: vibrate", CatalogDrive.Fire) { g, t, _, _ ->
        DynamicIcon(IslandIcons.Compact.RingerVibrate, null, White, size = g, effect = IconEffect.Wiggle(IconEffect.WiggleAxis.Horizontal), trigger = t)
    },
    CatalogEntry("Pulse", null, "Ongoing · ${IslandMotion.Symbol.PULSE_PERIOD_MS} ms", CatalogDrive.Hold) { g, _, on, _ ->
        DynamicIcon(IslandIcons.Compact.Mic, null, IslandTokens.RecordingRed, size = g, effect = IconEffect.Pulse(), active = on)
    },
    CatalogEntry("Breathe", null, "Present · ${IslandMotion.Symbol.BREATHE_PERIOD_MS} ms", CatalogDrive.Hold) { g, _, on, _ ->
        DynamicIcon(IslandIcons.Compact.Alarm, null, White, size = g, effect = IconEffect.Breathe(), active = on)
    },
    CatalogEntry("Rotate", null, "Working · ${IslandMotion.Symbol.ROTATE_PERIOD_MS} ms a turn", CatalogDrive.Hold) { g, _, on, _ ->
        DynamicIcon(IslandIcons.Compact.Repeat, null, White, size = g, effect = IconEffect.Rotate(), active = on)
    },
    CatalogEntry("Variable · level", null, "Download, battery, torch level", CatalogDrive.Level) { g, _, _, level ->
        LayeredIcon(IslandIcons.Compact.Download, White, size = g, level = { level })
    },
    CatalogEntry("Variable · cumulative", null, "Connecting · ${IslandMotion.Symbol.VARIABLE_STEP_MS} ms a step", CatalogDrive.Hold) { g, _, on, _ ->
        LayeredIcon(IslandIcons.Compact.Wifi, White, size = g, mode = VariableMode.Cumulative, active = on)
    },
    CatalogEntry("Variable · iterative", null, "Searching, unknown amount", CatalogDrive.Hold) { g, _, on, _ ->
        LayeredIcon(IslandIcons.Compact.Wifi, White, size = g, mode = VariableMode.Iterative, active = on, reversing = true)
    },
    CatalogEntry("Appear / disappear", IslandMotion.Symbol.appear, "Bluetooth connects", CatalogDrive.Hold) { g, _, on, _ ->
        Box(Modifier.size(g)) { DynamicIconVisibility(on) { Icon(IslandIcons.Compact.Bluetooth, null, tint = White, modifier = Modifier.size(g)) } }
    },
    CatalogEntry("Replace · down-up", IslandMotion.Symbol.replace, "A reaction to input", CatalogDrive.Hold) { g, _, on, _ ->
        DynamicIconSwitch(on, direction = ReplaceDirection.DownUp) { v ->
            Icon(if (v) IslandIcons.Compact.RingerVibrate else IslandIcons.Compact.RingerOn, null, tint = White, modifier = Modifier.size(g))
        }
    },
    CatalogEntry("Replace · up-up", IslandMotion.Symbol.replace, "Forward progression", CatalogDrive.Hold) { g, _, on, _ ->
        DynamicIconSwitch(on, direction = ReplaceDirection.UpUp) { v ->
            Icon(if (v) IslandIcons.Compact.SkipNext else IslandIcons.Compact.Play, null, tint = White, modifier = Modifier.size(g))
        }
    },
    CatalogEntry("Slash · bell", IslandMotion.Symbol.draw, "Ring ↔ Silent", CatalogDrive.Hold) { g, _, on, _ ->
        SlashOverlay(on, IslandTokens.RingerSilentRed.takeIf { on } ?: White, slashedGlyph = {
            Icon(IslandIcons.Compact.RingerSilent, null, tint = IslandTokens.RingerSilentRed, modifier = Modifier.size(g))
        }) { Icon(IslandIcons.Compact.RingerOn, null, tint = if (on) IslandTokens.RingerSilentRed else White, modifier = Modifier.size(g)) }
    },
    CatalogEntry("Slash · flashlight", IslandMotion.Symbol.draw, "Torch off from the card", CatalogDrive.Hold) { g, _, on, _ ->
        SlashOverlay(on, White) { Icon(IslandIcons.Compact.Flashlight, null, tint = White, modifier = Modifier.size(g)) }
    },
    CatalogEntry("Slash · mic", IslandMotion.Symbol.draw, "Recorder paused", CatalogDrive.Hold) { g, _, on, _ ->
        SlashOverlay(on, IslandTokens.RecordingRed) { Icon(IslandIcons.Compact.Mic, null, tint = IslandTokens.RecordingRed, modifier = Modifier.size(g)) }
    },
    CatalogEntry("Slash · mute", IslandMotion.Symbol.draw, "Call mute (phase 7)", CatalogDrive.Hold) { g, _, on, _ ->
        MuteGlyph(on, White, size = g)
    },
    CatalogEntry("Draw on · check", IslandMotion.Symbol.draw, "Unlock, connected", CatalogDrive.Hold) { g, _, on, _ ->
        DrawOnIcon(IconStrokes.Check, drawn = on, tint = White, size = g, delayFraction = 0.3f)
    },
    CatalogEntry("Morph · play ↔ pause", IslandMotion.Symbol.morph, "Media, recorder, timer", CatalogDrive.Hold) { g, _, on, _ ->
        MorphIcon(MorphShapes.PlayPause, atEnd = on, tint = White, contentDescription = null, size = g)
    },
)

/** The entry names, in order (tests and the clip renderer use them). */
val IconEffectCatalogNames: List<String> get() = entries.map { it.name }

private fun MotionToken.Perceptual.label(): String {
    val p = toPhysical()
    return String.format(Locale.US, "%.2f s / %.2f → ζ %.2f, k %.0f", duration, bounce, p.dampingRatio, p.stiffness)
}

/** A black squircle tile with the glyph at the real compact size, or zoomed. */
@Composable
private fun EffectTile(tile: Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.size(tile).background(Color.Black, SquircleShape(tile * 0.32f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Only the 2× tile of entry [index], tagged for the clip renderer. */
@Composable
internal fun IconEffectCatalogTile(index: Int, trigger: Int, on: Boolean, level: Float) {
    Box(Modifier.testTag("icon_catalog_zoom_$index")) {
        EffectTile(112.dp) { entries[index].draw(IslandTokens.Glyph.Compact * 2, trigger, on, level) }
    }
}

/**
 * One catalogue row: the effect at the island's compact size and at 2×, its token, and its
 * control. [tag] marks the zoomed tile, for the clip renderer.
 */
@Composable
internal fun IconEffectCatalogRow(index: Int, trigger: Int, on: Boolean, level: Float, onFire: () -> Unit, onToggle: (Boolean) -> Unit, onLevel: (Float) -> Unit) {
    val e = entries[index]
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        EffectTile(56.dp) { e.draw(IslandTokens.Glyph.Compact, trigger, on, level) }
        Box(Modifier.width(8.dp))
        Box(Modifier.testTag("icon_catalog_zoom_$index")) {
            EffectTile(112.dp) { e.draw(IslandTokens.Glyph.Compact * 2, trigger, on, level) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(e.name, fontWeight = FontWeight.SemiBold)
            Text(e.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            e.token?.let { Text(it.label(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (e.drive) {
                CatalogDrive.Fire -> FilledTonalButton(onClick = onFire, shape = RoundedCornerShape(12.dp)) { Text("Fire") }
                CatalogDrive.Hold -> Switch(checked = on, onCheckedChange = onToggle)
                CatalogDrive.Level -> Slider(value = level, onValueChange = onLevel)
            }
        }
    }
}

/**
 * I4.1: every icon effect and morph, to preview and tune by eye. [tierOverride] null follows the
 * island's own tier (the "Reduce motion" switch, "Remove animations", Battery Saver); [speed] is
 * the island's animation speed, so this shows exactly what the island runs. Tuning edits
 * [IslandMotion.Symbol]; there is no runtime editor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconEffectCatalog(settings: IslandSettings, tierOverride: MotionTier?, onTierOverride: (MotionTier?) -> Unit) {
    val systemTier = rememberMotionTier(settings.reduceMotion)
    val tier = tierOverride ?: systemTier
    val motion = remember(settings, tier) { IslandMotion.from(settings, tier) }
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tierOverride == null, onClick = { onTierOverride(null) }, label = { Text("Auto (${systemTier.name.lowercase()})") })
            for (t in MotionTier.entries) {
                FilterChip(selected = tierOverride == t, onClick = { onTierOverride(t) }, label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        Text(
            String.format(Locale.US, "Speed %.1f×, as the island runs it", motion.speed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CompositionLocalProvider(LocalIslandMotion provides motion) {
            entries.indices.forEach { i ->
                var trigger by remember { mutableIntStateOf(0) }
                var on by remember { mutableStateOf(true) }
                var level by remember { mutableFloatStateOf(0.5f) }
                IconEffectCatalogRow(i, trigger, on, level, onFire = { trigger++ }, onToggle = { on = it }, onLevel = { level = it })
            }
        }
    }
}
