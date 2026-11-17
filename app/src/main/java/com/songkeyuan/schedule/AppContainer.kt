package com.songkeyuan.schedule

import android.content.Context
import androidx.room.Room
import com.songkeyuan.schedule.data.RoomScheduleRepository
import com.songkeyuan.schedule.data.RoomScheduleStore
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.data.local.AppDatabase

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "course-schedule.db",
    ).build()

    val repository: ScheduleRepository = RoomScheduleRepository(RoomScheduleStore(database))
}
