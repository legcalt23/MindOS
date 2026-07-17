# MindBoost — Diseño de sincronización a Firestore del progreso cognitivo

**Fecha:** 2026-07-04
**Autor:** Staff Mobile Architect
**Estado:** Nota de diseño — decisión CEO
**Alcance de este turno:** **SOLO DISEÑO.** No se escribe código de sync, no se editan reglas Firestore, no se migran namespaces, no se toca el scoring de producción, no se implementa IA Coach ni Elite, no se hace commit. Todo lo aquí propuesto es una especificación revisable, no una implementación.

## Propósito

MindBoost persiste hoy los datos cognitivos **local-first y cifrados** (`SecureKeyValueStore`, AES-256-GCM + Android Keystore; `LocalFocusRepositories.kt` es el patrón canónico) y **nunca** los sube a la nube. La decisión del CEO es **añadir sincronización a Firestore como continuidad privacy-by-design**, conservando el local-first: el guardado local sigue siendo inmediato y autoritativo, y la nube es una **réplica de resúmenes estructurados** que jamás bloquea el entrenamiento ni sube trials crudos.

Este documento define el modelo de datos, la clasificación de sensibilidad, las reglas Firestore propuestas, el motor de sincronización, la extensión de repositorios sin reescritura, la integración con el borrado de cuenta y la matriz de tests. Cada afirmación técnica está anclada a código real con `archivo:línea`. Lo no verificable se marca **UNVERIFIED**.

> **Aviso de honestidad de cumplimiento (obligatorio):** este diseño satisface la intención de continuidad del PRD de forma **parcial**: cubre continuidad de progreso, recuperación multi-dispositivo y bases futuras. NO afirma cumplimiento total del PRD. En particular, la resolución de conflictos, la deduplicación exacta y la reconstrucción del perfil desde la nube tras reinstalación requieren decisiones adicionales que se marcan como **UNVERIFIED / pendientes de validación**. No existe ninguna suite de tests de sync a Firestore en el repo hoy.

---

## 3.1 Objetivo

Añadir una capa de sincronización a Firestore de los **resúmenes estructurados** de MindBoost para lograr:

1. **Continuidad de progreso** — que el perfil cognitivo y el historial de sesiones sobrevivan a una reinstalación o cambio de dispositivo, hoy imposible: los datos viven solo en el `SecureKeyValueStore` local (`LocalFocusRepositories.kt`).
2. **Recuperación multi-dispositivo** — que el usuario retome su progreso al iniciar sesión en otro dispositivo.
3. **Base futura para IA Coach** — que exista una entrada estructurada y no-PII (`aiCoach/summary`, `cognitiveProfile/main`) para un coach futuro. **No se implementa IA Coach este turno.**
4. **Base futura para Elite** — que el gating de suscripción tenga una superficie server-side donde apoyarse (`subscription`/`entitlement` escritos solo por backend). **No se implementa Elite este turno.**
5. **Base futura para analytics internos sin invadir privacidad** — métricas derivadas agregadas, nunca trials crudos ni PII.

## 3.2 No objetivos (MVP)

- **NO** subir raw trials completos. El contrato del dominio ya lo prohíbe: `ModuleContribution` "NEVER carries raw trials, timings, or anything uploadable" (`ModuleContribution.kt:5-6`); los repos locales guardan "session SUMMARIES (no raw trials)" (`LocalFocusRepositories.kt:51`).
- **NO** ranking avanzado, **NO** PvP, **NO** anti-trampa completo (INFRA-03 anti-gaming es capa aparte y parcial), **NO** datos cognitivos públicos.
- **NO** claims médicos, **NO** diagnóstico (`CognitiveTaxonomy.kt`: dimensiones "functional, descriptive — NEVER clinical/diagnostic labels").
- **NO** comparativas por país/edad.

---

## 3.3 Modelo de datos Firestore propuesto

### Estructura de árbol (todo bajo `users/{userId}`, owner-scoped)

```
users/{userId}
├── (doc raíz)                         # identidad mínima + estado de cuenta (ya usado por accountDeletion.ts)
├── profile/main                       # identidad mínima de MindBoost (consent, flags de sync)
├── cognitiveProfile/main              # snapshot del brain-profile (derivado del MindBoostProfile local)
├── blockScores/{blockId}              # snapshot MUTABLE del estado actual del bloque/módulo (no eventos)
├── sessions/{sessionId}               # evidencia INMUTABLE de una sesión de entrenamiento (de SessionResult)
├── scoreEvents/{eventId}              # (futuro/opcional) historial inmutable de cambios de score — fuera de MVP
├── syncState/main                     # cursor/estado de sync (mirror opcional del cursor local)
├── aiCoach/summary                    # resumen no-PII para IA Coach (futuro; solo escritura derivada)
└── plans/current                      # plan de entrenamiento vigente (futuro)
```

**Nota sobre el doc raíz `users/{uid}`:** ya existe y lo usa el borrado de cuenta (`accountDeletion.ts:435` lo borra tras marcar `DeletionPending`). El nuevo árbol MindBoost cuelga de él como **subcolecciones**; no colisiona con los campos existentes. Borrar el doc raíz **no** cascadea a las subcolecciones (Firestore no cascadea), de ahí el borrado explícito por subcolección en §3.8.

### Convenciones transversales (todos los documentos)

| Campo | Clasificación | Notas |
|---|---|---|
| `schemaVersion: Int` | Obligatorio | Espejo de `MindBoostProfileSchema.CURRENT` (`MindBoostProfileDtos.kt`) y del `FocusSchema` local. Bump en cambios incompatibles. |
| `updatedAtMs: Long` | Obligatorio · Derivado | Epoch-millis del reloj inyectado (`EpochClock`, `MindBoostProfileService.kt:36`), NUNCA `0L` fabricado (`ModuleContribution.kt:16-18`, learning 2026-07-04 sobre centinelas de tiempo). |
| `userId: String` | Obligatorio | Debe cumplir `== request.auth.uid` en reglas. |

