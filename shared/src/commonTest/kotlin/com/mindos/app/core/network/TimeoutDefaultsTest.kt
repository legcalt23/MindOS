package com.mindos.app.core.network

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimeoutDefaultsTest {

    // ── Threshold sanity checks (pinned so refactors can't change them silently) ──

    @Test
    fun firestoreDefaults_areReasonable() {
        assertEquals(15.seconds, FirestoreDefaults.ReadTimeout)
        assertEquals(20.seconds, FirestoreDefaults.WriteTimeout)
        assertEquals(30.seconds, FirestoreDefaults.BatchTimeout)
        // Sanity: batch >= write >= read
        assertTrue(FirestoreDefaults.BatchTimeout >= FirestoreDefaults.WriteTimeout)
        assertTrue(FirestoreDefaults.WriteTimeout >= FirestoreDefaults.ReadTimeout)
    }

    @Test
    fun authDefaults_areReasonable() {
        assertEquals(20.seconds, AuthDefaults.SignInTimeout)
        assertEquals(10.seconds, AuthDefaults.SignOutTimeout)
        assertEquals(15.seconds, AuthDefaults.PasswordResetTimeout)
        assertEquals(10.seconds, AuthDefaults.TokenRefreshTimeout)
        // Sanity: sign in is the slowest path (network + token).
        assertTrue(AuthDefaults.SignInTimeout >= AuthDefaults.SignOutTimeout)
    }

    @Test
    fun storageDefaults_areReasonable() {
        // Sanity: large > small (uploads).
        assertTrue(StorageDefaults.LargeUploadTimeout > StorageDefaults.SmallUploadTimeout)
        // Sanity: large > small (reads).
        assertTrue(StorageDefaults.LargeReadTimeout > StorageDefaults.SmallReadTimeout)
        // DownloadUrl + Delete are metadata-only — quick.
        assertTrue(StorageDefaults.DownloadUrlTimeout <= 30.seconds)
        assertTrue(StorageDefaults.DeleteTimeout <= 30.seconds)
    }

    // ── Functional: timeout fires when block exceeds limit ──

    @Test
    fun withAuthTimeout_throwsOnExpiry() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            withAuthTimeout(timeout = 50.milliseconds) {
                delay(10.seconds)
            }
        }
    }

    @Test
    fun withAuthTimeout_returnsValueWhenFast() = runTest {
        val result = withAuthTimeout(timeout = 1.seconds) { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun withStorageUploadTimeout_throwsOnExpiry() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            withStorageUploadTimeout(timeout = 50.milliseconds) {
                delay(10.seconds)
            }
        }
    }

    @Test
    fun withStorageReadTimeout_returnsValueWhenFast() = runTest {
        val result = withStorageReadTimeout(timeout = 1.seconds) { 42 }
        assertEquals(42, result)
    }

    @Test
    fun customTimeout_overridesDefault() = runTest {
        // Explicit short timeout must override the long default.
        assertFailsWith<TimeoutCancellationException> {
            withStorageUploadTimeout(timeout = 30.milliseconds) {
                delay(2.seconds)
            }
        }
    }

    // ── Functions (Cloud callable) timeouts ──

    @Test
    fun functionsDefaults_areReasonable() {
        assertEquals(15.seconds, FunctionsDefaults.ReadCallTimeout)
        assertEquals(20.seconds, FunctionsDefaults.WriteCallTimeout)
        // Sanity: a mutating callable (purchase) is allowed at least as long as a read.
        assertTrue(FunctionsDefaults.WriteCallTimeout >= FunctionsDefaults.ReadCallTimeout)
    }

    @Test
    fun withFunctionsReadTimeout_throwsOnExpiry() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            withFunctionsReadTimeout(timeout = 50.milliseconds) {
                delay(10.seconds)
            }
        }
    }

    @Test
    fun withFunctionsWriteTimeout_throwsOnExpiry() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            withFunctionsWriteTimeout(timeout = 50.milliseconds) {
                delay(10.seconds)
            }
        }
    }

    @Test
    fun withFunctionsReadTimeout_returnsValueWhenFast() = runTest {
        val result = withFunctionsReadTimeout(timeout = 1.seconds) { "balance" }
        assertEquals("balance", result)
    }

    @Test
    fun withFunctionsWriteTimeout_returnsValueWhenFast() = runTest {
        val result = withFunctionsWriteTimeout(timeout = 1.seconds) { 100 }
        assertEquals(100, result)
    }
}
