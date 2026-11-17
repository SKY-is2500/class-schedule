package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.data.RoomScheduleStore
import com.songkeyuan.schedule.data.ScheduledCourse
import com.songkeyuan.schedule.data.SemesterSettings
import java.time.LocalDate

data class ScheduleUiState(
    val viewMode: ScheduleViewMode = ScheduleViewMode.Daily,
    val weekCourses: List<ScheduledCourse> = emptyList(),
    val selectedWeek: Int = 1,
    val selectedWeekday: Int = 1,
    val weekMonday: LocalDate = RoomScheduleStore.DEFAULT_SETTINGS.firstWeekMonday,
    val settings: SemesterSettings = RoomScheduleStore.DEFAULT_SETTINGS,
    val courses: List<ScheduledCourse> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

enum class ScheduleViewMode { Daily, Weekly }
