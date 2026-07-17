package com.mindos.app.features.mindboost.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncSessionIdTest {

    // Whitespace/NUL chars are built at runtime via toChar() so no literal space nor raw NUL byte is
    // embedded in this source file (verified by the NUL-byte check in Phase 0 hardening).
    private val space = 32.toChar()
    private val nul = 0.toChar()

    @Test
    fun rejectsBlank() {
        assertFailsWith<IllegalArgumentException> { SyncSessionId("") }
        assertFailsWith<IllegalArgumentException> { SyncSessionId("     ") }
    }

    @Test
    fun rejectsPathSeparators() {
        assertFalse(SyncSessionId.isValid("a/b"))
        assertFalse(SyncSessionId.isValid("a\\b"))
    }

    @Test
    fun rejectsWhitespace() {
        assertFalse(SyncSessionId.isValid("a" + space + "b"))
        assertFalse(SyncSessionId.isValid("a\tb"))
        assertFalse(SyncSessionId.isValid("a\nb"))
        assertFalse(SyncSessionId.isValid("a\rb"))
    }

    @Test
    fun rejectsNulByteWithoutEmbeddingRawNulInSource() {
        // NUL is constructed at runtime (0.toChar()); there is NO raw NUL byte in this source file.
        assertFalse(SyncSessionId.isValid("a" + nul + "b"))
    }

    @Test
    fun rejectsDotAndReserved() {
        assertFalse(SyncSessionId.isValid("."))
        assertFalse(SyncSessionId.isValid(".."))
        assertFalse(SyncSessionId.isValid("__reserved__"))
    }

    @Test
    fun rejectsOverlyLong_acceptsAtLimit() {
        assertFalse(SyncSessionId.isValid("a".repeat(SyncSessionId.MAX_LENGTH + 1)))
        assertTrue(SyncSessionId.isValid("a".repeat(SyncSessionId.MAX_LENGTH)))
    }

    @Test
    fun acceptsTypicalId() {
        val raw = "enfoque-1700000000000-ab12cd34"
        assertTrue(SyncSessionId.isValid(raw))
        assertEquals(raw, SyncSessionId(raw).value)
    }

    @Test
    fun parseOrNull_nullForInvalid_valueForValid() {
        assertNull(SyncSessionId.parseOrNull("a/b"))
        assertNull(SyncSessionId.parseOrNull("a\tb"))
        assertNull(SyncSessionId.parseOrNull(""))
        assertEquals("ok-1", SyncSessionId.parseOrNull("ok-1")?.value)
    }

    @Test
    fun generator_producesValidPathSafeIds() {
        val id = DefaultSyncSessionIdGenerator().newSessionId("enfoque", startedAtMs = 1_700_000_000_000L)
        assertTrue(SyncSessionId.isValid(id.value), id.value)
    }

    @Test
    fun generator_sanitizesWhitespaceInModule() {
        val id = DefaultSyncSessionIdGenerator().newSessionId("a" + space + "b", startedAtMs = 1L)
        assertTrue(SyncSessionId.isValid(id.value), id.value)
        assertFalse(id.value.any { it.isWhitespace() })
    }

    @Test
    fun generator_producesDistinctIds_forSameModuleAndTime() {
        var n = 0
        val gen = DefaultSyncSessionIdGenerator(suffixSource = { "s${n++}" })
        assertNotEquals(gen.newSessionId("enfoque", 1L), gen.newSessionId("enfoque", 1L))
    }

    @Test
    fun value_isStableOnceCreated() {
        val id = SyncSessionId("enfoque-1-abc")
        assertEquals("enfoque-1-abc", id.value)
        assertEquals(id, SyncSessionId("enfoque-1-abc"))
    }
}
