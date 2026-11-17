package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.data.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class CoursePresentationTest {
    private fun item(id: Long, start: Int, end: Int) = ScheduledCourse(
        Course(id, "课程$id", "CODE$id", "教师"), Meeting(id, id, 2, start, end, 1L, "A101"),
    )
    @Test fun currentClassWinsOverLaterClassesRegardlessOfInputOrder() {
        val current = item(1, 1, 2)
        val result = dayFocus(listOf(item(2, 6, 7), current), java.time.LocalDate.of(2026, 11, 17), LocalDateTime.of(2026, 11, 17, 8, 30))
        assertEquals(current, result.course)
        assertEquals("正在上课", result.label)
    }
    @Test fun exactEndTimeIsFinishedAndNextClassIsChosen() {
        val next = item(2, 6, 7)
        assertEquals(next, dayFocus(listOf(item(1, 1, 2), next), java.time.LocalDate.of(2026,11,17), LocalDateTime.of(2026,11,17,9,35)).course)
    }
    @Test fun pastDayNeverClaimsClassIsUpcomingAndEmptyDayIsRestDay() {
        val now = LocalDateTime.of(2026,11,17,8,0)
        assertNull(dayFocus(listOf(item(1,1,2)), now.toLocalDate().minusDays(1), now).course)
        assertEquals("这天没有课程", dayFocus(emptyList(), now.toLocalDate(), now).label)
    }
    @Test fun futureDayShowsFirstClassAndTodayAfterLastClassIsFinished() {
        val first=item(1,1,2)
        val now=LocalDateTime.of(2026,11,17,22,0)
        assertEquals(first,dayFocus(listOf(item(2,6,7),first),now.toLocalDate().plusDays(1),now).course)
        assertEquals("今日课程已结束",dayFocus(listOf(first),now.toLocalDate(),now).label)
    }
    @Test fun courseColorSurvivesDatabaseIdsAndDisplayNameChangesWithSameCode() {
        val original=Course(1,"高等数学"," MATH101 ","王老师")
        assertEquals(courseColorIndex(original),courseColorIndex(original.copy(id=900,name="高等数学（调整）",code="math101")))
        assertTrue(courseColorIndex(original) in 0..7)
        assertEquals(courseColorIndex(original.copy(code="",id=2)),courseColorIndex(original.copy(code="",id=3)))
    }
}
