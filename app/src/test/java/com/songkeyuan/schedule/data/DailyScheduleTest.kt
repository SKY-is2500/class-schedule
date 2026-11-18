package com.songkeyuan.schedule.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyScheduleTest {
    private val semester = SemesterSettings(LocalDate.of(2026, 8, 31), 16)

    @Test
    fun selectsOnlyMeetingsForTheRealDayAndWeekInPeriodOrder() {
        val monday = LocalDate.of(2026, 9, 7)
        val courses = listOf(
            course(1, weekday = 1, period = 6, weeks = 0b10),
            course(2, weekday = 2, period = 1, weeks = 0b10),
            course(3, weekday = 1, period = 4, weeks = 0b01),
            course(4, weekday = 1, period = 2, weeks = 0b11),
        )

        val schedule = scheduleForDate(monday, semester, courses)

        assertEquals(2, schedule.week)
        assertEquals(listOf(4L, 1L), schedule.courses.map { it.course.id })
    }

    @Test
    fun doesNotShowFirstOrLastWeekCoursesOutsideTheSemester() {
        val sunday = course(1, weekday = 7, period = 1, weeks = 1)
        val monday = course(2, weekday = 1, period = 1, weeks = 1L shl 15)

        for (date in listOf(LocalDate.of(2026, 8, 30), LocalDate.of(2026, 12, 21))) {
            val schedule = scheduleForDate(date, semester, listOf(sunday, monday))
            assertNull(schedule.week)
            assertEquals(emptyList<ScheduledCourse>(), schedule.courses)
        }
    }

    @Test
    fun distinguishesANoClassDayFromBeingOutsideTheSemester() {
        val schedule = scheduleForDate(LocalDate.of(2026, 12, 20), semester, emptyList())

        assertEquals(16, schedule.week)
        assertEquals(emptyList<ScheduledCourse>(), schedule.courses)
    }

    private fun course(id: Long, weekday: Int, period: Int, weeks: Long) = CourseDetails(
        Course(id, "Course $id", "", ""),
        listOf(Meeting(id, id, weekday, period, period, weeks, "Room $id")),
    )
}
