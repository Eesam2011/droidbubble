package com.droidbubble.app.island.icon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import com.droidbubble.app.island.motion.IslandMotion
import com.droidbubble.app.island.motion.LocalIslandMotion
import com.droidbubble.app.island.motion.MotionTier

/** The slash runs top-left to bottom-right across the glyph, as in Material's "off" glyphs (24-unit box). */
private val SLASH_FROM = Offset(3.6f, 3.6f)
private val SLASH_TO = Offset(20.4f, 20.4f)
private const val SLASH_WIDTH = 2.2f
/** The gap cut on either side of the slash, as a multiple of its width. */
private const val SLASH_GAP = 1.5f

/**
 * The "magic replace" between a glyph and its "off" form (I2.2): [content] stays, and only the
 * slash draws across it (cutting a gap so it reads on the glyph) or retracts, on
 * [IslandMotion.Symbol.draw]. A change mid-way reverses from where the slash is.
 *
 * At rest, slashed, [slashedGlyph] is drawn instead when given: the real Material "off" glyph,
 * so the settled look is exactly the designed one. Reduced motion: the two states crossfade.
 */
@Composable
fun SlashOverlay(
    slashed: Boolean,
    slashTint: Color,
    modifier: Modifier = Modifier,
    slashedGlyph: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val motion = LocalIslandMotion.current
    val reduced = motion.tier == MotionTier.REDUCED
    val progress = remember { Animatable(if (slashed) 1f else 0f) }
    LaunchedEffect(slashed) {
        val target = if (slashed) 1f else 0f
        if (progress.value == target) return@LaunchedEffect
        progress.animateTo(
            target,
            if (reduced) TweenSpec(IslandMotion.Symbol.REDUCED_FADE_MS, 0, LinearEasing) else motion.spring(IslandMotion.Symbol.draw),
        )
    }
    // Flips once at the end of the slash, so the slash frames themselves recompose nothing
    val hasSlashedGlyph = slashedGlyph != null
    val settledSlashed by remember(hasSlashedGlyph) {
        derivedStateOf { hasSlashedGlyph && progress.value >= 1f && !progress.isRunning }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (settledSlashed || (reduced && slashedGlyph != null)) {
            // The designed "off" glyph; in Reduced motion it crossfades with the plain one
            Box(Modifier.graphicsLayer { alpha = if (reduced) progress.value else 1f }) { slashedGlyph!!() }
        }
        if (!settledSlashed) {
            Box(
                Modifier
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        if (reduced && slashedGlyph != null) alpha = 1f - progress.value
                    }
                    .drawWithContent {
                        drawContent()
                        val p = if (reduced) (if (slashedGlyph == null) progress.value else 0f) else progress.value
                        if (p <= 0f) return@drawWithContent
                        val k = size.width / 24f
                        val from = SLASH_FROM * k
                        val end = from + (SLASH_TO * k - from) * p.coerceIn(0f, 1f)
                        drawLine(Color.Black, from, end, SLASH_WIDTH * SLASH_GAP * k, StrokeCap.Round, blendMode = BlendMode.Clear)
                        drawLine(slashTint, from, end, SLASH_WIDTH * k, StrokeCap.Round)
                    },
            ) { content() }
        }
    }
}
