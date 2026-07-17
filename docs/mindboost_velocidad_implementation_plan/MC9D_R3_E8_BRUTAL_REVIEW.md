# MC9-D-R3 — E8 · Go/No-Go: revisión R3 brutal (pre-E9)

Revisión adversarial de MC9-D-R2 contra el PRD, con reclasificación de riesgos y remediación quirúrgica. No busca
optimismo: separa lo que E8 ES (módulo local honesto) de lo que NO es (integración MVP). Ver contrato sistémico en
[`EXERCISE_QUALITY_CONTRACT.md`](../mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md).

## A) Contradicciones del cierre anterior — reclasificadas

| # | Afirmación previa | Veredicto | Evidencia | ¿Contradice PRD? | Acción |
|---|---|---|---|---|---|
| 1 | "E8 cerrado a nivel implementación" | **PARCIAL** | L1 sí; L2 gateado; L3/L4 abiertos | No si se dice "a L1" | Reetiquetar por nivel (§0 contrato) |
| 2 | "E8 no es isla" | **PARCIAL (con condiciones)** | `officialRun` no tiene consumidor; `SpeedProfileSource` lee `MeasurementRun`, E8 no lo produce | Sí, contra "historial/perfil" | Adapter honesto + criterio de wiring; historial unificado = deuda §21 |
| 3 | "Persiste vía ExerciseTelemetryStore" | **VERDADERA** | `SecureExerciseTelemetryStore` (`tele:`), test round-trip | No | — |
| 4 | "Se borra con la cuenta" | **VERDADERA** | `mindboost_speed` en `AccountBound` + eraser `AppGraph` | No | — |
| 5 | "No premia velocidad sin precisión" | **VERDADERA** | caps + balanced accuracy + data-quality gate, probados | No | Reforzada (gate no bypaseable) |
| 6 | "Produce run oficial equivalente" | **VERDADERA pero no integrada** | `GoNoGoOfficialRunAdapter`; sin store consumidor | Sí (historial no wired) | Deuda §21.1 |
| 7 | "GO E9" | **ENGAÑOSA sin matizar** | contradice PRD (Firestore/perfil) | Sí | → "GO E9 condicionado" (§H) |
| 8 | "MVP-ready 45/100 limitado por transversales" | **VERDADERA** | Firestore/iOS/analytics/perfil abiertos | Es la descripción honesta | Mantener, con matriz §E |
| 9 | "MeasurementRun no puede representar NO-GO sin mentir" | **VERDADERA** | `TrialRecord` exige `responseUptimeNanos` real + `isCorrect` RT-tap | No | Justifica un `CognitiveSessionRun` (§21.1) |
| 10 | "No contribuye al perfil, y eso está bien" | **PARCIAL** | correcto NO contribuir hoy (§15 abierto); pero "está bien" solo si el criterio para SÍ es explícito | Roza "progreso demostrable" | Criterio exacto ahora explícito (`PROFILE_WIRING_ENABLED`) |

## B) P0/P1 reclasificados

- **B1 · `MeasurementRun` no representa NO-GO** → **P1 sistémico (bloquea L3 del bloque, no L1 de E8).** El adapter
  evita mentir; el historial oficial unificado (`CognitiveSessionRun` + `TrialOutcome` + `ExerciseSpecificMetrics`)
  es deuda §21.1. **Bloquea el claim "no es isla" en sentido fuerte** → por eso el veredicto lo baja a PARCIAL.
- **B2 · No contribuye al perfil** → **backlog bloqueante de L2/L3.** Criterio EXACTO para `true` ahora en código
  (`GoNoGoOfficialRunAdapter.PROFILE_WIRING_ENABLED`): §15 físico + store consumidor + `score.profileEligible`.
- **B3 · Firestore no implementado** → **backlog bloqueante de L3 (transversal §21.2).** E8 NO puede llamarse
  "cerrado a MVP" sin cloud sync. Schema/outbox especificado en el contrato.
- **B4 · Persistencia incompleta** → **FIXED (L1).** El summary ahora persiste conteos crudos + distribución RT
  (medio/mín/máx/mediana/variabilidad) + resultado scored (índice/categoría/versión/integridad/elegibilidad).
  "Reconstruible" ahora es verdad para la corrida local. N/A honestos (documentados): dificultad inicial/final (E8
  interleava por trial), delta/XP (no hay motor todavía), plataforma (necesita provider inyectado — §21), estado
  abandonado (solo se persisten corridas completas por diseño).
- **B5 · `score(diagnostics = null)`** → **FIXED (P1, no bypaseable).** `diagnostics` ahora es OBLIGATORIO: el gate
  de calidad es una garantía de COMPILACIÓN, no de disciplina humana. Único caller de producción intacto.
