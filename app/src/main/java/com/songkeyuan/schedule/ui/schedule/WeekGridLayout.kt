package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.data.ScheduledCourse

data class WeekCoursePlacement(val item: ScheduledCourse, val lane: Int, val laneCount: Int)

fun layoutDayCourses(courses: List<ScheduledCourse>): List<WeekCoursePlacement> {
    val lanes = mutableListOf<Int>()
    val assigned = courses.sortedBy { it.meeting.startPeriod }.map { item ->
        val lane = lanes.indexOfFirst { it < item.meeting.startPeriod }.let { if (it < 0) lanes.size else it }
        if (lane == lanes.size) lanes += item.meeting.endPeriod else lanes[lane] = item.meeting.endPeriod
        item to lane
    }
    return assigned.map { (item,lane) -> WeekCoursePlacement(item,lane,lanes.size) }
}
