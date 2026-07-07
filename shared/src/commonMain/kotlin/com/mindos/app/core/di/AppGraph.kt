package com.mindos.app.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.dm.data.FirebaseDMRepository
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.profile.card.data.EmptyProfileCardRepository
import com.mindos.app.features.profile.card.data.FirebaseProfileCardRepository
import com.mindos.app.features.profile.card.domain.repository.ProfileCardRepository
import com.mindos.app.features.profile.data.FirebaseMindOSProfileObserverRepository
import com.mindos.app.features.profile.data.FirebaseMindOSProfileRepository
import com.mindos.app.features.profile.data.NoRemoteProfilePresenceRepository
import com.mindos.app.features.profile.domain.repository.MindOSProfileRepository
import com.mindos.app.features.profile.domain.repository.ProfileObserverRepository
import com.mindos.app.features.profile.domain.repository.ProfilePresenceRepository
import com.mindos.app.features.privacy.rememberPrivacyEnabled
import com.mindos.app.features.privacy.application.NoOpUserDataEraser
import com.mindos.app.features.privacy.application.NoOpPendingAccountDeletionStore
import com.mindos.app.features.privacy.application.PendingAccountDeletionStore
import com.mindos.app.features.privacy.application.SecureNamespaceUserDataEraser
import com.mindos.app.features.privacy.application.SecurePendingAccountDeletionStore
import com.mindos.app.features.privacy.application.UserDataEraser
import com.mindos.app.features.social.data.feed.rememberGlobalFeedRepository
import com.mindos.app.features.social.data.repository.FirebaseProfileVisualRepository
import com.mindos.app.features.social.data.repository.FirebaseSocialInteractionRepository
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository
import com.mindos.app.features.social.domain.repository.ObservationRepository
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.shared.util.AppClock
import com.mindos.app.features.wallet.data.FirebaseWalletApiClient
import com.mindos.app.features.wallet.data.WalletApiClient
import com.mindos.app.features.waves.data.DisabledWaveRequestRepository
import com.mindos.app.features.waves.domain.repository.WaveRequestRepository
import com.mindos.app.features.waves.domain.service.WaveRequestService

/**
 * Aggregated dependency graph for the MindOS Compose tree.
 *
 * Bundles the long-lived collaborators that `MindOSApp` and its child screens
 * need. Centralizing them here lets tests swap a single object instead of
 * juggling five+ independent parameters, and keeps `MindOSApp`'s signature
 * stable as new dependencies arrive.
 *
 * The graph holds repository / client *interfaces* (not concrete classes), so
 * tests can substitute fakes without touching Firebase. The privacy toggle is
 * a `MutableState<Boolean>` so the same source of truth survives across
 * recompositions and feeds both the overlay and the settings screen.
 *
 * Lifecycle: an `AppGraph` instance is created once per `MindOSApp` mount
 * (see [rememberAppGraph]) and discarded when the host composable leaves
 * composition. None of the contained dependencies expose mutable state that
 * outlives the graph itself; each is either a plain repository (stateless)
 * or a `remember{}`-cached state holder.
 */
@Stable
data class AppGraph(
    val dmRepository: DMRepository,
    val walletClient: WalletApiClient,
    val waveRequestRepository: WaveRequestRepository,
    val globalFeedRepository: GlobalFeedRepository,
    val appClock: AppClock,
    val mindOSProfileRepository: MindOSProfileRepository,
    val mindOSProfileObserverRepository: ProfileObserverRepository,
    val mindOSProfilePresenceRepository: ProfilePresenceRepository,
    val privacyEnabledState: MutableState<Boolean>,
    val userDataEraser: UserDataEraser = NoOpUserDataEraser,
    val pendingAccountDeletionStore: PendingAccountDeletionStore = NoOpPendingAccountDeletionStore,
    // Stateless domain service wrapping [waveRequestRepository]. Lives in the graph
    // so screens (DM list, cajonera, chat) and the DM state holder all share one
    // instance instead of each call site constructing its own — App.kt no longer
    // wires `WaveRequestService(...)` inline. The DM state holder remains
    // composition-scoped (see rememberDMStateHolder) because it owns a
    // CoroutineScope tied to the UI lifecycle; it is fed entirely from this graph.
    val waveRequestService: WaveRequestService,
    // Profile-card repo, used ONLY to resolve the current user's favorite accent
    // color for `LocalFavoriteAccent`. Defaults to a stable no-op object so the
    // graph keeps a stable identity for equality (a per-instance default would
    // break AppGraphTest) and tests need no change.
    val profileCardRepository: ProfileCardRepository = EmptyProfileCardRepository,
)

/**
 * Production wiring of [AppGraph]. Defaults to live Firebase repositories.
 * Waves use an explicit disabled repository until a backend-authoritative
 * implementation lands; local in-memory state must never grant production chat
 * authorization.
 *
 * Each dependency is wrapped in a separate `remember {}` so it is recreated
 * only when its key changes (which for stateless production dependencies
 * means: never within a single mount).
 *
 * Tests typically bypass this composable entirely and construct an
 * [AppGraph] directly with fakes.
 */
