# Molvex — Plan de remediación de escalabilidad & rendimiento (paquete de ejecución `/ceo`)

> Compilado por `/ceo` (skill 2.0.0). **Modo: MISIÓN CRÍTICA** — justificación en 1 frase: la remediación toca
> dinero (wallet), borrado GDPR, arquitectura de feed a 1M, pérdida de datos por process-death, seguridad
> (App Check) y migración de esquema (contadores) — el eje de máximo riesgo, difícil de revertir.
>
> **Fuente de riesgos:** [`SCALABILITY_PERFORMANCE_AUDIT_2026-07-11.md`](SCALABILITY_PERFORMANCE_AUDIT_2026-07-11.md)
> (auditoría read-only, hallazgos A1–A10, B1–B10, C1–C8, D1–D4, E1–E2, F1–F4). Este plan NO re-cita cada
> `archivo:línea`; el ancla vive en la auditoría. Aquí se compila la EJECUCIÓN.
>
> **⚠️ ENMENDADO (2026-07-12):** la auditoría adversarial PROMPT A (Fable 5 max, contexto fresco) emitió
> `APTO_CON_CAMBIOS` con un delta de 20 enmiendas. **§20 "Delta vinculante" SUPERSEDE §9/§10/§16 donde entren
> en conflicto.** Ningún slice se implementa sin aplicar sus enmiendas D-xx.

---

## 1. Interpretación de la instrucción

**Founder:** "construye el mejor plan para dejar resueltos de la mejor manera todos los riesgos encontrados, y
posteriormente ejecútalo en el orden recomendado, sin interrupciones salvo que necesites mi feedback."

**Compilación:** (a) programa de remediación completo, proporcional al riesgo, trazable finding→slice→gate→rollback;
(b) ejecución en orden recomendado, delegando a `/mb`/agentes o solo-inline según corresponda; (c) parar **solo**
en los puntos donde el feedback del founder cambia la dirección (§4 Decisiones).

---

## 2. Snapshot de entorno (runtime, VERIFICADO)

| Campo | Valor |
|---|---|
| `git HEAD` | `71e17038733e23a603c8ecb85ee5ff9f965a62db` (detached) |
| `git status` | 177 entradas — worktree SUCIO, mutando en paralelo (Vibe Kanban/n8n). **No se toca WIP ajeno.** |
| Gates backend | `functions/`: `npm run typecheck` · `npm test` (jest) · `npm run scan:secrets` — **ejecutables vía Bash** (no Gradle). |
| Gates cliente | `:shared:desktopTest` · `:shared:detekt` · `:shared:compileAndroidMain`/`:androidApp:compileDebugKotlin` — **hand-off al founder si Android Studio posee el daemon** (§9 doctrina). |
| Canon producto | `docs/context_pack/` (canon VIVO: 09 v2.0.2 con S6R3-001..010; 03 con citas corregidas). ⚠️ La copia `docs/context-pack/` (2.0.0) está OBSOLETA (diverge 59 líneas en 09) — tarea pendiente con owner para retirarla/redirigirla (F-06). No hay carpeta `DOCS MB/`. |
| Grafo Graphify | OBSOLETO (`35016318` ≠ HEAD) — evidencia por lectura directa. |

### 2-bis. ⚠️ Estado de la infraestructura de ejecución (HONESTIDAD DE ESTADO — bloqueante hoy)

- **Límite de sesión de cuenta ACTIVO.** El workflow multi-agente lanzado antes en esta sesión **falló con
  "session limit · resets 4:20am (Europe/Madrid)"** en sus 4 agentes. ⇒ **la vía de ejecución delegada de `/ceo`
  (spawnear `/mb`/agentes/reviews independientes) está CAÍDA hasta el reset.** Estado: `NO DISPONIBLE`.
- **Consecuencia para Misión Crítica:** las revisiones independientes que este modo EXIGE (S1/S2/S3 tocan
  dinero/GDPR/pérdida de datos) **no pueden correr ahora**. Ejecutar código Misión-Crítica sin esa revisión y sin
  gates reproducibles violaría la honestidad y la preservación. → §16 secuencia lo refleja.
- **Gates cliente:** los de Kotlin NO se pueden correr por CLI si el IDE posee el daemon → se entregan al founder.
  Los de backend (`functions` jest) SÍ los puedo correr yo.

---

## 3. Contexto y provenance (etiquetado)

- Hallazgos **CONFIRMED** (verificables en código): A1–A9, B1–B10, C1–C8, D1–D4, E1–E2, F1–F4. Re-verificados
  esta sesión contra fuente: A1 (`socialGraph.ts:96`), A2 (sin writer de `feeds/`), B1/B3 (`MindBoostHubScreen.kt:198`
  + `SessionRecoveryScanner.kt:31`), cobertura de timeouts (48 usos, `.await()`=0), snapshot-first del perfil.
- Hallazgos **HYPOTHESIS** (necesitan medición en dispositivo): A10 (enforcement App Check en consola), C1/C2/C3/C6
  (TTID, jank, coste de descifrado), A4 (timeout de GC a escala). Sus comandos exactos: auditoría §5.
- **Invariantes heredados no-demolibles** (charter): `null ≠ 0`; el perfil solo se actualiza vía `ScoreResult`
  sellado con sus gates; anti-gaming con red-team; sin claims clínicos/IQ; datos cognitivos `@Serializable` +
  `SecureKeyValueStore` (no Room/SQLDelight); todo namespace nuevo → `AccountBound` + eraser + `accountDeletion.ts`
  en el MISMO cambio. **Ningún slide de caché/rendimiento puede violar estos.**

---

## 4. Decisiones que necesitan tu feedback (bloquean parte del P0)

> El resto de decisiones técnicas se resuelven con recomendación (no se elevan). Estas SÍ se elevan porque cambian
> arquitectura o implican **gasto** (genuine external decision; BUS-010 = 0 € por defecto).

