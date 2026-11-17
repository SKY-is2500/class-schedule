package com.songkeyuan.schedule.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryScheduleStore : ScheduleStore {
    private val coursesFlow = MutableStateFlow<List<CourseDetails>>(emptyList())
    private val settingsFlow = MutableStateFlow(SemesterSettings(LocalDate.of(2026, 8, 31), 16))
    private var seeded = false
    private var nextCourseId = 1L
    private var nextMeetingId = 1L
    private val backupAvailable = MutableStateFlow(false)
    private var backup: Pair<List<CourseDetails>,SemesterSettings>? = null

    override fun observeCourses(): Flow<List<CourseDetails>> = coursesFlow
    override fun observeCourse(id: Long): Flow<CourseDetails?> =
        MutableStateFlow(coursesFlow.value.firstOrNull { it.course.id == id })
    override fun observeSettings(): Flow<SemesterSettings> = settingsFlow
    override suspend fun snapshotCourses(): List<CourseDetails> = coursesFlow.value

    override suspend fun seedIfMissing() {
        if (seeded) return
        writeSeed()
        seeded = true
    }

    override suspend fun replaceCourse(draft: CourseDraft): Long {
        val id = draft.id ?: nextCourseId++
        val course = Course(id, draft.name, draft.code, draft.teachers, draft.note)
        val meetings = draft.meetings.map { meeting ->
            Meeting(
                id = meeting.id ?: nextMeetingId++,
                courseId = id,
                weekday = meeting.weekday,
                startPeriod = meeting.startPeriod,
                endPeriod = meeting.endPeriod,
                weekMask = meeting.weekMask,
                location = meeting.location,
            )
        }
        coursesFlow.value = coursesFlow.value.filterNot { it.course.id == id } + CourseDetails(course, meetings)
        return id
    }

    override suspend fun deleteCourse(id: Long) {
        coursesFlow.value = coursesFlow.value.filterNot { it.course.id == id }
    }

    override suspend fun updateSettings(settings: SemesterSettings) {
        settingsFlow.value = settings
    }

    override suspend fun resetToSeed() {
        nextCourseId = 1
        nextMeetingId = 1
        writeSeed()
        seeded = true
    }

    override fun observeImportBackupAvailable(): Flow<Boolean> = backupAvailable
    override suspend fun importCourses(courses: List<CourseDraft>) {
        backup=coursesFlow.value to settingsFlow.value
        backupAvailable.value=true
        coursesFlow.value=emptyList()
        courses.forEach { replaceCourse(it) }
    }
    override suspend fun restoreImportBackup() {
        val saved=backup ?: error("没有可恢复的导入前备份")
        coursesFlow.value=saved.first
        settingsFlow.value=saved.second
        backup=null
        backupAvailable.value=false
    }

    fun courseCount(): Int = coursesFlow.value.size

    fun meetingCount(): Int = coursesFlow.value.sumOf { it.meetings.size }

    fun clearCoursesOnly() {
        coursesFlow.value = emptyList()
    }

    private fun writeSeed() {
        settingsFlow.value = SemesterSettings(LocalDate.of(2026, 8, 31), 16)
        coursesFlow.value = SeedCatalog.courses.map { seed ->
            val courseId = nextCourseId++
            CourseDetails(
                course = Course(courseId, seed.name, seed.code, seed.teachers),
                meetings = seed.meetings.map { meeting ->
                    Meeting(
                        id = nextMeetingId++,
                        courseId = courseId,
                        weekday = meeting.weekday,
                        startPeriod = meeting.startPeriod,
                        endPeriod = meeting.endPeriod,
                        weekMask = meeting.weekMask,
                        location = meeting.location,
                    )
                },
            )
        }
    }
}
