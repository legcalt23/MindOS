package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionContent.ConfusionAxis
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory

/**
 * Ola 2 · R3 · P1 — the compositional item factory that removes memorization as a route to a high score.
 *
 * The problem it solves is plain: the authored seed holds 28 trainable phrases and one session consumes about
 * twenty, so by the second session the user has seen nearly all of it. What rises after that is recall of
 * phrase→category, not the ability to separate anything, and a rising number that means recall is exactly the
 * pseudo-progress this module must not produce.
 *
 * **Why the templates share vocabulary across categories.** The obvious compositional design gives each category
 * its own opening — `Siento…` a state, `Creo que…` a thought, `Quiero…` an urge, a counter a fact. That design
 * makes the problem WORSE: it turns the task into a prefix detector, and memorizing four prefixes is far cheaper
 * than memorizing twenty-eight phrases. A user would score full marks without reading past the first word, and a
 * bigger bank would have bought nothing.
 *
 * So the same lexical material appears under more than one category, which is also what produces the three
 * confusion axes the design asks for:
 *  - `Siento tensión en el pecho` (state) against `Siento que esto saldrá mal` (thought) — same verb.
 *  - `Quiero cerrar la aplicación` (urge) against `Acabo de cerrar la aplicación` (fact) — same verb.
 *  - `Esto ha ido regular` (a verdict) against `Esto ha durado seis minutos` (a reading) — same opening.
 *
 * The only way through is to read the whole phrase, which is the skill the exercise claims to train.
 *
 * **Difficulty rises by semantic boundary, never by intensity** — the rule `SeparatorDifficulty` already states.
 * Every slot below is everyday, mild and non-intimate; an `ambiguity = 1` item is harder because its category is
 * decided by the complement instead of the opening, not because its content weighs more. No combination of these
 * slots can be stronger than its parts, and the content guard test holds that line mechanically rather than
 * trusting this paragraph.
 *
 * **Deterministic by construction.** There is no RNG here: the bank is a fixed product walked in a fixed order.
 * Exposure history is keyed by phrase, so a bank that reshuffled between runs would silently re-point every
 * user's history at different items.
 */
internal object SeparatorItemFactory {

    /** Bank size the R3 design requires, counting the authored seed — it is extended, never replaced. */
    const val DESIGN_MIN_BANK_SIZE: Int = 300

    /**
     * Combinations drawn per template. Capped so no single template floods the bank: two hundred near-identical
     * `Siento X en Y` phrases are one item wearing many hats, and counting them all would let the bank's size
     * overstate how much it actually varies.
     */
    private const val MAX_PER_TEMPLATE: Int = 20

    /** Share of generated items reserved for evaluation, taken per (category, ambiguity) cell. */
    private const val HOLDOUT_EVERY_NTH: Int = 4

    // ---------------------------------------------------------------------------------------- slots

    /** Bodily sensations. Everyday and mild; none of them names a condition or an intense state. */
    private val SENSATIONS = listOf(
        "tensión", "calor", "peso", "cosquilleo", "presión", "ligereza", "frío", "hormigueo",
    )

    private val PLACES = listOf(
        "los hombros", "el pecho", "el estómago", "la mandíbula",
        "las manos", "la garganta", "la espalda", "las piernas",
    )

    /** Self-descriptions that are still a state, not a verdict. */
    private val STATE_ADJECTIVES = listOf(
        "inquieto", "despejado", "cansado", "sereno", "acelerado", "pesado", "ligero", "espeso",
    )

    /** Predictions and appraisals: interpretations, never observables. */
    private val PREDICTIONS = listOf(
        "esto saldrá mal", "no llegaré a tiempo", "es demasiado", "no funcionará",
        "todos lo notan", "debería ser sencillo", "voy lento", "me falta práctica",
        "no era el momento", "va a repetirse", "esto no cuenta", "lo estoy haciendo peor",
        "queda mucho", "no vale la pena", "así no avanzo", "esto se alarga",
    )

    /**
     * Actions in both tenses. The pairing IS the impulse↔fact axis: the same verb is an urge in the infinitive
     * and an observable once it has happened, so the verb itself can never be the cue.
     */
    private val ACTIONS = listOf(
        "cerrar la aplicación" to "cerrado la aplicación",
        "dejarlo" to "dejado el bloque",
        "protestar" to "protestado",
        "saltarme este paso" to "saltado un paso",
        "responder ya" to "respondido dos veces",
        "comprobarlo otra vez" to "comprobado la respuesta",
        "posponerlo" to "pospuesto el bloque",
        "cambiar de ejercicio" to "cambiado de ejercicio",
        "apurarme" to "acelerado el ritmo",
        "revisarlo entero" to "revisado la lista",
        "levantarme" to "levantado la vista",
        "escribir una nota" to "escrito una nota",
        "mirar el reloj" to "mirado el reloj",
        "bajar el volumen" to "bajado el volumen",
        "parar un momento" to "parado un momento",
        "repasar el enunciado" to "repasado el enunciado",
    )

    /** Verdicts about how something went — interpretations wearing the clothes of a report. */
    private val VERDICTS = listOf("regular", "mejor de lo previsto", "flojo", "como siempre", "cuesta arriba", "bien")