@Composable
@Suppress("LongMethod")
fun rememberAppGraph(): AppGraph {
    val dmRepository: DMRepository = remember { FirebaseDMRepository() }
    val walletClient: WalletApiClient = remember { FirebaseWalletApiClient() }
    val profileVisualRepository = remember { FirebaseProfileVisualRepository() }
    val observationRepository: ObservationRepository = remember { FirebaseSocialInteractionRepository() }
    val waveRequestRepository: WaveRequestRepository =
        remember { createProductionWaveRequestRepository() }
    val globalFeedRepository = rememberGlobalFeedRepository()
    val appClock = remember { AppClock { Timestamp(SystemEpochClock.nowMillis()) } }
    val mindOSProfileRepository = remember(profileVisualRepository, appClock) {
        FirebaseMindOSProfileRepository(
            visualRepository = profileVisualRepository,
            clock = appClock,
        )
    }
    val mindOSProfileObserverRepository = remember(observationRepository) {
        FirebaseMindOSProfileObserverRepository(observationRepository)
    }
    val mindOSProfilePresenceRepository = remember { NoRemoteProfilePresenceRepository() }
    val profileCardRepository: ProfileCardRepository = remember { FirebaseProfileCardRepository() }
    val privacyEnabledState = rememberPrivacyEnabled()
    val focusStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Focus)
    val memoryStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Memory)
    val speedStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Speed)
    val logicStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Logic)
    val creativityStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Creativity)
    val emotionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Emotions)
    val decisionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Decisiones)
    val languageStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Language)
    val profileStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Profile)
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val syncQueueStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.SyncQueue)
    val coachStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Coach)
    val deletionJournalStore = rememberSecureKeyValueStore("account_deletion_journal")
    // The eraser store list is DERIVED from MindBoostStorageNamespaces.AccountBound via
    // accountBoundEraserStores (Compose remember rules forbid mapping rememberSecureKeyValueStore in a loop,
    // so the stores are created individually and looked up by namespace). If the map below omits an
    // account-bound namespace, graph construction fails fast — a namespace can never be silently un-erased.
    val userDataEraser = remember(
        focusStore, memoryStore, speedStore, logicStore,
        creativityStore, emotionsStore, decisionsStore, languageStore, profileStore, sessionsStore,
        syncQueueStore, coachStore,
    ) {
        SecureNamespaceUserDataEraser(
            accountBoundEraserStores(
                mapOf(
                    MindBoostStorageNamespaces.Focus to focusStore,
                    MindBoostStorageNamespaces.Memory to memoryStore,
                    MindBoostStorageNamespaces.Speed to speedStore,
                    MindBoostStorageNamespaces.Logic to logicStore,
                    MindBoostStorageNamespaces.Creativity to creativityStore,
                    MindBoostStorageNamespaces.Emotions to emotionsStore,
                    MindBoostStorageNamespaces.Decisiones to decisionsStore,
                    MindBoostStorageNamespaces.Language to languageStore,
                    MindBoostStorageNamespaces.Profile to profileStore,
                    MindBoostStorageNamespaces.Sessions to sessionsStore,
                    MindBoostStorageNamespaces.SyncQueue to syncQueueStore,
                    MindBoostStorageNamespaces.Coach to coachStore,
                ),
            ),
        )
    }
    val pendingAccountDeletionStore = remember(deletionJournalStore) {
        SecurePendingAccountDeletionStore(deletionJournalStore)
    }
    val waveRequestService = remember(waveRequestRepository) {
        WaveRequestService(waveRequestRepository)
    }
    return remember(
        dmRepository,
        walletClient,
        waveRequestRepository,
        globalFeedRepository,
        appClock,
        mindOSProfileRepository,
        mindOSProfileObserverRepository,
        mindOSProfilePresenceRepository,
        privacyEnabledState,
        userDataEraser,
        pendingAccountDeletionStore,
        waveRequestService,
        profileCardRepository,
    ) {
        AppGraph(
            dmRepository = dmRepository,
            walletClient = walletClient,
            waveRequestRepository = waveRequestRepository,
            globalFeedRepository = globalFeedRepository,
            appClock = appClock,
            mindOSProfileRepository = mindOSProfileRepository,
            mindOSProfileObserverRepository = mindOSProfileObserverRepository,
            mindOSProfilePresenceRepository = mindOSProfilePresenceRepository,
            privacyEnabledState = privacyEnabledState,
            userDataEraser = userDataEraser,
            pendingAccountDeletionStore = pendingAccountDeletionStore,
            waveRequestService = waveRequestService,
            profileCardRepository = profileCardRepository,
        )
    }
}

/**
 * Ordered eraser stores for EVERY namespace in [MindBoostStorageNamespaces.AccountBound], derived from that
 * canonical set so an account-bound namespace can never be silently omitted from account deletion. Fails fast
 * (at graph construction) if [storesByNamespace] does not provide a store for an account-bound namespace.
 * Pure and testable — [com.mindos.app.features.mindboost.storage.AccountBoundErasureCoverageTest] exercises it.
 */
internal fun accountBoundEraserStores(
    storesByNamespace: Map<String, SecureKeyValueStore>,
): List<SecureKeyValueStore> = MindBoostStorageNamespaces.AccountBound.map { namespace ->
    requireNotNull(storesByNamespace[namespace]) {
        "AccountBound namespace '$namespace' has no store wired into the eraser."
    }
}

internal fun createProductionWaveRequestRepository(): WaveRequestRepository =
    DisabledWaveRequestRepository()
