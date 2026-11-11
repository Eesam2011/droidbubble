package com.droidbubble.app.island.scene

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.droidbubble.app.island.IslandGeometry
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.cardHeightFor
import com.droidbubble.app.island.liveActivityCornerRadius
import com.droidbubble.app.model.IslandSettings

/** Card heights that need a TextMeasurer: measured in composition, passed in. */
@Immutable
data class MeasuredHeights(val notification: Dp, val progress: Dp? = null)

/** Where every part of the island is heading, in dp. The view animates toward these. */
@Immutable
data class IslandTargets(
    /** Width of the compact pill; 0 while expanded, hidden or idle. */
    val compactWidth: Dp,
    /** Card 1: 0 wide and docked at the pill while collapsed, the full card while expanded. */
    val card1: IslandGeometry,
    /** Card 1's content height (its laid-out size, whether or not it is open). */
    val card1ContentHeight: Dp,
    val card2Height: Dp,
    /** Centre of the secondary bubble, measured from the pill's centre; 0 while it is tucked in. */
    val bubbleRestX: Dp,
    val horizontalOffset: Dp,
    val cardSpacing: Dp,
    /** Width of the charging, unlock, connection or ringer pill; null when no glance shows. */
    val alertWidth: Dp?,
)

/** Width of an expanded card: the token width × scale, kept inside the screen gutter. */
fun expandedCardWidth(settings: IslandSettings, env: IslandEnv): Dp =
    (IslandTokens.Shell.ExpandedCardWidth * settings.expandedScale).coerceAtMost(env.screenWidthDp.dp - IslandTokens.Shell.ScreenGutter)

/** Centre of the settled secondary bubble, measured from the pill's centre. */
fun bubbleRestCenterX(settings: IslandSettings): Dp =
    settings.widthDp.dp / 2f + IslandTokens.Shell.BubbleGap + (settings.heightDp.dp / 2f)

/**
 * The target sizes for a scene (F1.3). [card2Expanded] and [bubbleEmerged] are the view's short
 * stagger delays, passed in as flags until M3.2 replaces them.
 */
fun deriveIslandTargets(
    scene: IslandScene,
    settings: IslandSettings,
    env: IslandEnv,
    measured: MeasuredHeights,
    card2Expanded: Boolean,
    bubbleEmerged: Boolean,
): IslandTargets {
    val v = scene.visible
    val isExpanded = scene.isExpanded
    val hasAlert = scene.hasAlert

    val chargingIslandWidth = (settings.widthDp + IslandTokens.Shell.ChargingExtraWidthDp).dp.coerceAtLeast(IslandTokens.Shell.ChargingMinWidth)
    val unlockIslandWidth = (settings.widthDp * IslandTokens.Shell.UnlockWidthFactor).dp.coerceAtLeast(IslandTokens.Shell.UnlockMinWidth)
    val ringerIslandWidth = (settings.widthDp * IslandTokens.Shell.RingerWidthFactor).dp.coerceAtLeast(IslandTokens.Shell.RingerMinWidth)
    val alertWidth = when {
        v.isCharging -> chargingIslandWidth
        v.isRingerMode -> ringerIslandWidth
        v.isUnlock || v.isBluetooth -> unlockIslandWidth
        else -> null
    }

    // The pill docks at settings.topOffsetDp. It shrinks to 0 while the card is open.
    val compactWidth = when {
        !scene.isOccupied && !settings.alwaysShowPill -> 0.dp
        isExpanded -> 0.dp
        alertWidth != null -> alertWidth
        // Swiped right: gone. Swiped left: shrunk into a circle around the camera.
        scene.userHidden -> 0.dp
        scene.userMinimized -> settings.heightDp.dp
        // Dual mode keeps the main pill at full size; the secondary bubble sits beside it
        else -> settings.widthDp.dp
    }

    val cardOpen = isExpanded && !hasAlert
    val card1Width = if (cardOpen) expandedCardWidth(settings, env) else 0.dp
    val card1ContentHeight = cardHeightFor(scene.primary, settings.expandedScale, measured.notification, measured.progress, env.fontScale)
    val card1Height = if (isExpanded) card1ContentHeight else settings.heightDp.dp
    // One corner radius for every expanded card, concentric with the round controls of the live
    // activity cards (see LiveLayout). The squircle caps it at half the height.
    val corner = if (cardOpen) liveActivityCornerRadius(settings.expandedScale, env.fontScale) else IslandTokens.CompactCornerRadius
    // Open, the card sits below the status bar; closed, it docks over the camera
    val top = if (cardOpen) settings.topOffsetDp.dp + IslandTokens.Shell.ExpandedDrop else settings.topOffsetDp.dp

    val card2Height = if (card2Expanded && scene.showSecondaryCard) {
        cardHeightFor(scene.secondary, settings.expandedScale, measured.notification, measured.progress, env.fontScale)
    } else 0.dp

    return IslandTargets(
        compactWidth = compactWidth,
        card1 = IslandGeometry(card1Width, card1Height, corner, top),
        card1ContentHeight = card1ContentHeight,
        card2Height = card2Height,
        bubbleRestX = if (bubbleEmerged && scene.emergesBubble) bubbleRestCenterX(settings) else 0.dp,
        horizontalOffset = if (!isExpanded && !hasAlert) settings.horizontalOffsetDp.dp else 0.dp,
        cardSpacing = if (isExpanded && scene.showSecondaryCard) IslandTokens.Shell.CardSpacing else 0.dp,
        alertWidth = alertWidth,
    )
}
