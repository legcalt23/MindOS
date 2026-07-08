# MC9-C-R3 — E5 · Búsqueda Visual: Persistence Semantics & Integration Audit

Fase de INTEGRIDAD (no de producto) sobre la telemetría histórica de E5 introducida en MC9-C-R2 (commit
`d086c303`). Cierra riesgos semánticos de privacidad, orden, versionado, idempotencia y preparación de sync,
**sin reabrir la arquitectura**, sin migrar `TrialRecord`/`MeasurementRun`, sin AI Coach, sin ranking/PvP y sin
implementar Firestore sync (solo se declara su contrato futuro).

## API final del telemetry store

```kotlin
interface ExerciseTelemetryStore {
    suspend fun save(record: ExerciseTelemetryRecord)
    suspend fun forSession(userId: String, sessionId: String): ExerciseTelemetryRecord?  // ← userId obligatorio
    suspend fun forUser(userId: String): List<ExerciseTelemetryRecord>                    // reciente primero
    suspend fun deleteUserData(userId: String): Int
}
```

Impls: `SecureExerciseTelemetryStore` (durable, cifrada, sobre el `SecureKeyValueStore` del namespace
`mindboost_speed`, prefijo de clave `tele:{userId}::{sessionId}`) e `InMemoryExerciseTelemetryStore` (tests /
desktop / iOS). Ambas comparten el mismo contrato y el mismo orden histórico.

Helpers TIPADOS de E5 (extensiones, ninguna otra capa (de)serializa `summaryJson` a mano):

```kotlin
suspend fun ExerciseTelemetryStore.saveVisualScanSummary(userId, summary, createdAtEpochMs = summary.createdAtEpochMs)
suspend fun ExerciseTelemetryStore.visualScanSummaryForSession(userId, sessionId): VisualScanTelemetrySummaryV1?
suspend fun ExerciseTelemetryStore.visualScanSummariesForUser(userId): List<VisualScanTelemetrySummaryV1>
```

## Privacidad por userId (§2)

`forSession` **exige el `userId`** y lee una clave EXACTA (`tele:{userId}::{sessionId}`): no escanea otros
usuarios y no puede devolver el registro de otro usuario, aunque dos usuarios compartieran `sessionId`. Se
eliminó la firma vieja `forSession(sessionId)` (escaneo cross-user). Tests: mismo `sessionId` en dos usuarios no
se mezcla; `forSession(userA, s)` nunca devuelve el de `userB`; `forUser(userA)` solo devuelve `userA`.

## Orden histórico determinista (§3)

`forUser` devuelve **reciente primero**: `sortedByDescending(createdAtEpochMs)` con desempate estable por
`sessionId`. Idéntico en la impl durable y la in-memory (tests lo fijan en ambas). Elección: historial reciente
primero es lo natural para una lista de sesiones pasadas.

## Versionado (§5) — tres ejes ORTOGONALES

| Campo | Dónde | Qué versiona |
|---|---|---|
| `schemaVersion` (`EXERCISE_TELEMETRY_RECORD_VERSION = "1.0.0-mc9c-r3"`) | `ExerciseTelemetryRecord` | esquema del RECORD genérico (envoltura) |
| `telemetryVersion` | `ExerciseTelemetryRecord` | formato del summary (payload); hoy = `metricsVersion` |
| `summaryVersion` (`VISUAL_SCAN_TELEMETRY_SUMMARY_VERSION = "1.0.0-mc9c-r3"`) | `VisualScanTelemetrySummaryV1` | esquema del summary E5 |
| `metricsVersion` (`VISUAL_SCAN_METRICS_VERSION = "1.1.0-mc9c-r2"`) | dentro del summary | versión de FÓRMULAS del mapper |

Test: el summary round-trips con `summaryVersion`; `summaryVersion != metricsVersion`; `unavailableSignals` se
conserva.

## Estado de sincronización (§4) — contrato futuro, NO implementado

