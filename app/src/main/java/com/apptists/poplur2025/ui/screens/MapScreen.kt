package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.weather.WeatherBadge
import com.apptists.poplur2025.location.LocationProvider
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Image
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.nio.ByteBuffer
import java.net.URL
import java.util.Locale
import kotlin.math.abs

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(isVisible: Boolean = true) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    // Start on the full globe, then fly into the user's location.
    val globeView = Point.fromLngLat(-98.0, 18.0)
    val fallbackLocation = Point.fromLngLat(-73.985130, 40.758896)
    var selectedCoordinate by remember { mutableStateOf<Point?>(null) }
    var activeCityName by remember { mutableStateOf("Your Location") }
    var activeRadius by remember { mutableIntStateOf(DEFAULT_RADIUS_KM) }
    var activeWeatherPoint by remember { mutableStateOf<Point?>(fallbackLocation) }
    var activeWeather by remember { mutableStateOf(WeatherBadge.Loading) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var idleGeneration by remember { mutableIntStateOf(0) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { }
    val mapView = remember { MapView(context) }
    val locationProvider = remember { LocationProvider(context) }
    val postFeatures = remember {
        DemoPostStore.posts.mapNotNull { post ->
            val lat = post.latitude
            val lng = post.longitude
            if (lat != null && lng != null) Feature.fromGeometry(Point.fromLngLat(lng, lat)) else null
        }
    }
    val landmarkFeatures = remember {
        FeatureCollection.fromFeatures(TourStops.map { Feature.fromGeometry(it.coordinate) })
    }
    var mapReady by remember { mutableStateOf(false) }
    var hasZoomedToLocation by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible && !hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(mapReady, hasLocationPermission) {
        if (mapReady) {
            mapView.location.updateSettings {
                enabled = hasLocationPermission
                pulsingEnabled = true
            }
            mapView.mapboxMap.addOnMapClickListener { point ->
                selectedCoordinate = point
                activeCityName = "Selected Location"
                activeRadius = SELECTED_LOCATION_RADIUS_KM
                activeWeatherPoint = point
                idleGeneration++
                mapView.mapboxMap.flyTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(17.6)
                        .pitch(66.0)
                        .bearing(28.0)
                        .build(),
                    mapAnimationOptions { duration(1600) }
                )
                true
            }
        }
    }

    LaunchedEffect(activeWeatherPoint) {
        val point = activeWeatherPoint
        if (point == null) {
            activeWeather = WeatherBadge("🌎", "World")
            return@LaunchedEffect
        }
        activeWeather = WeatherBadge.Loading
        activeWeather = fetchCurrentWeather(point).getOrElse {
            fallbackWeatherBadge(point)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationProvider.lastLocation { lat, lng ->
                val finalLat = lat ?: fallbackLocation.latitude()
                val finalLng = lng ?: fallbackLocation.longitude()
                userLocation = Point.fromLngLat(finalLng, finalLat)
            }
        } else {
            userLocation = fallbackLocation
        }
    }

    LaunchedEffect(mapReady, userLocation) {
        val location = userLocation ?: return@LaunchedEffect
        if (mapReady) {
            mapView.mapboxMap.getStyle { style ->
                style.getSourceAs<GeoJsonSource>(CURRENT_PIN_SOURCE_ID)?.geometry(location)
            }
        }
    }

    LaunchedEffect(isVisible, mapReady, userLocation) {
        val location = userLocation
        if (isVisible && mapReady && location != null && !hasZoomedToLocation) {
            activeCityName = "Your Location"
            activeRadius = DEFAULT_RADIUS_KM
            activeWeatherPoint = location
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(globeView)
                    .zoom(0.45)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build()
            )
            delay(1150)
            mapView.mapboxMap.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(7.0)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build(),
                mapAnimationOptions { duration(1900) }
            )
            delay(2050)
            mapView.mapboxMap.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(16.9)
                    .pitch(66.0)
                    .bearing(28.0)
                    .build(),
                mapAnimationOptions { duration(3600) }
            )
            hasZoomedToLocation = true
        }
    }

    LaunchedEffect(isVisible, mapReady, idleGeneration) {
        if (!isVisible || !mapReady) return@LaunchedEffect
        delay(IDLE_TOUR_DELAY_MS)
        while (isVisible && mapReady) {
            TourStops.forEach { stop ->
                activeCityName = "World"
                activeRadius = EARTH_RADIUS_KM
                activeWeatherPoint = null
                mapView.mapboxMap.flyTo(
                    CameraOptions.Builder()
                        .center(globeView)
                        .zoom(0.55)
                        .pitch(0.0)
                        .bearing(0.0)
                        .build(),
                    mapAnimationOptions { duration(2600) }
                )
                delay(3300)
                activeCityName = stop.cityName
                activeRadius = stop.radiusKm
                activeWeatherPoint = stop.coordinate
                mapView.mapboxMap.flyTo(
                    CameraOptions.Builder()
                        .center(stop.coordinate)
                        .zoom(stop.zoom)
                        .pitch(66.0)
                        .bearing(stop.bearing)
                        .build(),
                    mapAnimationOptions { duration(4200) }
                )
                delay(TOUR_STOP_DWELL_MS)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                mapView.apply {
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(globeView)
                            .zoom(0.45)
                            .pitch(0.0)
                            .bearing(0.0)
                            .build()
                    )
                    mapboxMap.loadStyle(
                        style(Style.STANDARD) {
                            +atmosphere { }
                            +projection(ProjectionName.GLOBE)
                            +geoJsonSource(POST_SOURCE_ID) {
                                featureCollection(FeatureCollection.fromFeatures(postFeatures))
                            }
                            +geoJsonSource(CURRENT_PIN_SOURCE_ID) {
                                geometry(fallbackLocation)
                            }
                            +geoJsonSource(LANDMARK_PIN_SOURCE_ID) {
                                featureCollection(landmarkFeatures)
                            }
                            +circleLayer(POST_LAYER_ID, POST_SOURCE_ID) {
                                circleRadius(18.0)
                                circleColor("#ff2d8a")
                                circleOpacity(0.46)
                                circleStrokeColor("#ffffff")
                                circleStrokeOpacity(0.9)
                                circleStrokeWidth(2.5)
                                circleEmissiveStrength(1.0)
                            }
                            +symbolLayer(LANDMARK_PIN_LAYER_ID, LANDMARK_PIN_SOURCE_ID) {
                                iconImage(PINK_PIN_IMAGE_ID)
                                iconAnchor(IconAnchor.BOTTOM)
                                iconAllowOverlap(true)
                                iconIgnorePlacement(true)
                                iconSize(0.28)
                                iconOffset(listOf(0.0, -4.0))
                            }
                            +symbolLayer(CURRENT_PIN_LAYER_ID, CURRENT_PIN_SOURCE_ID) {
                                iconImage(BLUE_PIN_IMAGE_ID)
                                iconAnchor(IconAnchor.BOTTOM)
                                iconAllowOverlap(true)
                                iconIgnorePlacement(true)
                                iconSize(0.32)
                                iconOffset(listOf(0.0, -4.0))
                            }
                        }
                    ) { style ->
                        style.addStyleImage(BLUE_PIN_IMAGE_ID, 1.0f, createGdxStylePinImage(BLUE_PIN_THEME), false, emptyList(), emptyList(), null)
                        style.addStyleImage(PINK_PIN_IMAGE_ID, 1.0f, createGdxStylePinImage(PINK_PIN_THEME), false, emptyList(), emptyList(), null)
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "show3dObjects", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPointOfInterestLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showRoadLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPlaceLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "lightPreset", Value.valueOf("dusk"))
                        mapReady = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize().zIndex(0f)
        )

        WeatherParticleOverlay(
            weather = activeWeather,
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )

        Box(Modifier.fillMaxWidth().height(190.dp).zIndex(2f).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart).zIndex(2f)) {
            Text(activeCityName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tap the 3D map to choose exactly where a post should live.", color = Color.White.copy(alpha = .9f), fontSize = 16.sp)
        }
        Surface(Modifier.padding(end = 16.dp).align(Alignment.CenterEnd).zIndex(2f), color = Color.Black.copy(alpha = .42f), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(activeWeather.icon, fontSize = 36.sp)
                Text(activeWeather.label, color = Color.White, fontWeight = FontWeight.Bold)
                activeWeather.temperatureLabel?.let { temperature ->
                    Text(temperature, color = Color.White.copy(alpha = .78f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(bottom = 80.dp, start = 20.dp).align(Alignment.BottomStart).zIndex(2f)) {
            Text("476", color = Color(0xFFFF3B30), fontSize = 42.sp, fontWeight = FontWeight.Black)
            HorizontalDivider(color = Color.White.copy(alpha = .5f), thickness = 1.dp, modifier = Modifier.width(100.dp).padding(vertical = 8.dp))
            Text(DemoPostStore.posts.size.toString(), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text("posts", color = Color.White, fontSize = 18.sp)
        }
        selectedCoordinate?.let { point ->
            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 158.dp, start = 20.dp, end = 20.dp).zIndex(2f), color = Color.Black.copy(alpha = .72f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Post here", color = Color.White, fontWeight = FontWeight.Black)
                    Text("${"%.5f".format(point.latitude())}, ${"%.5f".format(point.longitude())}", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Button(onClick = { mediaPicker.launch("image/*") }, modifier = Modifier.padding(top = 8.dp)) { Text("Choose media") }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 60.dp).size(80.dp).align(Alignment.BottomCenter).zIndex(2f))
        RadiusDisplay(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd).zIndex(2f), radius = activeRadius)
    }
}

private const val POST_SOURCE_ID = "poplur-posts"
private const val POST_LAYER_ID = "poplur-post-markers"
private const val CURRENT_PIN_SOURCE_ID = "poplur-current-location-pin-source"
private const val CURRENT_PIN_LAYER_ID = "poplur-current-location-pin-layer"
private const val LANDMARK_PIN_SOURCE_ID = "poplur-landmark-pin-source"
private const val LANDMARK_PIN_LAYER_ID = "poplur-landmark-pin-layer"
private const val BLUE_PIN_IMAGE_ID = "poplur-gdx-blue-location-pin"
private const val PINK_PIN_IMAGE_ID = "poplur-gdx-pink-landmark-pin"
private const val MAPBOX_BASEMAP_IMPORT_ID = "basemap"
private const val IDLE_TOUR_DELAY_MS = 30_000L
private const val TOUR_STOP_DWELL_MS = 30_000L
private const val DEFAULT_RADIUS_KM = 5_000
private const val SELECTED_LOCATION_RADIUS_KM = 850
private const val EARTH_RADIUS_KM = 6_371
private const val WEATHER_TIMEOUT_MS = 5_000

private data class TourStop(
    val cityName: String,
    val coordinate: Point,
    val bearing: Double,
    val radiusKm: Int,
    val zoom: Double = 16.2
)

private val TourStops = listOf(
    TourStop("Paris", Point.fromLngLat(2.2945, 48.8584), 28.0, 105),
    TourStop("New York", Point.fromLngLat(-73.9857, 40.7484), -24.0, 132),
    TourStop("Tokyo", Point.fromLngLat(139.7454, 35.6586), 34.0, 148),
    TourStop("London", Point.fromLngLat(-0.1246, 51.5007), -32.0, 118),
    TourStop("Dubai", Point.fromLngLat(55.2744, 25.1972), 18.0, 96),
    TourStop("Toronto", Point.fromLngLat(-79.3871, 43.6426), 24.0, 84),
    TourStop("Sydney", Point.fromLngLat(151.2153, -33.8568), -28.0, 110)
)

private data class PinTheme(
    val base: Int,
    val mid: Int,
    val highlight: Int,
    val glow: Int
)

private val BLUE_PIN_THEME = PinTheme(
    base = AndroidColor.rgb(38, 128, 255),
    mid = AndroidColor.rgb(96, 231, 255),
    highlight = AndroidColor.WHITE,
    glow = AndroidColor.argb(110, 78, 220, 255)
)

private val PINK_PIN_THEME = PinTheme(
    base = AndroidColor.rgb(186, 44, 255),
    mid = AndroidColor.rgb(255, 74, 221),
    highlight = AndroidColor.WHITE,
    glow = AndroidColor.argb(118, 255, 70, 216)
)

private fun createGdxStylePinImage(theme: PinTheme): Image {
    val bitmap = createGdxStylePinBitmap(theme)
    return bitmap.toMapboxImage()
}

private fun createGdxStylePinBitmap(theme: PinTheme): Bitmap {
    val size = 168
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.shader = RadialGradient(
        size / 2f,
        size * 0.58f,
        size * 0.42f,
        intArrayOf(theme.glow, AndroidColor.TRANSPARENT),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(size / 2f, size * 0.58f, size * 0.42f, paint)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 10f
    paint.color = theme.glow
    canvas.drawCircle(size / 2f, size * 0.58f, 44f, paint)
    paint.strokeWidth = 5f
    paint.alpha = 150
    canvas.drawCircle(size / 2f, size * 0.58f, 65f, paint)
    paint.alpha = 255

    paint.style = Paint.Style.FILL
    val spire = Path().apply {
        moveTo(size / 2f, 10f)
        lineTo(size * 0.68f, 61f)
        lineTo(size * 0.56f, 128f)
        lineTo(size / 2f, 155f)
        lineTo(size * 0.44f, 128f)
        lineTo(size * 0.32f, 61f)
        close()
    }
    paint.shader = LinearGradient(
        size * 0.32f,
        18f,
        size * 0.68f,
        146f,
        intArrayOf(theme.highlight, theme.mid, theme.base),
        floatArrayOf(0f, 0.46f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(spire, paint)

    paint.shader = LinearGradient(
        size * 0.34f,
        22f,
        size * 0.54f,
        128f,
        intArrayOf(AndroidColor.argb(210, 255, 255, 255), AndroidColor.argb(40, 255, 255, 255)),
        null,
        Shader.TileMode.CLAMP
    )
    val leftFacet = Path().apply {
        moveTo(size / 2f, 12f)
        lineTo(size * 0.44f, 129f)
        lineTo(size * 0.33f, 61f)
        close()
    }
    canvas.drawPath(leftFacet, paint)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = AndroidColor.argb(210, 255, 255, 255)
    canvas.drawPath(spire, paint)

    return bitmap
}

private fun Bitmap.toMapboxImage(): Image {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val buffer = ByteBuffer.allocateDirect(pixels.size * 4)
    pixels.forEach { pixel ->
        buffer.put(AndroidColor.red(pixel).toByte())
        buffer.put(AndroidColor.green(pixel).toByte())
        buffer.put(AndroidColor.blue(pixel).toByte())
        buffer.put(AndroidColor.alpha(pixel).toByte())
    }
    buffer.flip()
    return Image(width, height, DataRef(buffer))
}

private suspend fun fetchCurrentWeather(point: Point): Result<WeatherBadge> = runCatching {
    withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${point.latitude()}" +
                "&longitude=${point.longitude()}" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=fahrenheit" +
                "&timezone=auto"
        )
        val connection = url.openConnection().apply {
            connectTimeout = WEATHER_TIMEOUT_MS
            readTimeout = WEATHER_TIMEOUT_MS
        }
        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val current = JSONObject(response).getJSONObject("current")
        val code = current.getInt("weather_code")
        val temperature = current.getDouble("temperature_2m")
        weatherBadgeForCode(code, temperature)
    }
}

private fun weatherBadgeForCode(code: Int, temperature: Double): WeatherBadge {
    val temperatureLabel = "${String.format(Locale.US, "%.0f", temperature)}°F"
    val condition = when (code) {
        0 -> "Clear" to "☀️"
        1 -> "Mostly Clear" to "🌤️"
        2 -> "Partly Cloudy" to "⛅"
        3 -> "Cloudy" to "☁️"
        45, 48 -> "Fog" to "🌫️"
        51, 53, 55, 56, 57 -> "Drizzle" to "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain" to "🌧️"
        71, 73, 75, 77, 85, 86 -> "Snow" to "❄️"
        95, 96, 99 -> "Storm" to "⛈️"
        else -> "Weather" to "☁️"
    }
    return WeatherBadge(icon = condition.second, label = condition.first, temperatureLabel = temperatureLabel)
}

private fun fallbackWeatherBadge(point: Point): WeatherBadge {
    val seed = abs((point.latitude() * 17 + point.longitude() * 11).toInt())
    val temperature = 54 + (seed % 34)
    val condition = when (seed % 5) {
        0 -> "Clear" to "☀️"
        1 -> "Partly Cloudy" to "⛅"
        2 -> "Cloudy" to "☁️"
        3 -> "Rain" to "🌧️"
        else -> "Mostly Clear" to "🌤️"
    }
    return WeatherBadge(condition.second, condition.first, "${temperature}°F")
}

@Composable
private fun WeatherParticleOverlay(
    weather: WeatherBadge,
    modifier: Modifier = Modifier
) {
    val label = weather.label
    if (
        label.contains("Weather", ignoreCase = true) ||
        label.contains("World", ignoreCase = true) ||
        label.contains("Clear", ignoreCase = true) && !label.contains("Cloudy", ignoreCase = true)
    ) {
        return
    }

    val transition = rememberInfiniteTransition(label = "map-weather-particles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "map-weather-phase"
    )

    Canvas(modifier = modifier) {
        val drawTop = 0f
        val drawHeight = size.height.coerceAtLeast(1f)

        when {
            label.contains("Rain", ignoreCase = true) || label.contains("Drizzle", ignoreCase = true) -> {
                repeat(125) { index ->
                    val lane = ((index * 37) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 2.1f + index * 47f) % (drawHeight + 90f)) - 45f
                    val x = lane * size.width + ((index % 7) - 3) * 5f
                    drawLine(
                        color = Color(0x99A8C7FF),
                        start = Offset(x, y),
                        end = Offset(x - 8f, y + 34f),
                        strokeWidth = 2.4f
                    )
                }
            }
            label.contains("Snow", ignoreCase = true) -> {
                repeat(95) { index ->
                    val lane = ((index * 41) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 1.05f + index * 39f) % (drawHeight + 70f)) - 35f
                    val x = lane * size.width + kotlin.math.sin((phase * 6.28f + index) * 0.7f) * 18f
                    drawCircle(
                        color = Color(0xCCFFFFFF),
                        radius = 2.4f + (index % 3),
                        center = Offset(x, y)
                    )
                }
            }
            label.contains("Storm", ignoreCase = true) -> {
                repeat(140) { index ->
                    val lane = ((index * 31) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 2.35f + index * 43f) % (drawHeight + 90f)) - 45f
                    val x = lane * size.width
                    drawLine(
                        color = Color(0xAAB5CBFF),
                        start = Offset(x, y),
                        end = Offset(x - 10f, y + 38f),
                        strokeWidth = 2.8f
                    )
                }
                if (phase < 0.08f) {
                    drawRect(Color.White.copy(alpha = 0.18f * (1f - phase / 0.08f)))
                }
            }
            label.contains("Cloudy", ignoreCase = true) || label.contains("Fog", ignoreCase = true) -> {
                repeat(95) { index ->
                    val lane = ((index * 29) % 100) / 100f
                    val drift = (phase * size.width * 0.62f + index * 29f) % (size.width + 220f) - 110f
                    val y = drawTop + lane * drawHeight
                    val x = drift
                    drawLine(
                        color = Color(0x38DDEBFF),
                        start = Offset(x, y),
                        end = Offset(x + 92f + (index % 5) * 16f, y + ((index % 3) - 1) * 1.5f),
                        strokeWidth = 2.8f
                    )
                    if (index % 3 == 0) {
                        drawCircle(
                            color = Color(0x44EEF6FF),
                            radius = 4.2f + (index % 4),
                            center = Offset(x + 36f, y)
                        )
                    }
                    if (index % 9 == 0) {
                        drawCircle(
                            color = Color(0x18FFFFFF),
                            radius = 18f + (index % 5) * 3f,
                            center = Offset(x + 58f, y + ((index % 7) - 3) * 6f)
                        )
                    }
                }
            }
        }
    }
}