- **B6 · cap binario ×0.30** → **FIXED.** Reemplazado por `TimingIntegrityLevel` escalonado: CLEAN / MINOR_ANOMALIES
  (penalización proporcional, piso 0.50) / COMPROMISED (×0.30 + baja confianza + inelegible). Un evento aislado ya no
  anula la sesión; sistemático sí.
- **B7 · missing onset** → **FIXED.** `missingOnsetTaps` contado por el holder, alimenta diagnostics → integridad →
  elegibilidad, y aparece en `anomalyFlags`. Nunca se fabrica un RT=0.
- **B8 · background/lifecycle** → **backlog bloqueante de L3 (transversal §21.4).** Especificado; no se añade hook sin
  call-site vivo (evitar control muerto). Device QA obligatorio.
- **B9 · `NonCancellable` insuficiente ante process-death** → **backlog bloqueante de L3 (§21.3).** NonCancellable
  cubre el dispose de scope (back inmediato); process-death necesita write-ahead journal (transversal).
- **B10 · `error()` en ramas muertas UI** → **accepted risk (documentado).** Decisión: mantener `error()` como
  aserción de invariante para un estado imposible (el hub enruta E8 a su pantalla dedicada; el `when` es exhaustivo).
  Una pantalla "no disponible" silenciosa MASCARARÍA una regresión de routing y no hay sink de analytics para
  señalarla. La versión graceful + `exercise_route_unsupported` es backlog cuando exista el sink (§21.5).

## C) Revisión de scoring Go/No-Go
Pesos: `0.35 inhibitoryControl(balanced) + 0.25 goAccuracy + 0.15 speed(norm) + 0.10 stability +
0.10 difficultyAdjustedControl + 0.05 anticipationControl`, renormalizados sobre los medidos. Comisión NO-GO:
cap multiplicativo ×0.30 (≥0.20). goAccuracy baja ×0.45, anticipación alta ×0.50. `inhibitoryControlIndex` =
precisión balanceada ⇒ tap-all/tap-none = 0.5 estructural. Probado que tap-all/tap-none/fast-but-wrong/RT-imposible/
robótico no dan score alto. **Categorías de score (nuevas, en el dato):** válido, `lowConfidence`,
`dataQualityCompromised`, `timingIntegrity∈{CLEAN,MINOR,COMPROMISED}`, `calibrationEligible`, `profileEligible`,
`rankingEligible`. No hay post-error slowing todavía (no modelado; backlog no bloqueante).

## E) Matriz de readiness

| Capability | Estado | Evidencia | PRD | Riesgo | Sev | Acción | Owner | Bloquea E9 | Bloquea MVP |
|---|---|---|---|---|---|---|---|---|---|
| Local completion | ✅ | holder E2E, tests | sí | bajo | — | — | — | No | No |
| Local persistence | ✅ (completa) | summary v1.2.0 + round-trip | sí | bajo | — | — | — | No | No |
| Process-death recovery | ❌ | NonCancellable only | sí | pérdida rara | Alta | outbox/journal | data-sync | No | **Sí** |
| Firestore sync | ❌ | LOCAL_ONLY | sí | sin cloud | Alta | schema+sync | data-sync | No | **Sí** |
| Account deletion | ✅ | eraser + namespace | sí | bajo | — | — | — | No | No |
| Official history | ❌ (adapter sin consumidor) | `officialRun` transitorio | sí | isla parcial | Alta | `CognitiveSessionRun` | kmp-arch | No | **Sí** |
| Profile contribution | ⏸ diferida (criterio explícito) | `PROFILE_WIRING_ENABLED=false` | sí | sin progreso | Media | wiring + §15 | assessment | No | **Sí** |
| Calibration eligibility | ✅ computada (no wired) | `calibrationEligible` | sí | — | Media | consumir en baseline | assessment | No | **Sí** |
| Scoring integrity | ✅ | caps + tiers + tests | sí | bajo | — | — | — | No | No |
| Data-quality gating | ✅ (no bypaseable) | `score(...,diagnostics)` req. | sí | bajo | — | — | — | No | No |
| Android runtime | ❓ no probado | solo JVM | sí | UX/timing | Alta | device QA | qa-safety | No | **Sí** |
| iOS runtime | ❓ no probado | Windows, diferido | sí | build/UX | Alta | macOS QA | qa-safety | No | **Sí** |
| Lifecycle/background | ❌ | sin hook | sí | corrompe RT | Alta | lifecycle guard | kmp-arch | No | **Sí** |
| AI Coach exposure | ⏸ contrato doc | allowlist derivable | sí | — | Baja | al existir sink | ai-coach | No | No |
| Ranking/PvP exposure | ⏸ flag computado | `rankingEligible` | no (post-MVP) | — | Baja | pipeline ranking | — | No | No |
| Copy/claims | ✅ | scanners ×3 | sí | bajo | — | — | — | No | No |
| Privacy/user separation | ✅ | clave exacta + test | sí | bajo | — | — | — | No | No |
| Metrics completeness | ✅ local (N/A: XP/delta/plataforma) | summary v1.2.0 | sí | parcial | Media | plataforma/XP transversal | data-analytics | No | Parcial |
| Versioning/migration | ✅ | 3 ejes de versión + defaults | sí | bajo | — | — | — | No | No |
| Analytics events | ❌ | doc-only | sí | sin observabilidad | Media | sink | data-analytics | No | **Sí** |

