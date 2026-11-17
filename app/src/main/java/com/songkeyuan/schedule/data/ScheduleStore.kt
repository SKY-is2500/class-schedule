package com.songkeyuan.schedule.data

import kotlinx.coroutines.flow.Flow

interface ScheduleStore {
    fun observeCourses(): Flow<List<CourseDetails>>
    fun observeCourse(id: Long): Flow<CourseDetails?>
    fun observeSettings(): Flow<SemesterSettings>
    suspend fun snapshotCourses(): List<CourseDetails>
    suspend fun seedIfMissing()
    suspend fun replaceCourse(draft: CourseDraft): Long
    suspend fun deleteCourse(id: Long)
    suspend fun updateSettings(settings: SemesterSettings)
    suspend fun resetToSeed()
    fun observeImportBackupAvailable(): Flow<Boolean>
    suspend fun importCourses(courses: List<CourseDraft>)
    suspend fun restoreImportBackup()
}
