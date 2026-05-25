package com.example.neuv3.features.social.data.publishing

import com.example.neuv3.features.social.data.local.InMemorySocialGraphDatabase
import com.example.neuv3.features.social.domain.publishing.CandidateGenerationService
import com.example.neuv3.features.social.domain.publishing.CandidateInjectionPipeline
import com.example.neuv3.features.social.domain.publishing.DefaultFeedFeatureEnrichmentPlane
import com.example.neuv3.features.social.domain.publishing.DefaultFeedRankingPlane
import com.example.neuv3.features.social.domain.publishing.DeletePostCommandHandler
import com.example.neuv3.features.social.domain.publishing.DeletePostUseCase
import com.example.neuv3.features.social.domain.publishing.EditPostCommandHandler
import com.example.neuv3.features.social.domain.publishing.EditPostUseCase
import com.example.neuv3.features.social.domain.publishing.FanoutDistributionPolicy
import com.example.neuv3.features.social.domain.publishing.FanoutMigrationService
import com.example.neuv3.features.social.domain.publishing.FeedEligibilityService
import com.example.neuv3.features.social.domain.publishing.FeedProjectionPipeline
import com.example.neuv3.features.social.domain.publishing.GetFeedUseCase
import com.example.neuv3.features.social.domain.publishing.IdempotencyKey
import com.example.neuv3.features.social.domain.publishing.ModerationPipeline
import com.example.neuv3.features.social.domain.publishing.OutboxDispatcher
import com.example.neuv3.features.social.domain.publishing.PublicationReconciliationService
import com.example.neuv3.features.social.domain.publishing.PublishPostCommandHandler
import com.example.neuv3.features.social.domain.publishing.PublishPostUseCase
import com.example.neuv3.features.social.domain.publishing.PublishingCommandBus
import com.example.neuv3.features.social.domain.publishing.PublishingDependencies
import com.example.neuv3.features.social.domain.publishing.RandomIdempotencyKeyGenerator
import com.example.neuv3.features.social.domain.publishing.RandomTraceContextFactory
import com.example.neuv3.features.social.domain.publishing.SequentialOutboxMessageIdGenerator
import com.example.neuv3.features.social.domain.publishing.SequentialPublishingEventIdGenerator
import com.example.neuv3.features.social.domain.publishing.StablePostIdGenerator
import com.example.neuv3.features.social.domain.publishing.TraceContext
import com.example.neuv3.features.social.domain.usecase.SocialGraphDependencies
import com.example.neuv3.features.social.shared.util.AppClock

