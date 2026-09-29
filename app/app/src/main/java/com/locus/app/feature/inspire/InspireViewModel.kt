package com.locus.app.feature.inspire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.locus.app.LocusApplication
import com.locus.app.core.data.ActivityRepository
import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InspireUiState(
    val selectedCategory: ActivityCategory? = null,
    val activities: List<Activity> = emptyList(),
    val isShuffling: Boolean = false,
    val isLoading: Boolean = true,
)

class InspireViewModel(
    private val repository: ActivityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspireUiState())
    val uiState: StateFlow<InspireUiState> = _uiState

    init {
        shuffle()
    }

    fun selectCategory(category: ActivityCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        shuffle()
    }

    /** 换一批：先播放退出动画（400ms），再换数据播放入场动画 */
    fun shuffle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isShuffling = true)
            delay(400)
            val newActivities = repository.getRandomActivities(
                count = 3,
                category = _uiState.value.selectedCategory,
            )
            _uiState.value = _uiState.value.copy(
                activities = newActivities,
                isShuffling = false,
                isLoading = false,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LocusApplication
                InspireViewModel(application.container.activityRepository)
            }
        }
    }
}
