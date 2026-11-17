package com.songkeyuan.schedule.ui.importing

import com.songkeyuan.schedule.MainDispatcherRule
import com.songkeyuan.schedule.data.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    @get:Rule val rule=MainDispatcherRule()
    @Test fun previewDoesNotWriteUntilConfirmed()=runTest(rule.dispatcher) {
        val store=InMemoryScheduleStore(); val repo=RoomScheduleRepository(store)
        repo.saveCourse(CourseDraft(name="原来的课"))
        val vm=ImportViewModel(repo,rule.dispatcher)
        vm.readFile("sample.xlsx") { javaClass.getResourceAsStream("/sample-timetable.xlsx")!! }
        advanceUntilIdle()
        assertEquals(15,vm.uiState.value.preview?.courses?.size)
        assertEquals("原来的课",repo.observePending().first().single().name)
        vm.confirmImport()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.finished)
        assertEquals(15,store.courseCount())
    }
    @Test fun unreadableFilePreservesDataAndOffersError()=runTest(rule.dispatcher) {
        val store=InMemoryScheduleStore(); val repo=RoomScheduleRepository(store)
        repo.saveCourse(CourseDraft(name="原来的课"))
        val vm=ImportViewModel(repo,rule.dispatcher)
        vm.readFile("bad.xlsx") { error("读取失败") }
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.preview)
        assertFalse(vm.uiState.value.isReading)
        assertEquals(1,store.courseCount())
    }
}
