package com.apptists.poplur2025.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapViewModel : ViewModel() {
    private val _centerOnUserTrigger = MutableSharedFlow<Unit>(replay = 0)
    val centerOnUserTrigger = _centerOnUserTrigger.asSharedFlow()

    suspend fun triggerCenterOnUser() {
        _centerOnUserTrigger.emit(Unit)
    }
}
