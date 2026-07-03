// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E2 · Semantic Surgery — lexical precision (LEX_P). Among near-synonyms, the user picks the term
 * that says *exactly* what is meant. Distractors are diagnostic: one is vague (PROD_VAGUE), one breaks
 * register/tone (PRAG_TONE). There is no "wrong word" — only a less precise one, so the user trains
 * the fine-grained choice that makes writing and comprehension sharp. Changing one word raises precision.
 */
object SemanticSurgeryBank {

    @Suppress("LongParameterList") // content factory: each field is one part of the item descriptor.
    private fun item(
        id: String,
        prompt: String,
        options: List<Pair<String, String>>,
        correct: String,
        distractors: Map<String, ErrorType>,
        level: Int,
    ): DemoItem = single(
        id = id,
        prompt = prompt,
        options = options.map { opt(it.first, it.second) },
        correct = correct,
        distractors = distractors,
        trait = TraitCode.LEX_P,
        operation = OperationType.IDENTIFY_NUANCE,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "sem_surg_1",
            prompt = "«El informe fue ___: no dejó ninguna duda.» ¿Qué palabra es más precisa?",
            options = listOf(
                "a" to "bueno",
                "b" to "concluyente",
                "c" to "majo",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "c" to ErrorType.PRAG_TONE),
            level = 1,
        ),
        item(
            id = "sem_surg_2",
            prompt = "«La herida no revestía gravedad; el médico la describió como ___.» " +
                "¿Qué palabra es más precisa?",
            options = listOf(
                "a" to "pequeña",
                "b" to "leve",
                "c" to "chunga",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "c" to ErrorType.PRAG_TONE),
            level = 1,
        ),
        item(
            id = "sem_surg_3",
            prompt = "«El testigo admitía varias lecturas; su respuesta fue ___.» " +
                "¿Qué palabra es más precisa?",
            options = listOf(
                "a" to "rara",
                "b" to "ambigua",
                "c" to "regulera",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "c" to ErrorType.PRAG_TONE),
            level = 2,
        ),
        item(
            id = "sem_surg_4",
            prompt = "«La empresa no rechazó la oferta, pero tampoco la aceptó: su postura fue ___.» " +
                "¿Qué palabra es más precisa?",
            options = listOf(
                "a" to "mala",
                "b" to "evasiva",
                "c" to "cutre",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "c" to ErrorType.PRAG_TONE),
            level = 2,
        ),
        item(
            id = "sem_surg_5",
            prompt = "«El político no negó la acusación; se limitó a ___ las preguntas.» " +
                "¿Qué verbo es más preciso?",
            options = listOf(
                "a" to "hacer",
                "b" to "esquivar",
                "c" to "torear",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PROD_VAGUE, "c" to ErrorType.PRAG_TONE),
            level = 3,
        ),
        item(
            id = "sem_surg_6",
            prompt = "En un correo formal a un cliente, ¿qué cierre es más preciso y adecuado?",
            options = listOf(
                "a" to "Bss, nos vemos.",
                "b" to "Quedo a su disposición para cualquier consulta.",
                "c" to "Le escribo cosas.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.PRAG_TONE, "c" to ErrorType.PROD_VAGUE),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "semantic_surgery",
        title = "Semantic Surgery",
        subtitle = "Elige la palabra exacta entre cuasi-sinónimos: precisión, no aproximación.",
        primaryTrait = TraitCode.LEX_P,
        family = LanguageExerciseFamily.SEMANTIC_SURGERY,
        items = items,
    )
}
