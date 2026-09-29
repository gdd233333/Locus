package com.locus.app.feature.sober

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.locus.app.LocusApplication
import com.locus.app.core.data.StreakRepository
import kotlinx.coroutines.launch

/** 冲浪练习：结束时把最近一条冲动标记为已平复 */
class SurfingViewModel(
    private val repository: StreakRepository,
) : ViewModel() {

    /**
     * 先写入平复记录，写库完成后再回调（返回导航）。
     * 注意：不能先 popBackStack 再写——返回会清掉本页 ViewModel，viewModelScope 随之中止，
     * 写入会丢（实测过）。
     */
    fun resolveLatestUrgeAndFinish(durationMinutes: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resolveLatestUrge(durationMinutes, METHOD)
            onDone()
        }
    }

    companion object {
        private const val METHOD = "冲浪练习"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LocusApplication
                SurfingViewModel(application.container.streakRepository)
            }
        }
    }
}
