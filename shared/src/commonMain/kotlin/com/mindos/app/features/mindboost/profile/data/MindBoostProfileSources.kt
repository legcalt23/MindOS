package com.mindos.app.features.mindboost.profile.data

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.decisiones.domain.repository.ReversibilityResultStore
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileReadOnly
import com.mindos.app.features.mindboost.focus.domain.repository.FocusProfileRepository
import com.mindos.app.features.mindboost.focus.domain.repository.SessionHistoryRepository
import com.mindos.app.features.mindboost.language.domain.repository.EvidenceRepository
import com.mindos.app.features.mindboost.language.domain.repository.SessionSummaryRepository
import com.mindos.app.features.mindboost.logic.domain.repository.LogicTrainingRepository
import com.mindos.app.features.mindboost.memory.domain.repository.DeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.PairsResultStore
import com.mindos.app.features.mindboost.memory.domain.repository.SpanResultStore
import com.mindos.app.features.mindboost.profile.data.adapter.DecisionsProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.EmotionProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.EpisodicProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.FocusProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.LanguageProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.LogicProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.MemoryProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.PairsProfileSource
import com.mindos.app.features.mindboost.profile.data.adapter.SpeedProfileSource
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore

/**
 * Assembles the real module → profile sources from each module's EXISTING public read surfaces.
 *
 * Only modules with a genuinely honest per-user signal are wired: ENFOQUE, VELOCIDAD, LÓGICA, LENGUAJE,
 * EMOCIONES (the last contributes data-sufficiency/trend with a null score by design), and the Memoria
 * feature — which feeds TWO distinct profile dimensions through ProfileUpdateGate. The EX1 complex-span
 * score ([spanStore]) and the EX4 associative score ([pairsStore]) carry moduleId MEMORIA → the aggregator
 * BLENDS them into WORKING_MEMORY. The EX3 24h delayed-recall score ([deferredStore]) carries moduleId
 * MEMORIA_EPISODICA → its OWN EPISODIC_MEMORY dimension (the episodic taxonomy split: 24h retention is not
 * working memory, so it is NOT folded under WORKING_MEMORY). Each is CALIBRATING until ≥20 valid trials,
 * then EMERGING/preliminary. DECISIONES is fed by the E2 reversibility discrimination score
 * ([reversibilityStore]) → DECISION_MAKING (CALIBRATING until ≥20 honest cards, then EMERGING/preliminary).
 * See docs/mindboost_profile_2a/ARCHITECTURE.md.
 *
 * Each surface is nullable: a module that is not yet production-wired (e.g. Lenguaje has no persisted
 * store today) is simply skipped, leaving its dimension CALIBRATING. Wiring it later is a one-line change.
 */
@Suppress("LongParameterList") // one read surface per wired module; grouping them would hide the wiring
fun mindBoostProfileSources(
    emotionProfile: EmotionProfileReadOnly?,
    speedStore: SpeedMeasurementStore?,
    focusHistory: SessionHistoryRepository?,
    focusProfiles: FocusProfileRepository?,
    logicRepository: LogicTrainingRepository?,
    languageEvidence: EvidenceRepository?,
    languageSummaries: SessionSummaryRepository?,
    spanStore: SpanResultStore?,
    pairsStore: PairsResultStore?,
    deferredStore: DeferredResultStore?,
    reversibilityStore: ReversibilityResultStore?,
    clock: EpochClock = SystemEpochClock,
): List<CognitiveModuleProfileSource> = buildList {
    if (emotionProfile != null) add(EmotionProfileSource(emotionProfile))
    if (speedStore != null) add(SpeedProfileSource(speedStore))
    if (focusHistory != null && focusProfiles != null) {
        add(FocusProfileSource(focusHistory, focusProfiles, clock = clock))
    }
    if (logicRepository != null) add(LogicProfileSource(logicRepository, clock = clock))
    if (languageEvidence != null && languageSummaries != null) {
        add(LanguageProfileSource(languageEvidence, languageSummaries))
    }
    if (spanStore != null) add(MemoryProfileSource(spanStore))
    if (pairsStore != null) add(PairsProfileSource(pairsStore))
    if (deferredStore != null) add(EpisodicProfileSource(deferredStore))
    if (reversibilityStore != null) add(DecisionsProfileSource(reversibilityStore))
}
