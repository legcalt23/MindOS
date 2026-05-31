package com.mindos.testkit

import kotlinx.serialization.Serializable

/** Command categories for the large-scale corpus (ZIP11 COMMAND_CORPUS_GUIDE). */
@Serializable
enum class CommandCategory { PROFILES, STORIES, CHATS, NOTIFICATIONS, SETTINGS, MIXED }

/** Variant kind of a generated row. */
@Serializable
enum class VariantKind { VALID, TYPO, SPACING, CAPITALIZATION, ALIAS, EDGE_CASE, MALFORMED }

/**
 * One machine-readable dataset row. [expectedIntentId] is non-null only for rows that must
 * deterministically resolve (valid/alias); variants/edge/malformed leave it null (only "no crash"
 * + determinism are guaranteed).
 */
@Serializable
data class DatasetRow(
    val text: String,
    val category: CommandCategory,
    val kind: VariantKind,
    val expectedIntentId: String? = null,
)

/** An alias stored **separately** from command patterns (ZIP11 ALIAS_DATASET_GUIDE). */
@Serializable
data class AliasRow(val text: String, val locale: String, val intentId: String)

/** A curated ambiguity case with its deterministic expected resolution (ZIP11 AMBIGUITY_DATASET). */
@Serializable
data class AmbiguityCase(
    val input: String,
    val description: String,
    /** "open_settings" | "open_profile" | … | "ambiguous" | "no_match". */
    val expected: String,
)
