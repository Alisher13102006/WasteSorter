package com.example.wastesorter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ЛР8: правила версионирования и принудительного обновления. */
class ReleaseTest {
    private fun code(v: String) = v.split(".").map { it.toInt() }.let { (a, b, c) -> a * 10000 + b * 100 + c }

    @Test fun versionCodeMatchesSemVer() =
        assertEquals(code(BuildConfig.VERSION_NAME.substringBefore("-")), BuildConfig.VERSION_CODE)

    @Test fun versionCodeGrowsWithEveryRelease() {
        val history = listOf("0.1.0", "1.0.0", "1.1.0", "1.2.0", "1.2.1", "1.10.0", "2.0.0").map(::code)
        assertEquals(history.sorted(), history)
        assertEquals(history.size, history.toSet().size)
    }

    @Test fun debugVariant() {
        assertEquals("https://dev.api.wastesorter.kz/", BuildConfig.API_BASE_URL)
        assertTrue(BuildConfig.VERSION_NAME.endsWith("-debug"))
    }

    @Test fun forceUpdateBelowMinVersion() {
        assertTrue(UpdateChecker.mustUpdate(current = 10200, min = 10201))
        assertFalse(UpdateChecker.mustUpdate(current = 10201, min = 10201))
        assertFalse(UpdateChecker.mustUpdate(current = 10000, min = 0))
    }

    @Test fun minVersionDefaultsToZeroWithoutFirebase() =
        assertEquals(0L, FeatureFlags.long(FeatureFlags.MIN_VERSION_CODE))
}
