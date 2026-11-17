package com.songkeyuan.schedule.domain

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodTimeTest {
    @Test
    fun representativePeriods_matchUniversityTimes() {
        assertEquals(LocalTime.of(8, 0)..LocalTime.of(8, 45), PeriodTime.forPeriod(1))
        assertEquals(LocalTime.of(11, 30)..LocalTime.of(12, 15), PeriodTime.forPeriod(5))
        assertEquals(LocalTime.of(14, 0)..LocalTime.of(14, 45), PeriodTime.forPeriod(6))
        assertEquals(LocalTime.of(16, 40)..LocalTime.of(17, 25), PeriodTime.forPeriod(9))
        assertEquals(LocalTime.of(19, 0)..LocalTime.of(19, 45), PeriodTime.forPeriod(10))
        assertEquals(LocalTime.of(20, 40)..LocalTime.of(21, 25), PeriodTime.forPeriod(12))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidPeriod_isRejected() {
        PeriodTime.forPeriod(13)
    }
}
