package com.songkeyuan.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import androidx.compose.ui.test.junit4.createComposeRule
import com.songkeyuan.schedule.data.*
import com.songkeyuan.schedule.data.importing.ImportedTimetable
import com.songkeyuan.schedule.ui.schedule.*
import com.songkeyuan.schedule.ui.importing.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScheduleAndImportScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun switchesBetweenDailyTimelineAndFullWeekAndOpensCourse() {
        val item=ScheduledCourse(Course(7,"周六调课","NET","教师"),Meeting(8,7,6,3,4,32L,"A110"))
        var opened=0L
        compose.setContent {
            var state by remember { mutableStateOf(ScheduleUiState(isLoading=false,selectedWeek=6,selectedWeekday=6,courses=listOf(item),weekCourses=listOf(item))) }
            MaterialTheme {
                ScheduleScreen(state,{}, {}, {}, {opened=it}, {}, {},onViewModeChange={state=state.copy(viewMode=it)})
            }
        }
        compose.onNodeWithText("周六调课").assertIsDisplayed()
        compose.onNodeWithText("每周总览").performClick()
        compose.onNodeWithText("周日").assertIsDisplayed()
        compose.onNodeWithText("周一").assertIsDisplayed()
        compose.onNodeWithText("本周 1 次课程").assertIsDisplayed()
        compose.onNodeWithText("周六调课").performClick()
        compose.runOnIdle { assertEquals(7L,opened) }
        compose.onNodeWithText("每日").performClick()
        compose.onNodeWithText("第3–4节 · A110").assertIsDisplayed()
    }
    @Test fun weeklyZoomSurvivesWeekNavigationAndViewSwitches() {
        compose.setContent {
            var state by remember { mutableStateOf(ScheduleUiState(isLoading=false,viewMode=ScheduleViewMode.Weekly)) }
            MaterialTheme {
                ScheduleScreen(state,{}, {state=state.copy(selectedWeek=state.selectedWeek+1)}, {}, {}, {}, {},onViewModeChange={state=state.copy(viewMode=it)})
            }
        }
        compose.onNodeWithText("放大课表").performClick()
        compose.onNodeWithText("显示全周").assertIsDisplayed()
        compose.onNodeWithContentDescription("下一周").performClick()
        compose.onNodeWithText("显示全周").assertIsDisplayed()
        compose.onNodeWithText("每日").performClick()
        compose.onNodeWithText("每周总览").performClick()
        compose.onNodeWithText("显示全周").assertIsDisplayed()
    }
    @Test fun weeklyDatesRemainReadableAtDoubleFontSize() {
        compose.setContent {
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,2f)) {
                MaterialTheme {
                    ScheduleScreen(ScheduleUiState(isLoading=false,viewMode=ScheduleViewMode.Weekly,weekMonday=LocalDate.of(2026,11,16)),{}, {}, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("11/16").assertIsDisplayed().assertHeightIsAtLeast(24.dp)
    }
    @Test fun importNeedsExplicitConfirmationAndCanBeCancelled() {
        var saves=0
        compose.setContent { MaterialTheme {
            ImportScreen(ImportUiState(preview=ImportedTimetable(listOf(CourseDraft(name="测试课",code="TEST")),1)),{_,_->},onConfirm={saves++},onBack={})
        } }
        compose.onNodeWithText("导入这份课表").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(0,saves) }
        compose.onNodeWithText("导入这份课表").performClick()
        compose.onNodeWithText("确认导入").performClick()
        compose.runOnIdle { assertEquals(1,saves) }
    }
}
