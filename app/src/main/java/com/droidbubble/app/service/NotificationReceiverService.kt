package com.droidbubble.app.service

import com.droidbubble.app.model.gateKind
import com.droidbubble.app.model.allows
import com.droidbubble.app.util.CrashLog
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.droidbubble.app.util.traced
import android.util.Log
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.ActiveCallState
import com.droidbubble.app.island.isClockOrRecorderPackage
import com.droidbubble.app.island.isClockPackage
import com.droidbubble.app.island.isRecorderPackage
import com.droidbubble.app.island.isSportsPackage
import com.droidbubble.app.island.isTrackingPackage
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.MediaInfo
import com.droidbubble.app.model.NotificationAction
import com.droidbubble.app.model.NotificationInfo
import com.droidbubble.app.model.ScreenRecordingState
import com.droidbubble.app.model.parseTimerTextMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Paused media older than this no longer occupies the island
private const val STALE_PAUSED_MEDIA_MS = 10 * 60 * 1000L
private const val COMPAT_SET_SHUFFLE = "android.support.v4.media.session.action.SET_SHUFFLE_MODE"
private const val COMPAT_ARG_SHUFFLE = "android.support.v4.media.session.action.ARGUMENT_SHUFFLE_MODE"
private const val COMPAT_SET_REPEAT = "android.support.v4.media.session.action.SET_REPEAT_MODE"
private const val COMPAT_ARG_REPEAT = "android.support.v4.media.session.action.ARGUMENT_REPEAT_MODE"

// Notification.FLAG_PROMOTED_ONGOING and Notification.EXTRA_SHORT_CRITICAL_TEXT (Android 16)
private const val FLAG_PROMOTED_ONGOING = 0x00040000
private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

class NotificationReceiverService : NotificationListenerService() {

