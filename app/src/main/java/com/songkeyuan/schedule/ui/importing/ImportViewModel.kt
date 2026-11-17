package com.songkeyuan.schedule.ui.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.data.importing.ImportedTimetable
import com.songkeyuan.schedule.data.importing.XlsxTimetableParser
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportUiState(
    val fileName: String = "",
    val isReading: Boolean = false,
    val isSaving: Boolean = false,
    val preview: ImportedTimetable? = null,
    val errorMessage: String? = null,
    val finished: Boolean = false,
)

class ImportViewModel(private val repository: ScheduleRepository,private val ioDispatcher: CoroutineDispatcher=Dispatchers.IO): ViewModel() {
    private val state=MutableStateFlow(ImportUiState())
    val uiState=state.asStateFlow()

    fun readFile(fileName: String,open: ()->InputStream) {
        if (state.value.isReading || state.value.isSaving) return
        state.value=ImportUiState(fileName=fileName,isReading=true)
        viewModelScope.launch {
            try {
                val result=withContext(ioDispatcher) { open().use { XlsxTimetableParser().parse(it) } }
                state.value=state.value.copy(isReading=false,preview=result)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                state.value=state.value.copy(isReading=false,errorMessage=error.message ?: "读取失败，请重新选择 Excel 文件")
            }
        }
    }

    fun confirmImport() {
        val preview=state.value.preview ?: return
        if (state.value.isSaving) return
        state.value=state.value.copy(isSaving=true,errorMessage=null)
        viewModelScope.launch {
            repository.importTimetable(preview)
                .onSuccess { state.value=state.value.copy(isSaving=false,finished=true) }
                .onFailure { state.value=state.value.copy(isSaving=false,errorMessage=it.message ?: "导入失败，原课表保持不变，请重试") }
        }
    }
}
