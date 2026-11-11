package com.droidbubble.app.island

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/** What happened on the island, for haptics. [HapticMap] decides what each one feels like. */
enum class IslandHapticEvent {
    TAP_EXPAND, COLLAPSE, LONG_PRESS, IDLE_TAP, DISMISS, MINIMISE, RESTORE, HIDE,
    THRESHOLD_CROSSED, GESTURE_COMMIT, TOGGLE_ON, TOGGLE_OFF, SEGMENT_TICK, SEGMENT_FREQUENT_TICK,
    CONFIRM, REJECT, NECK_SNAP, MERGE, SYSTEM_ALERT_CHARGING, SYSTEM_ALERT_RINGER
}

/** event → `HapticFeedbackConstants`, per Android version. Null means "no haptic". */
fun interface HapticMap {
    fun constantFor(event: IslandHapticEvent, sdk: Int): Int?

    companion object {
        /** The haptics the island shipped with (F3.3): one constant often stands for several meanings. */
        val Legacy = HapticMap { event, sdk ->
            when (event) {
                IslandHapticEvent.TAP_EXPAND, IslandHapticEvent.DISMISS, IslandHapticEvent.MINIMISE,
                IslandHapticEvent.RESTORE, IslandHapticEvent.HIDE, IslandHapticEvent.GESTURE_COMMIT,
                IslandHapticEvent.TOGGLE_ON -> HapticFeedbackConstants.CONTEXT_CLICK
                IslandHapticEvent.COLLAPSE, IslandHapticEvent.SEGMENT_TICK,
                IslandHapticEvent.SYSTEM_ALERT_RINGER -> HapticFeedbackConstants.CLOCK_TICK
                IslandHapticEvent.LONG_PRESS, IslandHapticEvent.SYSTEM_ALERT_CHARGING -> HapticFeedbackConstants.LONG_PRESS
                IslandHapticEvent.IDLE_TAP, IslandHapticEvent.TOGGLE_OFF, IslandHapticEvent.CONFIRM ->
                    if (sdk >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
                IslandHapticEvent.REJECT ->
                    if (sdk >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                // Events that did not exist before the gesture and liquid work: silent in Legacy
                IslandHapticEvent.THRESHOLD_CROSSED, IslandHapticEvent.SEGMENT_FREQUENT_TICK,
                IslandHapticEvent.NECK_SNAP, IslandHapticEvent.MERGE -> null
            }
        }
    }
}

/**
 * The one place island haptics go through (F3.3). Nothing vibrates when the app's haptics switch
 * is off. It only uses `performHapticFeedback`, so the phone's own "touch feedback" setting and
 * vibration strength always apply: no raw vibrator patterns.
 */
class IslandHaptics(
    private val perform: (constant: Int) -> Unit,
    private val enabled: () -> Boolean,
    private val map: HapticMap = HapticMap.Legacy,
    private val sdk: Int = Build.VERSION.SDK_INT,
) {
    constructor(view: View, enabled: () -> Boolean, map: HapticMap = HapticMap.Legacy) :
        this({ constant -> view.performHapticFeedback(constant) }, enabled, map)

    fun perform(event: IslandHapticEvent) {
        if (!enabled()) return
        val constant = map.constantFor(event, sdk) ?: return
        perform(constant)
    }

    companion object {
        /** Does nothing: for previews and tests that render one piece of the island alone. */
        val None = IslandHaptics(perform = {}, enabled = { false })
    }
}

/** `IslandOverlayRoot` provides the real one. */
val LocalIslandHaptics = staticCompositionLocalOf { IslandHaptics.None }
