package com.songkeyuan.schedule.ui.pending

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.songkeyuan.schedule.ui.theme.courseColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PendingScreen(state: PendingUiState, onCourseClick: (Long) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "待安排 · ${state.courses.size}",
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.errorMessage) }
            state.courses.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("所有课程都已安排") }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.courses, key = { it.id }) { course ->
                    val colors=courseColors(course)
                    Card(onClick = { onCourseClick(course.id) },colors=CardDefaults.cardColors(containerColor=colors.container,contentColor=colors.ink),modifier=Modifier.animateItem()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${course.code} · ${course.teachers}", color = colors.ink)
                            Text("查看详情与安排时间", modifier = Modifier.padding(top = 6.dp), color = colors.ink)
                        }
                    }
                }
            }
        }
    }
}
