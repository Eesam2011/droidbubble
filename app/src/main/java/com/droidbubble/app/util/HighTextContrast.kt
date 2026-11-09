package com.droidbubble.app.util

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Android's "High contrast text" accessibility setting (a hidden secure key). */
const val HIGH_TEXT_CONTRAST_KEY = "high_text_contrast_enabled"

fun readHighTextContrast(resolver: ContentResolver): Boolean =
    runCatching { Settings.Secure.getInt(resolver, HIGH_TEXT_CONTRAST_KEY, 0) == 1 }.getOrDefault(false)

/**
 * B5.3: Android's "High contrast text" as it changes. Emits the current value first, then each
 * change, so turning it on or off in Accessibility settings restyles the island at once.
 */
fun highTextContrastFlow(resolver: ContentResolver): Flow<Boolean> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            trySend(readHighTextContrast(resolver))
        }
    }
    trySend(readHighTextContrast(resolver))
    runCatching { resolver.registerContentObserver(Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST_KEY), false, observer) }
    awaitClose { runCatching { resolver.unregisterContentObserver(observer) } }
}.distinctUntilChanged()
