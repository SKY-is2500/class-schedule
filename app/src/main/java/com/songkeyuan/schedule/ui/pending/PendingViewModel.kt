package com.songkeyuan.schedule.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songkeyuan.schedule.data.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PendingViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PendingUiState())
    val uiState: StateFlow<PendingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observePending()
                .catch { _uiState.value = PendingUiState(isLoading = false, errorMessage = "待安排课程加载失败") }
                .collect { _uiState.value = PendingUiState(courses = it, isLoading = false) }
        }
    }
}
