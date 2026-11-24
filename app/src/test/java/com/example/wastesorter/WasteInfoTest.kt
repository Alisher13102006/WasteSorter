package com.example.wastesorter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteInfoTest {
    @Test fun everyModelClassHasDescription() {
        val labels = java.io.File("src/main/assets/labels.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(labels.isNotEmpty())
        labels.forEach { assertTrue("нет описания для $it", it in WASTE_INFO) }
    }

    @Test fun unknownLabelFallsBack() = assertEquals("❓", infoFor("metal").emoji)

    @Test fun binsAreDifferent() = assertEquals(WASTE_INFO.size, WASTE_INFO.values.map { it.bin }.toSet().size)
}

/** 1.1.0: «Поделиться». */
class ShareTextTest {
    @Test fun containsBinAndHowTo() {
        val text = shareText(Prediction("glass", 0.9f, listOf("glass" to 0.9f)))
        assertTrue(text.contains("Зелёный контейнер"))
        assertTrue(text.contains("Промойте"))
    }

    @Test fun shareEnabledByDefault() = assertTrue(FeatureFlags.isEnabled(FeatureFlags.SHARE_RESULT))
}
