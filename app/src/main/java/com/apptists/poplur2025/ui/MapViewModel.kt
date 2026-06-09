package com.apptists.poplur2025.ui

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapViewModel : ViewModel() {
    private val _centerOnUserTrigger = MutableSharedFlow<Unit>(replay = 0)
    val centerOnUserTrigger = _centerOnUserTrigger.asSharedFlow()
    private val _postCoordinateTrigger = MutableSharedFlow<Point>(replay = 0)
    val postCoordinateTrigger = _postCoordinateTrigger.asSharedFlow()

    suspend fun triggerCenterOnUser() {
        _centerOnUserTrigger.emit(Unit)
    }

    suspend fun triggerPostCoordinate(point: Point) {
        _postCoordinateTrigger.emit(point)
    }
}
