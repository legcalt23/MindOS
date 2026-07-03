package com.mindos.app.features.mindboost.core.scoring

import kotlinx.serialization.Serializable

/** Current schema version for [ScoreResultDto] (Json ignoreUnknownKeys=true on read, per repo convention). */
const val SCORE_RESULT_DTO_SCHEMA_VERSION: Int = 1

/**
 * INFRA-01 · Safe serialization surface for a verdict summary. Serialization rule (invariant `null != 0`):
 * a non-VALID state ALWAYS serializes `primaryValue = null`, NEVER 0.0. Only summaries are persisted,
 * never raw trials. Enforced by the canary test.
 */
@Serializable
data class ScoreResultDto(
    val schemaVersion: Int,
    val state: String,
    val primaryValue: Double?,
    val nEffective: Int,
    val minN: Int,
    val updatesProfile: Boolean,
    val preliminary: Boolean,
)

/** Maps a verdict to its DTO. A non-VALID state always yields `primaryValue = null`. */
fun ScoreResult.toDto(): ScoreResultDto = ScoreResultDto(
    schemaVersion = SCORE_RESULT_DTO_SCHEMA_VERSION,
    state = state.name,
    primaryValue = if (state == ScoreState.VALID) primarySignal?.value else null,
    nEffective = sampling.nEffective,
    minN = sampling.minNRequired,
    updatesProfile = updatesProfile,
    preliminary = preliminary,
)
