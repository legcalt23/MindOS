package com.mindos.action.resolver

import com.mindos.action.model.ReferenceKind

/** Immutable lookup of [ReferenceResolver] by [ReferenceKind]. No global mutable state. */
class ResolverRegistry(resolvers: List<ReferenceResolver>) {
    private val byKind: Map<ReferenceKind, ReferenceResolver> = resolvers.associateBy { it.kind }

    val supportedKinds: Set<ReferenceKind> get() = byKind.keys

    fun forKind(kind: ReferenceKind): ReferenceResolver? = byKind[kind]

    fun supports(kind: ReferenceKind): Boolean = byKind.containsKey(kind)
}
