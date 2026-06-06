package com.apptists.poplur2025.data.weather

data class WeatherBadge(
    val icon: String,
    val label: String,
    val temperatureLabel: String? = null
) {
    companion object {
        val Loading = WeatherBadge("...", "Weather")
        val Unavailable = WeatherBadge("?", "Weather")
    }
}
