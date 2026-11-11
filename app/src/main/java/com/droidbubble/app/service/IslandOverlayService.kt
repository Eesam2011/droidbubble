package com.droidbubble.app.service

import android.app.KeyguardManager
import android.media.AudioManager
import com.droidbubble.app.island.IslandStateManager.RingerMode
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import kotlin.math.roundToInt
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.droidbubble.app.MainActivity
import com.droidbubble.app.R
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandOverlayRoot
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.scene.IslandEnv
import com.droidbubble.app.island.scene.IslandMode
import com.droidbubble.app.island.scene.IslandScene
import com.droidbubble.app.island.scene.IslandTargets
import com.droidbubble.app.island.scene.deriveIslandScene
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.droidbubble.app.util.traced

class IslandOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "droidbubble_silent_service_v2"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.droidbubble.app.START_OVERLAY"
        const val ACTION_STOP = "com.droidbubble.app.STOP_OVERLAY"
        /** B11.3: the "Resume" action of the safe-mode notification. */
        const val ACTION_SAFE_MODE_RESUME = "com.droidbubble.app.SAFE_MODE_RESUME"
        const val CHANNEL_UPDATES_ID = "droidbubble_app_updates"
        const val UPDATE_NOTIFICATION_ID = 2002

        private var serviceInstance: IslandOverlayService? = null

        /**
         * Tests only: the settings the service reads instead of the DataStore. The DataStore delegate
         * is a process singleton bound to the first test's files dir, so service tests can't use it.
         */
        @androidx.annotation.VisibleForTesting
        @JvmField
        var settingsFlowForTest: kotlinx.coroutines.flow.Flow<IslandSettings>? = null

        fun onAccessibilityServiceChanged(accessibilityService: IslandAccessibilityService?) {
            serviceInstance?.handleAccessibilityServiceChanged(accessibilityService)
        }

        fun start(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    // Lazy: tests that supply settingsFlowForTest never open the process-wide DataStore
    private val settingsRepository by lazy { SettingsRepository(this) }
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val lifecycleOwner = CustomLifecycleOwner()
    private var currentSettings = IslandSettings()
    private var spokenAlerts: SpokenAlerts? = null
    private var spokenAlertsJob: kotlinx.coroutines.Job? = null

    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null
    private var maxTorchStrengthLevel: Int = 1
    private var torchCallback: CameraManager.TorchCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    /** Debug-only test commands (B1.1): registered not exported, and absent from release builds. */
    private var debugReceiver: BroadcastReceiver? = null

    private var lastRingerMode: Int? = null

    // Wi-Fi connected alert: a network callback, since the old CONNECTIVITY_ACTION broadcast
    // is no longer delivered to registered receivers on modern Android
    private var wifiCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var wifiCallbackRegisteredAt = 0L

    private fun registerWifiCallback() {
        if (wifiCallback != null) return
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
        val request = android.net.NetworkRequest.Builder()
            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                // Registration immediately reports an already-connected network; that's not a new connection
                if (android.os.SystemClock.elapsedRealtime() - wifiCallbackRegisteredAt < 2000L) return
                if (!currentSettings.showWifi) return
                serviceScope.launch { IslandStateManager.triggerWifiEvent() }
            }
        }
        wifiCallbackRegisteredAt = android.os.SystemClock.elapsedRealtime()
        runCatching {
            cm.registerNetworkCallback(request, callback)
            wifiCallback = callback
        }
    }

    private fun unregisterWifiCallback() {
        val cb = wifiCallback ?: return
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        runCatching { cm?.unregisterNetworkCallback(cb) }
        wifiCallback = null
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d("IslandOverlayService", "System broadcast received: $action")
            if (action == AudioManager.RINGER_MODE_CHANGED_ACTION || action == "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION") {
                if (!currentSettings.showRingerMode) return
                val mode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)
                val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val currentMode = if (mode != -1) mode else (am?.ringerMode ?: return)
                if (lastRingerMode == currentMode) {
                    return
                }
                lastRingerMode = currentMode
                when (currentMode) {
                    AudioManager.RINGER_MODE_SILENT -> IslandStateManager.triggerRingerModeEvent(RingerMode.SILENT)
                    AudioManager.RINGER_MODE_VIBRATE -> IslandStateManager.triggerRingerModeEvent(RingerMode.VIBRATE)
                    AudioManager.RINGER_MODE_NORMAL -> IslandStateManager.triggerRingerModeEvent(RingerMode.RING)
                }
                return
            }
            if (action == Intent.ACTION_USER_PRESENT) {
                IslandStateManager.setDeviceLocked(false)
                IslandStateManager.triggerUnlockEvent()
                return
            }
            if (action == Intent.ACTION_SCREEN_OFF) {
                val km = context?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                IslandStateManager.setDeviceLocked(km?.isKeyguardLocked ?: true)
                // The lock screen belongs to systemui (ignored), so forget the last app: its
                // notifications arriving while locked must still show
                IslandStateManager.setForegroundPackage(null)
                return
            }
            if (action == Intent.ACTION_SCREEN_ON) {
                val km = context?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val locked = km?.isKeyguardLocked == true
                IslandStateManager.setDeviceLocked(locked)
                if (km != null && !locked) {
                    IslandStateManager.triggerUnlockEvent()
                }
                return
            }
            if (action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED) {
                if (!currentSettings.showBluetooth) return
                IslandStateManager.triggerBluetoothEvent()
                return
            }
            if (action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED" ||
                action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" ||
                action == "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED"
            ) {
                if (!currentSettings.showBluetooth) return
                val state = intent.getIntExtra(android.bluetooth.BluetoothProfile.EXTRA_STATE, -1)
                if (state == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    IslandStateManager.triggerBluetoothEvent()
                    return
                }
            }
            if (action == Intent.ACTION_POWER_CONNECTED || action == "android.os.action.CHARGING") {
                if (!currentSettings.showCharging) return
                val ctx = context ?: return
                val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100).roundToInt().coerceIn(0, 100)
                } else {
                    100
                }
                val wattage = BatteryUtils.getChargingWattage(ctx, batteryIntent)
                IslandStateManager.triggerChargingEvent(pct, wattage)

                // Poll charging wattage at 400ms, 1000ms, 1700ms, and 2400ms so fast-charging negotiation updates live
                serviceScope.launch {
                    val sampleDelays = longArrayOf(400L, 600L, 700L, 700L)
                    for (waitMs in sampleDelays) {
                        delay(waitMs)
                        if (!IslandStateManager.isChargingActive) break
                        val updated = BatteryUtils.getChargingWattage(this@IslandOverlayService)
                        if (updated != null && updated > 0.0) {
                            IslandStateManager.updateChargingWattage(updated)
                        }
                    }
                }
            }
        }
    }

    private fun initCameraTorch() {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            cameraManager = cm
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        maxTorchStrengthLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    }
                    break
                }
            }
            if (torchCameraId == null) {
                for (id in cm.cameraIdList) {
                    val chars = cm.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        torchCameraId = id
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            maxTorchStrengthLevel = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                        }
                        break
                    }
                }
            }

            IslandStateManager.onSetFlashlightLevel = { level1to4 ->
                torchCameraId?.let { id ->
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxTorchStrengthLevel > 1) {
                            val scaled = if (maxTorchStrengthLevel == 4) {
                                level1to4
                            } else {
                                ((level1to4 - 1f) / 3f * (maxTorchStrengthLevel - 1) + 1).roundToInt().coerceIn(1, maxTorchStrengthLevel)
                            }
                            cm.turnOnTorchWithStrengthLevel(id, scaled)
                        } else {
                            cm.setTorchMode(id, true)
                        }
                    }
                }
            }

            IslandStateManager.onTurnOffFlashlight = {
                torchCameraId?.let { id ->
                    runCatching {
                        cm.setTorchMode(id, false)
                    }
                }
            }

            val cb = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (torchCameraId == null && enabled) {
                        torchCameraId = cameraId
                    }
                    if (cameraId == torchCameraId || torchCameraId == null) {
                        // Filtered: flash alerts for calls / notifications blink the torch
                        IslandStateManager.onTorchChanged(enabled)
                    }
                }

                override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
                    super.onTorchStrengthLevelChanged(cameraId, newStrengthLevel)
                    if (cameraId == torchCameraId) {
                        val level1to4 = if (maxTorchStrengthLevel <= 1) {
                            1
                        } else if (maxTorchStrengthLevel == 4) {
                            newStrengthLevel.coerceIn(1, 4)
                        } else {
                            ((newStrengthLevel - 1f) / (maxTorchStrengthLevel - 1).toFloat() * 3f + 1).roundToInt().coerceIn(1, 4)
                        }
                        IslandStateManager.updateFlashlightLevel(level1to4)
                    }
                }
            }
            torchCallback = cb
            cm.registerTorchCallback(cb, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        DebugCommands.enableStrictMode()
        com.droidbubble.app.util.CrashLog.install(this)
        serviceInstance = this
        // F3.5: island timeouts follow the phone's "Time to take action" accessibility setting
        IslandStateManager.timings = com.droidbubble.app.island.IslandTimings.from(this)
        // B11.3: after a crash loop, pause the activity that was on the island at the crash
        com.droidbubble.app.util.SafeMode.evaluateOnStart(this, System.currentTimeMillis())
        val appContext = applicationContext
        IslandStateManager.safeModeFilter = { info ->
            com.droidbubble.app.util.SafeMode.activePause(appContext, System.currentTimeMillis())
                ?.covers(info.packageName, com.droidbubble.app.island.liveActivityKind(info).name) == true
        }
        // B8.4: no blocking settings read here. currentSettings keeps its defaults until the first
        // emission of settingsFlow, and the overlay and telemetry wait for it (startSettingsCollector).
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null) {
            IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
        }
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        createNotificationChannel()
        initCameraTorch()
        startAutoUpdateChecker()

        debugReceiver = DebugCommands.register(this)

        // System events (B1.2): sent by the system process, which a not-exported receiver still
        // hears, while other apps can't reach it.
        val systemFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction("android.os.action.CHARGING")
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")
        }
        ContextCompat.registerReceiver(this, powerReceiver, systemFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Bluetooth events come from the Bluetooth app's own UID, not the system's, so a
        // not-exported receiver would drop them. They are protected broadcasts: only the system
        // and the Bluetooth stack can send them, so exporting this filter lets no app in.
        val bluetoothFilter = IntentFilter().apply {
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
        }
        ContextCompat.registerReceiver(this, powerReceiver, bluetoothFilter, ContextCompat.RECEIVER_EXPORTED)

        registerWifiCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SAFE_MODE_RESUME) {
            com.droidbubble.app.util.SafeMode.resume(this)
        }
        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()

        if (Settings.canDrawOverlays(this)) {
            // B8.4: the overlay is added by the settings collector once the real settings arrive,
            // so a disabled island or a custom size never shows wrong for a moment
            if (settingsReceived && composeView == null && currentSettings.isEnabled) initOverlay()
            startSettingsCollector()
        } else {
            stopSelf()
        }

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateWindowState(IslandStateManager.isExpanded.value)
    }

    private fun startForegroundService() {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_island)
            .setContentTitle("Droidbubble is active")
            .setContentText("Dynamic Island is running")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        // B11.3: say what safe mode paused, with a way to undo it at once
        com.droidbubble.app.util.SafeMode.activePause(this, System.currentTimeMillis())?.let { pause ->
            val appName = runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pause.pkg, 0)).toString()
            }.getOrDefault(pause.pkg)
            val resume = PendingIntent.getService(
                this, 1,
                Intent(this, IslandOverlayService::class.java).setAction(ACTION_SAFE_MODE_RESUME),
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentTitle("Paused $appName on the island")
                .setContentText("After repeated crashes. It comes back in an hour.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .addAction(0, "Resume", resume)
        }
        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private var touchManager: IslandTouchManager? = null

    private fun disableMoveAnimation(params: WindowManager.LayoutParams) {
        try {
            val field = WindowManager.LayoutParams::class.java.getField("privateFlags")
            val current = field.getInt(params)
            field.setInt(params, current or 0x00000040)
        } catch (_: Exception) {}
    }



    fun handleAccessibilityServiceChanged(accessibilityService: IslandAccessibilityService?) {
        if (composeView != null) {
            removeOverlay()
            if (Settings.canDrawOverlays(this) && currentSettings.isEnabled) {
                initOverlay()
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun initOverlay() {
        if (composeView != null) return

        val aService = IslandAccessibilityService.instance
        windowManager = if (aService != null) {
            aService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        } else {
            getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        val density = resources.displayMetrics.density
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // F1.4: the first window comes from the same scene as every later one. Not settled: the
        // pill may still be appearing.
        val initialWindow = windowScene(expanded = false)
            .let { compactWindowSpec(it.scene, it.targets, currentSettings, settled = false) }
            .toWindowPx(density)
        val initialWidth = initialWindow.width
        val initialHeight = initialWindow.height
        val initialFlags = if (initialWindow.touchable) flags else (flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        val windowType = if (aService != null) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            windowType,
            initialFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
            windowAnimations = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Screen-space origin, so card bounds map 1:1 to the liquid glass blur windows
                fitInsetsTypes = 0
                fitInsetsSides = 0
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        disableMoveAnimation(layoutParams!!)

        val view = ComposeView(aService ?: this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            fitsSystemWindows = false

            setContent {
                val storedSettings by settingsFlow.collectAsState(initial = currentSettings)
                // High contrast: the app's own setting, or Android's "High contrast text"
                // B5.3: followed live, so switching it in Accessibility settings restyles the island at once
                val systemHighContrast by remember { com.droidbubble.app.util.highTextContrastFlow(contentResolver) }
                    .collectAsState(initial = com.droidbubble.app.util.readHighTextContrast(contentResolver))
                val highContrast = storedSettings.highContrast || systemHighContrast
                SideEffect { com.droidbubble.app.island.IslandTokens.highContrast = highContrast }
                val liveSettings = if (highContrast) {
                    storedSettings.copy(
                        // The colour comes from IslandTokens.HighContrastOutline in the view (B5.1)
                        showOutline = true,
                        outlineThicknessDp = storedSettings.outlineThicknessDp.coerceAtLeast(1.5f)
                    )
                } else {
                    storedSettings
                }

                MaterialExpressiveTheme(
                    colorScheme = darkColorScheme(),
                    motionScheme = MotionScheme.expressive()
                ) {
                    // Tabular digits island-wide: timers, speeds and percentages tick without
                    // their width wobbling (only digits are affected; letters stay proportional).
                    // Material's body style adds 0.5sp tracking, which looks loose on the island:
                    // use the font's own spacing instead.
                    // Large text: the island follows the phone's font size setting, capped at 1.5x
                    // because the pill around the camera can't grow without limit.
                    val systemDensity = LocalDensity.current
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(fontFeatureSettings = "tnum", letterSpacing = 0.sp),
                        LocalDensity provides Density(systemDensity.density, systemDensity.fontScale.coerceIn(0.85f, 1.5f))
                    ) {
                        IslandOverlayRoot(
                            settings = liveSettings
                        )
                    }
                }
            }
        }

        composeView = view

        overlayJobs.forEach { it.cancel() }
        overlayJobs.clear()

        // Liquid glass blur windows go in first so they always stay beneath the island. B8.3: they
        // exist only while "Liquid glass" is on (no windows or URGENT_DISPLAY threads otherwise),
        // and the island window is added once they're on screen instead of blocking for them.
        glassContext = aService ?: this
        glassWindowType = windowType
        glassPaneWindows?.dismiss()
        glassPaneWindows = null
        if (currentSettings.liquidGlassCards) {
            glassPaneWindows = GlassPaneWindows(glassContext!!, windowType).also { panes ->
                panes.create { if (composeView === view && glassPaneWindows === panes) addIslandView(view) }
            }
        } else {
            addIslandView(view)
        }

        glassPaneJob?.cancel()
        glassPaneJob = null
        // Card rects go to the blur windows straight from the island's frame (no queue hop); the
        // windows apply them on their own thread, so the island never waits for a pane relayout
        IslandStateManager.glassPaneListener = { panes ->
            glassPaneWindows?.update(if (currentSettings.liquidGlassCards) panes else emptyList())
        }
        glassPaneWindows?.update(
            if (currentSettings.liquidGlassCards) IslandStateManager.glassPanes.value else emptyList()
        )

        // Listen for expansion and state changes to resize WindowManager touch bounds
        overlayJobs += serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                IslandStateManager.isExpanded,
                kotlinx.coroutines.flow.combine(
                    IslandStateManager.activeMedia,
                    IslandStateManager.activeNotification,
                    IslandStateManager.activeProgress,
                    IslandStateManager.screenRecordingState,
                    IslandStateManager.activeCallState
                ) { _, _, _, _, _ -> Unit },
                kotlinx.coroutines.flow.combine(
                    IslandStateManager.activeCharging,
                    IslandStateManager.isUnlocked,
                    IslandStateManager.isBluetoothConnected,
                    IslandStateManager.activeRingerMode,
                    IslandStateManager.isFlashlightOn
                ) { _, _, _, _, _ -> Unit },
                IslandStateManager.isDeviceLocked,
                kotlinx.coroutines.flow.combine(
                    IslandStateManager.userHidden,
                    IslandStateManager.userMinimized,
                    // B2.1: an app's own media / alert hides while it is in front, and so must the window
                    IslandStateManager.foregroundPackage
                ) { _, _, _ -> Unit }
            ) { expanded, _, _, _, _ -> expanded }.collectLatest { expanded ->
                val shapeKey = listOf(
                    expanded,
                    IslandStateManager.isChargingActive, IslandStateManager.isUnlockActive,
                    IslandStateManager.isBluetoothActive, IslandStateManager.isRingerModeActive,
                    IslandStateManager.isRecordingActive, IslandStateManager.isNotificationActive,
                    IslandStateManager.isMediaActive, IslandStateManager.isProgressActive,
                    IslandStateManager.isFlashlightActive,
                    IslandStateManager.isDeviceLocked.value,
                    IslandStateManager.userHidden.value, IslandStateManager.userMinimized.value,
                    // B2.1: an app's own cards hide while it is in front; the scene knows what is drawn
                    windowScene(expanded).scene.visible
                )
                if (shapeKey != lastShapeKey) {
                    lastShapeKey = shapeKey
                    boostRefreshRate()
                    startMorphHold()
                }
                updateWindowState(expanded)
            }
        }

        // Listen for keyboard focus state to enable soft keyboard for inline replies
        overlayJobs += serviceScope.launch {
            IslandStateManager.isKeyboardFocused.collectLatest { focused ->
                val view = composeView ?: return@collectLatest
                val params = layoutParams ?: return@collectLatest
                if (focused) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                disableMoveAnimation(params)
                runCatching { windowManager?.updateViewLayout(view, params) }
            }
        }

        startSettingsCollector()
    }

    /**
     * Listens for settings changes. Started once per service: it adds and removes the overlay
     * itself, so it must survive removeOverlay() and not be duplicated. The first emission is the
     * real settings (B8.4): it starts telemetry and adds the overlay.
     */
    private fun startSettingsCollector() {
        if (settingsJob == null) settingsJob = serviceScope.launch {
            settingsFlow.collectLatest { settings ->
                currentSettings = settings
                updateSpokenAlerts(settings.spokenAlerts)
                settingsReceived = true
                IslandStateManager.dismissMediaOnPause = settings.dismissMediaOnPause
                IslandStateManager.autoDismissNotifications = settings.autoDismissNotifications
                if (!settings.isEnabled) {
                    removeOverlay()
                } else if (composeView == null) {
                    initOverlay()
                } else {
                    syncGlassPanes(settings.liquidGlassCards)
                    updateWindowState(IslandStateManager.isExpanded.value)
                }
            }
        }
    }

    private var isCollapsing = false
    private val overlayJobs = mutableListOf<kotlinx.coroutines.Job>()
    private var settingsJob: kotlinx.coroutines.Job? = null
    private var settingsReceived = false

    @androidx.annotation.VisibleForTesting
    internal val overlayViewForTest: View? get() = composeView
    private var lastShapeKey: List<Any>? = null
    private val settingsFlow: kotlinx.coroutines.flow.Flow<IslandSettings>
        get() = settingsFlowForTest ?: settingsRepository.settingsFlow

    private var glassPaneWindows: GlassPaneWindows? = null
    private var glassContext: Context? = null
    private var glassWindowType: Int = 0

    @androidx.annotation.VisibleForTesting
    internal val glassPanesForTest: GlassPaneWindows? get() = glassPaneWindows

    private fun addIslandView(view: View) {
        if (view.parent != null) return
        try {
            windowManager?.addView(view, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * B8.3: "Liquid glass" switched while the island is up. On: add the panes, then re-add the
     * island so it sits above them. Off: remove the panes and their threads.
     */
    private fun syncGlassPanes(on: Boolean) {
        val view = composeView ?: return
        if (on && glassPaneWindows == null) {
            val context = glassContext ?: return
            glassPaneWindows = GlassPaneWindows(context, glassWindowType).also { panes ->
                panes.create {
                    if (composeView !== view || glassPaneWindows !== panes) return@create
                    if (view.parent != null) runCatching { windowManager?.removeViewImmediate(view) }
                    addIslandView(view)
                    panes.update(IslandStateManager.glassPanes.value)
                }
            }
        } else if (!on && glassPaneWindows != null) {
            glassPaneWindows?.dismiss()
            glassPaneWindows = null
            IslandStateManager.setGlassBlurAvailable(false)
        }
    }
    private var glassPaneJob: kotlinx.coroutines.Job? = null
    private var collapseJob: kotlinx.coroutines.Job? = null

    // Ask for the display's top refresh rate only while the island animates. Between animations
    // the system is free to drop to its low idle rate, so the extra smoothness costs no battery.
    private var refreshBoostJob: kotlinx.coroutines.Job? = null
    private val maxRefreshRate: Float by lazy {
        runCatching {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            val current = display?.mode
            display?.supportedModes
                ?.filter { current == null || (it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight) }
                ?.maxOfOrNull { it.refreshRate } ?: 0f
        }.getOrDefault(0f)
    }

    private fun boostRefreshRate() = traced("db.boostRefreshRate") { boostRefreshRateTraced() }

    private fun boostRefreshRateTraced() {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val rate = maxRefreshRate
        if (rate <= 60f) return
        if (params.preferredRefreshRate != rate) {
            params.preferredRefreshRate = rate
            disableMoveAnimation(params)
            runCatching { windowManager?.updateViewLayout(view, params) }
        }
        refreshBoostJob?.cancel()
        refreshBoostJob = serviceScope.launch {
            // Long enough for the slowest spring at the lowest animation-speed setting
            delay((1200L / currentSettings.animationSpeed.coerceIn(0.5f, 2f)).toLong())
            val v = composeView ?: return@launch
            val p = layoutParams ?: return@launch
            if (p.preferredRefreshRate == 0f) return@launch
            p.preferredRefreshRate = 0f
            disableMoveAnimation(p)
            runCatching { windowManager?.updateViewLayout(v, p) }
        }
    }
    private var wasExpanded = false

    private fun applyLayoutParams(
        view: View,
        params: WindowManager.LayoutParams,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        gravity: Int,
        flags: Int
    ): Unit = traced("db.applyLayoutParams") {
        if (params.width == width &&
            params.height == height &&
            params.x == x &&
            params.y == y &&
            params.gravity == gravity &&
            params.flags == flags
        ) {
            return@traced
        }
        params.width = width
        params.height = height
        params.x = x
        params.y = y
        params.gravity = gravity
        params.flags = flags
        disableMoveAnimation(params)
        runCatching { windowManager?.updateViewLayout(view, params) }
    }

    private class WindowScene(val scene: IslandScene, val targets: IslandTargets)

    /** The scene the view is drawing and its targets, derived from the current state and settings (F1.4). */
    private fun windowScene(expanded: Boolean): WindowScene {
        val config = resources.configuration
        val env = IslandEnv(
            isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = config.screenWidthDp,
            // The same clamp the overlay content applies to the font scale
            fontScale = config.fontScale.coerceIn(0.85f, 1.5f),
        )
        val scene = deriveIslandScene(IslandStateManager.snapshotInputs().copy(isExpanded = expanded), currentSettings, env)
        return WindowScene(scene, windowTargets(scene, currentSettings, env))
    }

    private fun windowSpec(ws: WindowScene): WindowSpec =
        compactWindowSpec(ws.scene, ws.targets, currentSettings, settled = android.os.SystemClock.uptimeMillis() >= morphUntilMs)

    // B2.2: until this time the compact window keeps the wide envelope (a shape change animates)
    private var morphUntilMs = 0L
    private var settleJob: kotlinx.coroutines.Job? = null

    /** A shape change: wide envelope now, then tighten once the island has settled. */
    private fun startMorphHold() {
        val hold = morphHoldMs(currentSettings.animationSpeed)
        morphUntilMs = android.os.SystemClock.uptimeMillis() + hold
        settleJob?.cancel()
        settleJob = serviceScope.launch {
            kotlinx.coroutines.delay(hold + 16)
            updateWindowState(IslandStateManager.isExpanded.value)
        }
    }

    private var lastLoggedSpec: WindowSpec? = null

    private fun updateWindowState(expanded: Boolean) {
        val view = composeView ?: return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density

        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null) {
            IslandStateManager.setDeviceLocked(km.isKeyguardLocked)
        }
        val ws = windowScene(expanded)
        val spec = windowSpec(ws)
        if (spec != lastLoggedSpec) {
            lastLoggedSpec = spec
            com.droidbubble.app.util.CrashLog.breadcrumb("window", "spec=$spec")
        }

        if (ws.scene.mode == IslandMode.OFF) {
            collapseJob?.cancel()
            isCollapsing = false
            applyLayoutParams(
                view = view,
                params = params,
                width = 1,
                height = 1,
                x = 0,
                y = 0,
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            return
        }

        if (expanded) {
            collapseJob?.cancel()
            isCollapsing = false
            wasExpanded = true
            // Full screen touch area to allow tapping outside to dismiss
            applyLayoutParams(
                view = view,
                params = params,
                width = WindowManager.LayoutParams.MATCH_PARENT,
                height = WindowManager.LayoutParams.MATCH_PARENT,
                x = 0,
                y = 0,
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            )
        } else {
            val isOccupied = ws.scene.isOccupied

            fun applyCompactBounds() {
                collapseJob?.cancel()
                if (spec is WindowSpec.Compact) {
                    val px = spec.toWindowPx(density)
                    val targetFlags = if (px.touchable) {
                        params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                    } else {
                        params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    }
                    applyLayoutParams(
                        view = view,
                        params = params,
                        width = px.width,
                        height = px.height,
                        x = 0,
                        y = 0,
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                        flags = targetFlags
                    )
                } else {
                    applyLayoutParams(
                        view = view,
                        params = params,
                        width = 1,
                        height = 1,
                        x = 0,
                        y = 0,
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                        flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    )
                }
            }

            if (ws.scene.hasAlert) {
                isCollapsing = false
                collapseJob?.cancel()
            } else if (isCollapsing) {
                return
            }

            val speedFactor = currentSettings.animationSpeed.coerceIn(0.4f, 2.5f)

            if (wasExpanded) {
                wasExpanded = false
                isCollapsing = true
                // Keep MATCH_PARENT until the island reports the collapse animation has finished
                // (it scales with the animation-speed setting); the timeout is only a safety net.
                collapseJob = serviceScope.launch {
                    kotlinx.coroutines.withTimeoutOrNull(1500L) { IslandStateManager.collapseSettled.first { it } }
                    isCollapsing = false
                    // Recompute from the state NOW (a notification may have arrived or a card ended
                    // during the collapse), not from what was captured when the collapse began
                    if (!IslandStateManager.isExpanded.value) {
                        updateWindowState(false)
                    }
                }
            } else if (!isOccupied && !currentSettings.alwaysShowPill) {
                // When unoccupied and not always showing pill and no persistent widgets, keep bounds briefly for the smooth shrink-to-middle animation, then collapse
                collapseJob?.cancel()
                collapseJob = serviceScope.launch {
                    kotlinx.coroutines.delay((420f / speedFactor).toLong().coerceIn(320L, 750L))
                    if (!isOccupied && !IslandStateManager.isExpanded.value && !currentSettings.alwaysShowPill) {
                        applyLayoutParams(
                            view = view,
                            params = params,
                            width = 1,
                            height = 1,
                            x = 0,
                            y = 0,
                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                            flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        )
                    }
                }
            } else {
                applyCompactBounds()
            }
        }
    }

    private fun removeOverlay() {
        overlayJobs.forEach { it.cancel() }
        overlayJobs.clear()
        collapseJob?.cancel()
        refreshBoostJob?.cancel()
        isCollapsing = false
        wasExpanded = false
        lastShapeKey = null
        glassPaneJob?.cancel()
        glassPaneJob = null
        IslandStateManager.glassPaneListener = null
        glassPaneWindows?.dismiss()
        glassPaneWindows = null
        composeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
    }

    /** Spoken alerts (Accessibility setting): speech engine and listener exist only while it's on. */
    private fun updateSpokenAlerts(enabled: Boolean) {
        if (enabled) {
            val alerts = spokenAlerts ?: SpokenAlerts(this).also { spokenAlerts = it }
            alerts.setEnabled(true)
            if (spokenAlertsJob == null) spokenAlertsJob = serviceScope.launch {
                IslandStateManager.activeNotification.collect { alerts.onNotification(it) }
            }
        } else {
            spokenAlertsJob?.cancel()
            spokenAlertsJob = null
            spokenAlerts?.shutdown()
            spokenAlerts = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        spokenAlertsJob?.cancel()
        spokenAlertsJob = null
        spokenAlerts?.shutdown()
        spokenAlerts = null
        if (serviceInstance === this) {
            serviceInstance = null
        }
        DebugCommands.unregister(this, debugReceiver)
        debugReceiver = null
        unregisterWifiCallback()
        runCatching { unregisterReceiver(powerReceiver) }
        torchCallback?.let { cb ->
            runCatching { cameraManager?.unregisterTorchCallback(cb) }
        }
        torchCallback = null
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        removeOverlay()
        serviceScope.cancel()
    }


    private fun startAutoUpdateChecker() {
        serviceScope.launch(Dispatchers.IO) {
            delay(15_000L)
            while (isActive) {
                try {
                    val result = com.droidbubble.app.util.UpdateManager.checkGithubUpdates(applicationContext)
                    if (result is com.droidbubble.app.util.UpdateCheckResult.UpdateAvailable) {
                        showUpdateNotification(result.release.version)
                    } else if (result is com.droidbubble.app.util.UpdateCheckResult.UpToDate) {
                        cancelUpdateNotification()
                    }
                } catch (e: Exception) {
                    Log.w("IslandOverlayService", "Auto-update check encountered error", e)
                }
                delay(2 * 60 * 60 * 1000L)
            }
        }
    }

    private fun showUpdateNotification(version: String) {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a new Droidbubble update is available"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_SHOW_UPDATE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            UPDATE_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_notification_island)
            .setContentTitle("Update to $version")
            .setContentText("Update to $version")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    private fun cancelUpdateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        notificationManager.cancel(UPDATE_NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            runCatching {
                manager?.deleteNotificationChannel("droidbubble_overlay_channel")
                manager?.deleteNotificationChannel("droidbubble_overlay_service")
            }
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Droidbubble Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps Dynamic Island running smoothly"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            manager?.createNotificationChannel(channel)
        }
    }
}

// Custom LifecycleOwner for Compose in WindowManager Service
class CustomLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}
