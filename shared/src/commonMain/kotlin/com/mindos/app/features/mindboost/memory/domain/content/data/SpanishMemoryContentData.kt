@file:Suppress("MagicNumber") // Data file: the literals ARE the authored ES content (see AppColors precedent).

package com.mindos.app.features.mindboost.memory.domain.content.data

import com.mindos.app.features.mindboost.memory.domain.content.model.AssociationStrength
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness.ABSTRACT
import com.mindos.app.features.mindboost.memory.domain.content.model.Concreteness.CONCRETE
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.PairId
import com.mindos.app.features.mindboost.memory.domain.content.model.PairItem
import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency.HIGH
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency.LOW
import com.mindos.app.features.mindboost.memory.domain.content.model.WordFrequency.MEDIUM
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.pool.PairPool
import com.mindos.app.features.mindboost.memory.domain.content.pool.WordPool

/**
 * The single source of authored Spanish (ES) content for MindBoost · Memoria V1.
 * This is the ONLY file that holds content literals; isolating them here keeps the
 * pool/sampler/partition code free of data (mirrors the AppColors token-file pattern).
 *
 * VOLUME HONESTY: this V1 pool (≈142 words / 56 pairs) is intentionally a
 * representative starter set, well above the hard floors but far below the
 * scientific ideal (>2000 words). [WordPool.qualityReport] flags this; expanding
 * the pool to 2000+ with a reference corpus (e.g. LEXESP/EsPal) is a tracked
 * follow-up before production launch.
 *
 * `lengthChars` is derived from `text.length`, never hand-typed, so it can't drift.
 * Pairs are built FROM the authored words (referential integrity by construction).
 */
internal object SpanishMemoryContentData {

    const val WORD_POOL_VERSION = 1
    const val PAIR_POOL_VERSION = 1

    private fun w(
        id: String,
        text: String,
        category: WordCategory,
        concreteness: Concreteness,
        frequency: WordFrequency,
    ): WordItem = WordItem(
        id = WordId(id),
        text = text,
        category = category,
        concreteness = concreteness,
        frequency = frequency,
        lengthChars = text.length,
    )

    private fun cat(
        prefix: String,
        category: WordCategory,
        concreteness: Concreteness,
        frequency: WordFrequency,
        vararg texts: String,
    ): List<WordItem> =
        texts.mapIndexed { i, text -> w("$prefix${i + 1}", text, category, concreteness, frequency) }

    private val animals = cat(
        "an", WordCategory.ANIMALS, CONCRETE, HIGH,
        "perro", "gato", "caballo", "vaca", "oveja", "conejo", "raton", "leon", "tigre",
        "oso", "lobo", "zorro", "aguila", "paloma", "abeja", "araña", "delfin", "ballena",
    )

    private val vehicles = cat(
        "ve", WordCategory.VEHICLES, CONCRETE, MEDIUM,
        "coche", "moto", "camion", "autobus", "tren", "avion", "barco", "bicicleta",
        "helicoptero", "tractor", "furgoneta", "metro", "taxi", "ambulancia",
    )

    private val foods = cat(
        "fo", WordCategory.FOODS, CONCRETE, HIGH,
        "pan", "queso", "manzana", "platano", "naranja", "tomate", "patata", "arroz",
        "huevo", "leche", "carne", "pescado", "pollo", "sopa", "pastel", "chocolate",
    )

    private val objects = cat(
        "ob", WordCategory.OBJECTS, CONCRETE, MEDIUM,
        "mesa", "silla", "libro", "lampara", "reloj", "telefono", "ordenador", "ventana",
        "puerta", "llave", "espejo", "botella", "paraguas", "martillo", "tijeras", "cuchara",
    )

    private val places = cat(
        "pl", WordCategory.PLACES, CONCRETE, MEDIUM,
        "casa", "escuela", "hospital", "parque", "playa", "montaña", "ciudad", "pueblo",
        "mercado", "estacion", "biblioteca", "teatro", "museo", "aeropuerto",
    )

    private val professions = cat(
        "pr", WordCategory.PROFESSIONS, CONCRETE, LOW,
        "medico", "abogado", "ingeniero", "profesor", "cocinero", "bombero", "policia",
        "enfermero", "carpintero", "electricista", "fontanero", "piloto", "periodista", "arquitecto",
    )

    private val actions = cat(
        "ac", WordCategory.ACTIONS, ABSTRACT, HIGH,
        "correr", "saltar", "escribir", "leer", "cantar", "bailar", "pensar", "hablar",
        "escuchar", "construir", "romper", "abrir", "cerrar", "empujar", "lanzar", "atrapar",
    )

    private val emotions = cat(
        "em", WordCategory.EMOTIONS, ABSTRACT, MEDIUM,
        "alegria", "tristeza", "miedo", "enfado", "sorpresa", "calma", "amor", "odio",
        "envidia", "orgullo", "verguenza", "esperanza", "ansiedad", "gratitud", "nostalgia", "entusiasmo",
    )

    private val concepts = cat(
        "co", WordCategory.CONCEPTS, ABSTRACT, LOW,
        "libertad", "justicia", "verdad", "tiempo", "idea", "memoria", "futuro", "pasado",
        "energia", "materia", "espacio", "numero", "logica", "teoria", "sistema", "equilibrio",
        "destino", "infinito",
    )

    private val allWords: List<WordItem> =
        animals + vehicles + foods + objects + places + professions + actions + emotions + concepts

    /** Number of word-word pairs authored from [allWords] (each pair uses two distinct words). */
    private const val PAIR_COUNT = 56

    private val allPairs: List<PairItem> = (0 until PAIR_COUNT).map { k ->
        PairItem(
            id = PairId("pair${k + 1}"),
            cue = allWords[2 * k],
            target = allWords[2 * k + 1],
            associationStrength = AssociationStrength.entries[k % AssociationStrength.entries.size],
        )
    }

    val wordPool: WordPool = WordPool(
        version = WORD_POOL_VERSION,
        language = Language.SPANISH_ES,
        items = allWords,
    )

    val pairPool: PairPool = PairPool(
        version = PAIR_POOL_VERSION,
        language = Language.SPANISH_ES,
        items = allPairs,
    )
}
