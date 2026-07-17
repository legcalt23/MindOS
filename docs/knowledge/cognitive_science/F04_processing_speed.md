---
title: "Processing speed as a cognitive construct and its role in fluid ability"
authors: "Salthouse, T. A.; Jensen, A. R.; Kail, R."
year: "1996–2008"
tier: T1
domains: [processing_speed]
constructs: [processing_speed, mental_chronometry, cognitive_aging, gf_mediation]
paradigms: [choice_rt, simple_rt, digit_symbol, inspection_time]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing
evidence_level: strong
---

## (a) Tesis central

La velocidad de procesamiento —la rapidez con que se ejecutan operaciones cognitivas elementales— es
un factor psicométrico distinto (Gs en la taxonomía CHC) y un **mediador** clave: buena parte del
declive de la inteligencia fluida con la edad se explica estadísticamente por el enlentecimiento del
procesamiento (teoría de la velocidad de procesamiento de Salthouse). Se mide con cronometría mental
(tiempos de reacción, tiempo de inspección, sustitución dígito-símbolo).

## (b) Claims RESPALDADOS

- La velocidad de procesamiento es un factor identificable y separable en el análisis factorial de la
  cognición (Gs). [factor-analítico]
- El enlentecimiento del procesamiento media una fracción sustancial del declive relacionado con la
  edad en memoria de trabajo y razonamiento fluido. [longitudinal/correlacional — Salthouse]
- La velocidad de procesamiento en la infancia predice el desarrollo de la memoria de trabajo y el
  razonamiento. [correlacional evolutivo — Kail]
- La medida es sensible: la precisión de la base temporal y el control del compromiso
  velocidad-precisión (SATF) son determinantes de su validez. [metodológico — ver
  [[P03_reaction_time_methodology]]]

## (c) Claims NO respaldados o refutados

- **No** está demostrado que hacer más rápida la respuesta mediante práctica repetida eleve la
  inteligencia fluida: la práctica acelera la *tarea entrenada* (aprendizaje procedimental,
  automatización), no la velocidad de procesamiento subyacente de forma transferible.
- Una RT más rápida **puede** reflejar un cambio en el criterio velocidad-precisión (responder antes
  a costa de errar más), no una mejora real — sin controlar SATF, "más rápido" no significa "mejor".
- **No** se puede inferir velocidad de procesamiento válida de una medida con base temporal
  no-autoritativa (jitter de frame, timestamps de baja resolución): el ruido de medición domina la
  señal.

## (d) Estado de replicación / críticas

El estatus de la velocidad como factor CHC y su papel mediador en el envejecimiento son robustos. La
crítica es que "velocidad de procesamiento" agrupa procesos heterogéneos (velocidad perceptiva,
velocidad de decisión, velocidad motora) y que las tareas clásicas (dígito-símbolo) contaminan
velocidad con memoria de trabajo y habilidad motora. La modelización moderna (drift-diffusion)
descompone la RT en tasa de acumulación de evidencia, umbral y tiempo no-decisional, separando
"velocidad cognitiva" real de estrategia y motricidad.

## (e) Implicaciones de diseño para MindBoost

- El módulo **Velocidad** mide una señal RT-crítica; por eso el repo exige **base temporal
  autoritativa** (`CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`) antes de que una sesión alimente el perfil
  (`SpeedProfileSource`/`core/timing`). Esta ficha es la justificación científica: sin timebase
  válido, la medida de velocidad no es interpretable — de ahí que fallback (desktop/iOS/Android<34)
  quede jugable pero **excluido** del perfil `PROCESSING_SPEED`. Correcto y honesto.
- **Controlar SATF es obligatorio:** cualquier índice de velocidad debe reportarse junto a precisión
  o penalizar la respuesta rápida-imprecisa; "te has vuelto más rápido" sin precisión estable es un
  claim vacío.
- La descomposición drift-diffusion (tasa de deriva vs umbral) es el camino correcto para separar
  "velocidad cognitiva" de "estrategia arriesgada" — remit del psychometrician.
- **Claim permitido:** "progreso en tu velocidad de respuesta frente a tu propia línea base, con
  precisión controlada". **Prohibido:** "aumenta tu velocidad de procesamiento / tu agilidad mental"
  como capacidad general.

## (f) Referencias

- Salthouse, T. A. (1996). The processing-speed theory of adult age differences in cognition.
  *Psychological Review, 103*(3), 403–428.
- Kail, R., & Salthouse, T. A. (1994). Processing speed as a mental capacity. *Acta Psychologica,
  86*(2–3), 199–225.
- Jensen, A. R. (2006). *Clocking the mind: Mental chronometry and individual differences.* Elsevier.
- Ratcliff, R., & McKoon, G. (2008). The diffusion decision model. *Neural Computation, 20*(4),
  873–922.
