package com.droidbubble.app.service.window

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import com.droidbubble.app.island.shell.IslandBackGesture

/**
 * M4.5: Back while the island is expanded collapses it (05 R7). Only active while [active] says
 * so (expanded and `IslandSettings.backCollapses`); the window is focusable exactly then
 * ([overlayFocusable]), so the Back goes to the island and the app below never receives it.
 *
 * The overlay has a hand-made lifecycle owner and no `OnBackPressedDispatcherOwner`, so this uses
 * the platform directly instead of Compose's `PredictiveBackHandler`:
 * - API 33+: an `OnBackInvokedCallback` on the window's dispatcher at `PRIORITY_OVERLAY`; on 34+
 *   an `OnBackAnimationCallback`, whose progress previews the shrink ([IslandBackGesture]).
 * - Every API: `KEYCODE_BACK` that nothing in the island handled (the path on API 29–32, and on
 *   newer versions whenever the platform still sends the key).
 */
class OverlayBackHandler(
    private val view: View,
    private val active: () -> Boolean,
) {
    private var callback: Any? = null
    private var dispatcher: Any? = null

    private val keyListener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event -> onKey(event) }

    fun attach() {
        ViewCompat.addOnUnhandledKeyEventListener(view, keyListener)
        sync()
    }

    fun detach() {
        ViewCompat.removeOnUnhandledKeyEventListener(view, keyListener)
        unregister()
    }

    /** Call whenever expanded or the setting changes: the callback is registered only while active. */
    fun sync() {
        if (active()) register() else unregister()
    }

    /** The key path. Consumes Back (down and up) while active; collapses on the up. */
    fun onKey(event: KeyEvent): Boolean =
        handleBackKey(event.keyCode, event.action, event.isCanceled, active(), IslandBackGesture::onInvoked)

    private fun register() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        registerApi33()
    }

    private fun unregister() {
        if (callback == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        unregisterApi33()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerApi33() {
        // The view's window may have changed (the island is re-added above the glass panes)
        val d = view.findOnBackInvokedDispatcher() ?: return
        if (callback != null && d === dispatcher) return
        unregisterApi33()
        val cb: OnBackInvokedCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) animationCallback()
            else OnBackInvokedCallback { IslandBackGesture.onInvoked() }
        d.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb)
        dispatcher = d
        callback = cb
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun unregisterApi33() {
        val d = dispatcher as? OnBackInvokedDispatcher
        val cb = callback as? OnBackInvokedCallback
        if (d != null && cb != null) runCatching { d.unregisterOnBackInvokedCallback(cb) }
        dispatcher = null
        callback = null
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun animationCallback(): OnBackInvokedCallback = object : OnBackAnimationCallback {
        override fun onBackStarted(backEvent: BackEvent) = IslandBackGesture.onStarted()
        override fun onBackProgressed(backEvent: BackEvent) = IslandBackGesture.onProgressed(backEvent.progress)
        override fun onBackCancelled() = IslandBackGesture.onCancelled()
        override fun onBackInvoked() = IslandBackGesture.onInvoked()
    }
}

/**
 * Pure: what a key event does. Back is taken (down and up) only while [active], so with the
 * setting off nothing changes; the up commits unless the system cancelled it.
 */
fun handleBackKey(keyCode: Int, action: Int, canceled: Boolean, active: Boolean, commit: () -> Unit): Boolean {
    if (keyCode != KeyEvent.KEYCODE_BACK || !active) return false
    if (action == KeyEvent.ACTION_UP && !canceled) commit()
    return true
}
