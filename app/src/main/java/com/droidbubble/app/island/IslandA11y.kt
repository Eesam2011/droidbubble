package com.droidbubble.app.island

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * C3.2: the island's accessibility environment. Answers "is a screen reader on?" and "how long
 * should things stay up?".
 *
 * Not `AccessibilityManager.isEnabled`: that is true whenever Droidbubble's own accessibility
 * service (the status bar layer) is on. A screen reader is touch exploration, or any enabled
 * spoken-feedback service from another package.
 */
object IslandA11y {

    private val _screenReaderOn = MutableStateFlow(false)
    val screenReaderOn: StateFlow<Boolean> = _screenReaderOn.asStateFlow()

    private var registered: Registration? = null

    /** Pure rule: [spokenServicePackages] are the packages of the enabled spoken-feedback services. */
    fun isScreenReader(touchExploration: Boolean, spokenServicePackages: Collection<String>, ourPackage: String): Boolean =
        touchExploration || spokenServicePackages.any { it != ourPackage }

    /** Re-reads the state from [am]. */
    fun refresh(am: AccessibilityManager, ourPackage: String) {
        val spoken = runCatching {
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .filter { it.feedbackType and AccessibilityServiceInfo.FEEDBACK_SPOKEN != 0 }
                .map { info -> info.resolveInfo?.serviceInfo?.packageName ?: info.id.orEmpty().substringBefore('/') }
        }.getOrDefault(emptyList())
        _screenReaderOn.value = isScreenReader(am.isTouchExplorationEnabled, spoken, ourPackage)
    }

    /** Starts following the phone's accessibility state (the overlay service's onCreate). */
    fun register(context: Context) {
        unregister()
        val am = context.getSystemService(AccessibilityManager::class.java) ?: return
        val pkg = context.packageName
        val onTouch = AccessibilityManager.TouchExplorationStateChangeListener { refresh(am, pkg) }
        val onState = AccessibilityManager.AccessibilityStateChangeListener { refresh(am, pkg) }
        am.addTouchExplorationStateChangeListener(onTouch)
        am.addAccessibilityStateChangeListener(onState)
        // A spoken service turning on while ours is already on changes neither of the above
        val onServices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AccessibilityManager.AccessibilityServicesStateChangeListener { refresh(am, pkg) }
                .also { am.addAccessibilityServicesStateChangeListener(context.mainExecutor, it) }
        } else null
        registered = Registration(am, onTouch, onState, onServices)
        refresh(am, pkg)
    }

    /** Stops following it (the overlay service's onDestroy). */
    fun unregister() {
        val r = registered ?: return
        registered = null
        runCatching { r.am.removeTouchExplorationStateChangeListener(r.onTouch) }
        runCatching { r.am.removeAccessibilityStateChangeListener(r.onState) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (r.onServices as? AccessibilityManager.AccessibilityServicesStateChangeListener)
                ?.let { runCatching { r.am.removeAccessibilityServicesStateChangeListener(it) } }
        }
    }

    /**
     * How long something with text and controls should stay up when it would normally stay
     * [baseMs]: the phone's "Time to take action" setting, through F3.5's [IslandTimings].
     */
    fun recommendedTimeoutMs(baseMs: Int): Long = IslandStateManager.timings.recommended(baseMs)

    @VisibleForTesting
    fun setScreenReaderOnForTest(on: Boolean) {
        _screenReaderOn.value = on
    }

    private class Registration(
        val am: AccessibilityManager,
        val onTouch: AccessibilityManager.TouchExplorationStateChangeListener,
        val onState: AccessibilityManager.AccessibilityStateChangeListener,
        val onServices: Any?,
    )
}
