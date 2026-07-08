# MC9-C-R2 — E5 · Búsqueda Visual: Telemetry Integrity & Historical Summary

Convierte la telemetría de E5 (commit base `c26d50e8`) de "útil solo en la corrida terminada" a "estructurada,
robusta y ANALIZABLE HISTÓRICAMENTE", **sin migrar `TrialRecord`**, sin tocar AI Coach, ranking/PvP, otros
ejercicios ni foreign-WIP.

## Preflight (superficies y decisión)
- `TrialRecord`/`MeasurementRun` NO llevan lado/carga/similitud → migrarlos era la migración grande prohibida.
- El store de Velocidad (`SecureSpeedMeasurementStore`) vive sobre `SecureKeyValueStore` del namespace
  `mindboost_speed` con prefijos `run:`/`dp:`. Ese namespace YA está en `AccountBound` y en el eraser de borrado
  de cuenta (`AppGraph`, `SecureNamespaceUserDataEraser`).
- **Decisión:** persistir el summary de E5 en un store SEPARADO (`ExerciseTelemetryStore`) sobre el MISMO
  `SecureKeyValueStore`/namespace, prefijo `tele:` — histórico y cubierto por el borrado de cuenta SIN nuevo
  namespace, sin tocar el pipeline de `MeasurementRun`, sin nuevo registro en el eraser.

## Qué se persistió (histórico)
`ExerciseTelemetryRecord(sessionId, userId, exerciseId, telemetryVersion, summaryJson, createdAtEpochMs)` — genérico
(el summary específico va como JSON). El summary es `VisualScanTelemetrySummaryV1` (`@Serializable`) con cada métrica
como `VisualScanSignal` (`Measured`/`Unavailable`, sealed `@Serializable`), `byLoad[]`, `diagnostics`,
`unavailableSignals[]`, `metricsVersion`, `createdAtEpochMs`. Se guarda **una vez por corrida terminada de E5**
(LaunchedEffect keyed por sessionId, best-effort: un fallo de storage nunca rompe la sesión). El borrado debug
("Borrar mis datos") ahora también limpia la telemetría.

## Fuente de verdad estructurada (§3 — fin del parseo frágil)
El generador adjunta `SpeedTrialPrompt.visualScan: VisualScanPromptMetadata?` (itemsPerSide/loadLevel/targetSymbol/
targetSide/similarityLevel/distractorCount/uniqueDistractorCount/trialConfigVersion). El mapper la lee como fuente
de verdad; el parseo de `leftLabel` queda SOLO como fallback legacy/degradado explícito.

## Correcciones de integridad
- **§4 sesgo lateral separado:** `targetSideImbalance = targetLeftRate − 0.5` (sesgo de los ESTÍMULOS) vs
  `sideResponseBias = responseLeftRate − targetLeftRate` (sesgo del USUARIO, corregido por la distribución). Ambos
  `Unavailable` con muestra insuficiente. El sesgo NO se muestra al usuario (quality-diagnostic, no se patologiza).
- **§5 taps prematuros:** el holder cuenta los taps en foreperiodo (`prematureTapCount`, antes descartados) →
  `prematureTapRate = premature / (trials + premature)`. La `anticipationRate` (tap rápido POST-onset) se mantiene
  como señal real y distinta.
- **§6 mixed-load balanceado:** scheduler de bag barajado (Fisher-Yates determinista por seed) sobre [3,4,5,6];
  cada nivel aparece por igual en una corrida normal (48 trials → 12 c/u), orden no trivial; `reset()` rebaraja.
  Si la corrida termina corta (cooldown) y faltan buckets, la métrica de carga queda `Unavailable`.
