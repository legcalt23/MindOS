package com.example.neuv3.core.security

object SafeInput {
    private val safeIdRegex = Regex("^[A-Za-z0-9_-]{3,128}$")
    private val usernameRegex = Regex("^[a-z0-9._]{3,30}$")
    private val repeatedWhitespaceRegex = Regex("[ \\t\\x0B\\f]+")
    private val repeatedNewlineRegex = Regex("\\n{3,}")

    fun hasBlockedControlChars(value: String, allowNewlines: Boolean = false): Boolean =
        value.any { char -> char.isBlockedControlChar(allowNewlines) }

    fun isSafeDocumentId(value: String): Boolean =
        safeIdRegex.matches(value) &&
            !value.contains("..") &&
            !value.contains("/") &&
            !value.contains("\\") &&
            value.none { it.isBlockedControlChar(allowNewlines = false) }

    fun requireSafeDocumentId(value: String, field: String = "id"): String {
        require(isSafeDocumentId(value)) { "$field contiene caracteres no permitidos." }
        return value
    }

    fun normalizeUsername(value: String): String =
        value
            .trim()
            .removePrefix("@")
            .lowercase()
            .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
            .joinToString("")
            .trim('.', '_')
            .replace(Regex("\\.{2,}"), ".")
            .replace(Regex("_{2,}"), "_")
            .take(30)

    fun isSafeUsername(value: String): Boolean =
        usernameRegex.matches(value) &&
            !value.startsWith(".") &&
            !value.endsWith(".") &&
            !value.contains("..") &&
            !hasBlockedControlChars(value)

    fun normalizeSingleLineHumanText(value: String, maxLength: Int): String =
        value
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(repeatedWhitespaceRegex, " ")
            .trim()
            .take(maxLength)

    fun normalizeMultilineHumanText(value: String, maxLength: Int): String =
        value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .joinToString("\n") { line -> line.replace(repeatedWhitespaceRegex, " ").trimEnd() }
            .replace(repeatedNewlineRegex, "\n\n")
            .trim()
            .take(maxLength)

    fun isSafeSingleLineHumanText(value: String, maxLength: Int, minLength: Int = 0): Boolean {
        val normalized = normalizeSingleLineHumanText(value, maxLength)
        return normalized.length in minLength..maxLength && !hasBlockedControlChars(value)
    }

    fun isSafeMultilineHumanText(value: String, maxLength: Int, minLength: Int = 0): Boolean {
        val normalized = normalizeMultilineHumanText(value, maxLength)
        return normalized.length in minLength..maxLength &&
            !hasBlockedControlChars(value, allowNewlines = true)
    }

    fun normalizeSearchQuery(value: String, maxLength: Int = 80): String =
        normalizeSingleLineHumanText(value, maxLength)

    private fun Char.isBlockedControlChar(allowNewlines: Boolean): Boolean {
        if (allowNewlines && (this == '\n' || this == '\t')) return false
        return code in 0..31 || code == 127
    }
}
