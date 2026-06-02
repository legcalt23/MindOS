package com.mindos.action.navigation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
enum class NavigationOperation {
    PUSH_NEW,
    SINGLETON,
}

@Serializable
enum class StackOverflowPolicy {
    REJECT_WHEN_FULL,
    DROP_OLDEST_WHEN_FULL,
}

@Serializable
data class NavigationPolicy(
    val maxDepth: Int = 32,
    val overflowPolicy: StackOverflowPolicy = StackOverflowPolicy.REJECT_WHEN_FULL,
    val operationByCommandType: Map<String, NavigationOperation> = DefaultOperations,
) {
    init {
        require(maxDepth > 0) { "maxDepth must be positive." }
        require(operationByCommandType.keys.all { it.isNotBlank() }) { "commandType keys must not be blank." }
    }

    fun operationFor(command: MindNavigationCommand): NavigationOperation =
        operationByCommandType[command.commandType] ?: NavigationOperation.PUSH_NEW

    companion object {
        val DefaultOperations = mapOf(
            MindNavigationCommand.OpenMessages.commandType to NavigationOperation.SINGLETON,
            MindNavigationCommand.OpenSettings.commandType to NavigationOperation.SINGLETON,
        )

        val Default = NavigationPolicy()
    }
}

class BoundedNavigationCoordinator(
    private val delegate: NavigationCoordinator,
    private val policy: NavigationPolicy = NavigationPolicy.Default,
) : NavigationCoordinator {

    private val mutex = Mutex()
    private val shadowStack = ArrayList<MindNavigationCommand>()

    override suspend fun navigate(command: MindNavigationCommand): NavigationOutcome {
        val shouldDelegate = mutex.withLock {
            when (policy.operationFor(command)) {
                NavigationOperation.SINGLETON -> handleSingletonLocked(command)
                NavigationOperation.PUSH_NEW -> handlePushLocked(command)
            }
        }
        return if (shouldDelegate) delegate.navigate(command) else NavigationOutcome.Success
    }

    suspend fun snapshot(): List<MindNavigationCommand> = mutex.withLock { shadowStack.toList() }

    private fun handleSingletonLocked(command: MindNavigationCommand): Boolean {
        if (shadowStack.lastOrNull() == command) return false
        val existingIndex = shadowStack.indexOf(command)
        if (existingIndex >= 0) {
            shadowStack.removeAt(existingIndex)
            shadowStack += command
            return true
        }
        return handlePushLocked(command)
    }

    private fun handlePushLocked(command: MindNavigationCommand): Boolean {
        if (shadowStack.lastOrNull() == command) return false
        if (shadowStack.size >= policy.maxDepth) {
            when (policy.overflowPolicy) {
                StackOverflowPolicy.REJECT_WHEN_FULL -> return false
                StackOverflowPolicy.DROP_OLDEST_WHEN_FULL -> shadowStack.removeAt(0)
            }
        }
        shadowStack += command
        return true
    }
}
