# MC9-C-R — E5 · Búsqueda Visual / Visual Scan Race Hardening

Mini-fase correctiva sobre E5 (commit base `35016318`) para convertirlo de "jugable + score preliminar" a un
instrumento con telemetría específica, honesta y versionada. **Sin scope creep**: no se tocó AI Coach, ranking,
Firestore, el score 0–1 preliminar, ni el esquema persistido compartido (`TrialRecord`/`MeasurementRun`); no se
tocó foreign-WIP.

## Agujeros que se cerraron
1. **E5 no tenía telemetría específica** (solo métricas genéricas 2-vías). Ahora deriva métricas reales de
   búsqueda visual de la corrida jugada.
2. **El feedback era solo macro-categoría del score.** Ahora `VisualScanFeedback.forRun` usa señales REALES
   (rápido-incorrecto, coste de carga medido, estabilidad) y cae al patrón macro solo cuando la señal no existe.
3. **La carga visual no era variable dentro de una corrida** ⇒ el coste por carga era estructuralmente inderivable.
   E5 ahora usa `VisualScanConfig.mixedLoad()` (niveles 3–6 por trial) ⇒ el coste por carga es real.
4. **Sin diagnósticos de calidad de datos de E5.** Añadido `VisualScanQualityDiagnostics` (no integrado al perfil).

## Arquitectura de la telemetría (honesta)
- El pipeline genérico persiste por trial rt/acierto/anticipación pero **descarta lado y carga**. Migrar
  `TrialRecord` (esquema `@Serializable` compartido + store + round-trips de TODOS los ejercicios) sería la
  "migración grande" prohibida ⇒ **NO** se hizo.
- El holder captura, por corrida, una lista TRANSITORIA de respuestas (`RecordedResponse`: prompt + lado tocado +
  rt + acierto + anticipación), se limpia por corrida, se expone en `SpeedUiState.responses`, y **NUNCA se
  escribe al store cifrado**. Es la fuente de la telemetría inmediata.
- `VisualScanTrialOutcome.from(responses)` deriva por trial: `itemsPerSide` (nº de símbolos del panel),
  `targetSide` (= lado correcto), `responseSide` (lado tocado), rt/acierto/anticipación. `VisualScanRunSummary`
  agrega por nivel de carga. `VisualScanMetricsMapper` produce `VisualScanMetrics` (versión `1.0.0-mc9c-r`).

## Telemetría → métrica → feedback → score
| Señal | De dónde | Estado | Uso en feedback |
|---|---|---|---|
| accuracy · anticipationRate · fastWrongRate (speed-accuracy) | outcomes de la corrida | **Measured** (≥6 trials) | fast-wrong ⇒ FAST_LOW_PRECISION |
| correctResponseSpeedMs · rtVariability · stabilityIndex | RT de aciertos | **Measured** (≥4 aciertos) | baja estabilidad ⇒ UNSTABLE_SEARCH |
| sideBiasIndex | lado tocado vs objetivo | **Measured** (≥6 resp.) | quality-diagnostic, NO feedback al usuario (no patologizar) |
| loadAdjustedAccuracy · visualLoadTolerance · highLoadCost | buckets por `itemsPerSide` | **Measured** SOLO con ≥2 niveles (mixedLoad lo garantiza) | coste alto ⇒ HIGH_VISUAL_LOAD_COST |
| similarityCost | — | **Unavailable** (similitud no persistida por trial en V1) | fallback macro |
| omissionPressureIndex | — | **Unavailable** (el flujo tap-to-advance no tiene omisiones; el tap tardío es error de comisión) | — |
| preliminaryScore01 (autoritativo) | `SpeedScoreEngine` 0–1 | reused (`preliminary=true`) | fallback cuando la señal falta |

