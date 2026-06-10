package com.mindos.app.features.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mindos.app.features.profile.domain.repository.MindOSProfileRepository
import com.mindos.app.features.profile.domain.repository.ProfileObserverRepository
import com.mindos.app.features.profile.domain.repository.ProfilePresenceRepository

/**
 * Composition-scoped factory for [MindOSProfileStateHolder].
 *
 * Repositories are required on purpose: production must pass the AppGraph's
 * Firebase-backed adapters, while tests/previews can pass explicit fakes.
 */
@Composable
fun rememberMindOSProfileStateHolder(
    profileRepository: MindOSProfileRepository,
    observerRepository: ProfileObserverRepository,
    presenceRepository: ProfilePresenceRepository,
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
