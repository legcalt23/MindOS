package com.example.neuv3.features.social.presentation.publisher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.neuv3.features.social.domain.error.ActionNotAuthorized
import com.example.neuv3.features.social.domain.error.DomainError
import com.example.neuv3.features.social.domain.error.InvalidValue
import com.example.neuv3.features.social.domain.error.UserNotFound
import com.example.neuv3.features.social.domain.publishing.CreatePostCommand
import com.example.neuv3.features.social.domain.publishing.DraftMediaAttachment
import com.example.neuv3.features.social.domain.publishing.GetFeedUseCase
import com.example.neuv3.features.social.domain.publishing.IdempotencyKey
import com.example.neuv3.features.social.domain.publishing.LocalEchoPublishingMediaUploader
import com.example.neuv3.features.social.domain.publishing.PostId
import com.example.neuv3.features.social.domain.publishing.PostMediaAttachment
import com.example.neuv3.features.social.domain.publishing.PostText
import com.example.neuv3.features.social.domain.publishing.PostVisibility
import com.example.neuv3.features.social.domain.publishing.PublicationReconciliationService
import com.example.neuv3.features.social.domain.publishing.PublishPostReceipt
import com.example.neuv3.features.social.domain.publishing.PublisherState
import com.example.neuv3.features.social.domain.publishing.PublisherStateMachine
import com.example.neuv3.features.social.domain.publishing.PublishingCommandBus
import com.example.neuv3.features.social.domain.publishing.PublishingDependencies
import com.example.neuv3.features.social.domain.publishing.PublishingMediaUploader
import com.example.neuv3.features.social.domain.publishing.TraceContext
import com.example.neuv3.features.social.domain.publishing.TraceId
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.Timestamp
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.PageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class PublisherOptimisticPost(
    val postId: PostId,
    val body: String,
    val mediaAttachments: List<PostMediaAttachment>,
    val acceptedAt: Timestamp,
    val trace: TraceContext,
    val authorOnly: Boolean = true,
    val visibleInFeed: Boolean = false,
)

data class PublisherUiState(
    val state: PublisherState = PublisherState.Idle,
    val body: String = "",
    val visibility: PostVisibility = PostVisibility.Public,
    val canPublish: Boolean = false,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val requestId: String? = null,
    val traceId: String? = null,
    val idempotencyKey: String? = null,
    val postId: String? = null,
    val pendingMediaCount: Int = 0,
    val uploadedMediaCount: Int = 0,
    val optimisticPosts: List<PublisherOptimisticPost> = emptyList(),
)

