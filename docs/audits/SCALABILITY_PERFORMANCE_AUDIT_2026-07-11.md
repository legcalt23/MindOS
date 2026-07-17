# Scalability & Performance Audit — Molvex (MindOS) · 2026-07-11

**Auditor:** Staff Performance & Scalability lens (read-only). **Scope:** ¿aguanta 1M+ usuarios y es resistente? ¿es fluida en cualquier teléfono? ¿dónde falta caché? **Método:** auditoría estática con evidencia `archivo:línea`, tres lentes en paralelo (backend-scale, resilience, client-fluidity) cuyos hallazgos de carga fueron **re-verificados a mano contra la fuente** antes de entrar aquí. No se aplicó ningún fix.

- **CONFIRMED** = verificable estáticamente en el código citado. **HYPOTHESIS** = necesita medición en dispositivo/emulador (comando exacto en §5). **Nunca** se fabrican números de benchmark.
- Árbol canónico `com/mindos/app/**` + `functions/src/**`. Worktrees (`.claude/worktrees/`, `.codex-security-worktrees/`) y rutas legacy `com/example/mindos` excluidos.
- HEAD auditado: `71e17038`.

---

## 0. Marco: dónde está realmente el riesgo a 1M

El núcleo cognitivo de MindBoost es **local-first** (`SecureKeyValueStore` cifrado; solo resúmenes; el gateway de sync Firestore está tras un flag OFF). Por diseño, entrenar **no cuesta operaciones de servidor**: el coste y la contención a escala se concentran casi por completo en la **capa social / wallet / feed** (backend Firestore + Cloud Functions) y en la **observabilidad/continuidad** (sin sync ni analítica). El cliente es fluido en gama media/alta pero no tiene ruta de degradación para gama baja y re-descifra agresivamente.

Ningún hallazgo es corrupción de datos hoy. Pero **A1 (contador caliente) y A3 (borrado que revienta) rompen features a escala**, **A2 deja el feed sin decidir su modelo de fan-out**, y **B1 pierde en silencio una sesión completada si el proceso muere durante el guardado**.

---

## 1. Resumen ejecutivo — veredicto por dimensión

| Dim | Área | Veredicto a 1M | Por qué (bloqueantes) |
|---|---|---|---|
| **A** | Backend a 1M | **NO APTO** | `counters/{uid}` doc caliente por follow (A1·CRITICAL), borrado de cuenta que carga el set completo en una invocación (A3·HIGH), feed materializado **sin writer** — el punto donde se decide el fan-out a 1M (A2·HIGH), GC de media que barre 1M en ~200 días (A4·HIGH), broker de voz con techo ~20 concurrentes (A5·HIGH). |
| **B** | Resiliencia | **APTO CON CONDICIONES** | Cobertura de timeouts **completa** (1 sola excepción) y write-ahead de lifecycle cableado en los 8 módulos — pero el write-ahead **no lleva el payload** (recovery solo abandona, B1·HIGH), **cero retry/backoff** (B2·HIGH), y las callables **no encolan offline** (B4). |
| **C** | Fluidez de cliente | **APTO CON CONDICIONES** | Sin descifrado en hilo principal (positivo), pero el primer frame se bloquea tras un descifrado de recovery (C1), 4 inits Firebase + SimpleCache/SQLite síncronos en el arranque (C2/C3), y una animación decorativa siempre activa **sobre la pantalla de medición RT** que puede contaminar la propia medida (C5·HIGH). |
| **D** | Caché para cargas ultrarrápidas | **APTO CON CONDICIONES** | El perfil ya pinta un snapshot cacheado al instante (positivo), pero el store **no cachea ni la `SecretKey` ni el valor**: cada lectura rehace `KeyStore.load` + `Cipher` y una lista es N descifrados secuenciales (D1·HIGH). No rompe a escala; rompe la *sensación de velocidad en cualquier teléfono*. |
| **E** | Adaptación por dispositivo | **APTO CON CONDICIONES** | **No existe** ningún device-tier consumible por UI (E1·HIGH): blur + animaciones infinitas corren idénticas en un flagship y en un teléfono de 2 GB. Las señales de capacidad que existen (`FrameStatsCalculator`, `DeviceTimingRisk`) están atadas a fiabilidad de scoring, no a render. Reutilizables sin nueva medición. |
| **F** | Deuda transversal | **APTO CON CONDICIONES** | El core local-first escala, pero **sin Firestore-sync/outbox durable** (nuevo teléfono = pérdida total del perfil cognitivo, F1) y **sin analytics sink** (a 1M estás ciego a `session_save_failed`/abandono, F2). Ambos declarados en `EXERCISE_QUALITY_CONTRACT §21`. |

**Una frase:** el entrenamiento escala solo; **arregla A1/A2/A3 antes de crecer**, cablea recuperación-de-payload + retry (B1/B2), y da a MindBoost un outbox durable + analítica (F1/F2) antes de reclamar "listo para 1M".

---

## 2. Tabla completa de hallazgos

Severidad: **CRITICAL** (rompe a escala o pierde datos) / HIGH / MEDIUM / LOW.