| ID | Decisión | Opciones | Recomendación `/ceo` |
|---|---|---|---|
| **DEC-1** | **Modelo de feed (A2)** — el feed materializado `feeds/{viewer}/posts` no tiene writer. | (a) **fan-out-on-read** + caché de servidor acotado; (b) fan-out-on-write con worker acotado (`onPostCreated`); (c) diferir el feed a su propio track. | **(a)** — coherente con local-first, sin write-storm de celebridad, coste dominado por lecturas cacheables. (b) amplifica escritura en cuentas con muchos seguidores. |
| **DEC-2** | **Autorización de gasto (A5/A6/A3-bg)** — `minInstances`, Cloud Tasks, ¿Memorystore? | (a) **gasto mínimo:** `minInstances=1` solo en callables económicas + Cloud Tasks (dentro de free tier) para borrado de fondo; **cero Memorystore** (usar contador Firestore shardeado, A1); (b) cero gasto (sin `minInstances`, aceptar cold starts); (c) discutir. | **(a)** — el techo de coste es `minInstances`; limitarlo a spend/purchase/verify. Memorystore NO es necesario si A1 va shardeado. |
| **DEC-3** | **Alcance F1/F2** — sync durable del perfil cognitivo + analytics sink. | (a) **fuera de este programa:** F1 = track Profile V2 §10-sync (ADR-DATA, gasto, gated); F2 = track de analítica propio; (b) incluir F1/F2 aquí. | **(a)** — F1 es una decisión de datos con gasto y esquema cloud (Profile V2); mezclarla aquí rompe la proporcionalidad. Este plan cierra el lado LOCAL (write-ahead B1), no el sync cloud. |
| **DEC-4** | **Arranque de ejecución HOY** dado el límite de sesión. | (a) **empezar ya el subconjunto seguro** (S5 cache, S6 retry, S7 atmosphere, S12 hygiene) solo-inline con gates hand-off; (b) esperar a tus decisiones DEC-1/2 + reset 4:20am (agentes de review vuelven) y correr el orden completo; (c) híbrido. | **(c)** — arrancar S5/S6/S7 (additivos, sin gasto, sin decisión, sin dinero/GDPR) mientras resuelves DEC-1/2; reservar S1/S2/S3/S4 (Misión-Crítica) para cuando haya review independiente (post-reset). |

---

## 5. Alcance y criterios de aceptación

**In-scope:** resolver A1–A10, B1–B10, C1–C8, D1–D4, E1–E2 mediante slices con gate y rollback. **Non-goals:**
F1 (sync durable — track Profile V2), F2 (analytics sink — track propio), rebrand de código a Molvex, migración
física a VPS, ranking público, features nuevas de ejercicio, tocar `features/profile/card`/social salvo lo citado.

**Criterio de aceptación global:** cada slice entra con su gate en verde (o hand-off documentado), sin regresión de
los invariantes charter, sin dato fabricado, y con su entrada en "Aprendizajes acumulados" de `CLAUDE.md` si corrige
un error real. La UI sigue funcionando en cada fase; ningún slice deja una pantalla rota ni un score fabricado.

---

## 6. Requisitos + trazabilidad (finding → slice)

| Slice | Findings que cierra | Superficie | Disciplina |
|---|---|---|---|
| **S1** | A1 | `functions/` + `firestore.rules` | backend |
| **S2** | A3 | `functions/` (+ Cloud Tasks) | backend |
| **S3** | B1, B3, B6, B7, F3 | `:shared` (mindboost core/session, holders) | cliente KMP |
| **S4** | A2 | `functions/` + cliente feed | backend + cliente |
| **S5** | D1, D3, C8 | `:shared` (`SecureKeyValueStore`) | cliente KMP |
| **S6** | B2 | `:shared` (`FirestoreDefaults`) | cliente KMP |
| **S7** | C5 | `:shared` (Focus UI) | cliente KMP |
| **S8** | A4, A5 | `functions/` | backend |
| **S9** | A6 | `functions/` | backend |
| **S10** | B4, B5, B8 | `:shared` (outbox de intents) | cliente KMP |
| **S11** | E1, E2 | `:shared` (core + UI) | cliente KMP |
| **S12** | A7, A8, A9 | `functions/` + `firestore.rules` | backend |
| **S13** | C1, C2, C3, C6, C7 | `:shared` + `:androidApp` | cliente KMP |
| **S14** | A10 | `firestore.rules` (+ verificación consola) | backend + founder |
| **S15** | D2, D4, B9, B10 | `:shared` | cliente KMP |

---

## 7. DAG de ejecución (dependencias)

```text
DEC-1 ──► S4 (feed)
DEC-2 ──► S2 (borrado bg), S9 (minInstances), S8 (maxInstances voz)
DEC-3 ──► (F1/F2 fuera)

P0:  S1 ──┐   S2 ──┐   S3 (indep.)      S4 (tras DEC-1)
          └─ independientes entre sí (S1 backend, S3 cliente) ─ pueden ir en paralelo de superficie distinta
P1:  S5 (indep.) · S6 (indep.) · S7 (indep.) · S8/S9 (tras DEC-2) · S10 (tras S6: reusa retry)
P2:  S11 · S12 · S13 · S14 · S15 (S15.D2 tras S5: reusa versión de namespace)

Reglas del DAG:
- Ningún slice borra la capa de invariantes antes de que su reemplazo esté verde.
- Gates de MISMO worktree SIEMPRE en serie (nunca 2 Gradle en paralelo sobre shared/build).
- Backend y cliente son superficies disjuntas → se pueden intercalar sin colisión de build.
```

---

## 8. Routing (agente/modelo por slice)

> ⚠️ Subagentes `NO DISPONIBLE` hasta el reset (§2-bis). Routing declarado para cuando vuelvan; hasta entonces,
> los slices marcados "solo-inline OK" los ejecuto yo en el hilo principal.

