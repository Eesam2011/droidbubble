package com.droidbubble.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.parseFeaturePriorities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "island_settings")

private object PreferencesKeys {
    val IS_ENABLED = booleanPreferencesKey("is_enabled")
    val WIDTH_DP = intPreferencesKey("width_dp")
    val HEIGHT_DP = intPreferencesKey("height_dp")
    val TOP_OFFSET_DP = intPreferencesKey("top_offset_dp")
    val HORIZONTAL_OFFSET_DP = intPreferencesKey("horizontal_offset_dp")
    val IS_LEFT_CUTOUT = booleanPreferencesKey("is_left_cutout")
    val SHOW_OUTLINE = booleanPreferencesKey("show_outline")
    val LIQUID_GLASS_CARDS = booleanPreferencesKey("liquid_glass_cards")
    val OUTLINE_COLOR_ARGB = longPreferencesKey("outline_color_argb")
    val OUTLINE_THICKNESS_DP = floatPreferencesKey("outline_thickness_dp")
    val EXPANDED_SCALE = floatPreferencesKey("expanded_scale")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val SHOW_CALLS = booleanPreferencesKey("show_calls")
    val SHOW_MEDIA = booleanPreferencesKey("show_media")
    val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
    val SHOW_LIVE_ACTIVITIES = booleanPreferencesKey("show_live_activities")
    val SHOW_LIVE_PROGRESS = booleanPreferencesKey("show_live_progress")
    val SHOW_SCREEN_RECORDING = booleanPreferencesKey("show_screen_recording")
    val SHOW_FLASHLIGHT = booleanPreferencesKey("show_flashlight")
    val SHOW_BLUETOOTH = booleanPreferencesKey("show_bluetooth")
    val SHOW_CHARGING = booleanPreferencesKey("show_charging")
    val SHOW_RINGER_MODE = booleanPreferencesKey("show_ringer_mode")
    val SHOW_IN_LANDSCAPE = booleanPreferencesKey("show_in_landscape")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val SPOKEN_ALERTS = booleanPreferencesKey("spoken_alerts")
    val ANIMATION_SPEED_MULTIPLIER = floatPreferencesKey("anim_speed_multiplier")
    val LEGACY_ANIMATION_SPEED = stringPreferencesKey("animation_speed")
    val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
    val PRIORITY_APPS = stringSetPreferencesKey("priority_apps")
    val SHOW_WIFI = booleanPreferencesKey("show_wifi")
    val RESTART_ON_BOOT = booleanPreferencesKey("restart_on_boot")
    val BUBBLE_EFFECT_ENABLED = booleanPreferencesKey("bubble_effect_enabled")
    val DISMISS_MEDIA_ON_PAUSE = booleanPreferencesKey("dismiss_media_on_pause")
    val ALWAYS_SHOW_PILL = booleanPreferencesKey("always_show_pill")
    val SHOW_WEATHER = booleanPreferencesKey("show_weather")
    val SHOW_NETWORK_SPEED = booleanPreferencesKey("show_network_speed")
    val AUTO_DISMISS_NOTIFICATIONS = booleanPreferencesKey("auto_dismiss_notifications")
    val BLURRED_ALBUM_ART_BACKGROUND = booleanPreferencesKey("blurred_album_art_background")
    val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
    val EXPAND_DAMPING = floatPreferencesKey("expand_damping")
    val EXPAND_STIFFNESS = floatPreferencesKey("expand_stiffness")
    val APPEAR_DAMPING = floatPreferencesKey("appear_damping")
    val APPEAR_STIFFNESS = floatPreferencesKey("appear_stiffness")
    val SPLIT_DAMPING = floatPreferencesKey("split_damping")
    val SPLIT_STIFFNESS = floatPreferencesKey("split_stiffness")
    val FEATURE_PRIORITIES = stringPreferencesKey("feature_priorities")
    val ISLAND_STYLE = stringPreferencesKey("island_style")
}

/**
 * B8.6: the one place preferences become [IslandSettings]. Every key is read here and written in
 * [write], and `SettingsRepositoryRoundTripTest` checks each field survives the trip.
 */
