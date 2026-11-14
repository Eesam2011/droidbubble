package com.droidbubble.app.island

/**
 * Root composition counts recorded by RecompositionBudgetTest (S4.1) on 2026-11-09, at the
 * Phase 0 checkpoint. A change may add at most 10 %; a task that lowers a count updates it here.
 *
 * At rest the root doesn't recompose at all: the equalizer and the timer recompose only inside
 * their own scopes. B8.1 and B8.2 therefore need a counter in those scopes to show their gains.
 */
object PerfBaseline {
    /**
     * M2.7: hard gates, no tolerance. An expand or a collapse may recompose the shell at most
     * twice: once for the state change itself, once when card 2's stagger flips. Everything that
     * moves frame by frame is read in layout / draw lambdas or inside its own restart scope.
     * (They were 30 and 37, about one per frame, at the Phase 0 checkpoint.)
     */
    const val MEDIA_EXPAND_500MS = 2
    const val MEDIA_COLLAPSE_600MS = 2
    /** M2.7 gate: ten ticks of a screen recording's timer never recompose the shell. */
    const val RECORDING_PILL_10S = 0
    const val MEDIA_PILL_REST_5S = 0
    const val TIMER_PILL_5S = 0
    const val FLASHLIGHT_DRAG_1S = 2
    // B8.2: the card was 104 before (250 ms ticker + tween). The root's 82 is the known
    // every-frame root recomposition (06 P0-1, M2), the same before and after B8.2.
    const val MEDIA_EXPANDED_PLAYING_3S_ROOT = 82
    const val MEDIA_EXPANDED_PLAYING_3S_CARD = 3
}
