---
title: "Practice effects, test–retest artifacts and regression to the mean"
authors: "Hausknecht, J. P.; Salthouse, T. A.; Scharfen, J."
year: "2007–2018"
tier: T1
domains: [processing_speed, working_memory, logical_reasoning]
constructs: [practice_effects, retest_gains, regression_to_the_mean, item_familiarity, holdout]
paradigms: []
mindboost_anchors:
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/BankIntegrityAuthority.kt
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming
  - shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/calibration/CalibrationPolicy.kt
evidence_level: strong
---

## (a) Tesis central

Repetir un test cognitivo produce **ganancias de práctica** (retest gains): la puntuación sube por
familiaridad con el formato, memoria de los ítems y estrategia — **sin** que la capacidad subyacente
cambie. El meta-análisis de Scharfen et al. (2018) cuantifica efectos de retest sustanciales que
decaen con el número de administraciones. Sumado a la **regresión a la media**, esto significa que una
"mejora" longitudinal puede ser un **artefacto de medición**, no un progreso cognitivo real.

## (b) Claims RESPALDADOS

- Las puntuaciones cognitivas suben en re-administraciones por práctica/familiaridad, con tamaños de
  efecto no triviales. [meta-análisis — Scharfen 2018, Hausknecht 2007]
- Los efectos de práctica son mayores con **ítems idénticos** y menores con formas paralelas/holdout.
  [experimental]
- La regresión a la media infla las "mejoras" de quienes puntuaron bajo en la primera medición.
  [estadística]
- El envejecimiento cognitivo real puede quedar **enmascarado** por ganancias de práctica en estudios
  longitudinales. [Salthouse]

## (c) Claims NO respaldados o refutados

- Una subida de puntuación entre sesiones **no** demuestra mejora cognitiva: hay que descontar práctica
  y regresión a la media (ver RCI en [[P01_psychometrics_reliability_validity]]).
- Reutilizar los mismos ítems **inFLA la mejora** y permite "memorizar el test" — invalida la señal.
- El auto-reporte de "me noto mejor" es especialmente vulnerable a expectativa; no es evidencia.

## (d) Estado de replicación / críticas

Fenómeno muy replicado y bien cuantificado. Es la razón por la que la evaluación cognitiva seria usa
**formas paralelas**, bancos de ítems con **holdout**, y modela la práctica explícitamente. Ignorarlo es
el error metodológico que hace que muchos productos reporten "mejoras" que son familiaridad.

## (e) Implicaciones de diseño para MindBoost

- **Es la justificación directa de la arquitectura de bancos e integridad del repo:**
  `BankIntegrityAuthority`, validación de banco con **holdout**, y el gate anti-gaming
  (`core/antigaming`) existen precisamente para que "mejorar" no sea "memorizar/gamear los ítems". Esta
  ficha es su respaldo científico.
- **Generación paramétrica de ítems** (ítems nuevos cada sesión) es la defensa correcta contra efectos
  de práctica por familiaridad de ítem — preferible a un banco fijo pequeño.
- **Progreso frente a la línea base debe descontar práctica**, pero con una distinción crítica: la
  **muestra mínima / días distintos** de `CalibrationPolicy` NO es control de efectos de práctica (un
  usuario aún en la pendiente de retest-gain pasa el gate de muestra). El control real contra la
  familiaridad de ítem es la **rotación de banco / holdout / `BankIntegrityAuthority` / generación
  paramétrica** (ya citada arriba). **Hueco declarado:** el repo hoy NO modela la curva de retest-gain
  ni descuenta la práctica temprana del baseline — un "sube y luego se estabiliza" temprano puede ser
  práctica, no capacidad.
- **Claim permitido:** "progreso frente a tu línea base, controlando la familiaridad con la tarea".
  **Prohibido:** presentar ganancias tempranas de práctica como mejora de capacidad.

## (f) Referencias

- Scharfen, J., Peters, J. M., & Holling, H. (2018). Retest effects in cognitive ability tests: A
  meta-analysis. *Intelligence, 67*, 44–66.
- Hausknecht, J. P., Halpert, J. A., Di Paolo, N. T., & Moriarty Gerrard, M. O. (2007). Retesting in
  selection: A meta-analysis of practice effects. *Journal of Applied Psychology, 92*(2), 373–385.
- Salthouse, T. A. (2010). Influence of age on practice effects in longitudinal neurocognitive change.
  *Neuropsychology, 24*(5), 563–572.
