package com.apptists.poplur2025.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptists.poplur2025.BuildConfig
import com.apptists.poplur2025.data.mapillary.MapillaryError
import com.apptists.poplur2025.data.mapillary.MapillaryImageProvider
import com.apptists.poplur2025.data.mapillary.MapillaryService
import com.mapbox.geojson.Point
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val mapillaryService: MapillaryImageProvider = MapillaryService(BuildConfig.MAPILLARY_ACCESS_TOKEN)
) : ViewModel() {
    private val _centerOnUserTrigger = MutableSharedFlow<Unit>(replay = 0)
    val centerOnUserTrigger = _centerOnUserTrigger.asSharedFlow()

    private val _postCoordinateTrigger = MutableSharedFlow<Point>(replay = 0)
    val postCoordinateTrigger = _postCoordinateTrigger.asSharedFlow()

    private val _openSearchTrigger = MutableSharedFlow<Unit>(replay = 0)
    val openSearchTrigger = _openSearchTrigger.asSharedFlow()

    private val _postToMapTrigger = MutableSharedFlow<String?>(replay = 0)
    val postToMapTrigger = _postToMapTrigger.asSharedFlow()

    private val _postMultipleTrigger = MutableSharedFlow<Unit>(replay = 0)
    val postMultipleTrigger = _postMultipleTrigger.asSharedFlow()

    private val _addMessageTrigger = MutableSharedFlow<Unit>(replay = 0)
    val addMessageTrigger = _addMessageTrigger.asSharedFlow()

    private val _isTourPaused = MutableStateFlow(false)
    val isTourPaused = _isTourPaused.asStateFlow()

    private val _use2DMap = MutableStateFlow(false)
    val use2DMap = _use2DMap.asStateFlow()

    private val _streetViewState = MutableStateFlow<StreetViewUiState>(StreetViewUiState.Idle)
    val streetViewState = _streetViewState.asStateFlow()
    private var streetViewJob: Job? = null

    fun toggleMapMode() {
        _use2DMap.value = !_use2DMap.value
    }

    /** Cancels any in-flight check for a previous coordinate and looks up the new one. */
    fun checkStreetView(latitude: Double, longitude: Double) {
        streetViewJob?.cancel()
        _streetViewState.value = StreetViewUiState.Checking
        streetViewJob = viewModelScope.launch {
            _streetViewState.value = try {
                val image = mapillaryService.nearestImage(latitude, longitude)
                if (image != null) StreetViewUiState.Available(image) else StreetViewUiState.Unavailable
            } catch (error: MapillaryError) {
                StreetViewUiState.Error("Street View could not be loaded. Try again.")
            }
        }
    }

    /** Returns the Street View button/flow to its default state, e.g. after the viewer is dismissed. */
    fun resetStreetView() {
        streetViewJob?.cancel()
        _streetViewState.value = StreetViewUiState.Idle
    }

    suspend fun triggerCenterOnUser() {
        _centerOnUserTrigger.emit(Unit)
    }

    suspend fun triggerPostCoordinate(point: Point) {
        _postCoordinateTrigger.emit(point)
    }

    suspend fun triggerOpenSearch() {
        _openSearchTrigger.emit(Unit)
    }

    suspend fun triggerPostToMap(locationQuery: String? = null) {
        _postToMapTrigger.emit(locationQuery)
    }

    suspend fun triggerPostMultiple() {
        _postMultipleTrigger.emit(Unit)
    }

    suspend fun triggerAddMessage() {
        _addMessageTrigger.emit(Unit)
    }

    fun setTourPaused(paused: Boolean) {
        _isTourPaused.value = paused
    }
}
