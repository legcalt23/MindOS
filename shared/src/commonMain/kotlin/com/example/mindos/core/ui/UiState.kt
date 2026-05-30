package com.example.mindos.core.ui

/**
 * Canonical state model for all screens and features in MindOS.
 *
 * Every screen/feature should map its internal state to this model
 * for consistent rendering of loading, error, empty, and success states.
 *
 * Usage:
 * ```
 * val state: UiState<ProfileData> = UiState.Loading
 * ```
 */
sealed interface UiState<out T> {

    /** Initial / idle state before any action has been taken. */
    data object Idle : UiState<Nothing>

    /** Data is being fetched or processed. */
    data object Loading : UiState<Nothing>

    /** Data loaded successfully. */
    data class Success<T>(val data: T) : UiState<T>

    /** Data loaded but the result set is empty. */
    data class Empty(val message: String = "") : UiState<Nothing>

    /** An error occurred. */
    data class Error(val error: UserFacingError) : UiState<Nothing>

    /** Device is offline and cached data is not available. */
    data object Offline : UiState<Nothing>

    /** User does not have permission to view this content. */
    data class PermissionDenied(val message: String = "") : UiState<Nothing>

    /** Session expired or user is not authenticated. */
    data object Unauthorized : UiState<Nothing>

    /** Data is being refreshed (pull-to-refresh). Previous data may still be visible. */
    data class Refreshing<T>(val previousData: T? = null) : UiState<T>

    /** A form submission or write operation is in progress. */
    data object Submitting : UiState<Nothing>
}

/**
 * Convenience extensions for UiState.
 */
val <T> UiState<T>.isLoading: Boolean
    get() = this is UiState.Loading || this is UiState.Submitting || this is UiState.Refreshing

val <T> UiState<T>.isError: Boolean
    get() = this is UiState.Error

val <T> UiState<T>.dataOrNull: T?
    get() = when (this) {
        is UiState.Success -> data
        is UiState.Refreshing -> previousData
        else -> null
    }
