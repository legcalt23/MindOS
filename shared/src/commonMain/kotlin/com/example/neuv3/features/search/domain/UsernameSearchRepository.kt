package com.example.neuv3.features.search.domain

interface UsernameSearchRepository {
    suspend fun searchByUsername(
        query: String,
        limit: Int = UsernameSearchMaxResults,
    ): List<UsernameSearchUser>
}
