package com.songkeyuan.schedule.domain

import java.time.LocalTime

object PeriodTime {
    private val times = mapOf(
        1 to (LocalTime.of(8, 0)..LocalTime.of(8, 45)),
        2 to (LocalTime.of(8, 50)..LocalTime.of(9, 35)),
        3 to (LocalTime.of(9, 50)..LocalTime.of(10, 35)),
        4 to (LocalTime.of(10, 40)..LocalTime.of(11, 25)),
        5 to (LocalTime.of(11, 30)..LocalTime.of(12, 15)),
        6 to (LocalTime.of(14, 0)..LocalTime.of(14, 45)),
        7 to (LocalTime.of(14, 50)..LocalTime.of(15, 35)),
        8 to (LocalTime.of(15, 50)..LocalTime.of(16, 35)),
        9 to (LocalTime.of(16, 40)..LocalTime.of(17, 25)),
        10 to (LocalTime.of(19, 0)..LocalTime.of(19, 45)),
        11 to (LocalTime.of(19, 50)..LocalTime.of(20, 35)),
        12 to (LocalTime.of(20, 40)..LocalTime.of(21, 25)),
    )

    fun forPeriod(period: Int): ClosedRange<LocalTime> =
        requireNotNull(times[period]) { "无效节次：$period" }

    fun forPeriods(startPeriod: Int, endPeriod: Int): ClosedRange<LocalTime> {
        require(startPeriod <= endPeriod) { "开始节次不能晚于结束节次" }
        return forPeriod(startPeriod).start..forPeriod(endPeriod).endInclusive
    }
}