    /** Readings anyone could check: the fact side of the thought↔fact axis. */
    private val MEASURES = listOf(
        "seis minutos", "dos rondas", "tres intentos", "medio bloque", "cuatro turnos", "un minuto",
    )

    /** Plural-only readings, so `Quedan …` stays grammatical for every slot it can take. */
    private val COUNTS = listOf(
        "dos rondas", "tres intentos", "cuatro turnos", "seis minutos", "cinco preguntas", "ocho segundos",
    )

    /**
     * Things on screen. Shared between the observable template and the verdict template below, which is what
     * makes the subject non-diagnostic: `La sesión lleva ocho minutos` is a reading, `La sesión no aporta nada`
     * is an opinion, and only the predicate tells them apart.
     */
    private val SUBJECTS = listOf(
        "El temporizador", "La pantalla", "El bloque", "La lista",
        "El contador", "La barra", "El texto", "La ventana",
        "El botón", "La sesión", "El aviso", "La tarjeta",
        "El icono", "La fila", "El título", "La casilla",
    )

    /** What each subject shows, paired by index with [SUBJECTS]. Checkable by anyone looking at the screen. */
    private val READINGS = listOf(
        "marca diez segundos", "muestra un aviso", "ha terminado", "tiene cuatro entradas",
        "va por la quinta", "está a la mitad", "ocupa dos líneas", "sigue abierta",
        "está en gris", "lleva ocho minutos", "ha desaparecido", "muestra cuatro opciones",
        "está arriba", "tiene tres elementos", "ocupa una línea", "está marcada",
    )

    /**
     * Appraisals of the same subjects. Deliberately gender-neutral so any predicate can follow any subject
     * without the grammar itself leaking which template produced the phrase.
     */
    private val SUBJECT_VERDICTS = listOf(
        "no tiene sentido", "no ayuda nada", "es un problema", "no aporta nada",
        "está de más", "no encaja aquí", "sobra aquí", "es lo de menos",
    )

    /** Observable statements, assembled by index: subject plus what it shows. */
    private val OBSERVABLES = SUBJECTS.mapIndexed { i, subject -> "$subject ${READINGS[i]}" }

    // ------------------------------------------------------------------------------------ templates

    private class Template(
        val category: EmotionCategory,
        val ambiguity: Int,
        val axis: ConfusionAxis?,
        val first: List<String>,
        val second: List<String>,
        val build: (String, String) -> String,
    )

    private val NONE = listOf("")

