# Plan de ejecución — Hardening de seguridad MindOS

**Fecha:** 2026-06-10
**Base:** [`AUDITORIA_TECNICA_SEGURIDAD_MINDOS_2026-06-02.md`](AUDITORIA_TECNICA_SEGURIDAD_MINDOS_2026-06-02.md)
**Gate de salida:** [`SECURITY_CHECKLIST_PRODUCTION.md`](SECURITY_CHECKLIST_PRODUCTION.md)
**Naturaleza:** roadmap de tickets. Cada ticket es autocontenido (problema → solución → aceptación → verificación). No reabre la auditoría; la convierte en backlog ejecutable.

> **Criterio no-go del repo:** no publicar producción hasta cerrar **H1–H8** y **M1–M3, M8–M11, M15**.

---

## ⚠️ Estado reconciliado (2026-06-10) — leer primero

Al ejecutar la Fase 0 se descubrió que la **auditoría 2026-06-02 está desactualizada**: el
working tree contenía una gran pasada de hardening sin commitear (suite backend 713 → **754
tests**). Esa base se fijó verificada en verde en el commit **`7ec5bdd`** (functions `tsc` + jest
754/754; `:shared:desktopTest` BUILD SUCCESSFUL). Reconciliación ticket-a-ticket contra el código:

| Ticket | Estado | Evidencia |
|---|---|---|
| SEC-F0 base | ✅ Hecho | commit `7ec5bdd`; ambos gates verdes |
| SEC-H1 media/caché | ✅ Hecho | `visibility="public"` 0 matches; Firestore persiste **`url = storagePath`** (ruta interna), no la download URL (`GlobalFeedRepositoryProvider.android.kt:221`); URL resuelta en lectura con timeout (`resolveStorageUrl`/`AppMediaViewer.kt:163`); `resourcePublic()` eliminado de `storage.rules` |
| SEC-H2 createPost | ✅ Hecho | `functions/src/lib/validators.ts:248` `validateCreatePostInput(ownerUid)`: path exacto `post_attachments/{owner}/{postId}`, rechaza `http://`/`..`/`//` |
| SEC-H3 borrado PII | ✅ Hecho | `accountDeletion.ts`: job durable, Storage bloqueante, pseudonimización de UID, Auth last |
| SEC-H4 iOS prod | ❌ Abierto | `App.kt:44` `PreviewSessionRepository` por defecto; `MainViewController` llama `App()`. **Verificación diferida a macOS (riesgo aceptado)** |
| SEC-H5 release/CI | ✅ Hecho | `release.yml`: preflight (secret scan, audit, backend, rules, KMP+Android, `productionReady`, clean-tree) + actions por SHA + JDK21/Node20 |
| SEC-H6 repos in-memory | ✅ Direccionado | `HomeScreen.kt` sin `InMemory`/`DEFAULT_BASE_COUNT` (0 matches) |
| SEC-H7 router | ✅ Mayormente | `abuseSubject` en `UnderstandingContext`; `forProduction` rechaza wiring no-op; `known-blockers.md` B1 **Closed** |
| SEC-H8 debug appId | 🟡 Parcial | `applicationIdSuffix=".debug"` ✓; falta app Firebase separada debug/staging |
| SEC-M1 profile_visits | ✅ Hecho | `firestore.rules:1531` `allow create,update,delete: if false` |
| SEC-M2 follow request | ✅ Hecho | `socialGraph.ts:60,64,177,181` `not-found`/`failed-precondition` |
| SEC-M3 admin MFA/IP | ✅ Hecho | `adminAuth.ts:114` `ADMIN_SECURITY_CONFIG` (default `hardenAdminByDefault`) + `assertRecent2FA` + IP allowlist + `admin-rbac.test.ts` |
| SEC-M8/M9 rules CI | ✅ Hecho | `package.json`: `firebase-tools 15.19.0` pin, `test:rules` con `--project`, `test:storage-rules:static`; `release.yml` JDK21 |
| SEC-M10 logs PII | ✅ Hecho | `logSafeRef`/`logSafeError` 76× en 11 archivos; sin UID/path crudos |
| SEC-M11 docs | 🟡 Pendiente | sincronizar Data Safety/privacy con account deletion real (tarea documental) |
| SEC-M12 FLAG_SECURE | ✅ Hecho | `PrivacyScreenOverlay` en android/desktop/ios/common + wired en `App.kt` |
| SEC-M14 webhook | ✅ Hecho (prod) | `webhooks.ts`: guardrail `isProductionFirebaseProject()` (sandbox bloqueado en prod), `SANDBOX_WEBHOOK_SECRET`+`timingSafeEqual`, providers reales fail-closed 403. Rate-limit por IP = defensa-en-profundidad solo para staging (prod-disabled), no requisito |
| SEC-M15 reauth | ✅ Hecho | `assertRecentAuthForAccountDeletion` (ventana 10 min) + tests |
| SEC-M4 orden/límite | ✅ Hecho | `GlobalFeedRepositoryProvider.android.kt:64,80` `orderBy(DESCENDING).limit()` |
| SEC-M5 timeouts | ✅ Hecho | mismo archivo: `withFirestoreReadTimeout`/`withFirestoreWriteTimeout(BatchTimeout)`/`withStorageReadTimeout` |
| SEC-M6 audio DM | ✅ Hecho | `DMAudioRecorder.android.kt`: `cacheDir/dm_audio` + `.delete()` tras envío + `deleteStaleAudioFiles` |
| SEC-M7 file picker | ✅ Hecho | `PlatformFilePicker.android.kt:39` `readBytesBounded(uri, maxBytes+1)` |
| SEC-L1–L7 | ⬜ No reverificado | hygiene no bloqueante; L2 (output-metadata) ya gitignored, L7 (clean tree) ✓ |

