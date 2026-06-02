package com.mindos.action.observability

object TraceRedactor {
    fun redact(label: String, value: String): String =
        "<redacted:$label:${stableHash(value)}>"

    private fun stableHash(value: String): String {
        var hash = FnvOffset
        for (ch in value) {
            hash = hash xor ch.code.toLong()
            hash *= FnvPrime
            hash = hash and 0xFFFF_FFFFL
        }
        return hash.toString(16).padStart(8, '0')
    }

    private const val FnvOffset: Long = 0x811C9DC5L
    private const val FnvPrime: Long = 0x01000193L
}
