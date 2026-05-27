package com.example.mindos.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorMapperTest {

    @Test
    fun networkErrorClassified() {
        val error = ErrorMapper.fromThrowable(Exception("Unable to resolve host: network unreachable"))
        assertEquals("network_error", error.code)
        assertTrue(error.isRetryable)
    }

    @Test
    fun timeoutErrorClassified() {
        val error = ErrorMapper.fromThrowable(Exception("DEADLINE_EXCEEDED: timed out"))
        assertEquals("timeout", error.code)
        assertTrue(error.isRetryable)
    }

    @Test
    fun authErrorClassified() {
        val error = ErrorMapper.fromThrowable(Exception("UNAUTHENTICATED: token expired"))
        assertEquals("unauthorized", error.code)
        assertFalse(error.isRetryable)
    }

    @Test
    fun permissionDeniedClassified() {
        val error = ErrorMapper.fromThrowable(Exception("PERMISSION_DENIED: insufficient permissions"))
        assertEquals("permission_denied", error.code)
        assertFalse(error.isRetryable)
    }

    @Test
    fun notFoundClassified() {
        val error = ErrorMapper.fromThrowable(Exception("NOT_FOUND: document does not exist"))
        assertEquals("not_found", error.code)
        assertFalse(error.isRetryable)
    }

    @Test
    fun rateLimitClassified() {
        val error = ErrorMapper.fromThrowable(Exception("RESOURCE_EXHAUSTED: quota exceeded"))
        assertEquals("rate_limited", error.code)
        assertTrue(error.isRetryable)
    }

    @Test
    fun serverErrorClassified() {
        val error = ErrorMapper.fromThrowable(Exception("INTERNAL: server error"))
        assertEquals("server_error", error.code)
        assertTrue(error.isRetryable)
    }

    @Test
    fun unknownErrorFallback() {
        val error = ErrorMapper.fromThrowable(Exception("something bizarre happened"))
        assertEquals("unknown", error.code)
        assertTrue(error.isRetryable)
    }

    @Test
    fun nullMessageFallsToUnknown() {
        val error = ErrorMapper.fromThrowable(Exception())
        assertEquals("unknown", error.code)
    }

    @Test
    fun validationErrorHasFieldInfo() {
        val error = ErrorMapper.validationError("email", "formato invalido")
        assertEquals("validation_error", error.code)
        assertTrue(error.message.contains("email"))
        assertTrue(error.message.contains("formato invalido"))
        assertFalse(error.isRetryable)
    }
}
