package com.example.neuv3.features.social.domain.publishing

import com.example.neuv3.features.social.data.local.InMemorySocialGraphComponent
import com.example.neuv3.features.social.data.publishing.InMemoryPublishingEventBackbone
import com.example.neuv3.features.social.data.publishing.InMemoryPublishingComponent
import com.example.neuv3.features.social.presentation.publisher.InMemoryPublisherLocalDraftStore
import com.example.neuv3.features.social.presentation.publisher.PublisherStateHolder
import com.example.neuv3.features.social.domain.valueobject.DisplayName
import com.example.neuv3.features.social.domain.valueobject.Handle
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.PageRequest
import com.example.neuv3.features.social.shared.util.getOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PublishingPipelineTest {
    private val author = UserId("author")
    private val follower = UserId("follower")

    @Test
    fun publishCommandIsIdempotentAndMaterializesFollowerInbox() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        val command = CreatePostCommand(
            actorId = author,
            body = "Primer pulso",
            idempotencyKey = IdempotencyKey("same-command"),
            trace = publishing.nextTraceContext("test_publish"),
        )

        val first = publishing.commandBus.execute(command).getOrThrow()
        val second = publishing.commandBus.execute(command.copy(trace = publishing.nextTraceContext("test_retry"))).getOrThrow()
        publishing.reconciliation.reconcilePending().getOrThrow()
        val post = publishing.dependencies.posts.get(first.postId)
        val feed = publishing.feedQuery.getFeed(follower, PageRequest(limit = 10)).getOrThrow()
        val followerCandidate = publishing.dependencies.feedInbox.listAllCandidates(follower).single { it.postId == first.postId }
        val fanoutReport = assertNotNull(publishing.dependencies.fanoutReports.latest(first.postId))

        assertEquals(first.postId, second.postId)
        assertEquals(PublicationState.Published, post?.state)
        assertEquals(listOf(first.postId), feed.items.map { it.postId })
        assertEquals(CandidateOrigin.FollowGraph, followerCandidate.origin)
        assertEquals(FanoutDistributionMode.FanoutOnWrite, followerCandidate.distributionMode)
        assertEquals(FanoutDistributionMode.FanoutOnWrite, fanoutReport.mode)
        assertTrue(publishing.metrics.snapshot().any { it.name == "outbox.dispatched" && it.tags["postId"] == first.postId.value })
        assertTrue(publishing.traces.snapshot().any { it.event.postId == first.postId.value })
    }

    @Test
    fun publishCommandReturnsAcceptedReceiptWhenFanoutIsDegraded() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        (publishing.dependencies.eventBackbone as InMemoryPublishingEventBackbone).register(
            object : PublishingEventSubscriber {
                override val subscriberId: EventSubscriberId = EventSubscriberId("test-degraded-fanout")

                override suspend fun handle(envelope: EventEnvelope): DomainResult<Unit> =
                    DomainResult.failure(com.example.neuv3.features.social.domain.error.PersistenceFailure("fanout unavailable"))
            },
        )

        val receipt = publishing.commandBus.execute(
            CreatePostCommand(
                actorId = author,
                body = "Pulso aceptado con fanout pendiente",
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_degraded_fanout"),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending()
        val post = publishing.dependencies.posts.get(receipt.postId)

        assertNotNull(post)
        assertEquals(receipt.postId, post.id)
        assertTrue(publishing.metrics.snapshot().any { it.name == "outbox.failed" && it.tags["postId"] == receipt.postId.value })
    }

    @Test
    fun canonicalPostAndOutboxRollbackTogetherWhenOutboxAppendFails() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        var nextOutboxId = 0
        val dependencies = publishing.dependencies.copy(
            outboxIds = OutboxMessageIdGenerator { _, _ ->
                nextOutboxId++
                OutboxMessageId(if (nextOutboxId <= 3) "outbox_$nextOutboxId" else "outbox_2")
            },
        )
        val handler = PublishPostCommandHandler(dependencies)

        val first = handler.handle(
            CreatePostCommand(
                actorId = author,
                body = "Pulso atomico",
                idempotencyKey = IdempotencyKey("atomic-1"),
                trace = publishing.nextTraceContext("test_atomic_1"),
            ),
        ).getOrThrow()
        val failedKey = IdempotencyKey("atomic-2")
        val failedPostId = dependencies.postIds.nextPostId(author, failedKey)
        val failed = handler.handle(
            CreatePostCommand(
                actorId = author,
                body = "Pulso que debe hacer rollback",
                idempotencyKey = failedKey,
                trace = publishing.nextTraceContext("test_atomic_2"),
            ),
        )

        assertEquals(PublicationState.Outboxed, publishing.dependencies.posts.get(first.postId)?.state)
        assertTrue(failed is DomainResult.Failure)
        assertEquals(null, publishing.dependencies.posts.get(failedPostId))
        assertEquals(null, publishing.dependencies.idempotency.get(failedKey))
    }

    @Test
    fun publisherRestoresAutosavedDraftAndPublishesWithPersistedIdempotencyKey() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        val store = InMemoryPublisherLocalDraftStore()
        val firstHolder = publisherHolder(publishing, store, this)

        firstHolder.restore(defaultBody = "Pulso restaurable")
        val persisted = assertNotNull(store.load(author))
        val restoredHolder = publisherHolder(publishing, store, this)

        restoredHolder.restore()
        restoredHolder.publish()
        val published = publishing.dependencies.posts.get(assertNotNull(restoredHolder.uiState.optimisticPosts.firstOrNull()).postId)

        assertEquals(persisted.idempotencyKey.value, published?.idempotencyKey?.value)
        assertEquals(PublisherState.FeedVisible, restoredHolder.uiState.state)
    }

    @Test
    fun publisherStateMachineContainsRequiredHappyPathAndRecoveryTransitions() {
        val happyPath = listOf(
            PublisherState.Idle,
            PublisherState.EditingDraft,
            PublisherState.Validating,
            PublisherState.UploadingMedia,
            PublisherState.ReadyToPublish,
            PublisherState.PublishingCommandSent,
            PublisherState.AwaitingCommit,
            PublisherState.PublishedAccepted,
            PublisherState.FanoutInProgress,
            PublisherState.FeedVisible,
        )

        happyPath.zipWithNext().forEach { (from, to) ->
            assertTrue(PublisherStateMachine.canTransition(from, to), "$from -> $to")
        }
        assertTrue(PublisherStateMachine.canTransition(PublisherState.FanoutInProgress, PublisherState.PartialDegradation))
        assertTrue(PublisherStateMachine.canTransition(PublisherState.PartialDegradation, PublisherState.FanoutInProgress))
        assertTrue(PublisherStateMachine.canTransition(PublisherState.Validating, PublisherState.FailedRecoverable))
        assertTrue(PublisherStateMachine.canTransition(PublisherState.Validating, PublisherState.FailedTerminal))
    }

    @Test
    fun highScaleAuthorsUseFanoutOnReadForFollowers() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        publishing.dependencies.authorScale.setTier(author, AuthorScaleTier.HighScale)

        val receipt = publishing.commandBus.execute(
            CreatePostCommand(
                actorId = author,
                body = "Pulso de alto alcance",
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_high_scale_publish"),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending().getOrThrow()
        val followerInbox = publishing.dependencies.feedInbox.listAllCandidates(follower)
        val feed = publishing.feedQuery.getFeed(follower, PageRequest(limit = 10)).getOrThrow()

        assertTrue(followerInbox.none { it.postId == receipt.postId })
        assertEquals(receipt.postId, feed.items.firstOrNull()?.postId)
        assertEquals(CandidateSource.FanoutOnRead, feed.items.firstOrNull()?.source)
    }

    @Test
    fun followerThresholdUsesFanoutOnReadWithoutManualTier() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val followers = (1..3).map { UserId("threshold_follower_$it") }
        followers.forEach {
            graph.seed(it)
            graph.useCases.followUser(it, author).getOrThrow()
        }
        val publishing = publishing(graph)
        publishing.dependencies.fanoutConfig.set(FanoutPolicyConfig(maxFollowersForFanoutOnWrite = 2))

        val receipt = publish(publishing, author, "Pulso con umbral dinamico")
        val decision = assertNotNull(publishing.dependencies.fanoutDecisions.get(receipt.postId))
        val report = assertNotNull(publishing.dependencies.fanoutReports.latest(receipt.postId))
        val followerInboxBeforeRead = publishing.dependencies.feedInbox.listAllCandidates(followers.first())
        val feed = publishing.feedQuery.getFeed(followers.first(), PageRequest(limit = 10)).getOrThrow()

        assertEquals(FanoutDistributionMode.FanoutOnRead, decision.mode)
        assertEquals(FanoutDecisionReason.FollowerThresholdExceeded, decision.reason)
        assertEquals(3, decision.authorMetrics.followerCount)
        assertEquals(FanoutDistributionMode.FanoutOnRead, report.mode)
        assertEquals(3, report.skippedFollowers)
        assertTrue(followerInboxBeforeRead.none { it.postId == receipt.postId })
        assertEquals(receipt.postId, feed.items.firstOrNull()?.postId)
        assertEquals(CandidateSource.FanoutOnRead, feed.items.firstOrNull()?.source)
        assertEquals(CandidateOrigin.FollowGraph, feed.items.firstOrNull()?.origin)
        assertEquals(FanoutDistributionMode.FanoutOnRead, feed.items.firstOrNull()?.distributionMode)
    }

    @Test
    fun viralPostMigratesToFanoutOnReadWithoutDuplicatingFeed() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        publishing.dependencies.fanoutConfig.set(
            FanoutPolicyConfig(maxFollowersForFanoutOnWrite = 100, viralScoreThresholdForFanoutOnRead = 50),
        )
        val receipt = publish(publishing, author, "Pulso que se vuelve viral")

        val migration = publishing.fanoutMigration.markPostMetricsAndRebalance(
            FanoutPostMetrics(
                postId = receipt.postId,
                impressionCount = 100,
                engagementCount = 5,
                updatedAt = publishing.dependencies.clock.now(),
            ),
            trace = publishing.nextTraceContext("test_viral_rebalance").withPostId(receipt.postId),
        ).getOrThrow()
        val feed = publishing.feedQuery.getFeed(follower, PageRequest(limit = 10)).getOrThrow()
        val followerCandidates = publishing.dependencies.feedInbox.listAllCandidates(follower)

        assertEquals(FanoutDistributionMode.FanoutOnRead, migration.mode)
        assertEquals(FanoutDecisionReason.ViralPost, migration.reason)
        assertTrue(followerCandidates.any { it.postId == receipt.postId && it.distributionMode == FanoutDistributionMode.FanoutOnWrite })
        assertTrue(followerCandidates.any { it.postId == receipt.postId && it.distributionMode == FanoutDistributionMode.FanoutOnRead })
        assertEquals(1, feed.items.count { it.postId == receipt.postId })
        assertEquals(FanoutDistributionMode.FanoutOnRead, publishing.dependencies.fanoutDecisions.get(receipt.postId)?.mode)
    }

    @Test
    fun migrationToFanoutOnWriteMaterializesFollowersIdempotently() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        publishing.dependencies.authorScale.setTier(author, AuthorScaleTier.HighScale)
        val receipt = publish(publishing, author, "Pulso migrable")

        val firstMigration = publishing.fanoutMigration.rebalancePost(
            postId = receipt.postId,
            forcedMode = FanoutDistributionMode.FanoutOnWrite,
            trace = publishing.nextTraceContext("test_force_write").withPostId(receipt.postId),
        ).getOrThrow()
        val secondMigration = publishing.fanoutMigration.rebalancePost(
            postId = receipt.postId,
            forcedMode = FanoutDistributionMode.FanoutOnWrite,
            trace = publishing.nextTraceContext("test_force_write_retry").withPostId(receipt.postId),
        ).getOrThrow()
        val followerCandidates = publishing.dependencies.feedInbox.listAllCandidates(follower)
            .filter { it.postId == receipt.postId && it.distributionMode == FanoutDistributionMode.FanoutOnWrite }

        assertEquals(FanoutDistributionMode.FanoutOnWrite, firstMigration.mode)
        assertEquals(FanoutDecisionReason.ForcedMigration, firstMigration.reason)
        assertEquals(0, secondMigration.materializedCandidates)
        assertEquals(1, followerCandidates.size)
    }

    @Test
    fun outboxRetryReplaysFanoutIdempotently() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        var failedOnce = false
        (publishing.dependencies.eventBackbone as InMemoryPublishingEventBackbone).register(
            object : PublishingEventSubscriber {
                override val subscriberId: EventSubscriberId = EventSubscriberId("test-transient-ack")

                override suspend fun handle(envelope: EventEnvelope): DomainResult<Unit> =
                    if (envelope.event is PostCommittedEvent && !failedOnce) {
                        failedOnce = true
                        DomainResult.failure(com.example.neuv3.features.social.domain.error.PersistenceFailure("transient fanout ack"))
                    } else {
                        DomainResult.success(Unit)
                    }
            },
        )

        val receipt = publishing.commandBus.execute(
            CreatePostCommand(
                actorId = author,
                body = "Pulso con retry idempotente",
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_publish"),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending()
        val beforeRetry = publishing.dependencies.feedInbox.listAllCandidates(follower).count { it.postId == receipt.postId }
        val reconciliation = publishing.reconciliation.reconcilePending().getOrThrow()
        val afterRetry = publishing.dependencies.feedInbox.listAllCandidates(follower).count { it.postId == receipt.postId }
        val reports = publishing.dependencies.fanoutReports.listByPost(receipt.postId)

        assertEquals(1, beforeRetry)
        assertEquals(1, afterRetry)
        assertEquals(0, reconciliation.stillPendingPosts)
        assertEquals(1, reports.size)
        assertTrue(publishing.metrics.snapshot().any { it.name == "event.consumer.duplicate_skipped" })
    }

    @Test
    fun eventBackboneDeduplicatesConsumersAndSupportsControlledReplay() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        val subscriberId = EventSubscriberId("test-replay-counter")
        var deliveries = 0
        (publishing.dependencies.eventBackbone as InMemoryPublishingEventBackbone).register(
            object : PublishingEventSubscriber {
                override val subscriberId: EventSubscriberId = subscriberId

                override suspend fun handle(envelope: EventEnvelope): DomainResult<Unit> {
                    deliveries++
                    return DomainResult.success(Unit)
                }
            },
        )
        val event = PostPublishedEvent(
            id = EventId("post-published:replay-test"),
            postId = PostId("replay-post"),
            authorId = author,
            materializedCandidates = 0,
            authorScaleTier = AuthorScaleTier.Normal,
            distributionMode = FanoutDistributionMode.FanoutOnRead,
            decisionReason = FanoutDecisionReason.AuthorHighScaleTier,
            occurredAt = publishing.dependencies.clock.now(),
            trace = publishing.nextTraceContext("test_replay").withPostId(PostId("replay-post")),
        )

        val first = publishing.dependencies.eventBackbone.publish(event).getOrThrow()
        val duplicate = publishing.dependencies.eventBackbone.publish(event).getOrThrow()
        publishing.dependencies.eventBackbone.replay(
            EventReplayRequest(
                requestId = RequestId("replay-skip"),
                subscriberId = subscriberId,
                fromOffset = first.offset,
            ),
        ).getOrThrow()
        publishing.dependencies.eventBackbone.replay(
            EventReplayRequest(
                requestId = RequestId("replay-force"),
                subscriberId = subscriberId,
                fromOffset = first.offset,
                force = true,
            ),
        ).getOrThrow()

        assertEquals(first.offset, duplicate.offset)
        assertEquals(2, deliveries)
    }

    @Test
    fun minimumPublicationAndFeedEventsArePartitionedAndTraceable() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)
        val published = publish(publishing, author, "Pulso con backbone")

        publishing.commandBus.edit(
            EditPostCommand(
                actorId = author,
                postId = published.postId,
                body = "Pulso editado",
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_edit").withPostId(published.postId),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending().getOrThrow()
        publishing.commandBus.delete(
            DeletePostCommand(
                actorId = author,
                postId = published.postId,
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_delete").withPostId(published.postId),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending().getOrThrow()
        val eventNames = publishing.database.state.eventJournal.map { it.event.eventName }.toSet()
        val feedAfterDelete = publishing.feedQuery.getFeed(follower, PageRequest(limit = 10)).getOrThrow()
        val candidateEvent = publishing.database.state.eventJournal.single {
            it.event is FeedCandidateGeneratedEvent &&
                it.event.viewerId == follower
        }
        val postEditedEvent = publishing.database.state.eventJournal.single { it.event.eventName == "post-edited" }

        assertTrue(
            setOf(
                "post-created",
                "post-published",
                "post-edited",
                "post-deleted",
                "moderation-requested",
                "moderation-completed",
                "fanout-requested",
                "feed-candidate-generated",
                "post-state-changed",
            ).all { it in eventNames },
        )
        assertEquals(EventPartitionKind.Viewer, candidateEvent.partitionKey.kind)
        assertEquals(follower.value, candidateEvent.partitionKey.value)
        assertEquals(EventPartitionKind.Post, postEditedEvent.partitionKey.kind)
        assertTrue(publishing.database.state.eventJournal.all { it.offset.value > 0L && it.event.trace.requestId.value.isNotBlank() })
        assertTrue(feedAfterDelete.items.none { it.postId == published.postId })
    }

    @Test
    fun feedServingUsesProjectedSnapshotsInsteadOfCanonicalPosts() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        graph.seed(follower)
        graph.useCases.followUser(follower, author).getOrThrow()
        val publishing = publishing(graph)

        val receipt = publish(publishing, author, "Snapshot original")
        val canonical = assertNotNull(publishing.dependencies.posts.get(receipt.postId))
        publishing.dependencies.posts.update(
            canonical.copy(
                text = PostText.from("Canonical changed after projection").getOrThrow(),
                version = canonical.version + 1,
                updatedAt = publishing.dependencies.clock.now(),
            ),
        )

        val feed = publishing.feedQuery.getFeed(follower, PageRequest(limit = 10)).getOrThrow()

        assertEquals("Snapshot original", feed.items.single { it.postId == receipt.postId }.text.value)
        assertTrue(publishing.metrics.snapshot().any { it.name == "feed.posts.rehydrated" })
    }

    @Test
    fun feedFiltersEligibilityAndKeepsSourceTaggedDedupedCandidates() = runTest {
        val graph = socialGraph()
        val muted = UserId("muted")
        val blocked = UserId("blocked")
        val privacy = UserId("privacy")
        val moderated = UserId("moderated")
        val fresh = UserId("fresh")
        listOf(author, follower, muted, blocked, privacy, moderated, fresh).forEach { graph.seed(it) }
        listOf(author, muted, blocked, privacy, moderated, fresh).forEach {
            graph.useCases.followUser(follower, it).getOrThrow()
        }
        val publishing = publishing(graph)

        val fatiguePosts = (0 until 5).map { index ->
            publish(publishing, author, "Fatigue pulse $index").postId
        }
        val mutedPost = publish(publishing, muted, "Muted pulse").postId
        val blockedPost = publish(publishing, blocked, "Blocked pulse").postId
        val privacyPost = publish(publishing, privacy, "Followers pulse", PostVisibility.Followers).postId
        val moderatedPost = publish(publishing, moderated, "Moderated pulse").postId
        val freshPost = publish(publishing, fresh, "Fresh pulse").postId

        publishing.dependencies.feedInbox.upsertAll(
            listOf(
                FeedCandidate(
                    id = FeedCandidateId("${follower.value}:${freshPost.value}:recommendation"),
                    viewerId = follower,
                    postId = freshPost,
                    authorId = fresh,
                    source = CandidateSource.Recommendation,
                    postCreatedAt = assertNotNull(publishing.dependencies.feedPosts.get(freshPost)).createdAt,
                    generatedAt = publishing.dependencies.clock.now(),
                    trace = publishing.nextTraceContext("test_duplicate_recommendation").withPostId(freshPost),
                ),
            ),
        )
        publishing.dependencies.feedMutes.mute(follower, muted, publishing.dependencies.clock.now())
        graph.useCases.blockUser(follower, blocked).getOrThrow()
        graph.follows.delete(follower, privacy)
        publishing.dependencies.moderation.upsert(
            PostModerationDecision(
                postId = moderatedPost,
                state = PostModerationState.Removed,
                reason = "policy",
                decidedAt = publishing.dependencies.clock.now(),
            ),
        )

        val feed = publishing.feedQuery.getFeed(follower, PageRequest(limit = 20)).getOrThrow()
        val postIds = feed.items.map { it.postId }.toSet()

        assertTrue(mutedPost !in postIds)
        assertTrue(blockedPost !in postIds)
        assertTrue(privacyPost !in postIds)
        assertTrue(moderatedPost !in postIds)
        assertTrue(freshPost in postIds)
        assertTrue(feed.items.count { it.postId in fatiguePosts } <= 3)
        assertEquals(CandidateSource.FanoutOnWrite, feed.items.single { it.postId == freshPost }.source)
        assertTrue(feed.items.all { it.sourcePlane == it.source.plane })
    }

    @Test
    fun feedCursorPaginationIsStableAcrossRankedCandidates() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)

        repeat(3) { index ->
            publish(publishing, author, "Pulso $index")
        }

        val firstPage = publishing.feedQuery.getFeed(author, PageRequest(limit = 2)).getOrThrow()
        val cursor = assertNotNull(firstPage.nextCursor)
        val secondPage = publishing.feedQuery.getFeed(author, PageRequest(limit = 2, cursor = cursor)).getOrThrow()

        assertEquals(2, firstPage.items.size)
        assertEquals(1, secondPage.items.size)
        assertTrue(firstPage.items.none { first -> secondPage.items.any { it.postId == first.postId } })
    }

    @Test
    fun feedMergeDedupesLocalPendingWhenRemoteConfirmationArrives() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        val receipt = publish(publishing, author, "Merged pulse")
        val remote = publishing.feedQuery.getFeed(author, PageRequest(limit = 10)).getOrThrow()
        val pending = PendingLocalFeedItem(
            postId = receipt.postId,
            authorId = author,
            text = PostText.from("Merged pulse").getOrThrow(),
            acceptedAt = receipt.acceptedAt,
            trace = receipt.trace,
        )

        val merged = FeedMergeService().mergePendingWithRemote(listOf(pending), remote)

        assertEquals(1, merged.items.count { it.postId == receipt.postId })
        assertEquals(MergedFeedItemState.RemoteConfirmed, merged.items.single { it.postId == receipt.postId }.state)
        assertNotNull(merged.items.single { it.postId == receipt.postId }.local)
    }

    @Test
    fun feedCacheServesCursorPagesFromRankedWindow() = runTest {
        val graph = socialGraph()
        graph.seed(author)
        val publishing = publishing(graph)
        repeat(3) { index ->
            publish(publishing, author, "Cache pulse $index")
        }

        val firstPage = publishing.feedQuery.getFeed(author, PageRequest(limit = 2)).getOrThrow()
        val secondPage = publishing.feedQuery.getFeed(author, PageRequest(limit = 2, cursor = assertNotNull(firstPage.nextCursor))).getOrThrow()

        assertTrue(secondPage.items.all { it.servedFromCache })
        assertTrue(publishing.metrics.snapshot().any { it.name == "feed.query.cache_hit" })
    }


    private fun socialGraph(): InMemorySocialGraphComponent =
        InMemorySocialGraphComponent.create()

    private fun publishing(graph: InMemorySocialGraphComponent): InMemoryPublishingComponent =
        InMemoryPublishingComponent.create(
            socialGraphDatabase = graph.database,
            socialGraphDependencies = graph.dependencies,
        )

    private fun publisherHolder(
        publishing: InMemoryPublishingComponent,
        store: InMemoryPublisherLocalDraftStore,
        scope: CoroutineScope,
    ): PublisherStateHolder =
        PublisherStateHolder(
            actorId = author,
            commandBus = publishing.commandBus,
            feedQuery = publishing.feedQuery,
            reconciliation = publishing.reconciliation,
            dependencies = publishing.dependencies,
            localStore = store,
            mediaUploader = LocalEchoPublishingMediaUploader(publishing.dependencies.clock),
            scope = scope,
        )

    private suspend fun publish(
        publishing: InMemoryPublishingComponent,
        actorId: UserId,
        body: String,
        visibility: PostVisibility = PostVisibility.Public,
    ): PublishPostReceipt {
        val receipt = publishing.commandBus.execute(
            CreatePostCommand(
                actorId = actorId,
                body = body,
                visibility = visibility,
                idempotencyKey = publishing.nextIdempotencyKey(),
                trace = publishing.nextTraceContext("test_publish"),
            ),
        ).getOrThrow()
        publishing.reconciliation.reconcilePending().getOrThrow()
        return receipt
    }

    private suspend fun InMemorySocialGraphComponent.seed(userId: UserId) {
        seedUser(
            userId = userId,
            handle = Handle.from(userId.value).getOrThrow(),
            displayName = DisplayName.from(userId.value.replaceFirstChar { it.uppercase() }).getOrThrow(),
        )
    }
}
