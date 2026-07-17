---
title: "Calibration of probability judgments and the Brier score"
authors: "Lichtenstein, S.; Fischhoff, B.; Brier, G. W.; Murphy, A. H.; Tetlock, P. E."
year: "1950–2015"
tier: T1
domains: [decision_making, metacognition]
constructs: [calibration, overconfidence, brier_score, proper_scoring_rules, forecasting]
paradigms: [confidence_calibration, probabilistic_forecasting]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain
evidence_level: strong
---

## (a) Tesis central

La **calibración** mide si las probabilidades subjetivas coinciden con las frecuencias reales: si dices
"70% seguro" muchas veces, deberías acertar ~70% de ellas. Las personas suelen estar **mal calibradas**
(típicamente sobreconfiadas). El **Brier score** (una regla de puntuación *propia*) evalúa la calidad de
predicciones probabilísticas penalizando tanto el error como la sobreconfianza. Tetlock mostró que la
calibración es **entrenable** (superforecasters).

## (b) Claims RESPALDADOS

- La sobreconfianza es un sesgo de calibración robusto y generalizado. [experimental, replicado —
  Lichtenstein & Fischhoff]
- El Brier score es una **regla de puntuación propia** (proper scoring rule): incentiva reportar la
  probabilidad verdadera; no se puede "gamear" exagerando confianza. [teoría matemática — Brier 1950]
- La calibración/precisión predictiva **mejora con práctica y feedback estructurado** (Good Judgment
  Project / superforecasters). [longitudinal — Tetlock]
- Descomposición del Brier (partición de Murphy): `BS = fiabilidad − resolución + incertidumbre` — la
  resolución se **resta** (mayor resolución baja el Brier); permite diagnosticar *qué* falla en el
  juicio. [teoría — Murphy 1973]

## (c) Claims NO respaldados o refutados

- La calibración es **específica de dominio**: ser buen predictor en un área no garantiza serlo en otra;
  no es un rasgo de "buen juicio" transferible universal.
- Entrenar calibración mejora *el juicio probabilístico practicado*, no la inteligencia general.
- Un único número de confianza sin comparación con el acierto no dice nada — se necesita muestra y
  contraste.

## (d) Estado de replicación / críticas

Fundamentos sólidos (Brier 1950 es matemática; sobreconfianza muy replicada; Tetlock riguroso). Matiz:
la magnitud de la sobreconfianza depende del formato de la pregunta (efecto "hard-easy") y de cómo se
elicita la confianza. La regla propia del Brier es la razón matemática por la que es la métrica correcta.

## (e) Implicaciones de diseño para MindBoost

- **Aplicabilidad acotada del Brier en el repo (corrección):** el Brier es una regla propia para
  **predicciones probabilísticas con desenlace resuelto** (se elicita P(evento) y luego se observa 0/1);
  por ser regla propia es **anti-gaming** (no se mejora exagerando confianza). Pero el **triaje de
  reversibilidad** (`ReversibilityScorer`) clasifica decisiones como reversibles/irreversibles: NO hay
  probabilidad elicitada ni desenlace binario resuelto, así que **el Brier no aplica ahí**. El propio
  repo ya lo reconoce (Bayes Arena puntúa con BUR + error absoluto y **difiere** Brier explícitamente:
  "no resolved binary outcome"). Un scoring tipo Brier tendría sentido en un ejercicio FUTURO de
  *pronosticar-y-verificar* (elicitar probabilidad, resolver el desenlace más tarde) — es una
  **propuesta de diseño**, no comportamiento actual. (Remit del psychometrician.)
- **Feedback de calibración honesto:** mostrar al usuario su curva de calibración (confianza declarada
  vs acierto real) es metacognición aplicada ([[M01_metacognition_dunlosky]]) — feedback de altísimo
  valor y honesto.
- **Especificidad:** reportar calibración *en el dominio practicado*, sin prometer "mejor juicio en la
  vida" (transferencia lejana no demostrada).
- **Gating de impulsividad / muestra mínima:** la calibración necesita suficientes juicios para ser
  fiable — coherente con `null ≠ 0` y muestra mínima antes de reportar.
- **Claim permitido:** "práctica de calibrar tu confianza; medimos si tu 70% acierta el 70%".
  **Prohibido:** "te hacemos mejor decisor en la vida".

## (f) Referencias

- Lichtenstein, S., Fischhoff, B., & Phillips, L. D. (1982). Calibration of probabilities: The state of
  the art to 1980. In *Judgment under uncertainty: Heuristics and biases* (pp. 306–334).
- Brier, G. W. (1950). Verification of forecasts expressed in terms of probability. *Monthly Weather
  Review, 78*(1), 1–3.
- Murphy, A. H. (1973). A new vector partition of the probability score. *Journal of Applied
  Meteorology, 12*(4), 595–600.
- Tetlock, P. E., & Gardner, D. (2015). *Superforecasting: The art and science of prediction.* Crown.
