package com.mindos.commandrouter.core

import com.mindos.commandrouter.patterns.CommandNormalizer
import com.mindos.commandrouter.patterns.PatternMatchResult
import com.mindos.commandrouter.patterns.PatternRegistry
import com.mindos.commandrouter.resolver.ResolutionContext
import com.mindos.commandrouter.resolver.ResolutionResult
import com.mindos.commandrouter.resolver.ResolverRegistry
import com.mindos.security.InputSecurityCode
import com.mindos.security.InputSecurityInspector

class DefaultCommandRouter(
    private val normalizer: CommandNormalizer,
    private val patternRegistry: PatternRegistry,
    private val resolverRegistry: ResolverRegistry,
    private val config: CommandRouterConfig = CommandRouterConfig.Default
) : CommandRouter {

    override suspend fun route(input: CommandInput): CommandRouterResult {
        val trace = CommandRoutingTrace.Builder(input)

        val inputSecurity = InputSecurityInspector.inspect(
            input.rawText,
            config.inputSecurityPolicy.copy(maxInputLength = config.maxInputLength),
        )
        if (inputSecurity.hasRejections) {
            val codes = inputSecurity.rejectionCodes.map { it.name }
            trace.inputTooLong()
            if (InputSecurityCode.INPUT_TOO_LONG in inputSecurity.rejectionCodes) {
                return CommandRouterResult.Failure(
                    CommandRouterFailure.InputTooLong(input.rawText.length, config.maxInputLength),
                    trace.build()
                )
            }
            trace.unsafeInput(codes.joinToString("|") { it.lowercase() })
            return CommandRouterResult.Failure(
                CommandRouterFailure.UnsafeInput(codes),
                trace.build()
            )
        }

        val normalized = normalizer.normalize(input.rawText)
        trace.normalized(normalized)

        if (normalized.isBlank()) {
            return CommandRouterResult.Failure(CommandRouterFailure.EmptyInput, trace.build())
        }

        when (val matchResult = patternRegistry.findMatch(normalized)) {
            is PatternMatchResult.NoMatch -> {
                trace.noMatch()
                return CommandRouterResult.Failure(
                    CommandRouterFailure.NoPatternMatched(normalized),
                    trace.build()
                )
            }
            is PatternMatchResult.Ambiguous -> {
                trace.ambiguous(matchResult.candidates)
                return CommandRouterResult.Failure(
                    CommandRouterFailure.AmbiguousCommand(
                        normalized,
                        matchResult.candidates.map { it.patternId }
                    ),
                    trace.build()
                )
            }
            is PatternMatchResult.Matched -> {
                trace.matched(matchResult.match, matchResult.pattern)

                val resolver = resolverRegistry.getResolver(matchResult.pattern.id)
                    ?: run {
                        trace.noResolver(matchResult.pattern.id)
                        return CommandRouterResult.Failure(
                            CommandRouterFailure.NoResolverForPattern(matchResult.pattern.id),
                            trace.build()
                        )
                    }

                val context = ResolutionContext(
                    locale = input.locale,
                    correlationId = input.correlationId,
                    source = input.source
                )

                return when (val resolution = resolver.resolve(matchResult.match, context)) {
                    is ResolutionResult.Resolved -> {
                        trace.resolved(resolution.action, resolution.resolverId)
                        CommandRouterResult.Success(resolution.action, trace.build())
                    }
                    is ResolutionResult.Failed -> {
                        trace.resolutionFailed(resolution.failure)
                        CommandRouterResult.Failure(
                            CommandRouterFailure.ResolutionFailed(resolution.failure),
                            trace.build()
                        )
                    }
                }
            }
        }
    }
}
