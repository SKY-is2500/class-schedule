package com.songkeyuan.schedule.domain

object WeekMask {
    fun fromWeeks(weeks: Set<Int>, totalWeeks: Int): Long {
        require(totalWeeks in 1..30) { "总周数必须在 1–30 之间" }
        require(weeks.all { it in 1..totalWeeks }) { "周次超出教学周范围" }
        return weeks.fold(0L) { mask, week -> mask or (1L shl (week - 1)) }
    }

    fun toWeeks(mask: Long, totalWeeks: Int): Set<Int> {
        require(totalWeeks in 1..30) { "总周数必须在 1–30 之间" }
        return (1..totalWeeks).filterTo(linkedSetOf()) { week ->
            mask and (1L shl (week - 1)) != 0L
        }
    }

    fun format(mask: Long, totalWeeks: Int): String {
        val weeks = toWeeks(mask, totalWeeks).toList()
        if (weeks.isEmpty()) return "未选周次"

        val parts = mutableListOf<String>()
        var start = weeks.first()
        var previous = start
        for (week in weeks.drop(1)) {
            if (week == previous + 1) {
                previous = week
            } else {
                parts += formatRange(start, previous)
                start = week
                previous = week
            }
        }
        parts += formatRange(start, previous)
        return parts.joinToString("、") + "周"
    }

    private fun formatRange(start: Int, end: Int): String =
        if (start == end) "$start" else "$start–$end"
}
