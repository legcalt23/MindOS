package com.mindos.command.runtime

import com.mindos.action.execution.ActionExecutor
import com.mindos.action.execution.ExecutionContext
import com.mindos.action.execution.ExecutionResult
import com.mindos.action.model.MindAction
import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto

class RecordingRouter(
    private val result: (RouterRequestDto) -> RouterResultDto,
) : KmpCommandRouter {
    val requests = mutableListOf<RouterRequestDto>()

    override suspend fun route(request: RouterRequestDto): RouterResultDto {
        requests += request
        return result(request)
    }
}

class RecordingExecutor(
    private val result: (MindAction, ExecutionContext) -> ExecutionResult,
) : ActionExecutor {
    val actions = mutableListOf<MindAction>()
    val contexts = mutableListOf<ExecutionContext>()

    override suspend fun execute(action: MindAction, context: ExecutionContext): ExecutionResult {
        actions += action
        contexts += context
        return result(action, context)
    }
}

class MutableConfirmationClock(var currentMillis: Long = 0L) : ConfirmationClock {
    override fun nowMillis(): Long = currentMillis
}

class IncrementingTokenFactory : ConfirmationTokenFactory {
    private var next = 0
    override fun next(): ConfirmationToken = ConfirmationToken("token-${next++}")
}

fun testConfirmationStore(clock: MutableConfirmationClock = MutableConfirmationClock()) =
    InMemoryConfirmationStore(IncrementingTokenFactory(), clock)
