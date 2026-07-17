---
title: "Deliberate practice and the acquisition of expert performance (and its limits)"
authors: "Ericsson, K. A.; Macnamara, B. N.; Hambrick, D. Z."
year: "1993–2014"
tier: T1
domains: [learning_strategy]
constructs: [deliberate_practice, expertise, feedback, performance_ceiling, individual_differences]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/adaptive
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/orchestration/MindBoostProfileOrchestrator.kt
evidence_level: moderate
---

## (a) Tesis central

Ericsson propuso que el rendimiento experto se adquiere sobre todo mediante **práctica deliberada**:
práctica estructurada, con objetivos específicos, feedback inmediato, en el límite de la propia
competencia y con repetición focalizada en las debilidades — no mera repetición ni experiencia acumulada.
El meta-análisis de Macnamara et al. (2014) matiza fuertemente la magnitud: la práctica deliberada
explica una fracción **importante pero limitada** de la varianza en desempeño (mucho menos que el
"10 000 horas" popularizado).

## (b) Claims RESPALDADOS

- La práctica **estructurada con feedback y en el límite de la competencia** es más efectiva que la
  práctica no dirigida o la mera experiencia. [experimental/observacional en dominios de expertise]
- El feedback inmediato y los objetivos específicos son ingredientes activos del aprendizaje efectivo.
  [convergente con L01/L03]

## (c) Claims NO respaldados o refutados

- **REFUTADO el determinismo de las "10 000 horas":** Macnamara et al. (2014) hallaron que la práctica
  deliberada explica ~26% de la varianza en juegos, ~21% en música, ~18% en deportes, **~4% en
  educación y ~1% en profesiones** — deja mucho a aptitud, edad de inicio y otros factores.
  [meta-análisis]
- **No** implica que "cualquiera puede llegar a experto solo con horas"; las diferencias individuales y
  la aptitud importan.
- No es un claim de transferencia cognitiva general; es sobre adquirir una habilidad específica.

## (d) Estado de replicación / críticas

Debate activo y a veces acalorado (Ericsson disputó el meta-análisis de Macnamara). El consenso
razonable: los **principios de diseño** de la práctica deliberada (objetivos, feedback, dificultad en el
límite, foco en debilidades) son valiosos y bien respaldados; la **magnitud** de su contribución al
expertise fue exagerada por la divulgación (Gladwell). Separar principios (sólidos) de la promesa
determinista (falsa) es la lección.

## (e) Implicaciones de diseño para MindBoost

- **Ingredientes de la práctica deliberada = arquitectura de MindBoost:** objetivos claros por
  ejercicio, feedback inmediato, dificultad adaptativa en el límite de la competencia
  ([[L03_desirable_difficulties_bjork]]), y foco en debilidades (el planner puede priorizar dimensiones
  más débiles del perfil). Buen respaldo de diseño.
- **Honestidad sobre el techo:** MindBoost NO debe prometer "conviértete en experto/genio con horas de
  práctica". La práctica mejora el desempeño en lo practicado, con límites individuales.
- El `MindBoostProfileOrchestrator`/planner puede usar el perfil para dirigir la práctica a puntos
  débiles (deliberate), midiendo progreso frente a la línea base propia.
- **Claim permitido:** "práctica estructurada con feedback, enfocada en tus puntos débiles".
  **Prohibido:** "10 000 horas te hacen experto" / promesas de talento garantizado.

## (f) Referencias

- Ericsson, K. A., Krampe, R. T., & Tesch-Römer, C. (1993). The role of deliberate practice in the
  acquisition of expert performance. *Psychological Review, 100*(3), 363–406.
- Macnamara, B. N., Hambrick, D. Z., & Oswald, F. L. (2014). Deliberate practice and performance in
  music, games, sports, education, and professions: A meta-analysis. *Psychological Science, 25*(8),
  1608–1618.