**Conclusión:** el conjunto bloqueante (H1–H8, M1–M3, M8–M11, M15) está **cerrado salvo H4** —
verificado con evidencia `file:line` arriba. Residuales que **no se pueden cerrar en este entorno**
(requieren macOS / Firebase Console / decisiones de producto), no por falta de trabajo:

- **H4 (iOS prod)** — requiere macOS+Xcode para inyectar repos reales/App Check y *verificar*. Riesgo
  aceptado y documentado; no tocable a ciegas desde Windows.
- **H8 (app Firebase debug separada)** — requiere crear la app en Firebase Console + `google-services.json`
  por buildType. `applicationIdSuffix=".debug"` ya está; el resto es acción de Console.
- **M11 (docs Data Safety/privacy)** — depende de decisiones de producto (Analytics / Advertising ID)
  aún no tomadas.
- **L1 (restricción de API keys)** — configuración en Google Cloud Console, no en el repo.

El resto del documento es el backlog original (foto 2026-06-02); úsese junto a esta tabla.

---

## Mapeo de la petición original → superficie real

La lista de partida era vocabulario de *system design*, no medidas de seguridad. Triaje contra el stack real (KMP + Compose + Firebase + Cloud Functions):

| Término | Superficie real | Tickets |
|---|---|---|
| REVISAR FIREWALL! | Perímetro = App Check + reglas Firestore/Storage + ingress Functions | SEC-H4, SEC-H8, SEC-M1, SEC-M14 |
| Revisar Rate Limit! | `rateLimiter.ts` (callables OK); faltan webhook por-IP, router, admin | SEC-M14, SEC-M3, SEC-H7 |
| Caché!! | Download URLs de larga vida = bearer URLs no revocables | SEC-H1 |
| forward/reverse proxy, LB, CDN | Capa gestionada GCP; extensión = Cloud Armor/WAF + admin IP allowlist | SEC-M3, SEC-M14 (opcional) |
| SQLITE | No usado; persistencia local = `SecureKeyValueStore` cifrado | — (verificación en SEC-V1) |
| ACID / sharding | Transacciones Firestore (ledger) + rate buckets | — (ya correcto) |
| CI/CD / cherry-picking | GitHub Actions + freeze de commit limpio | SEC-H5, SEC-L3, SEC-L7 |
| polling/websockets | Listeners realtime Firestore | SEC-M4 |
| SFTP, k8s, SQS, S3, kafka, rabbitMQ, elasticsearch, DynamoDB, tensorflow, serverless API gateway | **Fuera del stack** | N/A |
| [VAMP] | Placeholder sin resolver | ❓ pendiente de aclarar |

