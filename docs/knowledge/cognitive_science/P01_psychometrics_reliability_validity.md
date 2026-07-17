---
title: "Reliability, validity and measurement error (classical & modern test theory)"
authors: "Nunnally, J. C.; Cronbach, L. J.; Messick, S.; Jacobson, N. S.; Truax, P.; Hedge, C."
year: "1951–2018"
tier: T2
domains: []
constructs: [reliability, validity, measurement_error, reliable_change_index, standard_error_of_measurement]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/MindBoostProfileAggregator.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/calibration/ProfileSessionEligibility.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/calibration/CalibrationPolicy.kt
evidence_level: strong
---

## (a) Tesis central

Toda medición psicológica tiene **error**. La **fiabilidad** es la consistencia de la medida
(test-retest, consistencia interna); la **validez** es si mide lo que dice medir (de constructo, de
criterio, de contenido). Una medida poco fiable no puede ser válida. Para afirmar que alguien
**cambió** (mejoró) hace falta que la diferencia supere el error de medida — el **Reliable Change
Index (RCI)** formaliza cuándo un cambio individual es real y no ruido.

## (b) Claims RESPALDADOS

- Sin fiabilidad no hay validez: una medida ruidosa no puede sostener inferencias. [teoría psicométrica]
- El error estándar de medición (SEM) acota la precisión de una puntuación individual; los intervalos de
  confianza, no los puntos, son lo honesto. [CTT]
- Un cambio pre-post individual solo es "real" si excede el RCI (función de la fiabilidad y el SEM); de
  lo contrario es indistinguible de ruido/regresión a la media. [Jacobson & Truax 1991]
- La validez es un **argumento acumulativo** (Messick), no una propiedad binaria de un test.

## (c) Claims NO respaldados o refutados

- Una puntuación individual **no** es un valor exacto; presentarla sin incertidumbre es deshonesto.
- Una mejora entre dos sesiones **no** demuestra mejora real salvo que supere el RCI y se descarte
  práctica/regresión a la media (ver [[P02_practice_effects_test_retest]]).
- La fiabilidad de muchas tareas cognitivas (sobre todo scores de diferencia y EF) es **baja a nivel
  individual** (paradoja de fiabilidad) — límite duro para medir cambio fino en una app.

## (d) Estado de replicación / críticas

Fundamentos consolidados de la psicometría. La teoría de respuesta al ítem (IRT) refina la CTT
(información del test dependiente del nivel). La lección crítica para producto: **la mayoría de tareas
cognitivas no son medidores de precisión individual**; tratarlas como tal fabrica certeza.

## (e) Implicaciones de diseño para MindBoost

- **Es la base psicométrica del rigor charter (`null ≠ 0`).** Justifica directamente:
  `ProfileSessionEligibility` + `CalibrationPolicy` (exigir muestra/evidencia mínima antes de puntuar),
  `ScoreStateMachine` (`CALIBRATING` cuando la señal no es fiable), y mostrar "—" en vez de un número
  fabricado.
- **`dataSufficiency` ≠ `confidence`:** tamaño de muestra (¿suficientes datos?) y estabilidad (¿medida
  fiable?) son ortogonales — el repo ya los separa; esta ficha es la razón teórica de no mapear uno
  desde el otro.
- **Progreso frente a la propia línea base** debería usar lógica tipo RCI (celebrar "mejora" solo cuando
  supera el error de medida esperado). **Hueco declarado:** hoy el repo gatea por muestra mínima +
  estabilidad (`confidence`) y muestra `CALIBRATING`, pero NO implementa un test formal de cambio fiable
  (RCI/SEM); adoptarlo es trabajo futuro, no capacidad actual.
- **Mostrar incertidumbre**, no solo puntos: bandas/estados, no cifras exactas de "capacidad".
- **Claim permitido:** "tu progreso frente a tu línea base, cuando supera el margen de error".
  **Prohibido:** puntuaciones exactas de capacidad presentadas como hechos precisos.

## (f) Referencias

- Cronbach, L. J. (1951). Coefficient alpha and the internal structure of tests. *Psychometrika, 16*(3),
  297–334.
- Nunnally, J. C., & Bernstein, I. H. (1994). *Psychometric theory* (3rd ed.). McGraw-Hill.
- Messick, S. (1995). Validity of psychological assessment. *American Psychologist, 50*(9), 741–749.
- Jacobson, N. S., & Truax, P. (1991). Clinical significance: A statistical approach to defining
  meaningful change. *Journal of Consulting and Clinical Psychology, 59*(1), 12–19.
- Hedge, C., Powell, G., & Sumner, P. (2018). The reliability paradox: Why robust cognitive tasks do not
  produce reliable individual differences. *Behavior Research Methods, 50*(3), 1166–1186.
