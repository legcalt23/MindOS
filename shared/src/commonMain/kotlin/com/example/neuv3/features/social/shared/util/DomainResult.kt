package com.example.neuv3.features.social.shared.util

import com.example.neuv3.features.social.domain.error.DomainError
import com.example.neuv3.features.social.domain.valueobject.Cursor

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>

    companion object {
        fun <T> success(value: T): DomainResult<T> = Success(value)
        fun failure(error: DomainError): DomainResult<Nothing> = Failure(error)
    }
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }

inline fun <T, R> DomainResult<T>.flatMap(transform: (T) -> DomainResult<R>): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> transform(value)
        is DomainResult.Failure -> this
    }

suspend inline fun <T, R> DomainResult<T>.flatMapSuspend(
    crossinline transform: suspend (T) -> DomainResult<R>,
): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> transform(value)
        is DomainResult.Failure -> this
    }

fun <T> DomainResult<T>.getOrNull(): T? =
    when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> null
    }

fun <T> DomainResult<T>.getOrThrow(): T =
    when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> error("${error.code}: ${error.message}")
    }

data class PageRequest(
    val limit: Int = 50,
    val cursor: Cursor? = null,
) {
    init {
        require(limit in 1..200) { "Page limit must be between 1 and 200." }
    }
}

data class Page<T>(
    val items: List<T>,
    val nextCursor: Cursor? = null,
) {
    companion object {
        fun <T> empty(): Page<T> = Page(emptyList())
    }
}

inline fun <T, R> Page<T>.mapItems(transform: (T) -> R): Page<R> =
    Page(
        items = items.map(transform),
        nextCursor = nextCursor,
    )

