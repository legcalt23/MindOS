package com.mindos.app.features.social.domain.feed

import com.mindos.app.features.social.domain.error.PersistenceFailure
import com.mindos.app.features.social.shared.util.DomainResult

class DisabledGlobalFeedRepository(
    private val reason: String,
) : GlobalFeedRepository {
    override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
        failure()

    override suspend fun loadPostsByUser(userId: String, limit: Int): DomainResult<List<GlobalFeedPost>> =
        failure()

    override suspend fun createPost(request: CreateGlobalFeedPostRequest): DomainResult<GlobalFeedPost> =
        failure()

    private fun <T> failure(): DomainResult<T> =
        DomainResult.failure(PersistenceFailure(reason))
}
