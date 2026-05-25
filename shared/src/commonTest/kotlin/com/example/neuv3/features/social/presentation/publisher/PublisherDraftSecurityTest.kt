package com.example.neuv3.features.social.presentation.publisher

import com.example.neuv3.features.social.data.local.InMemorySocialGraphComponent
import com.example.neuv3.features.social.data.publishing.InMemoryPublishingComponent
import com.example.neuv3.features.social.domain.publishing.IdempotencyKey
import com.example.neuv3.features.social.domain.publishing.LocalEchoPublishingMediaUploader
import com.example.neuv3.features.social.domain.publishing.PostVisibility
import com.example.neuv3.features.social.domain.publishing.PublisherState
import com.example.neuv3.features.social.domain.publishing.TraceId
import com.example.neuv3.features.social.domain.valueobject.DisplayName
import com.example.neuv3.features.social.domain.valueobject.Handle
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.Timestamp
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.getOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PublisherDraftSecurityTest {
    private val author = UserId("author")

    @Test
    fun persistedDraftOmitsSensitiveLegacyFields() {
        val snapshot = PublisherDraftSnapshot(
            actorId = author,
            draftId = "draft_id",
            body = "borrador",
            visibility = PostVisibility.Followers,
            idempotencyKey = IdempotencyKey("idem-123"),
            traceId = TraceId("trace-123"),
            requestId = RequestId("request-123"),
            state = PublisherState.PartialDegradation,
            createdAt = Timestamp(1000),
            updatedAt = Timestamp(2000),
            lastError = "stack trace",
            attemptCount = 4,
        )

        val persisted = snapshot.toPersistedString()

        assertTrue(persisted.contains("schemaVersion=2"))
        assertFalse(persisted.contains("traceId"))
        assertFalse(persisted.contains("requestId"))
        assertFalse(persisted.contains("lastError"))
        assertFalse(persisted.contains("uploadedMedia"))
        assertFalse(persisted.contains("postId"))

        val restored = assertNotNull(publisherDraftSnapshotFromPersistedString(persisted))
        assertEquals(PublisherState.EditingDraft, restored.state)
        assertEquals("draft_idem-123", restored.draftId)
        assertEquals("draft-idem-123", restored.traceId.value)
        assertEquals("draft-idem-123", restored.requestId.value)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun successfulPublishClearsPersistedDraft() = runTest {
        val graph = InMemorySocialGraphComponent.create().also { seed(it, author) }
        val publishing = InMemoryPublishingComponent.create(
            socialGraphDatabase = graph.database,
            socialGraphDependencies = graph.dependencies,
        )
        val store = InMemoryPublisherLocalDraftStore()
        val holder = PublisherStateHolder(
            actorId = author,
            commandBus = publishing.commandBus,
            feedQuery = publishing.feedQuery,
            reconciliation = publishing.reconciliation,
            dependencies = publishing.dependencies,
            localStore = store,
            mediaUploader = LocalEchoPublishingMediaUploader(publishing.dependencies.clock),
            scope = CoroutineScope(coroutineContext),
        )

        holder.updateBody("Pulso seguro")
        runCurrent()
        assertNotNull(store.load(author))

        holder.publish()

        assertNull(store.load(author))
        assertEquals("", holder.uiState.body)
    }

    private suspend fun seed(graph: InMemorySocialGraphComponent, userId: UserId) {
        graph.seedUser(
            userId = userId,
            handle = Handle.from(userId.value).getOrThrow(),
            displayName = DisplayName.from(userId.value.replaceFirstChar { it.uppercase() }).getOrThrow(),
        )
    }
}