---

## Tabla de priorización

| ID | Severidad | Fase | Bloqueante release | Título |
|---|---|---|---|---|
| SEC-H1 | Alta | 1 | Sí | Media privada tratada como pública + URLs cacheables |
| SEC-H3 | Alta | 1 | Sí | Borrado de cuenta no garantiza purga de PII |
| SEC-M10 | Media | 1 | Sí | Logs backend con identificadores personales crudos |
| SEC-M15 | Media | 1 | Sí | Account deletion sin reauth reciente |
| SEC-M11 | Media | 1 | Sí | Docs privacy/Data Safety inconsistentes |
| SEC-H2 | Alta | 2 | Sí | `createPost` bypassa reglas de attachments |
| SEC-M1 | Media | 2 | Sí | `profile_visits` permite writes directos |
| SEC-M2 | Media | 2 | No | `sendFollowRequest` acepta targets públicos/inexistentes |
| SEC-M14 | Media | 2 | No | Webhook sandbox sin firma/rate-limit por IP |
| SEC-M3 | Media | 2 | Sí | Admin wallet sin MFA/IP enforce |
| SEC-H7 | Alta | 2 | Sí | Router `mindos/`: abuso/authz/telemetría no production-safe |
| SEC-H5 | Alta | 3 | Sí | Release pipeline sin revalidar gates + supply chain débil |
| SEC-H4 | Alta | 3 | Sí | iOS en modo preview/shell, sin Auth/App Check real |
| SEC-H8 | Media/Alta | 3 | Sí | Debug y release comparten applicationId y proyecto |
| SEC-M8 | Media | 3 | Sí | Storage rules sin cobertura CI ejecutable |
| SEC-M9 | Media | 3 | Sí | Rules tests bloqueados (JDK 21) + firebase-tools sin pin |
| SEC-H6 | Alta | 4 | No | Producción usa repos in-memory y contadores fabricados |
| SEC-M4 | Media | 4 | No | Queries limitan antes de ordenar |
| SEC-M5 | Media | 4 | No | Firebase `.await()` sin timeouts en feed/media |
| SEC-M6 | Media | 4 | No | Audio de DM persiste en disco tras envío |
| SEC-M7 | Media | 4 | No | File picker materializa archivo antes de validar tamaño |
| SEC-M12 | Media | 4 | No | Sin `FLAG_SECURE` en pantallas sensibles |
| SEC-L1..L7 | Baja | 4 | No | Higiene (API key restrictions, artefactos, scan repo-wide, gates QA) |

---

## Fase 0 — Congelar y medir (prerequisito)

### SEC-F0 — Base reproducible y gate de reglas operativo
**Fase:** 0 · **Severidad:** Media · **Depende de:** —
**Problema:** la auditoría se hizo sobre worktree sucio (L7) y `npm run test:rules` no corre (Firebase CLI exige JDK 21+, M9).
**Solución:** rama dedicada + commit limpio; fijar JDK 21+ para el emulador; `firebase-tools` como devDependency exacta.
**Aceptación:** desde clean clone, los tres en verde: `npm test --prefix functions`, `:shared:desktopTest`, `npm run test:rules`.
**Verificación:** `git status --short` vacío; CI ejecuta rules tests sin fallo de JDK.

---

## Fase 1 — Privacidad / PII (bloqueante)

