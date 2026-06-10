package com.mindos.app.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AuthDiagnosticsTest {
    @Test
    fun firebaseCombinedInvalidCredentialMessageMapsToInvalidLoginCredentials() {
        val error = IllegalStateException(
            "The supplied auth credential is incorrect, malformed or has expired."
        )

        val diagnostic = error.toAuthFailureDiagnostic()

        assertEquals("INVALID_LOGIN_CREDENTIALS", diagnostic.code)
        assertEquals(AuthFailureKind.InvalidLoginCredentials, diagnostic.kind)
    }

    @Test
    fun explicitWrongPasswordCodeKeepsWrongPasswordClassification() {
        val error = IllegalStateException("ERROR_WRONG_PASSWORD")

        val diagnostic = error.toAuthFailureDiagnostic()

        assertEquals("ERROR_WRONG_PASSWORD", diagnostic.code)
        assertEquals(AuthFailureKind.WrongPassword, diagnostic.kind)
    }

    @Test
    fun standaloneMalformedMessageKeepsMalformedCredentialClassification() {
        val error = IllegalStateException("Custom token is malformed")

        val diagnostic = error.toAuthFailureDiagnostic()

        assertEquals("MALFORMED_CREDENTIAL", diagnostic.code)
        assertEquals(AuthFailureKind.MalformedCredential, diagnostic.kind)
    }

    // ── Account enumeration prevention tests ────────────────────────────

    @Test
    fun userNotFoundAndWrongPasswordProduceSameUserMessage() {
        val notFoundMsg = AuthFailureDiagnostic(
            code = "ERROR_USER_NOT_FOUND",
            kind = AuthFailureKind.UserNotFound,
            message = "user not found",
            exceptionClass = "FirebaseAuthException",
        ).toUserMessage("test@example.com")

        val wrongPwdMsg = AuthFailureDiagnostic(
            code = "ERROR_WRONG_PASSWORD",
            kind = AuthFailureKind.WrongPassword,
            message = "wrong password",
            exceptionClass = "FirebaseAuthException",
        ).toUserMessage("test@example.com")

        assertEquals(notFoundMsg, wrongPwdMsg, "User-facing messages must be identical to prevent account enumeration")
    }

    @Test
    fun credentialErrorMessagesNeverContainEmail() {
        val email = "secret@example.com"
        val kinds = listOf(
            AuthFailureKind.UserNotFound,
            AuthFailureKind.WrongPassword,
            AuthFailureKind.InvalidLoginCredentials,
            AuthFailureKind.MalformedCredential,
        )
        for (kind in kinds) {
            val msg = AuthFailureDiagnostic(
                code = "TEST",
                kind = kind,
                message = "",
                exceptionClass = "Test",
            ).toUserMessage(email)
            assertFalse(
                msg.contains(email),
                "Message for $kind must not contain the email address"
            )
        }
    }

    @Test
    fun allCredentialKindsMergeToSingleMessage() {
        val credentialKinds = listOf(
            AuthFailureKind.UserNotFound,
            AuthFailureKind.WrongPassword,
            AuthFailureKind.InvalidLoginCredentials,
            AuthFailureKind.MalformedCredential,
        )
        val messages = credentialKinds.map { kind ->
            AuthFailureDiagnostic(
                code = "TEST",
                kind = kind,
                message = "",
                exceptionClass = "Test",
            ).toUserMessage("any@email.com")
        }.toSet()

        assertEquals(1, messages.size, "All credential failure kinds must produce the same message")
    }

    @Test
    fun networkErrorMessageDoesNotLeakEmail() {
        val email = "test@example.com"
        val msg = AuthFailureDiagnostic(
            code = "ERROR_NETWORK",
            kind = AuthFailureKind.Network,
            message = "",
            exceptionClass = "IOException",
        ).toUserMessage(email)

        assertFalse(msg.contains(email), "Network error message must not contain email")
    }
}
