package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.serialization.json.Json

/**
 * In-memory [CognitiveSessionRunStore]: the SAME [SecureCognitiveSessionRunStore] logic over an
 * [InMemorySecureKeyValueStore], so its behavior (keys, ordering, idempotency, conflict, corruption handling)
 * CANNOT diverge from the durable store — equivalence by construction. For tests and non-persistent contexts.
 *
 * [subject] defaults to [DEFAULT_SUBJECT] because the vast majority of callers (existing single-subject tests)
 * do not exercise cross-subject isolation and should not need to name a subject just to construct a store.
 * PRODUCTION code never relies on this default — every production construction site
 * ([com.mindos.app.features.mindboost.core.session.cognitive.SecureCognitiveSessionRunStore] via
 * `SpeedRootScreen`) supplies the real authenticated uid explicitly. A test that DOES care about isolation must
 * construct two instances with two DISTINCT explicit [subject] values (see `CognitiveSessionRunStoreTest`).
 */
class InMemoryCognitiveSessionRunStore(
    subject: String = DEFAULT_SUBJECT,
    json: Json = cognitiveRunJson,
) : CognitiveSessionRunStore by SecureCognitiveSessionRunStore(
    InMemorySecureKeyValueStore(),
    // Per-instance in-memory revocation backing: fine for this non-persistent/test store (revocation durability
    // and cross-instance sharing are exercised via SecureCognitiveSessionRunStore with explicit shared stores).
    InMemorySecureKeyValueStore(),
    subject,
    json,
) {
    companion object {
        /** Default subject label for single-subject tests that do not care about cross-subject isolation. */
        const val DEFAULT_SUBJECT = "in_memory_default_subject"
    }
}