internal fun Preferences.toIslandSettings(): IslandSettings {
    val resolvedSpeed = this[PreferencesKeys.ANIMATION_SPEED_MULTIPLIER] ?: run {
        when (this[PreferencesKeys.LEGACY_ANIMATION_SPEED]) {
            "FAST" -> 1.5f
            "SLOW" -> 0.7f
            else -> 0.7f
        }
    }

    return IslandSettings(
        isEnabled = this[PreferencesKeys.IS_ENABLED] ?: true,
        widthDp = this[PreferencesKeys.WIDTH_DP] ?: 180,
        heightDp = this[PreferencesKeys.HEIGHT_DP] ?: 36,
        topOffsetDp = this[PreferencesKeys.TOP_OFFSET_DP] ?: 12,
        horizontalOffsetDp = this[PreferencesKeys.HORIZONTAL_OFFSET_DP] ?: 0,
        isLeftCutout = this[PreferencesKeys.IS_LEFT_CUTOUT] ?: false,
        showOutline = this[PreferencesKeys.SHOW_OUTLINE] ?: false,
        liquidGlassCards = this[PreferencesKeys.LIQUID_GLASS_CARDS] ?: false,
        outlineColorArgb = this[PreferencesKeys.OUTLINE_COLOR_ARGB] ?: 0x33FFFFFF,
        outlineThicknessDp = this[PreferencesKeys.OUTLINE_THICKNESS_DP] ?: 1.5f,
        expandedScale = this[PreferencesKeys.EXPANDED_SCALE] ?: 1.1f,
        hapticsEnabled = this[PreferencesKeys.HAPTICS_ENABLED] ?: true,
        showCalls = this[PreferencesKeys.SHOW_CALLS] ?: true,
        showMedia = this[PreferencesKeys.SHOW_MEDIA] ?: true,
        showNotifications = this[PreferencesKeys.SHOW_NOTIFICATIONS] ?: false,
        showLiveActivities = this[PreferencesKeys.SHOW_LIVE_ACTIVITIES] ?: true,
        showLiveProgress = this[PreferencesKeys.SHOW_LIVE_PROGRESS] ?: true,
        showScreenRecording = this[PreferencesKeys.SHOW_SCREEN_RECORDING] ?: true,
        showFlashlight = this[PreferencesKeys.SHOW_FLASHLIGHT] ?: true,
        showBluetooth = this[PreferencesKeys.SHOW_BLUETOOTH] ?: true,
        showCharging = this[PreferencesKeys.SHOW_CHARGING] ?: true,
        showRingerMode = this[PreferencesKeys.SHOW_RINGER_MODE] ?: true,
        showInLandscape = this[PreferencesKeys.SHOW_IN_LANDSCAPE] ?: false,
        highContrast = this[PreferencesKeys.HIGH_CONTRAST] ?: false,
        spokenAlerts = this[PreferencesKeys.SPOKEN_ALERTS] ?: false,
        animationSpeed = resolvedSpeed,
        blockedPackages = this[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet(),
        priorityApps = this[PreferencesKeys.PRIORITY_APPS] ?: emptySet(),
        showWifi = this[PreferencesKeys.SHOW_WIFI] ?: true,
        restartOnBoot = this[PreferencesKeys.RESTART_ON_BOOT] ?: true,
        bubbleEffectEnabled = this[PreferencesKeys.BUBBLE_EFFECT_ENABLED] ?: true,
        dismissMediaOnPause = this[PreferencesKeys.DISMISS_MEDIA_ON_PAUSE] ?: false,
        alwaysShowPill = this[PreferencesKeys.ALWAYS_SHOW_PILL] ?: false,
        showWeather = this[PreferencesKeys.SHOW_WEATHER] ?: false,
        showNetworkSpeed = this[PreferencesKeys.SHOW_NETWORK_SPEED] ?: false,
        autoDismissNotifications = this[PreferencesKeys.AUTO_DISMISS_NOTIFICATIONS] ?: false,
        blurredAlbumArtBackground = this[PreferencesKeys.BLURRED_ALBUM_ART_BACKGROUND] ?: false,
        hideOnLockScreen = this[PreferencesKeys.HIDE_ON_LOCK_SCREEN] ?: false,
        expandDamping = this[PreferencesKeys.EXPAND_DAMPING] ?: 0.8f,
        expandStiffness = this[PreferencesKeys.EXPAND_STIFFNESS] ?: 380f,
        appearDamping = this[PreferencesKeys.APPEAR_DAMPING] ?: 0.6f,
        appearStiffness = this[PreferencesKeys.APPEAR_STIFFNESS] ?: 280f,
        splitDamping = this[PreferencesKeys.SPLIT_DAMPING] ?: 0.63f,
        splitStiffness = this[PreferencesKeys.SPLIT_STIFFNESS] ?: 320f,
        islandStyle = IslandSettings.normalizeStyle(this[PreferencesKeys.ISLAND_STYLE]),
        featurePriorities = parseFeaturePriorities(this[PreferencesKeys.FEATURE_PRIORITIES])
    )
}

/** B8.6: writes every field of [s]; the counterpart of [toIslandSettings]. */
internal fun MutablePreferences.write(s: IslandSettings) {
    this[PreferencesKeys.IS_ENABLED] = s.isEnabled
    this[PreferencesKeys.WIDTH_DP] = s.widthDp
    this[PreferencesKeys.HEIGHT_DP] = s.heightDp
    this[PreferencesKeys.TOP_OFFSET_DP] = s.topOffsetDp
    this[PreferencesKeys.HORIZONTAL_OFFSET_DP] = s.horizontalOffsetDp
    this[PreferencesKeys.IS_LEFT_CUTOUT] = s.isLeftCutout
    this[PreferencesKeys.SHOW_OUTLINE] = s.showOutline
    this[PreferencesKeys.LIQUID_GLASS_CARDS] = s.liquidGlassCards
    this[PreferencesKeys.OUTLINE_COLOR_ARGB] = s.outlineColorArgb
    this[PreferencesKeys.OUTLINE_THICKNESS_DP] = s.outlineThicknessDp
    this[PreferencesKeys.EXPANDED_SCALE] = s.expandedScale
    this[PreferencesKeys.HAPTICS_ENABLED] = s.hapticsEnabled
    this[PreferencesKeys.SHOW_CALLS] = s.showCalls
    this[PreferencesKeys.SHOW_MEDIA] = s.showMedia
    this[PreferencesKeys.SHOW_NOTIFICATIONS] = s.showNotifications
    this[PreferencesKeys.SHOW_LIVE_ACTIVITIES] = s.showLiveActivities
    this[PreferencesKeys.SHOW_LIVE_PROGRESS] = s.showLiveProgress
    this[PreferencesKeys.SHOW_SCREEN_RECORDING] = s.showScreenRecording
    this[PreferencesKeys.SHOW_FLASHLIGHT] = s.showFlashlight
    this[PreferencesKeys.SHOW_BLUETOOTH] = s.showBluetooth
    this[PreferencesKeys.SHOW_CHARGING] = s.showCharging
    this[PreferencesKeys.SHOW_RINGER_MODE] = s.showRingerMode
    this[PreferencesKeys.SHOW_IN_LANDSCAPE] = s.showInLandscape
    this[PreferencesKeys.HIGH_CONTRAST] = s.highContrast
    this[PreferencesKeys.SPOKEN_ALERTS] = s.spokenAlerts
    this[PreferencesKeys.ANIMATION_SPEED_MULTIPLIER] = s.animationSpeed
    remove(PreferencesKeys.LEGACY_ANIMATION_SPEED)
    this[PreferencesKeys.BLOCKED_PACKAGES] = s.blockedPackages
    this[PreferencesKeys.PRIORITY_APPS] = s.priorityApps
    this[PreferencesKeys.SHOW_WIFI] = s.showWifi
    this[PreferencesKeys.RESTART_ON_BOOT] = s.restartOnBoot
    this[PreferencesKeys.BUBBLE_EFFECT_ENABLED] = s.bubbleEffectEnabled
    this[PreferencesKeys.DISMISS_MEDIA_ON_PAUSE] = s.dismissMediaOnPause
    this[PreferencesKeys.ALWAYS_SHOW_PILL] = s.alwaysShowPill
    this[PreferencesKeys.SHOW_WEATHER] = s.showWeather
    this[PreferencesKeys.SHOW_NETWORK_SPEED] = s.showNetworkSpeed
    this[PreferencesKeys.AUTO_DISMISS_NOTIFICATIONS] = s.autoDismissNotifications
    this[PreferencesKeys.BLURRED_ALBUM_ART_BACKGROUND] = s.blurredAlbumArtBackground
    this[PreferencesKeys.HIDE_ON_LOCK_SCREEN] = s.hideOnLockScreen
    this[PreferencesKeys.EXPAND_DAMPING] = s.expandDamping
    this[PreferencesKeys.EXPAND_STIFFNESS] = s.expandStiffness
    this[PreferencesKeys.APPEAR_DAMPING] = s.appearDamping
    this[PreferencesKeys.APPEAR_STIFFNESS] = s.appearStiffness
    this[PreferencesKeys.SPLIT_DAMPING] = s.splitDamping
    this[PreferencesKeys.SPLIT_STIFFNESS] = s.splitStiffness
    this[PreferencesKeys.FEATURE_PRIORITIES] = s.featurePriorities.joinToString(",")
    this[PreferencesKeys.ISLAND_STYLE] = IslandSettings.normalizeStyle(s.islandStyle)
}

class SettingsRepository(private val context: Context) {

    /** Emits only real changes (B8.6): a write that leaves the settings equal emits nothing. */
    val settingsFlow: Flow<IslandSettings> = context.dataStore.data
        .map { it.toIslandSettings() }
        .distinctUntilChanged()

    suspend fun updateSettings(update: (IslandSettings) -> IslandSettings) {
        context.dataStore.edit { preferences ->
            preferences.write(update(preferences.toIslandSettings()))
        }
    }
}
