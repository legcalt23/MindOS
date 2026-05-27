package com.example.mindos.core.session

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
