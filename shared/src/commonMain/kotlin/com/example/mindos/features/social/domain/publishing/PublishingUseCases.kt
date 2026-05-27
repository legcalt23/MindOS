package com.example.mindos.features.social.domain.publishing

import com.example.mindos.features.social.domain.error.ActionNotAuthorized
import com.example.mindos.features.social.domain.error.IntegrityViolation
import com.example.mindos.features.social.domain.error.PersistenceFailure
import com.example.mindos.features.social.domain.error.UserNotFound
import com.example.mindos.features.social.domain.valueobject.Cursor
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.Page
import com.example.mindos.features.social.shared.util.PageRequest

class PublishPostCommandHandler(
    private val dependencies: PublishingDependencies,
) {
    suspend fun handle(command: CreatePostCommand): DomainResult<PublishPostReceipt> =
        dependencies.transactions.writeResult {
            val startedAt = dependencies.clock.now()
            val commandTrace = command.trace.withIdempotencyKey(command.idempotencyKey)
            dependencies.traceSink.record(
                StructuredLogEvent(
                    traceId = commandTrace.traceId.value,
                    requestId = commandTrace.requestId.value,
                    authorId = command.actorId.value,
                    event = "publish_command_received",
                    stage = Stage.Publishing.name,
                    result = Outcome.Started.name
                )
            )
            dependencies.idempotency.get(command.idempotencyKey)?.let { existing ->
                val receipt = existing.receipt as? PublishPostReceipt
                    ?: return@writeResult DomainResult.failure(PersistenceFailure("Idempotency key ${command.idempotencyKey.value} belongs to ${existing.commandName}."))
                dependencies.metrics.increment(
                    MetricName.DuplicateSuppression,
                    DefaultMetricTags(stage = Stage.Publishing, outcome = Outcome.Skipped, isDuplicateSuppressed = true)
                )
                return@writeResult DomainResult.success(receipt)
            }

            val account = dependencies.profiles.getAccount(command.actorId)
                ?: return@writeResult DomainResult.failure(UserNotFound(command.actorId))
            if (!account.canParticipateInSocialGraph) {
                return@writeResult DomainResult.failure(ActionNotAuthorized("User ${command.actorId.value} cannot publish."))
            }

            val text = when (val parsed = PostText.from(command.body)) {
                is DomainResult.Success -> parsed.value
                is DomainResult.Failure -> return@writeResult parsed
            }

            val now = dependencies.clock.now()
            val postId = dependencies.postIds.nextPostId(command.actorId, command.idempotencyKey)
            val trace = commandTrace.withPostId(postId)
            dependencies.posts.get(postId)?.let { existing ->
                if (existing.idempotencyKey == command.idempotencyKey && existing.authorId == command.actorId) {
                    val receipt = PublishPostReceipt(
                        postId = existing.id,
                        state = existing.state,
                        acceptedAt = existing.createdAt,
                        trace = existing.trace,
                    )
                    dependencies.idempotency.put(
                        CommandIdempotencyRecord(
                            key = command.idempotencyKey,
                            commandName = "CreatePostCommand",
                            actorId = command.actorId,
                            receipt = receipt,
                            createdAt = now,
                            trace = existing.trace,
                        ),
                    )
                    dependencies.metrics.increment(
                        MetricName.DuplicateSuppression,
                        DefaultMetricTags(stage = Stage.Publishing, outcome = Outcome.Skipped, isDuplicateSuppressed = true)
                    )
                    return@writeResult DomainResult.success(receipt)
                }
                return@writeResult DomainResult.failure(PersistenceFailure("Post ${postId.value} already exists."))
            }
            val received = CanonicalPost(
                id = postId,
                authorId = command.actorId,
                text = text,
                visibility = command.visibility,
                mediaAttachments = command.mediaAttachments,
                state = PublicationState.Received,
                idempotencyKey = command.idempotencyKey,
                createdAt = now,
                updatedAt = now,
                trace = trace,
            )

            // Inline moderation
            val lowerText = text.value.lowercase()
            val modState = when {
                lowerText.contains("hate speech") || lowerText.contains("slur") -> PostModerationState.Removed
                lowerText.contains("sensitive") || lowerText.contains("nsfw") -> PostModerationState.Hidden
                lowerText.contains("crypto") || lowerText.contains("spam") || command.mediaAttachments.isNotEmpty() -> PostModerationState.PublishedPendingReview
                else -> PostModerationState.PublishedFullDistribution
            }
            dependencies.moderation.upsert(
                PostModerationDecision(
                    postId = postId,
                    state = modState,
                    reason = "Inline moderation at publish time",
                    ruleVersion = "inline_v1",
                    decidedAt = now,
                )
            )

            val committed = when (val transition = PublicationStateMachine.transition(received, PublicationState.Committed, now)) {
                is DomainResult.Success -> transition.value
                is DomainResult.Failure -> return@writeResult transition
            }
            val outboxed = when (val transition = PublicationStateMachine.transition(committed, PublicationState.Outboxed, now)) {
                is DomainResult.Success -> transition.value
                is DomainResult.Failure -> return@writeResult transition
            }

            if (!dependencies.posts.insert(outboxed)) {
                return@writeResult DomainResult.failure(PersistenceFailure("Post ${postId.value} already exists."))
            }

            val postCreated = PostCommittedEvent(
                id = dependencies.eventIds.nextEventId("post-created"),
                postId = postId,
                authorId = command.actorId,
                visibility = command.visibility,
                mediaAttachments = command.mediaAttachments,
                occurredAt = now,
                trace = trace,
            )
            val stateChanged = PostStateChangedEvent(
                id = deterministicPublishingEventId(
                    "post-state-changed",
                    postId.value,
                    PublicationState.Received.name,
                    PublicationState.Outboxed.name,
                    outboxed.version.toString(),
                ),
                postId = postId,
                authorId = command.actorId,
                from = PublicationState.Received,
                to = PublicationState.Outboxed,
                canonicalVersion = outboxed.version,
                occurredAt = now,
                trace = trace,
            )
            val moderationRequested = ModerationRequestedEvent(
                id = deterministicPublishingEventId("moderation-requested", postId.value),
                postId = postId,
                authorId = command.actorId,
                visibility = command.visibility,
                occurredAt = now,
                trace = trace,
            )
            for (event in listOf(postCreated, stateChanged, moderationRequested)) {
                val outboxMessage = OutboxMessage(
                    id = dependencies.outboxIds.nextOutboxMessageId(postId, event),
                    aggregateId = postId,
                    event = event,
                    status = OutboxStatus.Pending,
                    createdAt = now,
                )
                if (!dependencies.outbox.append(outboxMessage)) {
                    return@writeResult DomainResult.failure(PersistenceFailure("Outbox message ${outboxMessage.id.value} already exists."))
                }
            }

            val receipt = PublishPostReceipt(
                postId = postId,
                state = outboxed.state,
                acceptedAt = now,
                trace = trace,
            )
            dependencies.idempotency.put(
                CommandIdempotencyRecord(
                    key = command.idempotencyKey,
                    commandName = "CreatePostCommand",
                    actorId = command.actorId,
                    receipt = receipt,
                    createdAt = now,
                    trace = trace,
                ),
            )
            val durationMs = dependencies.clock.now().epochMillis - startedAt.epochMillis
            dependencies.metrics.increment(MetricName.PublishSuccess, DefaultMetricTags(stage = Stage.Publishing, outcome = Outcome.Succeeded))
            dependencies.metrics.recordDistribution(
                MetricName.PublishLatency,
                durationMs,
                DefaultMetricTags(stage = Stage.Publishing, outcome = Outcome.Succeeded)
            )
            dependencies.traceSink.record(
                StructuredLogEvent(
                    traceId = trace.traceId.value,
                    requestId = trace.requestId.value,
                    postId = postId.value,
                    authorId = command.actorId.value,
                    event = "publish_command_completed",
                    stage = Stage.Publishing.name,
                    result = Outcome.Succeeded.name,
                    durationMs = durationMs
                )
            )
            DomainResult.success(receipt)
        }
}