### SEC-H1 — Media privada tratada como pública + URLs cacheables de larga vida
**Severidad:** Alta · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `storage.rules` (`canReadOwnerScopedMedia`, `resourcePublic()`); `shared/src/commonMain/.../ProfileVisualRepositories.kt`; `shared/src/androidMain/.../GlobalFeedRepositoryProvider.android.kt`.
**Problema:** uploads setean `visibility=public` y guardan `downloadUrl` persistente. Las download URLs de Firebase son *bearer URLs* de larga vida → media de perfiles privados / posts follower-gated sigue accesible por path o URL filtrada, sin revocación. (Este es el ángulo "Caché": el artefacto cacheable es el agujero.)
**Solución:** no usar `visibility=public` para contenido sujeto a privacidad; sustituir URLs persistentes por paths internos resueltos en lectura contra la privacidad actual, o URLs firmadas de vida corta; rotar/eliminar tokens al cambiar privacidad o borrar cuenta.
**Aceptación:** anónimo, usuario bloqueado y no-follower no pueden leer profile visuals ni post attachments privados; cambiar privacidad invalida el acceso anterior.
**Verificación:** tests negativos de Storage/Firestore por cada rol; test de transición de privacidad que revoque acceso previo.

### SEC-H3 — Borrado de cuenta no garantiza purga de PII
**Severidad:** Alta · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `functions/src/accountDeletion.ts`; `docs/security/ACCOUNT_DELETION_DECISION.md`.
**Problema:** `deleteStoragePrefix` captura errores y solo loguea warning, pero luego borra el Auth user → puede reportar éxito con media residual y sin posibilidad de reintento. El UID sigue como identificador estable en wallet/ledger/purchases.
**Solución:** deletion durable: marcar `deletion_pending` → purgar PII con retries y evidencia → borrar Auth solo al final. Pseudonimizar UID en registros retenidos (`deleted_<hmac(uid)>` o esquema aprobado legalmente).
**Aceptación:** fallo simulado de Storage ⇒ no se reporta deletion completa; tras deletion, no hay UID crudo en colecciones retenidas salvo excepciones documentadas.
**Verificación:** test de fallo de Storage; test post-deletion de ausencia de UID crudo.

### SEC-M15 — Account deletion sin reauth reciente
**Severidad:** Media · **Bloqueante** · **Depende de:** SEC-H3
**Superficie:** `functions/src/accountDeletion.ts` (`deleteAccount`).
**Problema:** exige Auth/App Check/rate limit pero no valida `auth_time` reciente ni nonce de reautenticación para una acción irreversible.
**Solución:** requerir reauth reciente (ventana de `auth_time`) y/o nonce antes de ejecutar.
**Aceptación:** request con token antiguo ⇒ `failed-precondition`/reauth requerido.
**Verificación:** test con `auth_time` fuera de ventana rechazado.

### SEC-M10 — Logs backend con identificadores personales crudos
**Severidad:** Media · **Bloqueante** · **Depende de:** —
**Superficie:** `accountDeletion.ts`, `posts.ts`, `socialGraph.ts`, `functions/src/lib/rateLimiter.ts`.
**Problema:** se loguean UID/postId/pares follow/path completos.
**Solución:** logs estructurados con correlationId + hashes/HMAC no reversibles; no interpolar UID/path completo. (Nota: `rateLimiter.ts` ya usa `logSafeRef`; extender el patrón al resto.)
**Aceptación:** ningún log de prod contiene UID/path crudos.
**Verificación:** grep de los archivos sin interpolación directa de identificadores; revisión de `logSafeRef` aplicado.

### SEC-M11 — Documentación privacy / Data Safety inconsistente
**Severidad:** Media · **Bloqueante** · **Depende de:** SEC-H3
**Superficie:** docs go/no-go, Play Data Safety, App Store privacy.
**Problema:** no reflejan el estado real de account deletion ni Analytics.
**Solución:** actualizar política y declaraciones tras decidir Advertising ID/Analytics y el account deletion real.
**Aceptación:** docs coinciden con comportamiento implementado.
**Verificación:** revisión cruzada doc ↔ código tras cerrar SEC-H3.

---

## Fase 2 — Integridad backend + perímetro ("firewall" / "rate limit")