    companion object {
        /** B10.2: media refresh bursts are collected for this long. */
        const val MEDIA_REFRESH_DEBOUNCE_MS = 100L
        var instance: NotificationReceiverService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var currentSettings = IslandSettings()

    /** Test seam (S5.3): settings without DataStore. */
    @androidx.annotation.VisibleForTesting
    internal fun setSettingsForTest(settings: IslandSettings) {
        currentSettings = settings
    }

    /**
     * Test seam (S5.3): importance per notification key, used instead of the framework ranking
     * (which only exists while the listener is bound). Null in production.
     */
    @androidx.annotation.VisibleForTesting
    internal var importanceForTest: ((String) -> Int?)? = null

    private fun isLowImportance(key: String): Boolean {
        importanceForTest?.let { lookup ->
            val importance = lookup(key) ?: return false
            return importance < android.app.NotificationManager.IMPORTANCE_DEFAULT
        }
        val ranking = Ranking()
        return currentRanking?.getRanking(key, ranking) == true &&
            ranking.importance < android.app.NotificationManager.IMPORTANCE_DEFAULT
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val userLikedTracks = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        refreshMedia()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest {
                currentSettings = it
                IslandStateManager.dismissMediaOnPause = it.dismissMediaOnPause
                refreshMedia()
            }
        }

        IslandStateManager.onPauseMedia = {
            activeMediaController?.transportControls?.pause()
        }

        IslandStateManager.onPlayPause = {
            activeMediaController?.let { controller ->
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
        }

        IslandStateManager.onSkipNext = {
            activeMediaController?.transportControls?.skipToNext()
        }

        IslandStateManager.onSkipPrevious = {
            activeMediaController?.transportControls?.skipToPrevious()
        }

        // Shuffle / repeat: prefer the app's own custom action (Spotify, YouTube Music...), else the
        // MediaSessionCompat actions that compat and Media3 sessions understand. The platform API
        // can't read these modes back, so the island remembers what it last set per app.
        IslandStateManager.onToggleShuffle = {
            activeMediaController?.let { controller ->
                val current = IslandStateManager.activeMedia.value
                val newOn = !(current?.isShuffleOn ?: false)
                runCatching {
                    val custom = findCustomAction(controller, "shuffle")
                    if (custom != null) {
                        controller.transportControls.sendCustomAction(custom.action, custom.extras)
                    } else {
                        controller.transportControls.sendCustomAction(
                            COMPAT_SET_SHUFFLE,
                            Bundle().apply { putInt(COMPAT_ARG_SHUFFLE, if (newOn) 1 else 0) }
                        )
                    }
                }
                shuffleOverrides[controller.packageName] = newOn
                if (current != null) IslandStateManager.updateMedia(current.copy(isShuffleOn = newOn))
            }
        }

        IslandStateManager.onCycleRepeat = {
            activeMediaController?.let { controller ->
                val current = IslandStateManager.activeMedia.value
                val next = when (current?.repeatMode ?: MediaInfo.REPEAT_OFF) {
                    MediaInfo.REPEAT_OFF -> MediaInfo.REPEAT_ALL
                    MediaInfo.REPEAT_ALL -> MediaInfo.REPEAT_ONE
                    else -> MediaInfo.REPEAT_OFF
                }
                runCatching {
                    val custom = findCustomAction(controller, "repeat")
                    if (custom != null) {
                        controller.transportControls.sendCustomAction(custom.action, custom.extras)
                    } else {
                        // Compat repeat modes: NONE = 0, ONE = 1, ALL = 2
                        val compatMode = when (next) {
                            MediaInfo.REPEAT_ALL -> 2
                            MediaInfo.REPEAT_ONE -> 1
                            else -> 0
                        }
                        controller.transportControls.sendCustomAction(
                            COMPAT_SET_REPEAT,
                            Bundle().apply { putInt(COMPAT_ARG_REPEAT, compatMode) }
                        )
                    }
                }
                repeatOverrides[controller.packageName] = next
                if (current != null) IslandStateManager.updateMedia(current.copy(repeatMode = next))
            }
        }

        IslandStateManager.onToggleLike = {
            val controller = activeMediaController
            val notif = activeNotifications?.firstOrNull { it.packageName == controller?.packageName }
            var handled = false

            // 1. Try CustomActions on playbackState
            val customActions = controller?.playbackState?.customActions
            val likeAction = customActions?.firstOrNull { ca ->
                val a = ca.action.lowercase()
                val n = ca.name?.toString()?.lowercase() ?: ""
                a.contains("like") || a.contains("favorite") || a.contains("thumb") ||
                n.contains("like") || n.contains("favorite") || n.contains("thumb")
            }
            if (likeAction != null && controller != null) {
                try {
                    controller.transportControls.sendCustomAction(likeAction.action, likeAction.extras)
                    handled = true
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending custom action like", e)
                }
            }

            // 2. Try Notification Actions
            if (!handled && notif?.notification?.actions != null) {
                val notifLikeAction = notif.notification.actions.firstOrNull { action ->
                    val title = action.title?.toString()?.lowercase() ?: ""
                    title.contains("like") || title.contains("favorite") || title.contains("thumb")
                }
                if (notifLikeAction?.actionIntent != null) {
                    try {
                        notifLikeAction.actionIntent.send()
                        handled = true
                    } catch (e: Exception) {
                        Log.e("NotificationReceiver", "Error sending notif like action", e)
                    }
                }
            }

            // 3. Try TransportControls.setRating
            if (!handled && controller != null) {
                val currentMedia = IslandStateManager.activeMedia.value
                val newLiked = !(currentMedia?.isLiked ?: false)
                try {
                    val rating = android.media.Rating.newHeartRating(newLiked)
                    controller.transportControls.setRating(rating)
                    handled = true
                } catch (e: Exception) {
                    try {
                        val thumb = android.media.Rating.newThumbRating(newLiked)
                        controller.transportControls.setRating(thumb)
                        handled = true
                    } catch (e2: Exception) {
                        Log.e("NotificationReceiver", "Error setting rating", e2)
                    }
                }
            }

            val current = IslandStateManager.activeMedia.value
            if (current != null) {
                val newLiked = !current.isLiked
                val trackKey = "${current.packageName}::${current.title}"
                userLikedTracks[trackKey] = newLiked
                IslandStateManager.updateMedia(current.copy(isLiked = newLiked))
            }
        }

        IslandStateManager.onSeekTo = { pos ->
            try {
                activeMediaController?.transportControls?.seekTo(pos)
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error seeking", e)
            }
            val current = IslandStateManager.activeMedia.value
            if (current != null) {
                IslandStateManager.updateMedia(
                    current.copy(
                        positionMs = pos,
                        lastPositionUpdateTimeMs = android.os.SystemClock.elapsedRealtime()
                    )
                )
            }
        }

        IslandStateManager.onDismissNotification = { key ->
            // Intentionally no-op: dismissing only dismisses the island card, not the system notification itself
        }

        IslandStateManager.onCancelSystemNotification = { key ->
            runCatching { cancelNotification(key) }
                .onFailure { Log.e("NotificationReceiver", "Error cancelling notification", it) }
        }

        IslandStateManager.onSendReply = { key, replyText ->
            val notif = IslandStateManager.activeNotification.value
            if (notif != null && notif.key == key) {
                val replyAction = notif.actions.firstOrNull { it.isReply }
                if (replyAction?.actionIntent != null && replyAction.remoteInputKey != null) {
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(replyAction.remoteInputKey, replyText)
                    val remoteInput = android.app.RemoteInput.Builder(replyAction.remoteInputKey).build()
                    android.app.RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                    try {
                        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ActivityOptions.makeBasic().apply {
                                pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            }.toBundle()
                        } else null
                        if (options != null) {
                            replyAction.actionIntent.send(this@NotificationReceiverService, 0, intent, null, null, null, options)
                        } else {
                            replyAction.actionIntent.send(this@NotificationReceiverService, 0, intent)
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationReceiver", "Error sending reply", e)
                    }
                }
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, NotificationReceiverService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
            refreshMedia()
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error registering media session listener", e)
        }
        // Calls, timers and downloads that were already running when the listener (re)connected
        runCatching {
            activeNotifications
                ?.filter { sbn ->
                    val flags = sbn.notification?.flags ?: 0
                    (flags and Notification.FLAG_ONGOING_EVENT) != 0 || (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
                }
                ?.sortedBy { it.postTime }
                ?.forEach { onNotificationPosted(it) }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            for ((ctrl, cb) in controllerCallbacks) {
                runCatching { ctrl.unregisterCallback(cb) }
            }
            controllerCallbacks.clear()
            activeMediaController = null
            // Removals aren't delivered while disconnected; don't leave stale cards behind
            IslandStateManager.updateNotification(null)
            IslandStateManager.updateProgress(null)
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error unregistering listener", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        for ((ctrl, cb) in controllerCallbacks) {
            runCatching { ctrl.unregisterCallback(cb) }
        }
        controllerCallbacks.clear()
        activeMediaController = null
        recordingTimerJob?.cancel()
        callTimerJob?.cancel()
        serviceScope.cancel()
    }

    /**
     * B10.2: media callbacks, window events and posts arrive in bursts. They are collected into one
     * run 100 ms after the first (the newest candidate controller wins). A call made while a run is
     * pending is folded into it, and a call after the run schedules a new one, so the last event is
     * always followed by a run.
     */
    fun refreshMedia(candidateController: MediaController? = null) {
        if (candidateController != null) pendingCandidate = candidateController
        if (refreshScheduled) return
        refreshScheduled = true
        mainHandler.postDelayed(runRefresh, MEDIA_REFRESH_DEBOUNCE_MS)
    }

    private val mainHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }
    private var refreshScheduled = false
    private var pendingCandidate: MediaController? = null
    private var lastMediaSignature: List<Any?>? = null

    @androidx.annotation.VisibleForTesting
    internal var mediaRefreshRunsForTest = 0

    private val runRefresh = Runnable {
        refreshScheduled = false
        val candidate = pendingCandidate
        pendingCandidate = null
        mediaRefreshRunsForTest++
        traced("db.refreshMedia") { guarded("refreshMedia") { refreshMediaTraced(candidate) } }
    }

    /**
     * What a refresh depends on. Equal to the last applied one, with no new candidate, means the
     * refresh would publish the same thing. A 30 s bucket lets a stale paused session still expire.
     */
    private fun mediaSignature(controllers: List<MediaController>): List<Any?> = listOf(
        currentSettings.showMedia, currentSettings.dismissMediaOnPause,
        System.currentTimeMillis() / 30_000L,
    ) + controllers.map { c ->
        val meta = c.metadata
        listOf(
            c.sessionToken, c.playbackState?.state, (c.playbackState?.position ?: 0L) / 1000L,
            meta?.getString(MediaMetadata.METADATA_KEY_TITLE), meta?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            meta?.getString(MediaMetadata.METADATA_KEY_ALBUM),
        )
    }

    private fun refreshMediaTraced(candidateController: MediaController?) {
        CrashLog.breadcrumb("nrs", "refreshMedia candidate=${candidateController?.packageName ?: "-"}")
        try {
            val componentName = ComponentName(this, NotificationReceiverService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)?.toMutableList() ?: mutableListOf()
            if (candidateController != null && controllers.none { it.packageName == candidateController.packageName }) {
                controllers.add(candidateController)
            }
            val signature = mediaSignature(controllers)
            if (candidateController == null && signature == lastMediaSignature) return
            lastMediaSignature = signature
            syncControllers(controllers)
            updateActiveMediaController(controllers)
        } catch (_: Exception) {
            updateMediaInfo()
        }
    }

    private fun syncControllers(controllers: List<MediaController>) {
        val toRemove = controllerCallbacks.keys.filter { existing ->
            controllers.none { it.sessionToken == existing.sessionToken }
        }
        for (old in toRemove) {
            val cb = controllerCallbacks.remove(old)
            if (cb != null) {
                runCatching { old.unregisterCallback(cb) }
            }
        }

        for (controller in controllers) {
            val already = controllerCallbacks.keys.any { it.sessionToken == controller.sessionToken }
            if (!already) {
                val cb = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        refreshMedia()
                    }

                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        refreshMedia()
                    }

                    override fun onSessionDestroyed() {
                        shuffleOverrides.remove(controller.packageName)
                        repeatOverrides.remove(controller.packageName)
                        refreshMedia()
                    }
                }
                runCatching { controller.registerCallback(cb) }
                controllerCallbacks[controller] = cb
            }
        }
    }

    private fun isControllerPlayingOrBuffering(controller: MediaController): Boolean {
        val state = controller.playbackState?.state ?: return false
        return state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING ||
                state == PlaybackState.STATE_FAST_FORWARDING ||
                state == PlaybackState.STATE_REWINDING
    }

    private fun getControllerTitle(controller: MediaController): String? {
        val meta = controller.metadata
        val rawTitle = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.takeIf { it.isNotBlank() }
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
        if (!rawTitle.isNullOrBlank()) return rawTitle

        val notif = mediaNotificationOf(controller.packageName)
        val notifTitle = notif?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: notif?.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.takeIf { it.isNotBlank() }
        return notifTitle
    }

    private fun findBestController(controllers: List<MediaController>?): MediaController? {
        if (controllers.isNullOrEmpty()) return null

        if (currentSettings.dismissMediaOnPause) {
            val playingWithMeta = controllers.firstOrNull {
                isControllerPlayingOrBuffering(it) && !getControllerTitle(it).isNullOrBlank()
            }
            if (playingWithMeta != null) return playingWithMeta
            return controllers.firstOrNull { isControllerPlayingOrBuffering(it) }
        }

        // 1. Prioritize any controller that is currently actively playing/buffering with valid title
        val playingWithMeta = controllers.firstOrNull {
            isControllerPlayingOrBuffering(it) && !getControllerTitle(it).isNullOrBlank()
        }
        if (playingWithMeta != null) return playingWithMeta

        // 2. If current active controller is still in the list and playing/buffering, keep it!
        val current = activeMediaController
        if (current != null) {
            val stillPresent = controllers.firstOrNull { it.packageName == current.packageName }
            if (stillPresent != null && isControllerPlayingOrBuffering(stillPresent)) {
                return stillPresent
            }
        }

        // 3. Current active controller if still present and has a valid title (paused)
        if (current != null) {
            val stillPresent = controllers.firstOrNull { it.packageName == current.packageName }
            if (stillPresent != null && !getControllerTitle(stillPresent).isNullOrBlank()) {
                return stillPresent
            }
        }

        // 4. Any controller with valid title (e.g. paused music player like Spotify, YouTube)
        val withMeta = controllers.firstOrNull {
            !getControllerTitle(it).isNullOrBlank()
        }
        if (withMeta != null) return withMeta

        // 5. Any controller that is currently actively playing/buffering (even without title yet)
        val playing = controllers.firstOrNull {
            isControllerPlayingOrBuffering(it)
        }
        if (playing != null) return playing

        // 6. Keep current controller if still in list
        if (current != null) {
            val stillPresent = controllers.firstOrNull { it.packageName == current.packageName }
            if (stillPresent != null) {
                return stillPresent
            }
        }

        return controllers.firstOrNull()
    }

    private fun updateActiveMediaController(controllers: List<MediaController>?) {
        val best = findBestController(controllers)
        if (best == null) {
            activeMediaController = null
            IslandStateManager.updateMedia(null)
            return
        }

        activeMediaController = best
        updateMediaInfo()
    }

    /**
     * B10.2: the media apps' own notifications, by key, kept from onNotificationPosted and
     * onNotificationRemoved. The media path used to call activeNotifications, a binder call that
     * unparcels every notification (bitmaps included), several times per refresh.
     */
    private val mediaNotifications = HashMap<String, StatusBarNotification>()

    private fun mediaNotificationOf(packageName: String): StatusBarNotification? =
        mediaNotifications.values.lastOrNull { it.packageName == packageName }

    private val artRequests = ArtRequests()
    private var cachedArtSourceSize: Pair<Int, Int>? = null

    /** B10.1: runs [work] on the IO dispatcher and applies the art only if its track is still current. */
    private fun loadArtAsync(artKey: String, sourceSize: Pair<Int, Int>?, work: () -> Bitmap?) {
        if (!artRequests.request(artKey)) return
        serviceScope.launch {
            val bitmap = withContext(Dispatchers.IO) { work() }
            if (artRequests.complete(artKey, bitmap != null) && bitmap != null) {
                cachedArtKey = artKey
                cachedArtBitmap = bitmap
                cachedArtSourceSize = sourceSize
                updateMediaInfo()
            }
        }
    }

    private fun updateMediaInfo() = traced("db.updateMediaInfo") { guarded("updateMediaInfo") { updateMediaInfoTraced() } }

    /**
     * B11.2: one malformed notification or a misbehaving media session is skipped and logged
     * (CrashLog.nonFatal) instead of crashing the listener, which START_STICKY would restart into
     * the same notification again. Cancellation passes through; an OutOfMemoryError is logged and
     * rethrown, because the process state is unknown after it.
     */
    private inline fun guarded(tag: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            CrashLog.nonFatal(tag, t)
            if (t is OutOfMemoryError) throw t
        }
    }

    private fun updateMediaInfoTraced() {
        if (!currentSettings.showMedia) {
            IslandStateManager.updateMedia(null)
            return
        }

        val controller = activeMediaController
        if (controller == null) {
            IslandStateManager.updateMedia(null)
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        if (metadata == null && playbackState == null) {
            IslandStateManager.updateMedia(null)
            return
        }

        val isPlaying = isControllerPlayingOrBuffering(controller)
        val nowMs = System.currentTimeMillis()
        if (isPlaying) mediaLastPlayingAt[controller.packageName] = nowMs
        // A paused session only keeps the island if it actually played recently. Apps like YouTube
        // keep a paused session alive after you leave a video, which showed a "playing" pill.
        val lastPlayedAt = mediaLastPlayingAt[controller.packageName]
            ?: playbackState?.lastPositionUpdateTime?.takeIf { it > 0L }?.let { updatedAt ->
                nowMs - (android.os.SystemClock.elapsedRealtime() - updatedAt).coerceAtLeast(0L)
            }
        if (!isPlaying && (lastPlayedAt == null || nowMs - lastPlayedAt > STALE_PAUSED_MEDIA_MS)) {
            IslandStateManager.updateMedia(null)
            return
        }
        if (currentSettings.dismissMediaOnPause && !isPlaying) {
            IslandStateManager.updateMedia(null)
            return
        }
        val title = getControllerTitle(controller) ?: if (isPlaying) "Playing Audio" else null
        if (title == null) {
            IslandStateManager.updateMedia(null)
            return
        }

        val rawArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_WRITER)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_COMPOSER)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)?.takeIf { it.isNotBlank() }

        val notif = mediaNotificationOf(controller.packageName)
        val notifArtist = notif?.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: notif?.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: notif?.notification?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.takeIf { it.isNotBlank() }

        val artist = rawArtist ?: notifArtist ?: try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(controller.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            controller.packageName
        }

        val metadataArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        // Same track as last time: reuse the cached art unless the app sent a differently sized image
        val artKey = "${controller.packageName}::$title::${rawArtist.orEmpty()}::${metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()}"
        artRequests.current(artKey)
        val metadataSize = metadataArt?.let { it.width to it.height }
        val cachedArt = cachedArtBitmap?.takeIf {
            artKey == cachedArtKey && (metadataArt == null || metadataSize == cachedArtSourceSize)
        }
        var artBitmap = cachedArt ?: metadataArt
        // B10.1: an oversized metadata bitmap is scaled down off the main thread; until then the
        // original shows, as before
        val oversizedMetadata = cachedArt == null && metadataArt != null &&
            maxOf(metadataArt.width, metadataArt.height) > ArtDecoder.MAX_METADATA_PX
        if (oversizedMetadata) {
            loadArtAsync(artKey, metadataSize) { ArtDecoder.scaleDown(metadataArt!!, ArtDecoder.MAX_METADATA_PX) }
        }

        // If bitmap is not directly available, check if artwork URI is provided. B10.1: decoded at
        // a sampled size on the IO dispatcher, and applied only if this track is still playing.
        if (artBitmap == null) {
            val uriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            if (!uriStr.isNullOrBlank()) {
                loadArtAsync(artKey, null) { ArtDecoder.decode(contentResolver, Uri.parse(uriStr)) }
            }
        }

        // Also check if an active notification from this app provides large icon art
        if (artBitmap == null) {
            artBitmap = runCatching {
                val notifObj = notif?.notification
                notifObj?.getLargeIcon()?.loadDrawable(this)?.let { drawableToBitmap(it) }
                    ?: (notifObj?.extras?.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
            }.getOrNull()
        }

        if (artBitmap != null && !oversizedMetadata && cachedArt == null) {
            cachedArtKey = artKey
            cachedArtBitmap = artBitmap
            cachedArtSourceSize = metadataSize
        }

        val appIconBitmap = getAppIconBitmap(controller.packageName)

        var duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (duration <= 0L) {
            val durationStr = metadata?.getString(MediaMetadata.METADATA_KEY_DURATION)
            if (!durationStr.isNullOrBlank()) {
                duration = durationStr.toLongOrNull() ?: 0L
            }
        }
        if (duration <= 0L && notif != null) {
            val maxProgress = notif.notification.extras?.getInt(Notification.EXTRA_PROGRESS_MAX) ?: 0
            val isIndeterminate = notif.notification.extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) ?: false
            if (!isIndeterminate && maxProgress > 0) {
                duration = if (maxProgress > 1000) maxProgress.toLong() else maxProgress * 1000L
            }
        }

        val position = playbackState?.position ?: 0L
        val speed = playbackState?.playbackSpeed?.takeIf { it > 0f } ?: 1f
        val lastUpdate = if (playbackState != null && playbackState.lastPositionUpdateTime > 0L) {
            playbackState.lastPositionUpdateTime
        } else {
            android.os.SystemClock.elapsedRealtime()
        }

        // Determine like capability
        val actions = playbackState?.actions ?: 0L
        val supportsRating = (actions and PlaybackState.ACTION_SET_RATING) != 0L
        val hasCustomLike = playbackState?.customActions?.any { ca ->
            val a = ca.action.lowercase()
            val n = ca.name?.toString()?.lowercase() ?: ""
            a.contains("like") || a.contains("favorite") || a.contains("thumb") ||
            n.contains("like") || n.contains("favorite") || n.contains("thumb")
        } ?: false
        val hasNotifLike = notif?.notification?.actions?.any { action ->
            val t = action.title?.toString()?.lowercase() ?: ""
            t.contains("like") || t.contains("favorite") || t.contains("thumb")
        } ?: false

        val isKnownMusicApp = controller.packageName in setOf(
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.soundcloud.android",
            "com.apple.android.music",
            "com.amazon.mp3",
            "deezer.android.app",
            "com.aspiro.tidal"
        )

        val canLike = supportsRating || hasCustomLike || hasNotifLike || isKnownMusicApp

        val trackKey = "${controller.packageName}::$title"
        val userExplicitLike = userLikedTracks[trackKey]

        val userRating = metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
        val isLikedFromRating = if (userRating != null && userRating.isRated) {
            userRating.hasHeart() || userRating.isThumbUp
        } else false

        val isLikedFromAction = playbackState?.customActions?.any { ca ->
            val a = ca.action.lowercase()
            val n = ca.name?.toString()?.lowercase() ?: ""
            (a.contains("unlike") || n.contains("unlike") || a.contains("remove_like") || n.contains("remove_like") || a.contains("remove_favorite")) &&
            !a.contains("like /") && !n.contains("like /")
        } ?: notif?.notification?.actions?.any { action ->
            val t = action.title?.toString()?.lowercase() ?: ""
            (t.contains("unlike") || t.contains("remove like") || t.contains("remove from favorites")) && !t.contains("like /")
        } ?: false

        val isLiked = userExplicitLike ?: (isLikedFromRating || isLikedFromAction)

        IslandStateManager.updateMedia(
            MediaInfo(
                packageName = controller.packageName,
                title = title,
                artist = artist,
                albumArt = artBitmap,
                appIcon = appIconBitmap,
                sessionActivity = controller.sessionActivity,
                isPlaying = isPlaying,
                durationMs = duration,
                positionMs = position,
                playbackSpeed = speed,
                lastPositionUpdateTimeMs = lastUpdate,
                canLike = canLike,
                isLiked = isLiked,
                isShuffleOn = shuffleOverrides[controller.packageName]
                    ?: (findCustomAction(controller, "shuffle")?.name?.toString()?.let { name ->
                        // A custom action labelled "turn off / disable shuffle" means it's on now
                        name.contains("off", ignoreCase = true) || name.contains("disable", ignoreCase = true)
                    } ?: false),
                repeatMode = repeatOverrides[controller.packageName] ?: MediaInfo.REPEAT_OFF
            )
        )
    }

    private var systemRecordingKey: String? = null
    private var recordingTimerJob: Job? = null
    private var recordingStopJob: Job? = null
    private var recordingStartTimeMs: Long = 0L
    private var recordingPausedAtMs: Long = 0L
    private var activeStopAction: Notification.Action? = null
    private var activePauseAction: Notification.Action? = null
    private var activeResumeAction: Notification.Action? = null

    private var systemCallKey: String? = null
    private var callTimerJob: Job? = null
    private var callStartTimeMs: Long = 0L
    private var activeEndCallAction: Notification.Action? = null
    private var activeMuteOrSpeakerAction: Notification.Action? = null

    private fun isOngoingCallNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val isOngoing = sbn.isOngoing ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (notification.flags and Notification.FLAG_NO_CLEAR) != 0
        if (!isOngoing) return false

        val extras = notification.extras ?: return false
        val template = extras.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val isCallCategory = notification.category == Notification.CATEGORY_CALL || template.contains("CallStyle")

        val actions = notification.actions
        val actionTitles = actions?.mapNotNull { it?.title?.toString()?.lowercase(java.util.Locale.ROOT) } ?: emptyList()

        val hasAnswerOrDecline = actionTitles.any {
            it.contains("answer") || it.contains("accept") || it.contains("decline") || it.contains("reject")
        }
        if (hasAnswerOrDecline) {
            // Incoming (ringing) call -> user opted for active/ongoing calls only
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callType = extras.getInt("android.callType", -1)
            if (callType == Notification.CallStyle.CALL_TYPE_INCOMING || callType == Notification.CallStyle.CALL_TYPE_SCREENING) {
                return false
            }
            if (callType == Notification.CallStyle.CALL_TYPE_ONGOING) {
                return true
            }
        }

        val hasHangupAction = actionTitles.any {
            it.contains("hang up") || it.contains("hangup") || it.contains("end call") || it == "end" || it.contains("leave") || it.contains("disconnect")
        }

        val pkg = sbn.packageName.lowercase(java.util.Locale.ROOT)
        val isDialerPkg = pkg.contains("dialer") || pkg.contains("incallui") || pkg.contains("telecom") || pkg.contains("phone") ||
                pkg == "com.whatsapp" || pkg == "org.telegram.messenger" || pkg == "com.discord" || pkg == "com.viber.voip" || pkg == "com.facebook.orca"

        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val subText = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").lowercase(java.util.Locale.ROOT)
        val isIncomingText = text.contains("incoming call") || subText.contains("incoming call") || text.contains("ringing")
        if (isIncomingText) return false

        val isOngoingCallText = text.contains("ongoing call") || text.contains("call in progress") || subText.contains("ongoing call")

        return (isCallCategory && (hasHangupAction || isDialerPkg || isOngoingCallText)) || (isDialerPkg && hasHangupAction)
    }

    private fun handleOngoingCallNotification(sbn: StatusBarNotification) {
        val isNewCall = systemCallKey != sbn.key || !IslandStateManager.activeCallState.value.isActive
        systemCallKey = sbn.key
        val notification = sbn.notification ?: return
        val extras = notification.extras

        val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.takeIf { it.isNotBlank() }
            ?: run {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val person = extras?.getParcelable("android.callPerson") as? android.app.Person
                    person?.name?.toString()?.takeIf { it.isNotBlank() }
                } else null
            }
            ?: "Active Call"

        activeEndCallAction = null
        activeMuteOrSpeakerAction = null

        val actions = notification.actions
        actions?.forEach { action ->
            val aTitle = (action?.title?.toString() ?: "").lowercase(java.util.Locale.ROOT)
            if (aTitle.hasWord("hang up", "hangup", "end", "leave", "disconnect")) {
                activeEndCallAction = action
            } else if (aTitle.contains("mute") || aTitle.contains("unmute") || aTitle.contains("speaker") || aTitle.contains("audio")) {
                if (activeMuteOrSpeakerAction == null || aTitle.contains("mute")) {
                    activeMuteOrSpeakerAction = action
                }
            }
        }
        if (activeEndCallAction == null && !actions.isNullOrEmpty()) {
            activeEndCallAction = actions.last()
        }
        if (activeMuteOrSpeakerAction == null && actions != null && actions.size >= 2) {
            activeMuteOrSpeakerAction = actions.firstOrNull { it != activeEndCallAction }
        }

        val muteTitleLower = activeMuteOrSpeakerAction?.title?.toString()?.lowercase(java.util.Locale.ROOT) ?: ""
        val isSpeakerButton = muteTitleLower.contains("speaker") || muteTitleLower.contains("audio")
        val isCurrentlyMuted = muteTitleLower.contains("unmute")

        IslandStateManager.onEndCall = {
            try {
                activeEndCallAction?.actionIntent?.send()
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error sending end call intent", e)
            }
            callTimerJob?.cancel()
            callTimerJob = null
            systemCallKey = null
            activeEndCallAction = null
            activeMuteOrSpeakerAction = null
            IslandStateManager.updateCallState(ActiveCallState(isActive = false))
        }

        IslandStateManager.onToggleCallMuteOrSpeaker = {
            val current = IslandStateManager.activeCallState.value
            var sent = false
            val actionToInvoke = activeMuteOrSpeakerAction
            if (actionToInvoke?.actionIntent != null) {
                try {
                    actionToInvoke.actionIntent.send()
                    sent = true
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending call mute/speaker intent", e)
                }
            }
            if (!sent) {
                try {
                    val am = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    if (am != null) {
                        am.isMicrophoneMute = !current.isMuted
                    }
                } catch (_: Exception) {}
            }
            IslandStateManager.updateCallState(current.copy(isMuted = !current.isMuted))
        }

        val chronometerBase = notification.`when`
        if (isNewCall) {
            callStartTimeMs = if (chronometerBase > 0L && chronometerBase <= System.currentTimeMillis()) {
                chronometerBase
            } else if (sbn.postTime > 0L && sbn.postTime <= System.currentTimeMillis()) {
                sbn.postTime
            } else {
                System.currentTimeMillis()
            }
        } else if (chronometerBase > 0L && chronometerBase <= System.currentTimeMillis()) {
            callStartTimeMs = chronometerBase
        }

        val elapsedSeconds = ((System.currentTimeMillis() - callStartTimeMs) / 1000).coerceAtLeast(0L)
        val prevMuted = IslandStateManager.activeCallState.value.isMuted
        IslandStateManager.updateCallState(
            ActiveCallState(
                isActive = true,
                callerName = rawTitle,
                packageName = sbn.packageName,
                durationSeconds = elapsedSeconds,
                isMuted = if (muteTitleLower.isNotBlank()) isCurrentlyMuted else prevMuted,
                isSpeaker = isSpeakerButton,
                contentIntent = notification.contentIntent
            )
        )

        if (callTimerJob == null || callTimerJob?.isActive != true) {
            callTimerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000)
                    val currentState = IslandStateManager.activeCallState.value
                    if (currentState.isActive) {
                        val elapsed = ((System.currentTimeMillis() - callStartTimeMs) / 1000).coerceAtLeast(0L)
                        IslandStateManager.updateCallState(currentState.copy(durationSeconds = elapsed))
                    }
                }
            }
        }
    }

    private fun isScreenRecordingSavedNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        val extras = notification.extras
        val title = (extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").lowercase()
        val text = (extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").lowercase()
        val subText = (extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").lowercase()
        val combined = "$title $text $subText"

        return combined.contains("saved") ||
                combined.contains("finished") ||
                combined.contains("completed") ||
                combined.contains("tap to view") ||
                combined.contains("tap to share") ||
                combined.contains("share") ||
                combined.contains("deleted") ||
                combined.contains("ready") ||
                // B4.4: an idle recorder waiting to be started isn't recording
                combined.contains("tap to start") ||
                combined.contains("ready to record")
    }

    private fun isScreenRecordingNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        // 1. If it's a "saved" / completed notification, it's NOT an active screen recording!
        if (isScreenRecordingSavedNotification(sbn, notification)) {
            return false
        }

        // 2. Active screen recording MUST be ongoing (cannot be dismissed while recording)
        val isOngoing = sbn.isOngoing ||
                (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (notification.flags and Notification.FLAG_NO_CLEAR) != 0
        if (!isOngoing) {
            return false
        }

        val pkg = sbn.packageName.lowercase()
        val channelId = notification.channelId?.lowercase() ?: ""
        val extras = notification.extras
        val title = (extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").lowercase()
        val text = (extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").lowercase()
        val subText = (extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: "").lowercase()

        val isRecordingText = title.contains("screen record") ||
                title.contains("recording screen") ||
                title.contains("screen recording") ||
                title.contains("screen recorder") ||
                title.contains("recording display") ||
                text.contains("screen record") ||
                text.contains("recording screen") ||
                text.contains("screen recording") ||
                subText.contains("recording screen")

        val isRecordingPkg = pkg.contains("screenrecorder") ||
                pkg.contains("screen_recorder") ||
                pkg.contains("screenrecord") ||
                pkg.contains("hecorat.screenrecorder") ||
                pkg == "com.android.systemui" ||
                pkg == "com.google.android.systemui"

        val isRecordingChannel = channelId.contains("screen_record") ||
                channelId.contains("screenrecord") ||
                channelId.contains("screen_capture")

        // B4.4: recording text alone isn't enough (a third-party recorder's idle "Screen recorder"
        // notification says it too). It also needs a recording channel, a recorder or system-UI
        // package (isRecordingPkg covers both), or a running chronometer.
        val showsChronometer = extras?.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) == true
        return (isRecordingChannel && (isRecordingText || isRecordingPkg)) ||
                (isRecordingPkg && isRecordingText) ||
                (showsChronometer && isRecordingText)
    }

    private fun handleScreenRecordingNotification(sbn: StatusBarNotification) {
        systemRecordingKey = sbn.key
        // The recorder re-posts (or remove+reposts) its ongoing notification on every tick:
        // cancel any pending stop so the timer keeps counting through updates.
        recordingStopJob?.cancel()
        recordingStopJob = null
        val notification = sbn.notification ?: return

        // Extract actions: Stop, Pause, Resume
        val actions = notification.actions
        actions?.forEach { action ->
            val aTitle = (action.title?.toString() ?: "").lowercase()
            if (aTitle.hasWord("stop", "end", "finish")) {
                activeStopAction = action
            } else if (aTitle.contains("pause")) {
                activePauseAction = action
            } else if (aTitle.contains("resume")) {
                activeResumeAction = action
            }
        }
        if (activeStopAction == null && !actions.isNullOrEmpty() && actions.size == 1) {
            activeStopAction = actions[0]
        }

        // Connect functions so island buttons trigger real recording actions
        IslandStateManager.onStopRecording = {
            try {
                activeStopAction?.actionIntent?.send()
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error sending stop intent", e)
            }
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            recordingStopJob?.cancel()
            recordingStopJob = null
            systemRecordingKey = null
            activeStopAction = null
            activePauseAction = null
            activeResumeAction = null
            recordingStartTimeMs = 0L
            recordingPausedAtMs = 0L
            IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
        }

        IslandStateManager.onTogglePauseRecording = {
            val currentState = IslandStateManager.screenRecordingState.value
            if (currentState.isPaused) {
                try {
                    activeResumeAction?.actionIntent?.send() ?: activePauseAction?.actionIntent?.send()
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending resume intent", e)
                }
                if (recordingPausedAtMs > 0) {
                    recordingStartTimeMs += (System.currentTimeMillis() - recordingPausedAtMs)
                    recordingPausedAtMs = 0L
                }
                IslandStateManager.updateRecordingState(currentState.copy(isPaused = false))
            } else {
                try {
                    activePauseAction?.actionIntent?.send()
                } catch (e: Exception) {
                    Log.e("NotificationReceiver", "Error sending pause intent", e)
                }
                recordingPausedAtMs = System.currentTimeMillis()
                IslandStateManager.updateRecordingState(currentState.copy(isPaused = true))
            }
        }

        if (recordingTimerJob == null || !IslandStateManager.screenRecordingState.value.isRecording) {
            val now = System.currentTimeMillis()
            // Anchor the elapsed clock once per recording. Prefer the notification's `when`
            // timestamp (constant recording start), then postTime, and never move the anchor
            // backwards so remove+repost updates can't reset the visible timer to 00:00.
            val anchor = when {
                recordingStartTimeMs in 1..now -> recordingStartTimeMs
                notification.`when` in 1..now -> notification.`when`
                sbn.postTime in 1..now -> sbn.postTime
                else -> now
            }
            recordingStartTimeMs = anchor
            recordingPausedAtMs = 0L
            val initialSeconds = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).coerceAtLeast(0L)
            IslandStateManager.updateRecordingState(
                ScreenRecordingState(isRecording = true, durationSeconds = initialSeconds, isPaused = false)
            )
            recordingTimerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000)
                    val currentState = IslandStateManager.screenRecordingState.value
                    if (!currentState.isPaused) {
                        val elapsed = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).coerceAtLeast(0L)
                        IslandStateManager.updateRecordingState(
                            currentState.copy(isRecording = true, durationSeconds = elapsed)
                        )
                    }
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn != null) CrashLog.breadcrumb("nrs", "posted pkg=${sbn.packageName} keyHash=${CrashLog.keyHash(sbn.key)}")
        traced("db.posted") { guarded("posted:${sbn?.packageName}") { handlePosted(sbn) } }
    }

    private fun handlePosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!currentSettings.showNotifications && !currentSettings.showLiveActivities && !currentSettings.showLiveProgress &&
            !currentSettings.showScreenRecording && !currentSettings.showCalls) return

        val packageName = sbn.packageName
        if (packageName == this.packageName) return // Ignore self
        if (currentSettings.blockedPackages.contains(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Group summaries ("5 messages from 2 chats") would replace the specific message card
        // and lose its reply action
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        // When an update of a card that's on screen gets filtered below, the old card must go too
        // (e.g. a download turning into "Download complete", a paused timer losing its chronometer)
        fun dropIfShown() = IslandStateManager.removeNotification(sbn.key)

        // 0. If a "screen recording saved" notification arrives, immediately shut down recording state
        if (isScreenRecordingSavedNotification(sbn, notification)) {
            val pkg = sbn.packageName.lowercase()
            val channelId = notification.channelId?.lowercase() ?: ""
            if (pkg.contains("screenrecord") || pkg == "com.android.systemui" || pkg == "com.google.android.systemui" || channelId.contains("screen_record") || systemRecordingKey == sbn.key) {
                systemRecordingKey = null
                recordingTimerJob?.cancel()
                recordingTimerJob = null
                recordingStopJob?.cancel()
                recordingStopJob = null
                activeStopAction = null
                activePauseAction = null
                activeResumeAction = null
                recordingStartTimeMs = 0L
                recordingPausedAtMs = 0L
                IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
                return
            }
        }

        // 1. Detect system screen recording notifications
        if (isScreenRecordingNotification(sbn, notification)) {
            if (currentSettings.showScreenRecording) {
                handleScreenRecordingNotification(sbn)
            }
            return
        }

        // 1B. Detect active ongoing phone call notifications
        if (isOngoingCallNotification(sbn, notification)) {
            if (currentSettings.showCalls) {
                handleOngoingCallNotification(sbn)
            }
            return
        } else if (systemCallKey == sbn.key) {
            // Call notification transitioned out of ongoing state
            systemCallKey = null
            callTimerJob?.cancel()
            callTimerJob = null
            activeEndCallAction = null
            activeMuteOrSpeakerAction = null
            IslandStateManager.updateCallState(ActiveCallState(isActive = false))
        }

        // 2. Skip media notifications (Media controls are handled exclusively via MediaSessionManager with slider & controls)
        if (isMediaNotification(sbn, notification)) {
            mediaNotifications[sbn.key] = sbn
            val token: android.media.session.MediaSession.Token? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? android.media.session.MediaSession.Token
            } ?: run {
                @Suppress("DEPRECATION")
                extras.getParcelable("android.mediaSession") as? android.media.session.MediaSession.Token
            }

            val directController = token?.let {
                runCatching { MediaController(this, it) }.getOrNull()
            }
            refreshMedia(directController)
            return
        }

        // 3. Extract live progress tracking (e.g. Google Play Store, downloads, transfers, navigation)
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val isProgressStyle = template.contains("ProgressStyle")
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        // Android 16 ProgressStyle: the max is the sum of the segment lengths
        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0).takeIf { it > 0 }
            ?: if (isProgressStyle) progressStyleMax(extras) else 0
        val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing
        // Android 16 "Live Updates": promoted ongoing notifications (rides, deliveries, navigation...)
        val isPromoted = (Build.VERSION.SDK_INT >= 36 && (notification.flags and FLAG_PROMOTED_ONGOING) != 0) ||
            (isProgressStyle && isOngoing)
        val shortCriticalText = extras.getCharSequence(EXTRA_SHORT_CRITICAL_TEXT)?.toString()?.takeIf { it.isNotBlank() }
        val rawWords = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)}".lowercase()
        // Ringing alarm or expired timer: the clock app posts it as an alarm with a full-screen intent
        val isAlert = isClockPackage(packageName) && (
            (notification.category == Notification.CATEGORY_ALARM && notification.fullScreenIntent != null) ||
                "time's up" in rawWords || "time\u2019s up" in rawWords || "time is up" in rawWords
            )
        // Android 14 foreground-service notifications can lack FLAG_ONGOING_EVENT, so check both.
        val isForegroundServiceNotif = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        val ongoingLike = isOngoing || isForegroundServiceNotif
        // Live activities that aren't timers: alerts, live updates, rides / deliveries / flights,
        // live scores and clock / recorder apps (alarms, paused recordings).
        // B4.2: a ride, sports or recorder app only counts when its notification is ongoing, so its
        // marketing and silent pushes still meet the ongoing, silent-channel and on-screen filters.
        // An upcoming alarm isn't always ongoing, so the clock clause stays as it is.
        val isLiveCandidate = isAlert || isPromoted ||
            (ongoingLike && (isTrackingPackage(packageName) || isSportsPackage(packageName) || isRecorderPackage(packageName))) ||
            (isClockPackage(packageName) && ("alarm" in rawWords || "snooz" in rawWords))
        val isNavCategory = notification.category == Notification.CATEGORY_NAVIGATION
        val isNavApp = packageName == "com.google.android.apps.maps" || packageName == "com.waze" || packageName.contains("maps")
        val isNavigation = (isNavCategory || isNavApp) && isOngoing
        val hasProgress = maxProgress > 0 || isIndeterminate || (progress >= 0 && maxProgress > 0) || isNavigation

        val callType = extras.getInt("android.callType", 0) // CallStyle: 1 incoming, 2 ongoing, 3 screening
        val isCall = notification.category == Notification.CATEGORY_CALL ||
            extras.getString(Notification.EXTRA_TEMPLATE)?.contains("CallStyle") == true

        // Skip persistent notifications ("App is running", VPN...) UNLESS they are live activities:
        // progress (downloads, navigation), calls, or a running timer / stopwatch / recorder.
        // Clock apps (AOSP / Google Clock) draw the countdown in a custom view, so the notification
        // has no title, text or chronometer extras at all: read the timer out of that view instead.
        val customTimer = if ((isOngoing || isForegroundServiceNotif) && !hasProgress && !isCall &&
            !extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) &&
            (isClockOrRecorderPackage(packageName) || notification.category == Notification.CATEGORY_ALARM ||
                notification.category == "stopwatch")
        ) readCustomViewTimer(notification) else null
        val looksLikeTimer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false) ||
            parseTimerTextMillis(extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()) != null ||
            customTimer != null
        // B3.1: each kind has its own toggle. "Incoming Notifications" only covers message banners;
        // calls, live activities and progress pass on their own switches.
        if (!currentSettings.allows(gateKind(isCall = isCall, isLive = looksLikeTimer || isLiveCandidate, hasProgress = hasProgress))) {
            dropIfShown()
            return
        }
        if ((isOngoing || isForegroundServiceNotif) && !hasProgress && !isCall && !looksLikeTimer && !isLiveCandidate) {
            dropIfShown()
            return
        }

        // Silent / low-importance channels don't pop the island (calls and live activities always do)
        if (!isCall && !looksLikeTimer && !hasProgress && !isLiveCandidate && isLowImportance(sbn.key)) {
            dropIfShown()
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.takeIf { it.isNotBlank() }
            ?: customTimer?.label
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.takeIf { it.isNotBlank() }
            ?: customTimer?.timeText

        if (title.isNullOrBlank() && text.isNullOrBlank()) { dropIfShown(); return }
        // Not from the app on screen -- except calls, whose answered/ended updates must get through
        // even while the in-call screen is open
        // An update of a card that's already showing is ignored too, but the card is kept: it is hidden
        // while its app is on screen and only ends when swiped away or cleared from the shade.
        // Timers / stopwatches / recorders are the exception too: they're almost always started from
        // inside their app, and a countdown is never reposted, so dropping that first post would
        // mean the timer never reaches the island. The island keeps it hidden until the app is left.
        if (!hasProgress && !isCall && !looksLikeTimer && !isLiveCandidate && packageName == IslandStateManager.foregroundPackage.value) {
            return
        }

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val iconBitmap = extractNotificationBitmap(notification, packageName)
        val callPerson: android.app.Person? = if (isCall) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable("android.callPerson", android.app.Person::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable("android.callPerson") as? android.app.Person
            }
        } else null
        val avatarBitmap = callPerson?.icon?.let { icon ->
            runCatching { icon.loadDrawable(this)?.let { drawableToBitmap(it) } }.getOrNull()
        } ?: extractAvatarBitmap(notification)

        // Answer / decline: CallStyle extras first, then the notification's own buttons
        fun callIntent(key: String): PendingIntent? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(key, PendingIntent::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(key) as? PendingIntent
            }
        fun actionIntent(vararg words: String): PendingIntent? = notification.actions?.firstOrNull { action ->
            (action.title?.toString() ?: "").hasWord(*words)
        }?.actionIntent
        val answerIntent = if (isCall) callIntent("android.answerIntent") ?: actionIntent("answer", "accept") else null
        val declineIntent = if (isCall) {
            callIntent("android.declineIntent") ?: callIntent("android.hangUpIntent") ?: actionIntent("decline", "reject", "hang up", "end")
        } else null
        val isIncomingCall = isCall && (callType == 1 || (callType == 0 && answerIntent != null))
        val callerNumber = callPerson?.uri?.takeIf { it.startsWith("tel:") }?.removePrefix("tel:")
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { t -> t.any { it.isDigit() } && t.count { it.isDigit() } >= 5 }

        // Live timers (voice recorders, calls, stopwatches): the app either shows a system
        // chronometer counting from `when`, or posts a static "00:00"-style title that the
        // listener never sees tick. Keep a base so the island can count locally.
        val showsChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        val timerTitleMs = parseTimerTextMillis(title)
        val isTimerPaused = text?.contains("pause", ignoreCase = true) == true
        val isForegroundService = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        val baseFromText = !(showsChronometer && notification.`when` > 0L) && customTimer == null
        val rawLiveTimerBaseMs = when {
            showsChronometer && notification.`when` > 0L -> notification.`when`
            customTimer != null -> if (customTimer.running) customTimer.baseWallMs else 0L
            timerTitleMs != null && !isTimerPaused && (isForegroundService || isOngoing) -> sbn.postTime - timerTitleMs
            else -> 0L
        }
        // A paused countdown keeps the flag so it still gets the timer design, not the stopwatch one
        val liveTimerCountDown = (showsChronometer && extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false)) ||
            customTimer?.countDown == true
        // Same start time on every update, so the island clock never steps back or stalls
        val liveTimerBaseMs = liveTimerBases.stabilize("${sbn.key}|$liveTimerCountDown", rawLiveTimerBaseMs, baseFromText)

        val actions = mutableListOf<NotificationAction>()
        notification.actions?.forEach { action ->
            if (action != null && !action.title.isNullOrBlank()) {
                val remoteInput = action.remoteInputs?.firstOrNull()
                val choices = remoteInput?.choices?.map { it.toString() } ?: emptyList()
                actions.add(
                    NotificationAction(
                        title = action.title.toString(),
                        actionIntent = action.actionIntent,
                        remoteInputKey = remoteInput?.resultKey,
                        remoteInputLabel = remoteInput?.label?.toString(),
                        remoteInputChoices = choices
                    )
                )
            }
        }

        val notifInfo = NotificationInfo(
            key = sbn.key,
            packageName = packageName,
            appName = appName,
            title = callPerson?.name?.toString()?.takeIf { it.isNotBlank() } ?: title ?: appName,
            text = text ?: "",
            iconBitmap = iconBitmap,
            avatarBitmap = avatarBitmap,
            contentIntent = notification.contentIntent,
            actions = actions.take(3),
            timestamp = sbn.postTime,
            progress = progress,
            maxProgress = maxProgress,
            isIndeterminate = isIndeterminate || (isNavigation && maxProgress <= 0),
            // Android 14 lets foreground-service notifications be swiped, dropping the ongoing flag;
            // they are still live activities (recorders, timers)
            isOngoing = isOngoing || (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
            liveTimerBaseMs = liveTimerBaseMs,
            liveTimerCountDown = liveTimerCountDown,
            isAutoCancel = (notification.flags and Notification.FLAG_AUTO_CANCEL) != 0,
            isCall = isCall,
            isIncomingCall = isIncomingCall,
            callerNumber = callerNumber,
            answerIntent = answerIntent,
            declineIntent = declineIntent,
            category = notification.category,
            isAlert = isAlert,
            isPromoted = isPromoted,
            shortCriticalText = shortCriticalText
        )

        // Same key switching between progress and alert (download -> "Download complete")
        // Live updates / rides keep their own card even with a progress bar
        if (hasProgress && com.droidbubble.app.island.liveActivityKind(notifInfo) == com.droidbubble.app.island.LiveActivityKind.NONE) {
            if (IslandStateManager.activeNotification.value?.key == sbn.key) IslandStateManager.removeNotification(sbn.key)
            IslandStateManager.updateProgress(notifInfo)
        } else {
            if (IslandStateManager.activeProgress.value?.key == sbn.key) IslandStateManager.updateProgress(null)
            IslandStateManager.updateNotification(notifInfo)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        CrashLog.breadcrumb("nrs", "removed pkg=${sbn.packageName} keyHash=${CrashLog.keyHash(sbn.key)}")
        guarded("removed:${sbn.packageName}") { handleRemoved(sbn) }
    }

    private fun handleRemoved(sbn: StatusBarNotification) {
        if (systemRecordingKey == sbn.key || (sbn.notification != null && isScreenRecordingNotification(sbn, sbn.notification))) {
            // Many recorders remove+repost the same ongoing notification on every update,
            // so postpone the stop briefly: an immediate repost cancels this and keeps the timer alive.
            recordingStopJob?.cancel()
            recordingStopJob = serviceScope.launch {
                delay(2500)
                systemRecordingKey = null
                activeStopAction = null
                activePauseAction = null
                activeResumeAction = null
                recordingTimerJob?.cancel()
                recordingTimerJob = null
                recordingStartTimeMs = 0L
                recordingPausedAtMs = 0L
                IslandStateManager.updateRecordingState(ScreenRecordingState(isRecording = false))
            }
        }
        if (systemCallKey == sbn.key || (sbn.notification != null && isOngoingCallNotification(sbn, sbn.notification))) {
            systemCallKey = null
            callTimerJob?.cancel()
            callTimerJob = null
            activeEndCallAction = null
            activeMuteOrSpeakerAction = null
            IslandStateManager.updateCallState(ActiveCallState(isActive = false))
        }
        mediaNotifications.remove(sbn.key)
        val notifObj = sbn.notification
        if (notifObj != null && (isMediaNotification(sbn, notifObj) || sbn.packageName == activeMediaController?.packageName)) {
            refreshMedia()
        }
        IslandStateManager.removeNotification(sbn.key)
    }

    private fun extractNotificationBitmap(notification: Notification, packageName: String): Bitmap? {
        getAppIconBitmap(packageName)?.let { return it }
        val smallIcon: Icon? = notification.smallIcon
        if (smallIcon != null) {
            val drawable = smallIcon.loadDrawable(this)
            if (drawable != null) return drawableToBitmap(drawable)
        }
        return getAppIconBitmap(packageName)
    }

    private val appIconCache = android.util.LruCache<String, Bitmap>(30)

    // Kept across remove + repost, which many recorders do on every tick
    private val liveTimerBases = com.droidbubble.app.model.LiveTimerBaseStabilizer()

    private val mediaLastPlayingAt = mutableMapOf<String, Long>()
    private val shuffleOverrides = mutableMapOf<String, Boolean>()
    private val repeatOverrides = mutableMapOf<String, Int>()

    private fun findCustomAction(controller: MediaController, keyword: String): PlaybackState.CustomAction? =
        controller.playbackState?.customActions?.firstOrNull { ca ->
            ca.action.contains(keyword, ignoreCase = true) || ca.name?.toString()?.contains(keyword, ignoreCase = true) == true
        }

    /** Contact / sender picture: last MessagingStyle sender's icon, else the large icon. */
    private fun extractAvatarBitmap(notification: Notification): Bitmap? {
        runCatching {
            val style = androidx.core.app.NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
            val senderIcon = style?.messages?.lastOrNull()?.person?.icon
            senderIcon?.loadDrawable(this)?.let { return drawableToBitmap(it) }
        }
        return runCatching {
            notification.getLargeIcon()?.loadDrawable(this)?.let { drawableToBitmap(it) }
        }.getOrNull()
    }

    // Every MediaController query unparcels a brand-new Bitmap, so without this the island would
    // get a "new" image on each play/pause/seek and redo its blur + accent-colour extraction.
    private var cachedArtKey: String? = null
    private var cachedArtBitmap: Bitmap? = null

    private fun getAppIconBitmap(packageName: String): Bitmap? {
        appIconCache.get(packageName)?.let { return it }
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            appIconCache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /** Sum of an Android 16 ProgressStyle's segment lengths (its progress max). */
    private fun progressStyleMax(extras: Bundle): Int = runCatching {
        @Suppress("DEPRECATION")
        val segments = extras.getParcelableArrayList<Bundle>("android.progressSegments")
        segments?.sumOf { it.getInt("length", 0) } ?: 0
    }.getOrDefault(0)

    /** A timer read from a notification's custom RemoteViews (see [readCustomViewTimer]). */
    private data class CustomViewTimer(
        val baseWallMs: Long,
        val countDown: Boolean,
        val running: Boolean,
        val timeText: String?,
        val label: String?
    )

    /**
     * Inflates a custom-view notification and pulls the [android.widget.Chronometer] out of it.
     * Only standard notifications built with setCustomContentView carry these views; for everything
     * else contentView is null on Android 7+ and this returns right away.
     */
    @Suppress("DEPRECATION")
    private fun readCustomViewTimer(notification: Notification): CustomViewTimer? {
        val views = notification.contentView ?: notification.bigContentView ?: return null
        val root = runCatching { views.apply(this, android.widget.FrameLayout(this)) }.getOrNull() ?: return null
        var chrono: android.widget.Chronometer? = null
        val labels = mutableListOf<String>()
        fun walk(v: android.view.View) {
            when (v) {
                is android.widget.Chronometer -> if (chrono == null && v.visibility == android.view.View.VISIBLE) chrono = v
                is android.widget.TextView -> v.text?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() && v.visibility == android.view.View.VISIBLE }
                    ?.let(labels::add)
                is android.view.ViewGroup -> for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
        }
        walk(root)
        val c = chrono ?: return null
        val countDown = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && c.isCountDown
        val baseWall = System.currentTimeMillis() + (c.base - android.os.SystemClock.elapsedRealtime())
        val timeText = c.text?.toString()?.replace("−", "")?.replace("-", "")?.trim()?.takeIf { it.isNotEmpty() }
        // Chronometer exposes no "started" flag: a running timer offers Pause, a paused one Resume / Start
        val actionTitles = notification.actions?.mapNotNull { it?.title?.toString()?.lowercase() }.orEmpty()
        val offersPause = actionTitles.any { "pause" in it || "stop" in it && "stopwatch" !in it }
        val offersResume = actionTitles.any { "resume" in it || "start" in it || "continue" in it }
        val saysPaused = labels.any { it.contains("paused", ignoreCase = true) }
        val running = !saysPaused && (offersPause || !offersResume)
        val label = labels.firstOrNull { parseTimerTextMillis(it) == null }
            ?: if (countDown) "Timer" else "Stopwatch"
        return CustomViewTimer(baseWall, countDown, running, timeText, label)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun isMediaNotification(sbn: StatusBarNotification, notification: Notification): Boolean {
        // 1. Category is TRANSPORT
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true

        // 2. Extras contains media session token or MediaStyle template
        val extras = notification.extras
        if (extras != null) {
            if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                extras.containsKey("android.mediaSession") ||
                extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
            ) {
                return true
            }
        }

        // 3. Players with custom notifications: an ongoing, non-progress notification from an app whose
        // media session is actually playing (messages and downloads never match this)
        val isOngoingNotif = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing
        val hasProgressExtras = (extras?.getInt(Notification.EXTRA_PROGRESS_MAX, 0) ?: 0) > 0 ||
            extras?.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) == true
        if (isOngoingNotif && !hasProgressExtras) {
            try {
                val componentName = ComponentName(this, NotificationReceiverService::class.java)
                val activeSessions = mediaSessionManager?.getActiveSessions(componentName)
                if (activeSessions?.any { it.packageName == sbn.packageName && isControllerPlayingOrBuffering(it) } == true) return true
            } catch (_: Exception) {}
        }

        return false
    }
}
