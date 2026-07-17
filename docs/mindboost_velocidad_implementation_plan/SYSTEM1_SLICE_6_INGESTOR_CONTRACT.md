> ⚠️ **SUPERSEDED (2026-07-10) por SYSTEM1_SLICE_6R_REAUDIT.md.** Este cierre describe la API v1 del ingestor
> (`ingest(blockId, runs) → (contribution: ModuleContribution?, calibration.calibrated)`) que la re-auditoría
> adversarial 6-R REESCRIBIÓ. La API vigente es `ingest(blockId, runs, nowMs) → (estimate: ProfileEstimate?,
> calibration, rank: RankReadiness, evidence)`; los "follow-ups MOD-1..4" de abajo YA están resueltos en 6-R.
> Léelo solo como registro histórico.

# SYSTEM-1 · Slice 6 — CognitiveSessionRun Ingestor Contract — Cierre (SUPERSEDED)

**Fecha:** 2026-07-10 · **Estado:** DONE & green · **Exit gate /mb:** ✅ 3/3 PASS (diff-auditor · efficacy-guardian · psychometrician) · **Wiring:** NO (contrato puro).

> Alcance respetado: pura/testeada, **NO cableada**. Sin aggregator vivo, sin mutar `MindBoostProfile`, sin rank/calibración viva, sin Firestore/SyncQueue/E9/outbox. E9 sigue BLOQUEADO.

---

## 1. Qué se construyó

`CognitiveSessionIngestor` (objeto puro en `profile/domain/ingestion/`) — el contrato que convierte el historial sellado de `CognitiveSessionRun` en la moneda del perfil para UN bloque:

```
ingest(blockId: ScoreModuleId, runs: List<CognitiveSessionRun>): CognitiveSessionIngestion
CognitiveSessionIngestion(moduleId: CognitiveModuleId, contribution: ModuleContribution?, calibration: CalibrationState)
CalibrationState(validSessionCount, distinctDays, status: DimensionStatus, calibrated: Boolean, ruleVersion: String)
```

- `contribution == null` ⇒ AUSENCIA de actualización (no un valor bajo fabricado; `null ≠ 0`).
- `contribution.normalizedScore` es la ÚNICA moneda (no raw/delta/xp — Not Applicable por charter).
- El mapeo `ScoreModuleId → CognitiveModuleId` se reusa de `profile.domain.aggregation` (fuera de `core`; `core` no depende de `profile`).

