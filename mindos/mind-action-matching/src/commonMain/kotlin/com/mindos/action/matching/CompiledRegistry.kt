package com.mindos.action.matching

import com.mindos.action.entity.EntityExtractorRegistry
import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.pattern.RegistrySchema
import com.mindos.action.understanding.MindIntentId

/**
 * The immutable, validated, indexed registry the matcher serves from. Built only by
 * [RegistryCompiler] (private constructor via internal). "Compile once, serve fast."
 */
class CompiledRegistry internal constructor(
    val schema: RegistrySchema,
    val normalizationConfig: NormalizationConfig,
    val intentIds: List<MindIntentId>,
    internal val exactIndex: ExactIndex,
    internal val aliasIndex: AliasIndex,
    internal val trie: TokenTrie,
    internal val entityRegistry: EntityExtractorRegistry,
    internal val parametricEntries: List<ParametricEntry>,
) {
    val exactCount: Int get() = exactIndex.size
    val aliasCount: Int get() = aliasIndex.size
    val parametricCount: Int get() = parametricEntries.size
}
