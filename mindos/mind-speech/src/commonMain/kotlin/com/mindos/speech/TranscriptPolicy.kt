package com.mindos.speech

fun interface TranscriptPolicy {
    fun evaluate(transcript: TranscriptResult): TranscriptPolicyResult
}

data class TranscriptPolicyConfig(
    val confidenceThreshold: Float = 0.80f,
    val acceptNullConfidence: Boolean = true,
    val maxTextLength: Int = 500,
    val requireWakeWord: Boolean = false,
    val wakeWords: Set<String> = setOf("MindVerse", "MindOS"),
) {
    init {
        require(confidenceThreshold in 0.0f..1.0f) { "confidenceThreshold must be between 0 and 1." }
        require(maxTextLength > 0) { "maxTextLength must be positive." }
        require(wakeWords.all { it.isNotBlank() }) { "wakeWords must not contain blanks." }
        require(!requireWakeWord || wakeWords.isNotEmpty()) {
            "wakeWords must not be empty when requireWakeWord is true."
        }
    }

    companion object {
        fun from(runtime: VoiceRuntimeConfig, maxTextLength: Int = 500) = TranscriptPolicyConfig(
            confidenceThreshold = runtime.confidenceThreshold,
            acceptNullConfidence = runtime.acceptNullConfidence,
            maxTextLength = maxTextLength,
            requireWakeWord = runtime.requireWakeWord,
            wakeWords = runtime.wakeWords,
        )
    }
}

sealed interface TranscriptPolicyResult {
    data class Accepted(
        val text: String,
        val languageTag: String,
    ) : TranscriptPolicyResult

    data class Rejected(val reason: VoiceRejectionReason) : TranscriptPolicyResult
}

class DefaultTranscriptPolicy(
    private val config: TranscriptPolicyConfig = TranscriptPolicyConfig(),
) : TranscriptPolicy {

    override fun evaluate(transcript: TranscriptResult): TranscriptPolicyResult {
        if (!transcript.isFinal) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.TRANSCRIPT_NOT_FINAL)
        }
        if (transcript.text.length > config.maxTextLength) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.TRANSCRIPT_TOO_LONG)
        }
        if (!confidenceAccepted(transcript.confidence)) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.CONFIDENCE_TOO_LOW)
        }

        val trimmed = transcript.text.trim()
        if (trimmed.isEmpty()) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.TRANSCRIPT_EMPTY)
        }

        val wakeWordMatch = config.wakeWords
            .sortedByDescending { it.length }
            .firstOrNull { trimmed.startsWithWakeWord(it) }
        if (config.requireWakeWord && wakeWordMatch == null) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.WAKE_WORD_MISSING)
        }

        val withoutWakeWord = if (wakeWordMatch == null) {
            trimmed
        } else {
            trimmed
                .drop(wakeWordMatch.length)
                .dropWhile { it.isWhitespace() || it in WakeWordSeparators }
        }
        val normalized = withoutWakeWord
            .trimEnd()
            .dropLastWhile { it in TerminalPunctuation }
            .trimEnd()

        if (normalized.isEmpty()) {
            return TranscriptPolicyResult.Rejected(VoiceRejectionReason.TRANSCRIPT_EMPTY)
        }
        return TranscriptPolicyResult.Accepted(
            text = normalized,
            languageTag = transcript.languageTag,
        )
    }

    private fun confidenceAccepted(confidence: Float?): Boolean =
        when {
            confidence == null -> config.acceptNullConfidence
            else -> confidence >= config.confidenceThreshold
        }

    private fun String.startsWithWakeWord(wakeWord: String): Boolean {
        if (!startsWith(wakeWord, ignoreCase = true)) return false
        val next = getOrNull(wakeWord.length) ?: return true
        return next.isWhitespace() || next in WakeWordSeparators
    }

    private companion object {
        val WakeWordSeparators = setOf(',', ':', '-')
        val TerminalPunctuation = setOf('.', '?', '!')
    }
}
