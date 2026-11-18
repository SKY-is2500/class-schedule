package com.songkeyuan.schedule

import android.content.Context
import androidx.room.Room
import com.songkeyuan.schedule.data.RoomScheduleRepository
import com.songkeyuan.schedule.data.RoomScheduleStore
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.data.local.AppDatabase
import java.time.LocalDate

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "course-schedule.db",
    ).build()

    private val store = RoomScheduleStore(database)
    val repository: ScheduleRepository = RoomScheduleRepository(store)

    internal fun observeScheduleChanges() =
        database.invalidationTracker.createFlow("courses", "meetings", "semester_settings")

    internal suspend fun readScheduleFor(date: LocalDate) = store.readScheduleFor(date)
}
