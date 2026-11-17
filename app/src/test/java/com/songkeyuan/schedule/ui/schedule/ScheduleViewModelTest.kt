package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.MainDispatcherRule
import com.songkeyuan.schedule.data.InMemoryScheduleStore
import com.songkeyuan.schedule.data.RoomScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: RoomScheduleRepository

    @Before
    fun setUp() = runTest(mainDispatcherRule.dispatcher) {
        repository = RoomScheduleRepository(InMemoryScheduleStore())
        repository.ensureSeeded()
    }

    @Test
    fun startsOnCalculatedCurrentWeekAndDay() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 9, 8) })
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.selectedWeek)
        assertEquals(2, viewModel.uiState.value.selectedWeekday)
    }

    @Test
    fun weekNavigationStaysWithinSemester() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 8, 1) })
        advanceUntilIdle()
        viewModel.previousWeek()
        assertEquals(1, viewModel.uiState.value.selectedWeek)
        repeat(20) { viewModel.nextWeek() }
        advanceUntilIdle()
        assertEquals(16, viewModel.uiState.value.selectedWeek)
    }

    @Test
    fun selectingTuesdayLoadsTuesdayCourses() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ScheduleViewModel(repository, today = { LocalDate.of(2026, 9, 7) })
        viewModel.selectWeekday(2)
        advanceUntilIdle()
        assertEquals(
            listOf("计算机操作系统", "Python程序设计语言", "Python程序设计语言实验"),
            viewModel.uiState.value.courses.map { it.course.name },
        )
    }

    @Test fun weeklyModeKeepsSelectedWeekAndIncludesSaturdayReschedule() = runTest(mainDispatcherRule.dispatcher) {
        val imported=javaClass.getResourceAsStream("/sample-timetable.xlsx")!!.use { com.songkeyuan.schedule.data.importing.XlsxTimetableParser().parse(it) }
        repository.importTimetable(imported)
        val model=ScheduleViewModel(repository,today={LocalDate.of(2026,10,5)})
        advanceUntilIdle()
        assertEquals(6,model.uiState.value.selectedWeek)
        model.selectViewMode(ScheduleViewMode.Weekly)
        assertEquals(6,model.uiState.value.selectedWeek)
        assertEquals("计算机网络安全",model.uiState.value.weekCourses.single { it.meeting.weekday==6 }.course.name)
        assertEquals(emptyList<String>(),model.uiState.value.weekCourses.filter { it.meeting.weekday==3 }.map { it.course.name })
        model.selectWeekday(6)
        model.selectViewMode(ScheduleViewMode.Daily)
        assertEquals("计算机网络安全",model.uiState.value.courses.single().course.name)
    }
}
