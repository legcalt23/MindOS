package com.mindos.app.features.profile.domain.model

/** Result of validating + normalizing a user-entered custom link URL. */
sealed interface LinkValidationResult {
    data class Valid(val normalizedUrl: String) : LinkValidationResult
    data class Invalid(val message: String) : LinkValidationResult
}

/**
 * Pure URL validation / normalization for custom profile links.
 *
 * - Accepts only `http`/`https`.
 * - A bare domain without scheme (`mindos.app`) is normalized to `https://`.
 * - Rejects empty input, whitespace, schemes other than http/https, and hosts
 *   without a dot (which can't resolve to a real site).
 */
object ProfileLinkValidator {

    private const val HTTPS_PREFIX = "https://"
    private const val HTTP_PREFIX = "http://"

    fun validate(raw: String): LinkValidationResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return LinkValidationResult.Invalid("Introduce un enlace web.")
        if (trimmed.any { it.isWhitespace() }) {
            return LinkValidationResult.Invalid("El enlace no puede contener espacios.")
        }

        val lower = trimmed.lowercase()
        val hasScheme = lower.startsWith(HTTPS_PREFIX) || lower.startsWith(HTTP_PREFIX)
        val hasUnsupportedScheme = !hasScheme && trimmed.contains("://")
        if (hasUnsupportedScheme) {
            return LinkValidationResult.Invalid("Solo se permiten enlaces http o https.")
        }

        val normalized = if (hasScheme) trimmed else "$HTTPS_PREFIX$trimmed"
        val host = normalized
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')

        if (!isValidHost(host)) {
            return LinkValidationResult.Invalid("Introduce un dominio válido (ej. mindos.app).")
        }

        return LinkValidationResult.Valid(normalized)
    }

    private fun isValidHost(host: String): Boolean =
        host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
}
