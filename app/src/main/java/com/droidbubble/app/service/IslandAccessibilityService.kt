package com.droidbubble.app.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.droidbubble.app.island.IslandStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandAccessibilityService : AccessibilityService() {

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()

        var instance: IslandAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null) {
            IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
        }
        IslandOverlayService.onAccessibilityServiceChanged(this)
    }

    private var lastRefreshPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null) {
                IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
            }
            val pkg = event.packageName?.toString() ?: return
            // Track the real foreground app; ignore the shade, keyboards and our own overlay,
            // which also fire window events but don't replace the app the user is in
            if (pkg != packageName && pkg != "com.android.systemui" && !isInputMethod(pkg)) {
                IslandStateManager.setForegroundPackage(pkg)
            }
            if (pkg == IslandStateManager.dismissedMediaPackage) {
                IslandStateManager.unDismissMedia(pkg)
            }
            // B10.2: only when the foreground app changes, not on every window event
            if (pkg != lastRefreshPackage) {
                lastRefreshPackage = pkg
                NotificationReceiverService.instance?.refreshMedia()
            }
        }
    }

    private fun isInputMethod(pkg: String): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        return imm?.enabledInputMethodList?.any { it.packageName == pkg } == true
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        IslandStateManager.setForegroundPackage(null)
        instance = null
        _isConnected.value = false
        IslandOverlayService.onAccessibilityServiceChanged(null)
    }
}
