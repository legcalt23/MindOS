package com.mindos.action.normalization

/**
 * Deterministic normalization + single-pass tokenization.
 *
 * Global text passes (case-preserving): trim → strip control → NFC → collapse whitespace.
 * Case folding and diacritic folding are applied **per token** so each [Token] keeps
 * [Token.raw] (original case), [Token.display] (folded case), and [Token.normalized]
 * (optionally diacritic-folded). This lets case-sensitive ids and accent-bearing free
 * text survive while static command tokens still match loosely.
 *
 * Hot-path guarantees: no `Regex` is compiled or executed per call; every step is a
 * linear char-scan; tokenization happens exactly once.
 */
class NormalizationPipeline(private val config: NormalizationConfig = NormalizationConfig.Default) {

    fun normalize(raw: String): NormalizedInput {
        var text = TrimStep.apply(raw)
        if (config.stripControlChars) text = ControlCharStep.apply(text)
        if (config.applyNfc) text = NfcStep.apply(text)
        if (config.collapseWhitespace) text = WhitespaceCollapseStep.apply(text)
        // `text` is now the case-preserved canonical text; spans below index into it.

        val tokens = tokenize(text)
        val normalizedText = joinBy(tokens) { it.normalized }
        val displayText = joinBy(tokens) { it.display }
        val rawText = joinBy(tokens) { it.raw }
        return NormalizedInput(
            raw = raw,
            text = normalizedText,
            displayText = displayText,
            rawText = rawText,
            tokens = tokens,
            config = config,
        )
    }

    /** Single-pass split on ASCII space (text is already whitespace-collapsed). */
    private fun tokenize(text: String): List<Token> {
        if (text.isEmpty()) return emptyList()
        val tokens = ArrayList<Token>()
        var start = 0
        var i = 0
        val n = text.length
        while (i <= n) {
            if (i == n || text[i] == ' ') {
                if (i > start) {
                    val rawTok = text.substring(start, i)
                    val display = CaseFoldStep.apply(rawTok)
                    val normalized = if (config.foldDiacritics) DiacriticFoldStep.apply(display) else display
                    tokens.add(Token(raw = rawTok, display = display, normalized = normalized, start = start, end = i))
                }
                start = i + 1
            }
            i++
        }
        return tokens
    }

    private inline fun joinBy(tokens: List<Token>, selector: (Token) -> String): String =
        buildString {
            tokens.forEachIndexed { i, t ->
                if (i > 0) append(' ')
                append(selector(t))
            }
        }
}
