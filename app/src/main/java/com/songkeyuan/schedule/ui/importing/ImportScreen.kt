package com.songkeyuan.schedule.ui.importing

import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.songkeyuan.schedule.domain.WeekMask
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(state: ImportUiState,onReadFile:(String,()->InputStream)->Unit,onConfirm:()->Unit,onBack:()->Unit) {
    BackHandler(enabled=state.isSaving) {}
    val context=LocalContext.current
    var confirm by remember { mutableStateOf(false) }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri!=null) {
            var name="课表.xlsx"
            runCatching {
                context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor ->
                    if (cursor.moveToFirst()) name=cursor.getString(0) ?: name
                }
            }
            onReadFile(name) { context.contentResolver.openInputStream(uri) ?: error("无法打开文件，请将 Excel 保存到手机后重试") }
        }
    }
    Scaffold(topBar={TopAppBar(title={Text("导入 Excel 课表")},navigationIcon={TextButton(onClick=onBack,enabled=!state.isSaving){Text("返回")}})}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item(key="stages") {
                val stage=if(state.isSaving) 2 else if(state.preview!=null) 1 else 0
                Surface(color=MaterialTheme.colorScheme.surfaceContainerLow,shape=RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        listOf("选择文件","核对预览","写入课表").forEachIndexed { index,label ->
                            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Surface(shape=RoundedCornerShape(10.dp),color=if(index<=stage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,contentColor=if(index<=stage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) {
                                    Box(Modifier.size(28.dp),contentAlignment=Alignment.Center) { Text("${index+1}",style=MaterialTheme.typography.labelLarge) }
                                }
                                Text(label,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Text("将教务系统导出的 .xlsx 文件保存到手机，再从这里选择。支持与示例相同的周课表格式，包含备注中的课程。")
                Text("第 1 周日期由学期设置决定，导入后可在设置中调整。",style=MaterialTheme.typography.bodySmall)
            }
            item { Button(onClick={picker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/octet-stream"))},enabled=!state.isReading && !state.isSaving) {Text("选择 Excel 文件")} }
            item(key="status") {
                AnimatedContent(targetState=when { state.isReading -> "正在读取课表…"; state.isSaving -> "正在保存课表…"; else -> state.errorMessage.orEmpty() },transitionSpec={fadeIn(tween(180)) togetherWith fadeOut(tween(120))},label="导入状态") { message ->
                    if(message.isNotBlank()) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(state.isReading || state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(message,color=if(state.errorMessage==message) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.fileName.isNotBlank()) item {Text(state.fileName,style=MaterialTheme.typography.bodySmall)}
            state.preview?.let { preview ->
                item {
                    Text("导入预览",style=MaterialTheme.typography.titleLarge)
                    Text("${preview.courses.size} 门课程 · ${preview.meetingCount} 条安排 · ${preview.pendingCount} 门待安排")
                    Text("上课周次最多到第 ${preview.requiredWeeks} 周")
                    Button(onClick={confirm=true},enabled=!state.isSaving,modifier=Modifier.fillMaxWidth().padding(top=12.dp)) {Text(if(state.isSaving) "正在导入…" else "导入这份课表")}
                }
                items(preview.courses,key={it.code}) { course ->
                    Card(Modifier.fillMaxWidth().animateItem().animateContentSize()) {
                        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(course.name,style=MaterialTheme.typography.titleMedium)
                            Text("${course.code} · ${course.teachers}",style=MaterialTheme.typography.bodySmall)
                            if(course.meetings.isEmpty()) Text("待安排",color=MaterialTheme.colorScheme.primary)
                            course.meetings.forEach { meeting ->
                                val day=listOf("一","二","三","四","五","六","日")[meeting.weekday-1]
                                Text("周$day ${meeting.startPeriod}–${meeting.endPeriod}节 · ${WeekMask.format(meeting.weekMask,30)}\n${meeting.location}",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("导入并替换当前课表？")},text={Text("确认后替换所有课程。原课表会保存为一份备份，可在设置中恢复。学期日期保持不变。")},
        confirmButton={TextButton(onClick={confirm=false;onConfirm()}){Text("确认导入")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
}