### SEC-H2 — `createPost` bypassa reglas estrictas de attachments
**Severidad:** Alta · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `functions/src/posts.ts`; `functions/src/lib/validators.ts`; `firestore.rules` (`validGlobalPostData`).
**Problema:** el Admin SDK escribe `attachments` con validación más laxa que las reglas (acepta `http://`, paths genéricos, sin límites por MIME/postType) → un cliente válido crea posts que el cliente directo no podría.
**Solución:** `validateCreatePostInput` debe replicar `validGlobalPostData` con contexto `callerUid/postId`: HTTPS-only, path exacto `post_attachments/{owner}/{postId}/...`, límites por MIME, coherencia `postType`, max sizes, duración audio/video, verificación opcional del objeto Storage.
**Aceptación:** Function rechaza `http://`, path de otro owner, size excesivo, `TEXT` con attachments, MIME/type mismatch.
**Verificación:** tests de Function por cada caso anterior.

### SEC-M1 — `profile_visits` permite writes directos que saltan el callable
**Severidad:** Media · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `firestore.rules` (`profile_visits`); `recordProfileVisit` callable.
**Problema:** las reglas permiten create/update por schema+identidad pero no replican rate limit, App Check, existencia/legibilidad del perfil ni bloqueo mutuo.
**Solución:** cerrar writes directos (solo backend) o replicar en reglas los checks de existencia, bloqueo y visibilidad.
**Aceptación:** write directo que salta el callable es denegado.
**Verificación:** test de reglas negativo + test de callable positivo.

### SEC-M2 — `sendFollowRequest` acepta targets públicos o inexistentes
**Severidad:** Media · **Depende de:** —
**Superficie:** `functions/src/socialGraph.ts` (`sendFollowRequest`).
**Problema:** crea solicitud y actualiza contador sin exigir target existente y privado.
**Solución:** rechazar público con `failed-precondition`, inexistente con `not-found`.
**Aceptación:** request a perfil público/inexistente rechazado con el código correcto.
**Verificación:** tests de Function por cada caso.

### SEC-M14 — Webhook sandbox sin firma / rate-limit por IP
**Severidad:** Media · **Depende de:** —
**Superficie:** `functions/src/webhooks.ts`; flag `SANDBOX_PAYMENTS_ENABLED`.
**Problema:** con el flag `true` acepta eventos sandbox sin secreto/firma ni rate limit por IP. (Es la superficie no autenticada del "rate limit".)
**Solución:** exigir secreto/header sandbox + firma; rate limit por IP; guardrail que bloquee sandbox en proyecto de producción.
**Aceptación:** evento sin firma ⇒ 401; sandbox imposible en proyecto prod.
**Verificación:** tests de webhook con/sin firma y guardrail de proyecto.

### SEC-M3 — Admin wallet sin MFA / IP enforce por defecto
**Severidad:** Media · **Bloqueante** · **Depende de:** —
**Superficie:** `functions/src/adminFunctions.ts`; `ADMIN_SECURITY_CONFIG`.
**Problema:** `require2FA=false`, `requireIpAllowlist=false`, y no se pasa IP → operaciones económicas sin segundo factor ni control de origen (el "forward proxy"/egress admin).
**Solución:** en prod, exigir MFA reciente + IP allowlist/proxy confiable por defecto; propagar IP a los checks.
**Aceptación:** admin sin MFA reciente o IP no permitida ⇒ denegado.
**Verificación:** tests de admin por ambos controles.

### SEC-H7 — Router `mindos/`: abuso / autorización / telemetría no production-safe
**Severidad:** Alta · **Bloqueante** · **Depende de:** —
**Superficie:** `mindos/` (`RouterRequestDto`, `PatternMatcherImpl`, `InMemoryAbuseGuard`, `DefaultActionValidator`, `ActionExecutorFactory.forProduction`, `MindTelemetryEvent`, `TraceRedactor`).
**Problema:** sin `abuseSubject` el rate limiting público queda inoperante; `AllowAll` por defecto valida acciones sensibles; telemetría emite comandos/usernames/IDs crudos; `TraceRedactor` usa FNV 32-bit sin secreto.
**Solución:** pasar `AbuseSubject` desde contexto autenticado del host; prohibir `AllowAll` en factory de producción; convertir detalles a códigos sin valor crudo; HMAC/secret rotation o tokens opacos para correlación.
**Aceptación:** 3+ requests del mismo sujeto con max=2 ⇒ rechazo; `DefaultActionValidator()` sin authorizer real ⇒ falla en prod; eventos sin `alice@example.com` ni input original.
**Verificación:** tests de producción del router por cada punto.

