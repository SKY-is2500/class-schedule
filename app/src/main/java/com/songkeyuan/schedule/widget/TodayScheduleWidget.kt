package com.songkeyuan.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.songkeyuan.schedule.MainActivity
import com.songkeyuan.schedule.R
import com.songkeyuan.schedule.ScheduleApplication
import com.songkeyuan.schedule.data.DailySchedule
import com.songkeyuan.schedule.domain.PeriodTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class TodayScheduleWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                refreshFromBroadcast(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshFromBroadcast(context)
    }

    private fun refreshFromBroadcast(context: Context) {
        val pendingResult = goAsync()
        val application = context.applicationContext as ScheduleApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000L) { refresh(application) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val updateLock = Mutex()
        private val classTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

        internal suspend fun refresh(application: ScheduleApplication) = updateLock.withLock {
            val manager = AppWidgetManager.getInstance(application)
            val ids = manager.getAppWidgetIds(ComponentName(application, TodayScheduleWidget::class.java))
            if (ids.isEmpty()) return@withLock

            val today = LocalDate.now()
            val schedule = try {
                application.container.readScheduleFor(today)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("TodayScheduleWidget", "Could not load today's schedule", error)
                null
            }
            manager.updateAppWidget(ids, render(application, today, schedule))
        }

        private fun render(context: Context, date: LocalDate, schedule: DailySchedule?): RemoteViews {
            val widget = RemoteViews(context.packageName, R.layout.today_schedule_widget)
            widget.removeAllViews(R.id.widget_courses)
            widget.setViewVisibility(R.id.widget_more, View.GONE)
            val dateLabel = context.getString(R.string.widget_date, date.monthValue, date.dayOfMonth)
            val week = schedule?.week
            widget.setTextViewText(
                R.id.widget_date,
                if (week == null) dateLabel else context.getString(R.string.widget_date_and_week, dateLabel, week),
            )
            val openToday = Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_TODAY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            widget.setOnClickPendingIntent(
                android.R.id.background,
                PendingIntent.getActivity(context, 0, openToday, PendingIntent.FLAG_IMMUTABLE),
            )

            val status = when {
                schedule == null -> R.string.widget_load_error
                schedule.week == null -> R.string.widget_outside_semester
                schedule.courses.isEmpty() -> R.string.widget_empty
                else -> null
            }
            if (status != null) {
                widget.setTextViewText(R.id.widget_status, context.getText(status))
                widget.setViewVisibility(R.id.widget_status, View.VISIBLE)
                return widget
            }

            val courses = checkNotNull(schedule).courses
            val maxVisible = if (context.resources.configuration.fontScale >= 1.4f) 2 else 3
            for ((course, meeting) in courses.take(maxVisible)) {
                val time = PeriodTime.forPeriods(meeting.startPeriod, meeting.endPeriod)
                val location = meeting.location.ifBlank { context.getString(R.string.widget_location_unknown) }
                val row = RemoteViews(context.packageName, R.layout.today_schedule_widget_course)
                row.setTextViewText(R.id.widget_course_name, course.name)
                row.setTextViewText(
                    R.id.widget_course_details,
                    context.getString(
                        R.string.widget_course_details,
                        time.start.format(classTimeFormat),
                        time.endInclusive.format(classTimeFormat),
                        location,
                    ),
                )
                widget.addView(R.id.widget_courses, row)
            }
            widget.setViewVisibility(R.id.widget_status, View.GONE)
            val remaining = courses.size - maxVisible
            if (remaining > 0) {
                widget.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, remaining))
                widget.setViewVisibility(R.id.widget_more, View.VISIBLE)
            }
            return widget
        }
    }
}
