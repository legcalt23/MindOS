---
title: "Ecological rationality: fast-and-frugal heuristics"
authors: "Gigerenzer, G.; Todd, P. M.; Goldstein, D. G."
year: "1996–2011"
tier: T2
domains: [decision_making]
constructs: [heuristics, ecological_rationality, bounded_rationality, recognition_heuristic, less_is_more]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring/ReversibilityScorer.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain
evidence_level: moderate
---

## (a) Tesis central

Frente al programa "heurísticos y sesgos" (que ve las heurísticas como fuentes de error), Gigerenzer
sostiene que las **heurísticas simples y rápidas** ("fast-and-frugal") son a menudo **adaptativas**: en
entornos de incerteñidad e información limitada pueden igualar o superar a modelos complejos ("less is
more"). La racionalidad es **ecológica**: una heurística es buena o mala según encaje con la estructura
del entorno, no contra un ideal lógico abstracto.

## (b) Claims RESPALDADOS

- Heurísticas simples (take-the-best, recognition heuristic) pueden ser sorprendentemente precisas y
  robustas frente al sobreajuste en entornos inciertos. [simulación + experimental]
- La racionalidad debe evaluarse frente al entorno real, no solo contra la lógica formal (bounded
  rationality de Simon). [teoría + evidencia]
- Representar la incertidumbre en **frecuencias naturales** (en vez de probabilidades) mejora el
  razonamiento bayesiano de la gente (incl. profesionales). [experimental — Gigerenzer & Hoffrage]

## (c) Claims NO respaldados o refutados

- No implica que "las heurísticas siempre ganan": su ventaja es **condicional al entorno**; en entornos
  con datos abundantes y estables, los modelos complejos pueden superar.
- El debate con Kahneman/Tversky no está "resuelto" a favor de un bando; ambos capturan verdades
  parciales (sesgos reales existen; heurísticas útiles también).
- No respalda un claim de "entrena y elimina sesgos"; sí respalda enseñar *representaciones* que mejoran
  el juicio.

## (d) Estado de replicación / críticas

Programa influyente y provocador. Críticas: algunos resultados "less-is-more" dependen de condiciones
específicas de simulación; el recognition heuristic tiene límites. El hallazgo más aplicable y replicado
es que **las frecuencias naturales mejoran el razonamiento probabilístico**.

## (e) Implicaciones de diseño para MindBoost

- **Informa el diseño del módulo Decisiones:** el **triaje de reversibilidad** (`ReversibilityScorer`)
  es en sí una heurística fast-and-frugal adaptativa (¿es reversible? decide rápido; ¿irreversible?
  delibera) — encaje directo y honesto con la racionalidad ecológica.
- **Presentar probabilidades como frecuencias naturales** en ejercicios de decisión/Bayes mejora la
  comprensión del usuario (evidencia T1) — recomendación de diseño accionable para Bayes Arena.
- **Honestidad:** enseñar buenas representaciones y heurísticas adaptadas al contexto, no prometer
  "pensamiento perfecto" ni eliminación de sesgos.
- **Claim permitido:** "práctica de decidir con heurísticas adecuadas al contexto y con la información
  bien representada". **Prohibido:** "te hacemos un decisor racional / sin sesgos".

## (f) Referencias

- Gigerenzer, G., & Goldstein, D. G. (1996). Reasoning the fast and frugal way: Models of bounded
  rationality. *Psychological Review, 103*(4), 650–669.
- Gigerenzer, G., & Hoffrage, U. (1995). How to improve Bayesian reasoning without instruction:
  Frequency formats. *Psychological Review, 102*(4), 684–704.
- Gigerenzer, G., Todd, P. M., & the ABC Research Group. (1999). *Simple heuristics that make us smart.*
  Oxford University Press.
