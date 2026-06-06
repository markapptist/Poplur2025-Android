package com.apptists.poplur2025.weather

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

enum class WeatherType {
    CLEAR, RAIN, SNOW, CLOUDY, SMOKE, THUNDERSTORM, UNKNOWN
}

data class WeatherInfo(
    val type: WeatherType,
    val temperature: Double,
    val description: String
)

class WeatherService(private val apiKey: String) {

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            // Using OpenWeatherMap One Call API or Current Weather API
            // For simplicity, using Current Weather API
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            val weatherArray = json.getJSONArray("weather")
            val mainWeather = weatherArray.getJSONObject(0)
            val conditionId = mainWeather.getInt("id")
            val description = mainWeather.getString("description")
            val main = json.getJSONObject("main")
            val temp = main.getDouble("temp")

            val type = when (conditionId) {
                in 200..232 -> WeatherType.THUNDERSTORM
                in 300..321 -> WeatherType.RAIN
                in 500..531 -> WeatherType.RAIN
                in 600..622 -> WeatherType.SNOW
                in 701..781 -> if (conditionId == 711) WeatherType.SMOKE else WeatherType.CLOUDY
                800 -> WeatherType.CLEAR
                in 801..804 -> WeatherType.CLOUDY
                else -> WeatherType.UNKNOWN
            }

            WeatherInfo(type, temp, description)
        } catch (e: Exception) {
            Log.e("WeatherService", "Error fetching weather", e)
            WeatherInfo(WeatherType.UNKNOWN, 0.0, "Error")
        }
    }
}