### Dimensión A — Backend a 1M

| id | sev | archivo:línea | clase | Impacto a 1M | Fix (1 línea) |
|---|---|---|---|---|---|
| **A1** | **CRITICAL** | `functions/src/socialGraph.ts:96` (+`:93,135,200,242,303,451`) | CONFIRMED | `counters/{targetId}.followersCount` es UN doc mutado en txn en cada follow/request/block. Una cuenta viral seguida a >~1/s excede el techo de Firestore (~1 escritura sostenida/s/doc) → `ABORTED`, reintentos, follows que fallan. | Contador distribuido en shards `counters/{uid}/shards/{0..N}`, sumados en lectura. |
| **A2** | HIGH | `shared/src/androidMain/.../social/data/feed/GlobalFeedRepositoryProvider.android.kt:61` · `functions/src/posts.ts:60` · `functions/src/index.ts` (sin `onPostCreated`) | CONFIRMED | El cliente LEE el feed materializado `feeds/{viewerId}/posts` pero **ninguna Cloud Function lo escribe**; `createPost` solo escribe `posts/`+`user_posts/`. El feed home está sin poblar hoy, y es el punto exacto donde se decide el fan-out a 1M. | Fan-out-on-read con caché de servidor, o worker de fan-out acotado — **nunca** una escritura síncrona por-seguidor en `createPost`. |
| **A3** | HIGH | `functions/src/accountDeletion.ts:128` (`deleteByQuery` → `await query.get()`), `:374` (`follows/followeeId`), `:208`, `:231` | CONFIRMED | Las lecturas de borrado cargan el **set completo** en una invocación de 256 MB/60 s (sin `.limit()`/cursor). Una cuenta con muchos seguidores/ledger → OOM o timeout → erasure GDPR atascada `running`, re-escanea desde cero en cada retry. | Paginar con cursor + mover a job de fondo (Cloud Tasks/`onDispatch`) con pasos checkpointeados. |
| **A4** | HIGH | `functions/src/lib/profileCardMediaGc.ts:46` (`DEFAULT_MAX_USERS=5000`), `:263` (lista todo el bucket) · `functions/src/profileCardGc.ts:55` | CONFIRMED | GC diario lista el bucket entero en memoria antes del cap → OOM/timeout (60 s/256 MB, sin `runWith`); 5000 usuarios/run ⇒ ~200 días para barrer 1M una vez → media huérfana + $ Storage crecen sin límite. | Paginar con `pageToken`, shardear por prefijo de uid, o GC por evento de mutación. |
| **A5** | HIGH | `functions/src/speechBroker.ts:460` (`maxInstances: 20`) | CONFIRMED | v1 = 1 request/instancia ⇒ 20 instancias = **~20 ASR concurrentes globales**, cada una ≤30 s → techo ~0.7 req/s. Cualquier adopción real de voz a 1M satura → `resource-exhausted`. | Subir `maxInstances` tras guard de coste + v2 concurrency o cola. |
| **A6** | MEDIUM | `functions/src/index.ts` (ningún export con `runWith`/`region`; solo `speechBroker.ts:457`) | CONFIRMED | Todas las callables económicas/sociales corren en `us-central1` por defecto (latencia global), 256 MB/60 s, **sin `minInstances`** (cold starts en wallet/spend), **sin `maxInstances`** (un pico abre conexiones DB sin límite). | Fijar región + `minInstances` en callables económicas calientes + guard `maxInstances`; migrar a v2. |
| **A7** | MEDIUM | `functions/src/lib/rateLimiter.ts:82` y `:120` (txn read+write por acción); TTL ausente | CONFIRMED | Cada acción hace una txn Firestore extra sobre `rate_limits/{uid}_{action}` → ~2× ops de escritura + suelo de latencia por acción; los buckets **no tienen TTL/GC** → crecimiento ilimitado (~#usuarios×#acciones docs). | Token-bucket en Memorystore para lecturas de alta frecuencia (`getBalance`); política TTL en `rate_limits`. |
| **A8** | MEDIUM | `functions/src/profileVisits.ts:28-75` (`:43` doc por par) | CONFIRMED | Cada visita = txn rate-limit + `hasBlockBetween` (lecturas) + txn de visita ≈ 4-5 ops; el dedup ahorra la escritura, no las lecturas. Los docs `profile_visits/{viewer}_{owner}` se acumulan por par sin TTL → crecimiento potencialmente N². | TTL sobre `profile_visits` + reducir ops por visita (cachear bloqueos). |
| **A9** | MEDIUM | `functions/src/webhooks.ts:90` · `functions/src/lib/rateLimiter.ts:55` (`webhook 60/min/IP`) | CONFIRMED (latente) | Los proveedores de pago entregan desde pocas IPs → todas las entregas contienden sobre UN doc `rate_limits` y el cap 60/min/IP estrangula tráfico legítimo a escala de compra. | No throttlear webhooks firmados por IP en Firestore — verificar firma + dedup por event-id. |
| **A10** | MEDIUM | `firestore.rules` (grep: **cero** `request.app`/`isAppChecked`) vs `storage.rules:10,102,310,430` | HYPOTHESIS | Las lecturas directas de contenido privado (`wallets/{uid}`, `notifications`, `dm_chats`, `counters`) están App-Check-gated solo si el toggle de enforcement del proyecto está ON — no verificable desde la fuente. | Verificar enforcement en consola (§5); añadir `request.app != null` a reglas de lectura privada como defensa en profundidad. |

**Positivos verificados (dim A) — NO son hallazgos:**
- **Todas** las reglas `allow list` llevan `request.query.limit != null && <= N` (`firestore.rules:1629,1667,1681,1696,1710,1721,1798,1806,1814,1822`; helpers `boundedUsernameSearch`/`boundedDMProfileDiscovery` capan ≤12). Sin agujero de amplificación de query.
- El **rate limiter es Firestore-backed** (`rateLimiter.ts:82-114`) → sobrevive cold starts (no se resetea por instancia). Los actos económicos son fail-closed (`:184-204`).
- **Sin fan-out de escritura** en `createPost` (`posts.ts:74-96`): evita el write-storm clásico de celebridad (el riesgo se traslada al modelo de feed, A2).
- Ledger de wallet **por usuario** (`balance_snapshots/{walletId==uid}`, lock optimista de versión) — sin hotspot global. `getWalletTransactions` capa ≤100 con cursor.
- **App Check enforced** en cada callable (`guards.ts:30-46`) y en todo Storage privado.

### Dimensión B — Resiliencia

| id | sev | archivo:línea | clase | Impacto a 1M | Fix (1 línea) |
|---|---|---|---|---|---|
| **B1** | HIGH | `shared/.../mindboost/core/session/SecureSessionRecordStore.kt:18` · `SessionRecoveryScanner.kt:31` | CONFIRMED | El write-ahead persiste **solo lifecycle** (id/block/status/counters/durations), nunca el payload puntuado; recovery solo marca huérfanos `ABANDONED`. Un crash/OOM durante el guardado real pierde una sesión **completada** sin reconstrucción → filas `FINALIZED` fantasma. | Journalizar el payload minimizado (outbox append-only) antes de navegar; recovery re-ejecuta el guardado pendiente, no solo abandona. |
| **B2** | HIGH | `shared/.../core/network/FirestoreDefaults.kt` (solo timeouts) · `shared/.../wallet/data/WalletApiClient.kt:175` | CONFIRMED | **No existe retry/backoff** en ninguna parte; todo blip transitorio (cold start, pérdida de 1 paquete, `UNAVAILABLE`) es terminal y exige un tap manual → millones de fallos visibles/tickets/compras abandonadas a escala. | Wrapper exp-backoff+jitter (≤3 intentos) para lecturas idempotentes y callables con idempotency-key. |
| **B3** | HIGH | `shared/.../mindboost/presentation/ui/MindBoostHubScreen.kt:198` | CONFIRMED | El seal de Decisiones es fire-and-forget sobre `rememberCoroutineScope` (`:85`): muerte de proceso entre `launch` y fin pierde `DecisionSession` + diario reflexivo; el write-ahead luego lo marca `ABANDONED`, nunca restaura. | Persistir el payload por write-ahead antes de navegar, sobre scope de vida de proceso. |
| **B4** | MEDIUM | `shared/.../wallet/data/WalletApiClient.kt:83` · `.../social/data/repository/ProfileVisitRepositories.kt:41` · `.../core/session/FirebaseSessionRepository.kt:99` | CONFIRMED | Todas las escrituras van por **callables HTTPS**, que saltan la persistencia offline de Firestore. Offline = fallo terminal inmediato sin cola/replay → cada momento de metro/ascensor falla compras, posts, follows. | Outbox local de intents (idempotency-keyed) reproducido al reconectar, o UI gated por conectividad. |
| **B5** | MEDIUM | `shared/.../social/presentation/feed/GlobalFeedComposerStateHolder.kt:299` · `GlobalFeedRepositoryProvider.android.kt:104` | CONFIRMED | Publicar sube adjuntos a Storage y **luego** llama `createPost`; el delete compensatorio solo corre en el `catch`. Muerte de proceso tras subir/antes de la callable deja bytes huérfanos + post perdido → fuga de $ Storage sostenida. | GC servidor de `post_attachments` huérfanos, o subir-después-de-callable / outbox resumible. |
| **B6** | MEDIUM | `shared/.../mindboost/speed/presentation/e8/GoNoGoSessionStateHolder.kt:300` | CONFIRMED | El path mejor endurecido: `withContext(NonCancellable){ … save CognitiveSessionRun }` sobrevive dispose pero **no** muerte de proceso (documentado en `:300-301`). Lifecycle FINALIZA pero el run puntuado se pierde; los outcomes crudos no se persistieron → recovery no puede reconstruirlo. | Igual que B1 (journal de payload + replay al siguiente arranque). |
| **B7** | MEDIUM | `shared/.../mindboost/presentation/ui/MindBoostHubScreen.kt:192` | CONFIRMED | `mindBoostScope.launch { reversibilityStore.record(userId, trials) }` — fire-and-forget **sin** write-ahead; un kill durante la escritura pierde los trials de reversibilidad (ninguna recovery cubre este store). | Await antes de navegar, o añadir al write-ahead/outbox. |
| **B8** | MEDIUM | `GlobalFeedRepositoryProvider.android.kt:67,83,126` · `FirebaseSocialInteractionRepository.kt:128` · `FirebaseUsernameSearchRepository.kt:42` · `FirebaseProfileCardRepository.kt:41` | CONFIRMED (comportamiento HYPOTHESIS) | Feed/search/profile-card/observation usan `.get()` de una vez (no `.snapshots`). Offline solo resuelven si la caché local de Firestore está caliente; en caché fría la pantalla queda vacía/error tras el timeout. | Lecturas cache-first + UI "offline, mostrando cacheado"; verificar persistencia habilitada. |
| **B9** | LOW | `shared/.../profile/card/data/FirebaseProfileCardMediaRepository.kt:79` | CONFIRMED | `storage.reference.child(storagePath).delete()` es la **única** suspend Firebase del árbol canónico **sin** `with*Timeout` (su hermana `ProfileVisualRepositories.deleteMedia:257` sí envuelve). Un delete colgado puede colgar la corrutina (best-effort, radio acotado). | Envolver en `withStorageReadTimeout(StorageDefaults.DeleteTimeout){ … }`. |
| **B10** | LOW | `shared/.../profile/card/presentation/ProfileCardStateHolder.kt:187` | CONFIRMED | `scheduleSave()` debounce 300 ms → `launch { repo.save }`. Muerte de proceso en la ventana pierde las últimas ediciones de apariencia (cosmético, re-editable, best-effort por diseño). | Flush del guardado pendiente en stop/dispose. |

**Positivos verificados (dim B):** cobertura de `with*Timeout` **completa** salvo B9 (verificado sobre DM/session/profile-visual/social/wallet/feed/card repos); write-ahead de lifecycle + orphan recovery **cableado en los 8 módulos** MindBoost (cada root screen llama `…SessionRecovery().recover()`); DM usa `.snapshots` cache-backed (`FirebaseDMRepository.kt:65,168`).

### Dimensión C — Fluidez de cliente

| id | sev | archivo:línea | clase | Impacto (cualquier teléfono) | Fix (1 línea) |
|---|---|---|---|---|---|
| **C5** | HIGH | `shared/.../focus/presentation/ui/components/FocusAtmosphere.kt:33` · usado en `FocusMeasureRunningContent.kt:56` (+7 pantallas focus) · `SessionQualityEvaluator.kt:83` | CONFIRMED (render) / HYPOTHESIS (jitter) | `rememberInfiniteTransition` siempre activo redibuja 2 gradientes radiales a pantalla completa cada frame, **incluida la ventana de medición RT**: la contención de frames puede inflar `criticalFrameJitter` → `SessionQuality.CONTAMINATED`. La decoración degrada la medida que decora. | Congelar la animación durante trials RT-críticos; gate de frecuencia por device-tier (E1). |
| **C1** | MEDIUM | `shared/.../App.kt:83-91` (+ `AccountDeletionCoordinator.kt:164`) | CONFIRMED (gate) / HYPOTHESIS (coste) | El primer frame de **todo** arranque en frío se bloquea tras `recoverPendingDeletion()` (descifrado de `account_deletion_journal` + `currentUser()`), mostrando solo `LaunchLoadingScreen`. | Resolver ruta inicial ya; correr recovery en paralelo y bloquear solo el path destructivo, no el primer frame. |
| **C2** | MEDIUM | `androidApp/.../MindOSApplication.kt:21-24` | HYPOTHESIS | 4 inits Firebase síncronos en `onCreate` (AppCheck install, Firebase init, Crashlytics, RemoteConfig defaults+fetch) → sumados al TTID de cada arranque. | Mantener Crashlytics/AppCheck sync; diferir `setDefaultsAsync`/`fetchAndActivate` de RemoteConfig a un initializer de fondo. |
| **C3** | MEDIUM | `androidApp/.../MainActivity.kt:44` → `MediaPreloadManager.android.kt:34-43` | CONFIRMED | `MediaPreloadManager.initialize()` construye un `SimpleCache` media3 (`StandaloneDatabaseProvider` = abrir SQLite + `mkdirs()`) en el **hilo principal antes de `setContent`**, para una feature de vídeo no necesaria al arrancar. | Lazy-init en la primera entrada al visor, o mover a `Dispatchers.IO`. |
| **C6** | MEDIUM | `shared/.../features/home/presentation/HomeScreen.kt:105,151,230` | CONFIRMED (estructura) / HYPOTHESIS (coste neto) | En cada entrada a Main: 4 repos Firebase construidos + un bootstrap `getConfig`+`saveConfig` condicional + 2 cargas de perfil a Firestore sin guard por sesión. | Guard del bootstrap tras flag once-per-session; cachear el visual config en memoria. |
| **C4** | LOW | `shared/.../mindboost/presentation/ui/MindBoostHubScreen.kt:236-242` | CONFIRMED | 8 track cards + plan + onboarding en `Column`+`verticalScroll` (sin virtualización). Aceptable hoy (lista fija ~10); crece con cada track nuevo. | `LazyColumn` con `key` estable por track si la lista crece. |
| **C7** | LOW | `shared/src/androidMain/.../AndroidRawImageLoader.kt:18` (callers `WaveMailboxIcon.android.kt:16`, `DMListScreenIcon.android.kt:14`) | CONFIRMED | `BitmapFactory.decodeResource` corre en hilo principal dentro de `remember{}` en composición, full-res (sin `inSampleSize`) — solo iconos de DM. | Decodificar off-main-thread con downsampling, o `painterResource`/vector. |
| **C8** | LOW | `shared/.../focus/data/local/SecureKeyValueStore.android.kt:49-51,88-102,202` | CONFIRMED | UN solo `SharedPreferences` (`neu_secure_focus`, constante) respalda TODOS los namespaces cognitivos: `keys(prefix)`/`clearNamespace()` iteran `prefs.all` = O(total de claves cross-namespace), y el primer acceso parsea todo ese XML de una vez (jank de primera lectura conforme el fichero crece con el uso). Solapa D1. | Fichero `SharedPreferences` por namespace, o cachear el índice de claves por namespace (junto a la caché de valor de D1). |

**Positivo verificado (dim C):** **no hay descifrado en hilo principal en composición**: `SecureKeyValueStore.read/put` son `suspend` sobre `withContext(Dispatchers.IO)` (`SecureKeyValueStore.android.kt:53,68`) y los constructores de store no hacen I/O (`prefs` es `by lazy`, `:49`); las ~15 construcciones de store en `AppGraph.kt:124-137` son baratas. El coste está en las **lecturas** (D1), no en la composición.

### Dimensión D — Caché para cargas ultrarrápidas

| id | sev | archivo:línea | clase | Impacto | Fix (1 línea) |
|---|---|---|---|---|---|
| **D1** | HIGH | `shared/.../focus/data/local/SecureKeyValueStore.android.kt:53,162` · amplificado en `LocalFocusRepositories.kt:62,114` | CONFIRMED | `read()` **no cachea el valor** y rehace la cripto por llamada: `KeyStore.getInstance().load(null)` + `getKey` + `Cipher.getInstance` en cada lectura. `recent()`/`probeHistory()` hacen `keys().mapNotNull { readVersioned }` → **N descifrados secuenciales = N loads de KeyStore + N init de cipher + N dispatches IO** por lista, por entrada de pantalla. | Cachear la `SecretKey` (y reusar `Cipher`) por instancia de store; caché en memoria de valores descifrados por namespace, invalidada en `put`/`remove`/`clearNamespace`/`quarantine`. |
| **D2** | HIGH | `shared/.../profile/presentation/MindBoostBrainProfileStateHolder.kt:28` · `MindBoostProfileService.kt:31,34` · `RememberBrainProfileStateHolder.kt:37` | CONFIRMED | `cached()` pinta el snapshot persistido al instante (**bien**), pero `refresh()` re-lee + re-descifra ~10 stores de módulo (focus history+profiles, speed, logic, emotion, memory×3, decisions) **incondicionalmente** en cada swipe al perfil. | Mantener el pintado cacheado; **saltar `refresh()`** salvo que una versión-de-escritura de namespace haya avanzado (dirty-version por store). |
| **D3** | MEDIUM | `shared/.../profile/data/local/LocalMindBoostProfileRepository.kt:25` · `LocalFocusRepositories.kt:38,77` | CONFIRMED | `load()`s de blob único re-descifran el mismo blob sin cambios en cada llamada; varios holders pueden re-cargar dentro de una sesión. | Subsumido por la caché de valor por key de D1. |
| **D4** | MEDIUM | `shared/.../features/home/presentation/HomeScreen.kt:151-192` | CONFIRMED (estructura) | `ProfileVisualConfig` `getConfig` re-fetch de Firestore (+ escritura condicional) en cada entrada a Home → lecturas/escrituras facturadas por sesión a 1M. | Cachear el config por sesión de app; escribir solo al cambiar. (Solapa C6/A8.) |

**Mapa "qué cachear DÓNDE" + garantía de honestidad:**

| Qué | Dónde | Qué lo invalida | Guardia de honestidad |
|---|---|---|---|
| `SecretKey` resuelta + `Cipher` reusable por namespace | dentro de `AndroidSecureKeyValueStore` | rotación de clave / `KeyPermanentlyInvalidatedException` (ya detectada en `:64`) → soltar clave | ninguna (material cripto, sin semántica de score) |
| Valor descifrado por key (`Map<String,String>`) | dentro de `AndroidSecureKeyValueStore` | `put`/`remove`/`clearNamespace`/`quarantine` de esa key (todos en el mismo archivo) | la caché guarda el MISMO blob opaco que el disco; los repos siguen corriendo los gates `readVersioned` encima → un blob en cuarentena/versión no soportada sigue fallando cerrado |
| Último snapshot del brain-profile | **ya existe** vía `LocalMindBoostProfileRepository` + `service.cached()` — reusar para pintado instantáneo | lo sobrescribe `refresh()`; saltar `refresh()` salvo que una versión de namespace cambie | **Nunca sirve un score que los gates invalidarían:** el snapshot es la SALIDA del agregador (post-gate); un cache-hit solo reproduce un resultado ya gated. **Recomputar (nunca parchear)** cuando una versión de store avance, para que una dimensión recién invalidada recompute a `CALIBRATING`/`—`, jamás un número fabricado (`null ≠ 0`). |
| `ProfileVisualConfig` (bootstrap Home) | en memoria por sesión, keyed por `userId` | sign-out / edición explícita de perfil | metadato UI/social — sin score cognitivo, sin gate |

**Orden:** **D1 primero** — es la primitiva compartida que también arregla D2/D3 (cada descifrado de lista y cada lectura de fuente del brain-profile pasa por `AndroidSecureKeyValueStore.read`).

### Dimensión E — Adaptación por capacidad de dispositivo

| id | sev | archivo:línea | clase | Impacto | Fix (1 línea) |
|---|---|---|---|---|---|
| **E1** | HIGH | grep `deviceTier/PerformanceTier/isLowRamDevice/ActivityManager` → **0 hits de producción** (solo `arena` `DeviceClass.LOW_END_60HZ` en `ComparabilityEnums.kt:10`, modelo sintético offline) · `Modifier.blur` en `AppLoadingOverlay.kt:89`, `PhotoUploadLoader.kt:172`, `ElementSelectionScreen.kt:189` · animación C5 | CONFIRMED | **No hay** tier LOW/MID/HIGH consumible por UI. Blur (solo gated por `blurEnabled` de Android-12, no por potencia) + animaciones infinitas corren idénticas en flagship y teléfono de 2 GB → jank en gama baja. | Introducir `DeviceRenderTier` (CompositionLocal) y gatear blur + frecuencia de `FocusAtmosphere` por él. |
| **E2** | INFO | `shared/.../core/timing/FrameTimingProbe.kt` + `.android.kt` + `FrameStatsCalculator.kt:26` · `SpeedEnums.kt:48` (`DeviceTimingRisk`) · `SessionQualityEvaluator.kt:48` | CONFIRMED | Las únicas señales de capacidad existentes están atadas a **fiabilidad de scoring**: la probe Choreographer mide un bloque de trial (`start/stop`) y `DeviceTimingRisk{LOW,MODERATE,HIGH}` se deriva de `apiLevel`+`criticalFrameJitter` y lo consume SOLO el gate de perfil de velocidad, nunca un renderer. | Reusar `FrameStatsCalculator` como input de degradación en runtime del nuevo tier (sin nueva medición). |

**Propuesta device-tier (reuse-first, sin degradar jamás la integridad de medición):**
```kotlin
// commonMain
enum class DeviceRenderTier { LOW, MID, HIGH }
val LocalDeviceRenderTier = staticCompositionLocalOf { DeviceRenderTier.MID } // default desktop/iOS
// androidMain — calculado UNA vez, provisto en la raíz de MindOSApp (junto a LocalFavoriteAccent, App.kt:134):
//   LOW  si ActivityManager.isLowRamDevice || memoryClass <= 128 || SDK_INT < 26
//   HIGH si memoryClass >= 256 && SDK_INT >= 31
//   else MID
// Degradación opcional en runtime: alimentar frameDropFraction de FrameStatsCalculator (ya existe) para
// demotar MID→LOW ante jank sostenido — sin código de medición nuevo.
```
Consumidores (todos identificados estáticamente): **C5** `FocusAtmosphere` (LOW → gradiente estático; desactivado siempre durante la ventana de medición RT en cualquier tier), **E1** los tres `Modifier.blur` (LOW → sin blur). **Invariante de integridad:** el tier degrada solo lo decorativo; la exclusión de señal RT-crítica con timebase no autoritativo ya la impone `SessionQualityEvaluator` y **no** la toca esta propuesta. Un tier bajo mantiene la sesión jugable pero **excluida del perfil** (regla ya existente).

### Dimensión F — Deuda transversal que se vuelve CRITICAL a 1M

| id | sev | fuente | clase | Impacto a 1M | Owner (doc) |
|---|---|---|---|---|---|
| **F1** | HIGH | `EXERCISE_QUALITY_CONTRACT.md §21.2`; gateway tras flag OFF (`GitLiveFocusSessionFirestoreGateway`) | CONFIRMED | Sin **Firestore-sync + outbox durable** para summaries/runs: MindBoost es local-only → un teléfono nuevo o reinstalación = **pérdida total del perfil cognitivo**, sin continuidad cross-device. Liga con B1/B4. | data-sync-dev |
| **F2** | HIGH | `EXERCISE_QUALITY_CONTRACT.md §21.5` | CONFIRMED | Sin **analytics sink** (`exercise_started/completed/abandoned`, `score_updated`, `session_save_failed`): a 1M estás **ciego** a fallos de guardado y tasas de abandono → no puedes detectar las pérdidas de B1/B3/B5 en campo. | data-analytics |
| **F3** | MEDIUM | `EXERCISE_QUALITY_CONTRACT.md §21.3-21.4` | CONFIRMED | Recovery de payload por process-death (= B1) y **lifecycle/background guard** no instrumentado → app en background durante un trial temporizado puede corromper la medición. | kmp-architect |
| **F4** | MEDIUM | `EXERCISE_QUALITY_CONTRACT.md §21.1,21.7` | CONFIRMED | Historial `CognitiveSessionRun` unificado parcial (slices SYSTEM-1 hechos, pero el run de E8 sin consumidor) + perfil de bloque/global + calibración de 5 sesiones aún abiertos. | kmp-architect + assessment-diagnostics |

---

## 3. Backlog priorizado de remediación (fases pequeñas, cada una con su gate)

Gate por fase (correr la variante del source-set tocado; en serie, nunca en paralelo sobre el mismo worktree): **`:shared:desktopTest`** · **`:shared:detekt`** · **`:androidApp:compileDebugKotlin`** (backend: **`npm --prefix functions run typecheck && npm --prefix functions test`**). iOS: verificación **DIFERIDA** (sin macOS).

**P0 — Rompe a escala / pierde datos (antes de crecer)**
1. **A1 contador distribuido** — sharded `counters/{uid}/shards/{n}` sumado en lectura. Gate backend. *(Firestore write-contention en cuentas virales.)*
2. **A3 borrado paginado + job de fondo** — cursor en `deleteByQuery`/`updateQueryDocs` + Cloud Tasks con checkpoints por lote. Gate backend. *(GDPR erasure atascada/OOM.)*
3. **B1/F3 outbox durable de payload + replay** — journal minimizado del resultado antes de navegar; recovery re-ejecuta el guardado (no solo abandona). Empezar por Decisiones (B3) y E8 (B6). Gate `:shared:desktopTest` + `detekt`. *(Pérdida silenciosa de sesión completada.)*
4. **A2 decidir modelo de feed** — implementar fan-out-on-read con caché de servidor **o** worker de fan-out acotado; prohibir escritura síncrona por-seguidor en `createPost`. Gate backend + decisión de arquitectura documentada. *(El feed home está sin poblar; es el punto de decisión del fan-out.)*

**P1 — Degrada la experiencia a escala**
5. **B2 retry/backoff** — wrapper exp-backoff+jitter (≤3) para lecturas idempotentes + callables idempotency-keyed, en `FirestoreDefaults`. Gate `:shared:desktopTest`.
6. **D1 caché de cripto + valor en `SecureKeyValueStore`** — `SecretKey`/`Cipher` cacheados + caché de valor por key invalidada en escritura. Gate `:shared:desktopTest` (test: 2ª lectura no re-genera clave; `put`/`remove` desalojan) + `detekt` + `:androidApp:compileDebugKotlin`.
7. **C5 congelar `FocusAtmosphere` en trials RT-críticos** — parar la animación durante la ventana de medición. Gate `:shared:desktopTest` + `:androidApp:compileDebugKotlin`.
8. **A4 GC de media paginado + shardeado** — `pageToken` + shard por prefijo de uid + `runWith` memoria/timeout. Gate backend.
9. **A5/A6 config de callables** — región + `minInstances` en económicas + `maxInstances`; subir el techo de voz tras guard de coste. Gate backend.
10. **B4 outbox de intents offline** — cola local idempotency-keyed reproducida al reconectar (wallet/posts/follows/visits). Gate `:shared:desktopTest`.

**P2 — Fluidez / coste / robustez**
11. **D2 dirty-version en brain-profile** — saltar `refresh()` salvo cambio de versión de namespace; recomputar (no parchear). Gate `:shared:desktopTest`.
12. **E1 `DeviceRenderTier`** + gate de blur/animación por tier. Gate `:shared:desktopTest` (tier puro desde `FrameStatsCalculator`) + `:androidApp:compileDebugKotlin`.
13. **C1/C2/C3 arranque** — desbloquear primer frame de la recovery; diferir RemoteConfig + `MediaPreloadManager` a fondo. Gate `:androidApp:compileDebugKotlin` + medición (§5).
14. **A7/A8/A9 coste Firestore** — TTL en `rate_limits`/`profile_visits`; token-bucket Memorystore para `getBalance`; dedup de webhooks por firma+event-id. Gate backend.
15. **F2 analytics sink** + **F1 sync/outbox durable** (proyecto propio, owner data-sync/data-analytics).
16. **B5/B7/B9/B10 + C6/C7/D4** — orphan-GC de adjuntos, write-ahead de reversibilidad, timeout en `card media delete`, flush de autosave, guards de bootstrap Home, decode off-thread. Gates según source-set.

---

## 4. Estimación de coste Firestore por usuario activo/día (orden de magnitud, HYPOTHESIS)

Supuestos **explícitos** (verificar tarifas vigentes): lecturas ≈ $0.06/100k, escrituras ≈ $0.18/100k, invocaciones CF ≈ $0.40/M. MindBoost cognitivo = **0 ops backend** (local-first). Uso/usuario-activo/día asumido: ~50–150 lecturas (apertura + browsing ligero), ~20–60 escrituras efectivas (acciones + la txn extra de rate-limit por acción, A7).

- Lecturas ~10² → `~100 × $6e-7 ≈ $6e-5` · Escrituras ~40 → `~40 × $1.8e-6 ≈ $7e-5` ⇒ **~$1e-4 / usuario activo / día** (orden 10⁻⁴).
- **A 1M DAU:** ops Firestore **~$3k–$9k/mes** (orden 10³–10⁴, dominado por el multiplicador de lecturas + la tasa de escritura de rate-limit A7); invocaciones CF ~900M/mes ≈ **~$360/mes** + compute. **Total ops backend: unidades de miles de $/mes**, EXCLUYENDO bytes+egress de Storage (media), que dependen de la adopción de vídeo y son probablemente la mayor partida.
- **El riesgo dominante a 1M no es el total en $ sino disponibilidad/corrección:** A1 estrangula el follow en cuentas populares y A3/A4 rompen borrado y GC independientemente del presupuesto. Arreglar esos tres antes de optimizar coste.

---

## 5. Mediciones dinámicas que debes correr tú (para confirmar las HYPOTHESIS)

Emulador/dispositivo Android (build debug `com.mindos.app.debug`):

- **Cold-start TTID (C1/C2/C3):** Macrobenchmark `StartupTimingMetric`, `CompilationMode.Partial()`, `StartupMode.COLD`; o rápido:
  `adb shell am start-activity -W -n com.mindos.app.debug/com.mindos.app.MainActivity` → leer `TotalTime`/`WaitTime`.
- **Trabajo en hilo principal en `onCreate` + gate de launch (C1/C2/C3):** Perfetto:
  `python record_android_trace -a com.mindos.app.debug -o trace.pftrace sched gfx view wm` — inspeccionar slices `Application.onCreate`/`MainActivity.onCreate` buscando `Firebase*`, `SimpleCache`/SQLite, y el gap `LaunchLoadingScreen`→primer frame real.
- **Coste de descifrado por lectura (D1/D2):** Android Studio CPU profiler ("Trace System Calls"/Java sample) entrando al brain-profile; contar invocaciones `KeyStore.load` / `Cipher.getInstance` por entrada (esperado ~N por lectura de lista).
- **Jank de animación en la pantalla de medición (C5):** `adb shell dumpsys gfxinfo com.mindos.app.debug framestats` (o `JankStats`) con la pantalla Focus running abierta, atmosphere ON vs un build con ella OFF; correlacionar con `SessionQuality`/`criticalFrameJitter`.
- **Recomposición (C4/C6):** Layout Inspector → recomposition counts en `HomeScreen` y `MindBoostHubScreen` navegando/scrolleando.
- **Offline real (B4/B8):** modo avión en instalación limpia; abrir Feed/Search/Wallet y observar vacío-vs-cacheado y si las escrituras fallan de inmediato.

Backend (staging):

- **A3/A4 riesgo de timeout:** en un proyecto staging, sembrar una cuenta con ~2M edges `follows.followeeId==uid` + ~10k filas de ledger, llamar `deleteAccount`, vigilar `DEADLINE_EXCEEDED`/`status:"running"` atascado en logs de Functions. Para A4, sembrar ~1M objetos bajo `profile_cards_public/*/media/` y correr `reconcileProfileCardMedia`; observar techo de memoria/timeout.
- **A1 contención:** load-test `followUser` contra un mismo `targetUid` a >10 req/s y contar respuestas `ABORTED`/`resource-exhausted`.
- **A10 App Check Firestore:** Consola → App Check → APIs → Cloud Firestore = **Enforced**; o API `GET https://firebaseappcheck.googleapis.com/v1/projects/<PROJECT>/services/firestore.googleapis.com` y comprobar `enforcementMode == "ENFORCED"`.

---

## 6. Cierre

- **Escalabilidad:** el diseño local-first del entrenamiento es un acierto estratégico (coste de servidor ~0 por usuario que entrena). La capa social/wallet **no está lista para 1M** por A1/A2/A3/A4/A5; son arreglos acotados y conocidos, no un rediseño.
- **Fluidez en cualquier teléfono:** buena base (sin cripto en hilo principal), pero falta la ruta de gama baja (E1) y hay que sacar la animación decorativa de la pantalla de medición (C5) — esto último toca la honestidad de medición, no solo el fps.
- **Caché para cargas ultrarrápidas:** D1 (cachear `SecretKey`+valor) y D2 (dirty-version del perfil) son los dos que más "sensación de instantáneo" compran, con invalidación honesta (un cache jamás sirve un score que los gates invalidaron).
- **Resistencia:** timeouts y recovery de lifecycle ya están; faltan recovery de **payload** (B1) y **retry** (B2), y **observabilidad** (F2) para siquiera ver los fallos a escala.

*Read-only. Ningún archivo fuera de este `.md` fue modificado. iOS NO verificado (sin macOS).*
