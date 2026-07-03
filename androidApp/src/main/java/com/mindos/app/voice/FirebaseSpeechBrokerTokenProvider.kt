package com.mindos.app.voice

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.mindos.android.voice.SpeechBrokerTokenProvider
import com.mindos.android.voice.SpeechBrokerTokens
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseSpeechBrokerTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val appCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance(),
) : SpeechBrokerTokenProvider {
    override suspend fun getTokens(): SpeechBrokerTokens {
        val user = requireNotNull(auth.currentUser) { "Speech broker requires an authenticated user." }
        val idToken = suspendCancellableCoroutine<String> { continuation ->
            user.getIdToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (!continuation.isActive) {
                        return@addOnSuccessListener
                    } else if (token.isNullOrBlank()) {
                        continuation.resumeWithException(IllegalStateException("Firebase ID token was empty."))
                    } else {
                        continuation.resume(token)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        val appCheckToken = suspendCancellableCoroutine<String> { continuation ->
            appCheck.getAppCheckToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (!continuation.isActive) {
                        return@addOnSuccessListener
                    } else if (token.isBlank()) {
                        continuation.resumeWithException(IllegalStateException("Firebase App Check token was empty."))
                    } else {
                        continuation.resume(token)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
        return SpeechBrokerTokens(idToken, appCheckToken)
    }
}
