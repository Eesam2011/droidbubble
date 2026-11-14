package com.droidbubble.app.island.icon

/**
 * What an icon does in response to an event or a state (phase 5). Effects come in two classes,
 * and the class decides how a call site triggers it:
 * - [Discrete] fires once each time its `trigger` changes (never on the first value it sees).
 * - [Indefinite] runs while `active` is true and stops cleanly when it turns false.
 *
 * Pick an effect by its message, not its look: a bounce says "something happened", a wiggle
 * "look here", a pulse "this is ongoing", a breathe "this is present" (recording, a timer), a
 * rotation "work in progress".
 */
sealed interface IconEffect {
    sealed interface Discrete : IconEffect
    sealed interface Indefinite : IconEffect

    /** "Something happened": a quick dip and elastic overshoot, drifting [direction]. */
    data class Bounce(val direction: Direction = Direction.Up) : Discrete

    /**
     * "Look here": a loose swing that settles by itself, turning about the pivot ([WiggleAxis.Rotate],
     * bell or phone) or sliding sideways ([WiggleAxis.Horizontal], vibrate). With [repeatEveryMs] it
     * swings again at that interval for as long as the icon is `active` (a ringing call).
     */
    data class Wiggle(val axis: WiggleAxis = WiggleAxis.Rotate, val repeatEveryMs: Long? = null) : Discrete

    enum class Direction { Up, Down }
    enum class WiggleAxis { Rotate, Horizontal }
}
