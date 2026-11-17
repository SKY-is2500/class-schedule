package com.songkeyuan.schedule.data

import com.songkeyuan.schedule.domain.MeetingDraft
import com.songkeyuan.schedule.domain.WeekMask
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScheduleRepositoryTest {
    private lateinit var store: InMemoryScheduleStore
    private lateinit var repository: ScheduleRepository

    @Before
    fun setUp() {
        store = InMemoryScheduleStore()
        repository = RoomScheduleRepository(store)
    }

    @Test
    fun ensureSeeded_runsOnceEvenAfterAllCoursesAreDeleted() = runTest {
        repository.ensureSeeded()
        assertEquals(15, store.courseCount())
        store.clearCoursesOnly()
        repository.ensureSeeded()
        assertEquals(0, store.courseCount())
    }

    @Test
    fun observeDay_filtersByWeekAndSortsByStartPeriod() = runTest {
        repository.ensureSeeded()
        val mondayWeekTwo = repository.observeDay(week = 2, weekday = 1).first()
        assertEquals(listOf("机器学习"), mondayWeekTwo.map { it.course.name })
        val mondayWeekTen = repository.observeDay(week = 10, weekday = 1).first()
        assertEquals(listOf("机器学习", "生涯规划与职业发展Ⅱ"), mondayWeekTen.map { it.course.name })
    }

    @Test
    fun pendingListContainsOnlyCoursesWithoutMeetings() = runTest {
        repository.ensureSeeded()
        assertEquals(
            listOf("形势与政策", "认知实习", "软件工程课程设计"),
            repository.observePending().first().map { it.name },
        )
    }

    @Test
    fun conflictingSaveReturnsConflictWithoutWriting() = runTest {
        repository.ensureSeeded()
        val before = store.courseCount()
        val result = repository.saveCourse(
            CourseDraft(
                name = "冲突课程",
                code = "CONFLICT",
                teachers = "测试教师",
                meetings = listOf(
                    MeetingDraft(
                        weekday = 5,
                        startPeriod = 1,
                        endPeriod = 2,
                        weekMask = WeekMask.fromWeeks(setOf(2), 16),
                    ),
                ),
            ),
        )
        assertTrue(result is SaveOutcome.Conflict)
        assertEquals(before, store.courseCount())
    }

    @Test
    fun resetDiscardsEditsAndRestoresDefaults() = runTest {
        repository.ensureSeeded()
        repository.updateSettings(SemesterSettings(LocalDate.of(2026, 9, 14), 20))
        repository.resetToSeed()
        assertEquals(15, store.courseCount())
        assertEquals(14, store.meetingCount())
        assertEquals(
            SemesterSettings(LocalDate.of(2026, 8, 31), 16),
            repository.observeSettings().first(),
        )
    }
}
