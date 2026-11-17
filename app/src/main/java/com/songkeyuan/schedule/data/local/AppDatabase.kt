package com.songkeyuan.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CourseEntity::class,
        MeetingEntity::class,
        SemesterSettingsEntity::class,
        AppMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LocalDateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
