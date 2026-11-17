package com.songkeyuan.schedule.data.importing

import com.songkeyuan.schedule.data.CourseDraft

data class ImportedTimetable(val courses: List<CourseDraft>, val requiredWeeks: Int) {
    val scheduledCount get() = courses.count { it.meetings.isNotEmpty() }
    val pendingCount get() = courses.count { it.meetings.isEmpty() }
    val meetingCount get() = courses.sumOf { it.meetings.size }
}
