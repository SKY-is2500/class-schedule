package com.songkeyuan.schedule.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.songkeyuan.schedule.data.local.AppDatabase
import com.songkeyuan.schedule.data.importing.ImportedTimetable
import com.songkeyuan.schedule.domain.MeetingDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImportStoreTest {
    private lateinit var db:AppDatabase
    private lateinit var repo:ScheduleRepository
    @Before fun setup() {
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        repo=RoomScheduleRepository(RoomScheduleStore(db))
    }
    @After fun close()=db.close()
    private fun imported()=ImportedTimetable(listOf(CourseDraft(name="周六课程",code="NEW",teachers="教师",meetings=listOf(MeetingDraft(weekday=6,startPeriod=3,endPeriod=4,weekMask=32L,location="A110")))),6)

    @Test fun firstLaunchIsBlankAndSecondLaunchKeepsUserCourses()=runBlocking {
        repo.ensureSeeded()
        assertTrue(db.scheduleDao().getAllCourses().isEmpty())
        repo.saveCourse(CourseDraft(name="自己的课"))
        repo.ensureSeeded()
        assertEquals("自己的课",repo.observePending().first().single().name)
    }
    @Test fun importAndBackupRoundTripUseRealRoom()=runBlocking {
        repo.ensureSeeded()
        repo.saveCourse(CourseDraft(name="原课",note="我的备注"))
        val settings=SemesterSettings(java.time.LocalDate.of(2026,9,7),20)
        repo.updateSettings(settings)
        assertTrue(repo.importTimetable(imported()).isSuccess)
        assertEquals(1,repo.observeDay(6,6).first().size)
        assertTrue(repo.observeDay(6,3).first().isEmpty())
        repo.updateSettings(settings.copy(totalWeeks=25))
        assertTrue(repo.restoreImportBackup().isSuccess)
        val restored=db.scheduleDao().getAllCourses().single()
        assertEquals("原课",restored.course.name)
        assertEquals("我的备注",restored.course.note)
        assertEquals(settings,repo.observeSettings().first())
        assertFalse(repo.observeImportBackupAvailable().first())
    }
    @Test fun failedTransactionRollsBackCoursesAndBackup()=runBlocking {
        repo.ensureSeeded()
        repo.saveCourse(CourseDraft(name="保留原课"))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER import_abort BEFORE INSERT ON courses WHEN NEW.name='坏课' BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        val result=repo.importTimetable(ImportedTimetable(listOf(CourseDraft(name="先写入的课"),CourseDraft(name="坏课")),1))
        assertTrue(result.isFailure)
        assertEquals("保留原课",repo.observePending().first().single().name)
        assertFalse(repo.observeImportBackupAvailable().first())
    }
    @Test fun repeatedImportReplacesAndNeverDuplicates()=runBlocking {
        repo.ensureSeeded()
        repeat(2) { assertTrue(repo.importTimetable(imported()).isSuccess) }
        assertEquals(1,db.scheduleDao().getAllCourses().size)
        assertEquals(1,db.scheduleDao().getAllMeetings().size)
    }
}
