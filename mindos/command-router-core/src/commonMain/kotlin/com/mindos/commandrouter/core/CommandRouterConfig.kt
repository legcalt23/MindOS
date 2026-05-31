package com.mindos.commandrouter.core

import com.mindos.security.InputSecurityPolicy

data class CommandRouterConfig(
    val maxInputLength: Int = 512,
    val inputSecurityPolicy: InputSecurityPolicy = InputSecurityPolicy.Default,
) {
    companion object {
        val Default = CommandRouterConfig()
    }
}
