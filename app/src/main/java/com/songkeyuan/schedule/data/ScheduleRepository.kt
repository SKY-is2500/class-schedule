package com.songkeyuan.schedule.data

import kotlinx.coroutines.flow.Flow
import com.songkeyuan.schedule.data.importing.ImportedTimetable

interface ScheduleRepository {
    fun observeDay(week: Int, weekday: Int): Flow<List<ScheduledCourse>>
    fun observePending(): Flow<List<Course>>
    fun observeCourse(id: Long): Flow<CourseDetails?>
    fun observeSettings(): Flow<SemesterSettings>
    suspend fun ensureSeeded()
    suspend fun saveCourse(draft: CourseDraft): SaveOutcome
    suspend fun deleteCourse(id: Long): Result<Unit>
    suspend fun updateSettings(settings: SemesterSettings): Result<Unit>
    suspend fun resetToSeed(): Result<Unit>
    fun observeImportBackupAvailable(): Flow<Boolean>
    suspend fun importTimetable(timetable: ImportedTimetable): Result<Unit>
    suspend fun restoreImportBackup(): Result<Unit>
}
