package com.mindos.app.core.ui

/**
 * Standardized user-facing error model.
 *
 * ALL errors shown to the user should be mapped to this model.
 * Internal exception messages, stack traces, and technical details
 * must NEVER be shown directly to the user.
 *
 * The `code` field is for logging/analytics, not for display.
 */
data class UserFacingError(
    /** Short human-readable title for the error. */
    val title: String,
    /** Longer description or recovery suggestion. */
    val message: String,
    /** Machine-readable error code for logging/analytics. */
    val code: String = "unknown",
    /** Whether the user can retry the action. */
    val isRetryable: Boolean = false,
)

/**
 * Maps common exceptions to [UserFacingError].
 *
 * This is the single source of truth for exception-to-user-message mapping.
 * Platform-specific exceptions can be added via extension functions.
 */
object ErrorMapper {

    fun fromThrowable(error: Throwable): UserFacingError {
        val message = error.message.orEmpty()
        return when {
            isNetworkError(message) -> networkError()
            isTimeoutError(message) -> timeoutError()
            isAuthError(message) -> authError()
            isPermissionError(message) -> permissionError()
            isNotFoundError(message) -> notFoundError()
            isRateLimitError(message) -> rateLimitError()
            isServerError(message) -> serverError()
            else -> unknownError()
        }
    }

    fun networkError() = UserFacingError(
        title = "Sin conexion",
        message = "No se pudo conectar. Verifica tu conexion a internet e intentalo de nuevo.",
        code = "network_error",
        isRetryable = true,
    )

    fun timeoutError() = UserFacingError(
        title = "Tiempo agotado",
        message = "La solicitud tardo demasiado. Intentalo de nuevo.",
        code = "timeout",
        isRetryable = true,
    )

    fun authError() = UserFacingError(
        title = "Sesion expirada",
        message = "Tu sesion ha expirado. Inicia sesion de nuevo.",
        code = "unauthorized",
        isRetryable = false,
    )

    fun permissionError() = UserFacingError(
        title = "Acceso denegado",
        message = "No tienes permiso para realizar esta accion.",
        code = "permission_denied",
        isRetryable = false,
    )

    fun notFoundError() = UserFacingError(
        title = "No encontrado",
        message = "El contenido que buscas no existe o ha sido eliminado.",
        code = "not_found",
        isRetryable = false,
    )

    fun rateLimitError() = UserFacingError(
        title = "Demasiadas solicitudes",
        message = "Has realizado demasiadas solicitudes. Espera un momento e intentalo de nuevo.",
        code = "rate_limited",
        isRetryable = true,
    )

    fun serverError() = UserFacingError(
        title = "Error del servidor",
        message = "Algo salio mal en nuestro lado. Intentalo de nuevo mas tarde.",
        code = "server_error",
        isRetryable = true,
    )

    fun validationError(field: String, reason: String) = UserFacingError(
        title = "Dato invalido",
        message = "$field: $reason",
        code = "validation_error",
        isRetryable = false,
    )

    fun unknownError() = UserFacingError(
        title = "Error inesperado",
        message = "Algo salio mal. Intentalo de nuevo.",
        code = "unknown",
        isRetryable = true,
    )

    // ── Classification helpers ──────────────────────────────────────────
    private fun isNetworkError(msg: String): Boolean =
        msg.contains("network", ignoreCase = true) ||
            msg.contains("connectivity", ignoreCase = true) ||
            msg.contains("unreachable", ignoreCase = true) ||
            msg.contains("UNAVAILABLE", ignoreCase = false) ||
            msg.contains("no internet", ignoreCase = true)

    private fun isTimeoutError(msg: String): Boolean =
        msg.contains("timeout", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true) ||
            msg.contains("DEADLINE_EXCEEDED", ignoreCase = false)

    private fun isAuthError(msg: String): Boolean =
        msg.contains("UNAUTHENTICATED", ignoreCase = false) ||
            msg.contains("unauthorized", ignoreCase = true) ||
            msg.contains("token expired", ignoreCase = true) ||
            msg.contains("auth", ignoreCase = true) && msg.contains("expired", ignoreCase = true)

    private fun isPermissionError(msg: String): Boolean =
        msg.contains("PERMISSION_DENIED", ignoreCase = false) ||
            msg.contains("forbidden", ignoreCase = true) ||
            msg.contains("403", ignoreCase = false)

    private fun isNotFoundError(msg: String): Boolean =
        msg.contains("NOT_FOUND", ignoreCase = false) ||
            msg.contains("not found", ignoreCase = true) ||
            msg.contains("404", ignoreCase = false)

    private fun isRateLimitError(msg: String): Boolean =
        msg.contains("RESOURCE_EXHAUSTED", ignoreCase = false) ||
            msg.contains("rate limit", ignoreCase = true) ||
            msg.contains("429", ignoreCase = false) ||
            msg.contains("too many requests", ignoreCase = true)

    private fun isServerError(msg: String): Boolean =
        msg.contains("INTERNAL", ignoreCase = false) ||
            msg.contains("server error", ignoreCase = true) ||
            msg.contains("500", ignoreCase = false)
}
