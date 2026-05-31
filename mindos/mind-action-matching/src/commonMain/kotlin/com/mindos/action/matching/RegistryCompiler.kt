package com.mindos.action.matching

import com.mindos.action.entity.EntityExtractorRegistry
import com.mindos.action.entity.EntityType
import com.mindos.action.normalization.NormalizationPipeline
import com.mindos.action.observability.MindTelemetryEvent
import com.mindos.action.observability.MindTelemetrySink
import com.mindos.action.observability.NoOpMindTelemetrySink
import com.mindos.action.pattern.ActionPattern.ParametricActionPattern
import com.mindos.action.pattern.PatternDeclaration
import com.mindos.action.pattern.PatternTemplate
import com.mindos.action.pattern.PatternToken
import com.mindos.action.pattern.RegistrySource
import com.mindos.action.pattern.SlotCardinality
import com.mindos.action.understanding.MindIntentId

/**
 * Validates a [RegistrySource] and, on success, builds an immutable [CompiledRegistry].
 *
 * Fails at **build time, not in production** (ZIP02 §7): duplicate ids/phrases/aliases,
 * unparseable templates, slots without an extractor, illegal greedy slots, and—critically—
 * same-shape/same-priority/different-intent pattern conflicts. All findings are collected
 * (not fail-fast) so authors see every problem at once.
 */
object RegistryCompiler {

    fun compile(
        source: RegistrySource,
        options: CompileOptions = CompileOptions(),
        telemetry: MindTelemetrySink = NoOpMindTelemetrySink,
    ): RegistryCompilationResult {
        val errors = ArrayList<RegistryIssue>()
        val warnings = ArrayList<RegistryIssue>()
        val pipeline = NormalizationPipeline(options.normalization)
        val norm: (String) -> String = { pipeline.normalize(it).text }

        if (source.patterns.isEmpty() && source.aliases.isEmpty()) {
            warnings += RegistryIssue(IssueSeverity.WARNING, IssueCode.EMPTY_REGISTRY, "registry has no patterns or aliases")
        }

        // 1) Duplicate pattern ids.
        val seenIds = HashSet<String>()
        for (decl in source.patterns) {
            if (!seenIds.add(decl.patternId.value)) {
                errors += RegistryIssue(
                    IssueSeverity.ERROR, IssueCode.DUPLICATE_PATTERN_ID,
                    "duplicate patternId '${decl.patternId.value}'",
                )
            }
        }

        // 2) Parse declarations into typed forms (declaration order = index in source.patterns).
        val exacts = ArrayList<CompiledExact>()
        val parametrics = ArrayList<ParametricEntry>()
        source.patterns.forEachIndexed { idx, decl ->
            when (decl) {
                is PatternDeclaration.Exact -> {
                    val phrase = norm(decl.phrase)
                    if (phrase.isEmpty()) {
                        errors += RegistryIssue(
                            IssueSeverity.ERROR, IssueCode.EMPTY_EXACT_PHRASE,
                            "exact pattern '${decl.patternId.value}' normalizes to an empty phrase",
                        )
                    } else {
                        exacts += CompiledExact(decl, phrase, idx)
                    }
                }
                is PatternDeclaration.Parametric -> {
                    when (val parsed = PatternTemplate.parse(decl.template)) {
                        is PatternTemplate.Result.Error -> errors += RegistryIssue(
                            IssueSeverity.ERROR, IssueCode.TEMPLATE_PARSE_ERROR,
                            "pattern '${decl.patternId.value}': ${parsed.message}",
                        )
                        is PatternTemplate.Result.Ok -> {
                            val pattern = ParametricActionPattern(decl.patternId, parsed.tokens, decl.intentId, decl.priority)
                            validateSlots(pattern, options.entityRegistry, errors)
                            parametrics += ParametricEntry(pattern, idx)
                        }
                    }
                }
            }
        }

        // 3) Same phrase mapped to multiple intents.
        exacts.groupBy { it.normalizedPhrase }.forEach { (phrase, group) ->
            val intents = group.map { it.decl.intentId }.toSet()
            if (intents.size > 1) {
                errors += RegistryIssue(
                    IssueSeverity.ERROR, IssueCode.DUPLICATE_EXACT_PHRASE,
                    "phrase '$phrase' maps to multiple intents ${intents.map { it.value }}",
                )
            }
        }

        // 4) Aliases: normalize, dedupe by (locale, text); conflict if different intent.
        val aliasByKey = HashMap<String, CompiledAlias>()
        val compiledAliases = ArrayList<CompiledAlias>()
        source.aliases.forEachIndexed { idx, alias ->
            val text = norm(alias.text)
            if (text.isEmpty()) return@forEachIndexed
            val key = AliasIndex.key(alias.localeTag, text)
            val existing = aliasByKey[key]
            when {
                existing == null -> {
                    val compiled = CompiledAlias(text, alias.localeTag, alias.intentId, source.patterns.size + idx)
                    aliasByKey[key] = compiled
                    compiledAliases += compiled
                }
                existing.intentId != alias.intentId -> errors += RegistryIssue(
                    IssueSeverity.ERROR, IssueCode.DUPLICATE_ALIAS,
                    "alias '$text' (locale '${alias.localeTag}') maps to multiple intents",
                )
                else -> Unit // same intent duplicate → idempotent, ignore
            }
        }

        // 5) Parametric conflicts (same shape, same priority, different intent).
        val conflictSeverity = if (options.failOnConflicts) IssueSeverity.ERROR else IssueSeverity.WARNING
        for (i in parametrics.indices) {
            for (j in i + 1 until parametrics.size) {
                val a = parametrics[i].pattern
                val b = parametrics[j].pattern
                if (a.intentId != b.intentId && a.priority == b.priority && structurallyConflict(a, b, norm)) {
                    val issue = RegistryIssue(
                        conflictSeverity, IssueCode.PATTERN_CONFLICT,
                        "pattern '${a.id.value}' conflicts with '${b.id.value}' " +
                            "(same shape, same priority, different intent); add priority, slot constraint or tieBreak",
                    )
                    if (conflictSeverity == IssueSeverity.ERROR) errors += issue else warnings += issue
                }
            }
        }

        // 6) Locale completeness (warning only).
        reportIncompleteLocales(compiledAliases, warnings)

        if (errors.isNotEmpty()) {
            telemetry.record(MindTelemetryEvent.RegistryCompileFailed(errors.size))
            return RegistryCompilationResult.Failure(errors, warnings)
        }

        val exactIndex = ExactIndex(
            exacts.associate {
                it.normalizedPhrase to ExactIndex.Entry(it.decl.patternId, it.decl.intentId, it.decl.priority, it.declOrder)
            },
        )
        val aliasIndex = AliasIndex(
            compiledAliases.associate {
                AliasIndex.key(it.localeTag, it.normalizedText) to AliasIndex.Entry(it.intentId, it.localeTag, it.declOrder)
            },
        )
        val trie = TokenTrie()
        for (entry in parametrics) trie.insert(entry, norm)

        val registry = CompiledRegistry(
            schema = source.schema,
            normalizationConfig = options.normalization,
            intentIds = source.declaredIntents,
            exactIndex = exactIndex,
            aliasIndex = aliasIndex,
            trie = trie,
            entityRegistry = options.entityRegistry,
            parametricEntries = parametrics,
        )
        telemetry.record(MindTelemetryEvent.RegistryCompiled(exactIndex.size + parametrics.size, aliasIndex.size))
        return RegistryCompilationResult.Success(registry, warnings)
    }

