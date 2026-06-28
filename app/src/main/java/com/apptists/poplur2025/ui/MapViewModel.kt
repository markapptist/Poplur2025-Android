package com.apptists.poplur2025.ui

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel : ViewModel() {
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

    fun toggleMapMode() {
        _use2DMap.value = !_use2DMap.value
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
