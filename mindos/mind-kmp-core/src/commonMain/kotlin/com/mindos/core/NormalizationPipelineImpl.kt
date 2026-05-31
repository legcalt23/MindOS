package com.mindos.core

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.normalization.NormalizationPipeline

data class NormalizedCommand(
    val text: String,
    val displayText: String,
    val rawText: String,
    val tokens: List<String>,
)

class NormalizationPipelineImpl(
    config: NormalizationConfig = NormalizationConfig.Default,
) {
    private val delegate = NormalizationPipeline(config)

    fun normalize(input: String): NormalizedCommand {
        val normalized = delegate.normalize(input)
        return NormalizedCommand(
            text = normalized.text,
            displayText = normalized.displayText,
            rawText = normalized.rawText,
            tokens = normalized.tokens.map { it.normalized },
        )
    }
}
