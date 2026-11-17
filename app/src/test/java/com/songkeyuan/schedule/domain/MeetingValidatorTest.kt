package com.songkeyuan.schedule.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MeetingValidatorTest {
    @Test
    fun rejectsInvalidDayPeriodAndEmptyWeeks() {
        val errors = MeetingValidator.validate(
            MeetingDraft(weekday = 8, startPeriod = 5, endPeriod = 4, weekMask = 0L),
            totalWeeks = 16,
        )
        assertEquals(listOf("请选择星期", "结束节次不能早于开始节次", "请选择上课周次"), errors)
    }
}
