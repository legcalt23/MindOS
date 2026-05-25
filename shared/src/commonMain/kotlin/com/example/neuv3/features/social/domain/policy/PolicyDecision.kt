package com.example.neuv3.features.social.domain.policy

import com.example.neuv3.features.social.domain.error.DomainError
import com.example.neuv3.features.social.shared.util.DomainResult

sealed interface PolicyDecision<out T> {
    data class Allowed<T>(val value: T) : PolicyDecision<T>
    data class Denied(val error: DomainError) : PolicyDecision<Nothing>
}

fun <T> PolicyDecision<T>.toDomainResult(): DomainResult<T> =
    when (this) {
        is PolicyDecision.Allowed -> DomainResult.success(value)
        is PolicyDecision.Denied -> DomainResult.failure(error)
    }

