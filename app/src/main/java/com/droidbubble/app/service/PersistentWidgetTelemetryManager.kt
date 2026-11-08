package com.droidbubble.app.service

import android.content.Context
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.FpsInfo
import com.droidbubble.app.model.HourlyWeather
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.model.NetworkSpeedInfo
import com.droidbubble.app.model.WeatherInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class PersistentWidgetTelemetryManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var networkJob: Job? = null
    private var weatherJob: Job? = null

    // Widgets only run while they are enabled AND the screen is on (see applyState)
    private var settings: IslandSettings? = null
    private var isScreenOn = true

    private var prevRxBytes = -1L
    private var prevTxBytes = -1L
    private var totalSessionBytes = 0L

    @Volatile private var lastWeatherFetchMs = 0L

    // Network type comes from a default-network callback instead of a ConnectivityManager query every second
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile private var isWifi = false
    @Volatile private var isCellular = false
    @Volatile private var linkDownKbps = 0

    // FPS via display refresh-rate change events: nothing runs per frame. Requesting every
    // Choreographer frame woke the app 60-120x/s and kept adaptive displays pinned at max Hz.
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private var isFpsRunning = false
    private var maxDisplayHz = 60

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) publishRefreshRate()
        }
    }

    fun start(settings: IslandSettings? = null) {
        if (settings != null) {
            updateSettings(settings)
        }
    }

    fun updateSettings(settings: IslandSettings) {
        this.settings = settings
        applyState()
    }

    /** Pauses every widget while the screen is off; resumes (refreshing stale data) when it turns on. */
    fun setScreenOn(on: Boolean) {
        if (isScreenOn == on) return
        isScreenOn = on
        applyState()
    }

    private fun applyState() {
        val active = settings?.takeIf { isScreenOn }

        // FPS counter was removed from the app; make sure monitoring never runs
        stopFpsMonitoring()

        if (active?.showNetworkSpeed == true) {
            if (networkJob == null) startNetworkSpeedMonitoring()
        } else {
            stopNetworkSpeedMonitoring()
        }

        if (active?.showWeather == true) {
            if (weatherJob == null) startWeatherMonitoring()
        } else {
            weatherJob?.cancel()
            weatherJob = null
        }
    }

    fun stop() {
        stopFpsMonitoring()
        stopNetworkSpeedMonitoring()
        weatherJob?.cancel()
        scope.cancel()
    }

    private fun startFpsMonitoring() {
        if (isFpsRunning) return
        val dm = displayManager ?: return
        isFpsRunning = true
        maxDisplayHz = runCatching {
            dm.getDisplay(Display.DEFAULT_DISPLAY)?.supportedModes?.maxOfOrNull { it.refreshRate }?.roundToInt()
        }.getOrNull()?.coerceIn(60, 240) ?: 60
        dm.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        publishRefreshRate()
    }

    private fun stopFpsMonitoring() {
        if (!isFpsRunning) return
        isFpsRunning = false
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
    }

    private fun publishRefreshRate() {
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val fps = display.refreshRate.roundToInt().coerceIn(10, 240)
        IslandStateManager.updateFps(
            FpsInfo(
                fps = fps,
                refreshRateHz = maxDisplayHz,
                frameTimeMs = 1000f / fps,
                isSmooth = fps >= 60
            )
        )
    }

    private fun startNetworkSpeedMonitoring() {
        networkJob?.cancel()
        registerNetworkCallback()
        networkJob = scope.launch(Dispatchers.IO) {
            val startRx = TrafficStats.getTotalRxBytes()
            val startTx = TrafficStats.getTotalTxBytes()
            // Resuming after a screen-off pause: count the traffic in the session total,
            // but don't report it as a one-second speed spike.
            if (prevRxBytes >= 0 && prevTxBytes >= 0 && startRx >= prevRxBytes && startTx >= prevTxBytes) {
                totalSessionBytes += (startRx - prevRxBytes) + (startTx - prevTxBytes)
            }
            prevRxBytes = startRx
            prevTxBytes = startTx

            while (isActive) {
                delay(1000L)

                val currentRx = TrafficStats.getTotalRxBytes()
                val currentTx = TrafficStats.getTotalTxBytes()

                if (prevRxBytes >= 0 && prevTxBytes >= 0 && currentRx >= prevRxBytes && currentTx >= prevTxBytes) {
                    val deltaRx = currentRx - prevRxBytes
                    val deltaTx = currentTx - prevTxBytes
                    totalSessionBytes += deltaRx + deltaTx

                    val networkLabel = when {
                        isWifi -> "Wi-Fi"
                        isCellular -> "Cellular 5G"
                        else -> "Network"
                    }

                    val info = NetworkSpeedInfo(
                        downloadSpeedFormatted = formatBytesPerSec(deltaRx),
                        uploadSpeedFormatted = formatBytesPerSec(deltaTx),
                        downloadBytesPerSec = deltaRx,
                        uploadBytesPerSec = deltaTx,
                        isWifi = isWifi,
                        networkLabel = networkLabel,
                        linkDownMbps = linkDownKbps / 1000,
                        sessionTotalMb = totalSessionBytes / (1024 * 1024)
                    )

                    // Equal values are dropped by updateNetworkSpeed, so an idle link causes no redraws
                    withContext(Dispatchers.Main) {
                        IslandStateManager.updateNetworkSpeed(info)
                    }
                }

                prevRxBytes = currentRx
                prevTxBytes = currentTx
            }
        }
    }

    private fun stopNetworkSpeedMonitoring() {
        networkJob?.cancel()
        networkJob = null
        networkCallback?.let { cb -> runCatching { connectivityManager?.unregisterNetworkCallback(cb) } }
        networkCallback = null
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = connectivityManager ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                linkDownKbps = caps.linkDownstreamBandwidthKbps
            }

            override fun onLost(network: Network) {
                isWifi = false
                isCellular = false
                linkDownKbps = 0
            }
        }
        runCatching {
            cm.registerDefaultNetworkCallback(cb)
            networkCallback = cb
        }
    }

    private fun formatBytesPerSec(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 10 -> String.format(Locale.US, "%.1f MB/s", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> "${bytes / 1024} KB/s"
            else -> "$bytes B/s"
        }
    }

    private fun startWeatherMonitoring() {
        weatherJob?.cancel()
        weatherJob = scope.launch(Dispatchers.IO) {
            // After a screen-off pause, refetch right away only if the data is actually stale
            val sinceLastFetch = System.currentTimeMillis() - lastWeatherFetchMs
            if (lastWeatherFetchMs > 0L && sinceLastFetch in 0 until WEATHER_REFRESH_MS) {
                delay(WEATHER_REFRESH_MS - sinceLastFetch)
            }
            while (isActive) {
                fetchWeather()
                lastWeatherFetchMs = System.currentTimeMillis()
                delay(WEATHER_REFRESH_MS)
            }
        }
    }

    private companion object {
        const val WEATHER_REFRESH_MS = 20 * 60 * 1000L
    }

    private suspend fun fetchWeather() {
        try {
            val tz = TimeZone.getDefault().id
            val (lat, lon, defaultCity) = approximateCoordsForTimeZone(tz)

            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val tempC = current.getDouble("temperature_2m").roundToInt()
                val humidity = current.optInt("relative_humidity_2m", 54)
                val windKmh = current.optDouble("wind_speed_10m", 12.0).roundToInt()
                val weatherCode = current.optInt("weather_code", 1)

                val daily = json.optJSONObject("daily")
                val highC = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, (tempC + 4).toDouble())?.roundToInt() ?: (tempC + 4)
                val lowC = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, (tempC - 4).toDouble())?.roundToInt() ?: (tempC - 4)

                val condition = mapWeatherCodeToCondition(weatherCode)

                val hourly = listOf(
                    HourlyWeather("Now", tempC, condition),
                    HourlyWeather("+1h", tempC + 1, condition),
                    HourlyWeather("+2h", highC, condition),
                    HourlyWeather("+3h", tempC, condition),
                    HourlyWeather("+4h", lowC + 2, "Clear")
                )

                val weatherInfo = WeatherInfo(
                    temperatureC = tempC,
                    condition = condition,
                    highC = highC,
                    lowC = lowC,
                    humidityPct = humidity,
                    windKmh = windKmh,
                    uvIndex = 3,
                    cityName = defaultCity,
                    hourlyForecast = hourly
                )

                withContext(Dispatchers.Main) {
                    IslandStateManager.updateWeather(weatherInfo)
                }
            } else {
                useFallbackWeather()
            }
        } catch (_: Exception) {
            useFallbackWeather()
        }
    }

    private suspend fun useFallbackWeather() {
        val tz = TimeZone.getDefault().id
        val (_, _, defaultCity) = approximateCoordsForTimeZone(tz)
        withContext(Dispatchers.Main) {
            IslandStateManager.updateWeather(
                WeatherInfo(
                    temperatureC = 22,
                    condition = "Partly Cloudy",
                    highC = 25,
                    lowC = 16,
                    humidityPct = 52,
                    windKmh = 14,
                    uvIndex = 3,
                    cityName = defaultCity
                )
            )
        }
    }

    private fun mapWeatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Partly Cloudy"
        }
    }

    private fun approximateCoordsForTimeZone(tzId: String): Triple<Double, Double, String> {
        val lower = tzId.lowercase(Locale.ROOT)
        return when {
            lower.contains("athens") || lower.contains("greece") -> Triple(37.98, 23.72, "Athens")
            lower.contains("london") -> Triple(51.50, -0.12, "London")
            lower.contains("paris") -> Triple(48.85, 2.35, "Paris")
            lower.contains("berlin") -> Triple(52.52, 13.40, "Berlin")
            lower.contains("new_york") -> Triple(40.71, -74.00, "New York")
            lower.contains("los_angeles") -> Triple(34.05, -118.24, "Los Angeles")
            lower.contains("chicago") -> Triple(41.87, -87.62, "Chicago")
            lower.contains("tokyo") -> Triple(35.67, 139.65, "Tokyo")
            lower.contains("sydney") -> Triple(-33.86, 151.20, "Sydney")
            lower.contains("rome") -> Triple(41.90, 12.49, "Rome")
            lower.contains("madrid") -> Triple(40.41, -3.70, "Madrid")
            lower.contains("toronto") -> Triple(43.65, -79.38, "Toronto")
            lower.contains("europe") -> Triple(48.85, 2.35, "Central Europe")
            lower.contains("america") -> Triple(40.71, -74.00, "Eastern Time")
            lower.contains("asia") -> Triple(35.67, 139.65, "Tokyo")
            else -> Triple(37.98, 23.72, "Local Weather")
        }
    }
}
