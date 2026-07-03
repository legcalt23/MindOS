package com.mindos.app.features.mindboost.core.scoring

import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityScore
import com.mindos.app.features.mindboost.language.domain.evaluator.LocalStubEvaluatorClient
import com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryDeferredScore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryPairsScore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemorySpanScore
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fase 1.2 — the scoring version registry is the single source of truth. Two NON-FRAGILE guards (value
 * parity only; no source-text scanning, so comments / fixtures / golden strings are never false positives):
 *  1. the registry pins the EXACT existing strings — provenance-preserving; changing any value is a
 *     deliberate change that must update this test (and reason about persisted provenance/migration);
 *  2. every block's local version constant reads FROM the registry, so a scattered literal that drifts
 *     from the registry breaks the build. (A brand-new scorer that introduces its own literal is caught by
 *     review + the registry convention, deliberately not by an aggressive text scanner — see the inventory.)
 */
class ScoringVersionRegistryCoverageTest {

    @Test
    fun registry_pinsExactExistingStrings() {
        assertEquals("user-display-score-mapper-1.0.0-linear-preliminary", ScoringVersionRegistry.USER_DISPLAY_SCORE_MAPPER)
        assertEquals("e2-reversibility-v1", ScoringVersionRegistry.DECISIONS_REVERSIBILITY)
        assertEquals("ex1-span-v1", ScoringVersionRegistry.MEMORY_SPAN)
        assertEquals("ex4-pairs-v1", ScoringVersionRegistry.MEMORY_PAIRS)
        assertEquals("ex3-deferred-v1", ScoringVersionRegistry.MEMORY_DEFERRED)
        assertEquals("1.0.0", ScoringVersionRegistry.SPEED_METRICS)
        assertEquals("1.0.0-mvp1", ScoringVersionRegistry.SPEED_STAIRCASE)
        assertEquals("1.0.0", ScoringVersionRegistry.SPEED_DIFFICULTY_MODEL)
        assertEquals("1.0.0-mvp3-preliminary", ScoringVersionRegistry.SPEED_SCORE)
        assertEquals("local-stub-1", ScoringVersionRegistry.LANGUAGE_EVALUATOR_STUB)
        assertEquals("local-stub-rubric-1", ScoringVersionRegistry.LANGUAGE_RUBRIC_STUB)
        assertEquals("v6-projector-1", ScoringVersionRegistry.LANGUAGE_PROJECTION_ESTIMATOR)
    }

    @Test
    fun everyBlockVersion_readsFromTheRegistry_noScatteredLiteral() {
        assertEquals(ScoringVersionRegistry.DECISIONS_REVERSIBILITY, ReversibilityScore.SCORING_VERSION)
        assertEquals(ScoringVersionRegistry.MEMORY_SPAN, MemorySpanScore.SCORING_VERSION)
        assertEquals(ScoringVersionRegistry.MEMORY_PAIRS, MemoryPairsScore.SCORING_VERSION)
        assertEquals(ScoringVersionRegistry.MEMORY_DEFERRED, MemoryDeferredScore.SCORING_VERSION)
        assertEquals(ScoringVersionRegistry.SPEED_METRICS, SpeedVersions.METRICS_VERSION)
        assertEquals(ScoringVersionRegistry.SPEED_STAIRCASE, SpeedVersions.STAIRCASE_VERSION)
        assertEquals(ScoringVersionRegistry.SPEED_DIFFICULTY_MODEL, SpeedVersions.DIFFICULTY_MODEL_VERSION)
        assertEquals(ScoringVersionRegistry.SPEED_SCORE, SpeedVersions.SCORE_VERSION)
        assertEquals(ScoringVersionRegistry.LANGUAGE_EVALUATOR_STUB, LocalStubEvaluatorClient.STUB_VERSION)
        assertEquals(ScoringVersionRegistry.LANGUAGE_RUBRIC_STUB, LocalStubEvaluatorClient.RUBRIC_VERSION)
        assertEquals(ScoringVersionRegistry.LANGUAGE_PROJECTION_ESTIMATOR, ProfileProjector.ESTIMATOR_VERSION)
    }
}
