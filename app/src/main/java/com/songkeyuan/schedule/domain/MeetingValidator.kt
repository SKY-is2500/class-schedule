package com.songkeyuan.schedule.domain

object MeetingValidator {
    fun validate(candidate: MeetingDraft, totalWeeks: Int): List<String> = buildList {
        if (candidate.weekday !in 1..7) add("请选择星期")
        if (candidate.startPeriod !in 1..12 || candidate.endPeriod !in 1..12) {
            add("请选择有效节次")
        } else if (candidate.endPeriod < candidate.startPeriod) {
            add("结束节次不能早于开始节次")
        }

        val validMask = if (totalWeeks in 1..30) {
            WeekMask.fromWeeks((1..totalWeeks).toSet(), totalWeeks)
        } else {
            0L
        }
        if (candidate.weekMask == 0L) {
            add("请选择上课周次")
        } else if (candidate.weekMask and validMask != candidate.weekMask) {
            add("上课周次超出教学周范围")
        }
    }
}
