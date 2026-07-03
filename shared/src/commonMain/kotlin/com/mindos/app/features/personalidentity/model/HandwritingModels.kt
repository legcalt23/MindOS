package com.mindos.app.features.personalidentity.model

/**
 * Canonical template for a future handwritten-font capture pipeline.
 *
 * Spanish-specific characters are explicit requirements of the template.
 */
data class HandwritingTemplateSpec(
    val rows: List<String> = defaultRows,
) {
    val characters: List<Char> = rows.flatMap { row ->
        row.filterNot { it.isWhitespace() }.toList()
    }

    companion object {
        val defaultRows: List<String> = listOf(
            "A B C D E F G H I J",
            "K L M N \u00D1 O P Q R S",
            "T U V W X Y Z",
            "a b c d e f g h i j",
            "k l m n \u00F1 o p q r s",
            "t u v w x y z",
            "0 1 2 3 4 5 6 7 8 9",
            ". , ! ? @ # \u00BF \u00A1 : ;",
        )
    }
}

data class HandwritingGlyph(
    val character: Char,
    val vectorPath: String? = null,
    val sourceImageId: String? = null,
)

data class HandwritingFontProfile(
    val id: String,
    val name: String,
    val glyphs: Map<Char, HandwritingGlyph> = emptyMap(),
    val templateSpec: HandwritingTemplateSpec = HandwritingTemplateSpec(),
) {
    val completionRatio: Float
        get() {
            val required = templateSpec.characters.distinct()
            if (required.isEmpty()) return 0f
            val completed = required.count { glyphs.containsKey(it) }
            return completed.toFloat() / required.size.toFloat()
        }

    val isComplete: Boolean get() = completionRatio >= COMPLETE_RATIO
}

private const val COMPLETE_RATIO = 1f