### `TrainingSession` → `users/{uid}/sessions/{sessionId}`

Derivado de `SessionResult` (`focus/domain/model/Results.kt:53-65`), pero **solo el resumen**: sin `blocks[].trials[]`. Los `TrainingSession` locales ya guardan summaries vía `toSummaryDto()` (`LocalFocusRepositories.kt:59`).

| Campo | Fuente real (file:line) | Clasificación |
|---|---|---|
| `sessionId` | `SessionResult.sessionId` (`Results.kt:54`) — en Enfoque el valor es `"<userId>:<endedAtMs>"` (`LocalFocusRepositories.kt:58`) | Obligatorio · Cloud-safe (idempotency key natural, **ver caveat abajo**) |
| `moduleId` | `ScoreModuleId` (`ScoreTypes.kt`) / `CognitiveModuleId` (`CognitiveTaxonomy.kt`) | Obligatorio · Cloud-safe |
| `exerciseFocus` | `SessionResult.exerciseFocus` (`Results.kt:56`) | Obligatorio · Cloud-safe |
| `startedAtMs` / `endedAtMs` | `Results.kt:58-59` | Obligatorio · Derivado · Cloud-safe |
| `durationMs` | `SessionResult.durationMs` derivado (`Results.kt:63`) | Derivado · Cloud-safe (usado por el gate `ProfileSessionEligibility.MIN_DURATION_MS = 120_000L`) |
| `totalTrials` | `SessionResult.totalTrials` (`Results.kt:64`) — **el CONTEO, no los trials** | Derivado · Cloud-safe |
| `sessionMetricsSummary` | Reducción allowlisted de `SessionResult.sessionMetrics: MetricSet` (`Results.kt:61`; ver contrato abajo) | Opcional · Derivado · Cloud-safe (**solo agregados; nunca por-trial**) |
| `subjectiveState` (sleep/caffeine/mood/hour) | `SubjectiveState` (`Results.kt:69-80`) | **Local-only en MVP** — es covariate sensible (sueño/ánimo). No subir sin decisión de privacidad explícita. |
| `blocks[].trials[]` | `BlockResult.trials` (`Results.kt:42`) | **Prohibido en Cloud** — raw trials, nunca se suben. |

> **Contrato concreto de `sessionMetricsSummary` (allowlist explícita).** `MetricSet` es un `typealias MetricSet = Map<MetricKey, MetricValue>` (`MetricKey.kt:72`); `MetricValue` = `{ key, value: Double, n: Int, confident: Boolean, unit: MetricUnit }` (`MetricKey.kt:57-63`). El resumen a la nube es un **mapa acotado** de un subconjunto **fijo y allowlisted** de `MetricKey` (`MetricKey.kt:10-36`), cada uno con `{ value, n, confident, unit }` — **jamás** con datos por-trial (`MetricValue` ya es un agregado por sesión, no lleva timings individuales). Subconjunto propuesto para MVP: `ACCURACY`, `RT_MEDIAN`, `RT_CV` (las tres métricas primarias honestas descritas en `MetricKey.kt:4-7`). El resto del catálogo (24 keys en total, `:10-36`) se marca **UNVERIFIED / pendiente de decisión de privacidad** hasta acordar cuáles son cloud-safe; el default es **no subirlas**. Regla de honestidad heredada: si `confident == false` (`MetricKey.kt:54-55`), la nube almacena el valor pero la UI/IA Coach lo trata como "datos insuficientes", nunca como número duro.

