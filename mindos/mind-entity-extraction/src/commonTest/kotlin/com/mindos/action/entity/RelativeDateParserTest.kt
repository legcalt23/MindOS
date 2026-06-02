package com.mindos.action.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RelativeDateParserTest {

    private val context = TemporalContext(
        today = CalendarDate(2026, 6, 1),
        timezone = "Europe/Madrid",
    )

    @Test
    fun parsesSpanishTomorrowWithExplicitTimezone() {
        val parsed = RelativeDateParser.parse("mañana", context)

        val single = assertIs<TemporalParseResult.Single>(parsed).value
        assertEquals(CalendarDate(2026, 6, 2), single.localDate)
        assertEquals("Europe/Madrid", single.timezone)
    }

    @Test
    fun parsesWeekendAsDeterministicRange() {
        val parsed = RelativeDateParser.parse("este finde", context)

        val range = assertIs<TemporalParseResult.Range>(parsed).value
        assertEquals(CalendarDate(2026, 6, 6), range.startInclusive)
        assertEquals(CalendarDate(2026, 6, 8), range.endExclusive)
    }

    @Test
    fun parsesNextWeekFromFixedClockDate() {
        val parsed = RelativeDateParser.parse("next week", context)

        val range = assertIs<TemporalParseResult.Range>(parsed).value
        assertEquals(CalendarDate(2026, 6, 8), range.startInclusive)
        assertEquals(CalendarDate(2026, 6, 15), range.endExclusive)
    }
}
