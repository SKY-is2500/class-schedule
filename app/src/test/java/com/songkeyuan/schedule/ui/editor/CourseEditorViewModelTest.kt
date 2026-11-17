package com.songkeyuan.schedule.ui.editor

import com.songkeyuan.schedule.MainDispatcherRule
import com.songkeyuan.schedule.data.Course
import com.songkeyuan.schedule.data.CourseDetails
import com.songkeyuan.schedule.data.CourseDraft
import com.songkeyuan.schedule.data.SaveOutcome
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.data.ScheduledCourse
import com.songkeyuan.schedule.data.SemesterSettings
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CourseEditorViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun blankNameDoesNotSave() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeEditorRepository()
        val viewModel = CourseEditorViewModel(repository, courseId = null)
        viewModel.setName("   ")
        viewModel.save()
        advanceUntilIdle()
        assertEquals("请输入课程名称", viewModel.uiState.value.errorMessage)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun courseWithoutMeetingsSavesAsPending() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeEditorRepository()
        val viewModel = CourseEditorViewModel(repository, courseId = null)
        viewModel.setName("自定义课程")
        viewModel.save()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.finished)
        assertEquals(emptyList<com.songkeyuan.schedule.domain.MeetingDraft>(), repository.lastDraft?.meetings)
    }
}

private class FakeEditorRepository : ScheduleRepository {
    var saveCalls = 0
    var lastDraft: CourseDraft? = null
    override fun observeDay(week: Int, weekday: Int): Flow<List<ScheduledCourse>> = flowOf(emptyList())
    override fun observePending(): Flow<List<Course>> = flowOf(emptyList())
    override fun observeCourse(id: Long): Flow<CourseDetails?> = MutableStateFlow(null)
    override fun observeSettings(): Flow<SemesterSettings> = flowOf(SemesterSettings(LocalDate.of(2026, 8, 31), 16))
    override suspend fun ensureSeeded() = Unit
    override suspend fun saveCourse(draft: CourseDraft): SaveOutcome {
        saveCalls++
        lastDraft = draft
        return SaveOutcome.Success(1)
    }
    override suspend fun deleteCourse(id: Long): Result<Unit> = Result.success(Unit)
    override suspend fun updateSettings(settings: SemesterSettings): Result<Unit> = Result.success(Unit)
    override suspend fun resetToSeed(): Result<Unit> = Result.success(Unit)
    override fun observeImportBackupAvailable(): Flow<Boolean> = flowOf(false)
    override suspend fun importTimetable(timetable: com.songkeyuan.schedule.data.importing.ImportedTimetable): Result<Unit> = Result.success(Unit)
    override suspend fun restoreImportBackup(): Result<Unit> = Result.success(Unit)
}
