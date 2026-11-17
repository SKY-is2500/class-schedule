package com.songkeyuan.schedule.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songkeyuan.schedule.data.CourseDraft
import com.songkeyuan.schedule.data.SaveOutcome
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.domain.MeetingDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseEditorViewModel(
    private val repository: ScheduleRepository,
    courseId: Long?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState(courseId = courseId))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(totalWeeks = settings.totalWeeks)
            }
        }
        if (courseId != null) {
            viewModelScope.launch {
                repository.observeCourse(courseId).collect { details ->
                    if (details != null) {
                        _uiState.value = _uiState.value.copy(
                            name = details.course.name,
                            code = details.course.code,
                            teachers = details.course.teachers,
                            note = details.course.note,
                            meetings = details.meetings.map { meeting ->
                                MeetingDraft(meeting.id, meeting.courseId, meeting.weekday, meeting.startPeriod, meeting.endPeriod, meeting.weekMask, meeting.location)
                            },
                        )
                    }
                }
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, errorMessage = null) }
    fun setCode(value: String) = update { copy(code = value, errorMessage = null) }
    fun setTeachers(value: String) = update { copy(teachers = value, errorMessage = null) }
    fun setNote(value: String) = update { copy(note = value, errorMessage = null) }

    fun addMeeting(meeting: MeetingDraft? = null) = update {
        val default = MeetingDraft(
            weekday = 1,
            startPeriod = 1,
            endPeriod = 2,
            weekMask = if (totalWeeks >= 1) (1L shl totalWeeks) - 1 else 0L,
        )
        copy(meetings = meetings + (meeting ?: default), errorMessage = null)
    }

    fun updateMeeting(index: Int, meeting: MeetingDraft) = update {
        copy(meetings = meetings.mapIndexed { i, item -> if (i == index) meeting else item }, errorMessage = null)
    }

    fun removeMeeting(index: Int) = update {
        copy(meetings = meetings.filterIndexed { i, _ -> i != index }, errorMessage = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入课程名称")
            return
        }
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.saveCourse(state.toDraft())) {
                is SaveOutcome.Success -> _uiState.value = _uiState.value.copy(isSaving = false, finished = true)
                is SaveOutcome.Invalid -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.messages.joinToString("；"))
                is SaveOutcome.Conflict -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message())
                is SaveOutcome.Failed -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun requestDelete() = update { copy(showDeleteConfirmation = true) }
    fun dismissDelete() = update { copy(showDeleteConfirmation = false) }

    fun confirmDelete() {
        val id = _uiState.value.courseId ?: return
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = false, isSaving = true)
        viewModelScope.launch {
            repository.deleteCourse(id)
                .onSuccess { _uiState.value = _uiState.value.copy(isSaving = false, finished = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "删除失败") }
        }
    }

    private fun update(block: EditorUiState.() -> EditorUiState) {
        _uiState.value = _uiState.value.block()
    }

    private fun EditorUiState.toDraft() = CourseDraft(courseId, name, code, teachers, note, meetings)

    private fun SaveOutcome.Conflict.message(): String {
        val weekdayName = listOf("一", "二", "三", "四", "五", "六", "日").getOrElse(weekday - 1) { "?" }
        val periodText = if (periods.first == periods.last) "第${periods.first}节" else "第${periods.first}–${periods.last}节"
        val weekText = weeks.sorted().joinToString("、") { "第${it}周" }
        return "与“$courseName”冲突：周$weekdayName$periodText，$weekText"
    }
}
