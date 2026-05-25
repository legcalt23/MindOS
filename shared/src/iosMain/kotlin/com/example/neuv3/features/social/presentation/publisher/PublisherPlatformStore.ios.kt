package com.example.neuv3.features.social.presentation.publisher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.neuv3.features.social.domain.valueobject.UserId

@Composable
actual fun rememberPublisherLocalDraftStore(namespace: String): PublisherLocalDraftStore =
    remember(namespace) {
        InMemoryPublisherLocalDraftStore()
    }

private class InMemoryPublisherLocalDraftStore : PublisherLocalDraftStore {
    private val drafts = mutableMapOf<UserId, PublisherDraftSnapshot>()

    override suspend fun load(actorId: UserId): PublisherDraftSnapshot? = drafts[actorId]

    override suspend fun save(snapshot: PublisherDraftSnapshot) {
        drafts[snapshot.actorId] = snapshot
    }

    override suspend fun clear(actorId: UserId) {
        drafts.remove(actorId)
    }
}
