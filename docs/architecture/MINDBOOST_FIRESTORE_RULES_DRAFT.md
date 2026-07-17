# MindBoost — Firestore Rules DRAFT (Enfoque session pilot)

**Fecha:** 2026-07-04 · **Autor:** Staff Mobile Engineer · **Estado:** DRAFT — NO desplegado

> **Por qué un doc separado y no una edición de `firestore.rules`:** desplegar reglas sin verificarlas contra
> el emulador (que exige **JDK 21** y no puede correrse aquí — el build usa JDK 17 y el IDE posee el daemon
> Gradle) es un cambio de cara externa con riesgo de producción. El piloto de Phase 1 **no cablea aún** el
> data source real de Firestore (eso es Phase 1.1), así que ninguna regla nueva hace falta viva todavía. Este
> borrador es el contrato a revisar + verificar en emulador **antes** de tocar `firestore.rules`.

---

## 1. Ruta y modelo

```
users/{userId}/sessions/{sessionId}
```

- `sessions/{sessionId}` = **evidencia inmutable** de una sesión (resumen cloud-safe, un doc por sesión).
- El `sessionId` del documento ES el id del doc y coincide con `SyncSessionId` (Firestore-safe, ≤200 chars,
  sin `/ \` ni espacios ni control chars). En Enfoque hoy es `"<userId>:<endedAtMs>"`.
- **`blockScores/{blockId}` NO se implementa en Phase 1** (snapshot mutable del estado por bloque). Queda para
  Phase 1.1 / Phase 2 y **no se mezcla** con `sessions` (semánticas distintas: evidencia inmutable vs snapshot
  mutable). Ver `MINDBOOST_FIRESTORE_SYNC_DESIGN.md §3.10`.

## 2. Reglas propuestas (a revisar + verificar en emulador antes de desplegar)

Reutiliza los helpers existentes (`signedIn()`, `uid()`, `isOwner(userId)`) y **añade** `isAppChecked()` (hoy
NO existe en `firestore.rules`; sí en `storage.rules` — aprendizaje 2026-05-26). App Check es un control
ortogonal a la autorización: ambos son necesarios para contenido privado.

```
// helper nuevo (parametrizar según cómo se exponga App Check en este proyecto):
function isAppChecked() {
  return request.app != null;               // enforce device attestation
}

function focusSessionKeys() {
  return [
    'userId', 'sessionId', 'moduleId', 'exerciseId',
    'startedAtMs', 'endedAtMs', 'durationMs', 'completed',
    'validForCalibration', 'normalizedScore',
    'scoringVersion', 'appVersion', 'sourcePlatform',
    'fingerprint', 'createdAtMs', 'updatedAtMs', 'schemaVersion'
  ];
}

function validFocusSessionCreate(userId, sessionId) {
  return isOwner(userId) &&
    isAppChecked() &&
    // el doc pertenece al usuario autenticado y su id coincide con el campo:
    request.resource.data.userId == userId &&
    request.resource.data.sessionId == sessionId &&
    // allowlist ESTRICTA: cualquier campo desconocido (rawTrials, blocks, trials, subjectiveState…) → DENEGADO
    request.resource.data.keys().hasOnly(focusSessionKeys()) &&
    request.resource.data.moduleId is string &&
    request.resource.data.startedAtMs is int &&
    request.resource.data.endedAtMs is int &&
    request.resource.data.durationMs is int &&
    request.resource.data.completed is bool &&
    request.resource.data.scoringVersion is string &&
    request.resource.data.fingerprint is string &&
    request.resource.data.schemaVersion is int;
}

match /users/{userId}/sessions/{sessionId} {
  // Owner-only lectura (App Check). Phase 1 NO lee del cloud; regla lista para la rehidratación (Phase 1.1).
  allow get:    if isOwner(userId) && isAppChecked();
  allow list:   if isOwner(userId) && isAppChecked() &&
                   request.query.limit != null && request.query.limit <= 100;

  // Create-only + inmutable. `create` ya implica documento inexistente en Firestore.
  allow create: if validFocusSessionCreate(userId, sessionId);

  // Evidencia inmutable: sin update de sesiones en Phase 1 (metadata-only update = decisión Phase 2 explícita).
  allow update: if false;

  // El borrado lo maneja el eraser de borrado de cuenta (backend), nunca el cliente.
  allow delete: if false;
}
```

## 3. Invariantes que estas reglas garantizan

| Invariante | Cómo |
|---|---|
| Owner-only, sin cross-user | `isOwner(userId)` en read/create; `userId == request.auth.uid` |
| App Check obligatorio | `isAppChecked()` en read/list/create |
| Sin raw trials / campos excesivos | `keys().hasOnly(focusSessionKeys())` → rechaza `rawTrials`, `blocks`, `trials`, `subjectiveState`, `email`, etc. |
| `userId`/`sessionId` consistentes con el doc | `data.userId == userId` && `data.sessionId == sessionId` |
| Sesiones inmutables | `allow update: if false` |
| Borrado solo backend | `allow delete: if false` |
| Reads acotadas | `allow list` exige `request.query.limit <= 100` (aprendizaje 2026-05-26) |
| Sin Elite/entitlement desde cliente | esta ruta no toca entitlements; siguen bajo sus reglas server-authoritative |

## 4. Plan de test de emulador (antes de desplegar) — `firestore.security.rules.test.mjs`

1. Owner crea su sesión con payload válido → **OK**.
2. Otro usuario intenta crear/leer `users/{owner}/sessions/*` → **DENEGADO** (cross-user).
3. Create con `rawTrials` (o cualquier clave fuera de la allowlist) → **DENEGADO**.
4. Create con `data.userId != userId` o `data.sessionId != <docId>` → **DENEGADO**.
5. `update` de una sesión existente → **DENEGADO**.
6. `delete` desde cliente → **DENEGADO**.
7. `list` sin `request.query.limit` → **DENEGADO**; con `limit <= 100` → **OK**.
8. Sin App Check → **DENEGADO**.

Comando (requiere **JDK 21**, un job de CI separado del build Kotlin JDK 17 — aprendizaje 2026-05-29):

```bash
npm run test:firestore-rules   # o el script real, con --project que empareje el projectId del .mjs
```

## 5. Gate de despliegue (pendiente, fuera de este turno)

- [ ] Revisión de las reglas + `isAppChecked()` (forma exacta de App Check en este proyecto).
- [ ] Suite de emulador **verde** (los 8 casos anteriores) en el job JDK 21.
- [ ] `firestore.rules` editado + `firestore_validate_security_rules` OK.
- [ ] Solo entonces: desplegar, y solo junto al wiring del data source real (Phase 1.1) tras consentimiento GDPR.

## 6. Actualización Phase 1.1 — el adapter real escribe exactamente esta forma

- La allowlist `focusSessionKeys()` **coincide 1:1** con los campos de `FocusSessionCloudDto` (Phase 1). El
  adapter real `GitLiveFocusSessionFirestoreGateway` escribe el DTO completo (sin raw trials) a
  `users/{userId}/sessions/{sessionId}` mediante **check-then-create** (`get` → si ausente `set`); la regla
  `allow update: if false` es el guard de sobrescritura autoritativo del servidor.
- **`firestore.rules` NO se edita en este turno** (exige verificación en emulador con JDK 21, imposible aquí; el
  IDE posee el daemon Gradle). Este borrador + el plan de emulador (§4) son el contrato a verificar antes de
  desplegar. El adapter y su clasificación se prueban con un **fake gateway** (`FirestoreFocusSessionRemoteDataSourceTest`);
  la ruta gitlive real y estas reglas quedan como **gate manual de emulador**.
- `blockScores` sigue **fuera del piloto** (snapshot mutable documentado en el design doc §3.10); no se añaden
  reglas de escritura para él en Phase 1.1.
