package com.mindos.commandrouter.patterns

import kotlin.test.Test
import kotlin.test.assertEquals

class CommandNormalizerTest {

    private val normalizer = DefaultCommandNormalizer()

    @Test
    fun trimsLeadingAndTrailingWhitespace() {
        assertEquals("open settings", normalizer.normalize("  open settings  "))
    }

    @Test
    fun lowercasesInput() {
        assertEquals("open settings", normalizer.normalize("OPEN SETTINGS"))
    }

    @Test
    fun collapsesInternalWhitespace() {
        assertEquals("open settings", normalizer.normalize("open   settings"))
    }

    @Test
    fun handlesTabsAndNewlines() {
        assertEquals("open settings", normalizer.normalize("open\tsettings\n"))
    }

    @Test
    fun emptyStringStaysEmpty() {
        assertEquals("", normalizer.normalize(""))
    }

    @Test
    fun blankStringBecomesEmpty() {
        assertEquals("", normalizer.normalize("   "))
    }
}
