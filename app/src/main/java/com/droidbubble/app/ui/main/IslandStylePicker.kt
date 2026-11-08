package com.droidbubble.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidbubble.app.model.IslandSettings

/**
 * SVG artwork for the style previews, in a 120 x 64 viewBox: the top of a phone screen with the
 * island drawn on it. Rendered with Compose's SVG path parser, so the previews stay crisp at any size.
 */
private object IslandStyleSvg {
    const val VIEW_W = 120f
    const val VIEW_H = 64f

    // Phone frame: rounded top corners, open at the bottom
    const val SCREEN = "M6 64 V16 Q6 4 18 4 H102 Q114 4 114 16 V64 Z"
    const val STATUS_LEFT = "M16 13 H28"
    const val STATUS_RIGHT = "M94 13 H104"

    // Floating capsule around the camera
    const val PILL = "M44 9 H76 A7 7 0 0 1 76 23 H44 A7 7 0 0 1 44 9 Z"

    // Notch hanging from the top edge, with concave ears where it meets the edge
    const val NOTCH = "M36 4 H84 Q79 4 79 9 V16 Q79 23 72 23 H48 Q41 23 41 16 V9 Q41 4 36 4 Z"

    // Camera dot (both styles)
    const val CAMERA = "M57.5 16 A2.5 2.5 0 1 0 62.5 16 A2.5 2.5 0 1 0 57.5 16 Z"
}

private fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

@Composable
private fun IslandStylePreview(isNotch: Boolean, modifier: Modifier = Modifier) {
    val screen = remember { svgPath(IslandStyleSvg.SCREEN) }
    val statusLeft = remember { svgPath(IslandStyleSvg.STATUS_LEFT) }
    val statusRight = remember { svgPath(IslandStyleSvg.STATUS_RIGHT) }
    val island = remember(isNotch) { svgPath(if (isNotch) IslandStyleSvg.NOTCH else IslandStyleSvg.PILL) }
    val camera = remember { svgPath(IslandStyleSvg.CAMERA) }
    val screenColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val frameColor = MaterialTheme.colorScheme.outline
    val statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier.aspectRatio(IslandStyleSvg.VIEW_W / IslandStyleSvg.VIEW_H)) {
        // Fit the viewBox into the canvas
        val s = minOf(size.width / IslandStyleSvg.VIEW_W, size.height / IslandStyleSvg.VIEW_H)
        val dx = (size.width - IslandStyleSvg.VIEW_W * s) / 2f
        val dy = (size.height - IslandStyleSvg.VIEW_H * s) / 2f
        translate(dx, dy) {
            scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(screen, screenColor)
                drawPath(screen, frameColor, style = Stroke(width = 1.4f))
                drawPath(statusLeft, statusColor, style = Stroke(width = 2.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(statusRight, statusColor, style = Stroke(width = 2.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(island, Color.Black)
                drawPath(camera, Color(0xFF1E2A3A))
            }
        }
    }
}

@Composable
private fun IslandStyleOption(
    title: String,
    subtitle: String,
    isNotch: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "styleBorder"
    )
    Surface(
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IslandStylePreview(isNotch = isNotch, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = onSelect)
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Pill vs notch selector with a live SVG preview of each style. */
@Composable
fun IslandStylePicker(
    selectedStyle: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Island style",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IslandStyleOption(
                title = "Pill",
                subtitle = "Floats around the camera",
                isNotch = false,
                selected = selectedStyle != IslandSettings.STYLE_NOTCH,
                onSelect = { onSelect(IslandSettings.STYLE_PILL) },
                modifier = Modifier.weight(1f)
            )
            IslandStyleOption(
                title = "Notch",
                subtitle = "Attached to the top edge",
                isNotch = true,
                selected = selectedStyle == IslandSettings.STYLE_NOTCH,
                onSelect = { onSelect(IslandSettings.STYLE_NOTCH) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
