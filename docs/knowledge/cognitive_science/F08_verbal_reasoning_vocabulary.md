---
title: "Verbal reasoning, vocabulary and crystallized knowledge"
authors: "Horn, J. L.; Perfetti, C. A.; Stanovich, K. E."
year: "1980–2015"
tier: T2
domains: [verbal_reasoning]
constructs: [crystallized_intelligence, vocabulary, reading_comprehension, verbal_reasoning]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/exercises/LanguageExerciseCatalog.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt
evidence_level: moderate
---

## (a) Tesis central

El razonamiento verbal se apoya en el **conocimiento cristalizado** (Gc): vocabulario, conocimiento
del mundo y habilidades lingüísticas adquiridas por exposición y práctica. A diferencia de Gf, Gc
**crece a lo largo de la vida** y es sensible a la instrucción y la lectura. La comprensión y el
razonamiento sobre texto son habilidades componibles y practicables, no un rasgo fijo.

## (b) Claims RESPALDADOS

- El vocabulario y el conocimiento verbal son un factor CHC (Gc) robusto que crece con la edad y la
  exposición. [factor-analítico + longitudinal]
- La comprensión lectora depende de habilidades componentes entrenables: decodificación, inferencia,
  integración, evaluación de argumentos y fuentes. [correlacional/experimental educativo]
- La práctica de estrategias de comprensión (identificar estructura del argumento, distinguir premisa
  de conclusión, evaluar evidencia) mejora el desempeño en esas tareas. [experimental educativo,
  efectos moderados]

## (c) Claims NO respaldados o refutados

- **No** existe evidencia de que ejercicios verbales eleven la inteligencia verbal general ni Gf; se
  entrena la *habilidad específica* practicada (analizar argumentos, comprimir texto, evaluar fuentes).
- Ganar en una tarea de "cirugía semántica" o "rayos-X de argumento" **no** transfiere automáticamente
  a "razonas mejor en general": la transferencia lejana verbal no está demostrada.
- Gc es en gran medida producto de **exposición acumulada**; una app no sustituye la lectura amplia.

## (d) Estado de replicación / críticas

El estatus de Gc es sólido. La literatura de "entrenamiento del razonamiento" (crítico/argumentativo)
muestra mejoras reales pero **específicas del dominio y del tipo de problema** practicado, con
transferencia limitada. La honestidad exige presentar estos ejercicios como práctica de habilidades
concretas de pensamiento sobre texto, no como "subir tu razonamiento verbal".

## (e) Implicaciones de diseño para MindBoost

- El módulo **Lenguaje** (`LanguageExerciseCatalog`: Evidence Lock, Semantic Surgery, Compression
  Ladder, Argument X-Ray, Scope & Negation, Source Triage) entrena habilidades componentes de
  razonamiento sobre texto — encaje honesto con la evidencia de que *esas habilidades específicas* son
  practicables.
- **Claim permitido:** "práctica de analizar argumentos, evaluar evidencia y precisión al leer".
  **Prohibido:** "mejora tu razonamiento verbal / tu inteligencia verbal" como capacidad transferible.
- La dimensión `VERBAL` del perfil debe mantenerse en `CALIBRATING` mientras no haya banco validado y
  señal suficiente (coherente con la nota del repo: Lenguaje sin store de producción ⇒ VERBAL
  calibrando). Reportar progreso por *habilidad practicada*, no un índice verbal global.
- Recomendación de coach: acompañar la práctica con lectura real (Gc crece por exposición) — sin
  prometer transferencia.

## (f) Referencias

- Horn, J. L., & Cattell, R. B. (1967). Age differences in fluid and crystallized intelligence.
  *Acta Psychologica, 26*, 107–129.
- Perfetti, C. A., & Stafura, J. (2014). Word knowledge in a theory of reading comprehension.
  *Scientific Studies of Reading, 18*(1), 22–37.
- Stanovich, K. E., West, R. F., & Toplak, M. E. (2016). *The rationality quotient.* MIT Press.
