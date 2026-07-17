---
title: "Do 'brain-training' programs work? (consensus review)"
authors: "Simons, D. J.; Boot, W. R.; Charness, N.; Gathercole, S. E.; Chabris, C. F.; Hambrick, D. Z.; Stine-Morrow, E. A. L."
year: 2016
tier: T1
domains: [attention, working_memory, processing_speed, logical_reasoning, learning_strategy, decision_making]
constructs: [cognitive_training, transfer, far_transfer, methodological_rigor]
paradigms: [n_back, complex_span, speed_of_processing, dual_task]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt
evidence_level: strong
---

## (a) Tesis central

Revisión exhaustiva del comité editorial de *Psychological Science in the Public
Interest* sobre la evidencia del entrenamiento cognitivo comercial. Conclusión: hay evidencia
**abundante** de que practicar una tarea mejora esa tarea, **algo menos** de que mejora tareas muy
parecidas (transferencia cercana), y **poca o ninguna** de que mejore el rendimiento cognitivo
cotidiano o capacidades generales (transferencia lejana). Es el documento de referencia que fija el
listón probatorio para cualquier claim de entrenamiento cognitivo.

## (b) Claims RESPALDADOS

- Practicar una tarea cognitiva mejora el rendimiento en esa tarea. [consenso]
- Existe transferencia cercana ocasional a tareas muy similares a la entrenada. [revisión] (evidencia
  limitada y mixta)
- La mayoría de estudios pro-"brain training" adolecen de fallos metodológicos: falta de grupos de
  control activos, expectativas/placebo, dependencia de una sola medida de resultado, muestras
  pequeñas. [revisión]

## (c) Claims NO respaldados o refutados

- **No** hay base sólida para afirmar que un programa de entrenamiento cognitivo mejore la inteligencia
  general, el rendimiento académico/laboral, o funciones cognitivas cotidianas (transferencia lejana).
- **No** basta con mostrar mejora en la tarea entrenada para afirmar un beneficio cognitivo: hay que
  demostrar transferencia con controles activos, medidas múltiples y pre-registro.
- Las mejoras auto-percibidas ("me siento más agudo") son especialmente vulnerables al efecto placebo
  y a las expectativas — no cuentan como evidencia.

## (d) Estado de replicación / críticas

Es un statement de consenso, contrapunto de un statement pro-industria firmado por otros
investigadores (el propio campo estaba dividido en 2014–2016; ver [[T06_stanford_consensus_open_letter]]).
Su fuerza está en el estándar metodológico que propone: **control activo, pre-registro, múltiples
medidas de transferencia, seguimiento**. Ese estándar sigue vigente y raramente lo cumplen los
productos comerciales.

## (e) Implicaciones de diseño para MindBoost

- **Es la fuente primaria de la frontera de claims de MindBoost.** Justifica que el producto NO prometa
  transferencia lejana y que el `ForbiddenLexicon` prohíba el lenguaje de "mejora tu cerebro / tu
  inteligencia / tu rendimiento en la vida".
- Valida el diseño honesto del repo: medir **progreso frente a la propia línea base en las tareas
  entrenadas** (transferencia cercana, lo único defendible) en vez de prometer beneficios generales.
- El estándar metodológico (control activo, múltiples medidas, pre-registro) es la vara para cualquier
  claim empírico futuro de MindBoost: sin eso, un "funciona" no es publicable ni afirmable.
- Refuerza `null ≠ 0` y el `ScoreStateMachine`: ante datos insuficientes, `CALIBRATING`, nunca una
  mejora fabricada.
- **Claim permitido:** "mejoras en las tareas que practicas, frente a tu línea base". **Prohibido:**
  cualquier claim de transferencia a la vida, estudio, trabajo o inteligencia general.

## (f) Referencias

- Simons, D. J., Boot, W. R., Charness, N., Gathercole, S. E., Chabris, C. F., Hambrick, D. Z., &
  Stine-Morrow, E. A. L. (2016). Do "brain-training" programs work? *Psychological Science in the
  Public Interest, 17*(3), 103–186.
