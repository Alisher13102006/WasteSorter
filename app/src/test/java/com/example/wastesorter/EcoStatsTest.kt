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

/** Регрессия hotfix 1.2.1: в 1.2.0 приложение падало при запуске с пустой статистикой. */
class EcoStatsEmptyTest {
    @Test fun emptyStatsGiveZeroPercent() {
        val s = EcoStats()
        assertEquals(0, s.total)
        WASTE_INFO.keys.forEach { assertEquals(0, s.percent(it)) }
    }
}