class InMemoryPublishingComponent private constructor(
    val database: InMemoryPublishingDatabase,
    val dependencies: PublishingDependencies,
    val commandBus: PublishingCommandBus,
    val feedQuery: GetFeedUseCase,
    val reconciliation: PublicationReconciliationService,
    val fanoutMigration: FanoutMigrationService,
    val metrics: InMemoryPublishingMetricsRecorder,
    val traces: InMemoryPublishingTraceSink,
) {
    fun nextIdempotencyKey(): IdempotencyKey =
        dependencies.idempotencyKeys.nextIdempotencyKey()

    fun nextTraceContext(operation: String): TraceContext =
        dependencies.traceContexts.nextTraceContext(operation)

    companion object {
        fun create(
            socialGraphDatabase: InMemorySocialGraphDatabase,
            socialGraphDependencies: SocialGraphDependencies,
            database: InMemoryPublishingDatabase = InMemoryPublishingDatabase(),
            clock: AppClock = socialGraphDependencies.clock,
        ): InMemoryPublishingComponent {
            val posts = InMemoryCanonicalPostRepository(database)
            val idempotency = InMemoryCommandIdempotencyRepository(database)
            val outbox = InMemoryOutboxRepository(database)
            val feedInbox = InMemoryFeedCandidateInboxRepository(database)
            val feedPosts = InMemoryFeedPostSnapshotRepository(database)
            val authorFeedIndex = InMemoryAuthorFeedIndexRepository(database)
            val fanoutConfig = InMemoryFanoutPolicyConfigRepository(database)
            val fanoutDecisions = InMemoryFanoutDistributionDecisionRepository(database)
            val fanoutPostMetrics = InMemoryFanoutPostMetricsRepository(database)
            val fanoutReports = InMemoryFanoutDeliveryReportRepository(database)
            val feedMutes = InMemoryFeedMuteRepository(database)
            val moderation = InMemoryFeedModerationRepository(database)
            val rankedFeedCache = InMemoryRankedFeedCacheRepository(database)
            val authorScale = InMemoryAuthorScaleRepository(database)
            val metrics = InMemoryPublishingMetricsRecorder(database, clock)
            val traces = InMemoryPublishingTraceSink(database, clock)
            val eventJournal = InMemoryPublishingEventJournalRepository(database, clock)
            val eventBackbone = InMemoryPublishingEventBackbone(database, eventJournal, metrics, clock)
            val audience = InMemorySocialAudienceRepository(socialGraphDatabase)

            val dependencies = PublishingDependencies(
                transactions = database,
                posts = posts,
                idempotency = idempotency,
                outbox = outbox,
                feedInbox = feedInbox,
                feedPosts = feedPosts,
                authorFeedIndex = authorFeedIndex,
                fanoutConfig = fanoutConfig,
                fanoutDecisions = fanoutDecisions,
                fanoutPostMetrics = fanoutPostMetrics,
                fanoutReports = fanoutReports,
                feedMutes = feedMutes,
                moderation = moderation,
                rankedFeedCache = rankedFeedCache,
                eventBackbone = eventBackbone,
                authorScale = authorScale,
                audience = audience,
                profiles = socialGraphDependencies.profiles,
                privacy = socialGraphDependencies.privacy,
                follows = socialGraphDependencies.follows,
                blocks = socialGraphDependencies.blocks,
                clock = clock,
                postIds = StablePostIdGenerator(),
                eventIds = SequentialPublishingEventIdGenerator(),
                outboxIds = SequentialOutboxMessageIdGenerator(),
                idempotencyKeys = RandomIdempotencyKeyGenerator(),
                traceContexts = RandomTraceContextFactory(),
                metrics = metrics,
                traceSink = traces,
            )

            val eligibility = FeedEligibilityService(
                profiles = socialGraphDependencies.profiles,
                privacy = socialGraphDependencies.privacy,
                follows = socialGraphDependencies.follows,
                blocks = socialGraphDependencies.blocks,
                mutes = feedMutes,
                moderation = moderation,
            )

            val fanoutPolicy = FanoutDistributionPolicy(dependencies)
            val candidateGeneration = CandidateGenerationService(dependencies, eligibility)
            val candidateInjection = CandidateInjectionPipeline(dependencies, candidateGeneration, fanoutPolicy)
            val featureEnrichment = DefaultFeedFeatureEnrichmentPlane(dependencies)

            val rankingPlane = DefaultFeedRankingPlane(
                clock = clock,
                metrics = metrics,
                rankingEngine = socialGraphDependencies.rankingEngine,
            )

            val dispatcher = OutboxDispatcher(dependencies)

            eventBackbone.register(FeedProjectionPipeline(dependencies, candidateInjection))
            eventBackbone.register(ModerationPipeline(dependencies))

            val publishPost = PublishPostUseCase(
                handler = PublishPostCommandHandler(dependencies),
            )
            val editPost = EditPostUseCase(
                handler = EditPostCommandHandler(dependencies),
            )
            val deletePost = DeletePostUseCase(
                handler = DeletePostCommandHandler(dependencies),
            )

            return InMemoryPublishingComponent(
                database = database,
                dependencies = dependencies,
                commandBus = PublishingCommandBus(publishPost, editPost, deletePost),
                feedQuery = GetFeedUseCase(
                    dependencies = dependencies,
                    eligibility = eligibility,
                    candidateInjection = candidateInjection,
                    featureEnrichment = featureEnrichment,
                    rankingPlane = rankingPlane,
                ),
                reconciliation = PublicationReconciliationService(dependencies, dispatcher),
                fanoutMigration = FanoutMigrationService(
                    dependencies = dependencies,
                    fanoutPolicy = fanoutPolicy,
                    candidateInjection = candidateInjection,
                ),
                metrics = metrics,
                traces = traces,
            )
        }
    }
}