package com.example.mindos.features.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mindos.features.profile.data.InMemoryMindOSProfileRepository
import com.example.mindos.features.profile.data.InMemoryProfileObserverRepository
import com.example.mindos.features.profile.data.InMemoryProfilePresenceRepository
import com.example.mindos.features.profile.domain.repository.MindOSProfileRepository
import com.example.mindos.features.profile.domain.repository.ProfileObserverRepository
import com.example.mindos.features.profile.domain.repository.ProfilePresenceRepository

/**
 * Composition-scoped factory for [MindOSProfileStateHolder].
 *
 * Defaults to the in-memory repositories (session-lived; ready to be swapped for
 * Firebase-backed implementations). The repositories are remembered so the
 * profile's edits and presence survive recompositions and tab switches while the
 * screen is mounted.
 */
@Composable
fun rememberMindOSProfileStateHolder(
    profileRepository: MindOSProfileRepository = remember { InMemoryMindOSProfileRepository() },
    observerRepository: ProfileObserverRepository = remember { InMemoryProfileObserverRepository() },
    presenceRepository: ProfilePresenceRepository = remember { InMemoryProfilePresenceRepository() },
): MindOSProfileStateHolder {
    val scope = rememberCoroutineScope()
    return remember(profileRepository, observerRepository, presenceRepository, scope) {
        MindOSProfileStateHolder(
            profileRepository = profileRepository,
            observerRepository = observerRepository,
            presenceRepository = presenceRepository,
            scope = scope,
        )
    }
}
