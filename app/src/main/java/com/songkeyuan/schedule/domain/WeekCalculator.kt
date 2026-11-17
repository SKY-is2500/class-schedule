package com.songkeyuan.schedule.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {
    fun weekFor(date: LocalDate, firstWeekMonday: LocalDate, totalWeeks: Int): Int {
        require(totalWeeks > 0) { "总周数必须大于 0" }
        val rawWeek = ChronoUnit.WEEKS.between(firstWeekMonday, date).toInt() + 1
        return rawWeek.coerceIn(1, totalWeeks)
    }

    fun mondayOf(week: Int, firstWeekMonday: LocalDate): LocalDate {
        require(week > 0) { "周次必须大于 0" }
        return firstWeekMonday.plusWeeks((week - 1).toLong())
    }
}
