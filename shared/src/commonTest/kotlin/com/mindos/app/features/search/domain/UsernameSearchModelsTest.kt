package com.mindos.app.features.search.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsernameSearchModelsTest {

    // ── normalizeUsernameSearchQuery ─────────────────────────────────

    @Test
    fun normalize_trims_whitespace() {
        assertEquals("abc", normalizeUsernameSearchQuery("  abc  "))
    }

    @Test
    fun normalize_removes_at_prefix() {
        assertEquals("user123", normalizeUsernameSearchQuery("@user123"))
    }

    @Test
    fun normalize_lowercases() {
        assertEquals("abcdef", normalizeUsernameSearchQuery("AbCdEf"))
    }

    @Test
    fun normalize_strips_disallowed_characters() {
        // Special chars are removed (not replaced), so "hello!@#world" → "helloworld"
        assertEquals("helloworld", normalizeUsernameSearchQuery("hello!@#world"))
        // underscore and dot are allowed
        assertEquals("a.b_c", normalizeUsernameSearchQuery("a.b_c"))
    }

    @Test
    fun normalize_truncates_at_30_chars() {
        val long = "a".repeat(50)
        assertEquals(30, normalizeUsernameSearchQuery(long).length)
    }

    @Test
    fun normalize_combined_operations() {
        assertEquals("cool.user_1", normalizeUsernameSearchQuery("  @Cool.User_1!  "))
    }

    // ── usernameMatchScore ──────────────────────────────────────────

    @Test
    fun exactMatch_returns_1() {
        assertEquals(1f, usernameMatchScore("alice", "alice"))
    }

    @Test
    fun prefixMatch_returnsHighScore() {
        val score = usernameMatchScore("ali", "alice")
        assertTrue(score > 0.72f, "Prefix match score $score should be > 0.72")
        assertTrue(score < 1f, "Prefix match score $score should be < 1.0")
    }

    @Test
    fun containsMatch_returnsMediumScore() {
        val score = usernameMatchScore("lic", "alice")
        assertTrue(score > 0.42f, "Contains match score $score should be > 0.42")
        assertTrue(score < 0.72f, "Contains match score $score should be < 0.72")
    }

    @Test
    fun noMatch_returns_0() {
        assertEquals(0f, usernameMatchScore("xyz", "alice"))
    }

    @Test
    fun blankQuery_returns_0() {
        assertEquals(0f, usernameMatchScore("", "alice"))
        assertEquals(0f, usernameMatchScore("   ", "alice"))
    }

    @Test
    fun blankUsername_returns_0() {
        assertEquals(0f, usernameMatchScore("alice", ""))
    }

    @Test
    fun matchIsCaseInsensitive() {
        assertEquals(1f, usernameMatchScore("Alice", "alice"))
        assertEquals(1f, usernameMatchScore("ALICE", "alice"))
    }

    @Test
    fun matchIgnoresAtPrefix() {
        assertEquals(1f, usernameMatchScore("@alice", "alice"))
    }
}
