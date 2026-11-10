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
    val condition: String = "Có mây rải rác",
    val highC: Int = 25,
    val lowC: Int = 16,
    val humidityPct: Int = 54,
    val windKmh: Int = 14,
    val uvIndex: Int = 3,
    val cityName: String = "Thời tiết địa phương",
    val hourlyForecast: List<HourlyWeather> = listOf(
        HourlyWeather("Hiện tại", 22, "Có mây rải rác"),
        HourlyWeather("+1h", 23, "Nắng"),
        HourlyWeather("+2h", 24, "Nắng"),
        HourlyWeather("+3h", 23, "Có mây rải rác"),
        HourlyWeather("+4h", 20, "Quang đãng")
    )
)

data class NetworkSpeedInfo(
    val downloadSpeedFormatted: String = "0 KB/s",
    val uploadSpeedFormatted: String = "0 KB/s",
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val isWifi: Boolean = true,
    val networkLabel: String = "Wi-Fi",
    val pingMs: Int = 18,
    val sessionTotalMb: Long = 0L
)
