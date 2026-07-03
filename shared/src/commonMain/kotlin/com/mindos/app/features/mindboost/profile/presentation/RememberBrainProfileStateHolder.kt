package com.mindos.app.features.mindboost.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.mindboost.decisiones.data.LocalReversibilityResultStore
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.local.LocalSessionHistoryRepository
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.logic.data.local.LocalLogicTrainingRepository
import com.mindos.app.features.mindboost.memory.data.local.LocalDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.local.LocalPairsResultStore
import com.mindos.app.features.mindboost.memory.data.local.LocalSpanResultStore
import com.mindos.app.features.mindboost.profile.data.local.LocalMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.data.mindBoostProfileSources
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.speed.data.persistence.SecureSpeedMeasurementStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces

/**
 * Production wiring for the brain-profile screen. Each module's coarse signal is read from the SAME
 * encrypted namespace the module itself writes to (so the profile reflects real training data), folded
 * via [mindBoostProfileSources], and the snapshot is persisted to its own encrypted namespace.
 *
 * `LocalEmotionRepository` implements `EmotionProfileReadOnly` directly (no cast). The Memoria feature is
 * wired from THREE stores in the `mindboost_memory` namespace and feeds TWO distinct profile dimensions:
 * the EX1 complex-span store and the EX4 associative pairs store carry MEMORIA → WORKING_MEMORY, while the
 * EX3 24h delayed-recall store carries MEMORIA_EPISODICA → its own EPISODIC_MEMORY dimension (the episodic
 * taxonomy split: 24h retention is not working memory). Each is CALIBRATING until ≥20 valid trials, then
 * EMERGING/preliminary. DECISIONES is wired from the E2 reversibility store in the `mindboost_decisiones`
 * namespace → DECISION_MAKING (CALIBRATING until ≥20 honest cards). LENGUAJE has no production store yet,
 * so it is passed as null and VERBAL stays CALIBRATING until it persists data.
 */
@Composable
fun rememberMindBoostProfileService(userId: String): MindBoostProfileService {
    val focusStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Focus)
    val speedStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Speed)
    val logicStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Logic)
    val emotionStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Emotions)
    val memoryStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Memory)
    val decisionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Decisiones)
    val profileStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Profile)
    return remember(
        focusStore, speedStore, logicStore, emotionStore, memoryStore, decisionsStore, profileStore, userId,
    ) {
        val sources = mindBoostProfileSources(
            emotionProfile = LocalEmotionRepository(emotionStore),
            speedStore = SecureSpeedMeasurementStore(speedStore),
            focusHistory = LocalSessionHistoryRepository(focusStore),
            focusProfiles = LocalFocusProfileRepository(focusStore),
            logicRepository = LocalLogicTrainingRepository(logicStore),
            languageEvidence = null, // Lenguaje has no production store yet → VERBAL stays calibrating
            languageSummaries = null,
            spanStore = LocalSpanResultStore(memoryStore),
            pairsStore = LocalPairsResultStore(memoryStore),
            deferredStore = LocalDeferredResultStore(memoryStore),
            reversibilityStore = LocalReversibilityResultStore(decisionsStore),
        )
        MindBoostProfileService(sources, LocalMindBoostProfileRepository(profileStore))
    }
}

/** Brain-profile screen holder over the shared [rememberMindBoostProfileService]. */
@Composable
fun rememberMindBoostBrainProfileStateHolder(userId: String): MindBoostBrainProfileStateHolder {
    val service = rememberMindBoostProfileService(userId)
    val scope = rememberCoroutineScope()
    return remember(service, userId, scope) {
        MindBoostBrainProfileStateHolder(service = service, userId = userId, scope = scope)
    }
}
