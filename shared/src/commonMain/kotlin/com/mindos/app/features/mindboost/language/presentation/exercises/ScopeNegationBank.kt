// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E5 · Scope & Negation — fine syntax (SYN). The user resolves the scope of negation and quantifiers
 * (no, todos, solo, salvo, ni…ni, casi), a systematic source of comprehension errors. Distractors are
 * diagnostic: one mis-resolves the operator's scope (SYN_SCOPE), one contradicts the literal text
 * (LIT_DIST). The correct reading follows the operator's scope without adding a false inference.
 */
object ScopeNegationBank {

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
        trait = TraitCode.SYN,
        operation = OperationType.SYNTACTIC_MAPPING,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "scope_neg_1",
            prompt = "«Todos aprobaron menos Luis.» ¿Qué es cierto?",
            options = listOf(
                "a" to "Luis aprobó.",
                "b" to "Luis no aprobó; los demás sí.",
                "c" to "No aprobó nadie.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SYN_SCOPE, "c" to ErrorType.LIT_DIST),
            level = 1,
        ),
        item(
            id = "scope_neg_2",
            prompt = "«No vinieron ni Ana ni Pedro.» ¿Qué es cierto?",
            options = listOf(
                "a" to "Vino solo uno de los dos.",
                "b" to "No vino ninguno de los dos.",
                "c" to "Vinieron los dos.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SYN_SCOPE, "c" to ErrorType.LIT_DIST),
            level = 1,
        ),
        item(
            id = "scope_neg_3",
            prompt = "«Solo María entregó el trabajo a tiempo.» ¿Qué es cierto?",
            options = listOf(
                "a" to "María no entregó a tiempo.",
                "b" to "Nadie más entregó a tiempo.",
                "c" to "Todos entregaron a tiempo.",
            ),
            correct = "b",
            distractors = mapOf("c" to ErrorType.SYN_SCOPE, "a" to ErrorType.LIT_DIST),
            level = 2,
        ),
        item(
            id = "scope_neg_4",
            prompt = "«No todos los alumnos suspendieron.» ¿Qué es cierto?",
            options = listOf(
                "a" to "No suspendió ningún alumno.",
                "b" to "Al menos un alumno aprobó.",
                "c" to "Suspendieron todos los alumnos.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SYN_SCOPE, "c" to ErrorType.LIT_DIST),
            level = 2,
        ),
        item(
            id = "scope_neg_5",
            prompt = "«Casi ningún estudiante faltó a más de una clase.» ¿Qué es cierto?",
            options = listOf(
                "a" to "La mayoría faltó a muchas clases.",
                "b" to "La mayoría faltó como mucho a una clase.",
                "c" to "Ningún estudiante fue a clase.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SYN_SCOPE, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
        item(
            id = "scope_neg_6",
            prompt = "«Nadie aprobó sin estudiar.» ¿Qué es cierto?",
            options = listOf(
                "a" to "Quien estudió aprobó seguro.",
                "b" to "Para aprobar, hizo falta estudiar.",
                "c" to "No aprobó nadie.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SYN_SCOPE, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "scope_negation",
        title = "Scope & Negation",
        subtitle = "Resuelve el alcance de «no», «solo», «todos», «ni…ni» sin inventar lo que no dice.",
        primaryTrait = TraitCode.SYN,
        family = LanguageExerciseFamily.SCOPE_NEGATION,
        items = items,
    )
}
