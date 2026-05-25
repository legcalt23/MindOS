package com.example.neuv3.features.search.domain

const val UsernameSearchMinQueryLength: Int = 3
const val UsernameSearchMaxResults: Int = 12

data class UsernameSearchUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val photoStoragePath: String? = null,
    val photoThumbnailStoragePath: String? = null,
    val isVideo: Boolean = false,
    val matchScore: Float = 0f,
)

fun normalizeUsernameSearchQuery(input: String): String =
    input
        .trim()
        .removePrefix("@")
        .lowercase()
        .filter { char -> char.isLetterOrDigit() || char == '.' || char == '_' }
        .take(30)

fun usernameMatchScore(query: String, username: String): Float {
    val normalizedQuery = normalizeUsernameSearchQuery(query)
    val normalizedUsername = normalizeUsernameSearchQuery(username)
    if (normalizedQuery.isBlank() || normalizedUsername.isBlank()) return 0f

    val coverage = normalizedQuery.length.toFloat() / normalizedUsername.length.coerceAtLeast(1)
    return when {
        normalizedUsername == normalizedQuery -> 1f
        normalizedUsername.startsWith(normalizedQuery) -> 0.72f + (coverage * 0.28f)
        normalizedUsername.contains(normalizedQuery) -> 0.42f + (coverage * 0.18f)
        else -> 0f
    }.coerceIn(0f, 1f)
}
