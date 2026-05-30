package com.example.mindos.features.search.domain

interface UsernameSearchRepository {
    suspend fun searchByUsername(
        query: String,
        limit: Int = UsernameSearchMaxResults,
    ): List<UsernameSearchUser>
}
