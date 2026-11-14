package com.droidbubble.app.service.window

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.droidbubble.app.island.IslandTokens
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandScene
import com.droidbubble.app.island.scene.MeasuredHeights
import com.droidbubble.app.island.scene.bubbleRestCenterX
import com.droidbubble.app.island.scene.deriveIslandTargets
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.service.WindowSpec
import kotlin.math.abs
import kotlin.math.max

/**
 * F5.4 (decision D7), a trial behind `IslandSettings.envelopeWindowTrial`: while a card is open
 * the window is an envelope around what the island can draw instead of the whole screen, so less
 * is filled and composited and the app below stays touchable. A tap outside the envelope arrives
 * as ACTION_OUTSIDE ([OutsideTapCollapser]) and collapses the card.
 *
 * The envelope covers today's two bodies: card 1 open, card 2 below it (dual), and the pill and
 * the secondary bubble the collapse animates back into, plus [ENVELOPE_MARGIN_DP] on every side
 * for the spring overshoot, the liquid stretch and the bounce.
 */
object EnvelopeWindow {
    /** Room around the cards on each side (and below), for overshoot, stretch and bounce. */
    const val ENVELOPE_MARGIN_DP = 24f

    /**
     * The envelope for an expanded [scene], centred like every island window (TOP |
     * CENTER_HORIZONTAL) and starting at the top of the screen, so the island draws at the same
     * place as in the full-screen window. [measured] are the card heights the view measured from
     * text; null (not published yet) returns null and the caller keeps MATCH_PARENT. A null width
     * means MATCH_PARENT: the left-cutout layout, or an envelope as wide as the screen.
     */
    fun envelopeFor(scene: IslandScene, settings: IslandSettings, env: IslandEnv, measured: MeasuredHeights?): WindowSpec.Compact? {
        if (measured == null) return null
        val targets = deriveIslandTargets(scene, settings, env, measured, card2Expanded = true, bubbleEmerged = true)
        val card = targets.card1Open
        val card2 = if (scene.showSecondaryCard) targets.card2Height.value else 0f

        // Half widths from the centre line: the open cards, the pill or glance they collapse
        // into, and the secondary bubble beside the pill in dual mode
        var half = card.width.value / 2f
        half = max(half, settings.widthDp / 2f)
        targets.alertWidth?.let { half = max(half, it.value / 2f) }
        if (scene.isDual) half = max(half, bubbleRestCenterX(settings).value + settings.heightDp / 2f)
        val widthDp = 2f * (half + ENVELOPE_MARGIN_DP + abs(settings.horizontalOffsetDp))

        val pillBottom = settings.topOffsetDp.toFloat() + settings.heightDp
        val cardsBottom = card.top.value + card.height.value +
            if (card2 > 0f) IslandTokens.Shell.CardSpacing.value + card2 else 0f
        val heightDp = max(pillBottom, cardsBottom) + ENVELOPE_MARGIN_DP

        val fullWidth = settings.isLeftCutout || widthDp >= env.screenWidthDp
        return WindowSpec.Compact(widthDp = if (fullWidth) null else widthDp, heightDp = heightDp, touchable = true)
    }

    /**
     * While the card stays open the envelope only grows: a card getting shorter (card 2 leaving,
     * a reply closing) animates, and shrinking the window at once would clip it. The collapse
     * shrinks it, after the island has settled. Sizes are px or `MATCH_PARENT`.
     */
    fun grownOnly(current: Int, next: Int): Int = when {
        current == WindowManager.LayoutParams.MATCH_PARENT || next == WindowManager.LayoutParams.MATCH_PARENT ->
            WindowManager.LayoutParams.MATCH_PARENT
        else -> max(current, next)
    }
}

/**
 * F5.4: collapses the open card when a tap lands outside the envelope window. The window has
 * `FLAG_WATCH_OUTSIDE_TOUCH` only in the trial, so this never fires otherwise. The tap still
 * reaches the app below as well: Android sends ACTION_OUTSIDE as a notice, not instead of it.
 */
class OutsideTapCollapser(
    private val isExpanded: () -> Boolean,
    private val collapse: () -> Unit,
) : View.OnTouchListener {
    override fun onTouch(v: View?, event: MotionEvent): Boolean = onAction(event.actionMasked)

    /** Returns true when the event was the outside tap (consumed). */
    fun onAction(actionMasked: Int): Boolean {
        if (actionMasked != MotionEvent.ACTION_OUTSIDE) return false
        if (isExpanded()) collapse()
        return true
    }
}
