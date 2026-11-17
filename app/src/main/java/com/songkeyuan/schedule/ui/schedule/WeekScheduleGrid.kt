package com.songkeyuan.schedule.ui.schedule

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songkeyuan.schedule.domain.PeriodTime
import com.songkeyuan.schedule.ui.theme.courseColors
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun WeekScheduleGrid(state:ScheduleUiState,onCourseClick:(Long)->Unit,expanded:Boolean,onExpandedChange:(Boolean)->Unit,now:LocalDateTime=LocalDateTime.now()) {
    val rowHeight=80.dp
    val fontScale=LocalDensity.current.fontScale
    val horizontal=rememberScrollState()
    val vertical=rememberScrollState()
    val days=listOf("一","二","三","四","五","六","日")
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("本周 ${state.weekCourses.size} 次课程",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            TextButton(onClick={onExpandedChange(!expanded)}) { Text(if(expanded) "显示全周" else "放大课表") }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val labelWidth=44.dp
            val colWidth=if(expanded) (128*fontScale.coerceAtLeast(1f)).dp else (maxWidth-labelWidth)/7
            val gridLine=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.55f)
            val todayColor=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.28f)
            val markerColor=MaterialTheme.colorScheme.error
            Column {
                // Independent horizontal axis stays visible while the periods scroll.
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(MaterialTheme.colorScheme.background)) {
                    Box(Modifier.width(labelWidth).fillMaxHeight(),contentAlignment=Alignment.Center) { Text("节次",style=MaterialTheme.typography.labelSmall) }
                    Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                        days.forEachIndexed { index,name ->
                            val date=state.weekMonday.plusDays(index.toLong())
                            val today=date==now.toLocalDate()
                            Column(Modifier.width(colWidth).heightIn(min=50.dp).padding(vertical=8.dp).background(if(today) todayColor else MaterialTheme.colorScheme.background),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                Text("周$name",style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold,color=if(today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                Text("${date.monthValue}/${date.dayOfMonth}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                HorizontalDivider(color=gridLine)
                Row(Modifier.fillMaxWidth().verticalScroll(vertical).padding(bottom=96.dp)) {
                    // This column participates in vertical scrolling only.
                    Column(Modifier.width(labelWidth)) {
                        (1..12).forEach { period ->
                            Column(Modifier.height(rowHeight).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                Text("$period",style=MaterialTheme.typography.labelLarge)
                                Text(PeriodTime.forPeriod(period).start.toString(),fontSize=9.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)
                            }
                        }
                    }
                    Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                        (1..7).forEach { day ->
                            val isToday=state.weekMonday.plusDays((day-1).toLong())==now.toLocalDate()
                            Box(Modifier.width(colWidth).height(rowHeight*12).drawBehind {
                                if(isToday) drawRect(todayColor)
                                val height=rowHeight.toPx()
                                repeat(13) { drawLine(gridLine,Offset(0f,it*height),Offset(size.width,it*height),1.dp.toPx()) }
                                drawLine(gridLine,Offset.Zero,Offset(0f,size.height),1.dp.toPx())
                            }) {
                                layoutDayCourses(state.weekCourses.filter { it.meeting.weekday==day }).forEach { placement ->
                                    val item=placement.item
                                    val width=colWidth/placement.laneCount
                                    val colors=courseColors(item.course)
                                    Card(
                                        onClick={onCourseClick(item.course.id)},shape=RoundedCornerShape(if(expanded) 12.dp else 8.dp),
                                        modifier=Modifier.offset(x=width*placement.lane,y=rowHeight*(item.meeting.startPeriod-1)).width(width).height(rowHeight*(item.meeting.endPeriod-item.meeting.startPeriod+1)).padding(2.dp)
                                            .semantics { contentDescription="${item.course.name}，周${days[day-1]}，第${item.meeting.startPeriod}至${item.meeting.endPeriod}节，${item.meeting.location}" },
                                        colors=CardDefaults.cardColors(containerColor=colors.container,contentColor=colors.ink),
                                    ) {
                                        Column(Modifier.padding(horizontal=if(expanded) 9.dp else 4.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                            Text(item.course.name,fontSize=if(expanded) 14.sp else 11.sp,lineHeight=if(expanded) 20.sp else 15.sp,fontWeight=FontWeight.SemiBold,maxLines=if(expanded) 4 else 6,overflow=TextOverflow.Ellipsis)
                                            val room=if(expanded) item.meeting.location else item.meeting.location.replace("公共教学楼","").replace("12号楼","12#")
                                            Text(room.ifBlank { "地点待定" },fontSize=if(expanded) 12.sp else 10.sp,lineHeight=14.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
                                            if(expanded) {
                                                Text("${item.meeting.startPeriod}–${item.meeting.endPeriod}节",style=MaterialTheme.typography.labelSmall)
                                                if(item.course.teachers.isNotBlank()) Text(item.course.teachers,style=MaterialTheme.typography.bodySmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                                if(isToday) {
                                    val period=(1..12).firstOrNull { now.toLocalTime()>=PeriodTime.forPeriod(it).start && now.toLocalTime()<PeriodTime.forPeriod(it).endInclusive }
                                    if(period!=null) {
                                        val range=PeriodTime.forPeriod(period)
                                        val fraction=ChronoUnit.SECONDS.between(range.start,now.toLocalTime()).toFloat()/ChronoUnit.SECONDS.between(range.start,range.endInclusive)
                                        Box(Modifier.offset(y=rowHeight*(period-1+fraction)).fillMaxWidth().height(2.dp).background(markerColor))
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
