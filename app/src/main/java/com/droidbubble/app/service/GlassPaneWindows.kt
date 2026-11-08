package com.droidbubble.app.service

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.droidbubble.app.R
import com.droidbubble.app.island.IslandStateManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Liquid-glass backing for the expanded island cards.
 *
 * Android can only blur what is behind a *window*, never behind part of one, and the island
 * window covers the whole screen while expanded. So each card gets a tiny window of its own,
 * placed exactly under it, whose background blur is clipped to the card's rounded corners
 * (Window.setBackgroundBlurRadius is the only public API that clips blur to a rounded rect).
 * The windows are added before the island window so they always sit beneath it, and shrink to
 * 1x1 px with no blur when the cards are collapsed or the option is off.
 *
 * The panes follow the cards on every frame of the expand / collapse animation. A trace showed
 * each resize waits about 28 ms (up to 80 ms) on the system's window manager, so:
 * - each pane has **its own thread** with its own looper and frame clock, so the island never
 *   waits for a pane and the two panes never wait for each other;
 * - those threads run at urgent-display priority, the same class as the system's own UI work,
 *   so they get the fast CPU cores instead of queueing on the slow ones.
 * Everything that touches a pane runs on that pane's thread only.
 */
class GlassPaneWindows(private val context: Context, private val windowType: Int) {

    private val blurRadiusPx = (30 * context.resources.displayMetrics.density).toInt()
    private val windowManager = context.getSystemService(WindowManager::class.java)
    // Replaced wholesale (never mutated), so update() can read it from any thread
    @Volatile private var workers: List<PaneWorker> = emptyList()

    @Volatile private var lastTargets: List<IslandStateManager.GlassPane> = emptyList()

    // Blur can be switched off at runtime (battery saver, developer option): re-apply when it changes
    private val blurListener = Consumer<Boolean> { enabled ->
        IslandStateManager.setGlassBlurAvailable(enabled)
        update(lastTargets)
    }

    private val isBlurSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager?.isCrossWindowBlurEnabled == true

    /** One blur window and the thread that owns it. */
    private inner class PaneWorker(index: Int) {
        val thread = HandlerThread("GlassPane$index", Process.THREAD_PRIORITY_URGENT_DISPLAY).apply { start() }
        val handler = Handler(thread.looper)

        // Pane-thread state
        var dialog: Dialog? = null
        val background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        var appliedBlurRadius = -1

        // Newest card rect for this pane (null = no card); the thread applies only the latest, so
        // if it falls behind it jumps straight to where the card is now
        private val pending = AtomicReference<Pending?>(null)
        private val scheduled = AtomicBoolean(false)
        private val applyLatest = Runnable {
            scheduled.set(false)
            pending.getAndSet(null)?.let { apply(it.target) }
        }

        fun post(target: IslandStateManager.GlassPane?) {
            pending.set(Pending(target))
            // Jump the queue: a pane update should never wait behind its own previous frame's work
            if (scheduled.compareAndSet(false, true)) handler.postAtFrontOfQueue(applyLatest)
        }

        fun create(): Boolean {
            // Explicit translucent theme: Dialog(ctx) would otherwise pick the opaque system dialog theme
            val d = Dialog(context, R.style.Theme_Droidbubble_GlassPane)
            d.setContentView(View(d.context))
            d.setCancelable(false)
            d.window?.apply {
                setType(windowType)
                setFormat(PixelFormat.TRANSLUCENT)
                setWindowAnimations(0)
                setDimAmount(0f)
                setGravity(Gravity.TOP or Gravity.START)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                setBackgroundDrawable(background)
                attributes = attributes.apply {
                    width = 1
                    height = 1
                    x = 0
                    y = 0
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    // Same origin as the island window, so card rects map 1:1 to pane positions
                    fitInsetsTypes = 0
                    fitInsetsSides = 0
                    // Without this the system animates every pane move on its own clock, so the
                    // blur slides after the card instead of jumping with it each frame
                    disableMoveAnimation(this)
                }
            }
            return runCatching { d.show() }
                .onSuccess { dialog = d }
                .onFailure { Log.w(TAG, "Could not show glass pane", it) }
                .isSuccess
        }

        /** Pane thread only. */
        private fun apply(target: IslandStateManager.GlassPane?) {
            val window = dialog?.window ?: return
            val visible = target?.takeIf { isBlurSupported }
            runCatching {
                if (visible == null) {
                    // Only touch a parked pane once: re-sending the same 1x1 layout every frame
                    // still costs a window relayout round trip to the system
                    setBlurRadius(window, 0)
                    setFrame(window, 0, 0, 1, 1)
                } else {
                    // The blur is clipped to the background drawable's outline: it gets the card's
                    // size and corner radius each frame so it matches the card while it morphs
                    val cornerChanged = background.cornerRadius != visible.cornerRadiusPx
                    if (cornerChanged) background.cornerRadius = visible.cornerRadiusPx
                    background.setBounds(0, 0, visible.width, visible.height)
                    if (window.decorView.background !== background) window.setBackgroundDrawable(background)
                    // Pure moves are applied by the system to the surface directly; a resize
                    // redraws the pane by itself, and a new blur radius invalidates on its own.
                    // So only a corner change needs an explicit redraw.
                    setFrame(window, visible.left, visible.top, visible.width, visible.height)
                    setBlurRadius(window, (blurRadiusPx * visible.strength.coerceIn(0f, 1f)).toInt())
                    if (cornerChanged) window.decorView.invalidate()
                }
            }.onFailure { Log.w(TAG, "Could not update glass pane", it) }
        }

        private fun setBlurRadius(window: Window, radius: Int) {
            if (appliedBlurRadius == radius) return
            window.setBackgroundBlurRadius(radius)
            appliedBlurRadius = radius
        }

        fun dismiss() {
            pending.set(null)
            handler.removeCallbacks(applyLatest)
            handler.post {
                runCatching { dialog?.dismiss() }
                dialog = null
                thread.quitSafely()
            }
        }
    }

