package com.droidbubble.app.island

/**
 * Root composition counts recorded by RecompositionBudgetTest (S4.1) on 2026-11-09, at the
 * Phase 0 checkpoint. A change may add at most 10 %; a task that lowers a count updates it here.
 *
 * At rest the root doesn't recompose at all: the equalizer and the timer recompose only inside
 * their own scopes. B8.1 and B8.2 therefore need a counter in those scopes to show their gains.
 */
object PerfBaseline {
    /** About one root composition per 16 ms frame: the whole root follows the morph (06 P0-1). */
    const val MEDIA_EXPAND_500MS = 30
    const val MEDIA_COLLAPSE_600MS = 37
    const val MEDIA_PILL_REST_5S = 0
    const val TIMER_PILL_5S = 0
    const val NETWORK_SPEED_1HZ_3S = 81
}
