package com.mindos.action.execution

import com.mindos.action.model.ReferenceKind
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.validation.ValidationFailure

/** Why execution failed after passing validation. */
sealed interface ExecutionFailure {
    val detail: String

    data class ResolutionFailed(
        val kind: ReferenceKind,
        val rawValue: String,
        override val detail: String,
    ) : ExecutionFailure

    data class NavigationRejected(override val detail: String) : ExecutionFailure
    data class NavigationFailed(override val detail: String) : ExecutionFailure
    data class UnsupportedAction(val actionType: String, override val detail: String) : ExecutionFailure
}

/** Typed outcome of executing an action. Never an exception for expected paths. */
sealed interface ExecutionResult {
    /** Validation passed, references resolved, navigation performed. */
    data class Executed(val command: MindNavigationCommand) : ExecutionResult

    /** Validation failed — nothing was resolved or navigated. */
    data class Rejected(val failures: List<ValidationFailure>) : ExecutionResult

    /** Failed after validation (resolution or navigation). */
    data class Failed(val failure: ExecutionFailure) : ExecutionResult
}