| Slice | Ejecutor | Review independiente (post-reset) | ¿solo-inline OK hoy? |
|---|---|---|---|
| S1 (contador) | `mindboost-backend` / inline | `mindverse-security-privacy` + `diff-auditor` (dinero adyacente) | ❌ (Misión Crítica: exige review) |
| S2 (borrado) | `mindboost-backend` / inline | `mindboost-security-privacy` + `diff-auditor` (GDPR) | ❌ |
| S3 (outbox payload) | `cognitive-engine-dev` | `silent-failure-hunter` + `diff-auditor` (pérdida de datos) | ❌ |
| S4 (feed) | `mindverse-backend-platform` | `architect` + `diff-auditor` | ❌ (tras DEC-1) |
| S5 (cache) | `kmp-architect` / inline | `kotlin-reviewer` | ✅ additivo, reversible |
| S6 (retry) | inline | `kotlin-reviewer` | ✅ |
| S7 (atmosphere) | inline | `psychometrician` (honestidad de medida) | ✅ |
| S8/S9 (config) | `mindverse-devops-sre` / inline | — | ✅ tras DEC-2 |
| S12 (hygiene) | `mindboost-backend` / inline | `security-reviewer` | ✅ |
| resto | inline | `code-reviewer` | ✅ |

---

## 9. Mapa de slices (spec ejecutable)

Formato por slice: **objetivo · archivos · approach · gate · rollback · riesgo**. Los prompts autónomos completos
de los 3 más críticos (S1, S3, S5) están en §10.

### P0 — rompe a escala / pierde datos

**S1 — Contador de seguidores distribuido (A1·CRITICAL)**
- **Objetivo:** eliminar la contención de escritura sobre `counters/{uid}` en follow/request/block.
- **Archivos:** `functions/src/socialGraph.ts` (todas las mutaciones de `counters/*`), `firestore.rules` (subcolección
  `counters/{uid}/shards/{n}`), migración de backfill (script one-off o lazy-init), tests `functions/src/*.test.ts`.
- **Approach:** shards `counters/{uid}/shards/{0..N-1}`; escritura → `FieldValue.increment(±1)` en shard aleatorio
  (NO read-then-write); lectura → suma de shards (o mantener un `counters/{uid}.total` eventualmente-consistente vía
  agregación). N=10 inicial. Backfill: al primer incremento, sembrar shards desde el valor legacy `counters/{uid}`.
- **Gate:** `npm --prefix functions test` (concurrencia: 100 follows concurrentes → conteo exacto, cero `ABORTED`) +
  `npm --prefix functions run typecheck` + test de reglas del emulador para la subcolección shards.
- **Rollback:** revert del slice; el doc legacy `counters/{uid}` se conserva durante la ventana dual.
- **Riesgo:** cambio de esquema en un read-path vivo → **dual-read** (leer shards + fallback legacy) hasta backfill completo.

**S2 — Borrado de cuenta paginado + job de fondo (A3·HIGH, GDPR)**
- **Objetivo:** que `deleteAccount` no cargue el set completo en una invocación (OOM/timeout → erasure atascada).
- **Archivos:** `functions/src/accountDeletion.ts` (`deleteByQuery` `:128`, `:208`, `:231`, `:374`), Cloud Tasks
  handler (`onDispatch`), tests.
- **Approach:** cada query de borrado con `.limit(BATCH)` + cursor `startAfter`; el envelope encola un job Cloud Tasks
  con checkpoints por lote (idempotente, reanudable); `timeoutSeconds` elevado en el worker. El erasure sigue
  **fail-closed**: no borra Auth hasta demostrar cero dato residual.
- **Gate:** `npm --prefix functions test` (cuenta sintética grande → borrado completo sin timeout, reanuda desde
  checkpoint) + `scan:secrets`.
- **Rollback:** revert; el job es idempotente, un revert a medias no corrompe (re-escanea).
- **Riesgo:** GDPR — un borrado que reporta completado sin estarlo. Mitiga el fail-closed + test de residuo cero.
- **Dep:** DEC-2 (Cloud Tasks).

**S3 — Write-ahead de payload + replay (B1/B3/B6/B7/F3·HIGH, pérdida de datos)**
- **Objetivo:** que una muerte de proceso durante el guardado de una sesión COMPLETADA no la pierda en silencio.
- **Archivos:** `MindBoostHubScreen.kt:182,192,198` (seals fire-and-forget), `core/session/**` (outbox de payload,
  espejo del write-ahead de lifecycle ya existente), `SessionRecoveryScanner.kt` (que el recovery RE-EJECUTE el
  guardado pendiente, no solo marque ABANDONED), holders E8/Decisiones/reversibilidad.
- **Approach:** journal append-only del payload minimizado (`@Serializable` + `SecureKeyValueStore`) ANTES de navegar;
  el recovery al arranque re-ejecuta los pendientes; borrar del journal solo tras confirmación de escritura. NO
  cambia scoring ni gates (charter intacto).
- **Gate:** `:shared:desktopTest` (matar-antes-de-suspensión no pierde el resumen; replay idempotente) + `:shared:detekt`
  + `:shared:compileAndroidMain`. **Hand-off si el IDE posee el daemon.**
- **Rollback:** revert; el journal extra es inerte si nadie lo lee.
- **Riesgo:** el journal persiste payload cognitivo → cifrado + namespace `AccountBound` + eraser + `accountDeletion.ts`
  en el MISMO cambio (invariante). Solo resúmenes, nunca trials crudos.

**S4 — Modelo de feed (A2·HIGH)** — **tras DEC-1.** Si (a): fan-out-on-read con caché de servidor acotado +
`.limit()` espejo; si (b): worker `onPostCreated` acotado. Gate backend + decisión de arquitectura documentada en ADR.

### P1 — degrada a escala / instant-feel

**S5 — Caché en `SecureKeyValueStore` (D1/D3/C8·HIGH instant-feel)** — additivo, reversible, honestidad-safe.
- **Archivos:** `focus/data/local/SecureKeyValueStore.android.kt` (+ `.kt` commonMain si el caché es un decorator).
- **Approach:** (1) cachear la `SecretKey` resuelta por instancia (`by lazy`, soltar en `KeyPermanentlyInvalidated`);
  (2) caché en memoria `(key)→plaintext` invalidada en `put`/`remove`/`clearNamespace`/`quarantine`; (3) opcional:
  fichero `SharedPreferences` por namespace (C8) o cachear el índice de `keys()`. **Honestidad:** el caché guarda el
  MISMO blob que disco; los repos siguen corriendo `readVersioned`/gates encima → un blob en cuarentena sigue fallando
  cerrado; sign-out limpia el caché (subject-scoping).
