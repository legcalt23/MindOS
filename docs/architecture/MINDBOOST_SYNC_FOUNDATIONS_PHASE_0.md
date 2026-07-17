# MindBoost — Firestore Sync · Phase 0: Foundations

**Fecha:** 2026-07-04 · **Autor:** Staff Mobile Engineer · **Estado:** Fundación verificable (no es sync)

> **Alcance de este turno:** SOLO cimientos. No se implementa sync real, no se suben datos, no se editan
> reglas Firestore, no se toca scoring/calibración/dominio/taxonomía, no se migran namespaces, no se hace
> commit. Todo el código nuevo es PURO y testeable; los motores existentes NO se modifican (su adopción del
> id canónico es Phase 1). Verificación estática — los gates los ejecuta el dueño (el IDE posee el daemon).

---

## 1. Estado actual

MindBoost persiste local-first cifrado y **no** sube datos cognitivos. Cada motor genera su propio id de
sesión con patrones distintos; dos motores (Focus, Memoria) ya tienen un `value class SessionId` propio, pero
solo validan **non-blank** y no son necesariamente ids de documento Firestore válidos. No existía un tipo
canónico de id para la capa de sync, ni clasificación de resultados de escritura, ni fingerprint de sesión.

## 2. Auditoría de `sessionId` (Tarea A)

| Motor | Productor (archivo) | `sessionId` actual | estable | único | riesgo | acción |
|---|---|---|---|---|---|---|
| **Enfoque** | `FocusEvaluationStateHolder:119` / `FocusBaselineStateHolder:115` / `FocusSessionStateHolder:213` | `SessionId("$userId:$endedAtMs")` (value class) | sí (1×/sesión) | user+ms | **P2** | mapear a `SyncSessionId` en Phase 1; validación hoy solo non-blank |
| **Velocidad** | `SpeedSessionStateHolder:102` | `"$userId:speed:${clock.nowMillis()}"` | sí | user+ms | **P2** | mapear a `SyncSessionId` |
| **Memoria** | `MemorySessionStateHolder:234/274/316` (typed `SessionId`) · `DeferredRecallSessionStateHolder:94` | `"$userId:$endedAt"` / `"$userId:delayed:$now"` | sí | user+ms | **P2** | mapear a `SyncSessionId` |
| **Lógica** | `LogicStateHolder:179` | `"$userId:$endedAt"` | sí | user+ms | **P2** | mapear a `SyncSessionId` |
| **Decisiones** | `MindBoostHubScreen:97` → `DecisionsFlowStateHolder` | `"$userId:$now"` | sí | user+ms | **P2** | mapear a `SyncSessionId` |
| **Emociones** | `LocalEmotionRepository:26/101` | key `"${prefix(userId)}$startedAt"` (sin campo `sessionId` explícito) | sí | user+ms | **P2** | añadir campo `sessionId` explícito + mapear |
| **Creatividad** | `CreativitySessionCoordinator:58` | `"${exerciseId.name}-$monotonicStartMs"` | reloj monotónico (reinicia por proceso) | **NO user-scoped, colisión cross-proceso** | **P1** | añadir `userId` + reloj estable antes de sincronizar Creatividad |
| **Lenguaje** | — (sin store de producción) | — | — | — | **P3** | definir `sessionId` al crear el store |

**Lectura transversal:** todos los ids basados en timestamp (`"$userId:$ms"`) son **estables** (creados una
vez por sesión, no se regeneran en retry) pero dependen **solo de timestamp** → riesgo teórico de colisión en
el mismo milisegundo, y contradicen el criterio CEO "no depender solo de timestamp inseguro". El único id
**inseguro de verdad** es **Creatividad** (monotónico + sin `userId`): no sirve como id de documento Firestore
per-usuario. Mitigante: Creatividad **no alimenta el perfil** (CREATIVITY "—" en MVP, DC-11) y **no se
sincroniza** → sin impacto vivo hoy; su corrección es prerequisito **antes** de cualquier sync de Creatividad.

Ninguna corrección se aplicó a los motores este turno (retrofit = Phase 1, tocaría dominio/presentación).

## 3. Decisiones aplicadas (Tareas B–E)

