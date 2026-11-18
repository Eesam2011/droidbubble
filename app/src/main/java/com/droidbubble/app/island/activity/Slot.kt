package com.droidbubble.app.island.activity

import com.droidbubble.app.island.icon.IconEffect

/**
 * A1.2: the V7 glyphs a slot can name. Presentation maps each to `IslandIcons.Compact.*`, so the
 * spec stays free of Compose types.
 */
enum class IslandIcon {
    NOTIFICATION, CALL, VIDEO_CALL, FLASHLIGHT, DOWNLOAD, MUSIC, MIC, TIMER, ALARM, DELIVERY, SPORTS,
    CHARGING, BLUETOOTH, WIFI, UNLOCKED, RINGER_ON, RINGER_SILENT, RINGER_VIBRATE, CAMERA, SHIELD, NAVIGATION
}

/** An image a slot shows. The bitmaps stay in the payload; this says which one. */
sealed interface ImageRef {
    /** The notification's sender / large-icon picture, else its app icon, else [fallback]. */
    data class Avatar(val fallback: IslandIcon) : ImageRef
    /** The notification's small app icon bitmap, else [fallback]. */
    data class AppIcon(val fallback: IslandIcon) : ImageRef
}

enum class SlotShape { Circle, RoundedSquare }

enum class CustomSlot { MEDIA_ART, EQUALIZER, FLASHLIGHT_GLOW, FLASHLIGHT_LEVEL, CONTACT_AVATAR, LEGACY }

/** A1.2: what one place of the island (compact leading or trailing, the bubble) draws. */
sealed interface Slot {
    data object Empty : Slot
    /** A V7 icon, with an I1 effect (no effect design here). */
    data class Icon(val icon: IslandIcon, val effect: IconEffect? = null) : Slot
    /** Avatar, app icon or maneuver arrow. */
    data class Image(val ref: ImageRef, val shape: SlotShape = SlotShape.Circle) : Slot
    /** [numeric] draws through RollingNumberText. */
    data class Text(val text: String, val numeric: Boolean = false) : Slot
    /** A ticking time counted from [baseWallMs]; [pausedText] while it stands still. */
    data class Timer(val baseWallMs: Long, val countDown: Boolean, val pausedText: String?) : Slot
    /** TimerRingIcon: a countdown's draining ring. */
    data class Ring(val baseWallMs: Long, val key: String) : Slot
    /** TimerClockIcon: the small clock face whose hand follows the timer. */
    data class ClockFace(val baseWallMs: Long, val countDown: Boolean) : Slot
    /** Null = indeterminate. */
    data class Progress(val fraction: Float?) : Slot
    data object Waveform : Slot
    data object RecordingDot : Slot
    data class Score(val home: String, val away: String) : Slot
    /** The escape hatch to today's bespoke composables. */
    data class Custom(val key: CustomSlot) : Slot
}