- **Gate:** `:shared:desktopTest` (2ª lectura no re-descifra; `put` desaloja; sign-out limpia) + `detekt` + `compileAndroid`.
- **Rollback:** revert (caché es transparente).
- **Riesgo:** servir un resumen viejo tras sesión nueva → la invalidación-en-write es OBLIGATORIA (test la fija).

**S6 — Retry/backoff (B2·HIGH)** — wrapper exp-backoff+jitter (≤3) para lecturas idempotentes + callables
idempotency-keyed, en `core/network/FirestoreDefaults.kt`. Re-lanza `CancellationException`; solo mapea `TimeoutCancellationException`/`UNAVAILABLE`. Gate `:shared:desktopTest`.

**S7 — Congelar `FocusAtmosphere` en trials RT-críticos (C5·HIGH honestidad de medida)** — parar la animación durante
la ventana de medición (evita inflar `criticalFrameJitter`→`CONTAMINATED`). Gate `:shared:desktopTest` + `compileAndroid`.
Review: `psychometrician` (es medición, no solo fps).

**S8 — GC de media paginado + techo de voz (A4/A5)** — `pageToken` + shard por prefijo de uid; `runWith` memoria/timeout;
subir `maxInstances` de voz tras guard de coste (DEC-2). Gate backend.

**S9 — Config de callables (A6)** — región + `minInstances` en económicas + `maxInstances` guard (DEC-2). Gate backend.

**S10 — Outbox de intents offline (B4/B5/B8)** — cola local idempotency-keyed reproducida al reconectar
(wallet/posts/follows/visits) + GC de adjuntos huérfanos + lecturas cache-first con UI "offline". Reusa S6. Gate `:shared:desktopTest`.

### P2 — fluidez / coste / robustez

**S11 — `DeviceRenderTier` (E1/E2)** — enum LOW/MID/HIGH derivado de `isLowRamDevice`/`memoryClass`/SDK + feedback de
`FrameStatsCalculator`; `LocalDeviceRenderTier` (CompositionLocal); gatea blur (`AppLoadingOverlay`/`PhotoUploadLoader`/
`ElementSelectionScreen`) y `FocusAtmosphere`. **Invariante:** el tier degrada solo lo decorativo; NUNCA la exclusión de
señal RT-crítica del perfil (ya la impone `SessionQualityEvaluator`). Gate `:shared:desktopTest` + `compileAndroid`.

**S12 — Higiene de coste Firestore (A7/A8/A9)** — política TTL en `rate_limits` y `profile_visits`; dedup de webhooks por
firma+event-id (no throttle por IP); reducir ops/visita. Gate backend (jest + rules).

**S13 — Arranque (C1/C2/C3/C6/C7)** — desbloquear primer frame de la recovery destructiva; diferir `setDefaultsAsync`/
`fetchAndActivate` de RemoteConfig a un initializer de fondo; `MediaPreloadManager` lazy/off-main; decode de iconos
off-main con downsampling; guard de bootstrap Home por sesión. Gate `:androidApp:compileDebugKotlin` + medición (auditoría §5).

**S14 — App Check en lecturas privadas (A10)** — añadir `request.app != null` a reglas de lectura privada
(`wallets`/`notifications`/`dm_chats`/`counters`) como defensa en profundidad + **founder verifica enforcement en consola**
(auditoría §5, comando incluido). Gate rules emulator.

**S15 — Misc (D2/D4/B9/B10)** — dirty-version en brain-profile (saltar `refresh()` salvo cambio de versión de namespace,
recomputar nunca parchear — reusa S5); envolver `FirebaseProfileCardMediaRepository.kt:79` en `withStorage*Timeout`;
flush de autosave de profile-card; caché de `ProfileVisualConfig` por sesión. Gate por source-set.

---

## 10. Prompts completos (autónomos, copy-paste) — S1, S3, S5

> Los demás slices son spec-ejecutables (§9). Estos tres son los de mayor riesgo/precisión.

### PROMPT S1 — Contador de seguidores distribuido

```
Actúa como ingeniero backend de Molvex (Firebase Cloud Functions v1 TypeScript en functions/, Firestore).
OBJETIVO: eliminar la contención de escritura de A1 — hoy socialGraph.ts:96 hace read-then-tx.set(merge) sobre
counters/{targetId}.followersCount en cada follow/request/accept/block, un único doc que a >~1 escritura/s revienta
con ABORTED en cuentas virales.

LEE PRIMERO: functions/src/socialGraph.ts (todas las mutaciones de counters/*), functions/src/lib/guards.ts,
firestore.rules (bloque counters), un *.test.ts de wallet para el estilo de test jest.

IMPLEMENTA:
1) Contador shardeado: subcolección counters/{uid}/shards/{0..9}. Escritura = FieldValue.increment(±1) en un shard
   elegido al azar (NUNCA read-then-write). Aplica a followersCount, followingCount, y los *PendingRequestsCount.
2) Lectura: helper sumShards(uid, field) que suma los 10 shards. Mantén counters/{uid} legacy en DUAL-READ (suma
   shards; si no hay shards, cae al doc legacy) durante la migración.
3) Backfill lazy: al primer increment de un uid sin shards, sembrar shard 0 con el valor legacy y marcar migrado.
4) firestore.rules: reglas para counters/{uid}/shards/{shardId} — lectura owner/canRead, escritura solo backend
   (allow write: if false; las escriben las CF). Test de reglas del emulador.

INVARIANTES: no toques la lógica de autorización (requireAppCheck→requireAuth→rate-limit sigue igual). El conteo
debe ser exacto bajo concurrencia. Sin secretos hardcodeados.

GATE (córrelos tú, reporta salida real):
  npm --prefix functions run typecheck
  npm --prefix functions test        # añade test: 100 follows concurrentes a un mismo target → conteo == 100, 0 ABORTED
  npm --prefix functions run scan:secrets
Si algún test de reglas necesita el emulador cruzado, usa el script test:*-rules con su --project.

ENTREGA: qué cambió · archivos · tests añadidos · cómo verificar · rollback (revert; legacy conservado) · y la
entrada para "Aprendizajes acumulados" de CLAUDE.md. NO ofrezcas commit/PR.
```

