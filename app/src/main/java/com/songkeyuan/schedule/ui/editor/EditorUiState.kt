package com.songkeyuan.schedule.ui.editor

import com.songkeyuan.schedule.domain.MeetingDraft

data class EditorUiState(
    val courseId: Long? = null,
    val name: String = "",
    val code: String = "",
    val teachers: String = "",
    val note: String = "",
    val meetings: List<MeetingDraft> = emptyList(),
    val totalWeeks: Int = 16,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val finished: Boolean = false,
)
