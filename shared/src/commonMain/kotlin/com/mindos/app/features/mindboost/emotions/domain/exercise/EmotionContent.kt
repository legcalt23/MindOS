package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory

/**
 * Authored, scanner-clean content banks for the v1 exercises. All copy is neutral, everyday,
 * non-intimate, non-moralizing and within the leve-only induction ceiling (no clinical/traumatic
 * content). The terminology gate scans this file. Banks are expandable (EM3 target = 80; v1 ships ≥30).
 */
object EmotionContent {

    /** A mild affective distractor word for EM1. [negative] marks a slightly negative tone (for congruency). */
    data class AffectiveWord(val text: String, val negative: Boolean)

    /** EM1 distractor bank: 24 mild words (12 slightly negative, 12 neutral/positive). Tone is never the answer. */
    val PAUSA_WORDS: List<AffectiveWord> = listOf(
        AffectiveWord("fallo", negative = true),
        AffectiveWord("error", negative = true),
        AffectiveWord("tarde", negative = true),
        AffectiveWord("prisa", negative = true),
        AffectiveWord("duda", negative = true),
        AffectiveWord("ruido", negative = true),
        AffectiveWord("molesto", negative = true),
        AffectiveWord("pesado", negative = true),
        AffectiveWord("torpe", negative = true),
        AffectiveWord("lío", negative = true),
        AffectiveWord("difícil", negative = true),
        AffectiveWord("lento", negative = true),
        AffectiveWord("espera", negative = false),
        AffectiveWord("ahora", negative = false),
        AffectiveWord("casi", negative = false),
        AffectiveWord("rápido", negative = false),
        AffectiveWord("alto", negative = false),
        AffectiveWord("ya", negative = false),
        AffectiveWord("justo", negative = false),
        AffectiveWord("fácil", negative = false),
        AffectiveWord("calma", negative = false),
        AffectiveWord("sigue", negative = false),
        AffectiveWord("vamos", negative = false),
        AffectiveWord("corre", negative = false),
    )

    /**
     * An EM3/EM10 classification item: a neutral phrase and its true category. [isEval] reserves it for holdout.
     * [ambiguity] is the difficulty lever (MC8u-B §2): 0 = prototypical/clear, 1 = deliberately confusable (near a
     * category boundary). EM3/EM10 include more ambiguity=1 items as the CATEGORY_AMBIGUITY / TEMPORAL_PRESSURE axis
     * rises, so higher difficulty changes the ACTUAL trials (harder discriminations), never just a number.
     */
    data class SeparatorItem(
        val phrase: String,
        val category: EmotionCategory,
        val isEval: Boolean,
        val ambiguity: Int = 0,
    )

    /**
     * EM3 bank: 40 items, 10 per category (7 training + 3 held-out eval each). Confusion axes are
     * deliberate (thought↔fact, impulse↔action). Training and eval pools are disjoint by phrase.
     */
    val SEPARATOR_ITEMS: List<SeparatorItem> = listOf(
        // INTERNAL_STATE (estado interno / sensación)
        SeparatorItem("Siento tensión en los hombros", EmotionCategory.INTERNAL_STATE, isEval = false),
        SeparatorItem("Noto el pecho apretado", EmotionCategory.INTERNAL_STATE, isEval = false),
        SeparatorItem("Me siento con energía", EmotionCategory.INTERNAL_STATE, isEval = false),
        SeparatorItem("Hay calma en mi cuerpo", EmotionCategory.INTERNAL_STATE, isEval = false),
        SeparatorItem("Siento cansancio", EmotionCategory.INTERNAL_STATE, isEval = false),
        SeparatorItem("Noto impaciencia", EmotionCategory.INTERNAL_STATE, isEval = false, ambiguity = 1),
        SeparatorItem("Me siento ligero", EmotionCategory.INTERNAL_STATE, isEval = false, ambiguity = 1),
        SeparatorItem("Hay inquietud en mí", EmotionCategory.INTERNAL_STATE, isEval = true),
        SeparatorItem("Siento alivio", EmotionCategory.INTERNAL_STATE, isEval = true),
        SeparatorItem("Noto algo de frustración", EmotionCategory.INTERNAL_STATE, isEval = true),
        // THOUGHT (pensamiento / interpretación)
        SeparatorItem("Esto va a salir mal", EmotionCategory.THOUGHT, isEval = false),
        SeparatorItem("Creo que no llegaré a tiempo", EmotionCategory.THOUGHT, isEval = false),
        SeparatorItem("Pienso que es injusto", EmotionCategory.THOUGHT, isEval = false),
        SeparatorItem("Supongo que saldrá regular", EmotionCategory.THOUGHT, isEval = false, ambiguity = 1),
        SeparatorItem("Imagino que fallaré otra vez", EmotionCategory.THOUGHT, isEval = false),
        SeparatorItem("Creo que no soy capaz de esto", EmotionCategory.THOUGHT, isEval = false),
        SeparatorItem("Pienso que es demasiado", EmotionCategory.THOUGHT, isEval = false, ambiguity = 1),
        SeparatorItem("Asumo que no funcionará", EmotionCategory.THOUGHT, isEval = true),
        SeparatorItem("Creo que todos lo notan", EmotionCategory.THOUGHT, isEval = true),
        SeparatorItem("Pienso que debería ser sencillo", EmotionCategory.THOUGHT, isEval = true),
        // IMPULSE (impulso / tendencia de acción)
        SeparatorItem("Quiero cerrar la aplicación", EmotionCategory.IMPULSE, isEval = false),
        SeparatorItem("Me dan ganas de rendirme", EmotionCategory.IMPULSE, isEval = false),
        SeparatorItem("Quiero responder ya mismo", EmotionCategory.IMPULSE, isEval = false, ambiguity = 1),
        SeparatorItem("Me apetece dejarlo", EmotionCategory.IMPULSE, isEval = false),
        SeparatorItem("Tengo el impulso de protestar", EmotionCategory.IMPULSE, isEval = false),
        SeparatorItem("Quiero saltarme este paso", EmotionCategory.IMPULSE, isEval = false),
        SeparatorItem("Me dan ganas de apurarme", EmotionCategory.IMPULSE, isEval = false, ambiguity = 1),
        SeparatorItem("Quiero comprobarlo otra vez", EmotionCategory.IMPULSE, isEval = true),
        SeparatorItem("Tengo el impulso de discutir", EmotionCategory.IMPULSE, isEval = true),
        SeparatorItem("Me apetece posponerlo", EmotionCategory.IMPULSE, isEval = true),
        // FACT (hecho observable)
        SeparatorItem("He fallado tres rondas", EmotionCategory.FACT, isEval = false),
        SeparatorItem("El temporizador marca diez segundos", EmotionCategory.FACT, isEval = false),
        SeparatorItem("He pulsado dos veces", EmotionCategory.FACT, isEval = false),
        SeparatorItem("Quedan cuatro ejercicios", EmotionCategory.FACT, isEval = false),
        SeparatorItem("He acertado la última", EmotionCategory.FACT, isEval = false, ambiguity = 1),
        SeparatorItem("La pantalla muestra un aviso", EmotionCategory.FACT, isEval = false, ambiguity = 1),
        SeparatorItem("He hecho cinco intentos", EmotionCategory.FACT, isEval = false),
        SeparatorItem("El bloque ha terminado", EmotionCategory.FACT, isEval = true),
        SeparatorItem("He tardado más que antes", EmotionCategory.FACT, isEval = true),
        SeparatorItem("Hay tres opciones en pantalla", EmotionCategory.FACT, isEval = true),
    )
}
