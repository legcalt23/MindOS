// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E1 · Evidence Lock — legitimate, evidence-anchored inference (INF). The user must pick the only
 * conclusion the text supports AND lock the exact span(s) that justify it. Picking the right option
 * without its evidence is capped (0.5) and flagged as over-inference (INF_OVER) by the scoring engine,
 * which is the whole point: it trains *not* over-reading. Every supported conclusion has textual proof.
 */
object EvidenceLockBank {

    @Suppress("LongParameterList") // content factory: each field is one part of the item descriptor.
    private fun item(
        id: String,
        prompt: String,
        options: List<Pair<String, String>>,
        evidence: List<Pair<String, String>>,
        correct: String,
        required: Set<String>,
        distractors: Map<String, ErrorType>,
        level: Int,
    ): DemoItem = evidenceSelection(
        id = id,
        prompt = prompt,
        options = options.map { opt(it.first, it.second) },
        evidence = evidence.map { opt(it.first, it.second) },
        correct = correct,
        requiredEvidence = required,
        distractors = distractors,
        trait = TraitCode.INF,
        operation = OperationType.EVIDENCE_SUPPORTED_INFERENCE,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "ev_lock_1",
            prompt = "«El parte: cielo nublado, viento del norte y bajada de temperatura.» " +
                "Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "Va a nevar seguro.",
                "b" to "Puede hacer más frío.",
                "c" to "Hará calor.",
            ),
            evidence = listOf(
                "e1" to "cielo nublado",
                "e2" to "viento del norte",
                "e3" to "bajada de temperatura",
            ),
            correct = "b",
            required = setOf("e3"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 1,
        ),
        item(
            id = "ev_lock_2",
            prompt = "«Marta salió con paraguas y botas de agua, y miró al cielo antes de ir al trabajo.» " +
                "Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "Marta espera lluvia.",
                "b" to "Está lloviendo ahora mismo.",
                "c" to "A Marta le gusta la moda.",
            ),
            evidence = listOf(
                "e1" to "paraguas",
                "e2" to "botas de agua",
                "e3" to "miró al cielo",
            ),
            correct = "a",
            required = setOf("e1", "e2"),
            distractors = mapOf("b" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 1,
        ),
        item(
            id = "ev_lock_3",
            prompt = "«La biblioteca anunció que cerrará dos horas antes durante toda la semana " +
                "por obras en la entrada.» Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "La biblioteca cerrará para siempre.",
                "b" to "Esta semana habrá menos horas de servicio.",
                "c" to "Las obras son en la sala de lectura.",
            ),
            evidence = listOf(
                "e1" to "cerrará dos horas antes",
                "e2" to "durante toda la semana",
                "e3" to "obras en la entrada",
            ),
            correct = "b",
            required = setOf("e1", "e2"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 2,
        ),
        item(
            id = "ev_lock_4",
            prompt = "«El equipo perdió sus últimos tres partidos; en rueda de prensa el entrenador " +
                "dijo que revisará la táctica.» Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "Van a despedir al entrenador.",
                "b" to "El entrenador planea cambios en el juego.",
                "c" to "El equipo ganó los últimos partidos.",
            ),
            evidence = listOf(
                "e1" to "perdió sus últimos tres partidos",
                "e2" to "revisará la táctica",
                "e3" to "rueda de prensa",
            ),
            correct = "b",
            required = setOf("e2"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 2,
        ),
        item(
            id = "ev_lock_5",
            prompt = "«Una panadería del barrio empezó a abrir también los domingos. Desde entonces, " +
                "las colas de los sábados se han acortado y el dueño ha contratado a una persona más. " +
                "Algunos vecinos dicen que el pan sabe igual que siempre.» " +
                "Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "El pan ha mejorado de calidad.",
                "b" to "Abrir en domingo repartió la demanda entre más días.",
                "c" to "La panadería va a cerrar los sábados.",
            ),
            evidence = listOf(
                "e1" to "abrir también los domingos",
                "e2" to "las colas de los sábados se han acortado",
                "e3" to "contratado a una persona más",
            ),
            correct = "b",
            required = setOf("e1", "e2"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
        item(
            id = "ev_lock_6",
            prompt = "«Un instituto sustituyó los exámenes finales por entregas mensuales. Al terminar " +
                "el curso repitieron menos alumnos que el año anterior, aunque ese año también hubo " +
                "menos faltas de asistencia. La dirección no ha publicado más datos.» " +
                "Elige la conclusión soportada y marca su evidencia.",
            options = listOf(
                "a" to "Las entregas mensuales hicieron que repitieran menos.",
                "b" to "Repitieron menos alumnos que el año anterior.",
                "c" to "Aumentaron las faltas de asistencia.",
            ),
            evidence = listOf(
                "e1" to "sustituyó los exámenes finales por entregas mensuales",
                "e2" to "repitieron menos alumnos que el año anterior",
                "e3" to "también hubo menos faltas de asistencia",
            ),
            correct = "b",
            required = setOf("e2"),
            distractors = mapOf("a" to ErrorType.INF_OVER, "c" to ErrorType.LIT_DIST),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "evidence_lock",
        title = "Evidence Lock",
        subtitle = "Infiere solo lo que el texto prueba. Marca la evidencia que sostiene tu conclusión.",
        primaryTrait = TraitCode.INF,
        family = LanguageExerciseFamily.EVIDENCE_LOCK,
        items = items,
    )
}
