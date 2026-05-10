package com.restaurant.management.ui.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object ReportDateUtils {
    fun systemZone(): ZoneId = ZoneId.systemDefault()

    fun thisMonthBounds(zone: ZoneId): Pair<Long, Long> =
        yearMonthBounds(YearMonth.now(zone), zone)

    fun yearMonthBounds(
        ym: YearMonth,
        zone: ZoneId,
    ): Pair<Long, Long> {
        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    /** Inclusive start/end local dates → [fromMillis, toMillisExclusive). */
    fun inclusiveLocalDateBounds(
        start: LocalDate,
        end: LocalDate,
        zone: ZoneId,
    ): Pair<Long, Long> {
        require(!end.isBefore(start)) { "End date must not be before start date" }
        val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val toExclusive = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from to toExclusive
    }
}
