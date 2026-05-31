package com.mindos.commandrouter.core

import com.mindos.commandrouter.actions.ActionId
import com.mindos.commandrouter.actions.ActionIdFactory
import com.mindos.commandrouter.actions.TimestampProvider
import com.mindos.commandrouter.patterns.CommandNormalizer
import com.mindos.commandrouter.patterns.CommandPattern
import com.mindos.commandrouter.patterns.DefaultCommandNormalizer
import com.mindos.commandrouter.patterns.PatternId
import com.mindos.commandrouter.patterns.PatternRegistry
import com.mindos.commandrouter.resolver.ActionFactory
import com.mindos.commandrouter.resolver.CommandResolver
import com.mindos.commandrouter.resolver.ResolverRegistry

class CommandRouterBuilder {
    private val patterns = mutableListOf<CommandPattern>()
    private val resolvers = mutableMapOf<PatternId, CommandResolver>()
    private var normalizer: CommandNormalizer = DefaultCommandNormalizer()
    private var config: CommandRouterConfig = CommandRouterConfig.Default
    private var idFactory: ActionIdFactory = ActionIdFactory { ActionId("action-${++idSeq}") }
    private var timestampProvider: TimestampProvider = TimestampProvider { currentTimeMillis() }
    private var idSeq = 0

    fun addPattern(pattern: CommandPattern, resolver: CommandResolver): CommandRouterBuilder {
        patterns += pattern
        resolvers[pattern.id] = resolver
        return this
    }

    fun withNormalizer(normalizer: CommandNormalizer): CommandRouterBuilder {
        this.normalizer = normalizer
        return this
    }

    fun withConfig(config: CommandRouterConfig): CommandRouterBuilder {
        this.config = config
        return this
    }

    fun withIdFactory(factory: ActionIdFactory): CommandRouterBuilder {
        this.idFactory = factory
        return this
    }

    fun withTimestampProvider(provider: TimestampProvider): CommandRouterBuilder {
        this.timestampProvider = provider
        return this
    }

    fun actionFactory(): ActionFactory = ActionFactory(idFactory, timestampProvider)

    fun build(): CommandRouter = DefaultCommandRouter(
        normalizer = normalizer,
        patternRegistry = PatternRegistry(patterns.toList()),
        resolverRegistry = ResolverRegistry(resolvers.toMap()),
        config = config
    )
}

internal expect fun currentTimeMillis(): Long