Regla de honestidad enforced: cada métrica es `VisualScanSignal.Measured(v)` o `Unavailable(razón)` — **nunca un
0 fabricado ni heurística que finja medición** (Regla #5). "No se llama medido por carga a nada que no esté
derivado por carga": el coste por carga es `Unavailable` si la corrida tuvo un solo nivel.

## Scoring (no reescrito; mapping documentado)
El score autoritativo sigue siendo el 0–1 preliminar. Mapeo de E5 → fórmula de velocidad mental (para cuando se
calibre §15): precisión-bajo-carga = `visualLoadTolerance`/`loadAdjustedAccuracy`; velocidad-correcta =
`correctResponseSpeedMs`; dificultad/carga = `itemsPerSide`/`highLoadCost`; estabilidad = `stabilityIndex`;
control-de-errores = `fastWrongRate`. Invariante verificada por test: **rápido-pero-incorrecto nunca produce
categoría positiva** (`FAST_LOW_PRECISION`, no GOOD/BALANCED). Versionado: `VISUAL_SCAN_METRICS_VERSION`.

## Anti-gaming / diagnostics (§7, no integrado al perfil)
`VisualScanQualityDiagnostics.evaluate`: RT imposible (<120 ms), sesgo lateral extremo, inundación de
anticipación, ráfaga rápida-incorrecta, perfecto-improbable-bajo-carga. Es una lente de calidad de datos de E5;
NO alimenta el perfil oficial (eso lo gobiernan `SpeedProfileSource` + `SpeedAntiGaming`, transversales, no
tocados). `CHOICE_EXERCISE_CODES` ya incluye E5 (MC9-C).

## Límites científicos
- La telemetría es de la corrida INMEDIATA (transitoria); no hay análisis histórico cross-sesión hasta persistir.
- `HIGH_VISUAL_LOAD_COST` es una lectura por-corrida (accuracy por nivel dentro de la sesión), NO una norma
  validada de tolerancia a la carga. El score sigue PRELIMINAR (§15 abierto).
- E5 no afirma inteligencia general, salud/visión, ni transferencia garantizada; sin comparación normativa.

## Pendiente (diferido, honesto)
- **Persistencia histórica de la telemetría E5** (escribir el summary/outcomes al store cifrado) — requiere
  decisión de esquema (namespace/summary tipado), fuera del alcance de esta mini-fase.
- **similarityCost real** — requiere persistir el nivel de similitud por trial.
- **omissionPressureIndex real** — requiere un mecanismo de omisión (auto-avance por deadline sin respuesta), que
  hoy no existe en el flujo.
- **Target variability** — `targetPool` soporta >1 objetivo (test), pero la config de producción usa 1 (★ por
  familia); variar el target en producción es un enhancement de contenido, documentado.

## QA cross-platform pendiente
Legibilidad de glyphs (●○▲△◆◇■□★) verificada en tests JVM, **no en dispositivo**: Android runtime (sin
instrumented test) e iOS (sin Mac) siguen sin probar. Firestore no aplica (diferido).

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` **3086/0/0** ✅ · `compileAndroidMain` ✅ · `detekt` (solo el foreign
untracked `IndexedSecureKeyValueStore:61`) ✅ · copy scanners ×3 ✅ · `scan:secrets` ✅. **iOS no probado** (sin
Mac) · **Android runtime no probado** (sin instrumented) · **Firestore no probado** (diferido).

## Riesgos residuales
1. La telemetría vive en `SpeedUiState.responses` (transitoria): matar el proceso antes de terminar la corrida la
   pierde (idéntico al patrón del score; no se agrava).
2. El parseo de `itemsPerSide` desde el label del panel acopla el mapper al formato E5 (paneles separados por
   espacio); cubierto por test, pero un cambio de `separator` lo rompería (documentado).
3. Sin persistencia histórica, la adaptación longitudinal por carga sigue pendiente.

## Veredicto
E5 pasa de "jugable + score preliminar" a **instrumento con telemetría específica REAL de la corrida** (accuracy,
velocidad correcta, estabilidad, fast-wrong, sesgo lateral, y coste por carga con mixed-load), feedback accionable
basado en señales, y diagnósticos de calidad — todo honesto (Measured/Unavailable). Sigue **MVP-candidate**, no
MVP-ready completo (analytics + §15 + persistencia histórica abiertos).
