package com.mindos.app.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Default timeout / resilience settings for Firestore operations.
 *
 * These are app-level timeouts layered on top of Firebase SDK's own transport
 * timeouts. They protect the UI from hanging indefinitely when a Firestore
 * call never resolves (e.g. offline with no cache hit).
 */
object FirestoreDefaults {

    /** Default timeout for a single read (get / query). */
    val ReadTimeout: Duration = 15.seconds

    /** Default timeout for a single write (set / update / delete). */
    val WriteTimeout: Duration = 20.seconds

    /** Default timeout for batch / transaction writes. */
    val BatchTimeout: Duration = 30.seconds
}

/**
 * Runs [block] with a Firestore-read-level timeout.
 * Throws [kotlinx.coroutines.TimeoutCancellationException] on expiry.
 */
suspend inline fun <T> withFirestoreReadTimeout(
    timeout: Duration = FirestoreDefaults.ReadTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Runs [block] with a Firestore-write-level timeout.
 */
suspend inline fun <T> withFirestoreWriteTimeout(
    timeout: Duration = FirestoreDefaults.WriteTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Default timeout settings for Firebase Authentication operations.
 *
 * Auth operations involve a round-trip to Identity Toolkit and (for some flows)
 * email delivery initiation. Slow networks or backend hiccups should not leave
 * the UI hanging — every suspendable auth call is bounded by these.
 */
object AuthDefaults {

    /** Sign in / sign up / re-authenticate. */
    val SignInTimeout: Duration = 20.seconds

    /** Sign out — local on most flows, but may hit the network for revocation. */
    val SignOutTimeout: Duration = 10.seconds

    /** Password reset email request (initiates delivery; does not wait for inbox). */
    val PasswordResetTimeout: Duration = 15.seconds

    /** Token refresh / reload of the current user. */
    val TokenRefreshTimeout: Duration = 10.seconds
}

/**
 * Runs [block] inside [AuthDefaults.SignInTimeout] (or a custom [timeout]).
 *
 * Use this for any suspending call to Firebase Auth: sign in, sign up,
 * password reset request, sign out, token refresh.
 */
suspend inline fun <T> withAuthTimeout(
    timeout: Duration = AuthDefaults.SignInTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Default timeout settings for Firebase Storage operations.
 *
 * Uploads and downloads can legitimately take minutes for large payloads.
 * We pick generous defaults but bounded ones — features uploading large
 * blobs (e.g. video) should pass an explicit higher [Duration].
 */
object StorageDefaults {

    /** Reading a small object (text/JSON/PDF metadata, < a few MB). */
    val SmallReadTimeout: Duration = 30.seconds

    /** Reading a large blob (video, full image, PDF body). */
    val LargeReadTimeout: Duration = 180.seconds

    /** Uploading a small object (thumbnail, profile photo, audio note < 5 MB). */
    val SmallUploadTimeout: Duration = 60.seconds

    /** Uploading a large blob (video, multi-image attachment). */
    val LargeUploadTimeout: Duration = 300.seconds

    /** Resolving a download URL — purely a metadata call. */
    val DownloadUrlTimeout: Duration = 15.seconds

    /** Deleting an object. */
    val DeleteTimeout: Duration = 15.seconds
}

/**
 * Runs [block] inside a Storage-read timeout. Default is [StorageDefaults.SmallReadTimeout];
 * callers fetching video / large blobs should pass [StorageDefaults.LargeReadTimeout].
 */
suspend inline fun <T> withStorageReadTimeout(
    timeout: Duration = StorageDefaults.SmallReadTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Runs [block] inside a Storage-upload timeout. Default is [StorageDefaults.SmallUploadTimeout];
 * callers uploading video / large blobs should pass [StorageDefaults.LargeUploadTimeout].
 */
suspend inline fun <T> withStorageUploadTimeout(
    timeout: Duration = StorageDefaults.SmallUploadTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Default timeout settings for Firebase Cloud Functions (HTTPS callables).
 *
 * Every callable is a network round-trip to the Functions backend. Without an
 * app-level bound a callable that never resolves (cold start stall, backend
 * hang, lost connectivity with no Firestore cache fallback) leaves the UI
 * spinning forever. This matters most for the wallet/money flow, where a hung
 * `verifyPurchase` after a possible charge is the worst UX.
 *
 * These bound the *client-observed* wait. They do NOT cancel server-side work:
 * the Functions backend deduplicates by idempotency key / single-use token, so
 * a client timeout never double-credits — the user simply refreshes the balance.
 */
object FunctionsDefaults {

    /** Read-style callable (getBalance, getTransactions). */
    val ReadCallTimeout: Duration = 15.seconds

    /** Mutating callable (initiatePurchase, verifyPurchase). */
    val WriteCallTimeout: Duration = 20.seconds
}

/**
 * Runs [block] inside a read-style callable timeout ([FunctionsDefaults.ReadCallTimeout]
 * by default). Use for non-mutating callables.
 */
suspend inline fun <T> withFunctionsReadTimeout(
    timeout: Duration = FunctionsDefaults.ReadCallTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Runs [block] inside a mutating-callable timeout ([FunctionsDefaults.WriteCallTimeout]
 * by default). Use for callables that change server state (purchases).
 */
suspend inline fun <T> withFunctionsWriteTimeout(
    timeout: Duration = FunctionsDefaults.WriteCallTimeout,
    crossinline block: suspend () -> T,
): T = withTimeout(timeout) { block() }

/**
 * Defaults for [retryTransient] — bounded so tail latency stays predictable.
 */
object RetryDefaults {

    /** Total attempts: the initial try plus retries. */
    const val MAX_ATTEMPTS: Int = 3

    /** Backoff before the first retry; doubles each subsequent retry. */
    val BaseDelay: Duration = 200.milliseconds

    /** Ceiling on any single backoff wait. */
    val MaxDelay: Duration = 5.seconds
}

/**
 * Immutable retry knobs grouped into one value so [retryTransient] stays within the
 * parameter budget. Defaults mirror [RetryDefaults] exactly — same attempts, backoff and cap.
 */
data class RetryPolicy(
    val maxAttempts: Int = RetryDefaults.MAX_ATTEMPTS,
    val baseDelay: Duration = RetryDefaults.BaseDelay,
    val maxDelay: Duration = RetryDefaults.MaxDelay,
)

/**
 * Retries [block] on transient failures with exponential backoff + full jitter.
 *
 * Contract (S6 / D-14):
 *  - At most [RetryPolicy.maxAttempts] total attempts (initial + retries).
 *  - The per-attempt timeout lives INSIDE [block] (wrap the call in a with*Timeout
 *    there). This wrapper does NOT bound each attempt globally — the timeout is
 *    per attempt, so every retry gets a fresh full timeout.
 *  - Only transient failures retry: a [TimeoutCancellationException] from a
 *    per-attempt with*Timeout, or an error whose chain reports UNAVAILABLE
 *    (see [isTransientFailure]).
 *  - Genuine structured cancellation ([CancellationException]) always propagates
 *    immediately — never retried, never swallowed.
 *  - Idempotency is the caller's responsibility: only wrap idempotent reads, or
 *    callables replayed with the SAME idempotencyKey.
 */
@Suppress("TooGenericExceptionCaught") // must inspect any failure to decide transience
suspend fun <T> retryTransient(
    policy: RetryPolicy = RetryPolicy(),
    random: Random = Random.Default,
    isTransient: (Throwable) -> Boolean = ::isTransientFailure,
    block: suspend () -> T,
): T {
    var attempt = 1
    while (true) {
        val failure: Throwable = try {
            return block()
        } catch (e: TimeoutCancellationException) {
            e // per-attempt timeout: transient by construction (D-14)
        } catch (e: CancellationException) {
            throw e // genuine cancellation always propagates
        } catch (e: Exception) {
            e
        }
        // Timeout is always transient (D-14), even if a custom [isTransient] omits it.
        val transient = failure is TimeoutCancellationException || isTransient(failure)
        if (!transient || attempt >= policy.maxAttempts) throw failure
        delay(backoffDelay(attempt, policy.baseDelay, policy.maxDelay, random))
        attempt++
    }
}

/**
 * True if [error]'s cause chain indicates a transient, retry-worthy condition:
 * a coroutine timeout, or a backend UNAVAILABLE status. Reflection-free string
 * inspection (mirrors the wallet error mapper) keeps this in commonMain.
 */
internal fun isTransientFailure(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        val token = (current.message ?: "") + " " + (current::class.simpleName ?: "")
        if (current is TimeoutCancellationException || "UNAVAILABLE" in token) return true
        current = current.cause
    }
    return false
}

/**
 * Exponential (1x, 2x, 4x... [base]) capped at [max], then full jitter -> [0, capped).
 * Bounded to [RetryDefaults.MAX_ATTEMPTS] by default, so the shift never overflows.
 */
private fun backoffDelay(attempt: Int, base: Duration, max: Duration, random: Random): Duration {
    val exponential = base * (1 shl (attempt - 1))
    val capped = minOf(exponential, max)
    return capped * random.nextDouble()
}