### `sessionId` — tipo canónico `SyncSessionId`
Se crea `SyncSessionId` (value class, `core/sync`) **distinto a propósito** de los `SessionId` per-feature de
focus/memory (que ya existen y validan solo non-blank): nombrarlo igual habría creado un TERCER `SessionId`
confuso. `SyncSessionId` valida id de **documento Firestore** (no vacío, sin `/`/`\`/espacios ni chars de
control, no `.`/`..`, no `__*__`, ≤200 chars) y ofrece `parseOrNull` para input no confiable. Un
`SyncSessionIdGenerator` (interfaz) + `DefaultSyncSessionIdGenerator` generan `"{module}-{startedAtMs}-{suffix}"`
con sufijo aleatorio (colisión-resistente), **una sola vez** por sesión; se persiste y se reutiliza sin
regenerar (idempotencia). Los motores lo adoptan en Phase 1.

### `blockScores` / `scoreEvents` — semántica cerrada (Tarea C)
Documentada en `MINDBOOST_FIRESTORE_SYNC_DESIGN.md §3.10`. `blockScores/{blockId}` = **snapshot mutable del
estado actual** (last-write-wins por `updatedAtMs`), NUNCA lista de eventos. `sessions/{sessionId}` = evidencia
inmutable. `scoreEvents/{eventId}` = historial inmutable **opcional/futuro** (fuera de MVP), para no contaminar
`blockScores` con semántica mixta.

### Clasificación segura de escrituras (Tarea D)
`SyncWriteResult` (sealed) + `SyncWriteClassifier` (puro): `Synced` / `AlreadySyncedDuplicate` / `Conflict` /
`PermissionDenied` / `NetworkFailure` / `ValidationFailure` / `UnknownFailure`. Invariantes:
- **`PERMISSION_DENIED` → `PermissionDenied`, JAMÁS `Synced`.**
- `ALREADY_EXISTS` → `AlreadySyncedDuplicate` **solo** tras verificación positiva (doc remoto existe **Y** mismo
  `userId` **Y** mismo fingerprint). Sin verificación, o si diverge, → `Conflict(reason)` (`UNVERIFIED_EXISTING`
  / `DIFFERENT_USER` / `DIFFERENT_FINGERPRINT`). Un "ya existe" **nunca** es éxito silencioso.
- `NetworkFailure` = reintentable (sesión queda PENDING); `ValidationFailure` = **no** reintentar en bucle.
Sin `try/catch` silencioso: `RawWriteOutcome` normaliza el error del SDK en el borde (Phase 1) y se clasifica
explícitamente.

### Fingerprint mínimo (Tarea E)
`SessionFingerprint` (puro): `userId, sessionId, moduleId, exerciseId?, startedAtMs, completedAtMs, durationMs,
scoringVersion, normalizedScore?`. **Sin raw trials.** `canonical()` (orden fijo, tokens de null explícitos) +
`fingerprint()` (FNV-1a de 64 bits, determinista cross-plataforma). `normalizedScore` es nullable → `null != 0`
preservado también en el fingerprint. La igualdad de `SessionFingerprint` ES el test de "misma sesión" que usa
el clasificador.

> **`fingerprint()` NO es criptográfico ni anti-trampa** — es una clave de idempotencia/conflicto (FNV-1a,
> determinista, rápida). No confundir con seguridad. **Almacenamiento cloud:** se guarda como MÍNIMO el
> `fingerprint` + los campos necesarios para verificar ownership/sesión (`userId`, `sessionId`, `moduleId`,
> tiempos, `scoringVersion`); `canonical()` NO se almacena como dato cloud salvo decisión futura explícita.
> **Nunca raw trials.**

## 4. Código añadido

| Archivo | Cambio | Razón |
|---|---|---|
| `core/sync/SyncSessionId.kt` | `SyncSessionId` + `SyncSessionIdGenerator` + `DefaultSyncSessionIdGenerator` | id canónico Firestore-safe, generado 1×, idempotente |
| `core/sync/SyncWriteResult.kt` | `SyncWriteResult` (sealed) + `ConflictReason` + `RawWriteOutcome` + `RemoteSessionProbe` + `SyncWriteClassifier` | permiso-denegado ≠ éxito; duplicado solo tras verificación |
| `core/sync/SessionFingerprint.kt` | `SessionFingerprint` + `canonical()` + `fingerprint()` (FNV-1a) | idempotencia/conflicto sin datos sensibles |
| `desktopTest/.../core/sync/SyncSessionIdTest.kt` | 8 tests | validación + generación |
| `desktopTest/.../core/sync/SyncWriteResultTest.kt` | 9 tests | clasificación segura |
| `desktopTest/.../core/sync/SessionFingerprintTest.kt` | 6 tests | fingerprint determinista, null≠0 |
| `docs/architecture/MINDBOOST_FIRESTORE_SYNC_DESIGN.md` | +§3.10 semántica blockScores/scoreEvents | Tarea C |

## 5. Qué queda pendiente (Phase 1+)
- **Adopción del id canónico por motor** (mapear cada `sessionId` actual → `SyncSessionId`), empezando por los
  motores que sí sincronizarán primero.
- **Corregir Creatividad (P1)** antes de sincronizarla: `userId` + reloj estable en el `sessionId`.
- **Emociones:** exponer un campo `sessionId` explícito (hoy es solo la key de store).
- **Borde de errores:** mapear el error del SDK Firebase → `RawWriteOutcome` (androidMain), sin `try/catch`
  silencioso.
- **Reglas Firestore** (`isAppChecked()` helper + colecciones nuevas) y **eraser de borrado de cuenta** — solo
  diseño hoy (design doc §3.5/§3.8).
- **Sync engine** real (cola, backoff, SyncState) — Phase 2.

## 6. Riesgos restantes
- **P1** — Creatividad genera un `sessionId` monotónico sin `userId`: inservible como id de documento
  per-usuario. Sin impacto vivo (no perfil, no sync), pero bloqueante para sincronizar Creatividad.
- **P2** — Todos los ids de motor dependen solo de timestamp (colisión en el mismo ms, improbable). El
  `SyncSessionId` canónico lo resuelve con sufijo aleatorio; falta la adopción (Phase 1).
- **P2** — Emociones no expone un campo `sessionId` explícito (solo key de store); requiere un pequeño cambio
  de modelo en Phase 1.
- **P3** — `RawWriteOutcome`/`RemoteSessionProbe` son un MODELO de los resultados; el mapeo real desde el SDK
  Firebase y la lectura del doc remoto son Phase 1 (androidMain).
- **P3** — Lenguaje no tiene store de producción; su `sessionId` se define cuando exista.

## 7. Siguiente fase recomendada (una)
**Phase 1 — adopción del id + borde de errores en UN motor piloto (Enfoque):** cablear `SyncSessionId`
(generado 1× al iniciar la sesión, persistido junto al `SessionResult`), construir el `SessionFingerprint`
desde el `ScoreResult` sellado, y mapear el error del SDK → `RawWriteOutcome`. Todo detrás de un feature flag
apagado y SIN activar escritura cloud masiva. Esto valida los cimientos end-to-end en un solo motor antes de
generalizar.

## 8. Checklist de entrada para Phase 1
- [ ] Feature flag de sync **apagado** por defecto (nada se sube sin opt-in).
- [ ] Consentimiento GDPR explícito antes de la primera subida (design doc §3.4).
- [ ] `isAppChecked()` añadido a `firestore.rules` + suite de emulador (design doc §3.5).
- [ ] Colecciones nuevas cableadas en `executeAccountDeletion` + `AccountBoundErasureCoverageTest` (design doc §3.8).
- [ ] Creatividad (P1) corregida ANTES de incluirla en sync.
- [ ] El motor piloto genera `SyncSessionId` **una vez** y lo persiste (no se regenera en retry).
- [ ] `RawWriteOutcome` mapeado desde el SDK sin `try/catch` silencioso.
- [ ] Sin subir raw trials — solo `SessionFingerprint` + resúmenes estructurados.

---

## 9. Actualización 2026-07-04 — Phase 1 (piloto Enfoque) implementada

El piloto de Enfoque está **implementado + testeado** (aditivo, NO montado en el grafo vivo, NO activado).
Detalle en `MINDBOOST_FIRESTORE_SYNC_PHASE_1_FOCUS_PILOT.md`. Decisiones nuevas relevantes para estos cimientos:

- **`SyncSessionId` adoptado en la capa sync de Enfoque** vía `SyncSessionId.parseOrNull(result.sessionId.value)`
  (el id `"<userId>:<endedAtMs>"` es válido y se conserva; no se toca el dominio). Guard extra: el prefijo owner
  debe igualar el `userId` autenticado.
- **Cola de sync = `core/sync/SyncQueueEntry`/`SyncQueueRepository`** (genérica) con `InMemorySyncQueueRepository`.
  La persistencia durable de la cola (cifrada + wiring en el eraser de borrado de cuenta) se **difiere a Phase
  1.1**: perder una fila in-memory NO pierde progreso (el resumen local ya es durable), solo retrasa el mirror.
- **Feature flag** ahora vive en el sistema real: `FeatureFlags.mindboostSyncFirestoreEnabled` (Default/Static/
  RemoteConfig), OFF por defecto incluida producción.
- **`SyncWriteResult` → estado de cola** fail-closed: `NetworkFailure`→`PENDING`; duplicado verificado→`SYNCED`;
  `PermissionDenied`→`FAILED` (jamás `SYNCED`).
- **Reglas Firestore**: borrador en `MINDBOOST_FIRESTORE_RULES_DRAFT.md`, sin editar `firestore.rules` (exige
  emulador JDK 21 para verificar antes de desplegar).
