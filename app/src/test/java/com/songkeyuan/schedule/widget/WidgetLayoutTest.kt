package com.songkeyuan.schedule.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutTest {
    @Test
    fun smallerWidgetsLeaveSpaceForAnOverflowCount() {
        val compact = WidgetLayout.forSize(130f, 117f, fontScale = 1f, courseCount = 5)
        val tall = WidgetLayout.forSize(350f, 320f, fontScale = 1f, courseCount = 5)

        assertFalse(compact.showTeachingWeek)
        assertEquals(1, compact.visibleCourses)
        assertTrue(compact.showOverflowCount)
        assertTrue(tall.showTeachingWeek)
        assertEquals(5, tall.visibleCourses)
        assertFalse(tall.showOverflowCount)
    }

    @Test
    fun largerTextShowsFewerRowsWhileKeepingACourseVisible() {
        val regular = WidgetLayout.forSize(320f, 180f, fontScale = 1f, courseCount = 5)
        val enlarged = WidgetLayout.forSize(320f, 180f, fontScale = 2f, courseCount = 5)

        assertTrue(regular.visibleCourses > enlarged.visibleCourses)
        assertEquals(1, enlarged.visibleCourses)
    }
}
