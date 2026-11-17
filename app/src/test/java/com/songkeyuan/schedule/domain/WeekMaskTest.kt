package com.songkeyuan.schedule.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekMaskTest {
    @Test
    fun continuousWeeks_roundTripAndFormat() {
        val mask = WeekMask.fromWeeks((1..16).toSet(), 16)
        assertEquals((1..16).toSet(), WeekMask.toWeeks(mask, 16))
        assertEquals("1–16周", WeekMask.format(mask, 16))
    }

    @Test
    fun alternatingWeeks_roundTripAndFormat() {
        val weeks = setOf(1, 2, 4, 6, 8, 10)
        val mask = WeekMask.fromWeeks(weeks, 16)
        assertEquals(weeks, WeekMask.toWeeks(mask, 16))
        assertEquals("1–2、4、6、8、10周", WeekMask.format(mask, 16))
    }
}
