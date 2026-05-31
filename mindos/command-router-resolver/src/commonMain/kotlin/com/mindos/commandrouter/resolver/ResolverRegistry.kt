package com.mindos.commandrouter.resolver

import com.mindos.commandrouter.patterns.PatternId

class ResolverRegistry(private val resolvers: Map<PatternId, CommandResolver>) {

    fun getResolver(patternId: PatternId): CommandResolver? = resolvers[patternId]

    fun patternIds(): Set<PatternId> = resolvers.keys
}
