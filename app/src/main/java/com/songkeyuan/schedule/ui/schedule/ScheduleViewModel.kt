package com.songkeyuan.schedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.domain.WeekCalculator
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ScheduleUiState(selectedWeekday = today().dayOfWeek.value),
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var coursesJob: Job? = null
    private var selectionInitialized = false

    init {
        viewModelScope.launch {
            repository.observeSettings()
                .catch {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = LOAD_ERROR)
                }
                .collect { settings ->
                    val week = if (selectionInitialized) {
                        _uiState.value.selectedWeek.coerceIn(1, settings.totalWeeks)
                    } else {
                        selectionInitialized = true
                        WeekCalculator.weekFor(today(), settings.firstWeekMonday, settings.totalWeeks)
                    }
                    _uiState.value = _uiState.value.copy(
                        selectedWeek = week,
                        weekMonday = WeekCalculator.mondayOf(week, settings.firstWeekMonday),
                        settings = settings,
                    )
                    loadCourses()
                }
        }
    }

    fun previousWeek() = selectWeek(_uiState.value.selectedWeek - 1)

    fun nextWeek() = selectWeek(_uiState.value.selectedWeek + 1)

    fun selectViewMode(mode: ScheduleViewMode) {
        _uiState.value = _uiState.value.copy(viewMode=mode)
    }

    fun returnToToday() {
        val state = _uiState.value
        val day=today()
        _uiState.value=state.copy(selectedWeekday=day.dayOfWeek.value)
        val week=WeekCalculator.weekFor(day,state.settings.firstWeekMonday,state.settings.totalWeeks)
        if (week==state.selectedWeek) {
            _uiState.value=_uiState.value.copy(courses=state.weekCourses.filter { it.meeting.weekday==day.dayOfWeek.value })
        } else selectWeek(week)
    }

    fun selectWeekday(weekday: Int) {
        if (weekday !in 1..7 || weekday == _uiState.value.selectedWeekday) return
        _uiState.value = _uiState.value.copy(selectedWeekday = weekday, courses=_uiState.value.weekCourses.filter { it.meeting.weekday==weekday })
    }

    fun retry() = loadCourses()

    private fun selectWeek(week: Int) {
        val state = _uiState.value
        val bounded = week.coerceIn(1, state.settings.totalWeeks)
        if (bounded == state.selectedWeek) return
        _uiState.value = state.copy(
            selectedWeek = bounded,
            weekMonday = WeekCalculator.mondayOf(bounded, state.settings.firstWeekMonday),
        )
        loadCourses()
    }

    private fun loadCourses() {
        coursesJob?.cancel()
        val state = _uiState.value
        _uiState.value=state.copy(isLoading=true,errorMessage=null)
        coursesJob = viewModelScope.launch {
            combine((1..7).map { repository.observeDay(state.selectedWeek,it) }) { days -> days.flatMap { it } }
                .catch {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = LOAD_ERROR)
                }
                .collect { courses ->
                    _uiState.value = _uiState.value.copy(
                        weekCourses = courses,
                        courses = courses.filter { it.meeting.weekday == _uiState.value.selectedWeekday },
                        isLoading = false,
                        errorMessage = null,
                    )
                }
        }
    }

    private companion object {
        const val LOAD_ERROR = "课程加载失败，请重试"
    }
}
