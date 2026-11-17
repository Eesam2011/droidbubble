package com.droidbubble.app.island.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandTokens

/**
 * V4.2: one header, padding and button rhythm for every tall card, taken from the notification
 * card (the best-tuned one). The card metrics read their spacing from here instead of keeping
 * their own literals.
 */
@Immutable
class CardRhythm(scale: Float) {
    /** Icon tile or art to the header text. */
    val iconGap: Dp = (IslandTokens.SpaceMd * scale).coerceIn(8.dp, 16.dp)
    /** Title to subtitle. */
    val titleToSubtitle: Dp = (IslandTokens.SpaceXxs * scale).coerceIn(1.dp, 3.dp)
    /** Header to the card's body text. */
    val headerToBody: Dp = (IslandTokens.SpaceSm * scale).coerceIn(6.dp, 10.dp)
    /** Body to the action buttons. */
    val bodyToActions: Dp = (IslandTokens.SpaceMd * scale).coerceIn(10.dp, 14.dp)
    /** Action buttons: 44 dp or more visually, inside a 48 dp touch row. */
    val buttonHeight: Dp = (42.dp * scale).coerceIn(44.dp, 50.dp)
    /** Header icon tile; its glyph is 0.55 of it (V7.2). */
    val tile: Dp = (40.dp * scale).coerceIn(32.dp, 48.dp)
}
