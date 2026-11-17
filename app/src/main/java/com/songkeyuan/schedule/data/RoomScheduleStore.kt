package com.songkeyuan.schedule.data

import androidx.room.withTransaction
import com.songkeyuan.schedule.data.local.AppDatabase
import com.songkeyuan.schedule.data.local.AppMetaEntity
import com.songkeyuan.schedule.data.local.CourseEntity
import com.songkeyuan.schedule.data.local.CourseWithMeetings
import com.songkeyuan.schedule.data.local.MeetingEntity
import com.songkeyuan.schedule.data.local.SemesterSettingsEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduleStore(private val database: AppDatabase) : ScheduleStore {
    private val dao = database.scheduleDao()

    override fun observeCourses(): Flow<List<CourseDetails>> =
        dao.observeCourses().map { rows -> rows.map { it.toModel() } }

    override fun observeCourse(id: Long): Flow<CourseDetails?> =
        dao.observeCourse(id).map { it?.toModel() }

    override fun observeSettings(): Flow<SemesterSettings> =
        dao.observeSettings().map { it?.toModel() ?: DEFAULT_SETTINGS }

    override suspend fun snapshotCourses(): List<CourseDetails> =
        dao.getAllCourses().map { it.toModel() }

    override suspend fun seedIfMissing() {
        database.withTransaction {
            if (dao.getMetaValue(SEED_VERSION_KEY) == null) {
                if (dao.getSettings() == null) dao.upsertSettings(DEFAULT_SETTINGS.toEntity())
                dao.upsertMeta(AppMetaEntity(SEED_VERSION_KEY, "blank-1"))
            }
        }
    }

    override suspend fun replaceCourse(draft: CourseDraft): Long = database.withTransaction {
        val entity = CourseEntity(
            id = draft.id ?: 0,
            name = draft.name,
            code = draft.code,
            teachers = draft.teachers,
            note = draft.note,
        )
        val courseId = if (draft.id == null) {
            dao.insertCourse(entity)
        } else {
            dao.updateCourse(entity)
            draft.id
        }
        dao.deleteMeetingsForCourse(courseId)
        if (draft.meetings.isNotEmpty()) {
            dao.insertMeetings(draft.meetings.map { meeting ->
                MeetingEntity(
                    courseId = courseId,
                    weekday = meeting.weekday,
                    startPeriod = meeting.startPeriod,
                    endPeriod = meeting.endPeriod,
                    weekMask = meeting.weekMask,
                    location = meeting.location,
                )
            })
        }
        courseId
    }

    override suspend fun deleteCourse(id: Long) {
        database.withTransaction { dao.deleteCourseById(id) }
    }

    override suspend fun updateSettings(settings: SemesterSettings) {
        dao.upsertSettings(settings.toEntity())
    }

    override suspend fun resetToSeed() {
        database.withTransaction {
            dao.clearMeetings()
            dao.clearCourses()
            dao.clearMeta()
            insertSeed()
        }
    }

    override fun observeImportBackupAvailable(): Flow<Boolean> = dao.observeMetaValue(BACKUP_KEY).map { it != null }

    override suspend fun importCourses(courses: List<CourseDraft>) {
        database.withTransaction {
            val previous = dao.getAllCourses().map { it.toModel() }
            val settings = dao.getSettings()?.toModel() ?: DEFAULT_SETTINGS
            dao.upsertMeta(AppMetaEntity(BACKUP_KEY,ImportBackup.encode(previous,settings)))
            dao.clearMeetings()
            dao.clearCourses()
            courses.forEach { replaceCourse(it.copy(id=null)) }
        }
    }

    override suspend fun restoreImportBackup() {
        database.withTransaction {
            val text = dao.getMetaValue(BACKUP_KEY) ?: error("没有可恢复的导入前备份")
            val (courses,settings)=ImportBackup.decode(text)
            dao.clearMeetings()
            dao.clearCourses()
            courses.forEach { replaceCourse(it) }
            dao.upsertSettings(settings.toEntity())
            dao.deleteMetaValue(BACKUP_KEY)
        }
    }

    private suspend fun insertSeed() {
        dao.upsertSettings(DEFAULT_SETTINGS.toEntity())
        SeedCatalog.courses.forEach { seedCourse ->
            val courseId = dao.insertCourse(
                CourseEntity(
                    name = seedCourse.name,
                    code = seedCourse.code,
                    teachers = seedCourse.teachers,
                ),
            )
            if (seedCourse.meetings.isNotEmpty()) {
                dao.insertMeetings(seedCourse.meetings.map { seedMeeting ->
                    MeetingEntity(
                        courseId = courseId,
                        weekday = seedMeeting.weekday,
                        startPeriod = seedMeeting.startPeriod,
                        endPeriod = seedMeeting.endPeriod,
                        weekMask = seedMeeting.weekMask,
                        location = seedMeeting.location,
                    )
                })
            }
        }
        dao.upsertMeta(AppMetaEntity(SEED_VERSION_KEY, "1"))
    }

    private fun CourseWithMeetings.toModel() = CourseDetails(
        course = Course(course.id, course.name, course.code, course.teachers, course.note),
        meetings = meetings.map { meeting ->
            Meeting(
                meeting.id,
                meeting.courseId,
                meeting.weekday,
                meeting.startPeriod,
                meeting.endPeriod,
                meeting.weekMask,
                meeting.location,
            )
        },
    )

    private fun SemesterSettingsEntity.toModel() = SemesterSettings(firstWeekMonday, totalWeeks)
    private fun SemesterSettings.toEntity() = SemesterSettingsEntity(firstWeekMonday = firstWeekMonday, totalWeeks = totalWeeks)

    companion object {
        private const val SEED_VERSION_KEY = "seed_version"
        private const val BACKUP_KEY = "import_backup_v1"
        val DEFAULT_SETTINGS = SemesterSettings(LocalDate.of(2026, 8, 31), 16)
    }
}
