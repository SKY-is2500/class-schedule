package com.songkeyuan.schedule.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class CourseWithMeetings(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val meetings: List<MeetingEntity>,
)
