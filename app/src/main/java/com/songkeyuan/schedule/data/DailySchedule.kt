package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.WeekCalculator
import java.time.LocalDate

internal data class DailySchedule(val week: Int?, val courses: List<ScheduledCourse>)

internal fun scheduleForDate(
    date: LocalDate,
    semester: SemesterSettings,
    courses: List<CourseDetails>,
): DailySchedule {
    val start = semester.firstWeekMonday
    if (date < start || date >= start.plusWeeks(semester.totalWeeks.toLong())) {
        return DailySchedule(null, emptyList())
    }
    val week = WeekCalculator.weekFor(date, start, semester.totalWeeks)
    return DailySchedule(week, courses.scheduledOn(week, date.dayOfWeek.value))
}

internal fun List<CourseDetails>.scheduledOn(week: Int, weekday: Int): List<ScheduledCourse> =
    flatMap { details ->
        details.meetings
            .filter { it.weekday == weekday && it.weekMask and (1L shl (week - 1)) != 0L }
            .map { ScheduledCourse(details.course, it) }
    }.sortedWith(compareBy({ it.meeting.startPeriod }, { it.meeting.endPeriod }, { it.course.name }))