**Archivos:**
- Producción: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/ingestion/CognitiveSessionIngestor.kt`
- Tests: `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/domain/ingestion/CognitiveSessionIngestorTest.kt` (21 tests)

---

## 2. Exclusiones (re-derivadas, no solo confiadas en flags)

Es el ÚLTIMO gate honesto antes del perfil, así que RE-DERIVA del run las condiciones del kill-switch que el score porta, EN VEZ de confiar en un flag upstream:

`isMeasurementValid()` (todas necesarias): `COMPLETED` · `!dataQualityCompromised` · `timingIntegrity != COMPROMISED` · `score.category == VALID` · `!preliminary` · `!lowConfidence`.

- **Contribución** = `isMeasurementValid() && profileEligible && purpose.mayContributeToProfile()` (PRACTICE/DEBUG nunca).
- **Calibración** = `isMeasurementValid() && calibrationEligible && purpose.mayContributeToCalibration()` (INITIAL_ASSESSMENT es provisional → no cuenta para calibración).
- **Otro bloque no contamina** (filtro por `blockId`).
- **E8 hoy**: preliminary + todas las lanes denegadas por el kill-switch ⇒ visible en historial, NUNCA ingerido.

Las dos condiciones del kill-switch que el run NO persiste como booleanos (timebase autoritativo, cobertura anti-gaming) NO son re-derivables aquí; para esas dos se confía en `profileEligible` (documentado con precisión en el KDoc, no asumido en silencio).

---

## 3. Honestidad de la señal

- **`null ≠ 0`**: sin run cualificante ⇒ `contribution = null`; el agregador (cuando se cablee) deja la dimensión CALIBRATING con valor null.
- **`dataSufficiency ⊥ confidence`**: `dataSufficiency = validProfileCount / ESTABLISHED_MIN_SESSIONS(5)` (muestra); `confidence = InterSessionConfidence.interSessionConfidence(scores)` (estabilidad; n<2 ⇒ 0.0 honesto). Ninguna deriva de la otra (invariante 2026-06-28).
- **`preliminary = true` SIEMPRE** (conservador, igual que todos los ProfileSource y el default de `ModuleContribution`): excluir *scores de sesión preliminares* NO valida físicamente el *módulo* (§15 abierto). Corregido tras el gate (era el bloqueante — ver §5).
- **Calibración vía autoridad única `CalibrationPolicy`** (sin segundo umbral = sin doble-verdad): `calibrated = eligibleCeiling(evidence) == ESTABLISHED` = `ESTABLISHED_MIN_SESSIONS(5)` sesiones válidas Y `ESTABLISHED_MIN_DISTINCT_DAYS(3)` días distintos.
- **`trend`** categórico (IMPROVING/STABLE/DECLINING/UNKNOWN), UNKNOWN por debajo de 2 sesiones (nunca fabrica dirección de una sesión).
- **`mayCountToward*` ≠ counted**: "counted" solo se decide dentro del resultado del ingestor.

---

## 4. Exit gate /mb — 3/3 PASS (contexto fresco)

- **diff-auditor: APTO** (tras corregir 1 bloqueante). Verificó pureza/no-cableado, dirección de dependencias, separación mayCountToward/counted, moneda normalized-only, calibración versionada/determinista/sin doble-verdad, y 14/14 casos CEO no-vacíos; recomputó a mano la fórmula de confianza y el day-bucketing.
- **efficacy-guardian: APTO** (8/8 invariantes de honestidad). **Ruling CEO-facing sobre los 3 días distintos: MANTENER** — exigir 3 días distintos además de 5 sesiones es la elección HONESTA (ESTABLISHED implica estimación de rasgo estable; 5 sesiones del mismo día están confundidas por práctica/fatiga). Si el CEO quisiera ESTABLISHED por conteo puro, eso REBAJA la honestidad y debe rechazarse; en su lugar exponer el progreso de días en UI.
- **psychometrician: APTO** (contrato inerte). Sin bugs de corrección; verificó ordenación, edge-cases, dedup y belt-and-suspenders.

---

## 5. Bloqueante corregido en el gate (diff-auditor #1)

**`contributionOf()` fijaba `preliminary = false`** — sobre-afirmaba validación física del módulo. Un `INITIAL_ASSESSMENT` limpio (profileEligible=true) salía `preliminary=false`, contradiciendo su propio KDoc "provisional-only" y a TODOS los adapters existentes (Focus/Logic/Language/Speed fijan `true` mientras §15 esté abierto). **Fix:** `preliminary = true` (conservador, safe default) + test 19 que asserta `.preliminary == true` para historial INITIAL_ASSESSMENT y TRAINING_FREE. Confundía el `score.preliminary` por-SESIÓN (que sí se excluye) con el `ModuleContribution.preliminary` por-MÓDULO (validación física §15).

Notas no-bloqueantes del gate también aplicadas: defense-in-depth de timing-integrity en `isMeasurementValid`; KDoc corregido (re-derivable vs confiado); source-scan ampliado a líneas de código no-comentario; test de días-distintos same-day; goldens de confianza exacta y rampa de dataSufficiency.

---

## 6. Tests (21) y gates

**Tests:** 14 casos CEO (5-calibran/4-no/preliminary/profileEligible=false/calibrationEligible=false/ABANDONED/dataQuality/PRACTICE·DEBUG/TRAINING_FREE/otro-bloque/E8-kill-switch/pureza/normalized/versionado) + 7 de robustez (no-wiring source-scan, same-day→EMERGING, trend directions+ε+V-shape, lowConfidence·non-VALID belt-and-suspenders, preliminary, confidence golden exacto, dataSufficiency ramp).

**Gates (verde):** `:shared:compileKotlinDesktop` · `:shared:desktopTest` (21) · `:shared:compileAndroidMain` · `:shared:detekt` (limpio) · copy-scanner (profile no está en scope; corrido igual, verde).

---

## 7. Follow-ups ANTES de cablear (documentados, NO en esta slice)

Ambos revisores coinciden: el ingestor es honesto como CONTRATO inerte, pero estos deben resolverse antes de cablearlo a un perfil VIVO:

1. **MOD-1** `normalizedScore = latest` → tendencia central robusta de ventana reciente (mediana-k / EWMA): "última cruda" es la elección de máximo error de medida.
2. **MOD-2** `confidence` vía CV (sensible a la escala; penaliza al estable-pero-bajo) → dispersión sobre rango fijo `[0,1]` (reservado en `InterSessionConfidence` Fase C).
3. **MOD-3** `trend` first-vs-last + ε0.05 (ignora el medio; ε bajo el error de medida) → pendiente/regresión sobre todos los puntos con deadband derivado del error de medida (o exigir ≥3 puntos).
4. **MOD-4** días-distintos = epoch-day UTC (aprox; yerra a caballo de medianoche UTC; vector de cramming leve) → día local/tz-aware o intervalo mínimo entre sesiones.
5. **preliminary** se queda `true` para siempre desde este contrato; pasar a `false` por bloque exige una señal externa "módulo físicamente validado (§15 cerrado)" que aún no existe — diferido correctamente, no un hueco.
6. **Guardarraíl de wiring**: el caller DEBE tratar `contribution == null` como AUSENCIA (`mapNotNull`), NUNCA coalescer a `normalizedScore = 0.0`; añadir un test end-to-end a través de `MindBoostProfileAggregator.aggregate(...)` (con dataSufficiency 0.2/0.4 para n=1-2 por debajo de `minSufficiency=0.5`).
7. **Precondición**: el ingestor asume que `score.normalized` ya está escalado por habilidad / es invariante a la dificultad a la que se ganó; documentar/verificar esa invarianza upstream antes de mezclar sesiones de dificultades distintas.

---

## Cierre

Slice 6 está **DONE & green** con exit gate 3/3 PASS. El ingestor es un contrato PURO y NO cableado que excluye honestamente (re-derivando el kill-switch portado por el score), reutiliza la autoridad única de calibración, mantiene `dataSufficiency ⊥ confidence`, no admite ningún vector de falso-progreso vivo, y E8 hoy queda excluido de perfil/calibración/ranking. Espero tu decisión para continuar (slice 7 analytics producer, o el orden que prefieras). **No E9. No perfil vivo. No cloud.**
