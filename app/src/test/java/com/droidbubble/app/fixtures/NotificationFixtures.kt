package com.droidbubble.app.fixtures

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Real-world notifications written as small JSON files, turned into [StatusBarNotification]s on
 * the JVM (Robolectric). Files live in `src/test/resources/fixtures/notifications/<group>/<name>.json`.
 *
 * Format (every field but `pkg` and `source` is optional):
 * ```
 * { "pkg": "com.google.android.deskclock", "appLabel": "Clock", "locale": "de-DE",
 *   "category": "stopwatch", "channelId": "Timers", "flags": ["ONGOING_EVENT", "FOREGROUND_SERVICE"],
 *   "template": "android.app.Notification$DecoratedCustomViewStyle", "when": -31000, "postTimeOffset": 0,
 *   "importance": 3, "extras": { "android.title": "Timer", "android.text": "Pausiert" },
 *   "actions": [ { "title": "Fortsetzen", "semanticAction": 0, "remoteInput": false, "choices": [] } ],
 *   "source": "Pixel 8, Android 16, dumpsys notification --noredact, 2026-11" }
 * ```
 * - `when` and `postTimeOffset` are milliseconds relative to [NOW_MS], a fixed test clock.
 * - `extras` overlay the builder's extras. JSON booleans become Boolean, strings String, whole
 *   numbers Int (Long if they don't fit), other numbers Double. Suffix a key with `@long` to force
 *   a Long (for example `"android.chronometerBase@long": 1800000000000`).
 * - `importance` is a `NotificationManager.IMPORTANCE_*` value. The listener reads it from the
 *   ranking, which the framework owns; tests feed it through the listener's test seam (S5.3).
 *
 * **Capture recipe.** On a device: `adb shell dumpsys notification --noredact`. Copy the fields
 * above by hand from the entry you want. **Replace every real name, phone number and message
 * with neutral text** ("Sam", "Hello there"). Hand-written fixtures say `"source": "synthetic"`.
 */
object NotificationFixtures {

    /** The fixed "now" every fixture's times are relative to. */
    const val NOW_MS = 1_800_000_000_000L

    private const val ROOT = "fixtures/notifications"

    data class Fixture(
        /** `<group>/<name>`, for example `clock/gclock_timer_running_en`. */
        val name: String,
        val sbn: StatusBarNotification,
        val appLabel: String?,
        val locale: String?,
        val importance: Int?,
        val source: String,
    ) {
        val notification: Notification get() = sbn.notification
        val key: String get() = sbn.key
    }

    fun load(context: Context, name: String): Fixture {
        val stream = javaClass.classLoader!!.getResourceAsStream("$ROOT/$name.json")
            ?: error("No fixture $ROOT/$name.json")
        val json = JSONObject(stream.bufferedReader().use { it.readText() })
        return build(context, name, json)
    }

    /** Every fixture name under the resource root, sorted, as `<group>/<name>`. */
    fun allNames(): List<String> {
        val url = javaClass.classLoader!!.getResource(ROOT) ?: return emptyList()
        val root = File(url.toURI())
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .map { it.relativeTo(root).invariantSeparatorsPath.removeSuffix(".json") }
            .sorted()
            .toList()
    }

    fun build(context: Context, name: String, json: JSONObject): Fixture {
        val pkg = json.getString("pkg")
        val channelId = json.optString("channelId", "default")
        val builder = Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
        json.optStringOrNull("category")?.let { builder.setCategory(it) }
        if (json.has("when")) {
            builder.setWhen(NOW_MS + json.getLong("when"))
            builder.setShowWhen(true)
        }

        val extrasJson = json.optJSONObject("extras")
        extrasJson?.optStringOrNull("android.title")?.let { builder.setContentTitle(it) }
        extrasJson?.optStringOrNull("android.text")?.let { builder.setContentText(it) }

        json.optJSONArray("actions")?.let { actions ->
            for (i in 0 until actions.length()) builder.addAction(action(context, actions.getJSONObject(i)))
        }

        val notification = builder.build()
        extrasJson?.let { overlay(notification.extras, it) }
        json.optStringOrNull("template")?.let { notification.extras.putString(Notification.EXTRA_TEMPLATE, it) }
        json.optJSONArray("flags")?.let { flags ->
            for (i in 0 until flags.length()) notification.flags = notification.flags or flag(flags.getString(i))
        }

        val postTime = NOW_MS + json.optLong("postTimeOffset", 0L)
        // The public (deprecated) constructor; its score argument is unused by the framework
        @Suppress("DEPRECATION")
        val sbn = StatusBarNotification(
            pkg, pkg, name.hashCode(), name, Process.myUid(), 0, 0, notification,
            Process.myUserHandle(), postTime
        )
        return Fixture(
            name = name,
            sbn = sbn,
            appLabel = json.optStringOrNull("appLabel"),
            locale = json.optStringOrNull("locale"),
            importance = if (json.has("importance")) json.getInt("importance") else null,
            source = json.optStringOrNull("source") ?: error("Fixture $name has no \"source\""),
        )
    }

    private fun action(context: Context, a: JSONObject): Notification.Action {
        val intent = android.app.PendingIntent.getBroadcast(
            context, a.getString("title").hashCode(), android.content.Intent("fixture.${a.getString("title")}"),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val b = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_play),
            a.getString("title"),
            intent
        )
        if (a.has("semanticAction")) b.setSemanticAction(a.getInt("semanticAction"))
        if (a.optBoolean("remoteInput", false)) {
            val choices = a.optJSONArray("choices")?.let { arr -> Array<CharSequence>(arr.length()) { arr.getString(it) } }
            b.addRemoteInput(RemoteInput.Builder("reply").setChoices(choices).build())
        }
        return b.build()
    }

    private fun overlay(extras: Bundle, json: JSONObject) {
        for (rawKey in json.keys()) {
            val forceLong = rawKey.endsWith("@long")
            val key = rawKey.removeSuffix("@long")
            when (val v = json.get(rawKey)) {
                is Boolean -> extras.putBoolean(key, v)
                is String -> extras.putString(key, v)
                is Int -> if (forceLong) extras.putLong(key, v.toLong()) else extras.putInt(key, v)
                is Long -> if (forceLong || v !in Int.MIN_VALUE..Int.MAX_VALUE) extras.putLong(key, v) else extras.putInt(key, v.toInt())
                is Number -> if (forceLong) extras.putLong(key, v.toLong()) else extras.putDouble(key, v.toDouble())
                is JSONArray -> extras.putStringArray(key, Array(v.length()) { v.getString(it) })
                JSONObject.NULL -> extras.remove(key)
                else -> error("Unsupported extra $rawKey: ${v::class.java}")
            }
        }
    }

    private fun flag(name: String): Int = when (name) {
        "ONGOING_EVENT" -> Notification.FLAG_ONGOING_EVENT
        "FOREGROUND_SERVICE" -> Notification.FLAG_FOREGROUND_SERVICE
        "NO_CLEAR" -> Notification.FLAG_NO_CLEAR
        "AUTO_CANCEL" -> Notification.FLAG_AUTO_CANCEL
        "INSISTENT" -> Notification.FLAG_INSISTENT
        "ONLY_ALERT_ONCE" -> Notification.FLAG_ONLY_ALERT_ONCE
        "GROUP_SUMMARY" -> Notification.FLAG_GROUP_SUMMARY
        "LOCAL_ONLY" -> Notification.FLAG_LOCAL_ONLY
        "BUBBLE" -> Notification.FLAG_BUBBLE
        else -> error("Unknown notification flag $name")
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
}
