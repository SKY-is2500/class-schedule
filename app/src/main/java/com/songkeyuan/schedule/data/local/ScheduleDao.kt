package com.songkeyuan.schedule.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY id")
    fun observeCourses(): Flow<List<CourseWithMeetings>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    fun observeCourse(id: Long): Flow<CourseWithMeetings?>

    @Transaction
    @Query("SELECT * FROM courses ORDER BY id")
    suspend fun getAllCourses(): List<CourseWithMeetings>

    @Query("SELECT * FROM meetings ORDER BY weekday, startPeriod, id")
    suspend fun getAllMeetings(): List<MeetingEntity>

    @Query("SELECT * FROM meetings WHERE courseId = :courseId ORDER BY weekday, startPeriod, id")
    suspend fun getMeetingsForCourse(courseId: Long): List<MeetingEntity>

    @Insert
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Insert
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Insert
    suspend fun insertMeetings(meetings: List<MeetingEntity>): List<Long>

    @Query("DELETE FROM meetings WHERE courseId = :courseId")
    suspend fun deleteMeetingsForCourse(courseId: Long)

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteCourseById(courseId: Long)

    @Query("DELETE FROM meetings")
    suspend fun clearMeetings()

    @Query("DELETE FROM courses")
    suspend fun clearCourses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: SemesterSettingsEntity)

    @Query("SELECT * FROM semester_settings WHERE id = 1")
    suspend fun getSettings(): SemesterSettingsEntity?

    @Query("SELECT * FROM semester_settings WHERE id = 1")
    fun observeSettings(): Flow<SemesterSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: AppMetaEntity)

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    suspend fun getMetaValue(key: String): String?

    @Query("SELECT value FROM app_meta WHERE `key` = :key")
    fun observeMetaValue(key: String): Flow<String?>

    @Query("DELETE FROM app_meta WHERE `key` = :key")
    suspend fun deleteMetaValue(key: String)

    @Query("DELETE FROM app_meta")
    suspend fun clearMeta()
}
