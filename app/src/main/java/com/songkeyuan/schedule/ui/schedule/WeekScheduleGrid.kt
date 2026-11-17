package com.songkeyuan.schedule.ui.schedule

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.songkeyuan.schedule.domain.PeriodTime

@Composable
fun WeekScheduleGrid(state: ScheduleUiState,onCourseClick: (Long)->Unit) {
    val rowHeight=72.dp
    var expanded by rememberSaveable { mutableStateOf(false) }
    val days=listOf("一","二","三","四","五","六","日")
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("本周 ${state.weekCourses.size} 次课程",style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "显示全周" else "放大课表") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val labelWidth=36.dp
        val colWidth=if(expanded) 128.dp else (maxWidth-labelWidth)/7
        Column(Modifier.horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()).padding(bottom=96.dp)) {
        Row {
            Box(Modifier.width(labelWidth).height(50.dp),contentAlignment=Alignment.Center) { Text("节次",style=MaterialTheme.typography.labelSmall) }
            days.forEachIndexed { i,name ->
                Column(Modifier.width(colWidth).height(50.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text("周$name",fontWeight=FontWeight.Bold,fontSize=12.sp)
                    val date=state.weekMonday.plusDays(i.toLong())
                    Text("${date.monthValue}/${date.dayOfMonth}",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row {
            Column(Modifier.width(labelWidth)) {
                (1..12).forEach { period ->
                    Column(Modifier.height(rowHeight).fillMaxWidth().border(0.5.dp,MaterialTheme.colorScheme.outlineVariant),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                        Text("$period",fontWeight=FontWeight.Bold)
                        Text(PeriodTime.forPeriod(period).start.toString(),fontSize=9.sp)
                    }
                }
            }
            (1..7).forEach { day ->
                Box(Modifier.width(colWidth).height(rowHeight*12)) {
                    Column { repeat(12) { Box(Modifier.width(colWidth).height(rowHeight).border(0.5.dp,MaterialTheme.colorScheme.outlineVariant)) } }
                    layoutDayCourses(state.weekCourses.filter { it.meeting.weekday==day }).forEach { placement ->
                        val item=placement.item
                        val width=colWidth/placement.laneCount
                        Card(onClick={ onCourseClick(item.course.id) },modifier=Modifier.offset(x=width*placement.lane,y=rowHeight*(item.meeting.startPeriod-1)).width(width).height(rowHeight*(item.meeting.endPeriod-item.meeting.startPeriod+1)).padding(3.dp),
                            colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(if(expanded) 7.dp else 3.dp)) {
                                Text(item.course.name,fontSize=if(expanded) 13.sp else 10.sp,lineHeight=if(expanded) 17.sp else 13.sp,fontWeight=FontWeight.Bold)
                                val room=if(expanded) item.meeting.location else item.meeting.location.replace("公共教学楼","").replace("12号楼","12#")
                                Text(room.ifBlank { "地点待定" },fontSize=if(expanded) 11.sp else 9.sp,lineHeight=12.sp)
                                if(expanded) {
                                    Text("${item.meeting.startPeriod}–${item.meeting.endPeriod}节",style=MaterialTheme.typography.labelSmall)
                                    if (item.course.teachers.isNotBlank()) Text(item.course.teachers,style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    }
}
