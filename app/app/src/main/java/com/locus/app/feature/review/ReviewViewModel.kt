package com.locus.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.locus.app.LocusApplication
import com.locus.app.core.data.StreakRepository
import com.locus.app.core.data.TimeLogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class Period { WEEK, MONTH }

data class ReviewUiState(
    val period: Period = Period.WEEK,
    val dailyDurations: List<Pair<LocalDate, Long>> = emptyList(),
    val urgeCounts: List<Pair<LocalDate, Int>> = emptyList(),
    val resolvedCount: Int = 0,
    val checkInDates: Set<LocalDate> = emptySet(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = true,
)

class ReviewViewModel(
    private val timeLogRepository: TimeLogRepository,
    private val streakRepository: StreakRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState

    private var observeJob: Job? = null

    init {
        observe()
    }

    fun setPeriod(period: Period) {
        if (_uiState.value.period == period) return
        _uiState.value = _uiState.value.copy(period = period)
        observe()
    }

    /** 月视图下切月（-1 上一月 / +1 下一月） */
    fun shiftMonth(months: Long) {
        _uiState.value = _uiState.value.copy(
            displayedMonth = _uiState.value.displayedMonth.plusMonths(months)
        )
        observe()
    }

    private fun observe() {
        observeJob?.cancel()
        val period = _uiState.value.period
        val month = _uiState.value.displayedMonth
        val (from, to) = rangeOf(period, month)

        observeJob = viewModelScope.launch {
            combine(
                timeLogRepository.getDailyDurations(from, to),
                streakRepository.getDailyUrgeCounts(from, to),
                streakRepository.getResolvedUrgeCount(from, to),
                streakRepository.getCheckInDatesInMonth(month),
            ) { durations, urges, resolved, checkIns ->
                ReviewUiState(
                    period = period,
                    dailyDurations = durations,
                    urgeCounts = urges,
                    resolvedCount = resolved,
                    checkInDates = checkIns,
                    displayedMonth = month,
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
        }
    }

    /** WEEK = 本周一 ~ 周日；MONTH = 展示月份的整月 */
    private fun rangeOf(period: Period, month: YearMonth): Pair<LocalDate, LocalDate> =
        when (period) {
            Period.WEEK -> {
                val today = LocalDate.now()
                val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
                monday to monday.plusDays(6)
            }
            Period.MONTH -> month.atDay(1) to month.atEndOfMonth()
        }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LocusApplication
                ReviewViewModel(
                    timeLogRepository = application.container.timeLogRepository,
                    streakRepository = application.container.streakRepository,
                )
            }
        }
    }
}
