package com.songkeyuan.schedule.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_meta",
    indices = [Index(value = ["key"], unique = true)],
)
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)
