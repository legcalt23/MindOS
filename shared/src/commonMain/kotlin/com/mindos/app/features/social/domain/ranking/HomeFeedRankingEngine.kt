package com.mindos.app.features.social.domain.ranking

import com.mindos.app.features.social.domain.publishing.CandidatePlane
import com.mindos.app.features.social.domain.publishing.EnrichedFeedCandidate
import com.mindos.app.features.social.domain.publishing.RankedFeedCandidate
import com.mindos.app.features.social.domain.valueobject.Timestamp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Ranker heurístico V1 para el Home Feed.
 * Desacoplado del almacenamiento, determinista y preparado para futura migración a modelos ML.
 *
 * Implementa evaluación de múltiples factores definidos por el sistema.
 */
class HomeFeedRankingEngine(
    private val config: RankingConfig = RankingConfig(),
    private val random: Random = Random.Default,
) {
    data class RankingConfig(
        val halfLifeMillis: Long = 12 * 60 * 60 * 1000L, // 12 hours basic decay
        val followAffinityBonus: Double = 2.0,
        val baseQualityMediaBonus: Double = 0.5,
        val textLengthPeakSubchar: Int = 200,
        val fatiguePenaltyFactor: Double = 0.8,
        val repetitionPenaltyBase: Double = 0.85,
        val safetyRiskPenaltyLimit: Double = 0.1, // Minimum multiplier when risk is max
        val explorationBoostMax: Double = 0.25,
        val noveltyBoostMax: Double = 0.3,
    )

    fun rankCandidates(
        candidates: List<EnrichedFeedCandidate>,
        now: Timestamp,
    ): List<RankedFeedCandidate> {
        if (candidates.isEmpty()) return emptyList()

        // 1. Extraemos features (preparado para enviarse al Feature Store en el futuro).
        // Suprimido intencionalmente: el cómputo no se consume todavía, pero ejercita
        // [extractFeatures] (que valida la forma de los candidatos) y mantiene el call
        // site listo para cuando el Feature Store esté online.
        @Suppress("UnusedPrivateProperty")
        val featureVectors = candidates.associate { it.candidate.id to extractFeatures(it) }

        // 2. Ordenamos por edad como línea base para aplicar contexto ordinal (ej: repetición)
        val timeOrdered = candidates.sortedBy { it.features.ageMillis }
        val authorSeenCounts = mutableMapOf<String, Int>()

        // 3. Calculamos la puntuación heurística
        val heuristicallyScored = timeOrdered.map { candidate ->
            val authorId = candidate.postSnapshot.authorId.value
            val timesSeen = authorSeenCounts.getOrElse(authorId) { 0 }
            authorSeenCounts[authorId] = timesSeen + 1

            val repetitionPenalty = calculateRepetitionPenalty(timesSeen)
            val score = scoreCandidate(candidate, repetitionPenalty)

            RankedFeedCandidate(
                candidate = candidate.candidate,
                score = score,
                rankedAt = now,
                features = candidate.features, // Conservar features originales para logs ML
                modelVersion = "heuristic.feed.v1",
            )
        }

        // 4. Aplicamos diversidad mediante un re-ranking de proximidad
        val diversified = applyDiversityPostProcessing(heuristicallyScored)

        // 5. Orden final
        return diversified.sortedByDescending { it.score }
    }

    private fun extractFeatures(enriched: EnrichedFeedCandidate): Map<String, Any> {
        // En un futuro este vector se serializa a Kafka/Feature Store
        val f = enriched.features
        return mapOf(
            "ageMillis" to f.ageMillis,
            "viewerFollowsAuthor" to f.viewerFollowsAuthor,
            "viewerIsAuthor" to f.viewerIsAuthor,
            "sourcePlane" to f.sourcePlane.name,
            "sourceWeight" to f.sourceWeight,
            "hasMedia" to f.hasMedia,
            "textLength" to f.textLength,
            "authorFatigue" to f.authorFatiguePenalty,
            "moderationPenalty" to f.moderationPenalty,
        )
    }

    private fun calculateRepetitionPenalty(timesSeen: Int): Double {
        // Penalización exponencial por cada aparición consecutiva del autor en el mismo contexto
        return config.repetitionPenaltyBase.pow(timesSeen.toDouble())
    }

    private fun scoreCandidate(
        candidate: EnrichedFeedCandidate,
        repetitionPenalty: Double,
    ): Double {
        val f = candidate.features

        // A. Recency Decay
        val lambda = ln(2.0) / config.halfLifeMillis
        val recencyDecay = exp(-lambda * max(0L, f.ageMillis).toDouble())

        // B. Afinidad Viewer-Autor
        var viewerAuthorAffinity = if (f.viewerIsAuthor) 3.0 else 1.0
        if (f.viewerFollowsAuthor) {
            viewerAuthorAffinity += config.followAffinityBonus
        }
        viewerAuthorAffinity += (f.sourceWeight * 0.4) // Peso de la ruta gráfica

        // C. Afinidad Viewer-Tema
        val viewerTopicAffinity = if (f.sourcePlane == CandidatePlane.Recommendation) {
            1.0 + f.sourceWeight
        } else {
            1.0
        }

        // D. Calidad Base del Contenido
        var baseQuality = 1.0
        if (f.hasMedia) baseQuality += config.baseQualityMediaBonus
        val textLengthFactor = 1.0 - min(1.0, abs(f.textLength - config.textLengthPeakSubchar) / 400.0)
        baseQuality += (textLengthFactor * 0.3)

        // E. Probabilidad de Interacción Útil (PUI)
        val predictedPui = baseQuality * (viewerAuthorAffinity * 0.65 + viewerTopicAffinity * 0.35)

        // F. Novedad
        val novelty = if (f.sourcePlane == CandidatePlane.Recommendation && f.ageMillis < 3600_000) {
            config.noveltyBoostMax
        } else {
            0.0
        }

        // G. Exploración Controlada
        val explorationBoost = if (f.sourcePlane == CandidatePlane.Exploration) {
            random.nextDouble(0.0, config.explorationBoostMax)
        } else {
            0.0
        }

        // H. Penalizaciones por Riesgo y Fatiga
        val integrityRiskPenalty = 1.0 - (f.moderationPenalty * (1.0 - config.safetyRiskPenaltyLimit))
        val actualFatiguePenalty = 1.0 - (f.authorFatiguePenalty * (1.0 - config.fatiguePenaltyFactor))

        // I. Composición de Score Base
        var score = (predictedPui * recencyDecay) + novelty + explorationBoost
        
        // J. Aplicación de Penalizaciones
        score *= repetitionPenalty
        score *= actualFatiguePenalty
        score *= integrityRiskPenalty

        return score
    }

    private fun applyDiversityPostProcessing(
        ranked: List<RankedFeedCandidate>,
    ): List<RankedFeedCandidate> {
        // Técnica de re-ranking basada en penalización de vecinos para promover diversidad (MMR simplificado)
        val initialSorted = ranked.sortedByDescending { it.score }.toMutableList()
        val finalRanked = mutableListOf<RankedFeedCandidate>()

        var lastAuthorId: String? = null

        while (initialSorted.isNotEmpty()) {
            val nextBestIndex = initialSorted.indexOfFirst {
                it.candidate.authorId.value != lastAuthorId
            }.takeIf { it != -1 } ?: 0 // Si no hay alternativas, elegimos el mejor restante

            val selected = initialSorted.removeAt(nextBestIndex)
            finalRanked.add(selected)
            lastAuthorId = selected.candidate.authorId.value
        }

        return finalRanked
    }
}
