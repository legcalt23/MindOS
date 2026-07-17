---
title: "Near vs far transfer: taxonomy and why far transfer is rare"
authors: "Barnett, S. M.; Ceci, S. J.; Sala, G.; Gobet, F."
year: "2002–2019"
tier: T1
domains: [attention, working_memory, processing_speed, logical_reasoning, learning_strategy]
constructs: [transfer, near_transfer, far_transfer, learning_specificity, domain_generality]
paradigms: [chess, music, working_memory, video_games]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt
evidence_level: strong
---

## (a) Tesis central

La **transferencia** —aplicar lo aprendido en un contexto a otro distinto— se organiza en un continuo:
**cercana** (contexto muy similar al entrenado) es común; **lejana** (contexto/dominio distinto) es
**rara**. La taxonomía de Barnett & Ceci (2002) formaliza las dimensiones de distancia (contenido,
contexto físico/social, modalidad, tiempo). Los meta-análisis de Sala & Gobet sobre ajedrez, música y
memoria de trabajo confirman: el entrenamiento cognitivo produce transferencia cercana pero **no lejana**.

## (b) Claims RESPALDADOS

- La transferencia decae con la distancia entre lo entrenado y lo evaluado; la cercana es la norma
  alcanzable. [taxonomía + meta-análisis]
- Ajedrez, música y entrenamiento de WM NO producen transferencia lejana a capacidades cognitivas
  generales o académicas cuando se controla bien. [meta-análisis — Sala & Gobet 2017, 2019]
- La transferencia es más probable cuando el dominio entrenado y el objetivo comparten **elementos
  idénticos / procesos comunes** (principio thorndikeano, respaldado por la evidencia moderna). [teoría
  + meta-análisis]

## (c) Claims NO respaldados o refutados

- **No** hay base para prometer que entrenar una habilidad cognitiva mejore capacidades cognitivas
  distintas o el rendimiento en la vida (transferencia lejana).
- La intuición de "ejercitar el cerebro como un músculo general" (disciplina formal) está **refutada**
  desde hace un siglo (Thorndike & Woodworth, 1901) y sigue sin apoyo.
- Excepciones aparentes (ajedrecistas listos) reflejan **selección** (los listos eligen ajedrez), no
  transferencia causal.

## (d) Estado de replicación / críticas

Es uno de los principios más sólidos y antiguos de la psicología del aprendizaje. La transferencia
*cercana/dentro de dominio* SÍ ocurre (ver [[F07_spatial_reasoning_training_uttal]],
[[T04_active_trial_speed_training]]): el matiz correcto no es "nada transfiere" sino "transfiere lo
cercano y específico, no lo lejano y general". Distinguir ambos es la competencia central para no
sobre-prometer.

## (e) Implicaciones de diseño para MindBoost

- **Es el principio organizador de la frontera de claims.** Todo claim de MindBoost debe caer del lado
  de la transferencia *cercana* (progreso en tareas practicadas y muy similares) y nunca de la *lejana*.
- Guía el diseño de ejercicios: si se quiere que la práctica sirva para algo concreto, diseñar la tarea
  para que **comparta elementos** con ese objetivo (no confiar en generalización mágica). Un ejercicio
  de Lenguaje que entrena "evaluar fuentes" transfiere, como mucho, a evaluar fuentes.
- Fundamenta la arquitectura del `MindBoostProfileAggregator`: dimensiones separadas por proceso
  entrenado, sin un salto a "capacidad general".
- **Claim permitido:** "mejoras en la tarea y en tareas muy parecidas". **Prohibido:** todo salto a
  dominios distintos, estudio, trabajo, inteligencia.

## (f) Referencias

- Barnett, S. M., & Ceci, S. J. (2002). When and where do we apply what we learn? A taxonomy for far
  transfer. *Psychological Bulletin, 128*(4), 612–637.
- Sala, G., & Gobet, F. (2017). Does far transfer exist? Negative evidence from chess, music, and
  working memory training. *Current Directions in Psychological Science, 26*(6), 515–520.
- Sala, G., & Gobet, F. (2019). Cognitive training does not enhance general cognition. *Trends in
  Cognitive Sciences, 23*(1), 9–20.
- Thorndike, E. L., & Woodworth, R. S. (1901). The influence of improvement in one mental function upon
  the efficiency of other functions. *Psychological Review, 8*(3), 247–261.