### PROMPT S3 — Write-ahead de payload + replay

```
Actúa como cognitive-engine-dev de Molvex (KMP, :shared, commonMain; tests en desktopTest).
OBJETIVO: cerrar B1/B3/B6/B7 — el sellado de sesión de Decisiones/reversibilidad/E8 persiste vía
mindBoostScope.launch{ store.record(...) } (MindBoostHubScreen.kt:182,192,198), fire-and-forget: muerte de proceso
entre launch y fin pierde una sesión COMPLETADA. El write-ahead actual (SessionRecordStore) persiste solo lifecycle,
no el payload puntuado, y SessionRecoveryScanner solo marca ABANDONED (no re-ejecuta).

LEE PRIMERO: MindBoostHubScreen.kt (seals), core/session/SessionRecordStore.kt + SecureSessionRecordStore.kt +
SessionRecoveryScanner.kt, el patrón de outbox/journal existente (core/session/cognitive/** si aplica),
MindBoostStorageNamespaces + AppGraph eraser + functions/src/accountDeletion.ts.

IMPLEMENTA:
1) Journal append-only de payload minimizado (@Serializable + SecureKeyValueStore, schemaVersion): escribe el payload
   ANTES de navegar (no fire-and-forget); marca pendiente; borra del journal solo tras confirmar la escritura real.
2) Recovery: SessionRecoveryScanner (o un PayloadReplayScanner hermano) al arranque RE-EJECUTA los pendientes de
   forma idempotente, no solo marca ABANDONED.
3) Reusa el patrón para Decisiones, reversibilidad y E8 (GoNoGoSessionStateHolder ya usa NonCancellable; súbelo a
   journal+replay).
INVARIANTES CHARTER (no negociables): solo RESÚMENES, nunca trials crudos; null≠0; no cambia scoring ni gates. El
namespace del journal DEBE entrar en MindBoostStorageNamespaces.AccountBound + el eraser de AppGraph (+ su test de
cobertura) + functions/src/accountDeletion.ts EN ESTE MISMO CAMBIO (si es cloud-espejado; si es solo local, basta
AccountBound+eraser). Subject-scoped (aislamiento por usuario).

GATE (hand-off si el IDE posee el daemon Gradle; en serie, nunca 2 en paralelo sobre shared/build):
  :shared:desktopTest   # test: matar-antes-de-suspensión no pierde el resumen; replay idempotente; round-trip real
  :shared:detekt
  :shared:compileAndroidMain
ENTREGA: cambios · archivos · tests · verificación · rollback (journal inerte si nadie lo lee) · entrada CLAUDE.md.
```

### PROMPT S5 — Caché en SecureKeyValueStore

```
Actúa como kmp-architect de Molvex (KMP, :shared). OBJETIVO: D1/D3/C8 — cada read() de SecureKeyValueStore.android.kt
rehace KeyStore.getInstance().load(null)+getKey (:162-164) y un descifrado AES-GCM por llamada; sin caché de valor.
El brain-profile descifra 7 namespaces por apertura. Además un único SharedPreferences (neu_secure_focus) respalda
todos los namespaces → keys()/clearNamespace() O(total claves).

LEE PRIMERO: focus/data/local/SecureKeyValueStore.android.kt + .kt (commonMain), RememberBrainProfileStateHolder.kt,
un test existente de SecureKeyValueStore.

IMPLEMENTA (additivo, transparente):
1) Cachear la SecretKey resuelta por instancia de store (by lazy); soltarla y regenerar SOLO en
   KeyPermanentlyInvalidatedException/UnrecoverableKeyException (la rama :64 ya lo detecta).
2) Caché en memoria (key)→plaintext dentro del store; invalidar la clave exacta en put/remove/quarantine y todo el
   caché en clearNamespace.
3) (Opcional C8) fichero SharedPreferences por namespace, o cachear el índice de keys() por namespace.
HONESTIDAD (charter): el caché guarda el MISMO blob que disco; los repos siguen corriendo readVersioned/gates encima
(un blob en cuarentena/versión no soportada sigue fallando cerrado → CALIBRATING/—, nunca un número). El caché se
limpia en sign-out (subject-scoping). null≠0 intacto.
GATE (hand-off si el IDE posee el daemon):
  :shared:desktopTest   # 2ª lectura no re-descifra (spy); put/remove desalojan; clearNamespace vacía; key-invalidation regenera
  :shared:detekt
  :shared:compileAndroidMain
ENTREGA: cambios · archivos · tests · verificación · rollback (caché transparente) · entrada CLAUDE.md.
```

---

## 11. Review prompts (independientes — ejecutar post-reset de sesión)

- **S1/S2 (dinero/GDPR):** `mindboost-security-privacy` + `diff-auditor` sobre el diff — foco: exactitud del conteo bajo
  concurrencia, fail-closed del erasure, cero secreto, cero regresión de reglas.
- **S3 (pérdida de datos):** `silent-failure-hunter` + `diff-auditor` — foco: ¿el replay es idempotente? ¿el journal
  entra en el eraser? ¿algún fallback silencioso?
- **S5 (honestidad):** `kotlin-reviewer` — foco: invalidación-en-write correcta; el caché nunca sirve un blob que los
  gates invalidarían.

## 12. Integrador

Tras cada fase, un pase integrador (`diff-auditor`, post-reset) verifica: (a) gates de la fase en verde/hand-off
documentado; (b) sin regresión de invariantes charter; (c) sin colisión con WIP ajeno del worktree (confirmar que los
findings son de MIS archivos); (d) entradas CLAUDE.md presentes.

## 13. Gates por superficie