    private class Pending(val target: IslandStateManager.GlassPane?)

    /**
     * Adds the panes, each on its own thread. Blocks briefly until they're on screen, because they
     * must be added before the island window to stay beneath it.
     */
    fun create(count: Int = 2) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            IslandStateManager.setGlassBlurAvailable(false)
            return
        }
        val added = CountDownLatch(count)
        val created = List(count) { PaneWorker(it) }
        workers = created
        created.forEach { worker ->
            worker.handler.post {
                try {
                    worker.create()
                } finally {
                    added.countDown()
                }
            }
        }
        if (!added.await(1500, TimeUnit.MILLISECONDS)) {
            Log.w(TAG, "Glass panes took too long to appear")
        }
        IslandStateManager.setGlassBlurAvailable(isBlurSupported)
        workers.firstOrNull()?.let { first ->
            runCatching { windowManager?.addCrossWindowBlurEnabledListener({ first.handler.post(it) }, blurListener) }
        }
    }

    /** Thread-safe: hands each pane its newest card rect without waiting for it. */
    fun update(targets: List<IslandStateManager.GlassPane>) {
        lastTargets = targets
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        workers.forEachIndexed { index, worker -> worker.post(targets.getOrNull(index)) }
    }

    private fun setFrame(window: Window, x: Int, y: Int, width: Int, height: Int) {
        val attrs = window.attributes
        if (attrs.width == width && attrs.height == height && attrs.x == x && attrs.y == y) return
        window.attributes = attrs.apply {
            this.width = width
            this.height = height
            this.x = x
            this.y = y
        }
    }

    private fun disableMoveAnimation(params: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            params.setCanPlayMoveAnimation(false)
            return
        }
        // PRIVATE_FLAG_NO_MOVE_ANIMATION, the same flag the island window sets
        runCatching {
            val field = WindowManager.LayoutParams::class.java.getField("privateFlags")
            field.setInt(params, field.getInt(params) or 0x00000040)
        }
    }

    /** Removes the panes on their threads, then ends the threads. Safe to call from any thread. */
    fun dismiss() {
        lastTargets = emptyList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { windowManager?.removeCrossWindowBlurEnabledListener(blurListener) }
        }
        val old = workers
        workers = emptyList()
        old.forEach { it.dismiss() }
    }

    private companion object {
        const val TAG = "GlassPaneWindows"
    }
}
