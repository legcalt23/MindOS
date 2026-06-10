package com.mindos.app.core.network

import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
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
