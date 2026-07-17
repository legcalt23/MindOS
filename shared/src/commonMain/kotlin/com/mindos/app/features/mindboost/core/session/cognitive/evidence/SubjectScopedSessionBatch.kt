package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun

/**
 * SYSTEM-1 slice 6-R2 · A defensively-copied wrapper around ONE subject's [CognitiveSessionRun] history. Its job
 * is a single, auditable, greppable construction chokepoint that (a) forces every call site to make an explicit
 * "this is a subject-scoped batch" assertion instead of passing a bare `List`, and (b) defends against a caller
 * mutating a `MutableList` after construction.
 *
 * HONEST LIMITATION (do not let the name over-promise): [CognitiveSessionRun] carries NO subject/user id by
 * design — identity is implicit in the per-user, account-bound STORAGE NAMESPACE
 * ([com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore]), not in the run. This type
 * therefore CANNOT verify that [runs] genuinely belongs to one subject; that guarantee comes from WHERE the list
 * was read (a namespace-scoped repository), which no Kotlin type in a single-Gradle-module build can see into.
 * [of] documents and audits the assertion; it is NOT an access-control boundary. [subjectToken] is a PSEUDONYMOUS
 * scope label (e.g. a namespace alias) — pseudonymous, NOT anonymized: it can relink one subject's records but is
 * not, by itself, an identifier. A real repository that constructs batches only from a single account-scoped store
 * is the remaining wiring precondition (W1).
 */
class SubjectScopedSessionBatch private constructor(
    val subjectToken: String,
    val runs: List<CognitiveSessionRun>,
) {
    companion object {
        /**
         * The sole construction path. Defensively copies [runs] so a caller mutating its own list afterward cannot
         * change an already-built batch. [subjectToken] must be a non-blank pseudonymous scope label.
         */
        fun of(subjectToken: String, runs: List<CognitiveSessionRun>): SubjectScopedSessionBatch {
            require(subjectToken.isNotBlank()) { "subjectToken must not be blank (a pseudonymous scope label)." }
            return SubjectScopedSessionBatch(subjectToken, runs.toList())
        }
    }
}
