package com.songkeyuan.schedule.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songkeyuan.schedule.domain.MeetingDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onTeachersChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAddMeeting: () -> Unit,
    onUpdateMeeting: (Int, MeetingDraft) -> Unit,
    onRemoveMeeting: (Int) -> Unit,
    onSave: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.courseId == null) "新建课程" else "编辑课程") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = onSave, enabled = !state.isSaving) { Text(if (state.isSaving) "保存中…" else "保存") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                OutlinedTextField(value = state.name, onValueChange = onNameChange, label = { Text("课程名称 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                OutlinedTextField(value = state.code, onValueChange = onCodeChange, label = { Text("课程代码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                OutlinedTextField(value = state.teachers, onValueChange = onTeachersChange, label = { Text("任课教师") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            item {
                OutlinedTextField(value = state.note, onValueChange = onNoteChange, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            }
            state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("上课安排", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onAddMeeting) { Text("添加时间") }
                }
            }
            items(state.meetings.size, key = { index -> state.meetings[index].id ?: -index.toLong() - 1 }) { index ->
                MeetingEditor(
                    index = index,
                    meeting = state.meetings[index],
                    totalWeeks = state.totalWeeks,
                    onChange = { onUpdateMeeting(index, it) },
                    onRemove = { onRemoveMeeting(index) },
                )
            }
            if (state.meetings.isEmpty()) {
                item { Text("没有上课时间时，课程会显示在“待安排”列表。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (state.courseId != null) {
                item { OutlinedButton(onClick = onRequestDelete, modifier = Modifier.fillMaxWidth()) { Text("删除课程", color = MaterialTheme.colorScheme.error) } }
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("删除这门课程？") },
            text = { Text("课程及其全部上课安排将从本机删除。") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("确认删除") } },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingEditor(
    index: Int,
    meeting: MeetingDraft,
    totalWeeks: Int,
    onChange: (MeetingDraft) -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.animateContentSize()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("安排 ${index + 1}", fontWeight = FontWeight.Bold)
                TextButton(onClick = onRemove) { Text("移除") }
            }
            Text("星期")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { dayIndex, name ->
                    FilterChip(
                        selected = meeting.weekday == dayIndex + 1,
                        onClick = { onChange(meeting.copy(weekday = dayIndex + 1)) },
                        label = { Text("周$name") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    PeriodDropdown("开始节次", meeting.startPeriod) { onChange(meeting.copy(startPeriod = it, endPeriod = meeting.endPeriod.coerceAtLeast(it))) }
                }
                Box(Modifier.weight(1f)) {
                    PeriodDropdown("结束节次", meeting.endPeriod) { onChange(meeting.copy(endPeriod = it)) }
                }
            }
            OutlinedTextField(
                value = meeting.location,
                onValueChange = { onChange(meeting.copy(location = it)) },
                label = { Text("上课地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            WeekPicker(meeting.weekMask, totalWeeks) { onChange(meeting.copy(weekMask = it)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDropdown(label: String, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "第 $selected 节",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..12).forEach { period ->
                DropdownMenuItem(text = { Text("第 $period 节") }, onClick = { onSelect(period); expanded = false })
            }
        }
    }
}

@Composable
fun WeekPicker(mask: Long, totalWeeks: Int, onChange: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("上课周次")
            Row {
                TextButton(onClick = { onChange((1L shl totalWeeks) - 1) }) { Text("全选") }
                TextButton(onClick = { onChange(0L) }) { Text("清空") }
            }
        }
        (1..totalWeeks).chunked(6).forEach { rowWeeks ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowWeeks.forEach { week ->
                    val bit = 1L shl (week - 1)
                    FilterChip(
                        selected = mask and bit != 0L,
                        onClick = { onChange(mask xor bit) },
                        label = { Text("$week") },
                    )
                }
            }
        }
    }
}