| Superficie | Gate | ¿Quién lo corre? |
|---|---|---|
| `functions/` | `npm --prefix functions run typecheck && npm --prefix functions test && npm --prefix functions run scan:secrets` | **yo** (Bash) |
| reglas Firestore/Storage | `npm run test:*-rules` con `--project` (emulador, JDK 21) | yo o founder |
| `:shared` lógica | `:shared:desktopTest` · `:shared:detekt` · `:shared:compileAndroidMain` | **founder** si el IDE posee el daemon |
| `:androidApp` | `:androidApp:compileDebugKotlin` (+ `assembleDebug` si toca) | founder |
| medición dinámica | Macrobenchmark/Perfetto/`dumpsys gfxinfo` (auditoría §5/§8) | founder |

## 14. Riesgos (top) + pre-mortem

| # | Riesgo | Mitigación |
|---|---|---|
| R1 | Migración de contador (A1) corrompe conteos en el corte legacy→shards | Dual-read + backfill lazy + test de concurrencia; el doc legacy se conserva. |
| R2 | Borrado de fondo (A3) reporta completado sin borrar (GDPR) | Fail-closed + test de residuo cero + job idempotente reanudable. |
| R3 | El journal de payload (S3) filtra trials crudos o se salta el eraser | Solo resúmenes; namespace en AccountBound+eraser+accountDeletion en el mismo cambio; test de cobertura. |
| R4 | Un caché (S5) sirve un score que los gates invalidaron | Invalidación-en-write obligatoria + gates corren encima del blob cacheado; sign-out limpia. |
| R5 | Ejecutar Misión-Crítica sin la review que el modo exige (sesión limitada) | S1/S2/S3/S4 esperan a review post-reset; hoy solo se ejecuta el subconjunto seguro (DEC-4c). |
| R6 | Colisión con WIP ajeno (177 entradas) | Confirmar que los findings son de MIS archivos; nunca "arreglar" untracked ajeno. |
| R7 | Gate Kotlin no reproducible (IDE posee el daemon) | Hand-off de comandos; no correr CLI Gradle en paralelo con el IDE. |
| R8 | Gasto no autorizado (minInstances/Memorystore) | DEC-2 lo autoriza explícitamente; recomendación = gasto mínimo, cero Memorystore. |

**Pre-mortem (si esto fracasa dentro de 3 meses):** (1) se ejecutó A1/A3 sin review y un bug de conteo/erasure llegó a
prod; (2) el caché sirvió un score obsoleto y rompió la honestidad de medición; (3) se mezcló F1 (sync) aquí y explotó el
alcance; (4) se ejecutó a ciegas sobre WIP ajeno del worktree sucio. **Todas mitigadas** por: review-gated P0, invalidación
obligatoria, F1 fuera (DEC-3), y verificación de pertenencia de archivos.

## 15. Rollback + stop conditions

- **Rollback:** por slice, `git revert` del slice (nunca reset/clean destructivo). Slices additivos (S5/S6/S7) son
  transparentes al revert. S1 conserva el legacy; S2/S3 son idempotentes.
- **STOP inmediato si:** un gate de backend queda rojo y no es flake; un cambio toca dinero/GDPR sin su review
  independiente disponible; se detecta que un finding pertenece a WIP ajeno; el límite de sesión se alcanza a mitad de
  un slice (dejar el slice en estado revertible, reportar). En cualquier STOP: reportar estado real, no continuar.

## 16. Secuencia recomendada de ejecución (el "orden recomendado")

Dado el límite de sesión y las dependencias de decisión:

1. **AHORA (founder):** resolver DEC-1, DEC-2, DEC-3, DEC-4.
2. **AHORA (yo, si DEC-4=a/c):** subconjunto seguro solo-inline, additivo, sin gasto, sin dinero/GDPR — **S5 (cache) →
   S6 (retry) → S7 (atmosphere) → S12 (hygiene backend, jest-gate-able por mí)**. Gates cliente hand-off.
3. **Post-reset 4:20am (agentes vuelven):** P0 Misión-Crítica con review independiente — **S1 → S2 → S3 → S4 (tras DEC-1)**.
4. **Luego:** P1 restante (S8/S9 tras DEC-2, S10) → P2 (S11, S13, S14, S15).
5. **Fuera (DEC-3):** F1 (Profile V2 sync), F2 (analytics) como tracks propios.

## 17. Checklist de cierre por slice

- [ ] Gate de su superficie en verde (o hand-off documentado con el comando exacto).
- [ ] Invariantes charter intactos (null≠0, evidencia sellada, sin claims clínicos).
- [ ] Namespace nuevo (si aplica) en AccountBound + eraser + accountDeletion.ts (mismo cambio).
- [ ] Sin regresión de scanners (copy/terminology/detekt ratchet).
- [ ] Entrada en "Aprendizajes acumulados" de CLAUDE.md si corrigió un error real.
- [ ] Pertenencia de archivos confirmada (no WIP ajeno).
- [ ] Review independiente (P0) APTO.

## 18. Estado resumible

