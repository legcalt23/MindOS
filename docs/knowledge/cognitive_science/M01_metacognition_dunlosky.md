---
title: "Metacognition: monitoring and controlling one's own learning"
authors: "Dunlosky, J.; Metcalfe, J.; Nelson, T. O.; Koriat, A."
year: "1990–2013"
tier: T2
domains: [metacognition]
constructs: [metacognitive_monitoring, metacognitive_control, judgments_of_learning, confidence_calibration]
paradigms: [judgment_of_learning, confidence_rating]
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/display/UserDisplayScoreMapper.kt
evidence_level: strong
---

## (a) Tesis central

La metacognición es "pensar sobre el propio pensamiento": el **monitoreo** (evaluar cuánto sé / cuán bien
lo hago, p. ej. juicios de aprendizaje y confianza) y el **control** (decidir qué estudiar, cuánto
tiempo, cuándo parar). El marco de Nelson & Narens formaliza el bucle monitor→control. Un hallazgo
central: el monitoreo humano es **sistemáticamente imperfecto** (ilusiones de saber), lo que degrada las
decisiones de control.

## (b) Claims RESPALDADOS

- Las personas monitorean su aprendizaje con precisión limitada; los juicios de aprendizaje se dejan
  engañar por la fluidez momentánea (sobrestiman lo que olvidarán). [experimental, replicado]
- Un mejor monitoreo (calibración) conduce a mejores decisiones de estudio (control). [experimental]
- El feedback y la práctica de recuperación **mejoran la calibración** metacognitiva (aprendes a saber
  qué sabes). [experimental]

## (c) Claims NO respaldados o refutados

- La metacognición **no** es una capacidad general fija que se "sube de nivel" transfiriendo a todo;
  la precisión del monitoreo es específica de dominio y de tarea.
- Medir confianza no basta: hay que compararla con el acierto real (calibración), o la métrica es vacía.
- No respalda claims de "mejora tu autoconciencia" como rasgo global.

## (d) Estado de replicación / críticas

Marco robusto y muy productivo. La calibración y las ilusiones de competencia están bien replicadas
(relacionadas con Dunning-Kruger, aunque ese efecto concreto tiene su propia controversia estadística).
La lección firme: **la confianza subjetiva es un dato ruidoso** que debe validarse contra el desempeño.

## (e) Implicaciones de diseño para MindBoost

- **Base del scoring de confianza en Decisiones:** pedir confianza y compararla con el acierto
  (calibración/Brier — ver [[D03_calibration_probabilistic_judgment]]) es metacognición aplicada. El
  módulo Decisiones y su reflexión/diario entrenan monitoreo honesto.
- **El coach debe cerrar el bucle monitor→control:** ayudar al usuario a calibrar (¿cuánto crees que
  acertaste? vs cuánto acertaste) y a decidir qué practicar, en vez de solo puntuar.
- Conecta con la honestidad de display: mostrar `CALIBRATING`/"—" cuando falta señal **modela buena
  metacognición** al propio producto (no fabricar certeza que no tiene). `UserDisplayScoreMapper` debe
  evitar transmitir falsa precisión.
- **Claim permitido:** "práctica de calibrar tu confianza con tu desempeño real". **Prohibido:** "mejora
  tu metacognición / autoconciencia" como capacidad general.

## (f) Referencias

- Nelson, T. O., & Narens, L. (1990). Metamemory: A theoretical framework and new findings.
  *Psychology of Learning and Motivation, 26*, 125–173.
- Dunlosky, J., & Metcalfe, J. (2009). *Metacognition.* SAGE.
- Koriat, A. (2007). Metacognition and consciousness. In *The Cambridge Handbook of Consciousness*.
