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

/**
 * Sizes that need a TextMeasurer: measured in composition, passed in. [compactSide] is the larger
 * side of the pill's content (V3.1); null (the window maths) keeps the pill at its full width.
 */
@Immutable
data class MeasuredHeights(
    val notification: Dp,
    val progress: Dp? = null,
    val compactSide: Dp? = null,
    val cameraZone: Dp = IslandTokens.Shell.CameraZone,
    /** V3.5: the glance pills' widest content, measured; null uses a generous estimate (the window). */
    val alertSides: AlertSides? = null,
    /** V4.1: the media card's measured height; null keeps the old table (the window maths). */
    val media: Dp? = null,
)

/**
 * V3.5: the widest content one side of a glance pill can hold: the ringer's longest label
 * ("Vibrate"), and the charging pill's bolt and widest wattage or its "100%".
 */
@Immutable
data class AlertSides(val ringer: Dp, val charging: Dp)

/**
 * A generous [AlertSides] without a text measurer (0.62 em per character, wider than the island's
 * font), so the window is never narrower than the pill the view measures.
 */
fun estimatedAlertSides(fontScale: Float): AlertSides {
    val em = 0.62f * fontScale
    val ringerLabel = 7 * em * com.droidbubble.app.island.IslandType.CompactLabel.value
    val value = com.droidbubble.app.island.IslandType.CompactValue.value
    val chargingLead = (IslandTokens.Glyph.Compact.value - 4f) + 3f + 4 * em * value
    val chargingTrail = 4 * em * value
    return AlertSides(maxOf(IslandTokens.Glyph.Compact.value, ringerLabel).dp, maxOf(chargingLead, chargingTrail).dp)
}

/**
 * The compact pill's width: the Width setting (owner, 2026-11-15), or wider when its text needs it
 * (at most TEXT_SIDE_MAX a side), like the ringer pill (owner's "text missing" report).
 */
fun mainPillWidth(settings: IslandSettings, measured: MeasuredHeights): Dp {
    val side = measured.compactSide ?: return settings.widthDp.dp
    val slots = com.droidbubble.app.island.layout.CompactSlots
    val needed = measured.cameraZone + minOf(side, slots.TEXT_SIDE_MAX) * 2 + slots.inset(settings.heightDp.dp) * 2
    return maxOf(settings.widthDp.dp, needed)
}

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
    /** M3.1: card 1's closed and open rects, the endpoints its content progress runs between. */
    val card1Closed: IslandGeometry = IslandGeometry(0.dp, 0.dp, 0.dp, 0.dp),
    val card1Open: IslandGeometry = IslandGeometry(0.dp, 0.dp, 0.dp, 0.dp),
)

/** Width of an expanded card: the token width × scale, kept inside the screen gutter. */
fun expandedCardWidth(settings: IslandSettings, env: IslandEnv): Dp =
    (IslandTokens.Shell.ExpandedCardWidth * settings.expandedScale).coerceAtMost(env.screenWidthDp.dp - IslandTokens.Shell.ScreenGutter)

/** Centre of the settled secondary bubble, measured from the pill's centre (6 dp from its edge). */
fun bubbleRestCenterX(settings: IslandSettings, pillWidth: Dp = settings.widthDp.dp): Dp =
    pillWidth / 2f + IslandTokens.Shell.BubbleGap + (settings.heightDp.dp / 2f)

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

    // V3.5: a glance pill is never narrower than its widest content, the same width in every
    // state (Ring, Vibrate and Silent alike), so nothing is clipped and the label never moves
    val alertSides = measured.alertSides ?: estimatedAlertSides(com.droidbubble.app.island.IslandFontScale.compact(env.fontScale))
    val chargingIslandWidth = maxOf(
        (settings.widthDp + IslandTokens.Shell.ChargingExtraWidthDp).dp.coerceAtLeast(IslandTokens.Shell.ChargingMinWidth),
        IslandTokens.Shell.ChargingContentPadding * 2 + measured.cameraZone + alertSides.charging * 2,
    )
    val unlockIslandWidth = (settings.widthDp * IslandTokens.Shell.UnlockWidthFactor).dp.coerceAtLeast(IslandTokens.Shell.UnlockMinWidth)
    val ringerIslandWidth = maxOf(
        (settings.widthDp * IslandTokens.Shell.RingerWidthFactor).dp.coerceAtLeast(IslandTokens.Shell.RingerMinWidth),
        IslandTokens.Shell.AlertContentPadding * 2 + measured.cameraZone + alertSides.ringer * 2,
    )
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
        // V3.1: as wide as its content; the secondary bubble sits beside it
        else -> mainPillWidth(settings, measured)
    }

    val cardOpen = isExpanded && !hasAlert
    val card1Width = if (cardOpen) expandedCardWidth(settings, env) else 0.dp
    val card1ContentHeight = cardHeightFor(scene.primary, settings.expandedScale, measured.notification, measured.progress, env.fontScale, measured.media)
    val card1Height = if (isExpanded) card1ContentHeight else settings.heightDp.dp
    // One corner radius for every expanded card, concentric with the round controls of the live
    // activity cards (see LiveLayout). The squircle caps it at half the height.
    val corner = if (cardOpen) liveActivityCornerRadius(settings.expandedScale, env.fontScale) else IslandTokens.CompactCornerRadius
    // Open, the card sits below the status bar; closed, it docks over the camera
    val top = if (cardOpen) settings.topOffsetDp.dp + IslandTokens.Shell.ExpandedDrop else settings.topOffsetDp.dp

    val card2Height = if (card2Expanded && scene.showSecondaryCard) {
        cardHeightFor(scene.secondary, settings.expandedScale, measured.notification, measured.progress, env.fontScale, measured.media)
    } else 0.dp

    return IslandTargets(
        compactWidth = compactWidth,
        card1 = IslandGeometry(card1Width, card1Height, corner, top),
        card1ContentHeight = card1ContentHeight,
        card2Height = card2Height,
        bubbleRestX = if (bubbleEmerged && scene.emergesBubble) bubbleRestCenterX(settings, mainPillWidth(settings, measured)) else 0.dp,
        horizontalOffset = if (!isExpanded && !hasAlert) settings.horizontalOffsetDp.dp else 0.dp,
        cardSpacing = if (isExpanded && scene.showSecondaryCard) IslandTokens.Shell.CardSpacing else 0.dp,
        alertWidth = alertWidth,
        card1Closed = IslandGeometry(0.dp, settings.heightDp.dp, IslandTokens.CompactCornerRadius, settings.topOffsetDp.dp),
        card1Open = IslandGeometry(
            expandedCardWidth(settings, env), card1ContentHeight,
            liveActivityCornerRadius(settings.expandedScale, env.fontScale),
            settings.topOffsetDp.dp + IslandTokens.Shell.ExpandedDrop,
        ),
    )
}
