package com.songkeyuan.schedule.ui.pending

import com.songkeyuan.schedule.MainDispatcherRule
import com.songkeyuan.schedule.data.InMemoryScheduleStore
import com.songkeyuan.schedule.data.RoomScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsThreeInitialPendingCourses() = runTest(mainDispatcherRule.dispatcher) {
        val repository = RoomScheduleRepository(InMemoryScheduleStore()).also { it.ensureSeeded() }
        val viewModel = PendingViewModel(repository)
        advanceUntilIdle()
        assertEquals(listOf("形势与政策", "认知实习", "软件工程课程设计"), viewModel.uiState.value.courses.map { it.name })
    }
}
