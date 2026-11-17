package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.data.Course
import com.songkeyuan.schedule.data.ScheduledCourse
import com.songkeyuan.schedule.domain.PeriodTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

data class DayFocus(val label: String, val course: ScheduledCourse? = null)

fun dayFocus(courses: List<ScheduledCourse>, date: LocalDate, now: LocalDateTime): DayFocus {
    if (courses.isEmpty()) return DayFocus("这天没有课程")
    if (date.isBefore(now.toLocalDate())) return DayFocus("当天共 ${courses.size} 次课程")
    val sorted = courses.sortedWith(compareBy({ it.meeting.startPeriod }, { it.meeting.id }))
    if (date.isAfter(now.toLocalDate())) return DayFocus("当天首课", sorted.first())
    val time = now.toLocalTime()
    val current = sorted.firstOrNull {
        val range = PeriodTime.forPeriods(it.meeting.startPeriod, it.meeting.endPeriod)
        time >= range.start && time < range.endInclusive
    }
    if (current != null) return DayFocus("正在上课", current)
    val next = sorted.firstOrNull { PeriodTime.forPeriod(it.meeting.startPeriod).start > time }
    return if (next != null) DayFocus("下一节课", next) else DayFocus("今日课程已结束")
}

/** Course codes, rather than database IDs, keep colors stable after reimport. */
fun courseColorIndex(course: Course): Int {
    val identity = course.code.trim().lowercase(Locale.ROOT).ifBlank { course.name.trim() }
    return Math.floorMod(identity.hashCode(), 8)
}
