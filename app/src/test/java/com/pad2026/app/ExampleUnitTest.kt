package com.pad2026.app

import com.pad2026.app.util.TimeUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parseTimeShouldReturnPositiveEpoch() {
        val epoch = TimeUtils.parseIsoToEpochSec("2026-01-10T12:00:00+09:00")
        assertTrue(epoch > 0)
    }
}
