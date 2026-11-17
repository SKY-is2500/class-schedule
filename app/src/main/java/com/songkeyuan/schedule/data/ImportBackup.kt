package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.MeetingDraft
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

internal object ImportBackup {
    fun encode(courses: List<CourseDetails>, settings: SemesterSettings): String = JSONObject().apply {
        put("date",settings.firstWeekMonday.toString())
        put("weeks",settings.totalWeeks)
        put("courses",JSONArray().apply {
            courses.forEach { details -> put(JSONObject().apply {
                put("name",details.course.name); put("code",details.course.code)
                put("teachers",details.course.teachers); put("note",details.course.note)
                put("meetings",JSONArray().apply { details.meetings.forEach { meeting -> put(JSONObject().apply {
                    put("day",meeting.weekday); put("start",meeting.startPeriod); put("end",meeting.endPeriod)
                    put("mask",meeting.weekMask); put("location",meeting.location)
                }) } })
            }) }
        })
    }.toString()

    fun decode(text: String): Pair<List<CourseDraft>, SemesterSettings> {
        val obj = JSONObject(text)
        val courses = obj.getJSONArray("courses")
        return (0 until courses.length()).map { index ->
            val course=courses.getJSONObject(index)
            val meetings=course.getJSONArray("meetings")
            CourseDraft(name=course.getString("name"),code=course.getString("code"),teachers=course.getString("teachers"),note=course.getString("note"),
                meetings=(0 until meetings.length()).map { i ->
                    val m=meetings.getJSONObject(i)
                    MeetingDraft(weekday=m.getInt("day"),startPeriod=m.getInt("start"),endPeriod=m.getInt("end"),weekMask=m.getLong("mask"),location=m.getString("location"))
                })
        } to SemesterSettings(LocalDate.parse(obj.getString("date")),obj.getInt("weeks"))
    }
}
