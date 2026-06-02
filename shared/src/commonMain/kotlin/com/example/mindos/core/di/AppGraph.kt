package com.example.mindos.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.example.mindos.core.time.SystemEpochClock
import com.example.mindos.features.dm.data.FirebaseDMRepository
import com.example.mindos.features.dm.domain.repository.DMRepository
import com.example.mindos.features.profile.data.FirebaseMindOSProfileObserverRepository
import com.example.mindos.features.profile.data.FirebaseMindOSProfileRepository
import com.example.mindos.features.profile.data.NoRemoteProfilePresenceRepository
import com.example.mindos.features.profile.domain.repository.MindOSProfileRepository
import com.example.mindos.features.profile.domain.repository.ProfileObserverRepository
import com.example.mindos.features.profile.domain.repository.ProfilePresenceRepository
import com.example.mindos.features.privacy.rememberPrivacyEnabled
import com.example.mindos.features.social.data.feed.rememberGlobalFeedRepository
import com.example.mindos.features.social.data.repository.FirebaseProfileVisualRepository
import com.example.mindos.features.social.data.repository.FirebaseSocialInteractionRepository
import com.example.mindos.features.social.domain.feed.GlobalFeedRepository
import com.example.mindos.features.social.domain.repository.ObservationRepository
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.wallet.data.FirebaseWalletApiClient
import com.example.mindos.features.wallet.data.WalletApiClient
import com.example.mindos.features.waves.data.InMemoryWaveRequestRepository
import com.example.mindos.features.waves.domain.repository.WaveRequestRepository
import com.example.mindos.features.waves.domain.service.WaveRequestService

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
    // Stateless domain service wrapping [waveRequestRepository]. Lives in the graph
    // so screens (DM list, cajonera, chat) and the DM state holder all share one
    // instance instead of each call site constructing its own — App.kt no longer
    // wires `WaveRequestService(...)` inline. The DM state holder remains
    // composition-scoped (see rememberDMStateHolder) because it owns a
    // CoroutineScope tied to the UI lifecycle; it is fed entirely from this graph.
    val waveRequestService: WaveRequestService,
)

/**
 * Production wiring of [AppGraph]. Defaults to live Firebase repositories +
 * the in-memory wave repository (waves are ephemeral by design today; see
 * Wave 3 tech-debt note in CLAUDE.md).
 *
 * Each dependency is wrapped in a separate `remember {}` so it is recreated
 * only when its key changes (which for stateless production dependencies
 * means: never within a single mount).
 *
 * Tests typically bypass this composable entirely and construct an
 * [AppGraph] directly with fakes.
 */
@Composable
fun rememberAppGraph(): AppGraph {
    val dmRepository: DMRepository = remember { FirebaseDMRepository() }
    val walletClient: WalletApiClient = remember { FirebaseWalletApiClient() }
    val profileVisualRepository = remember { FirebaseProfileVisualRepository() }
    val observationRepository: ObservationRepository = remember { FirebaseSocialInteractionRepository() }
    val waveRequestRepository: WaveRequestRepository =
        remember { InMemoryWaveRequestRepository() }
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
    val privacyEnabledState = rememberPrivacyEnabled()
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
        waveRequestService,
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
            waveRequestService = waveRequestService,
        )
    }
}
