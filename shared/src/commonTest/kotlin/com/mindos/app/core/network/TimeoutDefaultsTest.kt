package com.mindos.app.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    // ── Retry / backoff (S6 / D-14) ──

    /** Deterministic jitter so backoff timings are assertable under virtual time. */
    private class FixedRandom(private val value: Double) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = value
    }

    @Test
    fun retryTransient_succeedsAfterTransientFailuresWithGrowingBackoff() = runTest {
        val scheduler = testScheduler
        val stamps = mutableListOf<Long>()
        var calls = 0
        val result = retryTransient(random = FixedRandom(0.5)) {
            stamps += scheduler.currentTime
            calls++
            if (calls < 3) throw RuntimeException("code=UNAVAILABLE") else "done"
        }
        assertEquals("done", result)
        assertEquals(3, calls)
        // base = 200ms, jitter 0.5 → waits 100ms then 200ms: strictly growing.
        val firstWait = stamps[1] - stamps[0]
        val secondWait = stamps[2] - stamps[1]
        assertTrue(secondWait > firstWait, "backoff must grow: first=$firstWait second=$secondWait")
    }

    @Test
    fun retryTransient_respectsMaxAttempts() = runTest {
        var calls = 0
        assertFailsWith<RuntimeException> {
            retryTransient(random = FixedRandom(0.0)) {
                calls++
                throw RuntimeException("code=UNAVAILABLE")
            }
        }
        assertEquals(RetryDefaults.MaxAttempts, calls)
    }

    @Test
    fun retryTransient_doesNotRetryCancellation() = runTest {
        var calls = 0
        assertFailsWith<CancellationException> {
            retryTransient {
                calls++
                throw CancellationException("cancelled")
            }
        }
        assertEquals(1, calls)
    }

    @Test
    fun retryTransient_doesNotRetryNonTransient() = runTest {
        var calls = 0
        assertFailsWith<IllegalStateException> {
            retryTransient {
                calls++
                throw IllegalStateException("permanent")
            }
        }
        assertEquals(1, calls)
    }

    @Test
    fun retryTransient_retriesPerAttemptTimeout() = runTest {
        // D-14: the with*Timeout lives inside each attempt; an expired attempt is retried.
        var calls = 0
        val result = retryTransient(random = FixedRandom(0.0)) {
            calls++
            if (calls == 1) {
                withFunctionsReadTimeout(timeout = 50.milliseconds) { delay(10.seconds) }
            }
            "recovered"
        }
        assertEquals("recovered", result)
        assertEquals(2, calls)
    }

    @Test
    fun isTransientFailure_detectsUnavailableAndTimeoutButNotOthers() {
        assertTrue(isTransientFailure(RuntimeException("status: UNAVAILABLE")))
        assertTrue(isTransientFailure(RuntimeException("wrap", RuntimeException("UNAVAILABLE"))))
        assertFalse(isTransientFailure(IllegalStateException("UNAUTHENTICATED")))
    }
}
