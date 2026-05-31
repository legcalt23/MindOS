package com.mindos.security

import kotlinx.serialization.Serializable

@Serializable
enum class InputSecuritySeverity { WARNING, REJECT }

@Serializable
enum class InputSecurityCode {
    INPUT_TOO_LONG,
    INVALID_CONTROL_CHARACTER,
    MALFORMED_UNICODE,
    MIXED_SCRIPT,
    HOMOGLYPH_CONFUSABLE,
}

@Serializable
data class InputSecurityFinding(
    val code: InputSecurityCode,
    val severity: InputSecuritySeverity,
    val detail: String,
)

@Serializable
data class InputSecurityPolicy(
    val maxInputLength: Int = 512,
    val rejectControlCharacters: Boolean = true,
    val rejectMalformedUnicode: Boolean = true,
    val detectMixedScripts: Boolean = true,
    val detectConfusables: Boolean = true,
) {
    init {
        require(maxInputLength > 0) { "maxInputLength must be positive." }
    }

    companion object {
        val Default = InputSecurityPolicy()
    }
}

@Serializable
data class InputSecurityReport(
    val findings: List<InputSecurityFinding>,
) {
    val hasRejections: Boolean
        get() = findings.any { it.severity == InputSecuritySeverity.REJECT }

    val rejectionCodes: List<InputSecurityCode>
        get() = findings.filter { it.severity == InputSecuritySeverity.REJECT }.map { it.code }

    val warningCodes: List<InputSecurityCode>
        get() = findings.filter { it.severity == InputSecuritySeverity.WARNING }.map { it.code }

    companion object {
        val Clean = InputSecurityReport(emptyList())
    }
}

object InputSecurityInspector {

    fun inspect(raw: String, policy: InputSecurityPolicy = InputSecurityPolicy.Default): InputSecurityReport {
        val findings = ArrayList<InputSecurityFinding>()
        if (raw.length > policy.maxInputLength) {
            findings += InputSecurityFinding(
                code = InputSecurityCode.INPUT_TOO_LONG,
                severity = InputSecuritySeverity.REJECT,
                detail = "input length ${raw.length} exceeds ${policy.maxInputLength}",
            )
        }

        val scripts = LinkedHashSet<ScriptClass>()
        val confusableCodePoints = LinkedHashSet<String>()
        var index = 0
        while (index < raw.length) {
            val ch = raw[index]
            when {
                ch.isHighSurrogate() -> {
                    if (index + 1 >= raw.length || !raw[index + 1].isLowSurrogate()) {
                        if (policy.rejectMalformedUnicode) {
                            findings += InputSecurityFinding(
                                code = InputSecurityCode.MALFORMED_UNICODE,
                                severity = InputSecuritySeverity.REJECT,
                                detail = "unpaired high surrogate at index $index",
                            )
                        }
                    } else {
                        index += 1
                    }
                }
                ch.isLowSurrogate() && policy.rejectMalformedUnicode -> {
                    findings += InputSecurityFinding(
                        code = InputSecurityCode.MALFORMED_UNICODE,
                        severity = InputSecuritySeverity.REJECT,
                        detail = "unpaired low surrogate at index $index",
                    )
                }
            }

            if (policy.rejectControlCharacters && ch.isUnsafeControlOrFormat()) {
                findings += InputSecurityFinding(
                    code = InputSecurityCode.INVALID_CONTROL_CHARACTER,
                    severity = InputSecuritySeverity.REJECT,
                    detail = "control/format character U+${ch.code.hex4()} at index $index",
                )
            }

            scriptOf(ch)?.let { scripts += it }
            if (policy.detectConfusables && ch in ConfusableCharacters) {
                confusableCodePoints += "U+${ch.code.hex4()}"
            }
            index += 1
        }

        if (policy.detectMixedScripts && scripts.size > 1 && scripts.contains(ScriptClass.LATIN)) {
            findings += InputSecurityFinding(
                code = InputSecurityCode.MIXED_SCRIPT,
                severity = InputSecuritySeverity.WARNING,
                detail = "mixed scripts: ${scripts.joinToString(",") { it.name.lowercase() }}",
            )
        }

        if (confusableCodePoints.isNotEmpty()) {
            findings += InputSecurityFinding(
                code = InputSecurityCode.HOMOGLYPH_CONFUSABLE,
                severity = InputSecuritySeverity.WARNING,
                detail = "confusable characters: ${confusableCodePoints.joinToString(",")}",
            )
        }

        return if (findings.isEmpty()) InputSecurityReport.Clean else InputSecurityReport(findings)
    }

    private fun Char.isUnsafeControlOrFormat(): Boolean {
        if (this == '\t' || this == '\n' || this == '\r') return false
        return category == CharCategory.CONTROL || category == CharCategory.FORMAT
    }

    private fun scriptOf(ch: Char): ScriptClass? =
        when (ch.code) {
            in 0x0041..0x005A,
            in 0x0061..0x007A,
            in 0x00C0..0x024F,
            -> ScriptClass.LATIN

            in 0x0370..0x03FF -> ScriptClass.GREEK
            in 0x0400..0x052F -> ScriptClass.CYRILLIC
            else -> null
        }

    private fun Int.hex4(): String =
        toString(16).uppercase().padStart(4, '0')

    private enum class ScriptClass { LATIN, GREEK, CYRILLIC }

    private val ConfusableCharacters: Set<Char> = setOf(
        '\u0391', // Greek Alpha
        '\u0392', // Greek Beta
        '\u0395', // Greek Epsilon
        '\u0397', // Greek Eta
        '\u0399', // Greek Iota
        '\u039A', // Greek Kappa
        '\u039C', // Greek Mu
        '\u039D', // Greek Nu
        '\u039F', // Greek Omicron
        '\u03A1', // Greek Rho
        '\u03A4', // Greek Tau
        '\u03A7', // Greek Chi
        '\u03B1', // Greek alpha
        '\u03BF', // Greek omicron
        '\u0440', // Cyrillic er
        '\u0441', // Cyrillic es
        '\u0443', // Cyrillic u
        '\u0445', // Cyrillic ha
        '\u0430', // Cyrillic a
        '\u0435', // Cyrillic ie
        '\u043E', // Cyrillic o
        '\u0456', // Cyrillic byelorussian-ukrainian i
        '\u0410', // Cyrillic A
        '\u0412', // Cyrillic Ve
        '\u0415', // Cyrillic Ie
        '\u041A', // Cyrillic Ka
        '\u041C', // Cyrillic Em
        '\u041D', // Cyrillic En
        '\u041E', // Cyrillic O
        '\u0420', // Cyrillic Er
        '\u0421', // Cyrillic Es
        '\u0422', // Cyrillic Te
        '\u0425', // Cyrillic Ha
    )
}
