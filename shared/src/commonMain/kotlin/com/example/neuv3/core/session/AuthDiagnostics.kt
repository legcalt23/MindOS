package com.example.neuv3.core.session

import com.example.neuv3.core.logging.safeDebugLog
import kotlinx.coroutines.CancellationException

internal enum class AuthFailureKind(
    val logValue: String,
) {
    UserNotFound("user_not_found"),
    WrongPassword("wrong_password"),
    InvalidEmail("invalid_email"),
    InvalidLoginCredentials("invalid_login_credentials"),
    OperationNotAllowed("operation_not_allowed"),
    InvalidCustomToken("invalid_custom_token"),
    InvalidIdToken("invalid_id_token"),
    MalformedCredential("malformed_credential"),
    Network("network"),
    Unknown("unknown"),
}

internal data class AuthFailureDiagnostic(
    val code: String,
    val kind: AuthFailureKind,
    val message: String,
    val exceptionClass: String,
)

internal fun logAuth(message: String) {
    safeDebugLog("FirebaseAuth", message)
}

internal fun Throwable.toAuthFailureDiagnostic(): AuthFailureDiagnostic {
    if (this is CancellationException) throw this

    val diagnosticText = diagnosticText()
    val code = when {
        diagnosticText.hasAny("ERROR_USER_NOT_FOUND", "USER_NOT_FOUND") -> "ERROR_USER_NOT_FOUND"
        diagnosticText.hasAny("ERROR_WRONG_PASSWORD", "WRONG_PASSWORD") -> "ERROR_WRONG_PASSWORD"
        diagnosticText.hasAny("ERROR_INVALID_EMAIL", "INVALID_EMAIL") -> "ERROR_INVALID_EMAIL"
        diagnosticText.hasAny("ERROR_OPERATION_NOT_ALLOWED", "OPERATION_NOT_ALLOWED") -> "ERROR_OPERATION_NOT_ALLOWED"
        diagnosticText.hasAny("ERROR_INVALID_CUSTOM_TOKEN", "INVALID_CUSTOM_TOKEN") -> "ERROR_INVALID_CUSTOM_TOKEN"
        diagnosticText.hasAny("ERROR_INVALID_ID_TOKEN", "INVALID_ID_TOKEN") -> "ERROR_INVALID_ID_TOKEN"
        diagnosticText.hasAny("INVALID_LOGIN_CREDENTIALS") -> "INVALID_LOGIN_CREDENTIALS"
        diagnosticText.isFirebaseInvalidLoginCredentialMessage() -> "INVALID_LOGIN_CREDENTIALS"
        diagnosticText.hasAny("FirebaseAuthInvalidCredentialsException") -> "INVALID_LOGIN_CREDENTIALS"
        diagnosticText.hasAny("ERROR_INVALID_CREDENTIAL", "invalid credential") -> "INVALID_LOGIN_CREDENTIALS"
        diagnosticText.hasAny("NETWORK_ERROR", "ERROR_NETWORK_REQUEST_FAILED") -> "ERROR_NETWORK_REQUEST_FAILED"
        diagnosticText.hasAny("malformed") -> "MALFORMED_CREDENTIAL"
        else -> this::class.simpleName ?: "UNKNOWN_AUTH_ERROR"
    }

    val kind = when (code) {
        "ERROR_USER_NOT_FOUND" -> AuthFailureKind.UserNotFound
        "ERROR_WRONG_PASSWORD" -> AuthFailureKind.WrongPassword
        "ERROR_INVALID_EMAIL" -> AuthFailureKind.InvalidEmail
        "ERROR_OPERATION_NOT_ALLOWED" -> AuthFailureKind.OperationNotAllowed
        "ERROR_INVALID_CUSTOM_TOKEN" -> AuthFailureKind.InvalidCustomToken
        "ERROR_INVALID_ID_TOKEN" -> AuthFailureKind.InvalidIdToken
        "INVALID_LOGIN_CREDENTIALS" -> AuthFailureKind.InvalidLoginCredentials
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthFailureKind.Network
        "MALFORMED_CREDENTIAL" -> AuthFailureKind.MalformedCredential
        else -> AuthFailureKind.Unknown
    }

    return AuthFailureDiagnostic(
        code = code,
        kind = kind,
        message = authSafeMessage(),
        exceptionClass = rootExceptionClassName(),
    )
}

internal fun AuthFailureDiagnostic.toUserMessage(email: String): String =
    when (kind) {
        AuthFailureKind.UserNotFound ->
            "No existe ningun usuario Firebase para $email."
        AuthFailureKind.WrongPassword ->
            "Contrasena incorrecta para $email."
        AuthFailureKind.InvalidEmail ->
            "El email de login no es valido: $email."
        AuthFailureKind.InvalidLoginCredentials ->
            "Credenciales no validas para $email. Firebase no distingue si el usuario no existe o si la contrasena no coincide."
        AuthFailureKind.OperationNotAllowed ->
            "El login por email y contrasena no esta habilitado en Firebase para este proyecto."
        AuthFailureKind.InvalidCustomToken ->
            "El custom token enviado a Firebase no es valido."
        AuthFailureKind.InvalidIdToken ->
            "El ID token enviado a Firebase no es valido."
        AuthFailureKind.MalformedCredential ->
            "La credencial enviada a Firebase esta mal construida para $email."
        AuthFailureKind.Network ->
            "No se pudo conectar con Firebase Auth. Revisa la red e intentalo de nuevo."
        AuthFailureKind.Unknown ->
            message.ifBlank { "No se pudo autenticar con Firebase para $email." }
    }

internal fun passwordPresenceForLog(password: String): String =
    "password_present=${password.isNotEmpty()} password_blank=${password.isBlank()}"

private fun Throwable.diagnosticText(): String {
    val parts = mutableListOf<String>()
    var current: Throwable? = this
    while (current != null) {
        current::class.simpleName?.let(parts::add)
        current.message?.let(parts::add)
        current = current.cause
    }
    return parts.joinToString(separator = " ")
}

private fun Throwable.rootExceptionClassName(): String {
    var current: Throwable = this
    while (current.cause != null) {
        current = current.cause!!
    }
    return current::class.simpleName ?: this::class.simpleName ?: "UnknownThrowable"
}

private fun Throwable.authSafeMessage(): String {
    var current: Throwable? = this
    while (current != null) {
        val message = current.message?.trim()
        if (!message.isNullOrBlank()) return message
        current = current.cause
    }
    return ""
}

private fun String.hasAny(vararg needles: String): Boolean =
    needles.any { needle -> contains(needle, ignoreCase = true) }

private fun String.isFirebaseInvalidLoginCredentialMessage(): Boolean =
    hasAny("auth credential is incorrect, malformed or has expired")
