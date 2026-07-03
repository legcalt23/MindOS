@file:Suppress("ConstructorParameterNaming", "VariableNaming")

package com.mindverse.mindboost.core.domain

import kotlinx.serialization.Serializable

private const val HASH_MULTIPLIER = 31

@Serializable
data class DecisionSession(
    val sessionId: String,
    val timestampMs: Long,
    val icd: Double,
    val ied: Double,
    val cde: Double,
    val fatigueScore: Double,
    val gamingScore: Double,
)

@Serializable
data class DecisionMetrics(
    val objectivesAlignment: Double,
    val bayesianUpdateRatio: Double,
    val biasDetected: Boolean,
    val alternativesScannedRatio: Double,
    val reactionTimeMs: Long,
    val timeLimitMs: Long,
)

@Serializable
data class DifficultyVector(
    val numberOfVariables: Int,
    val numberOfAlternatives: Int,
    val timePressure: Double,
    val noiseLevel: Double,
    val ambiguityLevel: Double,
    val irreversibility: Double,
    val informationCost: Double,
    val evidenceContradiction: Double,
    val memoryLoad: Int,
    val justificationDemand: Double,
)

@Serializable
data class CognitiveReadinessState(
    val goNoGoAccuracy: Double,
    val reactionTimeCV: Double,
    val subjectiveFatigue: Double,
    val criValue: Double,
)

@Serializable
data class PredictionEvent(
    val predictionId: String,
    val timestampMs: Long,
    val predictedProbability: Double,
    val encryptedTextPayload: ByteArray,
    val predictionHash: String,
    val isVerified: Boolean,
    val outcomeValue: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PredictionEvent) return false

        return predictionId == other.predictionId &&
            timestampMs == other.timestampMs &&
            predictedProbability == other.predictedProbability &&
            encryptedTextPayload.contentEquals(other.encryptedTextPayload) &&
            predictionHash == other.predictionHash &&
            isVerified == other.isVerified &&
            outcomeValue == other.outcomeValue
    }

    override fun hashCode(): Int {
        var result = predictionId.hashCode()
        result = HASH_MULTIPLIER * result + timestampMs.hashCode()
        result = HASH_MULTIPLIER * result + predictedProbability.hashCode()
        result = HASH_MULTIPLIER * result + encryptedTextPayload.contentHashCode()
        result = HASH_MULTIPLIER * result + predictionHash.hashCode()
        result = HASH_MULTIPLIER * result + isVerified.hashCode()
        result = HASH_MULTIPLIER * result + (outcomeValue ?: 0)
        return result
    }
}

@Serializable
data class BrierPrediction(
    val forecast: Double,
    val outcome: Double,
)

@Serializable
data class BayesScenario(
    val baseRate: Double,
    val sensitivity: Double,
    val falsePositiveRate: Double,
    val expectedPosterior: Double,
)

@Serializable
data class SensorEvidence(
    val sensorId: String,
    val reportedValue: Boolean,
    val reliability: Double,
)

@Serializable
data class ReversibilityScenario(
    val id: String,
    val description: String,
    val isReversible: Boolean,
    val delayCostRate: Double,
    val lockedDurationMs: Long,
)

@Serializable
data class DiaryEntry(
    val id: String,
    val timestampMs: Long,
    val encryptedContent: ByteArray,
    val safetyFlag: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiaryEntry) return false

        return id == other.id &&
            timestampMs == other.timestampMs &&
            encryptedContent.contentEquals(other.encryptedContent) &&
            safetyFlag == other.safetyFlag
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = HASH_MULTIPLIER * result + timestampMs.hashCode()
        result = HASH_MULTIPLIER * result + encryptedContent.contentHashCode()
        result = HASH_MULTIPLIER * result + safetyFlag.hashCode()
        return result
    }
}

@Serializable
data class CoachTemplate(
    val id: String,
    val targetProfile: String,
    val messageText: String,
)

@Serializable
data class UserSkillProfile(
    val phd: Double,
    val CE: Double,
    val CAG: Double,
    val CD: Double,
    val RRS: Double,
)
