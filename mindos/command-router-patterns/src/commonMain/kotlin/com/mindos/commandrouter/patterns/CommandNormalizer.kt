package com.mindos.commandrouter.patterns

interface CommandNormalizer {
    fun normalize(input: String): String
}

class DefaultCommandNormalizer : CommandNormalizer {
    override fun normalize(input: String): String =
        input.trim().lowercase().replace(Regex("\\s+"), " ")
}
