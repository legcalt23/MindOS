package com.mindos.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputSecurityInspectorTest {

    @Test
    fun cleanAsciiInputHasNoFindings() {
        val report = InputSecurityInspector.inspect("open settings")
        assertFalse(report.hasRejections)
        assertTrue(report.findings.isEmpty())
    }

    @Test
    fun rejectsOversizedInput() {
        val report = InputSecurityInspector.inspect("abcdef", InputSecurityPolicy(maxInputLength = 3))
        assertTrue(report.hasRejections)
        assertEquals(listOf(InputSecurityCode.INPUT_TOO_LONG), report.rejectionCodes)
    }

    @Test
    fun rejectsControlAndFormatCharacters() {
        val report = InputSecurityInspector.inspect("open\u0001settings\u200B")
        assertTrue(report.hasRejections)
        assertEquals(
            listOf(InputSecurityCode.INVALID_CONTROL_CHARACTER, InputSecurityCode.INVALID_CONTROL_CHARACTER),
            report.rejectionCodes,
        )
    }

    @Test
    fun rejectsMalformedUnicode() {
        val report = InputSecurityInspector.inspect("open \uD800 settings")
        assertTrue(report.hasRejections)
        assertEquals(listOf(InputSecurityCode.MALFORMED_UNICODE), report.rejectionCodes)
    }

    @Test
    fun detectsCyrillicHomoglyphInLatinName() {
        val report = InputSecurityInspector.inspect("open m\u0430rta")
        assertFalse(report.hasRejections)
        assertTrue(InputSecurityCode.MIXED_SCRIPT in report.warningCodes)
        assertTrue(InputSecurityCode.HOMOGLYPH_CONFUSABLE in report.warningCodes)
    }
}