    private val TEMPLATES: List<Template> = listOf(
        // --- INTERNAL_STATE, prototypical -------------------------------------------------------
        Template(EmotionCategory.INTERNAL_STATE, 0, null, SENSATIONS, PLACES) { s, p -> "Siento $s en $p" },
        Template(EmotionCategory.INTERNAL_STATE, 0, null, SENSATIONS, PLACES) { s, p -> "Noto $s en $p" },
        Template(EmotionCategory.INTERNAL_STATE, 0, null, SENSATIONS, PLACES) { s, p -> "Hay $s en $p" },
        // --- INTERNAL_STATE, boundary with THOUGHT ----------------------------------------------
        // "Me noto inquieto" sounds like a verdict about oneself, but it reports a state.
        Template(EmotionCategory.INTERNAL_STATE, 1, ConfusionAxis.STATE_THOUGHT, STATE_ADJECTIVES, NONE) { a, _ ->
            "Me noto $a"
        },
        Template(EmotionCategory.INTERNAL_STATE, 1, ConfusionAxis.STATE_THOUGHT, SENSATIONS, NONE) { s, _ ->
            "Siento $s ahora mismo"
        },

        // --- THOUGHT, prototypical --------------------------------------------------------------
        Template(EmotionCategory.THOUGHT, 0, null, PREDICTIONS, NONE) { p, _ -> "Creo que $p" },
        Template(EmotionCategory.THOUGHT, 0, null, PREDICTIONS, NONE) { p, _ -> "Pienso que $p" },
        // --- THOUGHT, boundary with INTERNAL_STATE ----------------------------------------------
        // The state verb carrying an interpretation: the phrase that breaks "Siento ⇒ estado".
        Template(EmotionCategory.THOUGHT, 1, ConfusionAxis.STATE_THOUGHT, PREDICTIONS, NONE) { p, _ ->
            "Siento que $p"
        },
        Template(EmotionCategory.THOUGHT, 1, ConfusionAxis.STATE_THOUGHT, PREDICTIONS, NONE) { p, _ ->
            "Noto que $p"
        },
        // --- THOUGHT, boundary with FACT --------------------------------------------------------
        Template(EmotionCategory.THOUGHT, 1, ConfusionAxis.THOUGHT_FACT, VERDICTS, NONE) { v, _ ->
            "Esto ha ido $v"
        },
        // The hardest and most useful item type in the bank: the SAME subject as an observable, followed by an
        // opinion instead of a reading. Without it, no phrase starting with a screen object is ever a thought,
        // and the whole `El…`/`La…` family becomes a free giveaway for FACT.
        Template(EmotionCategory.THOUGHT, 1, ConfusionAxis.THOUGHT_FACT, SUBJECTS, SUBJECT_VERDICTS) { s, v ->
            "$s $v"
        },

        // --- IMPULSE, prototypical --------------------------------------------------------------
        Template(EmotionCategory.IMPULSE, 0, null, ACTIONS.map { it.first }, NONE) { a, _ -> "Quiero $a" },
        Template(EmotionCategory.IMPULSE, 0, null, ACTIONS.map { it.first }, NONE) { a, _ -> "Me dan ganas de $a" },
        Template(EmotionCategory.IMPULSE, 0, null, ACTIONS.map { it.first }, NONE) { a, _ ->
            "Tengo el impulso de $a"
        },
        Template(EmotionCategory.IMPULSE, 0, null, ACTIONS.map { it.first }, NONE) { a, _ -> "Me apetece $a" },
        // --- IMPULSE, boundary with FACT --------------------------------------------------------
        // Reads like a report about the world; it is still a tendency that has not happened.
        Template(EmotionCategory.IMPULSE, 1, ConfusionAxis.IMPULSE_FACT, ACTIONS.map { it.first }, NONE) { a, _ ->
            "Estoy a punto de $a"
        },

        // --- FACT, prototypical -----------------------------------------------------------------
        Template(EmotionCategory.FACT, 0, null, OBSERVABLES, NONE) { o, _ -> o },
        Template(EmotionCategory.FACT, 0, null, ACTIONS.map { it.second }, NONE) { a, _ -> "Ya he $a" },
        Template(EmotionCategory.FACT, 0, null, COUNTS, NONE) { c, _ -> "Quedan $c" },
        // --- FACT, boundary with IMPULSE --------------------------------------------------------
        // The same verbs as the urge templates. "Acabo de X" even keeps the infinitive, so neither the verb nor
        // its form can decide the category: only whether it already happened.
        Template(EmotionCategory.FACT, 1, ConfusionAxis.IMPULSE_FACT, ACTIONS.map { it.second }, NONE) { a, _ ->
            "He $a"
        },
        Template(EmotionCategory.FACT, 1, ConfusionAxis.IMPULSE_FACT, ACTIONS.map { it.first }, NONE) { a, _ ->
            "Acabo de $a"
        },
        // --- FACT, boundary with THOUGHT --------------------------------------------------------
        // Same opening as the verdict template, with a reading instead of an appraisal after it.
        Template(EmotionCategory.FACT, 1, ConfusionAxis.THOUGHT_FACT, MEASURES, NONE) { m, _ ->
            "Esto ha durado $m"
        },
    )

    // ------------------------------------------------------------------------------------ assembly

    /**
     * The generated items, in a fixed order, excluding anything the authored seed already contains.
     *
     * Every phrase is unique across the whole bank. A phrase appearing under two categories would have no correct
     * answer, so it would be manufacturing noise and calling it difficulty.
     */
    val GENERATED: List<EmotionContent.SeparatorItem> by lazy { build() }

    /** Authored seed plus generated extension — the bank the exercise draws from. */
    val ALL: List<EmotionContent.SeparatorItem> by lazy { EmotionContent.SEPARATOR_ITEMS + GENERATED }

    private fun build(): List<EmotionContent.SeparatorItem> {
        val seen = EmotionContent.SEPARATOR_ITEMS.mapTo(mutableSetOf()) { it.phrase }
        val perCell = mutableMapOf<Pair<EmotionCategory, Int>, Int>()
        val out = mutableListOf<EmotionContent.SeparatorItem>()
        TEMPLATES.forEach { template ->
            val cap = minOf(MAX_PER_TEMPLATE, template.first.size * template.second.size)
            for (k in 0 until cap) {
                val phrase = template.build(template.first[k % template.first.size], secondSlot(template, k))
                if (!seen.add(phrase)) continue
                val cell = template.category to template.ambiguity
                val indexInCell = perCell.getOrElse(cell) { 0 }
                perCell[cell] = indexInCell + 1
                out += EmotionContent.SeparatorItem(
                    phrase = phrase,
                    category = template.category,
                    // Reserved per (category, ambiguity) cell rather than off the top of the list, so the held-out
                    // partition carries the same mix of prototypical and boundary items as the trained one. A
                    // holdout made mostly of boundary items would be a harder task than training, and the gap
                    // between them would read as a transfer failure that is really a difficulty difference.
                    isEval = indexInCell % HOLDOUT_EVERY_NTH == HOLDOUT_EVERY_NTH - 1,
                    ambiguity = template.ambiguity,
                    axis = template.axis,
                )
            }
        }
        return out
    }

    /**
     * Walks the second slot on a shifting diagonal so a capped take spreads across the grid.
     *
     * Advancing both slots in lockstep would only ever pair index i with index i, so the first `first.size` draws
     * would be the diagonal and every draw after it a repeat of that same diagonal — the dedupe would silently
     * discard most of the cap and the bank would come out a fraction of its intended size.
     */
    private fun secondSlot(template: Template, k: Int): String =
        template.second[(k / template.first.size + k) % template.second.size]
}
