package com.songkeyuan.schedule.ui.pending

import com.songkeyuan.schedule.data.Course

data class PendingUiState(
    val courses: List<Course> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
