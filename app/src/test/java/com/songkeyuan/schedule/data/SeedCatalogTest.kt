package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.WeekMask
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedCatalogTest {
    @Test
    fun seedContainsTwelveScheduledAndThreePendingCourses() {
        assertEquals(15, SeedCatalog.courses.size)
        assertEquals(12, SeedCatalog.courses.count { it.meetings.isNotEmpty() })
        assertEquals(3, SeedCatalog.courses.count { it.meetings.isEmpty() })
        assertEquals(14, SeedCatalog.courses.sumOf { it.meetings.size })
    }

    @Test
    fun computerGraphics_usesCorrectRoomsByWeek() {
        val course = SeedCatalog.courses.single { it.code == "COST4168" }
        assertEquals(2, course.meetings.size)
        assertEquals(
            setOf(1, 2, 4, 6, 8, 10),
            WeekMask.toWeeks(course.meetings.single { it.location == "公共教学楼 A108" }.weekMask, 16),
        )
        assertEquals(
            setOf(3, 5, 7, 9, 11),
            WeekMask.toWeeks(course.meetings.single { it.location == "12号楼 A443" }.weekMask, 16),
        )
    }

    @Test
    fun pendingCourseCodesAndTeachers_arePreserved() {
        val pending = SeedCatalog.courses.filter { it.meetings.isEmpty() }.associate { it.code to it.teachers }
        assertEquals(
            mapOf(
                "MARX1012-132" to "张晓敏",
                "COST4196-2" to "胡滨",
                "COST4121-2" to "任守纲",
            ),
            pending,
        )
    }
}
