package com.songkeyuan.schedule.ui.settings

data class SettingsUiState(
    val importBackupAvailable: Boolean = false,
    val showRestoreBackupConfirmation: Boolean = false,
    val firstWeekMonday: String = "2026-08-31",
    val totalWeeks: String = "16",
    val isSaving: Boolean = false,
    val showResetConfirmation: Boolean = false,
    val message: String? = null,
)
