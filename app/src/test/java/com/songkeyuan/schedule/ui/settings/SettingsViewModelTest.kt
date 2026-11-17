package com.songkeyuan.schedule.ui.settings

import com.songkeyuan.schedule.MainDispatcherRule
import com.songkeyuan.schedule.data.InMemoryScheduleStore
import com.songkeyuan.schedule.data.RoomScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun rejectsWeeksOutsideOneToThirty() = runTest(mainDispatcherRule.dispatcher) {
        val repository = RoomScheduleRepository(InMemoryScheduleStore()).also { it.ensureSeeded() }
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()
        viewModel.setTotalWeeks("31")
        viewModel.save()
        advanceUntilIdle()
        assertEquals("总周数请输入 1–30", viewModel.uiState.value.message)
    }

    @Test
    fun resetRequiresConfirmationAndRestoresDefaults() = runTest(mainDispatcherRule.dispatcher) {
        val repository = RoomScheduleRepository(InMemoryScheduleStore()).also { it.ensureSeeded() }
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()
        viewModel.requestReset()
        assertTrue(viewModel.uiState.value.showResetConfirmation)
        viewModel.confirmReset()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showResetConfirmation)
        assertEquals("2026-08-31", viewModel.uiState.value.firstWeekMonday)
        assertEquals("16", viewModel.uiState.value.totalWeeks)
    }
}
