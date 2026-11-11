package com.droidbubble.app.island.motion

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.droidbubble.app.island.AudioPowerPolicy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Which motion tier the island runs at (M5.1):
 * - [MotionTier.REDUCED]: the in-app "Reduce motion" switch, or the system animator scale below
 *   1 (0 is "Remove animations"; 05 R11 also counts the slower-than-normal scales as a wish for
 *   less motion)
 * - [MotionTier.EFFICIENT]: Battery Saver
 * - otherwise [MotionTier.FULL]
 */
fun resolveMotionTier(reduceMotionSetting: Boolean, animatorScale: Float, batterySaver: Boolean): MotionTier = when {
    reduceMotionSetting || animatorScale < 1f -> MotionTier.REDUCED
    batterySaver -> MotionTier.EFFICIENT
    else -> MotionTier.FULL
}

fun readAnimatorScale(resolver: ContentResolver): Float =
    runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)

/** The system animator duration scale ("Animator duration scale" in developer options) as it changes. */
fun animatorScaleFlow(resolver: ContentResolver): Flow<Float> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(readAnimatorScale(resolver))
        }
    }
    trySend(readAnimatorScale(resolver))
    runCatching {
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
    }
    awaitClose { runCatching { resolver.unregisterContentObserver(observer) } }
}.distinctUntilChanged()

/** The motion tier, live: it follows the switch, the animator scale and Battery Saver without a restart. */
@Composable
fun rememberMotionTier(reduceMotionSetting: Boolean): MotionTier {
    val resolver = LocalContext.current.contentResolver
    val scaleFlow = remember(resolver) { animatorScaleFlow(resolver) }
    val animatorScale by scaleFlow.collectAsState(initial = remember(resolver) { readAnimatorScale(resolver) })
    // Compose state: Battery Saver changes recompose this
    return resolveMotionTier(reduceMotionSetting, animatorScale, AudioPowerPolicy.lowPower)
}
