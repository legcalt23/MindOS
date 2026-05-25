package com.example.neuv3.features.social.data.local

import com.example.neuv3.features.social.data.repository.InMemoryAuditEventRepository
import com.example.neuv3.features.social.data.repository.InMemoryBlockRepository
import com.example.neuv3.features.social.data.repository.InMemoryFollowRepository
import com.example.neuv3.features.social.data.repository.InMemoryFollowRequestRepository
import com.example.neuv3.features.social.data.repository.InMemoryPrivacySettingsRepository
import com.example.neuv3.features.social.data.repository.InMemoryProfileCounterRepository
import com.example.neuv3.features.social.data.repository.InMemoryUserProfileRepository
import com.example.neuv3.features.social.domain.model.AccountStatus
import com.example.neuv3.features.social.domain.model.AuditEventType
import com.example.neuv3.features.social.domain.model.PrivacySettings
import com.example.neuv3.features.social.domain.model.UserAccount
import com.example.neuv3.features.social.domain.model.UserProfile
import com.example.neuv3.features.social.domain.repository.AuditEventRepository
import com.example.neuv3.features.social.domain.repository.BlockRepository
import com.example.neuv3.features.social.domain.repository.FollowRepository
import com.example.neuv3.features.social.domain.repository.FollowRequestRepository
import com.example.neuv3.features.social.domain.repository.PrivacySettingsRepository
import com.example.neuv3.features.social.domain.repository.ProfileCounterRepository
import com.example.neuv3.features.social.domain.repository.UserProfileRepository
import com.example.neuv3.features.social.domain.usecase.SocialGraphDependencies
import com.example.neuv3.features.social.domain.usecase.SocialGraphUseCases
import com.example.neuv3.features.social.domain.usecase.audit
import com.example.neuv3.features.social.domain.valueobject.Bio
import com.example.neuv3.features.social.domain.valueobject.DisplayName
import com.example.neuv3.features.social.domain.valueobject.Handle
import com.example.neuv3.features.social.domain.valueobject.ProfileId
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.AppClock
import com.example.neuv3.features.social.shared.util.AuditEventIdGenerator
import com.example.neuv3.features.social.shared.util.IncrementingAppClock
import com.example.neuv3.features.social.shared.util.RequestIdGenerator
import com.example.neuv3.features.social.shared.util.SequentialAuditEventIdGenerator
import com.example.neuv3.features.social.shared.util.SequentialRequestIdGenerator

class InMemorySocialGraphComponent private constructor(
    val database: InMemorySocialGraphDatabase,
    val profiles: UserProfileRepository,
    val privacy: PrivacySettingsRepository,
    val follows: FollowRepository,
    val requests: FollowRequestRepository,
    val blocks: BlockRepository,
    val counters: ProfileCounterRepository,
    val auditEvents: AuditEventRepository,
    val dependencies: SocialGraphDependencies,
    val useCases: SocialGraphUseCases,
) {
    suspend fun seedUser(
        userId: UserId,
        handle: Handle,
        displayName: DisplayName,
        bio: Bio = Bio.Empty,
        privacySettings: PrivacySettings = PrivacySettings(userId = userId),
        status: AccountStatus = AccountStatus.Active,
    ) {
        database.write {
            val now = dependencies.clock.now()
            profiles.saveAccount(
                UserAccount(
                    id = userId,
                    handle = handle,
                    status = status,
                    createdAt = now,
                ),
            )
            profiles.saveProfile(
                UserProfile(
                    id = ProfileId.fromUserId(userId),
                    userId = userId,
                    handle = handle,
                    displayName = displayName,
                    bio = bio,
                    createdAt = now,
                ),
            )
            privacy.save(privacySettings.copy(updatedAt = now))
            dependencies.audit(
                type = AuditEventType.ProfileCreated,
                actorId = userId,
                subjectId = userId,
            )
        }
    }

    suspend fun seedUserIfMissing(
        userId: UserId,
        handle: Handle,
        displayName: DisplayName,
        bio: Bio = Bio.Empty,
        privacySettings: PrivacySettings = PrivacySettings(userId = userId),
        status: AccountStatus = AccountStatus.Active,
    ) {
        database.write {
            val existingAccount = profiles.getAccount(userId)
            val existingProfile = profiles.getProfile(userId)
            val existingPrivacy = privacy.get(userId)
            if (existingAccount != null && existingProfile != null && existingPrivacy != null) {
                return@write
            }

            val now = dependencies.clock.now()
            if (existingAccount == null) {
                profiles.saveAccount(
                    UserAccount(
                        id = userId,
                        handle = handle,
                        status = status,
                        createdAt = now,
                    ),
                )
            }
            if (existingProfile == null) {
                profiles.saveProfile(
                    UserProfile(
                        id = ProfileId.fromUserId(userId),
                        userId = userId,
                        handle = handle,
                        displayName = displayName,
                        bio = bio,
                        createdAt = now,
                    ),
                )
            }
            if (existingPrivacy == null) {
                privacy.save(privacySettings.copy(updatedAt = now))
            }
            dependencies.audit(
                type = AuditEventType.ProfileCreated,
                actorId = userId,
                subjectId = userId,
            )
        }
    }

    companion object {
        fun create(
            database: InMemorySocialGraphDatabase = InMemorySocialGraphDatabase(),
            clock: AppClock = IncrementingAppClock(),
            requestIdGenerator: RequestIdGenerator = SequentialRequestIdGenerator(),
            auditEventIdGenerator: AuditEventIdGenerator = SequentialAuditEventIdGenerator(),
        ): InMemorySocialGraphComponent {
            val profiles = InMemoryUserProfileRepository(database)
            val privacy = InMemoryPrivacySettingsRepository(database)
            val follows = InMemoryFollowRepository(database)
            val requests = InMemoryFollowRequestRepository(database)
            val blocks = InMemoryBlockRepository(database)
            val counters = InMemoryProfileCounterRepository(database)
            val auditEvents = InMemoryAuditEventRepository(database)
            val dependencies = SocialGraphDependencies(
                profiles = profiles,
                privacy = privacy,
                follows = follows,
                requests = requests,
                blocks = blocks,
                counters = counters,
                auditEvents = auditEvents,
                transactions = database,
                clock = clock,
                requestIds = requestIdGenerator,
                auditIds = auditEventIdGenerator,
            )
            return InMemorySocialGraphComponent(
                database = database,
                profiles = profiles,
                privacy = privacy,
                follows = follows,
                requests = requests,
                blocks = blocks,
                counters = counters,
                auditEvents = auditEvents,
                dependencies = dependencies,
                useCases = SocialGraphUseCases(dependencies),
            )
        }
    }
}
