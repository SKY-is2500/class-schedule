package com.songkeyuan.schedule.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConflictDetectorTest {
    @Test
    fun overlappingPeriodAndWeek_conflicts() {
        val existing = MeetingDraft(id = 1, weekday = 3, startPeriod = 3, endPeriod = 4, weekMask = WeekMask.fromWeeks((1..8).toSet(), 16))
        val candidate = MeetingDraft(id = 2, weekday = 3, startPeriod = 4, endPeriod = 5, weekMask = WeekMask.fromWeeks(setOf(8, 9), 16))
        assertEquals(existing, ConflictDetector.find(candidate, listOf(existing)))
    }

    @Test
    fun adjacentPeriodsOrDisjointWeeks_doNotConflict() {
        val existing = MeetingDraft(id = 1, weekday = 3, startPeriod = 3, endPeriod = 4, weekMask = WeekMask.fromWeeks((1..8).toSet(), 16))
        val adjacent = existing.copy(id = 2, startPeriod = 5, endPeriod = 6)
        val laterWeeks = existing.copy(id = 3, weekMask = WeekMask.fromWeeks((9..16).toSet(), 16))
        assertNull(ConflictDetector.find(adjacent, listOf(existing)))
        assertNull(ConflictDetector.find(laterWeeks, listOf(existing)))
    }

    @Test
    fun editingSameMeeting_excludesItself() {
        val candidate = MeetingDraft(id = 7, weekday = 1, startPeriod = 3, endPeriod = 4, weekMask = 3L)
        assertNull(ConflictDetector.find(candidate, listOf(candidate)))
    }
}