`ExerciseTelemetryRecord` gana `syncStatus: TelemetrySyncStatus = LOCAL_ONLY`, `updatedAtEpochMs = createdAtEpochMs`,
`schemaVersion`. En el MVP local actual TODO record es `LOCAL_ONLY`. `PENDING_SYNC / SYNCED / SYNC_FAILED` están
modelados pero **no hay capa de sync a Firestore**: son contrato futuro para no re-versionar el esquema cuando
llegue. `encodeDefaults = true` + `ignoreUnknownKeys = true` (`SpeedJson`) garantizan round-trip y lectura hacia
delante.

## Idempotencia (§7)

Política: **sobrescribir por clave exacta** `tele:{userId}::{sessionId}`. Re-guardar la misma sesión sobrescribe
(no duplica); una versión nueva actualiza; una sesión distinta crea un segundo record. Tests cubren los tres casos
en el store y vía el helper tipado.

## Guardado best-effort (§8)

El summary se construye y guarda en un `LaunchedEffect(state.lastSession?.session?.id)` de `SpeedExerciseScreen`
(keyed por el id de sesión ⇒ corre **una vez por corrida terminada**, no por recomposición). La escritura pasa por
`saveVisualScanSummary` (idempotente); `CancellationException` se relanza; cualquier otro fallo se traga y emite un
breadcrumb PII-free (`e5_telemetry_persist_failed`) — la telemetría NUNCA rompe la sesión de entrenamiento.

**Durabilidad honesta:** el `MeasurementRun` es durable por su propia vía (`store.saveRun`); el **summary de
telemetría es best-effort local** hasta que exista una capa de sync/retry. Matar el proceso entre el fin de la
corrida y el efecto de guardado pierde ESE summary (no el `MeasurementRun`). No se promete durabilidad total.

## Debug delete / account erasure (§9)

- **Debug delete** ("Borrar mis datos"): ya llama `graph.store.deleteUserData(userId)` **y**
  `graph.telemetryStore.deleteUserData(userId)` (R2). Muestra el total combinado.
- **Account deletion:** `SecureNamespaceUserDataEraser.eraseUserData` → `clearNamespace()` sobre el store
  `mindboost_speed`, que borra TODAS las claves del namespace incluidas las `tele:`. La telemetría está cubierta
  sin nuevo namespace ni entrada nueva en el eraser. Test local: compartiendo un `InMemorySecureKeyValueStore`,
  `clearNamespace()` borra los records de telemetría.
- **No probado:** el borrado de cuenta end-to-end en dispositivo (Android Keystore) — sin runtime QA.

## Estado de Firestore sync

**No implementado (diferido).** El record queda preparado (`syncStatus`, `updatedAtEpochMs`, `schemaVersion`) para
una futura capa de sync/retry, que definirá su propio contrato (upload, backoff, resolución de conflictos). Esta
fase no toca Firestore.

## Gates

`compileKotlinDesktop` · targeted tests (`VisualScanTelemetryPersistenceTest`) · full `desktopTest` ·
`compileAndroidMain` · `detekt` (único finding esperado: el foreign untracked `IndexedSecureKeyValueStore:61`) ·
copy scanners (base + CommonTest + DesktopTest) · `scan:secrets`.
**Android runtime NO probado** (sin instrumented) · **iOS NO probado** (sin Mac) · **Firestore NO probado**
(no implementado).

## Riesgos residuales

1. Summary best-effort: proceso muerto entre fin de corrida y guardado pierde ESE summary (no el `MeasurementRun`).
2. Sin sync a Firestore: la telemetría histórica es local-only.
3. Borrado de cuenta end-to-end no verificado en dispositivo (solo namespace-wipe probado en JVM).
4. E5 sigue **MVP-candidate**, NO MVP-ready completo: analytics `mental_speed_*`, §15 físico, sync y runtime
   Android/iOS siguen abiertos.

## Decisión GO/NO-GO para el 8º ejercicio

**GO.** La persistencia de E5 es ahora privada por `userId`, de orden determinista, versionada en sus tres ejes,
idempotente, con estado de sync preparado y cubierta por el borrado de cuenta y de datos. Los bloqueadores
restantes son transversales (sync, analytics, runtime QA) y no crecen con más ejercicios. No iniciar el 8º sin
confirmación explícita.