    private fun validateSlots(
        pattern: ParametricActionPattern,
        entityRegistry: EntityExtractorRegistry,
        errors: MutableList<RegistryIssue>,
    ) {
        pattern.tokens.forEachIndexed { index, token ->
            if (token !is PatternToken.Slot) return@forEachIndexed
            val slot = token.slot
            if (!entityRegistry.supports(slot.acceptedType)) {
                errors += RegistryIssue(
                    IssueSeverity.ERROR, IssueCode.UNSUPPORTED_SLOT_TYPE,
                    "pattern '${pattern.id.value}' slot '${slot.name.value}' uses type " +
                        "'${slot.acceptedType.id}' with no registered extractor",
                )
            }
            if (slot.cardinality == SlotCardinality.Greedy) {
                if (index != pattern.tokens.lastIndex) {
                    errors += RegistryIssue(
                        IssueSeverity.ERROR, IssueCode.INVALID_GREEDY_SLOT,
                        "pattern '${pattern.id.value}': greedy slot '${slot.name.value}' must be the last token",
                    )
                }
                if (slot.acceptedType != EntityType.FreeText) {
                    errors += RegistryIssue(
                        IssueSeverity.ERROR, IssueCode.INVALID_GREEDY_SLOT,
                        "pattern '${pattern.id.value}': greedy slot '${slot.name.value}' must be free_text",
                    )
                }
            }
        }
    }

    private fun structurallyConflict(
        a: ParametricActionPattern,
        b: ParametricActionPattern,
        norm: (String) -> String,
    ): Boolean {
        if (a.tokens.size != b.tokens.size) return false
        for (i in a.tokens.indices) {
            val ta = a.tokens[i]
            val tb = b.tokens[i]
            when {
                ta is PatternToken.Static && tb is PatternToken.Static ->
                    if (norm(ta.value) != norm(tb.value)) return false
                ta is PatternToken.Slot && tb is PatternToken.Slot ->
                    if (!EntityTypeOverlap.overlap(ta.slot.acceptedType, tb.slot.acceptedType)) return false
                else -> return false // different kinds ⇒ different specificity ⇒ ranking resolves it
            }
        }
        return true
    }

    private fun reportIncompleteLocales(aliases: List<CompiledAlias>, warnings: MutableList<RegistryIssue>) {
        val localeUnion = aliases.map { it.localeTag }.filterNot { it == "und" }.toSet()
        if (localeUnion.size <= 1) return
        aliases.groupBy { it.intentId }.forEach { (intent, list) ->
            val covered = list.map { it.localeTag }.filterNot { it == "und" }.toSet()
            val missing = localeUnion - covered
            if (covered.isNotEmpty() && missing.isNotEmpty()) {
                warnings += RegistryIssue(
                    IssueSeverity.WARNING, IssueCode.INCOMPLETE_LOCALE,
                    "intent '${intent.value}' has aliases for $covered but is missing $missing",
                )
            }
        }
    }

    private data class CompiledExact(
        val decl: PatternDeclaration.Exact,
        val normalizedPhrase: String,
        val declOrder: Int,
    )

    private data class CompiledAlias(
        val normalizedText: String,
        val localeTag: String,
        val intentId: MindIntentId,
        val declOrder: Int,
    )
}
