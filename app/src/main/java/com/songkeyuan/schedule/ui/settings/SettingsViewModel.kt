package com.songkeyuan.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.data.SemesterSettings
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: ScheduleRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeImportBackupAvailable().collect { available ->
                _uiState.value=_uiState.value.copy(importBackupAvailable=available)
            }
        }
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    firstWeekMonday = settings.firstWeekMonday.toString(),
                    totalWeeks = settings.totalWeeks.toString(),
                    isSaving = false,
                )
            }
        }
    }

    fun setFirstWeekMonday(value: String) {
        _uiState.value = _uiState.value.copy(firstWeekMonday = value, message = null)
    }

    fun setTotalWeeks(value: String) {
        _uiState.value = _uiState.value.copy(totalWeeks = value.filter(Char::isDigit), message = null)
    }

    fun save() {
        val state = _uiState.value
        val weeks = state.totalWeeks.toIntOrNull()
        if (weeks == null || weeks !in 1..30) {
            _uiState.value = state.copy(message = "总周数请输入 1–30")
            return
        }
        val date = try {
            LocalDate.parse(state.firstWeekMonday)
        } catch (_: DateTimeParseException) {
            _uiState.value = state.copy(message = "日期格式请输入 YYYY-MM-DD")
            return
        }
        _uiState.value = state.copy(isSaving = true, message = null)
        viewModelScope.launch {
            repository.updateSettings(SemesterSettings(date, weeks))
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, message = "设置已保存") }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, message = it.message ?: "保存失败") }
        }
    }

    fun requestReset() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = true)
    }

    fun requestRestoreBackup() { _uiState.value=_uiState.value.copy(showRestoreBackupConfirmation=true) }
    fun dismissRestoreBackup() { _uiState.value=_uiState.value.copy(showRestoreBackupConfirmation=false) }
    fun confirmRestoreBackup() {
        _uiState.value=_uiState.value.copy(showRestoreBackupConfirmation=false,isSaving=true,message=null)
        viewModelScope.launch {
            repository.restoreImportBackup()
                .onSuccess { _uiState.value=_uiState.value.copy(isSaving=false,message="已恢复导入前的课表") }
                .onFailure { _uiState.value=_uiState.value.copy(isSaving=false,message=it.message ?: "恢复失败") }
        }
    }

    fun dismissReset() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = false)
    }

    fun confirmReset() {
        _uiState.value = _uiState.value.copy(showResetConfirmation = false, isSaving = true, message = null)
        viewModelScope.launch {
            repository.resetToSeed()
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, message = "已恢复初始课表") }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, message = it.message ?: "恢复失败") }
        }
    }
}
