package com.songkeyuan.schedule.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ScheduleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scheduleDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun courseWithMeeting_isObserved() = runBlocking {
        val courseId = dao.insertCourse(CourseEntity(name = "测试课程", code = "TEST1001", teachers = "测试教师"))
        dao.insertMeeting(
            MeetingEntity(
                courseId = courseId,
                weekday = 1,
                startPeriod = 3,
                endPeriod = 4,
                weekMask = 3L,
                location = "测试教室",
            ),
        )

        val loaded = dao.observeCourse(courseId).first()
        assertNotNull(loaded)
        assertEquals("测试课程", loaded?.course?.name)
        assertEquals(1, loaded?.meetings?.size)
    }

    @Test
    fun courseWithoutMeeting_hasEmptyRelation() = runBlocking {
        val courseId = dao.insertCourse(CourseEntity(name = "待安排", code = "WAIT", teachers = "教师"))
        val loaded = dao.observeCourse(courseId).first()
        assertTrue(loaded?.meetings?.isEmpty() == true)
    }

    @Test
    fun deletingCourse_cascadesMeetings() = runBlocking {
        val courseId = dao.insertCourse(CourseEntity(name = "将删除", code = "DEL", teachers = "教师"))
        dao.insertMeeting(MeetingEntity(courseId = courseId, weekday = 2, startPeriod = 1, endPeriod = 2, weekMask = 1L, location = "A101"))
        dao.deleteCourseById(courseId)
        assertTrue(dao.getMeetingsForCourse(courseId).isEmpty())
    }

    @Test
    fun semesterSettings_roundTripLocalDate() = runBlocking {
        val settings = SemesterSettingsEntity(firstWeekMonday = LocalDate.of(2026, 8, 31), totalWeeks = 16)
        dao.upsertSettings(settings)
        assertEquals(settings, dao.getSettings())
    }
}
