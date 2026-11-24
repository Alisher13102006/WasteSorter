package com.example.wastesorter

import org.junit.Assert.assertEquals
import org.junit.Test

class EcoStatsTest {
    @Test fun countsAndPercents() {
        val s = EcoStats().apply { add("plastic"); add("plastic"); add("glass"); add("paper") }
        assertEquals(4, s.total)
        assertEquals(50, s.percent("plastic"))
        assertEquals(25, s.percent("glass"))
    }
}
