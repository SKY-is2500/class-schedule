package com.songkeyuan.schedule.ui.schedule

import com.songkeyuan.schedule.data.Course
import com.songkeyuan.schedule.data.Meeting
import com.songkeyuan.schedule.data.ScheduledCourse
import org.junit.Assert.*
import org.junit.Test

class WeekGridLayoutTest {
    private fun item(id:Long,start:Int,end:Int,day:Int=1)=ScheduledCourse(Course(id,"课程$id","",""),Meeting(id,id,day,start,end,1L,"A101"))
    @Test fun adjacentCoursesShareLaneAndOverlapsRemainVisible() {
        val a=item(1,1,2);val b=item(2,2,3);val c=item(3,4,5)
        val placements=layoutDayCourses(listOf(a,b,c))
        assertNotEquals(placements.single { it.item==a }.lane,placements.single { it.item==b }.lane)
        assertEquals(2,placements.first().laneCount)
        assertEquals(0,placements.single { it.item==c }.lane)
    }
    @Test fun nonOverlappingCoursesUseFullWidth() {
        val placements=layoutDayCourses(listOf(item(1,1,2),item(2,3,5)))
        assertTrue(placements.all { it.lane==0 && it.laneCount==1 })
    }
}
