package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.patterns.PatternMatch

interface CommandResolver {
    val id: ResolverId
    suspend fun resolve(match: PatternMatch, context: ResolutionContext): ResolutionResult
}
