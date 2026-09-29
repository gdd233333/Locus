package com.locus.app.feature.sober

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.locus.app.LocusApplication
import com.locus.app.core.data.StreakRepository
import com.locus.app.core.data.settings.SettingsRepository
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
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
    val userInitial: String = "K",
    val showCheckInSheet: Boolean = false,
    val showEmergencySheet: Boolean = false,
    /** 打卡成功的时间戳：变化一次播放一次粒子庆祝 */
    val celebrationTrigger: Long = 0L,
)

class SoberViewModel(
    private val repository: StreakRepository,
    private val settingsRepository: SettingsRepository,
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
            }.collect { newState ->
                // 数据字段来自 Flow，纯 UI 状态（弹层/庆祝/头像字母）保留当前值，避免被覆盖
                _uiState.value = newState.copy(
                    showCheckInSheet = _uiState.value.showCheckInSheet,
                    showEmergencySheet = _uiState.value.showEmergencySheet,
                    celebrationTrigger = _uiState.value.celebrationTrigger,
                    userInitial = _uiState.value.userInitial,
                )
            }
        }

        // 头像字母来自设置（DataStore），默认 "K"
        viewModelScope.launch {
            settingsRepository.userInitial.collect { name ->
                _uiState.value = _uiState.value.copy(userInitial = name)
            }
        }
    }

    fun showCheckInSheet() { _uiState.value = _uiState.value.copy(showCheckInSheet = true) }

    fun hideCheckInSheet() { _uiState.value = _uiState.value.copy(showCheckInSheet = false) }

    fun showEmergencySheet() { _uiState.value = _uiState.value.copy(showEmergencySheet = true) }

    fun hideEmergencySheet() { _uiState.value = _uiState.value.copy(showEmergencySheet = false) }

    /** 打卡：写入后触发庆祝粒子（弹层由 sheet 自己先 hide 再回调关闭） */
    fun checkIn(mood: Int, note: String?) {
        viewModelScope.launch {
            repository.checkIn(mood, note)
            _uiState.value = _uiState.value.copy(
                celebrationTrigger = System.currentTimeMillis(),
            )
        }
    }

    /** 记录一次冲动（SOS 弹层三个出口共用） */
    fun logUrge(intensity: UrgeIntensity) {
        viewModelScope.launch { repository.logUrge(intensity) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LocusApplication
                SoberViewModel(
                    repository = application.container.streakRepository,
                    settingsRepository = application.container.settingsRepository,
                )
            }
        }
    }
}