## Cambios de código realizados (verificados)
- `GoNoGoScore.kt`: `diagnostics` obligatorio (gate compile-time); `TimingIntegrityLevel` escalonado; cap
  proporcional; elegibilidad por capa (`profile/calibration/ranking`) computada; `GONOGO_SCORE_VERSION 1.1.0-mc9d-r3`.
- `GoNoGoQualityDiagnostics.kt`: `missingOnsetTaps` (señal viva + flag + `clean`).
- `GoNoGoSessionStateHolder.kt`: cuenta `missingOnsetTaps`; `onStimulusTap(rtMs: Long?)` (null = input perdido);
  pasa `score` a la persistencia.
- `E8InhibicionScreen.kt`: tap sin onset → `onStimulusTap(null)` (nunca RT=0 fabricado).
- `GoNoGoOfficialRunAdapter.kt`: `PROFILE_WIRING_ENABLED` (criterio exacto); expone integridad + elegibilidad;
  `contributesToOfficialProfile = profileEligible && PROFILE_WIRING_ENABLED`.
- `GoNoGoTelemetrySummaryV1.kt`: persiste conteos crudos + distribución RT completa + resultado scored + elegibilidad;
  `summaryVersion 1.2.0-mc9d-r3`.
- Tests: +5 (tiers de integridad, elegibilidad por capa, missing-onset, persistencia completa); fixtures stale
  corregidos (RTs de "variabilidad" ahora ≥120 ms; assert de elegibilidad ajustado a la barra de confianza honesta).

## Gates (verificados en esta sesión)
`compileTestKotlinDesktop` ✅ · **`desktopTest` 3217/0/0** (3209 → +8 tests R3) ✅ · `compileAndroidMain` ✅ ·
`detekt` ✅ (solo los 2 findings foreign untracked ajenos: `IndexedSecureKeyValueStore:61`,
`InterSessionConfidence:31` — NO tocados) · copy scanners ×3 ✅ · `scan:secrets` ✅. **Android runtime / iOS /
Firestore: NO probados (fuera de JVM).**

## Gate de salida (contexto fresco) — `diff-auditor`
Veredicto **APTO** (sin hallazgos críticos/bloqueantes). Confirmó independientemente: gate no bypaseable (único
caller de producción pasa diagnostics reales), math de integridad escalonada correcta en bordes (total==0,
violations>total, límite 0.10 inclusivo), `missingOnsetTaps` con productor vivo y sin fabricación de RT=0,
elegibilidad correctamente anidada (profile ⊇ calibration ⊇ ranking), persistencia completa y honesta, `no es
isla = PARCIAL` correctamente rebajado (adapter sin consumidor), copy limpio, sin overclaims en docs. Gaps
menores señalados y **cerrados** en esta misma fase: (1) test de nivel-holder del camino vivo missing-onset
(`a_tap_without_onset_is_counted_as_a_lost_input_through_the_holder_never_fabricated`); (2) tests de borde de
integridad (0.10 inclusivo; violations>total ⇒ COMPROMISED sin crash); (3) comentario aclaratorio del guard
`category != CALIBRATING` (defensivo, no vivo); (4) corrección de string de versión en el contrato.

## Veredicto
**E8 = cerrado a L1 (módulo local entrenable) y sustancialmente a L2 (elegibilidad honesta computada); NO cerrado a
L3 (MVP) ni L4 (ranking).** "E8 no es isla" = **PARCIAL**: persiste, se borra, produce un run-equivalente honesto y
computa elegibilidad — pero NO escribe en historial/perfil oficial porque ese pipeline unificado no existe (deuda
§21, transversal, no de E8). **GO E9 CONDICIONADO** (ver decisión en el reporte de sesión), con la deuda §21 como
bloqueante EXPLÍCITO de MVP y el Exercise Quality Contract como requisito obligatorio para E9…E129.
