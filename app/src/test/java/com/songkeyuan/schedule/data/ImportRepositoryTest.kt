package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.data.importing.ImportedTimetable
import com.songkeyuan.schedule.data.importing.XlsxTimetableParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ImportRepositoryTest {
    @Test fun importsReplaceInsteadOfDuplicatingAndRestorePreviousCourses() = runTest {
        val store = InMemoryScheduleStore()
        val repository = RoomScheduleRepository(store)
        repository.ensureSeeded()
        repository.saveCourse(CourseDraft(name = "我自己的课"))
        val imported = javaClass.getResourceAsStream("/sample-timetable.xlsx")!!.use { XlsxTimetableParser().parse(it) }
        assertTrue(repository.importTimetable(imported).isSuccess)
        assertEquals(15,store.courseCount())
        assertEquals(15,store.meetingCount())
        assertTrue(repository.observeImportBackupAvailable().first())
        assertTrue(repository.importTimetable(imported).isSuccess)
        assertEquals(15,store.courseCount())
        assertTrue(repository.restoreImportBackup().isSuccess)
        // Latest import backs up the immediately preceding timetable.
        assertEquals(15,store.courseCount())
    }

    @Test fun invalidImportDoesNotReplaceExistingCourse() = runTest {
        val store = InMemoryScheduleStore()
        val repository = RoomScheduleRepository(store)
        repository.saveCourse(CourseDraft(name = "保留我"))
        val imported = ImportedTimetable(listOf(CourseDraft(name="", meetings=emptyList())),16)
        assertTrue(repository.importTimetable(imported).isFailure)
        assertEquals("保留我",repository.observePending().first().single().name)
    }

    @Test fun semesterTooShortBlocksImportAndPreservesOriginal()=runTest {
        val store=InMemoryScheduleStore();val repo=RoomScheduleRepository(store)
        repo.saveCourse(CourseDraft(name="保留我"))
        repo.updateSettings(SemesterSettings(java.time.LocalDate.of(2026,8,31),5))
        val imported=javaClass.getResourceAsStream("/sample-timetable.xlsx")!!.use { XlsxTimetableParser().parse(it) }
        assertTrue(repo.importTimetable(imported).isFailure)
        assertEquals("保留我",repo.observePending().first().single().name)
        assertFalse(repo.observeImportBackupAvailable().first())
    }

    @Test fun restoreRecoversManualDataAndSemesterSettings() = runTest {
        val store=InMemoryScheduleStore()
        val repo=RoomScheduleRepository(store)
        repo.saveCourse(CourseDraft(name="我的课程"))
        val original=SemesterSettings(java.time.LocalDate.of(2026,9,7),20)
        repo.updateSettings(original)
        val imported=ImportedTimetable(listOf(CourseDraft(name="导入课程")),1)
        assertTrue(repo.importTimetable(imported).isSuccess)
        repo.updateSettings(original.copy(totalWeeks=25))
        assertTrue(repo.restoreImportBackup().isSuccess)
        assertEquals("我的课程",repo.observePending().first().single().name)
        assertEquals(original,repo.observeSettings().first())
        assertFalse(repo.observeImportBackupAvailable().first())
    }
}
