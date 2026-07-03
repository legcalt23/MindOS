// Dense calibrated content bank; the literal difficulty levels are the data, not a magic-number smell.
@file:Suppress("MagicNumber")

package com.mindos.app.features.mindboost.language.presentation.exercises

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.presentation.DemoItem

/**
 * E3 · Compression Ladder — synthesis under constraint (SUM). The user picks the summary that keeps
 * the thesis within a tight budget. Distractors are diagnostic: one drops the thesis (SUM_NO_THESIS),
 * one distorts/overgeneralizes it (SUM_DISTORT). The desirable difficulty is the word limit: the core
 * must survive compression. A faithful short summary preserves the claim and its qualifier.
 */
object CompressionLadderBank {

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
        trait = TraitCode.SUM,
        operation = OperationType.SUMMARIZE,
        level = level,
    )

    val items: List<DemoItem> = listOf(
        item(
            id = "comp_lad_1",
            prompt = "«Subió el precio del café por una sequía en Brasil; los bares ya lo notan.» " +
                "¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "El texto habla de Brasil.",
                "b" to "Una sequía encarece el café y afecta a los bares.",
                "c" to "El café está caro porque sí.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 1,
        ),
        item(
            id = "comp_lad_2",
            prompt = "«El ayuntamiento amplió el carril bici; ahora hay menos coches en el centro " +
                "y más comercios abren los fines de semana.» ¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "El ayuntamiento hizo cosas en el centro.",
                "b" to "Ampliar el carril bici redujo coches y animó el comercio del centro.",
                "c" to "El carril bici prohíbe los coches en la ciudad.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 1,
        ),
        item(
            id = "comp_lad_3",
            prompt = "«Una app de idiomas añadió recordatorios diarios. Quienes los activaron " +
                "practicaron más días seguidos, aunque algunos los desactivaron por molestos.» " +
                "¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "Una app cambió y pasaron cosas.",
                "b" to "Los recordatorios aumentaron la constancia, aunque a algunos les molestaron.",
                "c" to "Los recordatorios molestan a todos los usuarios.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 2,
        ),
        item(
            id = "comp_lad_4",
            prompt = "«El museo abrió gratis los jueves por la tarde. Las visitas de jóvenes crecieron, " +
                "pero la tienda del museo vendió menos esos días.» ¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "El museo hizo una promoción.",
                "b" to "La gratuidad de los jueves atrajo a más jóvenes pero bajó las ventas de la tienda.",
                "c" to "El museo gana más dinero los jueves.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 2,
        ),
        item(
            id = "comp_lad_5",
            prompt = "«Un pueblo instaló paneles solares en el colegio; la factura de luz bajó " +
                "y usaron el ahorro para comprar libros.» ¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "El pueblo hizo una instalación.",
                "b" to "Los paneles del colegio bajaron la factura y el ahorro compró libros.",
                "c" to "El pueblo dejó de pagar la luz.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 3,
        ),
        item(
            id = "comp_lad_6",
            prompt = "«Una cadena de cafeterías quitó los vasos de un solo uso y dio descuento a quien " +
                "llevara el suyo. Se redujo la basura del local, aunque al principio bajaron algo las " +
                "ventas para llevar.» ¿Cuál es el mejor resumen?",
            options = listOf(
                "a" to "Una cafetería cambió sus vasos.",
                "b" to "Premiar el vaso reutilizable redujo la basura, con una caída inicial de ventas para llevar.",
                "c" to "Quitar los vasos arruinó a la cafetería.",
            ),
            correct = "b",
            distractors = mapOf("a" to ErrorType.SUM_NO_THESIS, "c" to ErrorType.SUM_DISTORT),
            level = 3,
        ),
    )

    val exercise: LanguageExercise = LanguageExercise(
        id = "compression_ladder",
        title = "Compression Ladder",
        subtitle = "Comprime sin perder la tesis: el mejor resumen conserva el núcleo y su matiz.",
        primaryTrait = TraitCode.SUM,
        family = LanguageExerciseFamily.COMPRESSION_LADDER,
        items = items,
    )
}
