package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.MeetingDraft
import java.time.LocalDate

data class Course(
    val id: Long,
    val name: String,
    val code: String,
    val teachers: String,
    val note: String = "",
)

data class Meeting(
    val id: Long,
    val courseId: Long,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekMask: Long,
    val location: String,
)

data class CourseDetails(val course: Course, val meetings: List<Meeting>)

data class ScheduledCourse(val course: Course, val meeting: Meeting)

data class SemesterSettings(
    val firstWeekMonday: LocalDate,
    val totalWeeks: Int,
)

data class CourseDraft(
    val id: Long? = null,
    val name: String,
    val code: String = "",
    val teachers: String = "",
    val note: String = "",
    val meetings: List<MeetingDraft> = emptyList(),
)

sealed interface SaveOutcome {
    data class Success(val courseId: Long) : SaveOutcome
    data class Invalid(val messages: List<String>) : SaveOutcome
    data class Conflict(
        val courseName: String,
        val weekday: Int,
        val periods: IntRange,
        val weeks: Set<Int>,
    ) : SaveOutcome
    data class Failed(val message: String) : SaveOutcome
}
