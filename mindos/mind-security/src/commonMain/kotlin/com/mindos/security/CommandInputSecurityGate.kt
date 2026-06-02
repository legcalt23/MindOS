package com.mindos.security

import kotlinx.serialization.Serializable

@Serializable
enum class CommandInputSource {
    TEXT,
    DEEP_LINK,
    DTO,
    NOTIFICATION,
}

@Serializable
data class SecurityContext(
    val requestId: String? = null,
    val source: CommandInputSource,
    val localeTag: String = "und",
) {
    init {
        require(localeTag.isNotBlank()) { "localeTag must not be blank." }
    }
}

@Serializable
sealed interface SecurityDecision {
    @Serializable
    data object Allowed : SecurityDecision

    @Serializable
    data class Rejected(
        val codes: List<String>,
        val evidenceRedacted: String,
    ) : SecurityDecision {
        init {
            require(codes.isNotEmpty()) { "codes must not be empty." }
            require(codes.all { it.isNotBlank() }) { "codes must not contain blanks." }
            require(evidenceRedacted.isNotBlank()) { "evidenceRedacted must not be blank." }
        }
    }
}

interface CommandInputSecurityGate {
    fun inspectText(input: String, context: SecurityContext): SecurityDecision
    fun inspectDeepLink(rawLink: String, context: SecurityContext): SecurityDecision
    fun inspectResolvedFields(fields: Map<String, String>, context: SecurityContext): SecurityDecision
}

class DefaultCommandInputSecurityGate(
    private val policy: InputSecurityPolicy = InputSecurityPolicy.Default,
) : CommandInputSecurityGate {

    override fun inspectText(input: String, context: SecurityContext): SecurityDecision =
        inspectValue(input, rejectWarnings = false, label = "text")

    override fun inspectDeepLink(rawLink: String, context: SecurityContext): SecurityDecision {
        val decoded = percentDecode(rawLink)
            ?: return SecurityDecision.Rejected(listOf("MALFORMED_PERCENT_ENCODING"), "<redacted:deep_link>")
        val pieces = listOf(rawLink, decoded) + decoded
            .substringAfter("://", decoded)
            .split('/', '?', '#', '&', '=')
            .filter { it.isNotBlank() }

        val rejected = pieces.asSequence()
            .map { inspectValue(it, rejectWarnings = true, label = "deep_link") }
            .filterIsInstance<SecurityDecision.Rejected>()
            .firstOrNull()
        return rejected ?: SecurityDecision.Allowed
    }

    override fun inspectResolvedFields(fields: Map<String, String>, context: SecurityContext): SecurityDecision {
        fields.forEach { (field, value) ->
            if (field.isBlank()) {
                return SecurityDecision.Rejected(listOf("BLANK_FIELD_NAME"), "<redacted:field>")
            }
            val decision = inspectValue(value, rejectWarnings = true, label = field)
            if (decision is SecurityDecision.Rejected) return decision
        }
        return SecurityDecision.Allowed
    }

    private fun inspectValue(value: String, rejectWarnings: Boolean, label: String): SecurityDecision {
        val report = InputSecurityInspector.inspect(value, policy)
        val rejectedCodes = report.rejectionCodes.map { it.name }
        val warningCodes = if (rejectWarnings) report.warningCodes.map { it.name } else emptyList()
        val codes = rejectedCodes + warningCodes
        return if (codes.isEmpty()) {
            SecurityDecision.Allowed
        } else {
            SecurityDecision.Rejected(
                codes = codes.distinct(),
                evidenceRedacted = "<redacted:$label:${stableHash(value)}>",
            )
        }
    }

    private fun percentDecode(value: String): String? {
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val ch = value[index]
            if (ch != '%') {
                out.append(ch)
                index += 1
                continue
            }
            if (index + 2 >= value.length) return null
            val high = value[index + 1].hexValue() ?: return null
            val low = value[index + 2].hexValue() ?: return null
            out.append(((high shl 4) + low).toChar())
            index += 3
        }
        return out.toString()
    }

    private fun Char.hexValue(): Int? =
        when (this) {
            in '0'..'9' -> code - '0'.code
            in 'a'..'f' -> code - 'a'.code + 10
            in 'A'..'F' -> code - 'A'.code + 10
            else -> null
        }

    private fun stableHash(value: String): String {
        var hash = FnvOffset
        for (ch in value) {
            hash = hash xor ch.code.toUInt()
            hash *= FnvPrime
        }
        return hash.toString(16).padStart(8, '0')
    }

    private companion object {
        val FnvOffset: UInt = 0x811C9DC5u
        const val FnvPrime: UInt = 0x01000193u
    }
}
