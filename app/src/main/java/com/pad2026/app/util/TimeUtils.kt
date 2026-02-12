package com.pad2026.app.util

import java.time.OffsetDateTime

object TimeUtils {
    fun parseIsoToEpochSec(iso: String): Long = OffsetDateTime.parse(iso).toEpochSecond()
}
