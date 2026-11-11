package com.droidbubble.app.service.window

import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Immutable

/**
 * Everything the overlay window can be asked to be, in pixels (F5.1). The service builds one from
 * three independent wishes: the size (what the island needs), the keyboard (a reply is being
 * typed) and the refresh boost (an animation is running). The controller applies all of them in
 * one window update.
 */
@Immutable
data class WindowState(
    /** Pixels, or `MATCH_PARENT`. */
    val width: Int,
    val height: Int,
    val touchable: Boolean,
    /** The reply field (and later Back, M4.5) needs key events. */
    val focusable: Boolean = false,
    /** 0 = let the system choose. */
    val preferredRefreshRate: Float = 0f,
    val gravity: Int = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
) {
    companion object {
        /** 1×1 and not touchable: the island is not on screen. */
        val Gone = WindowState(width = 1, height = 1, touchable = false)
    }
}

/**
 * Owns the overlay window's layout params and changes them with **at most one**
 * `updateViewLayout` per [apply], and none when nothing changed. Before this, a shape change sent
 * two (the refresh boost, then the resize), and each costs a window relayout.
 *
 * [update] is `windowManager.updateViewLayout(view, params)`; it is a parameter so tests can count
 * the calls.
 */
class OverlayWindowController(
    private val params: WindowManager.LayoutParams,
    private val update: (WindowManager.LayoutParams) -> Unit,
    private val sdk: Int = Build.VERSION.SDK_INT,
) {
    /** How many window updates this controller has sent (for tests and traces). */
    var updates = 0
        private set

    init {
        disableMoveAnimation()
    }

    /** What the window is now, read back from the params. */
    val current: WindowState
        get() = WindowState(
            width = params.width,
            height = params.height,
            touchable = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0,
            focusable = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE == 0,
            preferredRefreshRate = params.preferredRefreshRate,
            gravity = params.gravity,
        )

    /** Moves the window to [state]. Returns true when an update was sent. */
    fun apply(state: WindowState): Boolean {
        var flags = params.flags
        flags = if (state.touchable) flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        else flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        flags = if (state.focusable) flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (params.width == state.width && params.height == state.height && params.flags == flags &&
            params.preferredRefreshRate == state.preferredRefreshRate && params.gravity == state.gravity &&
            params.x == 0 && params.y == 0
        ) {
            return false
        }
        params.width = state.width
        params.height = state.height
        params.flags = flags
        params.preferredRefreshRate = state.preferredRefreshRate
        params.gravity = state.gravity
        params.x = 0
        params.y = 0
        disableMoveAnimation()
        updates++
        runCatching { update(params) }
        return true
    }

    /**
     * The window manager animates a window's move by default; the island animates itself, so that
     * would only add lag. Android 14 has a public switch; older versions need the private flag.
     */
    private fun disableMoveAnimation() {
        if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { params.setCanPlayMoveAnimation(false) }
            return
        }
        try {
            val field = WindowManager.LayoutParams::class.java.getField("privateFlags")
            field.setInt(params, field.getInt(params) or PRIVATE_FLAG_NO_MOVE_ANIMATION)
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val PRIVATE_FLAG_NO_MOVE_ANIMATION = 0x00000040
    }
}
