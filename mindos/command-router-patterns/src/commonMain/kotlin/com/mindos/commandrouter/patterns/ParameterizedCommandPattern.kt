package com.mindos.commandrouter.patterns

private sealed interface TemplateToken {
    data class Static(val value: String) : TemplateToken
    data class Param(val name: String) : TemplateToken
}

class ParameterizedCommandPattern(
    override val id: PatternId,
    val template: String,
    override val priority: PatternPriority = PatternPriority.Default
) : CommandPattern {

    private val tokens: List<TemplateToken> = parseTemplate(template)
    private val staticCount = tokens.count { it is TemplateToken.Static }
    private val paramCount = tokens.count { it is TemplateToken.Param }

    override fun match(normalizedInput: String): PatternMatch? {
        if (tokens.isEmpty()) return null
        val inputTokens = normalizedInput.split(" ").filter { it.isNotEmpty() }
        val params = matchTokens(tokens, inputTokens) ?: return null
        return PatternMatch(id, params)
    }

    override fun specificity() = PatternSpecificity(staticTokenCount = staticCount, parameterCount = paramCount)

    companion object {
        private val PARAM_RE = Regex("^\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}$")

        private fun parseTemplate(template: String): List<TemplateToken> =
            template.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { tok ->
                val m = PARAM_RE.matchEntire(tok)
                if (m != null) TemplateToken.Param(m.groupValues[1]) else TemplateToken.Static(tok)
            }

        private fun matchTokens(
            tokens: List<TemplateToken>,
            inputTokens: List<String>
        ): Map<String, String>? {
            val result = mutableMapOf<String, String>()
            var inputIdx = 0

            for (tIdx in tokens.indices) {
                when (val tok = tokens[tIdx]) {
                    is TemplateToken.Static -> {
                        if (inputIdx >= inputTokens.size || inputTokens[inputIdx] != tok.value) return null
                        inputIdx++
                    }
                    is TemplateToken.Param -> {
                        val isLast = tIdx == tokens.lastIndex
                        if (inputIdx >= inputTokens.size) return null
                        if (isLast) {
                            result[tok.name] = inputTokens.drop(inputIdx).joinToString(" ")
                            inputIdx = inputTokens.size
                        } else {
                            result[tok.name] = inputTokens[inputIdx]
                            inputIdx++
                        }
                    }
                }
            }
            return if (inputIdx == inputTokens.size) result else null
        }
    }
}
