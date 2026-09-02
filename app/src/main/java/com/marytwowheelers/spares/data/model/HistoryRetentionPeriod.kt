package com.marytwowheelers.spares.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HistoryRetentionPeriod(
    val label: String,
    val days: Int?
) {
    NEVER(
        label = "Never",
        days = null
    ),
    NINETY_DAYS(
        label = "90 Days",
        days = 90
    ),
    SIX_MONTHS(
        label = "6 Months",
        days = 180
    ),
    ONE_YEAR(
        label = "1 Year",
        days = 365
    ),
    CUSTOM(
        label = "Custom",
        days = null
    );

    fun getCutoffTimestamp(customDays: Int? = null): Long? {
        if (this == NEVER) return null
        val effectiveDays = if (this == CUSTOM) customDays else days
        if (effectiveDays == null) return null
        val millis = effectiveDays.toLong() * 24L * 60L * 60L * 1000L
        return System.currentTimeMillis() - millis
    }

    fun getFormattedCutoffDate(customDays: Int? = null): String {
        if (this == NEVER) return "Never (Keep indefinitely)"
        val cutoff = getCutoffTimestamp(customDays) ?: return "All time"
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(cutoff))
    }
}
