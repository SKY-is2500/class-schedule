package com.songkeyuan.schedule.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        refreshFromBroadcast(context, appWidgetId)
    }

    private fun refreshFromBroadcast(context: Context, appWidgetId: Int? = null) {
        val pendingResult = goAsync()
        val application = context.applicationContext as ScheduleApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000L) { refresh(application, appWidgetId) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val updateLock = Mutex()
        private val classTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

        internal suspend fun refresh(application: ScheduleApplication, appWidgetId: Int? = null) = updateLock.withLock {
            val manager = AppWidgetManager.getInstance(application)
            val boundIds = manager.getAppWidgetIds(ComponentName(application, TodayScheduleWidget::class.java))
            val ids = if (appWidgetId == null) boundIds else boundIds.filter { it == appWidgetId }.toIntArray()
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
            for (id in ids) {
                val options = manager.getAppWidgetOptions(id)
                manager.updateAppWidget(id, sizedViews(application, today, schedule, options))
            }
        }

        private fun sizedViews(
            context: Context,
            date: LocalDate,
            schedule: DailySchedule?,
            options: Bundle,
        ): RemoteViews {
            val fontScale = context.resources.configuration.fontScale
            val courseCount = schedule?.courses.orEmpty().size
            fun renderFor(size: SizeF): RemoteViews {
                val layout = WidgetLayout.forSize(size.width, size.height, fontScale, courseCount)
                return render(context, date, schedule, layout)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizes = BundleCompat.getParcelableArrayList(
                    options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java,
                ).orEmpty().filter { it.width > 0f && it.height > 0f }
                if (sizes.isNotEmpty()) return RemoteViews(sizes.associateWith(::renderFor))
            }

            fun dimension(key: String, fallback: Float): Float =
                options.getInt(key).takeIf { it > 0 }?.toFloat() ?: fallback
            val minWidth = dimension(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220f)
            val minHeight = dimension(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 190f)
            val maxWidth = dimension(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth)
            val maxHeight = dimension(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
            return RemoteViews(
                renderFor(SizeF(maxWidth, minHeight)),
                renderFor(SizeF(minWidth, maxHeight)),
            )
        }

        private fun render(
            context: Context,
            date: LocalDate,
            schedule: DailySchedule?,
            layout: WidgetLayout,
        ): RemoteViews {
            val widget = RemoteViews(context.packageName, R.layout.today_schedule_widget)
            widget.removeAllViews(R.id.widget_courses)
            widget.setViewVisibility(R.id.widget_more, View.GONE)
            val dateLabel = context.getString(R.string.widget_date, date.monthValue, date.dayOfMonth)
            val week = schedule?.week
            widget.setTextViewText(
                R.id.widget_date,
                if (week != null && layout.showTeachingWeek) {
                    context.getString(R.string.widget_date_and_week, dateLabel, week)
                } else {
                    dateLabel
                },
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
            for ((course, meeting) in courses.take(layout.visibleCourses)) {
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
            if (layout.showOverflowCount) {
                val remaining = courses.size - layout.visibleCourses
                widget.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more, remaining))
                widget.setViewVisibility(R.id.widget_more, View.VISIBLE)
            }
            return widget
        }
    }
}
