package com.example.mindos.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeInputTest {
    @Test
    fun safeDocumentIdsRejectSqlPayloadsAndPathTraversal() {
        val invalid = listOf(
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "\" OR \"\"=\"",
            "admin'--",
            "../../etc",
            "user/other",
            "abc\u0000def",
        )

        invalid.forEach { value ->
            assertFalse(SafeInput.isSafeDocumentId(value), value)
        }

        assertTrue(SafeInput.isSafeDocumentId("user_123-ABC"))
    }

    @Test
    fun humanTextKeepsNaturalTextButRejectsControlCharacters() {
        assertEquals(
            "Hola mundo",
            SafeInput.normalizeSingleLineHumanText("  Hola   mundo\n", maxLength = 80),
        )
        assertTrue(SafeInput.isSafeMultilineHumanText("Hola\nmundo", maxLength = 80))
        assertFalse(SafeInput.isSafeMultilineHumanText("Hola\u0007mundo", maxLength = 80))
    }

    @Test
    fun searchQueryIsBoundedAndSingleLine() {
        val payload = "%_%_%\nadmin'--"

        assertEquals("%_%_% admin'--", SafeInput.normalizeSearchQuery(payload, maxLength = 80))
    }
}
