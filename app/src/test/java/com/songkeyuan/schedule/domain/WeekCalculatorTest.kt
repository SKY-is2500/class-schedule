package com.songkeyuan.schedule.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekCalculatorTest {
    private val firstMonday = LocalDate.of(2026, 8, 31)

    @Test
    fun firstMonday_isWeekOne() {
        assertEquals(1, WeekCalculator.weekFor(firstMonday, firstMonday, 16))
    }

    @Test
    fun followingMonday_isWeekTwo() {
        assertEquals(2, WeekCalculator.weekFor(LocalDate.of(2026, 9, 7), firstMonday, 16))
    }

    @Test
    fun datesOutsideSemester_areClamped() {
        assertEquals(1, WeekCalculator.weekFor(LocalDate.of(2026, 8, 1), firstMonday, 16))
        assertEquals(16, WeekCalculator.weekFor(LocalDate.of(2027, 2, 1), firstMonday, 16))
    }

    @Test
    fun weekMonday_usesSemesterAnchor() {
        assertEquals(LocalDate.of(2026, 9, 7), WeekCalculator.mondayOf(2, firstMonday))
    }
}
