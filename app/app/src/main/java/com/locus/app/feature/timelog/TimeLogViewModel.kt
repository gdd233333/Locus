package com.locus.app.feature.timelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.app.core.data.FakeTimeLogRepository
import com.locus.app.core.model.FrequentActivity
import com.locus.app.core.model.TimeLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class TimeLogUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDates: List<LocalDate> = emptyList(),
    val activeLog: TimeLog? = null,
    val elapsedSeconds: Long = 0L,
    val logsForDate: List<TimeLog> = emptyList(),
    val suggestedActivities: List<FrequentActivity> = emptyList(),
    val showStartSheet: Boolean = false,
    val isLoading: Boolean = true,
)

class TimeLogViewModel(
    private val repository: FakeTimeLogRepository = FakeTimeLogRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimeLogUiState())
    val uiState: StateFlow<TimeLogUiState> = _uiState

    private var tickerJob: Job? = null

    init {
        // 以今天为锚点的本周（周一 ~ 周日）
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        _uiState.value = _uiState.value.copy(
            weekDates = (0..6).map { monday.plusDays(it.toLong()) },
            suggestedActivities = repository.getFrequentActivities(),
        )

        // 监听进行中的记录：出现时启动秒表，消失时归零
        viewModelScope.launch {
            repository.getActiveLog().collect { active ->
                _uiState.value = _uiState.value.copy(activeLog = active)
                if (active != null) startTicker(active) else stopTicker()
            }
        }

        observeDate(LocalDate.now())
    }

    /** 切换查看日期：重新订阅该日期的记录列表 */
    private fun observeDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        viewModelScope.launch {
            repository.getLogsForDate(date).collect { logs ->
                _uiState.value = _uiState.value.copy(logsForDate = logs, isLoading = false)
            }
        }
    }

    /** 每秒刷新累计时长 */
    private fun startTicker(log: TimeLog) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = log.durationSeconds(Instant.now())
                )
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        _uiState.value = _uiState.value.copy(elapsedSeconds = 0L)
    }

    fun selectDate(date: LocalDate) = observeDate(date)

    fun showStartSheet() { _uiState.value = _uiState.value.copy(showStartSheet = true) }
    fun hideStartSheet() { _uiState.value = _uiState.value.copy(showStartSheet = false) }

    fun startLog(activityName: String) {
        viewModelScope.launch {
            repository.startLog(activityName)
            hideStartSheet()
        }
    }

    fun endLog() {
        val id = _uiState.value.activeLog?.id ?: return
        viewModelScope.launch { repository.endLog(id) }
    }
}