```yaml
programa: scalability-remediation-2026-07-12
base_sha: 71e17038
decisiones: {DEC-1: fan-out-on-read, DEC-2: gasto-minimo, DEC-3: F1/F2-fuera, DEC-4: hibrido}
prompt_a: APTO_CON_CAMBIOS (5 rondas, 7/7 criterios con delta; run wf_41179753-ba1) -> delta APLICADO en §20
prompt_b_batch_1: COMPLETADO 2026-07-13 (run wf_ab8e1c29-ae3, 17 agentes, integrador = APTO_CON_CAMBIOS)
ejecutado_batch_1:
  - S12a (TTL expireAt + verificacion dedup): EJECUTADO_Y_VERDE (typecheck+jest+scan:secrets). TTL inerte
    hasta activar politica gcloud (hand-off).
  - S1 (contador shardeado A1): EJECUTADO_Y_VERDE (5 gates backend incl. emulador counter-shards). Reviews
    security+diff-audit = APTO_CON_CAMBIOS x2. Sharding SOLO campos con fan-in de terceros; read-model
    counters/{uid} intacto; shards borrados antes del padre en accountDeletion.
  - S2 (borrado paginado + Cloud Tasks): EJECUTADO_Y_VERDE. Reviews APTO_CON_CAMBIOS+APTO. Inoperativo hasta
    habilitar Cloud Tasks API + IAM + deploy (hand-off; fail-closed mientras).
  - S4 (feed fan-out-on-read): EJECUTADO_Y_VERDE lado backend (ADR_FEED_FANOUT_2026-07-12.md + callable
    loadHomeFeed). Review APTO_CON_CAMBIOS (1 solo revisor). INCOMPLETO para prod: swap del read-path cliente
    + deploy de indice compuesto = fase siguiente.
  - S7fix / S5 cache / S6 retry / S3 journal+replay: VERIFICADO_ESTATICAMENTE_HANDOFF (Kotlin; IDE posee el
    daemon). S3 = 20 archivos, namespace nuevo mindboost_payload_journal en AccountBound+eraser+coverage-test.
    Reviews S3 = APTO_CON_CAMBIOS x2. Borradores CLAUDE.md pendientes de gate verde (en el output del run).
gates_founder_pendientes: ver §18-bis (lista consolidada del integrador)
batch_2_pendiente: [S12b (firma webhooks, review security), S8, S9, S10, S11(D-15), S13(D-16), S14(D-06), S15(D-05b), triage de hallazgos APTO_CON_CAMBIOS de las reviews, swap cliente S4]
siguiente_accion: >
  founder corre los 7 gates Kotlin consolidados EN SERIE + hand-offs infra (gcloud TTL, Cloud Tasks
  API/IAM, deploy indices+functions); con verde, escribir las entradas CLAUDE.md pendientes; luego batch 2.
```

## 19. Decisiones pendientes + informe del validator

- **Decisiones que necesito de ti:** DEC-1 (feed), DEC-2 (gasto), DEC-3 (alcance F1/F2), DEC-4 (arranque hoy). Ver §4.
- **Validator del paquete (`validate-ceo-package.mjs`):** `NO EJECUTADO`. Razón: materializar este programa de 15 slices
  como JSON conforme al schema es desproporcionado frente a tu intención (plan + ejecución), y el artefacto primario aquí
  es este markdown ejecutable + los prompts §10. Puedo materializarlo y correr el validator si quieres el gate formal.
- **Honestidad de estado global:** `NO EJECUTADO` (ninguna slice corrida). El plan está `EJECUTADO Y VERDE` como
  compilación; la ejecución espera tus decisiones + (para P0) el reset de sesión que restaura la review independiente.
