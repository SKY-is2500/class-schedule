package com.songkeyuan.schedule.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songkeyuan.schedule.data.ScheduledCourse
import com.songkeyuan.schedule.domain.PeriodTime
import com.songkeyuan.schedule.domain.WeekMask
import java.time.LocalDate

@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectDay: (Int) -> Unit,
    onCourseClick: (Long) -> Unit,
    onAddCourse: () -> Unit,
    onRetry: () -> Unit,
    onViewModeChange: (ScheduleViewMode) -> Unit = {},
    onImport: () -> Unit = {},
    onToday: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WeekHeader(state, onPreviousWeek, onNextWeek)
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                FilterChip(selected=state.viewMode==ScheduleViewMode.Daily,onClick={onViewModeChange(ScheduleViewMode.Daily)},label={Text("每日")})
                FilterChip(selected=state.viewMode==ScheduleViewMode.Weekly,onClick={onViewModeChange(ScheduleViewMode.Weekly)},label={Text("每周总览")})
                Spacer(Modifier.weight(1f))
                TextButton(onClick=onToday) { Text("今天") }
                TextButton(onClick=onImport) { Text("导入") }
            }
            if (state.viewMode==ScheduleViewMode.Daily) DaySelector(state.weekMonday, state.selectedWeekday, onSelectDay)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.errorMessage)
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("重试") }
                    }
                }
                state.viewMode==ScheduleViewMode.Weekly -> WeekScheduleGrid(state,onCourseClick)
                state.courses.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("今天没有已安排的课程", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick=onImport) { Text("从 Excel 导入课表") }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.courses, key = { "${it.course.id}-${it.meeting.id}" }) { course ->
                        CourseCard(course, state.settings.totalWeeks) { onCourseClick(course.course.id) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddCourse,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).semantics { contentDescription="新建课程" },
        ) { Text("＋", style = MaterialTheme.typography.headlineSmall) }
    }
}

@Composable
fun WeekHeader(state: ScheduleUiState, onPreviousWeek: () -> Unit, onNextWeek: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousWeek, enabled = state.selectedWeek > 1) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 ${state.selectedWeek} 周", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(dateRange(state.weekMonday), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onNextWeek, enabled = state.selectedWeek < state.settings.totalWeeks) { Text("›", style = MaterialTheme.typography.headlineMedium) }
        }
    }
}

@Composable
fun DaySelector(weekMonday: LocalDate, selectedWeekday: Int, onSelectDay: (Int) -> Unit) {
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        names.forEachIndexed { index, name ->
            val weekday = index + 1
            val selected = weekday == selectedWeekday
            Surface(
                onClick = { onSelectDay(weekday) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            ) {
                Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("周$name", style = MaterialTheme.typography.labelMedium)
                    Text("${weekMonday.plusDays(index.toLong()).dayOfMonth}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CourseCard(item: ScheduledCourse, totalWeeks: Int, onClick: () -> Unit) {
    val time = PeriodTime.forPeriods(item.meeting.startPeriod, item.meeting.endPeriod)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time.start.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box(Modifier.padding(vertical = 6.dp).size(8.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {}
                }
                Text(time.endInclusive.toString(), style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.padding(start = 18.dp)) {
                Text(item.course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("第${item.meeting.startPeriod}–${item.meeting.endPeriod}节 · ${item.meeting.location.ifBlank { "地点待定" }}")
                Text("${WeekMask.format(item.meeting.weekMask, totalWeeks)} · ${item.course.teachers.ifBlank { "教师待定" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun dateRange(monday: LocalDate): String {
    val sunday = monday.plusDays(6)
    return "${monday.monthValue}月${monday.dayOfMonth}日–${sunday.monthValue}月${sunday.dayOfMonth}日"
}
