package com.mindos.action.entity

import kotlinx.serialization.Serializable

@Serializable
data class CalendarDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<CalendarDate> {
    init {
        require(year in 1..9999) { "year out of range." }
        require(month in 1..12) { "month out of range." }
        require(day in 1..daysInMonth(year, month)) { "day out of range." }
    }

    override fun compareTo(other: CalendarDate): Int =
        compareValuesBy(this, other, CalendarDate::year, CalendarDate::month, CalendarDate::day)

    fun plusDays(days: Int): CalendarDate {
        var y = year
        var m = month
        var d = day
        repeat(days) {
            d += 1
            if (d > daysInMonth(y, m)) {
                d = 1
                m += 1
                if (m > 12) {
                    m = 1
                    y += 1
                }
            }
        }
        return CalendarDate(y, m, d)
    }

    override fun toString(): String =
        year.toString().padStart(4, '0') + "-" +
            month.toString().padStart(2, '0') + "-" +
            day.toString().padStart(2, '0')
}

@Serializable
data class DateValue(
    val localDate: CalendarDate,
    val timezone: String,
    val sourceText: String,
)

@Serializable
data class DateRange(
    val startInclusive: CalendarDate,
    val endExclusive: CalendarDate,
    val timezone: String,
    val sourceText: String,
)

@Serializable
data class TemporalContext(
    val today: CalendarDate,
    val timezone: String,
) {
    init {
        require(timezone.isNotBlank()) { "timezone must be explicit." }
    }
}

sealed interface TemporalParseResult {
    data class Single(val value: DateValue) : TemporalParseResult
    data class Range(val value: DateRange) : TemporalParseResult
    data class Rejected(val reason: String) : TemporalParseResult
}

object RelativeDateParser {
    fun parse(input: String, context: TemporalContext): TemporalParseResult {
        val text = input.trim().lowercase()
        return when (text) {
            "hoy", "today" -> single(context.today, context, input)
            "mañana", "manana", "tomorrow" -> single(context.today.plusDays(1), context, input)
            "pasado mañana", "pasado manana", "day after tomorrow" -> single(context.today.plusDays(2), context, input)
            "este finde", "este fin de semana", "this weekend" -> weekend(context, input)
            "la semana que viene", "next week" -> nextWeek(context, input)
            else -> TemporalParseResult.Rejected("unsupported relative date")
        }
    }

    private fun single(date: CalendarDate, context: TemporalContext, source: String): TemporalParseResult.Single =
        TemporalParseResult.Single(DateValue(date, context.timezone, source))

    private fun weekend(context: TemporalContext, source: String): TemporalParseResult.Range {
        val saturdayOffset = daysUntil(context.today.dayOfWeek(), DayOfWeek.SATURDAY)
        val start = context.today.plusDays(saturdayOffset)
        return TemporalParseResult.Range(DateRange(start, start.plusDays(2), context.timezone, source))
    }

    private fun nextWeek(context: TemporalContext, source: String): TemporalParseResult.Range {
        val mondayOffset = daysUntil(context.today.dayOfWeek(), DayOfWeek.MONDAY).let { if (it == 0) 7 else it }
        val start = context.today.plusDays(mondayOffset)
        return TemporalParseResult.Range(DateRange(start, start.plusDays(7), context.timezone, source))
    }
}

private enum class DayOfWeek { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

private fun CalendarDate.dayOfWeek(): DayOfWeek {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year
    if (month < 3) y -= 1
    val value = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
    return when (value) {
        0 -> DayOfWeek.SUNDAY
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        else -> DayOfWeek.SATURDAY
    }
}

private fun daysUntil(from: DayOfWeek, target: DayOfWeek): Int =
    (target.ordinal - from.ordinal + 7) % 7

internal fun daysInMonth(year: Int, month: Int): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> error("invalid month")
    }

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