```

---

## 20. Delta vinculante (PROMPT A — APTO_CON_CAMBIOS, aplicado 2026-07-12)

> Auditoría adversarial en contexto fresco (Fable 5 · effort max · 5 rondas · 7/7 criterios de convergencia con
> este delta · run `wf_41179753-ba1`, hallazgos completos en su output). **Estas enmiendas SUPERSEDEN §9/§10/§16
> donde entren en conflicto.** Todo implementador aplica las D-xx de su slice ANTES de tocar código.

- **D-01 (S1 · F-01 CRITICAL):** archivos de S1 incluyen `functions/src/accountDeletion.ts`. Nuevo paso: tras el
  delete de `counters/{uid}` (`:364`), borrar la subcolección `counters/{uid}/shards` con `deleteByQuery` paginado
  (borrar el doc padre NO borra subcolecciones) + test jest de residuo cero de shards.
- **D-02 (S1 · F-07):** backfill lazy TRANSACCIONAL: en la MISMA tx del follow, leer `counters/{uid}` legacy y un
  marker `migratedToShards`; si falta, `tx.set(shard0 = legacy + delta)` + `tx.set(marker)` — dos primeros-increments
  concurrentes no pueden sembrar dos veces (la tx perdedora reintenta y ve el marker). Test de carrera de siembra.
- **D-03 (S1 · F-14 + M-01):** shardear también los deltas de `blockUser` sobre el TARGET (`socialGraph.ts:459-464`);
  decidir `blockedUsersCount` (shard o justificar legacy: solo lo escribe el propio caller); coerción `>= 0` en la
  lectura agregada (`FieldValue.increment` no clampa; hoy protege `Math.max(0,...)`). `counters/{uid}` se CONSERVA
  como read-model eventualmente consistente mantenido por backend — cliente y rules de lectura no cambian.
- **D-04 (S1 gate · F-05):** el test de concurrencia va contra el EMULADOR Firestore (no el harness de mocks de
  `security.test.ts`): suma exacta + siembra única bajo carrera. DECLARAR: la contención real (~1 write/s/doc) no es
  reproducible en emulador — la prueba de producción es el load-test de auditoría §5 en staging (hand-off founder).
- **D-05 (S5 · F-02/F-03/F-08/F-12/F-20):** (a) la caché de valor es un decorator OBLIGATORIO en `commonMain`
  (testeable con spy en desktopTest) y COMPARTIDA a nivel de proceso por namespace físico (registro keyed por
  `safeNamespace+physicalKey`, invalidación compartida — NUNCA per-instance: el mismo namespace está abierto en >=9
  call-sites). (b) Añadir write-version por namespace (contador compartido incrementado en
  `put/remove/clearNamespace/quarantine`), expuesto para S15.D2. (c) Limpieza en sign-out (hook en el observer de
  authState de `App.kt:98` o caché keyed-by-subject) + test. (d) PROHIBIDO compartir instancias de `Cipher`
  (stateful, no thread-safe): cachear solo `SecretKey`. (e) La opción C8 (prefs por namespace) NO se ejecuta en este
  slice sin migración one-time desde `neu_secure_focus` + fallback de lectura + test de no-pérdida; alternativa sin
  migración: cachear el índice de `keys()` en el decorator.
- **D-06 (S14 · F-04 + M-09):** gate factible: replicar el patrón TEST-ONLY de `storage.dm.rules.test.mjs:17-26`
  (parchear el guard App Check al cargar reglas en el emulador) para conservar los 134 tests verdes; el deny-path se
  prueba con `request.app` nulo; añadir check estático `functions/scripts/check-firestore-appcheck.js` (espejo de
  `check-storage-rules.js`). Dependencia nueva: el guard cubre TAMBIÉN `counters/{uid}/shards/{n}` (S1→S14).
- **D-07 (S12 · F-09/F-10/F-11):** partir S12. **S12a (inline hoy):** añadir campo `expireAt` de tipo Timestamp a
  `rate_limits` y `profile_visits` (los campos actuales son NUMBER y no sirven para TTL) + hand-off founder con el
  comando gcloud exacto para activar las políticas TTL; el dedup de webhooks por event-id YA existe
  (`webhooks.ts:178-181`) — NO reimplementar. **S12b (post, con review security):** implementar verificación
  criptográfica REAL de firma de webhooks (hoy stub fail-closed `:115-122`) ANTES de relajar el throttle IP.
  §16.2 queda: subconjunto inline de hoy = S5 → S6 → S12a.
- **D-08 (S3 · F-16 + M-02):** re-anclar al hub ACTUAL: `MindBoostHubScreen.kt:198` YA ordena
  persist→finalize→navigate vía `sealDecisionsSession` con write-ahead lifecycle subject-scoped (`:160-171`) y flag
  sealed (`:168`); `:182` es el abandono; `:192` (reversibility) SIGUE fire-and-forget — el delta restante es SOLO
  journal de payload + replay. Payload = EXACTAMENTE lo que el store destino ya persiste (ReversibilityTrialDto,
  DecisionSession+DiaryEntry, CognitiveSessionRun E8), nunca un snapshot más rico. La escritura del journal COMPLETA
  antes de que el callback de navegación retorne. Timestamps vía `EpochClock` inyectado (desconocido = null, jamás
  0L). Test de drenado: journal vacío tras confirmación (minimiza la copia transitoria del diario). En
  `accountDeletion.ts` el journal local-only es no-op — verificarlo y documentarlo, no omitirlo en silencio.
- **D-09 (S2 · F-17 + M-05):** checklist de prereqs de despliegue como hand-off founder (habilitar Cloud Tasks API +
  rol IAM enqueuer para el SA de Functions); jest mockea el enqueue. VERIFICADO ejecutando node: firebase-functions
  5.1.1 expone `functions.tasks.taskQueue` en v1 y firebase-admin 12.7.0 expone `getFunctions()` (sin migrar a v2 —
  learning 2026-06-19 respetado). El cursor por colección se persiste en el job doc existente
  (`account_deletion_jobs` + `markStep`, `accountDeletion.ts:104-124`).
- **D-10 (S4 · F-18):** mini-ADR OBLIGATORIO antes de implementar: algoritmo acotado del fan-out-on-read (chunks
  `in`<=30, cap de following, paginación), definición del caché de servidor SIN Memorystore (p.ej. doc
  `feed_cache/{viewer}` con TTL o caché por instancia best-effort), política de invalidación, coste estimado por
  feed-load, y plan de transición del read-path cliente (`GlobalFeedRepositoryProvider.android.kt:61-67` hoy lee
  `feeds/{uid}/posts`; rules `:1694-1699`; `accountDeletion.ts:409`): poblar `feeds/`, sustituir por callable, o
  retirar la ruta de rules/erasure.
- **D-11 (canon · F-06):** toda referencia = `docs/context_pack/` (canon vivo v2.0.2). La copia `docs/context-pack/`
  (2.0.0) está OBSOLETA — tarea con owner para eliminarla/redirigirla.
- **D-12 (cobertura · F-15):** §6 gana la fila `S13 <- C4` (LazyColumn con key estable si el hub crece — DIFERIDO,
  owner kmp-architect). §5 non-goals gana: F4 (historial `CognitiveSessionRun` unificado / consumidor E8 /
  calibración — DIFERIDO, owner kmp-architect + assessment-diagnostics, track SYSTEM-1). §5 in-scope incluye F3
  (cerrado por S3).
- **D-13 (S7 · F-13):** completar el freeze: crear `rememberInfiniteTransition`/`animateFloat` SOLO cuando
  `animated == true` (hoy `FocusAtmosphere.kt:33-51` corren SIEMPRE — ticks residuales por frame en la superficie
  RT-crítica aunque el draw no invalide). Gate añade la medición A/B `dumpsys gfxinfo framestats` (hand-off).
  Estado §18: S7 = aplicado PARCIAL hasta esto.
- **D-14 (S6 · M-06):** el retry envuelve CADA intento en su propio `with*Timeout` (timeout por intento, no global);
  reintentos de callables SOLO con el MISMO `idempotencyKey` (`WalletApiClient.kt:27` ya lo contempla); test del
  backoff con reloj virtual (`runTest`).
- **D-15 (S11 · F-19):** eliminar la condición muerta `SDK_INT < 26` (minSdk=29); LOW se deriva de
  `isLowRamDevice`/`memoryClass`.
- **D-16 (S13 · M-07):** verificado: `boolOrDefault` cae al default de CÓDIGO cuando `VALUE_SOURCE_STATIC`
  (`MindOSApplication.kt:151-154`) => diferir `setDefaultsAsync`/`fetchAndActivate` es seguro para los kill-switches.
- **D-17 (§13 · M-04):** métrica de éxito por slice con el comando de auditoría §5: S5 = nº de `KeyStore.load` por
  apertura del brain-profile; S7 = gfxinfo A/B; S13 = TTID (`am start -W`); S1 = load-test follow en staging;
  S14 = `enforcementMode == ENFORCED`.
- **D-18 (PROMPT B §3 · M-08):** los dirs de features MindBoost están ESCANEADOS por el copy-scanner: citar
  invariantes POR REFERENCIA (ForbiddenLexicon/CLAUDE.md), nunca copiar a comentarios/tests redacción del plan que
  nombre lexemas prohibidos; correr la variante del scanner del source-set tocado hasta verde.
- **D-19 (S3+S10 · M-03):** decidir en S3 si el journal de payload (local) y el outbox de intents (remoto, S10)
  comparten UNA primitiva append-only idempotente o son mecanismos separados con rationale — y reflejarlo en S10.
- **D-20 (§7 DAG):** aristas nuevas: `S1 -> S14` (el guard App Check cubre los shards) · `S1 -> accountDeletion`
  (paso de shards en el MISMO cambio) · `S15.D2 -> S5` (write-version por namespace, añadido a S5 por D-05b).