@Stable
class PublisherStateHolder(
    private val actorId: UserId,
    private val commandBus: PublishingCommandBus,
    private val feedQuery: GetFeedUseCase,
    private val reconciliation: PublicationReconciliationService,
    private val dependencies: PublishingDependencies,
    private val localStore: PublisherLocalDraftStore,
    private val mediaUploader: PublishingMediaUploader,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(PublisherUiState())
        private set

    private val publishMutex = Mutex()
    private var currentSnapshot: PublisherDraftSnapshot? = null

    suspend fun restore(defaultBody: String = "") {
        val saved = localStore.load(actorId)
        val snapshot = saved ?: defaultBody.takeIf { it.isNotBlank() }?.let { body ->
            createSnapshot(body = body, state = PublisherState.EditingDraft).also { localStore.save(it) }
        }

        if (snapshot == null) {
            currentSnapshot = null
            uiState = PublisherUiState()
            return
        }

        currentSnapshot = snapshot
        uiState = snapshot.toUiState()

        if (snapshot.postId != null && snapshot.state.requiresRemoteReconcile()) {
            reconcileAccepted(snapshot)
        }
    }

    fun updateBody(body: String) {
        uiState = uiState.copy(
            state = PublisherState.EditingDraft,
            body = body,
            canPublish = body.isNotBlank(),
            isBusy = false,
            feedbackMessage = "Borrador guardado.",
            errorMessage = null,
        )
        scope.launch {
            val snapshot = ensureSnapshot(body = body)
            saveSnapshot(
                snapshot.copy(
                    body = body,
                    state = PublisherState.EditingDraft,
                    updatedAt = dependencies.clock.now(),
                    lastError = null,
                ),
            )
            recordLocalTrace("publisher.draft.autosaved", snapshot.traceContext())
        }
    }

    fun setVisibility(visibility: PostVisibility) {
        uiState = uiState.copy(visibility = visibility)
        scope.launch {
            val snapshot = ensureSnapshot(body = uiState.body)
            saveSnapshot(
                snapshot.copy(
                    visibility = visibility,
                    state = PublisherState.EditingDraft,
                    updatedAt = dependencies.clock.now(),
                ),
            )
        }
    }

    fun attachMedia(media: DraftMediaAttachment) {
        scope.launch {
            val snapshot = ensureSnapshot(body = uiState.body)
            val nextMedia = (snapshot.pendingMedia.filterNot { it.clientMediaId == media.clientMediaId } + media)
            saveSnapshot(
                snapshot.copy(
                    pendingMedia = nextMedia,
                    state = PublisherState.EditingDraft,
                    updatedAt = dependencies.clock.now(),
                    lastError = null,
                ),
                feedback = "Media lista para subir.",
            )
        }
    }

    suspend fun publish() {
        if (!publishMutex.tryLock()) {
            currentSnapshot?.let { snapshot ->
                recordLocalTrace("publisher.publish.tap_ignored_in_flight", snapshot.traceContext())
            }
            return
        }

        try {
            var snapshot = ensureSnapshot(body = uiState.body)
            if (snapshot.postId != null && snapshot.state.requiresRemoteReconcile()) {
                reconcileAccepted(snapshot)
                return
            }
            if (!PublisherStateMachine.acceptsPublishTap(snapshot.state)) {
                recordLocalTrace("publisher.publish.tap_ignored_state", snapshot.traceContext(), mapOf("state" to snapshot.state.name))
                return
            }
            if (snapshot.state == PublisherState.Idle || snapshot.state == PublisherState.FailedTerminal) {
                snapshot = move(snapshot, PublisherState.EditingDraft)
            }

            snapshot = move(snapshot, PublisherState.Validating)
            when (val preflight = preflight(snapshot)) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> {
                    fail(snapshot, preflight.error)
                    return
                }
            }

            snapshot = uploadMediaIfNeeded(snapshot)
                ?: return

            snapshot = move(snapshot, PublisherState.PublishingCommandSent)
            snapshot = move(snapshot, PublisherState.AwaitingCommit)
            val command = CreatePostCommand(
                actorId = actorId,
                body = snapshot.body,
                visibility = snapshot.visibility,
                mediaAttachments = snapshot.uploadedMedia,
                idempotencyKey = snapshot.idempotencyKey,
                trace = snapshot.traceContext(),
            )

            when (val result = commandBus.execute(command)) {
                is DomainResult.Success -> accept(snapshot, result.value)
                is DomainResult.Failure -> fail(snapshot, result.error)
            }
        } finally {
            publishMutex.unlock()
        }
    }

    suspend fun retry() {
        publish()
    }

    suspend fun reconcile() {
        currentSnapshot?.let { snapshot ->
            if (snapshot.postId != null) {
                reconcileAccepted(snapshot)
            }
        }
    }

    private suspend fun accept(snapshot: PublisherDraftSnapshot, receipt: PublishPostReceipt) {
        val accepted = snapshot.copy(
            state = PublisherState.PublishedAccepted,
            postId = receipt.postId,
            acceptedAt = receipt.acceptedAt,
            updatedAt = dependencies.clock.now(),
            lastError = null,
        )
        currentSnapshot = accepted
        localStore.clear(actorId)
        uiState = accepted.toUiState(feedback = "Publicación aceptada. El feed se está sincronizando.")
        recordLocalTrace("publisher.commit.accepted", accepted.traceContext(receipt.postId))
        reconcileAccepted(accepted)
    }

    private suspend fun reconcileAccepted(snapshot: PublisherDraftSnapshot) {
        var working = snapshot
        if (working.state != PublisherState.FanoutInProgress) {
            working = move(working, PublisherState.FanoutInProgress, feedback = "Publicación aceptada. El feed se está sincronizando.")
        }

        when (val report = reconciliation.reconcilePending()) {
            is DomainResult.Success -> {
                recordLocalTrace(
                    span = "publisher.reconcile.completed",
                    trace = working.traceContext(working.postId),
                    attributes = mapOf(
                        "dispatched" to report.value.dispatchedOutboxMessages.toString(),
                        "repaired" to report.value.repairedOutboxMessages.toString(),
                        "stillPending" to report.value.stillPendingPosts.toString(),
                    ),
                )
            }
            is DomainResult.Failure -> {
                move(
                    snapshot = working,
                    target = PublisherState.PartialDegradation,
                    feedback = "Publicación aceptada. Feed pendiente.",
                    error = report.error.message,
                )
                return
            }
        }

        val postId = working.postId
        if (postId == null) {
            move(working, PublisherState.PartialDegradation, feedback = "Publicación aceptada. Feed pendiente.")
            return
        }

        when (
            val feed = feedQuery.getFeed(
                viewerId = actorId,
                pageRequest = PageRequest(limit = 50),
                trace = dependencies.traceContexts.nextTraceContext("publisher_feed_reconcile")
                    .withIdempotencyKey(working.idempotencyKey)
                    .withPostId(postId),
            )
        ) {
            is DomainResult.Success -> {
                if (feed.value.items.any { it.postId == postId }) {
                    val visible = move(working, PublisherState.FeedVisible, feedback = "Publicado.")
                    localStore.clear(actorId)
                    currentSnapshot = null
                    uiState = visible.toUiState(feedback = "Publicado.").copy(body = "", canPublish = false)
                } else {
                    move(working, PublisherState.PartialDegradation, feedback = "Publicación aceptada. Feed pendiente.")
                }
            }
            is DomainResult.Failure -> {
                move(
                    snapshot = working,
                    target = PublisherState.PartialDegradation,
                    feedback = "Publicación aceptada. Feed pendiente.",
                    error = feed.error.message,
                )
            }
        }
    }

    private suspend fun preflight(snapshot: PublisherDraftSnapshot): DomainResult<PostText> {
        val trace = snapshot.traceContext()
        recordLocalTrace("publisher.preflight.started", trace)
        val account = dependencies.profiles.getAccount(actorId)
            ?: return DomainResult.failure(UserNotFound(actorId))
        if (!account.canParticipateInSocialGraph) {
            return DomainResult.failure(ActionNotAuthorized("User ${actorId.value} cannot publish."))
        }
        if (snapshot.pendingMedia.size + snapshot.uploadedMedia.size > MaxMediaAttachments) {
            return DomainResult.failure(InvalidValue("media", "must contain $MaxMediaAttachments items or less"))
        }
        val totalPendingBytes = snapshot.pendingMedia.sumOf { it.sizeBytes }
        if (totalPendingBytes > MaxPendingMediaBytes) {
            return DomainResult.failure(InvalidValue("media", "pending upload is too large"))
        }
        val text = when (val parsed = PostText.from(snapshot.body)) {
            is DomainResult.Success -> parsed
            is DomainResult.Failure -> return parsed
        }
        recordLocalTrace(
            span = "publisher.preflight.completed",
            trace = trace,
            attributes = mapOf(
                "pendingMedia" to snapshot.pendingMedia.size.toString(),
                "uploadedMedia" to snapshot.uploadedMedia.size.toString(),
            ),
        )
        return text
    }

    private suspend fun uploadMediaIfNeeded(snapshot: PublisherDraftSnapshot): PublisherDraftSnapshot? {
        if (snapshot.pendingMedia.isEmpty()) {
            return move(snapshot, PublisherState.ReadyToPublish)
        }

        val uploading = move(snapshot, PublisherState.UploadingMedia)
        recordLocalTrace("publisher.media.upload.started", uploading.traceContext(), mapOf("count" to uploading.pendingMedia.size.toString()))
        return when (
            val result = mediaUploader.upload(
                actorId = actorId,
                draftId = uploading.draftId,
                idempotencyKey = uploading.idempotencyKey,
                media = uploading.pendingMedia,
                trace = uploading.traceContext(),
            )
        ) {
            is DomainResult.Success -> {
                val ready = uploading.copy(
                    pendingMedia = emptyList(),
                    uploadedMedia = (uploading.uploadedMedia + result.value).distinctBy { it.clientMediaId },
                    updatedAt = dependencies.clock.now(),
                    lastError = null,
                )
                recordLocalTrace("publisher.media.upload.completed", ready.traceContext(), mapOf("count" to result.value.size.toString()))
                move(ready, PublisherState.ReadyToPublish)
            }
            is DomainResult.Failure -> {
                fail(uploading, result.error)
                null
            }
        }
    }

    private suspend fun fail(snapshot: PublisherDraftSnapshot, error: DomainError) {
        val target = if (error.isTerminalPublishError()) {
            PublisherState.FailedTerminal
        } else {
            PublisherState.FailedRecoverable
        }
        move(
            snapshot = snapshot,
            target = target,
            feedback = if (target == PublisherState.FailedRecoverable) "Reintentar es seguro." else null,
            error = error.message,
        )
        recordLocalTrace(
            span = "publisher.publish.failed",
            trace = snapshot.traceContext(snapshot.postId),
            attributes = mapOf("code" to error.code, "recoverable" to (target == PublisherState.FailedRecoverable).toString()),
        )
    }

    private suspend fun ensureSnapshot(body: String): PublisherDraftSnapshot {
        currentSnapshot?.let { current ->
            if (current.actorId == actorId) return current.copy(body = body, updatedAt = dependencies.clock.now())
        }
        localStore.load(actorId)?.let { persisted ->
            currentSnapshot = persisted
            return persisted.copy(body = body.ifBlank { persisted.body }, updatedAt = dependencies.clock.now())
        }
        return createSnapshot(body = body, state = if (body.isBlank()) PublisherState.Idle else PublisherState.EditingDraft)
            .also {
                currentSnapshot = it
                localStore.save(it)
            }
    }

    private fun createSnapshot(body: String, state: PublisherState): PublisherDraftSnapshot {
        val key = dependencies.idempotencyKeys.nextIdempotencyKey()
        val trace = dependencies.traceContexts.nextTraceContext("publisher_draft").withIdempotencyKey(key)
        val now = dependencies.clock.now()
        return PublisherDraftSnapshot(
            actorId = actorId,
            draftId = "draft_${key.value}",
            body = body,
            visibility = PostVisibility.Public,
            idempotencyKey = key,
            traceId = trace.traceId,
            requestId = trace.requestId,
            state = state,
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun move(
        snapshot: PublisherDraftSnapshot,
        target: PublisherState,
        feedback: String? = null,
        error: String? = null,
    ): PublisherDraftSnapshot {
        val state = when (val transition = PublisherStateMachine.transition(snapshot.state, target)) {
            is DomainResult.Success -> transition.value
            is DomainResult.Failure -> {
                recordLocalTrace(
                    span = "publisher.state.invalid_transition",
                    trace = snapshot.traceContext(snapshot.postId),
                    attributes = mapOf("from" to snapshot.state.name, "to" to target.name),
                )
                target
            }
        }
        val next = snapshot.copy(
            state = state,
            updatedAt = dependencies.clock.now(),
            lastError = error,
            attemptCount = if (target == PublisherState.AwaitingCommit) snapshot.attemptCount + 1 else snapshot.attemptCount,
        )
        saveSnapshot(next, feedback = feedback, error = error)
        recordLocalTrace(
            span = "publisher.state.changed",
            trace = next.traceContext(next.postId),
            attributes = mapOf("from" to snapshot.state.name, "to" to state.name),
        )
        return next
    }

    private suspend fun saveSnapshot(
        snapshot: PublisherDraftSnapshot,
        feedback: String? = null,
        error: String? = null,
    ) {
        currentSnapshot = snapshot
        localStore.save(snapshot)
        uiState = snapshot.toUiState(feedback = feedback, error = error)
    }

    private suspend fun recordLocalTrace(
        span: String,
        trace: TraceContext,
        attributes: Map<String, String> = emptyMap(),
    ) {
        dependencies.traceSink.record(
            com.example.neuv3.features.social.domain.publishing.StructuredLogEvent(
                traceId = trace.traceId.value,
                requestId = trace.requestId.value,
                postId = trace.postId?.value,
                authorId = actorId.value,
                event = span,
                stage = com.example.neuv3.features.social.domain.publishing.Stage.Publishing.name,
                result = attributes["state"] ?: attributes["to"],
                errorCode = attributes["code"]
            )
        )
    }

    private fun PublisherDraftSnapshot.traceContext(postId: PostId? = this.postId): TraceContext =
        TraceContext(
            traceId = traceId,
            requestId = requestId,
            postId = postId,
            idempotencyKey = idempotencyKey,
        )

    private fun PublisherDraftSnapshot.toUiState(
        feedback: String? = null,
        error: String? = lastError,
    ): PublisherUiState {
        val acceptedAt = acceptedAt
        val optimistic = if (postId != null && acceptedAt != null) {
            listOf(
                PublisherOptimisticPost(
                    postId = postId,
                    body = body,
                    mediaAttachments = uploadedMedia,
                    acceptedAt = acceptedAt,
                    trace = traceContext(postId),
                    visibleInFeed = state == PublisherState.FeedVisible,
                ),
            )
        } else {
            emptyList()
        }
        return PublisherUiState(
            state = state,
            body = body,
            visibility = visibility,
            canPublish = body.isNotBlank() && PublisherStateMachine.acceptsPublishTap(state),
            isBusy = state.isBusy(),
            feedbackMessage = feedback ?: state.defaultFeedback(),
            errorMessage = error,
            requestId = requestId.value,
            traceId = traceId.value,
            idempotencyKey = idempotencyKey.value,
            postId = postId?.value,
            pendingMediaCount = pendingMedia.size,
            uploadedMediaCount = uploadedMedia.size,
            optimisticPosts = optimistic,
        )
    }

    private fun PublisherState.defaultFeedback(): String? =
        when (this) {
            PublisherState.Idle -> null
            PublisherState.EditingDraft -> "Borrador guardado."
            PublisherState.Validating -> "Validando publicación."
            PublisherState.UploadingMedia -> "Subiendo media."
            PublisherState.ReadyToPublish -> "Lista para publicar."
            PublisherState.PublishingCommandSent,
            PublisherState.AwaitingCommit,
            -> "Enviando publicación."
            PublisherState.PublishedAccepted,
            PublisherState.FanoutInProgress,
            -> "Publicación aceptada. El feed se está sincronizando."
            PublisherState.FeedVisible -> "Publicado."
            PublisherState.PartialDegradation -> "Publicación aceptada. Feed pendiente."
            PublisherState.FailedRecoverable -> "Reintentar es seguro."
            PublisherState.FailedTerminal -> null
        }

    private fun PublisherState.isBusy(): Boolean =
        this == PublisherState.Validating ||
            this == PublisherState.UploadingMedia ||
            this == PublisherState.PublishingCommandSent ||
            this == PublisherState.AwaitingCommit ||
            this == PublisherState.FanoutInProgress

    private fun PublisherState.requiresRemoteReconcile(): Boolean =
        this == PublisherState.PublishedAccepted ||
            this == PublisherState.FanoutInProgress ||
            this == PublisherState.PartialDegradation ||
            this == PublisherState.FailedRecoverable

    private fun DomainError.isTerminalPublishError(): Boolean =
        this is InvalidValue || this is UserNotFound || this is ActionNotAuthorized

    companion object {
        private const val MaxMediaAttachments = 4
        private const val MaxPendingMediaBytes = 60L * 1024L * 1024L
    }
}

@Composable
fun rememberPublisherStateHolder(
    actorId: UserId,
    commandBus: PublishingCommandBus,
    feedQuery: GetFeedUseCase,
    reconciliation: PublicationReconciliationService,
    dependencies: PublishingDependencies,
    localStore: PublisherLocalDraftStore,
    scope: CoroutineScope,
    mediaUploader: PublishingMediaUploader? = null,
): PublisherStateHolder {
    val resolvedMediaUploader = mediaUploader ?: remember(dependencies.clock) {
        LocalEchoPublishingMediaUploader(dependencies.clock)
    }
    return remember(actorId, commandBus, feedQuery, reconciliation, dependencies, localStore, resolvedMediaUploader, scope) {
        PublisherStateHolder(
            actorId = actorId,
            commandBus = commandBus,
            feedQuery = feedQuery,
            reconciliation = reconciliation,
            dependencies = dependencies,
            localStore = localStore,
            mediaUploader = resolvedMediaUploader,
            scope = scope,
        )
    }
}
