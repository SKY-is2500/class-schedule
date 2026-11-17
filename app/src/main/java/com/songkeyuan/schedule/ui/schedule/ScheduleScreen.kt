package com.songkeyuan.schedule.ui.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songkeyuan.schedule.data.ScheduledCourse
import com.songkeyuan.schedule.domain.PeriodTime
import com.songkeyuan.schedule.domain.WeekMask
import com.songkeyuan.schedule.ui.AppIcon
import com.songkeyuan.schedule.ui.AppIconType
import com.songkeyuan.schedule.ui.theme.courseColors
import java.time.LocalDate
import java.time.LocalDateTime

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
    val now = rememberScheduleClock()
    val fontScale = LocalDensity.current.fontScale
    var expandedWeekGrid by rememberSaveable { mutableStateOf(fontScale>1.3f) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WeekHeader(state, onPreviousWeek, onNextWeek, onToday)
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    ScheduleViewMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected=state.viewMode==mode, onClick={onViewModeChange(mode)},
                            shape=SegmentedButtonDefaults.itemShape(index,2),
                            label={Text(if(mode==ScheduleViewMode.Daily) "每日" else "每周总览")},
                        )
                    }
                }
                TextButton(onClick=onImport, modifier=Modifier.padding(start=8.dp)) { Text("导入") }
            }
            AnimatedVisibility(state.viewMode==ScheduleViewMode.Daily, enter=expandVertically(tween(180))+fadeIn(tween(180)), exit=shrinkVertically(tween(180))+fadeOut(tween(120))) {
                DaySelector(state.weekMonday, state.selectedWeekday, onSelectDay, now.toLocalDate())
            }
            AnimatedContent(
                targetState=state,
                contentKey={Triple(it.viewMode,it.selectedWeek,it.selectedWeekday)},
                modifier=Modifier.fillMaxWidth().weight(1f),
                transitionSpec={
                    if(targetState.selectedWeek!=initialState.selectedWeek) {
                        val direction=if(targetState.selectedWeek>initialState.selectedWeek) 1 else -1
                        (slideInHorizontally(tween(200)) { it/8*direction }+fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally(tween(150)) { -it/8*direction }+fadeOut(tween(150)))
                    } else fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                }, label="课表切换",
            ) { display ->
                when {
                    display.isLoading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
                    display.errorMessage!=null -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(display.errorMessage)
                            Button(onClick=onRetry,modifier=Modifier.padding(top=12.dp)) { Text("重试") }
                        }
                    }
                    display.viewMode==ScheduleViewMode.Weekly -> WeekScheduleGrid(display,onCourseClick,expandedWeekGrid,{expandedWeekGrid=it},now)
                    else -> DailyCourses(display,now,onCourseClick,onImport)
                }
            }
        }
        FloatingActionButton(
            onClick=onAddCourse,
            containerColor=MaterialTheme.colorScheme.primary,
            contentColor=MaterialTheme.colorScheme.onPrimary,
            modifier=Modifier.align(Alignment.BottomEnd).padding(20.dp).semantics { contentDescription="新建课程" },
        ) { AppIcon(AppIconType.Add) }
    }
}

