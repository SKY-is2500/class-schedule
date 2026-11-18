package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.ConflictDetector
import com.songkeyuan.schedule.domain.MeetingDraft
import com.songkeyuan.schedule.domain.MeetingValidator
import com.songkeyuan.schedule.domain.WeekMask
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.songkeyuan.schedule.data.importing.ImportedTimetable

class RoomScheduleRepository(private val store: ScheduleStore) : ScheduleRepository {
    override fun observeDay(week: Int, weekday: Int): Flow<List<ScheduledCourse>> =
        store.observeCourses().map { it.scheduledOn(week, weekday) }

    override fun observePending(): Flow<List<Course>> =
        store.observeCourses().map { courses ->
            courses.filter { it.meetings.isEmpty() }.map { it.course }
        }

    override fun observeCourse(id: Long): Flow<CourseDetails?> = store.observeCourse(id)

    override fun observeSettings(): Flow<SemesterSettings> = store.observeSettings()

    override suspend fun ensureSeeded() = store.seedIfMissing()

    override suspend fun saveCourse(draft: CourseDraft): SaveOutcome {
        val normalized = draft.copy(
            name = draft.name.trim(),
            code = draft.code.trim(),
            teachers = draft.teachers.trim(),
            note = draft.note.trim(),
            meetings = draft.meetings.map { it.copy(location = it.location.trim()) },
        )
        if (normalized.name.isBlank()) return SaveOutcome.Invalid(listOf("请输入课程名称"))

        val settings = store.observeSettings().firstValue()
        val errors = normalized.meetings.flatMap { MeetingValidator.validate(it, settings.totalWeeks) }
        if (errors.isNotEmpty()) return SaveOutcome.Invalid(errors.distinct())

        val existingCourses = store.snapshotCourses().filterNot { it.course.id == normalized.id }
        val existingMeetings = existingCourses.flatMap { details ->
            details.meetings.map { it.toDraft() }
        }
        normalized.meetings.forEach { candidate ->
            val conflict = ConflictDetector.find(candidate, existingMeetings)
            if (conflict != null) {
                val details = existingCourses.first { course -> course.meetings.any { it.id == conflict.id } }
                return SaveOutcome.Conflict(
                    courseName = details.course.name,
                    weekday = candidate.weekday,
                    periods = max(candidate.startPeriod, conflict.startPeriod)..min(candidate.endPeriod, conflict.endPeriod),
                    weeks = WeekMask.toWeeks(candidate.weekMask and conflict.weekMask, settings.totalWeeks),
                )
            }
        }

        return try {
            SaveOutcome.Success(store.replaceCourse(normalized))
        } catch (error: Exception) {
            SaveOutcome.Failed(error.message ?: "保存失败")
        }
    }

    override suspend fun deleteCourse(id: Long): Result<Unit> = runCatching { store.deleteCourse(id) }

    override suspend fun updateSettings(settings: SemesterSettings): Result<Unit> {
        if (settings.totalWeeks !in 1..30) return Result.failure(IllegalArgumentException("总周数请输入 1–30"))
        return runCatching { store.updateSettings(settings) }
    }

    override suspend fun resetToSeed(): Result<Unit> = runCatching { store.resetToSeed() }

    override fun observeImportBackupAvailable(): Flow<Boolean> = store.observeImportBackupAvailable()

    override suspend fun importTimetable(timetable: ImportedTimetable): Result<Unit> = runCatching {
        require(timetable.courses.isNotEmpty()) { "没有可导入的课程" }
        val settings = store.observeSettings().first()
        require(timetable.requiredWeeks <= settings.totalWeeks) {
            "课表包含第 ${timetable.requiredWeeks} 周，请先在设置中将总教学周数调整为至少 ${timetable.requiredWeeks} 周"
        }
        val courses = timetable.courses.map { course ->
            require(course.name.isNotBlank()) { "课程名称不能为空" }
            require(course.meetings.all { MeetingValidator.validate(it, settings.totalWeeks).isEmpty() }) { "存在无效上课安排" }
            course.copy(id = null, name = course.name.trim(), meetings = course.meetings.map { it.copy(id = null, courseId = null) }.distinct())
        }
        // Real school exports may contain overlaps. Preserve them in both views for review;
        // manual edits continue to enforce the existing conflict rule.
        store.importCourses(courses)
    }

    override suspend fun restoreImportBackup(): Result<Unit> = runCatching { store.restoreImportBackup() }

    private fun Meeting.toDraft() = MeetingDraft(
        id = id,
        courseId = courseId,
        weekday = weekday,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        weekMask = weekMask,
        location = location,
    )
}

private suspend fun <T> Flow<T>.firstValue(): T = first()