> **Caveat de la idempotency key (UNVERIFIED para 7 de 8 motores).** El formato `"<userId>:<endedAtMs>"` está **verificado solo en Enfoque** (`LocalFocusRepositories.kt:58`). Los otros motores (Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental, Memoria) **NO están verificados** que produzcan una clave de sesión única, user-scoped y libre de colisiones. Antes de extender el tríptico (§3.7) a cada motor, hay que **verificar o normalizar** su `sessionId` a una clave con esas tres garantías; hasta entonces, la invariante "no duplicados entre dos dispositivos" (test #7) solo está probada para Enfoque.

### `BlockScore` → `users/{uid}/blockScores/{blockId}`

**Snapshot MUTABLE del estado ACTUAL** de un bloque/módulo cognitivo (NO un evento por sesión, NO una lista de eventos). `blockId` es el id **estable del bloque/módulo** — **nunca** un `sessionId`. Se reemplaza (last-write-wins por `updatedAtMs`) a medida que nuevas sesiones actualizan el estado del bloque; el veredicto por-sesión (`ScoreResult`) vive en `sessions/{sessionId}` como **evidencia**, y si algún día se necesita auditoría histórica de cambios de score, en `scoreEvents/{eventId}` (futuro). Ver §3.10.

| Campo | Fuente real / semántica | Clasificación |
|---|---|---|
| `userId` | `== request.auth.uid` | Obligatorio |
| `blockId` / `moduleId` | id **estable del bloque/módulo** (`ScoreModuleId` / `CognitiveModuleId`, `CognitiveTaxonomy.kt`) — **NUNCA** un `sessionId` | Obligatorio · Cloud-safe |
| `score0to100` (o `score`) | valor ACTUAL del bloque, derivado del pipeline de scoring/agregación | **Opcional/nullable** — `null` mientras calibra; `null != 0` se serializa como ausente, **nunca** 0 |
| `status` (rank / calibrationStatus) | `DimensionStatus` (CALIBRATING / EMERGING / ESTABLISHED) | Obligatorio · Cloud-safe |
| `confidence` | confianza agregada actual (`ModuleContribution.confidence` / `DimensionState.confidence`) | Derivado · Cloud-safe |
| `validSessionCount` | `SessionEvidence.validSessions` (`CalibrationPolicy`) | Derivado · Cloud-safe |
| `totalSessionCount` | conteo total de sesiones del bloque (si disponible) | Opcional · Derivado · Cloud-safe |
| `lastSessionId` | `SyncSessionId` de la última sesión que actualizó el snapshot | Obligatorio · Cloud-safe |
| `lastSessionAtMs` | epoch-millis de esa última sesión | Derivado · Cloud-safe |
| `scoringVersion` | `ScoringVersionRegistry` — versión de fórmula activa | Obligatorio · Cloud-safe |
| `updatedAtMs` | epoch-millis del reloj inyectado (last-write-wins) | Obligatorio · Derivado |
| `schemaVersion` | versión de esquema del snapshot | Obligatorio |

> **`blockScores` es un snapshot MUTABLE:** se sobrescribe con el estado más reciente (§3.6, last-write-wins), NO acumula historial. La evidencia por-sesión vive en `sessions/{sessionId}`; el historial de cambios (si se necesita) en `scoreEvents/{eventId}`. `blockScores` **nunca** usa `sessionId` como `blockId` ni se documenta como lista de eventos.

### `CognitiveProfile` → `users/{uid}/cognitiveProfile/main`

Derivado de `MindBoostProfile` (`MindBoostProfile.kt:71-80`) — hoy persistido local vía `MindBoostProfileDto` (`MindBoostProfileDtos.kt`). El DTO de Firestore **reutiliza ese mismo shape** (mismo `schemaVersion`).

| Campo | Fuente real (file:line) | Clasificación |
|---|---|---|
| `userId` | `MindBoostProfile.userId` (`MindBoostProfile.kt:72`) | Obligatorio |
| `dimensions[]` (9 dimensiones) | `MindBoostProfile.dimensions` (`MindBoostProfile.kt:73`); taxonomía = 9 `CognitiveDimension` (`CognitiveTaxonomy.kt`) | Obligatorio · Derivado · Cloud-safe |
| `dimensions[].value: Double?` | `DimensionState.value` — `null` en CALIBRATING (`MindBoostProfile.kt:21-22,35`) | **Opcional/nullable** — se renderiza CALIBRATING como ausente, **nunca 0** |
| `dimensions[].status` | `DimensionStatus` = CALIBRATING / EMERGING / ESTABLISHED (`MindBoostProfile.kt:4-13`) | Obligatorio · Cloud-safe |
| `overall.compositeIndex: Double?` | `OverallSummary.compositeIndex` — promedio simple de ESTABLISHED, `null` si < 3 (`MindBoostProfile.kt:60-65`; `MindBoostProfileAggregator.kt:95-113`) | **Opcional/nullable** · Derivado · Cloud-safe |
| `updatedAtMs` | `MindBoostProfile.updatedAtMs` — reloj inyectado (`MindBoostProfile.kt:75`) | Obligatorio · Derivado |

> **Taxonomía (canon).** Superficie de producto = 8 motores (Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental, Memoria). Taxonomía interna = 9 `CognitiveDimension` y 9 `CognitiveModuleId`. El motor **Memoria mapea a DOS dimensiones internas** (WORKING_MEMORY + EPISODIC_MEMORY; los módulos internos son `MEMORIA` + `MEMORIA_EPISODICA`, alimentados por `MemoryProfileSource`/`PairsProfileSource`/`EpisodicProfileSource`); los otros 7 motores son 1:1. El DTO de nube almacena las **9 dimensiones**, no los 8 motores.

> **Rigor preservado (crítico).** La nube almacena el **resultado derivado** del scoring, no reimplementa scoring. El `compositeIndex` es "promedio simple de las dimensiones ESTABLISHED, requiere ≥ 3 establecidas, si no `null`" (`MindBoostProfileAggregator.kt:95-113`). Una dimensión puede tener valor (EMERGING) y aun así quedar **excluida** del Global Score. La fórmula ponderada simple del PRD (`0.30·Acc + 0.20·Speed + …`) es un **baseline conceptual**, NUNCA una instrucción para degradar `MindBoostProfileAggregator`. La sync **no toca** el modelo de scoring: se preservan los scorers bespoke por módulo y la state machine honesta CALIBRATING/EMERGING/ESTABLISHED (autoridad `CalibrationPolicy`: EMERGING a ≥2 administraciones válidas cross-session; ESTABLISHED a ≥5 sesiones válidas en ≥3 días distintos; gate de validez de sesión = 120000 ms, `ProfileSessionEligibility.MIN_DURATION_MS`).

### `SyncState` → `users/{uid}/syncState/main`

**Tipo nuevo, no existe en el repo hoy.** Espejo remoto **opcional** del cursor local.

| Campo | Clasificación | Notas |
|---|---|---|
| `lastSyncedAtMs: Long` | Derivado · Cloud-safe | Última reconciliación completada. |
| `lastSyncedSessionId: String?` | Opcional · Cloud-safe | Cursor de la cola. |
| `schemaVersion: Int` | Obligatorio | |
| `pendingCount` / `failedCount` | **Local-only** | La **autoridad** de la cola es local (§3.6); no se suben. |

### `AICoachSummary` → `users/{uid}/aiCoach/summary` (futuro, no se llena este turno)

| Campo | Clasificación | Notas |
|---|---|---|
| `dominantDimension` / `developingDimension` | Derivado · Cloud-safe · Apto IA Coach | Espejo de `OverallSummary.strongest` / `developing` (`MindBoostProfile.kt:63-64`). |
| `compositeIndexBand` | Derivado · Cloud-safe · Apto IA Coach | Banda descriptiva, no número clínico. |
| `updatedAtMs` | Obligatorio | |
| (cualquier raw trial / PII) | **Prohibido** | La entrada del coach son **resúmenes**, nunca trials. |

---

## 3.4 Estrategia de datos sensibles

Clasificación obligatoria de todo campo antes de que cruce la frontera de red:

| Categoría | Contenido | Regla |
|---|---|---|
| **Identidad mínima** | `userId` (== `auth.uid`), `pseudoUserId` (hash device-bound, `ScoreResult.kt:43`) | Se sube. `userId` solo como path/owner-key. |
| **Datos de entrenamiento necesarios** | `sessions/{id}` resumen: moduleId, exerciseFocus, started/endedAtMs, durationMs, totalTrials (`Results.kt:53-64`) | Se sube (resumen). |
| **Métricas derivadas** | `blockScores` (snapshot actual: score/status/confidence/validSessionCount), `cognitiveProfile` (dimensions, compositeIndex, `MindBoostProfile.kt`), `sessionMetricsSummary` allowlisted (§3.3) | Se sube. `null` = ausente (nunca 0). |
| **Local-only** | `SubjectiveState` (sueño, cafeína, ánimo, hora, `Results.kt:69-80`); `syncState.pending/failed`; toda clave del Keystore | Nunca sube en MVP. |
| **Apto para IA Coach** | `aiCoach/summary` derivado (bandas, dimensión dominante), `cognitiveProfile` agregado | Entrada del coach = solo estos resúmenes. |
| **Prohibidos (nunca en Firestore, nunca en el modelo de sync)** | raw trials (`BlockResult.trials`, `Results.kt:42`), timings por-trial, DNI, datos médicos, diagnósticos, profesión obligatoria, fecha exacta de nacimiento, teléfono, dirección, cualquier claim clínico | Rechazo estructural. |

**Frontera de minimización.** El contrato ya existe en el dominio: `ModuleContribution` "NEVER carries raw trials, timings, or anything uploadable" (`ModuleContribution.kt:5-6`) y `SessionEvidence` "carries NO session ids, no timestamps and nothing uploadable" (`SessionEvidence.kt:4-6`). La sync **hereda** esa minimización: solo mapea DTOs ya minimizados (`MindBoostProfileDto`, summaries de sesión).

---

## 3.5 Reglas Firestore (PROPUESTA — NO se aplican este turno)

> **HARD CONSTRAINT:** este bloque es una **propuesta de diseño**. NO se edita `firestore.rules` este turno. Sintaxis Firestore Rules v2.

**Hallazgo de auditoría a resolver al implementar (verificado).** En `firestore.rules` el helper `isOwner(userId)` (`firestore.rules:14-16`) **NO** llama a `isAppChecked()` — de hecho **no existe** `isAppChecked()` ni `request.app` en todo `firestore.rules` (0 ocurrencias). En cambio, `storage.rules` **sí** define `isAppChecked() { return request.app != null; }` (`storage.rules:10-11`) y su `isOwner` lo compone (`storage.rules:19`). Por tanto, para MindBoost hay que **añadir explícitamente** un helper `isAppChecked()` a `firestore.rules` y exigirlo en cada regla nueva: App Check es ortogonal a la autorización (invariante del repo, learning 2026-05-26). No confiar en que `isOwner()` ya lo haga.

```javascript
// PROPUESTA (no aplicar este turno). Añadir junto a los helpers de firestore.rules:6-16
function isAppChecked() {
  return request.app != null;               // espejo de storage.rules:10-11
}

// MindBoost: subcolecciones cognitivas — SOLO el dueño, SOLO con App Check.
// Escrituras validadas por campos; sin campos privilegiados; listas SIEMPRE acotadas.

match /users/{userId} {

  // 1) Perfil cognitivo (snapshot derivado). Un único doc 'main'.
  match /cognitiveProfile/{docId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if false;                    // documento único; no se lista
    allow create, update: if isOwner(userId) && isAppChecked()
      && request.resource.data.keys().hasOnly(
           ['schemaVersion','userId','dimensions','overall','updatedAtMs'])
      && request.resource.data.userId == userId          // no falsear el dueño
      && request.resource.data.schemaVersion is int;
    allow delete: if isOwner(userId) && isAppChecked();
  }

  // 2) Resúmenes de sesión de entrenamiento (nunca raw trials).
  match /sessions/{sessionId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if isOwner(userId) && isAppChecked()
      && request.query.limit != null && request.query.limit <= 100;   // INVARIANTE list-con-limit
    allow create: if isOwner(userId) && isAppChecked()
      && request.resource.data.keys().hasOnly(
           ['schemaVersion','userId','moduleId','exerciseFocus',
            'startedAtMs','endedAtMs','durationMs','totalTrials',
            'sessionMetricsSummary','updatedAtMs'])
      && request.resource.data.userId == userId
      && request.resource.data.durationMs is int
      && !('trials' in request.resource.data)             // rechazo estructural de raw trials
      && !('subjectiveState' in request.resource.data);   // covariate sensible: local-only
    allow update: if false;                  // sesión inmutable (idempotencia por sessionId)
    allow delete: if isOwner(userId) && isAppChecked();
  }

  // 3) Snapshot actual por bloque/módulo (MUTABLE, last-write-wins; NO es una lista de eventos).
  match /blockScores/{blockId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if isOwner(userId) && isAppChecked()
      && request.query.limit != null && request.query.limit <= 100;
    // Snapshot mutable: create y update permitidos, owner-scoped y validados (rechaza raw trials).
    allow create, update: if isOwner(userId) && isAppChecked()
      && request.resource.data.userId == userId
      && !('trials' in request.resource.data);
    allow delete: if isOwner(userId) && isAppChecked();
  }

  // 4) IA Coach (solo lectura del cliente; escritura derivada por backend/callable).
  match /aiCoach/{docId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if false;
    allow write: if false;                   // NUNCA cliente — evita "acceso IA a datos incorrectos"
  }

  // 5) Plan vigente.
  match /plans/{planId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if false;
    allow create, update: if isOwner(userId) && isAppChecked()
      && request.resource.data.userId == userId;
    allow delete: if isOwner(userId) && isAppChecked();
  }

  // 6) Cursor de sync.
  match /syncState/{docId} {
    allow get: if isOwner(userId) && isAppChecked();
    allow list: if false;
    allow create, update: if isOwner(userId) && isAppChecked()
      && request.resource.data.userId == userId
      && !('pendingCount' in request.resource.data)   // la cola es local-only (autoridad local)
      && !('failedCount' in request.resource.data);
    allow delete: if isOwner(userId) && isAppChecked();
  }
}

// 7) ENTITLEMENT / SUBSCRIPTION (Elite) — escritura SOLO backend, jamás cliente.
//    Ya existe el patrón exacto en firestore.rules:1812-1818 (write: if false).
match /user_entitlements/{userId}/items/{entitlementId} {
  allow get: if isOwner(userId) && isAppChecked();
  allow list: if isOwner(userId) && isAppChecked()
    && request.query.limit != null && request.query.limit <= 100;
  allow write: if false;                    // subscription/entitlement solo por callable/Admin SDK
}
```

**Lo que estas reglas impiden (mapa a los requisitos del CEO):**

- **Lectura/escritura de otros usuarios** → `isOwner(userId)` (`uid() == userId`, `firestore.rules:14-16`) en cada `match`.
- **Escritura de campos privilegiados** → `keys().hasOnly([...])` + `userId == userId`.
- **Queries sin límite** → `request.query.limit != null && <= N` en toda `list` (invariante del repo, learning 2026-05-26; patrón en `firestore.rules:1814-1816`).
- **Manipulación de datos críticos** → `sessions` es create-only (`update: if false`, idempotencia por id); `blockScores` es un **snapshot mutable** owner-scoped y validado por reglas básicas (rechaza `trials`), NO inmutable ni lista de eventos. **P2:** un snapshot escrito client-side puede manipularse localmente; mitigación futura = validación server-side / recomputación desde `sessions`.
- **Elevación falsa de suscripción** → `user_entitlements` `write: if false` (`firestore.rules:1817`); Elite se escribe **solo** por callable/Admin SDK. El Admin SDK bypassa Rules tras validar App Check + auth (patrón análogo documentado en el bloque `posts`, `firestore.rules:1673`).
- **Acceso IA a datos incorrectos** → `aiCoach` `write: if false` (solo backend deriva el summary); cliente solo lee su propio doc.

> **UNVERIFIED:** que estas reglas pasen el emulador (`test:rules`, JDK 21+) — no se ejecutan este turno. Al implementarlas hay que añadir tests de regresión `list-sin-limit→FAIL` / `list-con-limit→OK` / `cross-user→FAIL` / `write en aiCoach→FAIL` / `write en entitlement→FAIL` / `trials-en-payload→FAIL` (learning 2026-06-30 sobre reglas `list` y cliente; un test positivo de `list` debe replicar el `where` que la regla exige).

---

## 3.6 Sync engine (diseño)

**Modelo de estado — `SyncStatus`** (enum nuevo propuesto; **no existe hoy** en el repo):

```
PENDING   → guardado local OK; encolado para subir.
SYNCED    → réplica remota confirmada: escritura nueva (Synced) o duplicado VERIFICADO (AlreadySyncedDuplicate). Nunca se alcanza por PERMISSION_DENIED.
FAILED    → intento fallido reintentable con backoff.
CONFLICT  → divergencia detectada entre local y remoto (requiere resolución).
```

**Principios:**

1. **El entrenamiento NUNCA se bloquea.** Igual que el patrón fire-and-forget ya usado en el repo para persistir (learning 2026-07-03 sobre `mindBoostScope`): la sesión se **guarda local primero** (autoritativo, síncrono) y la subida se lanza en un scope de app que sobrevive a la navegación. Un fallo de nube deja el registro `PENDING`, no error de UX.
2. **Cola local como autoridad.** Cada `TrainingSession`/`BlockScore` guardado marca una entrada `PENDING` en un `SyncQueue` local (persistido en un namespace cifrado nuevo, p. ej. `mindboost_sync`, siguiendo el patrón `SecureKeyValueStore`). La nube nunca es la cola; solo el destino.
3. **Idempotencia por `sessionId`.** Cada sesión usa un document id estable (en Enfoque `"<userId>:<endedAtMs>"`, `LocalFocusRepositories.kt:58`; ver caveat §3.3 para los demás motores). La escritura se diseña como **create-only / check-then-create** para evitar sobrescrituras. Si el documento remoto **ya existe**, el sync engine lo **lee/prueba** y compara **ownership + fingerprint**: SOLO si coinciden se clasifica como **`AlreadySyncedDuplicate`** (éxito lógico → cola `SYNCED`). Si no existe, si no puede verificarse, o si diverge, se clasifica como **`Conflict`** (o el fallo correspondiente), **nunca** como éxito silencioso. Un `create` nuevo sobre id inexistente es idempotente por definición.

   **`PERMISSION_DENIED` JAMÁS se interpreta como éxito ni como duplicado sincronizado.** Significa fallo de permisos, reglas, App Check, ownership o intento de operación no permitida → queda como **`PermissionDenied`**, salvo que una verificación remota independiente y positiva produzca `AlreadySyncedDuplicate` por otra rama del flujo. Con `create()`, un id ya existente devuelve **`ALREADY_EXISTS`** (que el clasificador resuelve por verificación), NO `PERMISSION_DENIED`. Ver `SyncWriteClassifier` (`SyncWriteResult.kt`).
4. **Reintentos con backoff exponencial** sobre `FAILED`, acotados por los timeouts existentes: `withFirestoreWriteTimeout` (20s) / `withFirestoreReadTimeout` (15s) / `BatchTimeout` (30s) de `FirestoreDefaults.kt`. Distinguir `TimeoutCancellationException` (reintentable) de `CancellationException` genuina (rethrow) — patrón obligatorio del repo (learning 2026-05-29).

**Resolución de conflictos — decisión:**

- **`cognitiveProfile/main` y `blockScores` → last-write-wins por `updatedAtMs`.** Justificación: ambos son **snapshots derivados** re-computables desde el historial local; el más reciente por reloj inyectado gana (`updatedAtMs`, `MindBoostProfile.kt:75`). No se hace merge por campo porque el objeto es un agregado atómico producido por `MindBoostProfileAggregator` — mezclar dimensiones de dos snapshots distintos rompería la coherencia del `compositeIndex`.
- **`sessions` → sin conflicto posible (append-only + inmutable).** Cada sesión tiene id único e `update: if false`; dos dispositivos generan ids distintos (distinto `endedAtMs`), así que se **unionan**, no colisionan (sujeto al caveat de clave única §3.3 para los motores no-Enfoque).
- **`syncState` → local-first, remoto es cache.** El cursor local manda.

> **UNVERIFIED / riesgo:** last-write-wins puede **perder** progreso si dos dispositivos recomputan el `cognitiveProfile` a la vez. Mitigación de diseño: el profile es derivado → tras subir todas las `sessions`, un dispositivo puede **re-agregar** desde el historial unido. La estrategia de rehidratación del perfil tras reinstalación (¿re-agregar en cliente desde `sessions` descargadas, o descargar `cognitiveProfile/main`?) queda **pendiente de decisión**; no se resuelve este turno.

**Escenarios cubiertos por diseño:**

| Escenario | Comportamiento |
|---|---|
| **Offline** | Guardado local OK; entrada `PENDING`; sin bloqueo; se sube al reconectar. |
| **Logout** | La cola local permanece; al re-login del mismo uid se drena. (Single-account por dispositivo.) |
| **Reinstalación** | Local vacío; se descargan `sessions` / `cognitiveProfile` del uid. **UNVERIFIED**: estrategia de rehidratación (arriba). |
| **Usuario en dos dispositivos** | `sessions` se unionan por id; `cognitiveProfile` LWW por `updatedAtMs`. |
| **Sesión duplicada** | El sync engine **verifica el documento remoto** por `sessionId`: si existe, pertenece al mismo usuario y el fingerprint coincide → `AlreadySyncedDuplicate` (cola `SYNCED`). Si no puede verificarse o diverge → `Conflict`. **`PERMISSION_DENIED` nunca se mapea a `SYNCED`.** (§3.6.3) |
| **Fallo parcial** | Cada entrada de la cola tiene su propio estado; las que subieron quedan `SYNCED`, las demás `PENDING`/`FAILED` y reintentan. El perfil NO se marca sincronizado hasta que suban sus dependencias. |

---

## 3.7 Repositorios: extensión SIN reescritura

**Tesis: no hace falta reescribir nada.** Los 4 contratos de Enfoque ya son interfaces puras del dominio (`FocusRepositories.kt`): `FocusProfileRepository`, `SessionHistoryRepository`, `BaselineRepository`, `ProbeBankRepository`. La implementación local (`LocalFocusProfileRepository`, `LocalSessionHistoryRepository`, … en `LocalFocusRepositories.kt`) queda **intacta**. Se añade una implementación **remota** detrás de la MISMA interfaz y un **coordinador** local+remote:

```kotlin
// PROPUESTA. Espejo del patrón Firestore-backed ya usado en el repo (FirebaseProfileCardRepository).
class FirebaseSessionHistoryRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : SessionHistoryRepository {
    override suspend fun append(result: SessionResult) {
        val uid = requireOwner(auth)                                   // uid == owner
        withFirestoreWriteTimeout {                                    // FirestoreDefaults
            firestore.collection("users").document(uid)
                .collection("sessions").document(result.sessionId.value)
                .set(result.toSummaryDto())                            // DTO minimizado, sin trials
        }
    }
    override suspend fun recent(userId: String, limit: Int) = /* query .limit(limit) ≤ 100 */
}

// Coordinador: local es autoritativo; remoto es fire-and-forget encolado.
class SyncingSessionHistoryRepository(
    private val local: SessionHistoryRepository,       // LocalSessionHistoryRepository (intacto)
    private val remote: SessionHistoryRepository,       // FirebaseSessionHistoryRepository (nuevo)
    private val queue: SyncQueue,
) : SessionHistoryRepository {
    override suspend fun append(result: SessionResult) {
        local.append(result)                            // 1) INMEDIATO, síncrono, autoritativo
        queue.enqueuePending(result.sessionId.value)    // 2) marca PENDING (no bloquea)
        // 3) el motor de sync drena la cola en un scope de app (fire-and-forget)
    }
    override suspend fun recent(userId: String, limit: Int) = local.recent(userId, limit)  // lectura local-first
}
```

**Wiring:** hoy los stores locales se instancian en el grafo de DI (`AppGraph`). El coordinador se cablearía ahí (o en el grafo del motor, p. ej. `FocusGraph`), sustituyendo la interfaz inyectada — sin tocar dominio ni UI. **No se cablea este turno.**

**Patrón para el resto de motores.** Cada motor ya tiene su propia interfaz de repo (Memoria, Lenguaje, Lógica, Decisiones/Reversibilidad, Emociones, Velocidad, y el brain-profile). El mismo tríptico se aplica: **interfaz existente → impl remota Firestore-backed → coordinador `Syncing*` que envuelve local+remote**. Ninguna interfaz de dominio cambia; solo se añaden clases nuevas en la capa `data`. **Precondición por motor:** verificar/normalizar antes su clave de sesión (caveat §3.3) para garantizar la idempotencia cross-device.

---

## 3.8 Borrado de cuenta (integración GDPR Art. 17)

**Backend — `executeAccountDeletion` (`functions/src/accountDeletion.ts:325-475`).** El paso 2 borra docs single-owner (`profiles`, `profile_visuals`, `privacy`, `counters`, `profile_cards_private/public`, `:361-366`); el patrón para subcolecciones ya existe (`deleteByQuery(db().collection('user_posts').doc(uid).collection('posts'))`, `:407-409`). Se añade un **nuevo paso, insertado explícitamente ANTES del borrado del doc raíz `users/{uid}` (`:435`) y del Auth user (`:440`)**, bloqueante y reintentable (mismo contrato que el paso de Storage, `:416-428`):

```typescript
// PROPUESTA (no editar este turno). Nuevo paso, espejo de accountDeletion.ts:407-409.
// DEBE ejecutarse ANTES de borrar el doc raíz users/{uid} (:435): Firestore no cascadea subcolecciones.
const mbSessionsDeleted    = await deleteByQuery(db().collection('users').doc(uid).collection('sessions'));
const mbBlockScoresDeleted = await deleteByQuery(db().collection('users').doc(uid).collection('blockScores'));
const mbCogProfileDeleted  = await deleteByQuery(db().collection('users').doc(uid).collection('cognitiveProfile'));
const mbSyncStateDeleted   = await deleteByQuery(db().collection('users').doc(uid).collection('syncState'));
const mbAiCoachDeleted     = await deleteByQuery(db().collection('users').doc(uid).collection('aiCoach'));
const mbPlansDeleted       = await deleteByQuery(db().collection('users').doc(uid).collection('plans'));
await markStep(jobRef, 'mindboost_data_deleted', 'completed',
  { mbSessionsDeleted, mbBlockScoresDeleted, mbCogProfileDeleted, mbSyncStateDeleted, mbAiCoachDeleted, mbPlansDeleted });
```

Documentos/subcolecciones eliminados (enumeración exacta):
- `users/{uid}/sessions/*`
- `users/{uid}/blockScores/*`
- `users/{uid}/cognitiveProfile/*` (incl. `main`)
- `users/{uid}/syncState/*`
- `users/{uid}/aiCoach/*`
- `users/{uid}/plans/*`

No hay prefijos de Storage nuevos (el MVP no sube blobs cognitivos).

**Cliente — eraser local.** `SecureNamespaceUserDataEraser` (`AccountDeletionCoordinator.kt`) ya limpia los namespaces `AccountBound` (`MindBoostStorageNamespaces.kt`) vía `clearNamespace()`. **Si se crea el namespace `mindboost_sync`** (cola local), aplica el **learning 2026-06-28**:

> Añadir un namespace a `AccountBound` obliga a, en el mismo cambio: (1) añadirlo a `MindBoostStorageNamespaces.AccountBound`, (2) añadir su store a la lista del `SecureNamespaceUserDataEraser` en `AppGraph` (keys del `remember` + `listOf`), y (3) actualizar el pin de `AccountBoundErasureCoverageTest`. Omitir cualquiera de los tres deja datos sensibles sin borrar y el test de cobertura falla (fail-closed).

---

## 3.9 Tests obligatorios (diseño, NO implementación)

| # | Test | Fixture | Assert |
|---|---|---|---|
| 1 | **Guardado local inmediato** | `SyncingSessionHistoryRepository` con `remote` que nunca resuelve | `local.recent()` devuelve la sesión **sin esperar** a la nube. |
| 2 | **Sync pending** | Fake `SyncQueue`; `append(result)` | Entrada queda `PENDING` tras el guardado local. |
| 3 | **Sync successful** | Fake remote que confirma | Entrada pasa `PENDING→SYNCED`; `syncState.lastSyncedSessionId == sessionId`. |
| 4 | **Sync failed** | Fake remote lanza `IOException` | Entrada pasa a `FAILED`; el entrenamiento no falla. |
| 5 | **Retry con backoff** | Remote falla 2 veces, éxito la 3ª | Tras backoff, `FAILED→SYNCED`; nº de intentos == 3. |
| 6 | **Idempotencia** | `append` del mismo `sessionId` dos veces; el doc remoto ya existe (create-only) y su fingerprint coincide | Un solo doc remoto; el 2º intento → `AlreadySyncedDuplicate` (cola `SYNCED`), **NO** vía `PERMISSION_DENIED`, sin `FAILED` ni churn. |
| 7 | **No duplicados** | Dos dispositivos, `endedAtMs` distintos (**Enfoque; UNVERIFIED otros motores**) | Dos docs `sessions` distintos; misma sesión nunca duplicada. |
| 8 | **No acceso cruzado** | Emulador: uid A intenta leer `users/B/sessions` | `assertFails` (deniega por `isOwner`). |
| 9 | **Reglas Firestore (emulador)** | `test:rules` con JDK 21+ | `list-sin-limit→FAIL`, `list-con-limit→OK`, `cross-user→FAIL`, `write en aiCoach→FAIL`, `write en entitlement→FAIL`, `trials en payload→FAIL`. |
| 10 | **Borrado de cuenta** | `executeAccountDeletion(uid)` con `sessions`/`blockScores`/`cognitiveProfile` seed | Todas las subcolecciones `users/{uid}/*` de MindBoost quedan vacías; `markStep('mindboost_data_deleted','completed')`; se ejecutó antes del borrado del doc raíz. |
| 11 | **Sesión offline** | Remote no disponible | `local.append` OK; `PENDING`; sin excepción propagada a UI. |
| 12 | **Reconexión** | Cola con 3 `PENDING`, remote vuelve | Los 3 drenan a `SYNCED` en orden; un id ya existente **verificado** (mismo user + fingerprint) → `AlreadySyncedDuplicate` (`SYNCED`), sin churn — nunca vía `PERMISSION_DENIED`. |
| 13 | **Conflictos (LWW)** | Dos `cognitiveProfile` con `updatedAtMs` distinto | Gana el de `updatedAtMs` mayor; el otro se descarta sin corromper dimensiones. |
| 14 | **No bloqueo por fallo cloud** | Remote cuelga hasta `WriteTimeout` (`FirestoreDefaults`) | `append` retorna tras el guardado local (< timeout); el timeout de nube no propaga a la ruta de entrenamiento. |
| 15 | **Cobertura de erasure (si se crea `mindboost_sync`)** | — | `AccountBoundErasureCoverageTest` incluye el nuevo namespace (learning 2026-06-28). |

Todos los tests de dominio/cola viven en `commonTest`/`desktopTest` con fakes (patrón del repo); los de reglas en el emulador (`firestore.security.rules.test.mjs`). **UNVERIFIED:** ninguno existe hoy.

---

## 3.10 Semántica definitiva de `blockScores` vs `scoreEvents` (decisión Phase 0)

**Decisión CEO sellada:** `blockScores/{blockId}` es un **snapshot mutable del estado ACTUAL** del bloque,
NO una lista de eventos.

- **`users/{uid}/sessions/{sessionId}`** — evidencia de entrenamiento. Una sesión = un documento inmutable
  (append-only). Es la FUENTE de la señal.
- **`users/{uid}/blockScores/{blockId}`** — **estado derivado ACTUAL** del bloque: `score` actual, `status`
  (CALIBRATING/EMERGING/ESTABLISHED), `confidence`, `validSessions`, `lastSessionId`, `scoringVersion`,
  `updatedAtMs`. Documento **reemplazable** (last-write-wins por `updatedAtMs`). **No acumula historial.**
- **`users/{uid}/scoreEvents/{eventId}`** — historial **inmutable, opcional y futuro** de cambios de score
  (append-only). **Fuera de MVP.** Se documenta aquí para NO contaminar `blockScores` con semántica mixta el
  día que se necesite auditoría histórica detallada.

**Reglas duras:**
1. `blockScores/{blockId}` **nunca** es una lista de eventos.
2. `sessions/{sessionId}` guarda la **evidencia**; `blockScores/{blockId}` guarda el **estado derivado actual**.
3. La auditoría histórica detallada, si se necesita, vive en `scoreEvents/{eventId}` — jamás en `blockScores`.
4. Ambos preservan `null != 0`: un bloque en calibración tiene `score = null`, nunca `0`.

La sección `### BlockScore` (§3.3) describe los CAMPOS; esta sección fija la SEMÁNTICA (snapshot, no historial)
y separa el historial en `scoreEvents`. Ver también `MINDBOOST_SYNC_FOUNDATIONS_PHASE_0.md`.

## Cierre — compatibilidad futura

**IA Coach.** La entrada del coach son **exclusivamente** los resúmenes estructurados: `aiCoach/summary` (bandas descriptivas, dimensión dominante/desarrollo) y `cognitiveProfile/main` (dimensiones + `compositeIndex`, `MindBoostProfile.kt:60-65`). **Nunca** raw trials — el contrato de minimización (`ModuleContribution.kt:5-6`) y las reglas (`!('trials' in request.resource.data)`, §3.5) lo hacen estructuralmente imposible. El coach lee `aiCoach` (cliente read-only; escritura solo backend), evitando "acceso IA a datos incorrectos".

**Elite.** El gating de suscripción es **server-side**: `user_entitlements/{uid}/items` con `write: if false` (`firestore.rules:1812-1818`) — el cliente **nunca** puede auto-elevarse a Elite; solo una callable/Admin SDK (que bypassa Rules tras validar App Check + auth) escribe el entitlement. Este diseño deja lista esa superficie sin implementarla.

**Honestidad de cumplimiento final.** Este diseño cubre continuidad, recuperación multi-dispositivo y bases futuras. Quedan **pendientes de decisión (UNVERIFIED)**: (a) estrategia de rehidratación del perfil tras reinstalación (re-agregar en cliente vs. descargar snapshot); (b) semántica exacta de LWW vs. re-agregación cuando dos dispositivos divergen offline; (c) allowlist final de `MetricKey` cloud-safe más allá de `ACCURACY`/`RT_MEDIAN`/`RT_CV`; (d) verificación/normalización de la clave de sesión única en los 7 motores no-Enfoque; (e) si `SubjectiveState` llegará a subirse (hoy local-only por sensibilidad); (f) validación de las reglas en emulador. Ninguna decisión de este documento degrada el scoring, sube trials crudos, migra namespaces ni edita archivos de reglas.