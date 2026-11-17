package com.songkeyuan.schedule.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDateChange: (String) -> Unit,
    onWeeksChange: (String) -> Unit,
    onSave: () -> Unit,
    onRequestReset: () -> Unit,
    onDismissReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onImport: () -> Unit = {},
    onRequestRestoreBackup: () -> Unit = {},
    onDismissRestoreBackup: () -> Unit = {},
    onConfirmRestoreBackup: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("学期设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("设置本学期第 1 周的周一日期。修改后会重新计算每周日期。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = state.firstWeekMonday,
            onValueChange = onDateChange,
            label = { Text("第 1 周周一（YYYY-MM-DD）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.totalWeeks,
            onValueChange = onWeeksChange,
            label = { Text("总教学周数（1–30）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        state.message?.let { Text(it, color = if (it.contains("失败") || it.contains("请输入")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave, enabled = !state.isSaving) { Text(if (state.isSaving) "处理中…" else "保存设置") }
            OutlinedButton(onClick = onRequestReset, enabled = !state.isSaving) { Text("恢复初始课表") }
        }
        Text("数据仅保存在本机，本应用不需要联网权限。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onImport,enabled=!state.isSaving) { Text("从 Excel 导入课表") }
        if(state.importBackupAvailable) OutlinedButton(onClick=onRequestRestoreBackup,enabled=!state.isSaving) { Text("恢复导入前的课表") }
    }

    if (state.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissReset,
            title = { Text("恢复初始课表？") },
            text = { Text("恢复后将清除所有自定义修改，并重新载入本学期 15 门课程。") },
            confirmButton = { TextButton(onClick = onConfirmReset) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = onDismissReset) { Text("取消") } },
        )
    }

    if(state.showRestoreBackupConfirmation) AlertDialog(onDismissRequest=onDismissRestoreBackup,title={Text("恢复导入前的课表？")},text={Text("将替换当前课程及学期设置，恢复后这份备份会被移除。")},
        confirmButton={TextButton(onClick=onConfirmRestoreBackup){Text("确认恢复备份")}},dismissButton={TextButton(onClick=onDismissRestoreBackup){Text("取消")}})
}