@Composable
private fun DailyCourses(state: ScheduleUiState, now: LocalDateTime, onCourseClick:(Long)->Unit, onImport:()->Unit) {
    val date=state.weekMonday.plusDays((state.selectedWeekday-1).toLong())
    val focus=dayFocus(state.courses,date,now)
    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(start=16.dp,top=8.dp,end=16.dp,bottom=96.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),
    ) {
        item(key="summary") {
            Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(focus.label,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    focus.course?.let { item ->
                        val time=PeriodTime.forPeriods(item.meeting.startPeriod,item.meeting.endPeriod)
                        Text("${time.start} · ${item.course.name}",style=MaterialTheme.typography.titleMedium)
                        Text(item.meeting.location.ifBlank { "地点待定" },style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } ?: Text(if(state.courses.isEmpty()) "留一点时间给自己，也可以添加或导入课程。" else "安排已记录，按自己的节奏继续。",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if(state.courses.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                AppIcon(AppIconType.Calendar,modifier=Modifier.size(36.dp))
                Text("这天没有已安排的课程",modifier=Modifier.padding(top=12.dp),style=MaterialTheme.typography.titleMedium)
                TextButton(onClick=onImport) { Text("从 Excel 导入课表") }
            }
        } else {
            item { Text("当天课程 · ${state.courses.size} 次",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.courses,key={"${it.course.id}-${it.meeting.id}"}) { item ->
                Box(Modifier.animateItem()) { CourseCard(item,state.settings.totalWeeks) { onCourseClick(item.course.id) } }
            }
        }
    }
}

@Composable
fun WeekHeader(state:ScheduleUiState,onPreviousWeek:()->Unit,onNextWeek:()->Unit,onToday:()->Unit={}) {
    Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp,top=6.dp,bottom=4.dp),verticalAlignment=Alignment.CenterVertically) {
        AnimatedContent(targetState=state.selectedWeek to state.weekMonday,modifier=Modifier.weight(1f),transitionSpec={fadeIn(tween(180)) togetherWith fadeOut(tween(120))},label="教学周") { (week,monday) ->
            Column {
                Text("第 $week 周",style=MaterialTheme.typography.titleLarge)
                Text(dateRange(monday),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick=onPreviousWeek,enabled=state.selectedWeek>1) { AppIcon(AppIconType.Previous,description="上一周") }
        IconButton(onClick=onNextWeek,enabled=state.selectedWeek<state.settings.totalWeeks) { AppIcon(AppIconType.Next,description="下一周") }
        TextButton(onClick=onToday) { Text("今天") }
    }
}

@Composable
fun DaySelector(weekMonday:LocalDate,selectedWeekday:Int,onSelectDay:(Int)->Unit,today:LocalDate=LocalDate.now()) {
    val names=listOf("一","二","三","四","五","六","日")
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { index,name ->
            val selected=index+1==selectedWeekday
            val isToday=weekMonday.plusDays(index.toLong())==today
            val color by animateColorAsState(if(selected) MaterialTheme.colorScheme.primary else Color.Transparent,tween(180),label="选中日期")
            Surface(onClick={onSelectDay(index+1)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=color,
                contentColor=if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("周$name",style=MaterialTheme.typography.labelSmall)
                    Text("${weekMonday.plusDays(index.toLong()).dayOfMonth}",style=MaterialTheme.typography.titleMedium)
                    Box(Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(if(isToday) (if(selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary) else Color.Transparent))
                }
            }
        }
    }
}

@Composable
fun CourseCard(item:ScheduledCourse,totalWeeks:Int,onClick:()->Unit) {
    val time=PeriodTime.forPeriods(item.meeting.startPeriod,item.meeting.endPeriod)
    val colors=courseColors(item.course)
    Card(onClick=onClick,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=colors.container,contentColor=colors.ink)) {
        Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(48.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(time.start.toString(),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                Box(Modifier.padding(vertical=7.dp).width(2.dp).height(20.dp).background(colors.accent.copy(alpha=.45f)))
                Text(time.endInclusive.toString(),style=MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(item.course.name,style=MaterialTheme.typography.titleMedium)
                Text("第${item.meeting.startPeriod}–${item.meeting.endPeriod}节 · ${item.meeting.location.ifBlank { "地点待定" }}",style=MaterialTheme.typography.bodyMedium)
                Text("${WeekMask.format(item.meeting.weekMask,totalWeeks)} · ${item.course.teachers.ifBlank { "教师待定" }}",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun dateRange(monday:LocalDate):String {
    val sunday=monday.plusDays(6)
    return "${monday.monthValue}月${monday.dayOfMonth}日–${sunday.monthValue}月${sunday.dayOfMonth}日"
}