class EditPostCommandHandler(
    private val dependencies: PublishingDependencies,
) {
    suspend fun handle(command: EditPostCommand): DomainResult<PostMutationReceipt> =
        dependencies.transactions.writeResult {
            val commandTrace = command.trace.withIdempotencyKey(command.idempotencyKey).withPostId(command.postId)
            dependencies.idempotency.get(command.idempotencyKey)?.let { existing ->
                val receipt = existing.receipt as? PostMutationReceipt
                    ?: return@writeResult DomainResult.failure(PersistenceFailure("Idempotency key ${command.idempotencyKey.value} belongs to ${existing.commandName}."))
                dependencies.metrics.increment("publish.edit.idempotent_hit", existing.trace.tags())
                return@writeResult DomainResult.success(receipt)
            }

            val existing = dependencies.posts.get(command.postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${command.postId.value}."))
            if (existing.authorId != command.actorId) {
                return@writeResult DomainResult.failure(ActionNotAuthorized("User ${command.actorId.value} cannot edit ${command.postId.value}."))
            }
            if (existing.state == PublicationState.Deleted || existing.state == PublicationState.Rejected) {
                return@writeResult DomainResult.failure(IntegrityViolation("Post ${command.postId.value} cannot be edited from ${existing.state}."))
            }
            val text = when (val parsed = PostText.from(command.body)) {
                is DomainResult.Success -> parsed.value
                is DomainResult.Failure -> return@writeResult parsed
            }
            val now = dependencies.clock.now()
            val updated = existing.copy(
                text = text,
                mediaAttachments = command.mediaAttachments,
                updatedAt = now,
                version = existing.version + 1,
                trace = commandTrace,
            )
            if (!dependencies.posts.update(updated)) {
                return@writeResult DomainResult.failure(PersistenceFailure("Post ${command.postId.value} could not be updated."))
            }

            val event = PostEditedEvent(
                id = deterministicPublishingEventId("post-edited", command.postId.value, updated.version.toString()),
                postId = command.postId,
                authorId = command.actorId,
                canonicalVersion = updated.version,
                occurredAt = now,
                trace = commandTrace,
            )
            val outboxMessage = OutboxMessage(
                id = dependencies.outboxIds.nextOutboxMessageId(command.postId, event),
                aggregateId = command.postId,
                event = event,
                status = OutboxStatus.Pending,
                createdAt = now,
            )
            if (!dependencies.outbox.append(outboxMessage)) {
                return@writeResult DomainResult.failure(PersistenceFailure("Outbox message ${outboxMessage.id.value} already exists."))
            }

            val receipt = PostMutationReceipt(
                postId = command.postId,
                state = updated.state,
                acceptedAt = now,
                trace = commandTrace,
            )
            dependencies.idempotency.put(
                CommandIdempotencyRecord(
                    key = command.idempotencyKey,
                    commandName = "EditPostCommand",
                    actorId = command.actorId,
                    receipt = receipt,
                    createdAt = now,
                    trace = commandTrace,
                ),
            )
            dependencies.metrics.increment("publish.edit.accepted", commandTrace.tags())
            DomainResult.success(receipt)
        }
}

class DeletePostCommandHandler(
    private val dependencies: PublishingDependencies,
) {
    suspend fun handle(command: DeletePostCommand): DomainResult<PostMutationReceipt> =
        dependencies.transactions.writeResult {
            val commandTrace = command.trace.withIdempotencyKey(command.idempotencyKey).withPostId(command.postId)
            dependencies.idempotency.get(command.idempotencyKey)?.let { existing ->
                val receipt = existing.receipt as? PostMutationReceipt
                    ?: return@writeResult DomainResult.failure(PersistenceFailure("Idempotency key ${command.idempotencyKey.value} belongs to ${existing.commandName}."))
                dependencies.metrics.increment("publish.delete.idempotent_hit", existing.trace.tags())
                return@writeResult DomainResult.success(receipt)
            }

            val existing = dependencies.posts.get(command.postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${command.postId.value}."))
            if (existing.authorId != command.actorId) {
                return@writeResult DomainResult.failure(ActionNotAuthorized("User ${command.actorId.value} cannot delete ${command.postId.value}."))
            }
            val now = dependencies.clock.now()
            val deleted = when (val transition = PublicationStateMachine.transition(existing, PublicationState.Deleted, now)) {
                is DomainResult.Success -> transition.value.copy(trace = commandTrace)
                is DomainResult.Failure -> return@writeResult transition
            }
            if (!dependencies.posts.update(deleted)) {
                return@writeResult DomainResult.failure(PersistenceFailure("Post ${command.postId.value} could not be deleted."))
            }

            val events = listOf(
                PostDeletedEvent(
                    id = deterministicPublishingEventId("post-deleted", command.postId.value, deleted.version.toString()),
                    postId = command.postId,
                    authorId = command.actorId,
                    canonicalVersion = deleted.version,
                    occurredAt = now,
                    trace = commandTrace,
                ),
                PostStateChangedEvent(
                    id = deterministicPublishingEventId(
                        "post-state-changed",
                        command.postId.value,
                        existing.state.name,
                        PublicationState.Deleted.name,
                        deleted.version.toString(),
                    ),
                    postId = command.postId,
                    authorId = command.actorId,
                    from = existing.state,
                    to = PublicationState.Deleted,
                    canonicalVersion = deleted.version,
                    occurredAt = now,
                    trace = commandTrace,
                ),
            )
            for (event in events) {
                val outboxMessage = OutboxMessage(
                    id = dependencies.outboxIds.nextOutboxMessageId(command.postId, event),
                    aggregateId = command.postId,
                    event = event,
                    status = OutboxStatus.Pending,
                    createdAt = now,
                )
                if (!dependencies.outbox.append(outboxMessage)) {
                    return@writeResult DomainResult.failure(PersistenceFailure("Outbox message ${outboxMessage.id.value} already exists."))
                }
            }

            val receipt = PostMutationReceipt(
                postId = command.postId,
                state = deleted.state,
                acceptedAt = now,
                trace = commandTrace,
            )
            dependencies.idempotency.put(
                CommandIdempotencyRecord(
                    key = command.idempotencyKey,
                    commandName = "DeletePostCommand",
                    actorId = command.actorId,
                    receipt = receipt,
                    createdAt = now,
                    trace = commandTrace,
                ),
            )
            dependencies.metrics.increment("publish.delete.accepted", commandTrace.tags())
            DomainResult.success(receipt)
        }
}

data class OutboxRelayConfig(
    val maxBatchSize: Int = 100,
    val dispatchLeaseTimeoutMillis: Long = 30_000,
    val baseRetryDelayMillis: Long = 0,
    val maxRetryDelayMillis: Long = 60_000,
) {
    init {
        require(maxBatchSize in 1..500) { "maxBatchSize must be between 1 and 500." }
        require(dispatchLeaseTimeoutMillis >= 0) { "dispatchLeaseTimeoutMillis cannot be negative." }
        require(baseRetryDelayMillis >= 0) { "baseRetryDelayMillis cannot be negative." }
        require(maxRetryDelayMillis >= baseRetryDelayMillis) { "maxRetryDelayMillis must be >= baseRetryDelayMillis." }
    }
}

class OutboxDispatcher(
    private val dependencies: PublishingDependencies,
    private val config: OutboxRelayConfig = OutboxRelayConfig(),
) {
    suspend fun dispatchPending(limit: Int = 100): DomainResult<Int> {
        var dispatched = 0
        val messages = dependencies.transactions.write {
            dependencies.outbox.claimPending(
                limit = limit.coerceIn(1, config.maxBatchSize),
                now = dependencies.clock.now(),
                dispatchLeaseTimeoutMillis = config.dispatchLeaseTimeoutMillis,
            )
        }
        for (message in messages) {
            dependencies.traceSink.record(
                "outbox.relay.dispatching",
                message.event.trace,
                message.event.tags() + mapOf(
                    "outboxMessageId" to message.id.value,
                    "attempt" to message.attempts.toString(),
                ),
            )
            when (val result = dependencies.eventBackbone.publish(message.event)) {
                is DomainResult.Success -> {
                    val envelope = result.value
                    dependencies.transactions.write {
                        dependencies.outbox.markDispatched(message.id, dependencies.clock.now(), envelope.offset)
                    }
                    dependencies.metrics.increment(
                        "outbox.dispatched",
                        envelope.tags() + mapOf("outboxMessageId" to message.id.value),
                    )
                    dispatched++
                }
                is DomainResult.Failure -> {
                    val failedAt = dependencies.clock.now()
                    val nextAttemptAt = Timestamp(failedAt.epochMillis + retryDelayMillis(message.attempts))
                    dependencies.transactions.write {
                        dependencies.outbox.markFailed(message.id, failedAt, result.error.message, nextAttemptAt)
                    }
                    dependencies.metrics.increment(
                        "outbox.failed",
                        message.event.tags() + mapOf(
                            "outboxMessageId" to message.id.value,
                            "nextAttemptAt" to nextAttemptAt.epochMillis.toString(),
                        ),
                    )
                    return result
                }
            }
        }
        return DomainResult.success(dispatched)
    }

    private fun retryDelayMillis(attempt: Int): Long {
        if (config.baseRetryDelayMillis == 0L) return 0L
        val safeAttempt = attempt.coerceAtLeast(1).coerceAtMost(30)
        val multiplier = 1L shl (safeAttempt - 1).coerceAtMost(20)
        return (config.baseRetryDelayMillis * multiplier).coerceAtMost(config.maxRetryDelayMillis)
    }
}

class FeedProjectionPipeline(
    private val dependencies: PublishingDependencies,
    private val candidateInjection: CandidateInjectionPipeline,
) : PublishingEventSubscriber {
    override val subscriberId: EventSubscriberId = EventSubscriberId("feed-projection")

    override suspend fun handle(envelope: EventEnvelope): DomainResult<Unit> =
        when (val event = envelope.event) {
            is PostCommittedEvent -> projectCommittedPost(event)
            is PostEditedEvent -> projectEditedPost(event)
            is PostDeletedEvent -> projectDeletedPost(event)
            is PostPublishedEvent,
            is PostStateChangedEvent,
            is ModerationRequestedEvent,
            is ModerationCompletedEvent,
            is FanoutRequestedEvent,
            is FeedCandidateGeneratedEvent -> DomainResult.success(Unit)
        }

    private suspend fun projectCommittedPost(event: PostCommittedEvent): DomainResult<Unit> =
        dependencies.transactions.writeResult {
            val post = dependencies.posts.get(event.postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${event.postId.value}."))
            val readyForFanout = when (post.state) {
                PublicationState.Published -> post
                PublicationState.Propagating -> post
                PublicationState.Outboxed -> {
                    when (val transition = PublicationStateMachine.transition(post, PublicationState.Propagating, dependencies.clock.now())) {
                        is DomainResult.Success -> {
                            dependencies.posts.update(transition.value)
                            transition.value
                        }
                        is DomainResult.Failure -> return@writeResult transition
                    }
                }
                PublicationState.Failed -> {
                    val outboxed = when (val transition = PublicationStateMachine.transition(post, PublicationState.Outboxed, dependencies.clock.now())) {
                        is DomainResult.Success -> transition.value
                        is DomainResult.Failure -> return@writeResult transition
                    }
                    when (val transition = PublicationStateMachine.transition(outboxed, PublicationState.Propagating, dependencies.clock.now())) {
                        is DomainResult.Success -> {
                            dependencies.posts.update(transition.value)
                            transition.value
                        }
                        is DomainResult.Failure -> return@writeResult transition
                    }
                }
                else -> return@writeResult DomainResult.failure(
                    IntegrityViolation("Post ${post.id.value} cannot be projected from ${post.state}."),
                )
            }
            candidateInjection.indexPostSnapshot(readyForFanout)
            val fanoutReport = when (val injected = candidateInjection.injectWriteCandidates(readyForFanout)) {
                is DomainResult.Success -> injected.value
                is DomainResult.Failure -> return@writeResult injected
            }
            val published = if (readyForFanout.state == PublicationState.Published) {
                readyForFanout
            } else {
                when (val transition = PublicationStateMachine.transition(readyForFanout, PublicationState.Published, dependencies.clock.now())) {
                    is DomainResult.Success -> {
                        dependencies.posts.update(transition.value)
                        transition.value
                    }
                    is DomainResult.Failure -> return@writeResult transition
                }
            }
            candidateInjection.indexPostSnapshot(published)
            val tier = dependencies.authorScale.getTier(event.authorId)
            dependencies.metrics.increment(
                name = "feed.inbox.materialized",
                tags = event.trace.tags() + mapOf(
                    "tier" to tier.name,
                    "mode" to fanoutReport.mode.name,
                    "reason" to fanoutReport.reason.name,
                ),
                value = fanoutReport.materializedCandidates.toLong(),
            )
            when (val publishedEvent = dependencies.eventBackbone.publish(
                PostPublishedEvent(
                    id = deterministicPublishingEventId("post-published", event.postId.value),
                    postId = event.postId,
                    authorId = event.authorId,
                    materializedCandidates = fanoutReport.materializedCandidates,
                    authorScaleTier = tier,
                    distributionMode = fanoutReport.mode,
                    decisionReason = fanoutReport.reason,
                    occurredAt = dependencies.clock.now(),
                    trace = event.trace,
                ),
            )) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> return@writeResult publishedEvent
            }
            when (val stateChangedEvent = dependencies.eventBackbone.publish(
                PostStateChangedEvent(
                    id = deterministicPublishingEventId(
                        "post-state-changed",
                        event.postId.value,
                        readyForFanout.state.name,
                        PublicationState.Published.name,
                        published.version.toString(),
                    ),
                    postId = event.postId,
                    authorId = event.authorId,
                    from = readyForFanout.state,
                    to = PublicationState.Published,
                    canonicalVersion = published.version,
                    occurredAt = dependencies.clock.now(),
                    trace = event.trace,
                ),
            )) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> return@writeResult stateChangedEvent
            }
            DomainResult.success(Unit)
        }

    private suspend fun projectEditedPost(event: PostEditedEvent): DomainResult<Unit> =
        dependencies.transactions.writeResult {
            val post = dependencies.posts.get(event.postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${event.postId.value}."))
            if (post.state == PublicationState.Deleted || post.state == PublicationState.Rejected) {
                return@writeResult DomainResult.success(Unit)
            }
            candidateInjection.indexPostSnapshot(post)
            dependencies.rankedFeedCache.invalidateAll(post.audienceCacheKeys())
            DomainResult.success(Unit)
        }

    private suspend fun projectDeletedPost(event: PostDeletedEvent): DomainResult<Unit> =
        dependencies.transactions.writeResult {
            val post = dependencies.posts.get(event.postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${event.postId.value}."))
            candidateInjection.indexPostSnapshot(post)
            dependencies.rankedFeedCache.invalidateAll(post.audienceCacheKeys())
            dependencies.metrics.increment("feed.post.deleted_projected", event.tags())
            DomainResult.success(Unit)
        }

    private suspend fun CanonicalPost.audienceCacheKeys(): Set<UserId> {
        val viewers = mutableSetOf(authorId)
        var cursor: Cursor? = null
        do {
            val page = dependencies.audience.listFollowers(
                authorId = authorId,
                pageRequest = PageRequest(limit = 200, cursor = cursor),
            )
            viewers += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        return viewers
        }
}

class ModerationPipeline(
    private val dependencies: PublishingDependencies,
) : PublishingEventSubscriber {
    override val subscriberId: EventSubscriberId = EventSubscriberId("moderation-pipeline")

    override suspend fun handle(envelope: EventEnvelope): DomainResult<Unit> =
        when (val event = envelope.event) {
            is ModerationRequestedEvent -> completeModeration(event)
            is PostCommittedEvent,
            is PostPublishedEvent,
            is PostEditedEvent,
            is PostDeletedEvent,
            is ModerationCompletedEvent,
            is FanoutRequestedEvent,
            is FeedCandidateGeneratedEvent,
            is PostStateChangedEvent -> DomainResult.success(Unit)
        }

    private suspend fun completeModeration(event: ModerationRequestedEvent): DomainResult<Unit> =
        dependencies.transactions.writeResult {
            val now = dependencies.clock.now()
            val existing = dependencies.moderation.get(event.postId)
            val decision = existing ?: PostModerationDecision(
                postId = event.postId,
                state = PostModerationState.PublishedFullDistribution,
                decidedAt = now,
            )
            dependencies.moderation.upsert(decision) // Async review could happen here
            when (val completed = dependencies.eventBackbone.publish(
                ModerationCompletedEvent(
                    id = deterministicPublishingEventId(
                        "moderation-completed",
                        event.postId.value,
                        decision.state.name,
                    ),
                    postId = event.postId,
                    state = decision.state,
                    reason = decision.reason,
                    occurredAt = now,
                    trace = event.trace,
                ),
            )) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> return@writeResult completed
            }
            val durationMs = now.epochMillis - event.occurredAt.epochMillis
            dependencies.metrics.recordDistribution(
                MetricName.ModerationLatency,
                durationMs,
                DefaultMetricTags(stage = Stage.Moderation, outcome = Outcome.Succeeded)
            )
            dependencies.traceSink.record(
                StructuredLogEvent(
                    traceId = event.trace.traceId.value,
                    requestId = event.trace.requestId.value,
                    postId = event.postId.value,
                    event = "moderation_completed",
                    stage = Stage.Moderation.name,
                    result = Outcome.Succeeded.name,
                    durationMs = durationMs,
                    moderationDecision = decision.state.name
                )
            )
            DomainResult.success(Unit)
        }
}

class PublicationReconciliationService(
    private val dependencies: PublishingDependencies,
    private val dispatcher: OutboxDispatcher,
) {
    suspend fun reconcilePending(limit: Int = 100): DomainResult<ReconciliationReport> {
        val repaired = dependencies.transactions.write {
            var repairedMessages = 0
            val page = dependencies.posts.listByStates(
                states = PublicationState.entries.filter(PublicationStateMachine::isReconcileable).toSet(),
                pageRequest = PageRequest(limit = limit.coerceIn(1, 200)),
            )
            for (post in page.items) {
                if (!dependencies.outbox.hasUndispatchedForAggregate(post.id)) {
                    val event = PostCommittedEvent(
                        id = deterministicPublishingEventId("post-created", post.id.value, "reconcile"),
                        postId = post.id,
                        authorId = post.authorId,
                        visibility = post.visibility,
                        mediaAttachments = post.mediaAttachments,
                        occurredAt = dependencies.clock.now(),
                        trace = post.trace,
                    )
                    dependencies.outbox.append(
                        OutboxMessage(
                            id = dependencies.outboxIds.nextOutboxMessageId(post.id, event),
                            aggregateId = post.id,
                            event = event,
                            status = OutboxStatus.Pending,
                            createdAt = dependencies.clock.now(),
                        ),
                    )
                    repairedMessages++
                }
            }
            repairedMessages
        }

        val dispatched = when (val result = dispatcher.dispatchPending(limit)) {
            is DomainResult.Success -> result.value
            is DomainResult.Failure -> return result
        }
        val stillPending = dependencies.transactions.read {
            dependencies.posts.listByStates(
                states = setOf(PublicationState.Outboxed, PublicationState.Propagating, PublicationState.Failed),
                pageRequest = PageRequest(limit = limit.coerceIn(1, 200)),
            ).items.size
        }
        dependencies.metrics.increment(
            "publish.reconciliation.completed",
            mapOf("repaired" to repaired.toString(), "dispatched" to dispatched.toString()),
        )
        return DomainResult.success(
            ReconciliationReport(
                dispatchedOutboxMessages = dispatched,
                repairedOutboxMessages = repaired,
                stillPendingPosts = stillPending,
            ),
        )
    }
}

class FanoutMigrationService(
    private val dependencies: PublishingDependencies,
    private val fanoutPolicy: FanoutDistributionPolicy,
    private val candidateInjection: CandidateInjectionPipeline,
) {
    suspend fun rebalancePost(
        postId: PostId,
        forcedMode: FanoutDistributionMode? = null,
        trace: TraceContext = dependencies.traceContexts.nextTraceContext("fanout_migration").withPostId(postId),
    ): DomainResult<FanoutDeliveryReport> =
        dependencies.transactions.writeResult {
            val post = dependencies.posts.get(postId)
                ?: return@writeResult DomainResult.failure(IntegrityViolation("Missing canonical post ${postId.value}."))
            if (post.state != PublicationState.Published) {
                return@writeResult DomainResult.failure(
                    IntegrityViolation("Post ${postId.value} must be published before fanout migration."),
                )
            }
            fanoutPolicy.decideForPost(
                post = post,
                trace = trace,
                forcedMode = forcedMode,
                useExistingDecision = false,
            )
            candidateInjection.indexPostSnapshot(post)
            when (val injected = candidateInjection.injectWriteCandidates(post)) {
                is DomainResult.Success -> {
                    dependencies.traceSink.record(
                        span = "fanout.migration.completed",
                        trace = trace,
                        attributes = mapOf(
                            "postId" to postId.value,
                            "mode" to injected.value.mode.name,
                            "reason" to injected.value.reason.name,
                            "materializedCandidates" to injected.value.materializedCandidates.toString(),
                        ),
                    )
                    DomainResult.success(injected.value)
                }
                is DomainResult.Failure -> injected
            }
        }

    suspend fun markPostMetricsAndRebalance(
        metrics: FanoutPostMetrics,
        trace: TraceContext = dependencies.traceContexts.nextTraceContext("fanout_metrics").withPostId(metrics.postId),
    ): DomainResult<FanoutDeliveryReport> {
        dependencies.fanoutPostMetrics.upsert(metrics)
        return rebalancePost(metrics.postId, trace = trace)
    }
}

class GetFeedUseCase(
    private val dependencies: PublishingDependencies,
    private val eligibility: FeedEligibilityService,
    private val candidateInjection: CandidateInjectionPipeline,
    private val featureEnrichment: FeedFeatureEnrichmentPlane,
    private val rankingPlane: FeedRankingPlane,
    private val config: FeedServingConfig = FeedServingConfig(),
) {
    suspend fun getFeed(
        viewerId: UserId,
        pageRequest: PageRequest,
        trace: TraceContext = dependencies.traceContexts.nextTraceContext("feed_read"),
    ): DomainResult<Page<FeedItem>> {
        val startedAt = dependencies.clock.now()
        dependencies.traceSink.record(
            StructuredLogEvent(
                traceId = trace.traceId.value,
                requestId = trace.requestId.value,
                authorId = viewerId.value,
                event = "feed_query_received",
                stage = Stage.FeedServing.name,
                result = Outcome.Started.name
            )
        )

        when (val injected = candidateInjection.injectReadCandidates(viewerId, trace)) {
            is DomainResult.Success -> Unit
            is DomainResult.Failure -> return injected
        }

        val cached = dependencies.rankedFeedCache.get(viewerId)
        if (cached != null && cached.isFresh(dependencies.clock.now(), config)) {
            val cachedPage = cached.entries.pageRanked(pageRequest)
            val cachedItems = hydrateRankedPage(viewerId, cachedPage.items, trace, servedFromCache = true)
            if (cachedItems.size == cachedPage.items.size) {
                val isStale = (dependencies.clock.now().epochMillis - cached.generatedAt.epochMillis) >= 30 * 60 * 1000L
                val freshness = if (isStale) FeedFreshness.STALE else if ((dependencies.clock.now().epochMillis - cached.generatedAt.epochMillis) >= 5 * 60 * 1000L) FeedFreshness.WARM else FeedFreshness.FRESH
                
                dependencies.metrics.increment(
                    MetricName.FeedLatency,
                    DefaultMetricTags(stage = Stage.FeedServing, outcome = Outcome.Succeeded, freshness = freshness)
                )
                dependencies.metrics.increment("feed.query.cache_hit", trace.tags())
                if (isStale) {
                    dependencies.metrics.increment(MetricName.StaleFeedRead, DefaultMetricTags(stage = Stage.FeedServing, freshness = FeedFreshness.STALE))
                }
                if (cachedItems.isEmpty()) {
                    dependencies.metrics.increment(MetricName.FeedReadEmpty, DefaultMetricTags(stage = Stage.FeedServing))
                }
                
                dependencies.metrics.recordDistribution(
                    MetricName.FeedLatency,
                    dependencies.clock.now().epochMillis - startedAt.epochMillis,
                    DefaultMetricTags(stage = Stage.FeedServing, outcome = Outcome.Succeeded, freshness = freshness)
                )
                return DomainResult.success(Page(items = cachedItems, nextCursor = cachedPage.nextCursor))
            }
            dependencies.rankedFeedCache.invalidate(viewerId)
            dependencies.metrics.increment("feed.query.cache_rebuilt", trace.tags())
        }

        val candidates = collectCandidateWindow(viewerId, trace)
        val snapshots = dependencies.feedPosts.getMany(candidates.map { it.postId })
        val eligibleCandidates = filterEligibleCandidates(viewerId, candidates, snapshots, trace)
        val enriched = featureEnrichment.enrich(viewerId, eligibleCandidates, snapshots, trace)
        val ranked = filterAuthorFatigue(rankingPlane.rank(viewerId, enriched, trace), trace)

        dependencies.rankedFeedCache.put(
            RankedFeedCacheRecord(
                viewerId = viewerId,
                entries = ranked,
                generatedAt = dependencies.clock.now(),
                rankingModelVersion = ranked.firstOrNull()?.modelVersion ?: "heuristic.feed.v1",
                candidateWindowSize = config.candidateWindowSize,
            ),
        )
        val page = ranked.pageRanked(pageRequest)
        val items = hydrateRankedPage(viewerId, page.items, trace, servedFromCache = false)
        if (items.isEmpty()) {
            dependencies.metrics.increment(MetricName.FeedReadEmpty, DefaultMetricTags(stage = Stage.FeedServing))
        }
        val durationMs = dependencies.clock.now().epochMillis - startedAt.epochMillis
        dependencies.metrics.recordDistribution(
            MetricName.FeedLatency,
            durationMs,
            DefaultMetricTags(stage = Stage.FeedServing, outcome = Outcome.Succeeded, freshness = FeedFreshness.FRESH)
        )
        dependencies.traceSink.record(
            StructuredLogEvent(
                traceId = trace.traceId.value,
                requestId = trace.requestId.value,
                authorId = viewerId.value,
                event = "feed_query_served",
                stage = Stage.FeedServing.name,
                result = Outcome.Succeeded.name,
                durationMs = durationMs
            )
        )
        return DomainResult.success(Page(items = items, nextCursor = page.nextCursor))
    }

    private suspend fun collectCandidateWindow(viewerId: UserId, trace: TraceContext): List<FeedCandidate> {
        val collected = mutableListOf<FeedCandidate>()
        var cursor: Cursor? = null
        var remaining = config.candidateWindowSize
        do {
            val page = dependencies.feedInbox.listCandidates(
                viewerId = viewerId,
                pageRequest = PageRequest(limit = remaining.coerceAtMost(PageRequestMaxLimit), cursor = cursor),
            )
            collected += page.items
            remaining -= page.items.size
            cursor = page.nextCursor
        } while (cursor != null && remaining > 0)

        val deduped = collected.deduplicateByPost()
        dependencies.metrics.increment("feed.candidates.scanned", trace.tags(), collected.size.toLong())
        if (deduped.size != collected.size) {
            dependencies.metrics.increment(
                "feed.eligibility.rejected",
                trace.tags() + mapOf("reason" to FeedEligibilityRejectionReason.Duplicate.name),
                (collected.size - deduped.size).toLong(),
            )
        }
        return deduped
    }

    private suspend fun filterEligibleCandidates(
        viewerId: UserId,
        candidates: List<FeedCandidate>,
        snapshots: Map<PostId, FeedPostSnapshot>,
        trace: TraceContext,
    ): List<FeedCandidate> {
        val eligible = mutableListOf<FeedCandidate>()
        for (candidate in candidates) {
            val snapshot = snapshots[candidate.postId]
            if (snapshot == null) {
                dependencies.metrics.increment(
                    "feed.eligibility.rejected",
                    trace.tags() + mapOf("reason" to FeedEligibilityRejectionReason.NotPublished.name),
                )
                continue
            }
            val decision = eligibility.evaluateServingCandidate(viewerId, snapshot)
            if (decision.accepted) {
                eligible += candidate
            } else {
                dependencies.metrics.increment(
                    "feed.eligibility.rejected",
                    trace.tags() + mapOf("reason" to decision.rejectionReason?.name.orEmpty()),
                )
            }
        }
        return eligible
    }

    private suspend fun filterAuthorFatigue(
        ranked: List<RankedFeedCandidate>,
        trace: TraceContext,
    ): List<RankedFeedCandidate> {
        val counts = mutableMapOf<UserId, Int>()
        val filtered = ranked.filter { candidate ->
            val count = counts.getOrDefault(candidate.candidate.authorId, 0) + 1
            counts[candidate.candidate.authorId] = count
            val keep = count <= config.maxAuthorItemsPerRankedWindow
            if (!keep) {
                dependencies.metrics.increment(
                    "feed.eligibility.rejected",
                    trace.tags() + mapOf("reason" to FeedEligibilityRejectionReason.Fatigue.name),
                )
            }
            keep
        }
        return filtered
    }

    private suspend fun hydrateRankedPage(
        viewerId: UserId,
        rankedPage: List<RankedFeedCandidate>,
        trace: TraceContext,
        servedFromCache: Boolean,
    ): List<FeedItem> {
        val snapshots = dependencies.feedPosts.getMany(rankedPage.map { it.candidate.postId })
        val items = rankedPage.mapNotNull { rankedCandidate ->
            val snapshot = snapshots[rankedCandidate.candidate.postId] ?: return@mapNotNull null
            if (!eligibility.canServeCandidate(viewerId, snapshot)) return@mapNotNull null
            FeedItem(
                postId = snapshot.postId,
                authorId = snapshot.authorId,
                text = snapshot.text,
                mediaAttachments = snapshot.mediaAttachments,
                score = rankedCandidate.score,
                source = rankedCandidate.candidate.source,
                origin = rankedCandidate.candidate.origin,
                distributionMode = rankedCandidate.candidate.distributionMode,
                createdAt = snapshot.createdAt,
                trace = rankedCandidate.candidate.trace,
                rankingModelVersion = rankedCandidate.modelVersion,
                scoreKey = rankedCandidate.scoreKey,
                servedFromCache = servedFromCache,
            )
        }
        dependencies.metrics.increment(
            "feed.posts.rehydrated",
            trace.tags() + mapOf("cache" to servedFromCache.toString()),
            items.size.toLong(),
        )
        return items
    }

    private companion object {
        const val PageRequestMaxLimit = 200
    }
}

data class FeedServingConfig(
    val candidateWindowSize: Int = 500,
    val maxAuthorItemsPerRankedWindow: Int = 3,
    val cacheTtlMillis: Long = 30_000,
) {
    init {
        require(candidateWindowSize in 1..5_000) { "candidateWindowSize must be between 1 and 5000." }
        require(maxAuthorItemsPerRankedWindow in 1..50) { "maxAuthorItemsPerRankedWindow must be between 1 and 50." }
        require(cacheTtlMillis >= 0) { "cacheTtlMillis cannot be negative." }
    }
}

class PublishingCommandBus(
    private val publishPost: PublishPostUseCase,
    private val editPost: EditPostUseCase,
    private val deletePost: DeletePostUseCase,
) {
    suspend fun execute(command: CreatePostCommand): DomainResult<PublishPostReceipt> =
        publishPost.execute(command)

    suspend fun edit(command: EditPostCommand): DomainResult<PostMutationReceipt> =
        editPost.execute(command)

    suspend fun delete(command: DeletePostCommand): DomainResult<PostMutationReceipt> =
        deletePost.execute(command)

    suspend fun execute(command: PublishingCommand): DomainResult<PublishingCommandReceipt> =
        when (command) {
            is CreatePostCommand -> publishPost.execute(command)
            is EditPostCommand -> editPost.execute(command)
            is DeletePostCommand -> deletePost.execute(command)
        }
}

class PublishPostUseCase(
    private val handler: PublishPostCommandHandler,
) {
    suspend fun execute(command: CreatePostCommand): DomainResult<PublishPostReceipt> =
        handler.handle(command)
}

class EditPostUseCase(
    private val handler: EditPostCommandHandler,
) {
    suspend fun execute(command: EditPostCommand): DomainResult<PostMutationReceipt> =
        handler.handle(command)
}

class DeletePostUseCase(
    private val handler: DeletePostCommandHandler,
) {
    suspend fun execute(command: DeletePostCommand): DomainResult<PostMutationReceipt> =
        handler.handle(command)
}

private data class RankedCursor(
    val scoreKey: Long,
    val createdAtMillis: Long,
    val postId: String,
)

private fun List<RankedFeedCandidate>.pageRanked(pageRequest: PageRequest): Page<RankedFeedCandidate> {
    val cursor = pageRequest.cursor?.decodeRankedCursor()
    val filtered = cursor?.let { rankedCursor ->
        filter { it.isAfter(rankedCursor) }
    } ?: this
    val items = filtered.take(pageRequest.limit)
    val nextCursor = if (filtered.size > pageRequest.limit) {
        items.lastOrNull()?.toCursor()
    } else {
        null
    }
    return Page(items = items, nextCursor = nextCursor)
}

private fun RankedFeedCandidate.isAfter(cursor: RankedCursor): Boolean =
    scoreKey < cursor.scoreKey ||
        (scoreKey == cursor.scoreKey && candidate.postCreatedAt.epochMillis < cursor.createdAtMillis) ||
        (scoreKey == cursor.scoreKey && candidate.postCreatedAt.epochMillis == cursor.createdAtMillis && candidate.postId.value > cursor.postId)

private fun RankedFeedCandidate.toCursor(): Cursor =
    Cursor("${scoreKey}|${candidate.postCreatedAt.epochMillis}|${candidate.postId.value}")

private fun Cursor.decodeRankedCursor(): RankedCursor? {
    val parts = value.split("|")
    if (parts.size != 3) return null
    return RankedCursor(
        scoreKey = parts[0].toLongOrNull() ?: return null,
        createdAtMillis = parts[1].toLongOrNull() ?: return null,
        postId = parts[2].takeIf { it.isNotBlank() } ?: return null,
    )
}

private fun List<FeedCandidate>.deduplicateByPost(): List<FeedCandidate> =
    groupBy { it.postId }
        .values
        .map { candidates ->
            candidates.minWith(
                compareBy<FeedCandidate> { it.source.priority() }
                    .thenBy { it.generatedAt.epochMillis }
                    .thenBy { it.id.value },
            )
        }
        .sortedWith(
            compareByDescending<FeedCandidate> { it.postCreatedAt.epochMillis }
                .thenBy { it.postId.value },
        )

private fun CandidateSource.priority(): Int =
    when (this) {
        CandidateSource.FanoutOnWrite -> 0
        CandidateSource.FanoutOnRead -> 1
        CandidateSource.Recommendation -> 2
        CandidateSource.Exploration -> 3
    }

private fun RankedFeedCacheRecord.isFresh(now: com.example.mindos.features.social.domain.valueobject.Timestamp, config: FeedServingConfig): Boolean =
    now.epochMillis - generatedAt.epochMillis <= config.cacheTtlMillis &&
        candidateWindowSize == config.candidateWindowSize

class FeedMergeService {
    fun mergePendingWithRemote(
        pendingLocal: List<PendingLocalFeedItem>,
        remote: Page<FeedItem>,
    ): Page<MergedFeedItem> {
        val remoteByPostId = remote.items.associateBy { it.postId }
        val pendingOnly = pendingLocal
            .filterNot { remoteByPostId.containsKey(it.postId) }
            .map { pending ->
                MergedFeedItem(
                    postId = pending.postId,
                    authorId = pending.authorId,
                    text = pending.text,
                    mediaAttachments = pending.mediaAttachments,
                    createdAt = pending.acceptedAt,
                    state = MergedFeedItemState.LocalPending,
                    local = pending,
                )
            }
        val remoteItems = remote.items.map { item ->
            MergedFeedItem(
                postId = item.postId,
                authorId = item.authorId,
                text = item.text,
                mediaAttachments = item.mediaAttachments,
                createdAt = item.createdAt,
                state = MergedFeedItemState.RemoteConfirmed,
                remote = item,
                local = pendingLocal.firstOrNull { it.postId == item.postId },
            )
        }
        return Page(
            items = (pendingOnly + remoteItems)
                .distinctBy { it.postId }
                .sortedWith(compareByDescending<MergedFeedItem> { it.createdAt.epochMillis }.thenBy { it.postId.value }),
            nextCursor = remote.nextCursor,
        )
    }
}