- **§7 coste vs pendiente:** `highLoadCost = max(0, acc(baja) − acc(alta))` (un COSTE nunca es negativo, Regla #4);
  `loadAccuracySlope = acc(alta) − acc(baja)` (con signo). Ambos `Unavailable` con <2 buckets.
- **§8 feedback más accionable:** nuevo `STABLE_REDUCE_LATENCY` (alta precisión + RT de aciertos lento) → "detección
  estable; siguiente objetivo reducir la latencia". Prioridad: fast-wrong > coste-de-carga medido > inestable >
  precise-but-slow > buen control > equilibrio. Señal ausente ⇒ fallback honesto al patrón macro del score.
- **§9 diagnostics integrados:** `VisualScanQualityDiagnostics` (RT imposible, sesgo extremo, anticipación flood,
  fast-wrong burst, perfecto-improbable-bajo-carga) se incluye en el summary persistido. NO invalida el perfil
  oficial (transversal, no tocado). Tono no acusatorio.
- **§10 similitud/target:** el summary agrega `bySimilarity`; `similarityCost = max(0, acc(baja) − acc(alta))`
  Measured SOLO con ≥2 niveles con muestra. Producción usa un único nivel por corrida (LOW por defecto) ⇒
  `similarityCost` queda `Unavailable`, pero el mapper YA está listo para runs con varios niveles.

## Métricas históricas reales (persistidas por sesión)
accuracy · correctResponseSpeedMs · rtVariability · stabilityIndex · anticipationRate · prematureTapRate ·
fastWrongRate · targetSideImbalance · sideResponseBias · loadAdjustedAccuracy · visualLoadTolerance · highLoadCost ·
loadAccuracySlope · byLoad[] · diagnostics — todas `Measured` con muestra suficiente, o `Unavailable(razón)`.

## Métricas que siguen Unavailable (razón estructural)
- `similarityCost` — producción usa un solo nivel de similitud por corrida (mapper ya listo si se mezclan niveles).
- `omissionPressureIndex` — el flujo tap-to-advance no tiene omisiones (el tap tardío es error de comisión).

## Mapping señal → feedback → score futuro
precisión-bajo-carga (`visualLoadTolerance`/`loadAdjustedAccuracy`) · velocidad-correcta (`correctResponseSpeedMs`) ·
dificultad/carga (`highLoadCost`/`loadAccuracySlope`) · estabilidad (`stabilityIndex`) · control-de-errores
(`fastWrongRate`/`prematureTapRate`). El score autoritativo sigue el 0–1 preliminar (`SpeedScoreEngine`,
`preliminary=true`); el feedback nunca premia velocidad cuando cae la precisión (test: fast-wrong ⇒ categoría no
positiva).

## Gates
`compileKotlinDesktop` ✅ · `desktopTest` **3104/0/0** ✅ · `compileAndroidMain` ✅ · `detekt` (solo el foreign
untracked `IndexedSecureKeyValueStore:61`) ✅ · copy scanners ×3 ✅ · `scan:secrets` ✅.
**Android runtime NO probado** (sin instrumented test) · **iOS NO probado** (sin Mac) · **Firestore NO probado**
(diferido) · score **PRELIMINAR** (§15 abierto).

## Riesgos residuales
1. La telemetría inmediata vive en `SpeedUiState.responses`/`prematureTaps` (transitoria); el summary SÍ persiste,
   pero matar el proceso entre el fin de la corrida y el LaunchedEffect de guardado pierde ESE summary (best-effort,
   como el write-ahead recorder; el `MeasurementRun` sí se persiste por su propia vía).
2. La telemetría histórica no se sincroniza a Firestore (diferido).
3. Legibilidad de glyphs verificada solo en JVM, no en dispositivo.

## Decisión GO/NO-GO para el 8º ejercicio
**GO.** La telemetría de E5 es ahora estructurada (metadata, no parseo), corregida (sesgo separado, coste no
negativo, premature real, mixed-load balanceado, feedback priorizado) y **persistida históricamente** en un store
aislado cubierto por el borrado de cuenta. Bloqueadores transversales (analytics `mental_speed_*`, §15 físico,
Firestore-sync, Android/iOS runtime) siguen abiertos y NO crecen con más ejercicios. E5 sigue **MVP-candidate**,
NO MVP-ready completo.
