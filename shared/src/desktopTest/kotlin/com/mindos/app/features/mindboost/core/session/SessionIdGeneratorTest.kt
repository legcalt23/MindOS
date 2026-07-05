package com.mindos.app.features.mindboost.core.session

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionIdGeneratorTest {

    @Test
    fun fakeGenerator_returnsFixedId() {
        val gen = SessionIdGenerator { "fixed-id" }
        assertEquals("fixed-id", gen.newSessionId())
    }

    @Test
    fun random_producesDistinctOpaqueIds() {
        val gen = RandomSessionIdGenerator(Random(42))
        val a = gen.newSessionId()
        val b = gen.newSessionId()
        assertNotEquals(a, b)
        assertTrue(a.startsWith("sess_"))
    }

    @Test
    fun random_idCarriesNoUserData() {
        val userId = "user-1234-abcd"
        val email = "person@example.com"
        val id = RandomSessionIdGenerator(Random(7)).newSessionId()
        assertFalse(id.contains(userId))
        assertFalse(id.contains(email))
        assertFalse(id.contains("@"))
    }
}
