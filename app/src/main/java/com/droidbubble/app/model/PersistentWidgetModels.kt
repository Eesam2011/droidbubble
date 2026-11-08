package com.droidbubble.app.model

data class FpsInfo(
    val fps: Int = 60,
    val refreshRateHz: Int = 120,
    val frameTimeMs: Float = 16.6f,
    val isSmooth: Boolean = true
)

data class HourlyWeather(
    val timeLabel: String,
    val tempC: Int,
    val condition: String
)

data class WeatherInfo(
    val temperatureC: Int = 22,
    val condition: String = "Partly Cloudy",
    val highC: Int = 25,
    val lowC: Int = 16,
    val humidityPct: Int = 54,
    val windKmh: Int = 14,
    val uvIndex: Int = 3,
    val cityName: String = "Local Weather",
    val hourlyForecast: List<HourlyWeather> = listOf(
        HourlyWeather("Now", 22, "Partly Cloudy"),
        HourlyWeather("+1h", 23, "Sunny"),
        HourlyWeather("+2h", 24, "Sunny"),
        HourlyWeather("+3h", 23, "Partly Cloudy"),
        HourlyWeather("+4h", 20, "Clear")
    )
)

data class NetworkSpeedInfo(
    val downloadSpeedFormatted: String = "0 KB/s",
    val uploadSpeedFormatted: String = "0 KB/s",
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val isWifi: Boolean = true,
    val networkLabel: String = "Wi-Fi",
    val linkDownMbps: Int = 0, // estimated link bandwidth from the system; 0 = unknown
    val sessionTotalMb: Long = 0L
)
