package com.droidbubble.app.service

import android.content.BroadcastReceiver
import android.content.Context

/**
 * Release build: no debug commands (B1.1). The debug source set has the real DebugCommands; this
 * one keeps the ACTION_* strings and their receiver out of the APK.
 */
object DebugCommands {
    fun register(context: Context): BroadcastReceiver? = null
    fun enableStrictMode() {}
    fun unregister(context: Context, receiver: BroadcastReceiver?) {}
}
