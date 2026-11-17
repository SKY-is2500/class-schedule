package com.songkeyuan.schedule.domain

data class MeetingDraft(
    val id: Long? = null,
    val courseId: Long? = null,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekMask: Long,
    val location: String = "",
)
