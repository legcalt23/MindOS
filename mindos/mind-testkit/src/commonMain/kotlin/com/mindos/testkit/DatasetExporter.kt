package com.mindos.testkit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializes datasets to machine-readable JSON (ZIP11 "output machine-readable assets"). Pure and
 * deterministic — the same rows always serialize to the same bytes, so committed sample assets stay
 * stable and golden-comparable. File writing is a platform concern done by the caller.
 */
object DatasetExporter {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    fun rowsToJson(rows: List<DatasetRow>): String = json.encodeToString(rows)

    fun aliasesToJson(rows: List<AliasRow>): String = json.encodeToString(rows)

    fun ambiguityToJson(cases: List<AmbiguityCase>): String = json.encodeToString(cases)
}