---

## Fase 3 — Release gate / CI-CD + paridad móvil

### SEC-H5 — Release pipeline sin revalidar gates + supply chain débil
**Severidad:** Alta · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `.github/workflows/release.yml`; `functions/package.json`; workflows con Node 18.
**Problema:** release construye/sube AAB sin depender de `kotlin-ci`, `backend-ci`, rules tests, instrumented tests ni `mindos productionReady`; actions por tag (`@v4`) en vez de SHA; Functions en Node 18.
**Solución:** release depende del preflight completo (KMP, Android release, backend, rules, `mindos productionReady`, `npm audit`, secret scan repo-wide); pinear actions por SHA; migrar a Node 20/22.
**Aceptación:** un test roto bloquea el upload a Play.
**Verificación:** `rg 'uses:\s+[^@]+@v[0-9]' .github/workflows` sin resultados; `rg 'node-version.*18|"node": "18"|nodejs18'` sin resultados.

### SEC-H4 — iOS en modo preview/shell, sin Auth/App Check real
**Severidad:** Alta · **Bloqueante** · **Depende de:** —
**Superficie:** `shared/src/iosMain/.../MainViewController.kt`; `App.kt` (`PreviewSessionRepository`); `ProfileRemoteWriteAccessGuard.ios.kt`; `iosApp/Podfile`.
**Problema:** iOS usa repos preview y no instala provider App Check antes de `FirebaseApp.configure()` → paridad de seguridad falsa si se distribuye.
**Solución:** inyectar repos reales iOS, instalar provider App Check, wirear Auth/Crashlytics/Remote Config, sustituir `App()` preview por grafo de producción iOS.
**Aceptación:** login real iOS, token App Check válido, escritura protegida rechazada sin attestation.
**Verificación:** smoke test iOS (verificación diferida a macOS — riesgo explícito).

### SEC-H8 — Debug y release comparten applicationId y proyecto Firebase
**Severidad:** Media/Alta · **Bloqueante** · **Depende de:** —
**Superficie:** `androidApp/build.gradle.kts`; `src/debug/...BuildVariantFirebaseAppCheckInstaller.kt`; `google-services.json`.
**Problema:** `applicationId=com.mindos.app` sin suffix debug + Debug App Check provider → debug puede apuntar a backend de producción.
**Solución:** `applicationIdSuffix=".debug"`, Firebase app separada para debug/staging, `google-services.json` por flavor/buildType.
**Aceptación:** `assembleDebug` produce `com.mindos.app.debug`; debug token no funciona contra prod.
**Verificación:** inspección del APK + prueba de token debug contra prod.

### SEC-M8 — Storage rules sin cobertura CI ejecutable
**Severidad:** Media · **Bloqueante** · **Depende de:** SEC-F0
**Superficie:** `package.json` (`test:rules` excluye Storage por limitación `request.app`).
**Solución:** añadir tests estáticos/negativos viables + modo emulator-only que no relaje producción.
**Aceptación:** ownership/MIME/tamaño/App Check con regresión automatizada.
**Verificación:** CI ejecuta tests de Storage rules.

### SEC-M9 — Rules tests bloqueados (JDK 21) + firebase-tools sin pin
**Severidad:** Media · **Bloqueante** · **Depende de:** SEC-F0 (solapa)
**Superficie:** `package.json`; workflows que instalan `firebase-tools` global.
**Solución:** fijar JDK 21+ para el emulador; `firebase-tools` como devDependency con versión exacta.
**Aceptación:** `npm run test:rules` corre en CI.
**Verificación:** job de CI verde con versión pineada.

---

## Fase 4 — Arquitectura / UX / higiene (no bloqueante)

