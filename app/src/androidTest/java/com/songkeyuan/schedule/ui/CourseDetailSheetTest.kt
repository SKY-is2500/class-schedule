package com.songkeyuan.schedule.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.songkeyuan.schedule.data.*
import com.songkeyuan.schedule.ui.theme.ClassScheduleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CourseDetailSheetTest {
    @get:Rule val compose=createComposeRule()
    @Test fun detailsShowFullInformationAndEditingRequiresExplicitAction() {
        var edits=0
        val details=CourseDetails(Course(5,"网络安全","NET5","王老师、李老师","第六周调整教室"),listOf(
            Meeting(1,5,2,1,2,3,"公共教学楼 A110"),Meeting(2,5,6,3,4,32,"12号楼 B201"),
        ))
        compose.setContent { ClassScheduleTheme { CourseDetailSheet(details,16,{}, {edits++}) } }
        compose.onNodeWithText("王老师、李老师").assertIsDisplayed()
        compose.onNodeWithText("公共教学楼 A110").assertIsDisplayed()
        compose.onNodeWithText("12号楼 B201").assertIsDisplayed()
        compose.onNodeWithText("第六周调整教室").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0,edits) }
        compose.onNodeWithText("编辑课程").performClick()
        compose.runOnIdle { assertEquals(1,edits) }
    }
}
