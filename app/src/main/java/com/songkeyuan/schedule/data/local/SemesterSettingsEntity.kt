package com.songkeyuan.schedule.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "semester_settings")
data class SemesterSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val firstWeekMonday: LocalDate,
    val totalWeeks: Int,
)