### SEC-H6 — Producción usa repos in-memory y contadores fabricados
**Severidad:** Alta · **Depende de:** —
**Superficie:** `HomeScreen.kt` (`InMemorySocialGraphComponent`); `RememberMindOSProfileStateHolder.kt`; `InMemoryMindOSProfileRepositories.kt` (`DEFAULT_BASE_COUNT=1_700`).
**Solución:** inyectar repos Firebase desde `AppGraph` u ocultar features hasta tener backend; eliminar contadores base fabricados.
**Aceptación:** test de wiring que falle si prod instancia `InMemory...`; reinicio conserva datos reales.
**Verificación:** test de wiring + persistencia tras reinicio.

### SEC-M4 — Queries limitan antes de ordenar
**Severidad:** Media · **Superficie:** `GlobalFeedRepositoryProvider.android.kt`; `FirebaseSocialInteractionRepository.kt`.
**Solución:** `orderBy(... DESCENDING)` antes de `limit` con índices.
**Verificación:** revisión de queries + índices desplegados.

### SEC-M5 — Firebase `.await()` sin timeouts en feed/media
**Severidad:** Media · **Superficie:** `GlobalFeedRepositoryProvider.android.kt`.
**Solución:** aplicar `withFirestoreReadTimeout`, `withFirestoreWriteTimeout(BatchTimeout)`, `withStorageUploadTimeout`, `withStorageReadTimeout` (patrón ya existente en el repo).
**Verificación:** todas las operaciones suspendibles envueltas.

### SEC-M6 — Audio de DM persiste en disco tras envío
**Severidad:** Media · **Superficie:** `DMAudioRecorder.android.kt` / iOS.
**Solución:** borrar tras envío exitoso o usar cache/no-backup con limpieza.

### SEC-M7 — File picker materializa archivo antes de validar tamaño
**Severidad:** Media · **Superficie:** picker Android (`readBytes()`) / iOS (`NSData`).
**Solución:** validar tamaño por descriptor/metadata antes de materializar; quitar opción "Archivo" si no soportada.

### SEC-M12 — Sin `FLAG_SECURE` en pantallas sensibles
**Severidad:** Media · **Superficie:** `MainActivity.kt`.
**Solución:** protección contextual de screenshots (Android `FLAG_SECURE` + equivalente iOS) en DMs, wallet, auth.

### SEC-V1 — Verificación de datos locales en reposo (tu "SQLITE")
**Severidad:** Baja · **Superficie:** `SecureKeyValueStore` (AES-GCM+Keystore).
**Solución:** confirmar que no existe DB local en claro (no se usa SQLite) y que las prefs cifradas están excluidas de backups (`backup_rules.xml`/`data_extraction_rules.xml`).
**Verificación:** revisión de exclusiones de backup + ausencia de almacenamiento en claro.

### SEC-L1..L7 — Higiene
- **L1** restringir Firebase API keys por package+SHA/bundle ID + APIs permitidas (fuera de repo).
- **L2** desversionar `androidApp/release/output-metadata.json` y limpiar scratch (`CHAT1VS1.kt`, `out_lh.txt`).
- **L3** secret scan repo-wide (`git ls-files` + allowlist de config pública).
- **L4** añadir Kover/Jacoco, visual regression, Macrobenchmark/Baseline Profile, budgets.
- **L5** `PrivacyScreenOverlay.android.kt`: crear executor solo si se usa o cerrarlo siempre con `DisposableEffect`.
- **L6** waveform real o UI neutra (no sintética).
- **L7** congelar branch/commit limpio antes de release.

---

## Definition of Done global
1. Todo el [`SECURITY_CHECKLIST_PRODUCTION.md`](SECURITY_CHECKLIST_PRODUCTION.md) en verde.
2. Cerrados **H1–H8** y **M1–M3, M8–M11, M15**.
3. Si real-money: firma de webhook + admin 2FA + IP allowlist + reconciliación programada + pen-test externo.
4. Gates ejecutados desde clean clone, no desde worktree sucio.
