---
title: "Arousal, stress and emotion regulation for performance"
authors: "Yerkes, R. M.; Dodson, J. D.; Gross, J. J.; Eysenck, M. W."
year: "1908–2011"
tier: T1
domains: [emotional_regulation_for_performance]
constructs: [arousal_performance, stress, cognitive_test_anxiety, emotion_regulation, reappraisal, choking_under_pressure]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/catalog/EmotionExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: moderate
---

## (a) Tesis central

El arousal y el estrés modulan el rendimiento cognitivo: demasiado poca o demasiada activación degrada
la ejecución (la idea popular "U invertida" de Yerkes-Dodson). El estrés y la ansiedad consumen recursos
de memoria de trabajo (teoría del control atencional de Eysenck: la ansiedad daña la eficiencia del
procesamiento antes que la efectividad). La **regulación emocional** (modelo de proceso de Gross:
reevaluación cognitiva > supresión) permite gestionar ese estado para rendir mejor bajo presión.

## (b) Claims RESPALDADOS

- El estrés/ansiedad ocupa recursos de memoria de trabajo y degrada tareas exigentes de control
  atencional. [experimental — Attentional Control Theory, Eysenck et al. 2007]
- La **reevaluación cognitiva** es una estrategia de regulación emocional más adaptativa que la
  **supresión** (mejor afecto, mejor memoria, menor coste fisiológico). [experimental — Gross]
- La presión temporal/evaluativa puede provocar "choking": deterioro del rendimiento en tareas bien
  aprendidas. [experimental — Beilock]

## (c) Claims NO respaldados o refutados

- La "U invertida" de Yerkes-Dodson está **sobre-simplificada y sobre-citada**: el original era sobre
  ratones y dificultad de tarea; la relación arousal-rendimiento depende de la tarea y no es una ley
  universal limpia.
- MindBoost **no** trata ansiedad clínica ni es terapia: entrena regulación emocional *para el
  rendimiento* en tareas, no como intervención de salud mental.
- La inducción emocional debe ser **leve**; nunca provocar malestar real (límite del charter).

## (d) Estado de replicación / críticas

La Attentional Control Theory y el modelo de Gross tienen buen respaldo. Yerkes-Dodson es un caso de
"zombie científico" citado más allá de su evidencia — usarlo solo como metáfora, no como ley. El choking
bajo presión está replicado en dominios de habilidad.

## (e) Implicaciones de diseño para MindBoost

- **Fundamenta el módulo Emociones** (`EmotionExerciseCatalog`: EM1 Pausa, anti-impulso, separación bajo
  presión) y la dimensión `EMOTIONAL_REGULATION`: entrenar reevaluación/pausa antes de responder tiene
  respaldo como estrategia *para el rendimiento*. Encaja con el diseño de inducción **leve** y cifrado
  local del repo.
- Conecta con **E13 Presión** (Enfoque, deadline con coste por error, `PRESSURE_DELTA`): medir cómo
  cambia tu precisión bajo presión es un índice honesto de regulación para el rendimiento — sin claim
  clínico.
- **Seguridad (Regla anti-gaming/charter):** nunca penalizar por señal emocional/frustración; una
  salida por seguridad emocional no debe castigar. La regulación se entrena, no se sanciona.
- **Claim permitido:** "práctica de pausar y regular tu estado para rendir bajo presión". **Prohibido:**
  claims de tratar ansiedad, estrés clínico o salud mental.

## (f) Referencias

- Eysenck, M. W., Derakshan, N., Santos, R., & Calvo, M. G. (2007). Anxiety and cognitive performance:
  Attentional control theory. *Emotion, 7*(2), 336–353.
- Gross, J. J. (1998). The emerging field of emotion regulation: An integrative review. *Review of
  General Psychology, 2*(3), 271–299.
- Beilock, S. L., & Carr, T. H. (2001). On the fragility of skilled performance: What governs choking
  under pressure? *Journal of Experimental Psychology: General, 130*(4), 701–725.
- Corbett, B., & others; Yerkes, R. M., & Dodson, J. D. (1908). The relation of strength of stimulus to
  rapidity of habit-formation. *Journal of Comparative Neurology and Psychology, 18*, 459–482.
