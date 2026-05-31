package com.mindos.core

import com.mindos.action.entity.EntityExtractionResult
import com.mindos.action.entity.EntityExtractorRegistry
import com.mindos.action.entity.EntityType
import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.normalization.NormalizationPipeline

sealed interface CoreEntityExtractionResult {
    data class Extracted(
        val typeId: String,
        val raw: String,
        val value: String,
    ) : CoreEntityExtractionResult

    data class Rejected(
        val typeId: String,
        val reason: String,
    ) : CoreEntityExtractionResult
}

class EntityExtractorImpl(
    private val registry: EntityExtractorRegistry = EntityExtractorRegistry.default(),
    normalizationConfig: NormalizationConfig = NormalizationConfig.Default,
) {
    private val normalizer = NormalizationPipeline(normalizationConfig)

    fun extract(input: String, typeId: String): CoreEntityExtractionResult {
        if (typeId == "date") return extractIsoDate(input)
        val type = EntityType.byId(typeId)
            ?: return CoreEntityExtractionResult.Rejected(typeId, "unsupported entity type")
        val extractor = registry.forType(type)
            ?: return CoreEntityExtractionResult.Rejected(typeId, "no extractor registered")
        val normalized = normalizer.normalize(input)
        return when (val result = extractor.extract(normalized, 0, normalized.tokens.size)) {
            is EntityExtractionResult.Extracted -> CoreEntityExtractionResult.Extracted(
                typeId = result.entity.type.id,
                raw = result.entity.raw,
                value = result.entity.value,
            )
            is EntityExtractionResult.Rejected -> CoreEntityExtractionResult.Rejected(
                typeId = result.type.id,
                reason = result.reason.detail,
            )
        }
    }

    private fun extractIsoDate(input: String): CoreEntityExtractionResult {
        val value = input.trim()
        if (!IsoDate.matches(value) || !validIsoDate(value)) {
            return CoreEntityExtractionResult.Rejected("date", "date must use valid yyyy-MM-dd shape")
        }
        return CoreEntityExtractionResult.Extracted("date", value, value)
    }

    private fun validIsoDate(value: String): Boolean {
        val month = value.substring(5, 7).toInt()
        val day = value.substring(8, 10).toInt()
        if (month !in 1..12) return false
        val maxDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> 29
        }
        return day in 1..maxDay
    }

    companion object {
        private val IsoDate = Regex("""^\d{4}-\d{2}-\d{2}$""")
    }
}
