package com.songkeyuan.schedule.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songkeyuan.schedule.data.*
import com.songkeyuan.schedule.domain.PeriodTime
import com.songkeyuan.schedule.domain.WeekMask
import com.songkeyuan.schedule.ui.theme.courseColors
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class DetailLoad(val details:CourseDetails?=null,val loading:Boolean=false,val error:String?=null)

@Composable
fun CourseDetailHost(repository:ScheduleRepository,courseId:Long,onDismiss:()->Unit,onEdit:()->Unit) {
    val flow=remember(repository,courseId) {
        repository.observeCourse(courseId).map { DetailLoad(details=it) }
            .catch { emit(DetailLoad(error="课程读取失败，请关闭后重试")) }
    }
    val result by flow.collectAsStateWithLifecycle(DetailLoad(loading=true))
    val settingsFlow=remember(repository) { repository.observeSettings() }
    val settings by settingsFlow.collectAsStateWithLifecycle(SemesterSettings(java.time.LocalDate.now(),30))
    LaunchedEffect(result) {
        if(!result.loading && result.details==null && result.error==null) onDismiss()
    }
    result.details?.let { CourseDetailSheet(it,settings.totalWeeks,onDismiss,onEdit) }
    if(result.loading || result.error!=null) DetailStatusSheet(result.error,onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailStatusSheet(error:String?,onDismiss:()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(error==null) CircularProgressIndicator() else Text(error)
            TextButton(onClick=onDismiss) { Text("关闭") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(details:CourseDetails,totalWeeks:Int,onDismiss:()->Unit,onEdit:()->Unit) {
    val colors=courseColors(details.course)
    val sheet=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    val scope=rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheet) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text("课程详情",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Text(details.course.name,style=MaterialTheme.typography.headlineSmall)
            if(details.course.code.isNotBlank()) Text(details.course.code,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(details.course.teachers.ifBlank { "教师待定" },style=MaterialTheme.typography.bodyLarge)
            if(details.meetings.isEmpty()) Text("时间待安排，可通过编辑添加上课安排。",color=MaterialTheme.colorScheme.onSurfaceVariant)
            details.meetings.sortedWith(compareBy({it.weekday},{it.startPeriod})).forEach { meeting ->
                val time=PeriodTime.forPeriods(meeting.startPeriod,meeting.endPeriod)
                Surface(color=colors.container,contentColor=colors.ink,shape=RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("周${listOf("一","二","三","四","五","六","日")[meeting.weekday-1]} · 第${meeting.startPeriod}–${meeting.endPeriod}节",style=MaterialTheme.typography.titleSmall)
                        Text("${time.start}–${time.endInclusive} · ${WeekMask.format(meeting.weekMask,totalWeeks)}",style=MaterialTheme.typography.bodySmall)
                        Text(meeting.location.ifBlank { "地点待定" },style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if(details.course.note.isNotBlank()) {
                Text("备注",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(details.course.note,style=MaterialTheme.typography.bodyMedium)
            }
            Button(onClick={scope.launch { sheet.hide(); onEdit() }},modifier=Modifier.fillMaxWidth()) { Text("编辑课程") }
        }
    }
}
