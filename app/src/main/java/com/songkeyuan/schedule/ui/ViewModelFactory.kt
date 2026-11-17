package com.songkeyuan.schedule.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.ui.editor.CourseEditorViewModel
import com.songkeyuan.schedule.ui.pending.PendingViewModel
import com.songkeyuan.schedule.ui.schedule.ScheduleViewModel
import com.songkeyuan.schedule.ui.settings.SettingsViewModel
import com.songkeyuan.schedule.ui.importing.ImportViewModel
import java.time.LocalDate

class ViewModelFactory(
    private val repository: ScheduleRepository,
    private val today: () -> LocalDate = LocalDate::now,
    private val courseId: Long? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ScheduleViewModel::class.java) -> ScheduleViewModel(repository, today) as T
        modelClass.isAssignableFrom(ImportViewModel::class.java) -> ImportViewModel(repository) as T
        modelClass.isAssignableFrom(PendingViewModel::class.java) -> PendingViewModel(repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
        modelClass.isAssignableFrom(CourseEditorViewModel::class.java) -> CourseEditorViewModel(repository, courseId) as T
        else -> error("未知 ViewModel：${modelClass.name}")
    }
}
