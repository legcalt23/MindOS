package com.mindos.app.features.search.domain

interface UsernameSearchRepository {
    suspend fun searchByUsername(
        query: String,
        limit: Int = UsernameSearchMaxResults,
    ): List<UsernameSearchUser>
}
