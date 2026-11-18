package com.droidbubble.app.island

import com.droidbubble.app.util.CrashLog
import com.droidbubble.app.island.activity.ActivityId
import com.droidbubble.app.island.activity.ActivityKind
import com.droidbubble.app.island.activity.ActivityPayload
import com.droidbubble.app.island.activity.ActivitySource
import com.droidbubble.app.island.activity.ActivityStore
import com.droidbubble.app.island.activity.LegacySlots
import com.droidbubble.app.island.activity.LegacySpecMapper
import com.droidbubble.app.island.activity.projectLegacySlots
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.model.IslandPriority
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How long a torch blink sequence is remembered when telling a flashlight from a flash alert. */
const val TORCH_FLICKER_WINDOW_MS = 4000L

/**
 * How long the torch must stay on before the flashlight pill shows. A normal switch-on waits
 * briefly; a torch that has been toggling (flash alerts for calls / notifications) must stay on
 * much longer than any blink.
 */
fun torchShowDelayMs(recentToggles: Int): Long = if (recentToggles >= 3) 1600L else 450L

object IslandStateManager {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var popInJob: Job? = null
    private var chargingJob: Job? = null

    data class ChargingInfo(
        val percentage: Int,
        val wattage: Double? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _activeCharging = MutableStateFlow<ChargingInfo?>(null)
    val activeCharging: StateFlow<ChargingInfo?> = _activeCharging.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()
    private var unlockJob: Job? = null

    // V1.1: the hardware camera, measured from the display cutout by the overlay service
    private val _cameraGeometry = MutableStateFlow(com.droidbubble.app.island.layout.CameraGeometry.None)
    val cameraGeometry: StateFlow<com.droidbubble.app.island.layout.CameraGeometry> = _cameraGeometry.asStateFlow()

    /** V1.3, debug builds: draw a thin ring where the camera was measured (Test panel switch). */
    val showCameraOutline = MutableStateFlow(false)

    fun setCameraGeometry(geometry: com.droidbubble.app.island.layout.CameraGeometry) {
        if (_cameraGeometry.value == geometry) return
        android.util.Log.d("IslandStateManager", "camera: $geometry")
        _cameraGeometry.value = geometry
    }

    private val _isDeviceLocked = MutableStateFlow(false)
    val isDeviceLocked: StateFlow<Boolean> = _isDeviceLocked.asStateFlow()

    fun setDeviceLocked(locked: Boolean) {
        if (_isDeviceLocked.value != locked) {
            _isDeviceLocked.value = locked
            if (locked && _isExpanded.value) {
                collapse()
            }
        }
    }

    // Connection alert (Bluetooth device or Wi-Fi network connected). isBluetoothConnected is the
    // "a connection alert is showing" flag; connectionAlert says which icon to draw.
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()
    private var bluetoothJob: Job? = null

    enum class ConnectionAlert { BLUETOOTH, WIFI }

    private val _connectionAlert = MutableStateFlow(ConnectionAlert.BLUETOOTH)
    val connectionAlert: StateFlow<ConnectionAlert> = _connectionAlert.asStateFlow()

    // App currently in the foreground (from the accessibility service). Activities that belong to
    // it are hidden, e.g. the music pill disappears while the music app itself is open.
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    fun setForegroundPackage(packageName: String?) {
        // Only hides the app's own cards while it is on screen (see IslandOverlayRoot). The card is
        // NOT dismissed: it comes back after leaving the app as long as the notification is still in
        // the shade. Cards end only when swiped away or when the notification is cleared.
        _foregroundPackage.value = packageName
    }

    // Swipe gestures on the pill: right hides it, left shrinks it into the camera circle.
    // Both reset as soon as something new happens (new notification, song, alert...).
    private val _userHidden = MutableStateFlow(false)
    val userHidden: StateFlow<Boolean> = _userHidden.asStateFlow()

    private val _userMinimized = MutableStateFlow(false)
    val userMinimized: StateFlow<Boolean> = _userMinimized.asStateFlow()

    fun hideIsland() {
        collapse()
        _userMinimized.value = false
        _userHidden.value = true
    }

    fun minimizeIsland() {
        collapse()
        _userHidden.value = false
        _userMinimized.value = true
    }

    fun restoreIsland() {
        _userHidden.value = false
        _userMinimized.value = false
    }

    enum class RingerMode {
        SILENT,
        VIBRATE,
        RING
    }

    private val _activeRingerMode = MutableStateFlow<RingerMode?>(null)
    val activeRingerMode: StateFlow<RingerMode?> = _activeRingerMode.asStateFlow()
    private var ringerJob: Job? = null

    private val _activeMedia = MutableStateFlow<MediaInfo?>(null)
    val activeMedia: StateFlow<MediaInfo?> = _activeMedia.asStateFlow()

    /** I3.1: counters that trigger the icons' one-shot effects (bounce, wiggle, appear). */
    private val _iconSignals = MutableStateFlow(com.droidbubble.app.island.icon.IconSignals())
    val iconSignals: StateFlow<com.droidbubble.app.island.icon.IconSignals> = _iconSignals.asStateFlow()

    /**
     * Every activity the island knows about (A1.3). Since A1.4 the old single slots
     * ([activeNotification], [activeProgress]) are projections of it, so a third live activity is
     * no longer lost behind a one-off alert.
     */
    val store = ActivityStore(now = { IslandClock.wallMs() }, threadCheck = ::checkMainThreadInDebug)

    private fun checkMainThreadInDebug() {
        if (!com.droidbubble.app.BuildConfig.DEBUG) return
        val main = runCatching { android.os.Looper.getMainLooper() }.getOrNull() ?: return
        // A warning, not check(): in shadow mode a wrong thread must not crash the debug build on
        // the phone; A1.4 (when the store becomes the source) can make it fatal
        if (android.os.Looper.myLooper() != main) {
            android.util.Log.w("ActivityStore", "written off the main thread", IllegalStateException(Thread.currentThread().name))
        }
    }

    /** A1.4: the specs behind the two old slots, as last projected. */
    private var legacySlots = LegacySlots.Empty

    /** Re-projects the two slots after a store change; [changed] is the entry just posted. */
    private fun projectSlots(changed: ActivityId? = null): LegacySlots {
        legacySlots = projectLegacySlots(store.activities.value.values, legacySlots, changed)
        return legacySlots
    }

    private val com.droidbubble.app.island.activity.LiveActivitySpec?.info: NotificationInfo?
        get() = (this?.payload as? ActivityPayload.Notification)?.info

    /** Removes every notification-sourced entry of one family (the listener disconnected). */
    private fun clearStore(progress: Boolean) {
        store.activities.value.values
            .filter { it.source == ActivitySource.NOTIFICATION && (it.kind == ActivityKind.PROGRESS) == progress }
            .forEach { store.remove(it.id) }
    }

    private val _activeNotification = MutableStateFlow<NotificationInfo?>(null)
    val activeNotification: StateFlow<NotificationInfo?> = _activeNotification.asStateFlow()

    private val _activeProgress = MutableStateFlow<NotificationInfo?>(null)
    val activeProgress: StateFlow<NotificationInfo?> = _activeProgress.asStateFlow()

    private val _screenRecordingState = MutableStateFlow(ScreenRecordingState())
    val screenRecordingState: StateFlow<ScreenRecordingState> = _screenRecordingState.asStateFlow()

    private val _activeCallState = MutableStateFlow(ActiveCallState())
    val activeCallState: StateFlow<ActiveCallState> = _activeCallState.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    // Fired by the island view once the collapse animation has visually finished
    // True once the cards have fully collapsed (state, not an event, so it can't be missed)
    private val _collapseSettled = MutableStateFlow(true)
    val collapseSettled: StateFlow<Boolean> = _collapseSettled.asStateFlow()

    fun notifyCollapseSettled() {
        _collapseSettled.value = true
        _flashlightSolo.value = false
    }

    // F5.3: the shape the island has come to rest in, or null while any shape animation runs.
    // The window waits for it before it shrinks.
    private val _settledShape = MutableStateFlow<SettledShape?>(null)
    val settledShape: StateFlow<SettledShape?> = _settledShape.asStateFlow()

    fun publishSettledShape(shape: SettledShape?) {
        _settledShape.value = shape
    }

    // F5.4 envelope trial: the card heights the view measured from text, so the window can size
    // an envelope around them. Published only while the trial is on; null until then.
    private val _measuredCardHeights = MutableStateFlow<com.droidbubble.app.island.scene.MeasuredHeights?>(null)
    val measuredCardHeights: StateFlow<com.droidbubble.app.island.scene.MeasuredHeights?> = _measuredCardHeights.asStateFlow()

    fun publishMeasuredCardHeights(heights: com.droidbubble.app.island.scene.MeasuredHeights) {
        _measuredCardHeights.value = heights
    }

    private val _expandedPriority = MutableStateFlow<IslandPriority?>(null)
    val expandedPriority: StateFlow<IslandPriority?> = _expandedPriority.asStateFlow()

    // Tapping the flashlight expands only the flashlight card instead of every active card (F1.5:
    // state here, so the scene can be derived outside Compose). Latched until the collapse has
    // settled, so the card's content doesn't swap to another activity halfway through.
    private val _flashlightSolo = MutableStateFlow(false)
    val flashlightSolo: StateFlow<Boolean> = _flashlightSolo.asStateFlow()

    /** Changes which card the open island shows; the flashlight-solo latch follows while open. */
    private fun setExpandedPriority(priority: IslandPriority?) {
        _expandedPriority.value = priority
        if (_isExpanded.value) _flashlightSolo.value = priority == IslandPriority.FLASHLIGHT
    }

    private val _isPopInActive = MutableStateFlow(false)
    val isPopInActive: StateFlow<Boolean> = _isPopInActive.asStateFlow()

    private val _isKeyboardFocused = MutableStateFlow(false)
    val isKeyboardFocused: StateFlow<Boolean> = _isKeyboardFocused.asStateFlow()

    fun setKeyboardFocused(focused: Boolean) {
        _isKeyboardFocused.value = focused
    }

    private var lastMediaPlayingTime = 0L
    private var lastTrackKey: String? = null

    // Callbacks for media transport controls
    var onPlayPause: (() -> Unit)? = null
    var onPauseMedia: (() -> Unit)? = null
    var onSkipNext: (() -> Unit)? = null
    var onSkipPrevious: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onToggleLike: (() -> Unit)? = null
    var onToggleShuffle: (() -> Unit)? = null
    var onCycleRepeat: (() -> Unit)? = null

    var dismissedMediaPackage: String? = null
        private set

    // Callbacks for screen recording controls
    var onStopRecording: (() -> Unit)? = null
    var onTogglePauseRecording: (() -> Unit)? = null

    // Callbacks for active phone call controls
    var onEndCall: (() -> Unit)? = null
    var onToggleCallMuteOrSpeaker: (() -> Unit)? = null

    var onResetNetworkSession: (() -> Unit)? = null

    // Callback to dismiss notification from listener
    var onDismissNotification: ((String) -> Unit)? = null

    // Removes the notification from the system notification shade too
    var onCancelSystemNotification: ((String) -> Unit)? = null

    /** Swipe-to-clear on the pill: clears the card AND the notification in the shade. */
    fun clearCurrentNotification(): Boolean {
        val notif = _activeNotification.value ?: return false
        // B6.2: a ringing call or alarm is never swiped away, here or in the shade
        if (notif.isRingingOrAlert) return false
        onCancelSystemNotification?.invoke(notif.key)
        return dismissCurrentNotification()
    }

    /**
     * The user opened the notification from the island. Like tapping it in the shade: an
     * auto-cancel notification is cleared (and its card with it); any other one keeps its card
     * until it is swiped away or cleared from the notification panel.
     */
    fun onNotificationOpened() {
        val notif = _activeNotification.value ?: return
        if (notif.isAutoCancel) {
            onCancelSystemNotification?.invoke(notif.key)
            dismissCurrentNotification()
        }
    }

    /**
     * A notification action button (not reply) was pressed. The card stays: if the action really
     * finishes the notification (e.g. "Mark as read") the app removes it and the card goes too.
     */
    fun onNotificationActionPressed() {
        autoDismissNotificationJob?.cancel()
    }

    /** True while a phone / VoIP call is ringing (shown or waiting behind another card). */
    val isIncomingCallRinging: Boolean
        get() = _activeNotification.value?.isIncomingCall == true ||
            store.activities.value.values.any { it.kind == ActivityKind.CALL_RINGING }

    // Liquid glass: on-screen rects of the expanded cards, published every frame while they
    // morph, which the overlay service backs with blur windows. Empty once collapsed.
    // strength 0..1 scales the blur radius, so the blur fades in and out with the card
    data class GlassPane(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int,
        val cornerRadiusPx: Float,
        val strength: Float = 1f
    )

    private val _glassPanes = MutableStateFlow<List<GlassPane>>(emptyList())
    val glassPanes: StateFlow<List<GlassPane>> = _glassPanes.asStateFlow()

    /**
     * Called straight from [setGlassPanes], inside the island's own frame, so the blur windows get
     * the new card rects without waiting for a hop through the main thread's message queue.
     */
    @Volatile
    var glassPaneListener: ((List<GlassPane>) -> Unit)? = null

    fun setGlassPanes(panes: List<GlassPane>) {
        if (_glassPanes.value != panes) {
            _glassPanes.value = panes
            glassPaneListener?.invoke(panes)
        }
    }

    // Whether the system can blur behind windows right now (off on some devices / battery saver).
    // Without it the glass cards use a more opaque gradient so text stays readable.
    private val _glassBlurAvailable = MutableStateFlow(false)
    val glassBlurAvailable: StateFlow<Boolean> = _glassBlurAvailable.asStateFlow()

    fun setGlassBlurAvailable(available: Boolean) {
        _glassBlurAvailable.value = available
    }

    // Callback to send direct reply to notification
    var onSendReply: ((key: String, replyText: String) -> Unit)? = null

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _flashlightLevel = MutableStateFlow(1)
    val flashlightLevel: StateFlow<Int> = _flashlightLevel.asStateFlow()

    private val _maxFlashlightLevel = MutableStateFlow(1)
    val maxFlashlightLevel: StateFlow<Int> = _maxFlashlightLevel.asStateFlow()

    // Callbacks for flashlight controls
    var onSetFlashlightLevel: ((Int) -> Unit)? = null
    var onTurnOffFlashlight: (() -> Unit)? = null

    val isRecordingActive: Boolean
        get() = _screenRecordingState.value.isRecording

    val isCallActive: Boolean
        get() = _activeCallState.value.isActive

    val isNotificationActive: Boolean
        get() = _activeNotification.value != null

    /** Island timeouts (F3.5). The service swaps in the accessibility-scaled ones when it starts. */
    var timings: IslandTimings = IslandTimings.Unscaled

    var dismissMediaOnPause: Boolean = false
    var autoDismissNotifications: Boolean = false
    private var autoDismissNotificationJob: Job? = null

    /**
     * D2.2: new message banners peek open into their card, then recede. Set by the overlay
     * service; off here so tests and previews that post messages see the compact island.
     */
    @Volatile
    var peekBanners: Boolean = false

    /** D2.2: TalkBack (touch exploration) is on. Set by the overlay service. */
    @Volatile
    var touchExplorationOn: Boolean = false

    /** D2.2: the open card is a peek: it recedes after [IslandTimings.peek] unless touched. */
    private val _isPeeking = MutableStateFlow(false)
    val isPeeking: StateFlow<Boolean> = _isPeeking.asStateFlow()
    private val lastPeekByPackage = HashMap<String, Long>()

    /** D2.3: "same app, new event", throttled per app. */
    private val pulseDetector = PulseDetector()

    val isMediaActive: Boolean
        get() {
            val media = _activeMedia.value ?: return false
            if (dismissMediaOnPause && !media.isPlaying) return false
            // Keep active whenever a media session exists with a title so the island remains visible & expandable when paused!
            return media.title.isNotBlank()
        }

    /**
     * The state the scene is derived from, read now (F1.4). Main thread only.
     */
    fun snapshotInputs(flashlightSolo: Boolean = _flashlightSolo.value) = com.droidbubble.app.island.scene.IslandInputs(
        isExpanded = _isExpanded.value,
        expandedPriority = _expandedPriority.value,
        isPopIn = _isPopInActive.value,
        media = _activeMedia.value,
        notification = _activeNotification.value,
        progress = _activeProgress.value,
        recording = _screenRecordingState.value,
        charging = _activeCharging.value,
        isUnlocked = _isUnlocked.value,
        isBluetoothConnected = _isBluetoothConnected.value,
        connectionAlert = _connectionAlert.value,
        ringerMode = _activeRingerMode.value,
        isFlashlightOn = _isFlashlightOn.value,
        isCallActive = _activeCallState.value.isActive,
        foregroundPackage = _foregroundPackage.value,
        userHidden = _userHidden.value,
        userMinimized = _userMinimized.value,
        isDeviceLocked = _isDeviceLocked.value,
        flashlightSolo = flashlightSolo,
    )

    val isProgressActive: Boolean
        get() = _activeProgress.value != null

    val isChargingActive: Boolean
        get() = _activeCharging.value != null

    val isUnlockActive: Boolean
        get() = _isUnlocked.value

    val isBluetoothActive: Boolean
        get() = _isBluetoothConnected.value

    val isFlashlightActive: Boolean
        get() = _isFlashlightOn.value

    val isRingerModeActive: Boolean
        get() = _activeRingerMode.value != null

    val isOccupied: Boolean
        get() = isCallActive || isRecordingActive || isNotificationActive || isMediaActive || isProgressActive || isChargingActive || isUnlockActive || isBluetoothActive || isFlashlightActive || isRingerModeActive

    // Live priority determination: Call is top priority, then Media, Flashlight, Progress, Screen Recording, and Notification alerts last
    val currentPriority: IslandPriority
        get() {
            return when {
                isCallActive -> IslandPriority.CALL
                isMediaActive -> IslandPriority.MEDIA
                isFlashlightActive -> IslandPriority.FLASHLIGHT
                isProgressActive -> IslandPriority.PROGRESS
                _screenRecordingState.value.isRecording -> IslandPriority.SCREEN_RECORDING
                _activeNotification.value != null -> IslandPriority.POP_NOTIFICATION
                else -> IslandPriority.IDLE
            }
        }

    fun updateFlashlightState(enabled: Boolean, level: Int = _flashlightLevel.value, maxLevel: Int = 4) {
        if (enabled && !_isFlashlightOn.value) restoreIsland()
        _isFlashlightOn.value = enabled
        _flashlightLevel.value = level.coerceIn(1, 4)
        _maxFlashlightLevel.value = 4
        if (enabled) {
            store.upsert(LegacySpecMapper.fromTorch(_flashlightLevel.value, 4, IslandClock.wallMs(), store.get(ActivityId.TORCH)))
        } else {
            store.remove(ActivityId.TORCH)
        }
        if (!enabled && _isExpanded.value && (_expandedPriority.value == IslandPriority.FLASHLIGHT || !isOccupied)) {
            collapse()
        }
    }

    // Torch state as reported by the camera service, before flicker filtering
    private var torchRawOn = false
    private var torchRawLevel = 1
    private val torchToggleTimes = ArrayDeque<Long>()
    private var torchConfirmJob: Job? = null

    /** Monotonic clock for the torch flicker filter (replaceable in tests). */
    var nowMs: () -> Long = { System.nanoTime() / 1_000_000L }

    /**
     * Torch callback from the camera service. "Flash on call / notification" features blink the
     * torch several times a second; showing the flashlight pill for each blink made it flash in and
     * out over the call card. The pill only appears once the torch has stayed on for a moment, waits
     * longer while it is blinking, and never appears over a ringing call.
     */
    fun onTorchChanged(enabled: Boolean, level: Int = torchRawLevel) {
        val now = nowMs()
        if (enabled != torchRawOn) torchToggleTimes.addLast(now)
        while (torchToggleTimes.isNotEmpty() && now - torchToggleTimes.first() > TORCH_FLICKER_WINDOW_MS) {
            torchToggleTimes.removeFirst()
        }
        torchRawOn = enabled
        torchRawLevel = level
        torchConfirmJob?.cancel()
        if (!enabled) {
            if (_isFlashlightOn.value) updateFlashlightState(false)
            return
        }
        if (_isFlashlightOn.value) {
            updateFlashlightLevel(level)
            return
        }
        scheduleTorchConfirm()
    }

    private fun scheduleTorchConfirm() {
        torchConfirmJob?.cancel()
        if (!torchRawOn || _isFlashlightOn.value) return
        val waitMs = torchShowDelayMs(torchToggleTimes.size)
        torchConfirmJob = scope.launch {
            delay(waitMs)
            if (torchRawOn && !isIncomingCallRinging) updateFlashlightState(true, torchRawLevel)
        }
    }

    fun updateFlashlightLevel(level: Int) {
        _flashlightLevel.value = level.coerceIn(1, 4)
    }

    fun setFlashlightBrightness(level: Int) {
        val clamped = level.coerceIn(1, 4)
        _flashlightLevel.value = clamped
        onSetFlashlightLevel?.invoke(clamped)
    }

    fun turnOffFlashlight() {
        _isFlashlightOn.value = false
        onTurnOffFlashlight?.invoke()
        if (_isExpanded.value && (_expandedPriority.value == IslandPriority.FLASHLIGHT || !isOccupied)) {
            collapse()
        }
    }

    fun triggerUnlockEvent() {
        // Never fold a ringing call card away for a passing system alert
        if (isIncomingCallRinging) return
        if (_isExpanded.value) collapse()
        unlockJob?.cancel()
        _isUnlocked.value = true
        unlockJob = scope.launch {
            delay(timings.unlock)
            _isUnlocked.value = false
            android.util.Log.d("IslandStateManager", "triggerUnlockEvent() finished: isUnlocked reset to false")
        }
    }

    fun triggerBluetoothEvent() = triggerConnectionAlert(ConnectionAlert.BLUETOOTH)

    fun triggerWifiEvent() = triggerConnectionAlert(ConnectionAlert.WIFI)

    private fun triggerConnectionAlert(kind: ConnectionAlert) {
        if (isIncomingCallRinging) return
        if (_isExpanded.value) collapse()
        restoreIsland()
        bluetoothJob?.cancel()
        _connectionAlert.value = kind
        _isBluetoothConnected.value = true
        _iconSignals.update { it.copy(connectionAppear = it.connectionAppear + 1) }
        bluetoothJob = scope.launch {
            delay(timings.connection)
            _isBluetoothConnected.value = false
        }
    }

    fun triggerRingerModeEvent(mode: RingerMode) {
        // Some phones switch the ringer / audio mode as a call starts ringing
        if (isIncomingCallRinging) return
        if (_isExpanded.value) collapse()
        ringerJob?.cancel()
        _activeRingerMode.value = mode
        ringerJob = scope.launch {
            delay(timings.ringer)
            _activeRingerMode.value = null
        }
    }

    fun triggerChargingEvent(percentage: Int, wattage: Double? = null) {
        if (isIncomingCallRinging) return
        if (_isExpanded.value) collapse()
        restoreIsland()
        chargingJob?.cancel()
        _activeCharging.value = ChargingInfo(percentage.coerceIn(0, 100), wattage)
        _iconSignals.update { it.copy(chargeStart = it.chargeStart + 1) }
        chargingJob = scope.launch {
            delay(timings.charging)
            _activeCharging.value = null
        }
    }

    fun updateChargingWattage(wattage: Double?) {
        val current = _activeCharging.value ?: return
        _activeCharging.value = current.copy(wattage = wattage)
    }

    fun updateMedia(media: MediaInfo?) {
        // A1.3: the store keeps its own dismissal (signature = playing), so a swiped-away paused
        // session comes back only when it plays again
        when {
            media == null -> store.activities.value.values.filter { it.kind == ActivityKind.MEDIA }.forEach { store.remove(it.id) }
            dismissMediaOnPause && !media.isPlaying -> store.remove(ActivityId.media(media.packageName))
            else -> {
                val id = ActivityId.media(media.packageName)
                store.upsert(LegacySpecMapper.fromMedia(media, IslandClock.wallMs(), store.get(id)))
            }
        }
        if (dismissMediaOnPause && media != null && !media.isPlaying) {
            _activeMedia.value = null
            if (_isExpanded.value && (_expandedPriority.value == IslandPriority.MEDIA || !isOccupied)) {
                collapse()
            }
            return
        }
        if (media != null && media.packageName == dismissedMediaPackage) {
            if (media.isPlaying) {
                // Playback resumed, un-dismiss!
                dismissedMediaPackage = null
            } else {
                // Still dismissed by user swipe
                _activeMedia.value = null
                if (_isExpanded.value && (_expandedPriority.value == IslandPriority.MEDIA || !isOccupied)) {
                    collapse()
                }
                return
            }
        }
        _activeMedia.value = media
        if (media == null && _isExpanded.value && (_expandedPriority.value == IslandPriority.MEDIA || !isOccupied)) {
            collapse()
        }

        // New song: bring back a pill the user swiped away (no auto-expand)
        val trackKey = media?.let { "${it.packageName}|${it.title}|${it.artist}" }
        if (trackKey != null && lastTrackKey != null && trackKey != lastTrackKey && media.isPlaying) {
            restoreIsland()
        }
        if (trackKey != null) lastTrackKey = trackKey
        if (media?.isPlaying == true) {
            lastMediaPlayingTime = System.currentTimeMillis()
        }
    }

    fun dismissMedia() {
        val current = _activeMedia.value
        current?.let { store.dismiss(ActivityId.media(it.packageName)) }
        dismissedMediaPackage = current?.packageName
        onPauseMedia?.invoke()
        _activeMedia.value = null
        if (_isExpanded.value) {
            if (!isOccupied) collapse() else if (_expandedPriority.value == IslandPriority.MEDIA) setExpandedPriority(null)
        }
    }

    fun unDismissMedia(packageName: String? = null) {
        if (packageName == null || dismissedMediaPackage == packageName) {
            dismissedMediaPackage?.let { store.undismiss(ActivityId.media(it)) }
            dismissedMediaPackage = null
        }
    }

    // A1.4: cards the user dismissed are remembered by the store (id -> stateSignature, B4.5):
    // a re-post that only ticks a number doesn't bring the card back, a real state change does.
    // A download stays dismissed until the app removes it.

    private val NotificationInfo.isRingingOrAlert: Boolean get() = isIncomingCall || isAlert

    /** B11.3: true for a notification safe mode has paused. Set by the island service. */
    @Volatile
    var safeModeFilter: ((NotificationInfo) -> Boolean)? = null

    fun updateNotification(notification: NotificationInfo?) {
        CrashLog.breadcrumb(
            "state",
            if (notification == null) "notification cleared"
            // B11.3: "primary" names what the island shows, for the crash-loop guard
            else "primary kind=${notification.kind} pkg=${notification.packageName} keyHash=${CrashLog.keyHash(notification.key)}"
        )
        // B11.3: a kind paused by safe mode is refused here too (the listener drops it first)
        if (notification != null && safeModeFilter?.invoke(notification) == true) return
        val wasRinging = isIncomingCallRinging
        // Plain downloads go to the progress card; rides, deliveries and Android 16 live updates
        // keep their own live-activity design even though they carry a progress bar
        if (notification != null && notification.hasProgress && notification.kind == LiveActivityKind.NONE) {
            updateProgress(notification)
            return
        }
        // A1.4: the store takes every update, including ones the slot keeps out (a message while
        // a call rings); a dismissed card's ticks are refused there. The projection then picks the
        // slot: a ringing call or alarm isn't covered, a live re-post waits behind a one-off card,
        // and whatever waits stays in the store until the card in front goes.
        if (notification == null) {
            clearStore(progress = false)
            legacySlots = legacySlots.copy(notification = null)
        } else {
            val id = ActivityId.notification(notification.key)
            if (!store.upsert(LegacySpecMapper.fromNotification(notification, store.get(id)))) return
            if (projectSlots(id).notification?.id != id) return
        }
        autoDismissNotificationJob?.cancel()
        val previous = _activeNotification.value
        val isNewNotification = notification != null && previous?.key != notification.key
        // D2.3: the same app again with something new: one beat per 600 ms per app, which the
        // icon (I3), the haptic tick and the pill's 3 % pulse all follow
        if (pulseDetector.onUpdate(previous, notification, nowMs()) != null) {
            _iconSignals.update { it.copy(sameAppNudge = it.sameAppNudge + 1) }
        }
        _activeNotification.value = notification
        onRingingChanged(wasRinging)
        // Incoming call: open the call card (answer / decline) right away
        if (notification != null && notification.isIncomingCall && (isNewNotification || previous?.isIncomingCall != true)) {
            _iconSignals.update { it.copy(ringStart = it.ringStart + 1) }
            restoreIsland()
            expand(IslandPriority.POP_NOTIFICATION)
        }
        // Ringing alarm / expired timer: open the full island with its Stop / Snooze buttons
        if (notification != null && notification.isAlert && (isNewNotification || previous?.isAlert != true)) {
            _iconSignals.update { it.copy(alertStart = it.alertStart + 1) }
            restoreIsland()
            expand(IslandPriority.POP_NOTIFICATION)
        }
        if (notification == null) {
            _isPopInActive.value = false
            if (_isExpanded.value && (_expandedPriority.value == IslandPriority.POP_NOTIFICATION || !isOccupied)) {
                if (!isOccupied) {
                    collapse()
                } else {
                    setExpandedPriority(null)
                }
            }
        } else if (isNewNotification) {
            restoreIsland()
            triggerPopIn()
            scheduleAutoDismiss(notification)
            // D2.2: a new message peeks open into its card, then recedes (see the auto-collapse
            // loop in the shell); a touch keeps it open
            val now = nowMs()
            if (peekBanners && PeekPolicy.shouldPeek(
                    notification, isNew = true, nowMs = now, lastPeekMs = lastPeekByPackage[notification.packageName],
                    locked = _isDeviceLocked.value, touchExploration = touchExplorationOn, expanded = _isExpanded.value,
                )
            ) {
                lastPeekByPackage[notification.packageName] = now
                expand(IslandPriority.POP_NOTIFICATION)
                _isPeeking.value = true
            }
        }
    }

    /** "Auto-dismiss notifications": a message leaves after a while unless its card is open. */
    private fun scheduleAutoDismiss(notification: NotificationInfo) {
        // Live activities (calls, timers, recorders) stay until the app removes them
        if (!autoDismissNotifications || notification.isLiveActivity) return
        val notifKey = notification.key
        autoDismissNotificationJob?.cancel()
        autoDismissNotificationJob = scope.launch {
            delay(timings.notificationAuto)
            if (_activeNotification.value?.key == notifKey && (!_isExpanded.value || _expandedPriority.value != IslandPriority.POP_NOTIFICATION)) {
                dismissCurrentNotification()
            }
        }
    }

    /** D2.2: a touch on a peeking card: it stays open like any card the user opened. */
    fun cancelPeek() {
        _isPeeking.value = false
    }

    fun updateProgress(progress: NotificationInfo?) {
        if (progress == null) {
            clearStore(progress = true)
            legacySlots = legacySlots.copy(progress = null)
        } else {
            // A progress card the user dismissed stays gone while the download keeps ticking
            val id = ActivityId.notification(progress.key)
            if (!store.upsert(LegacySpecMapper.fromProgress(progress, store.get(id)))) return
            projectSlots(id)
        }
        if (progress != null && _activeProgress.value?.key != progress.key) restoreIsland()
        _activeProgress.value = progress
        if (progress == null) {
            if (_isExpanded.value && (_expandedPriority.value == IslandPriority.PROGRESS || !isOccupied)) {
                if (!isOccupied) {
                    collapse()
                } else {
                    setExpandedPriority(null)
                }
            }
        }
    }

    fun removeNotification(key: String) {
        val wasRinging = isIncomingCallRinging
        // Also forgets a dismissal: the next post of this key is a new card
        store.remove(ActivityId.notification(key))
        var removed = false
        if (_activeNotification.value?.key == key) {
            autoDismissNotificationJob?.cancel()
            // The newest live activity left in the store takes the card (none is lost)
            _activeNotification.value = projectSlots().notification.info
            _isPopInActive.value = false
            onRingingChanged(wasRinging)
            removed = true
        }
        if (_activeProgress.value?.key == key) {
            // Another download still running takes its place
            _activeProgress.value = projectSlots().progress.info
            removed = true
        }
        if (removed && _isExpanded.value && !isOccupied) {
            collapse()
        }
    }

    /** The ringing just stopped: a torch the user really turned on can show its pill now. */
    private fun onRingingChanged(wasRinging: Boolean) {
        if (wasRinging && !isIncomingCallRinging) scheduleTorchConfirm()
    }

    fun triggerPopIn() {
        _isPopInActive.value = true
        popInJob?.cancel()
        popInJob = scope.launch {
            delay(timings.popIn) // Bubble pop-in for 4.5 seconds then auto-shrink to compact
            _isPopInActive.value = false
        }
    }

    /**
     * Hides the current card without cancelling the system notification. While a call is ringing or
     * an alarm is going off it refuses (B6.2), unless [endingRingingOrAlert]: only the card's own
     * Decline and Stop buttons pass that, because they end the call or alarm.
     */
    fun dismissCurrentNotification(endingRingingOrAlert: Boolean = false): Boolean {
        val notif = _activeNotification.value ?: return false
        if (notif.isRingingOrAlert && !endingRingingOrAlert) return false
        autoDismissNotificationJob?.cancel()
        // Dismiss card only, without cancelling actual system notification
        val wasRinging = isIncomingCallRinging
        store.dismiss(ActivityId.notification(notif.key))
        // A live activity that was waiting behind this card takes its place
        val restored = projectSlots().notification.info
        _activeNotification.value = restored
        _isPopInActive.value = false
        onRingingChanged(wasRinging)
        if (restored != null) return true
        if (_isExpanded.value && (_expandedPriority.value == IslandPriority.POP_NOTIFICATION || !isOccupied)) {
            if (!isOccupied) {
                collapse()
            } else {
                setExpandedPriority(null)
            }
        }
        return true
    }

    fun dismissProgress(): Boolean {
        val prog = _activeProgress.value ?: return false
        // Dismiss card only, without cancelling actual system notification
        store.dismiss(ActivityId.notification(prog.key))
        _activeProgress.value = projectSlots().progress.info
        if (_isExpanded.value && (_expandedPriority.value == IslandPriority.PROGRESS || !isOccupied)) {
            if (!isOccupied) {
                collapse()
            } else {
                setExpandedPriority(null)
            }
        }
        return true
    }

    fun updateRecordingState(state: ScreenRecordingState) {
        if (state.isRecording) {
            store.upsert(LegacySpecMapper.fromRecording(state, IslandClock.wallMs(), store.get(ActivityId.SCREEN_RECORDING)))
        } else {
            store.remove(ActivityId.SCREEN_RECORDING)
        }
        val wasRecording = _screenRecordingState.value.isRecording
        if (state.isRecording && !wasRecording) restoreIsland()
        _screenRecordingState.value = state
        if (wasRecording && !state.isRecording && _isExpanded.value) {
            if (!isOccupied) collapse() else if (_expandedPriority.value == IslandPriority.SCREEN_RECORDING) setExpandedPriority(null)
        }
    }

    fun updateCallState(state: ActiveCallState) {
        val wasActive = _activeCallState.value.isActive
        _activeCallState.value = state
        if (wasActive && !state.isActive && _isExpanded.value && (_expandedPriority.value == IslandPriority.CALL || !isOccupied)) {
            collapse()
        }
    }

    fun expand(priority: IslandPriority? = null) {
        CrashLog.breadcrumb("state", "expand priority=${priority?.name ?: "current"}")
        _isPeeking.value = false
        val resolved = priority ?: currentPriority
        if (resolved == IslandPriority.POP_NOTIFICATION) {
            autoDismissNotificationJob?.cancel()
        }
        _expandedPriority.value = priority
        _collapseSettled.value = false
        _isExpanded.value = true
        _flashlightSolo.value = priority == IslandPriority.FLASHLIGHT
    }

    fun collapse() {
        CrashLog.breadcrumb("state", "collapse")
        val receded = _isPeeking.value
        _isPeeking.value = false
        _isExpanded.value = false
        _expandedPriority.value = null
        _isKeyboardFocused.value = false
        // D2.2: a message that peeked and receded leaves on the usual auto-dismiss timer
        if (receded) _activeNotification.value?.let { scheduleAutoDismiss(it) }
    }

    /** Puts the island back to a clean idle state. Tests only: the object is a process singleton. */
    @androidx.annotation.VisibleForTesting
    fun resetForTest() {
        listOf(popInJob, chargingJob, unlockJob, bluetoothJob, ringerJob, autoDismissNotificationJob, torchConfirmJob)
            .forEach { it?.cancel() }
        _activeCharging.value = null
        _cameraGeometry.value = com.droidbubble.app.island.layout.CameraGeometry.None
        _iconSignals.value = com.droidbubble.app.island.icon.IconSignals()
        _isUnlocked.value = false
        _isBluetoothConnected.value = false
        _activeRingerMode.value = null
        _activeMedia.value = null
        _activeNotification.value = null
        _activeProgress.value = null
        store.clear()
        _screenRecordingState.value = ScreenRecordingState()
        _activeCallState.value = ActiveCallState()
        _isExpanded.value = false
        _expandedPriority.value = null
        _collapseSettled.value = true
        _settledShape.value = null
        _measuredCardHeights.value = null
        _flashlightSolo.value = false
        _isPopInActive.value = false
        _isKeyboardFocused.value = false
        _isFlashlightOn.value = false
        _foregroundPackage.value = null
        _userHidden.value = false
        _userMinimized.value = false
        torchRawOn = false
        torchToggleTimes.clear()
        legacySlots = LegacySlots.Empty
        CountdownTotals.resetForTest()
        safeModeFilter = null
        dismissedMediaPackage = null
        lastTrackKey = null
        dismissMediaOnPause = false
        autoDismissNotifications = false
        timings = IslandTimings.Unscaled
        peekBanners = false
        touchExplorationOn = false
        _isPeeking.value = false
        lastPeekByPackage.clear()
        pulseDetector.reset()
    }
}
