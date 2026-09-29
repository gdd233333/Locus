package com.locus.app.feature.sober

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.app.core.data.FakeStreakRepository
import com.locus.app.core.model.UrgeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SoberUiState(
    val currentStreakDays: Int = 0,
    val isPersonalBest: Boolean = false,
    val todayUrgeCount: Int = 0,
    val latestUrge: UrgeEvent? = null,
    val urgeWaveData: List<Float> = emptyList(),
    val todayCheckedIn: Boolean = false,
    val isLoading: Boolean = true,
)

class SoberViewModel(
    private val repository: FakeStreakRepository = FakeStreakRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoberUiState())
    val uiState: StateFlow<SoberUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                repository.getCurrentStreakDays(),
                repository.isPersonalBest(),
                repository.getTodayUrgeCount(),
                repository.getLatestUrge(),
                repository.getRecentUrgeIntensities(),
                repository.getTodayCheckIn(),
            ) { values ->
                SoberUiState(
                    currentStreakDays = values[0] as Int,
                    isPersonalBest = values[1] as Boolean,
                    todayUrgeCount = values[2] as Int,
                    latestUrge = values[3] as UrgeEvent?,
                    urgeWaveData = values[4] as List<Float>,
                    todayCheckedIn = values[5] != null,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }
}
