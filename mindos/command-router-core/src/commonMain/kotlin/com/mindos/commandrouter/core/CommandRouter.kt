package com.mindos.commandrouter.core

interface CommandRouter {
    suspend fun route(input: CommandInput): CommandRouterResult
}
