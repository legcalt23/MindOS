# Auditoria tecnica y de seguridad - MindOS

Fecha: 2026-06-02  
Repositorio auditado: `C:\Users\curro\AndroidStudioProjects\MindOS`  
Modo: read-only sobre codigo; se ejecutaron comprobaciones no destructivas.

## Veredicto ejecutivo

MindOS no esta listo para release de produccion general. La base tiene controles importantes bien encaminados - reglas Firebase extensas, App Check en Android, Crashlytics Android, network security config, backup deshabilitado, tests backend amplios y suites KMP que pasan - pero hay bloqueadores de privacidad, integridad backend, release engineering e iOS.

Los riesgos mas importantes son:

1. Storage marca media de perfil/post como publica y guarda download URLs largas, lo que rompe privacidad/revocacion.
2. `createPost` usa Admin SDK con validacion mas laxa que `firestore.rules`, permitiendo datos que el cliente directo no podria escribir.
3. `deleteAccount` declara purga de PII, pero la limpieza de Storage es best-effort y el ledger/wallet retiene UID estable.
4. iOS sigue conectado al flujo preview, sin Auth/App Check real equivalente a Android.
5. El workflow de release puede publicar sin revalidar todos los gates, usa actions sin SHA y Functions sigue en Node 18.
6. Algunas superficies productivas usan repositorios in-memory o contadores fabricados.
7. El subproyecto `mindos/` compila, pero tiene fallos de abuso/autorizacion/telemetria en el router.

## Evidencia de verificacion

Comandos ejecutados:

| Comando | Resultado |
|---|---|
| `git status --short` | Worktree sucio con muchos cambios en `mindos/` y `AGENTS.md`; auditoria realizada sobre ese estado actual. |
| `node -v` | `v24.14.1` local. CI/Functions declaran Node 18 en varios puntos. |
| `npm run scan:secrets --prefix functions` | Pasa, pero solo escanea 4 targets. |
| `npm audit --package-lock-only --json` en raiz | 0 vulnerabilidades. |
| `npm audit --package-lock-only --json` en `functions/` | 8 vulnerabilidades moderadas, via `firebase-admin`/transitivos. |
| `npm run typecheck --prefix functions` | Pasa. |
| `npm test --prefix functions -- --runInBand` | Pasa: 32 suites, 713 tests. |
| `.\gradlew.bat :shared:desktopTest --no-daemon --stacktrace` | Pasa. |
| `.\gradlew.bat :androidApp:assembleDebug --no-daemon --stacktrace` | Pasa. |
| `.\gradlew.bat productionReady --no-daemon --stacktrace` en `mindos/` | Pasa; advierte de features Gradle deprecated para Gradle 10. |
| `npm run test:rules` | No ejecuta tests: Firebase CLI local exige JDK 21+. |

## Hallazgos criticos y altos

### H1 - Media privada tratada como publica en Firebase Storage

**Severidad:** Alta  
**Area:** Storage, privacidad, revocacion de acceso  
**Evidencia:** `storage.rules` permite `resourcePublic()` por metadata `visibility == "public"` en `canReadOwnerScopedMedia`. Los uploads de perfil en `shared/src/commonMain/.../ProfileVisualRepositories.kt` setean `visibility = "public"`. Los uploads de posts en `shared/src/androidMain/.../GlobalFeedRepositoryProvider.android.kt` tambien setean `visibility = "public"` y guardan `reference.downloadUrl.await().toString()`.

**Impacto:** Media de perfiles privados, posts follower-gated o contenido que deberia ser revocable puede seguir siendo accesible por path o URL si se filtra. El problema es peor con Firebase download URLs porque actuan como bearer URLs de larga vida.

**Correccion recomendada:** No usar `visibility=public` para contenido sujeto a privacidad. Reemplazar URLs persistentes por paths internos y resolver acceso en tiempo de lectura contra privacidad actual, o usar URLs firmadas de vida corta. Rotar/eliminar tokens al cambiar privacidad o borrar cuenta.

**Verificacion:** Tests de Storage/Firestore donde anonimos, usuarios bloqueados y no-followers no pueden leer profile visuals ni post attachments. Test de cambio de privacidad que invalide acceso anterior.

### H2 - `createPost` bypasses reglas estrictas de attachments

**Severidad:** Alta  
**Area:** Cloud Functions, integridad de datos, privacidad  
**Evidencia:** `functions/src/posts.ts` escribe con Admin SDK `attachments: input.attachments`. `functions/src/lib/validators.ts` acepta `http://`, paths genericos y no replica limites por MIME/tipo/postType. `firestore.rules` exige HTTPS, paths exactos `post_attachments/{owner}/{postId}/...`, coherencia tipo/MIME/tamano y reglas de `postType`.

**Impacto:** Un cliente valido puede crear posts con URLs externas/tracking, paths de otro usuario, metadata invalida o posts que el cliente directo no podria crear. Esto rompe la garantia de las reglas y puede crear contenido inconsistente.

**Correccion recomendada:** Hacer que `validateCreatePostInput` replique `validGlobalPostData` con contexto `callerUid/postId`: HTTPS only, path exacto, limites por MIME, coherencia `postType`, max sizes, duracion para audio/video y verificacion opcional del objeto Storage.

**Verificacion:** Tests de Function rechazando `http://`, path de otro owner, size excesivo, `TEXT` con attachments y MIME/type mismatch.

### H3 - Borrado de cuenta no garantiza purga de PII

**Severidad:** Alta  
**Area:** GDPR/store compliance, privacidad  
**Evidencia:** `functions/src/accountDeletion.ts` hace `deleteStoragePrefix`, pero captura errores y solo loguea warning. Luego borra el Auth user. El mismo archivo retiene wallet/ledger/purchases sin pseudonimizar UID. `docs/security/ACCOUNT_DELETION_DECISION.md` documenta retencion anonimizada como objetivo.

**Impacto:** La funcion puede devolver exito aunque media en Storage permanezca. Tras borrar Auth, el usuario no puede reintentar. Ademas, el UID sigue siendo identificador estable en registros financieros/auditoria.

**Correccion recomendada:** Convertir deletion en job durable: marcar cuenta `deletion_pending`, purgar PII con retries y evidencias, solo borrar Auth al final. Pseudonimizar UID en ledger/wallet/purchases con `deleted_<hmac(uid)>` o esquema aprobado legalmente.

**Verificacion:** Test que simule fallo de Storage y pruebe que no se reporta deletion completa. Test post-deletion que no haya raw UID en colecciones retenidas salvo excepciones documentadas.

### H4 - iOS esta en modo preview/shell, no en produccion segura

**Severidad:** Alta  
**Area:** iOS, Auth, App Check, arquitectura multiplataforma  
**Evidencia:** `shared/src/iosMain/.../MainViewController.kt` llama `App()`, que en `App.kt` usa `PreviewSessionRepository(false)`. `ProfileRemoteWriteAccessGuard.ios.kt` devuelve `NoOpProfileRemoteWriteAccessGuard`. `iosApp/Podfile` incluye FirebaseAppCheck, pero no se encontro instalacion de provider App Check antes de `FirebaseApp.configure()`.

**Impacto:** iOS no puede autenticarse ni operar contra backend real como Android. Si se distribuye, la paridad de seguridad y privacidad es falsa.

**Correccion recomendada:** Inyectar repositorios reales iOS, instalar provider App Check, wirear Auth/Crashlytics/Remote Config y sustituir `App()` preview por un grafo de produccion iOS.

**Verificacion:** Smoke test iOS con login real, token App Check valido y escritura protegida rechazada sin attestation.

### H5 - Pipeline de release no revalida todos los gates y usa supply chain debil

**Severidad:** Alta  
**Area:** CI/CD, supply chain, release  
**Evidencia:** `.github/workflows/release.yml` construye y sube AAB por tag/manual sin depender de `kotlin-ci`, `backend-ci`, rules tests, Android instrumented tests ni `mindos productionReady`. Actions usan tags (`actions/*@v4`, `r0adkll/upload-google-play@v1`) en vez de SHA. `functions/package.json` y varios workflows siguen en Node 18.

**Impacto:** Una release puede publicarse sin validar regresiones de reglas/backend/router/UI, y el workflow con secretos de Play/signing ejecuta acciones no pineadas.

**Correccion recomendada:** Hacer que release dependa de preflight completo: KMP, Android release, backend tests, rules, `mindos productionReady`, audit deps y scanner repo-wide. Pinear actions por SHA y migrar Functions/CI a Node 20/22 con runtime explicito.

**Verificacion:** Un test roto debe bloquear upload a Play. `rg 'uses:\s+[^@]+@v[0-9]' .github/workflows` sin resultados. `rg 'node-version.*18|"node": "18"|nodejs18'` sin resultados.

### H6 - Produccion usa repositorios in-memory y contadores fabricados

**Severidad:** Alta  
**Area:** Arquitectura, integridad funcional, UX  
**Evidencia:** `HomeScreen.kt` crea `InMemorySocialGraphComponent`. `RememberMindOSProfileStateHolder.kt` usa por defecto `InMemoryMindOSProfileRepository`. `InMemoryMindOSProfileRepositories.kt` define `DEFAULT_BASE_COUNT = 1_700`.

**Impacto:** Perfil, observadores, presencia y acciones sociales pueden verse reales sin persistir ni reflejar Firebase. Es un fallo de integracion y puede inducir al usuario a creer que hay datos reales.

**Correccion recomendada:** Inyectar repositorios Firebase desde `AppGraph` o ocultar esas features hasta tener backend. Eliminar contadores base fabricados.

**Verificacion:** Test de wiring que falle si produccion instancia `InMemory...`; reinicio de app debe conservar datos reales desde Firebase.

### H7 - Router `mindos/`: abuso/autorizacion/telemetria no son production-safe

**Severidad:** Alta  
**Area:** Command router, abuso, autorizacion, privacidad  
**Evidencia:** `RouterRequestDto` no incluye sujeto de abuso; `PatternMatcherImpl` crea contexto sin `abuseSubject`; `InMemoryAbuseGuard` omite politicas sin key. `DefaultActionValidator` usa `AllowAllAuthorizer` por defecto y `ActionExecutorFactory.forProduction` no lo rechaza. `MindTelemetryEvent.EntityRejected` recibe `detail`, `DefaultActionMatcher` emite `res.reason.detail` y extractores incluyen valores crudos. `TraceRedactor` usa FNV 32-bit sin secreto.

**Impacto:** Rate limiting del router publico puede quedar inoperante, acciones sensibles pueden validarse con `AllowAll`, y telemetria puede exponer comandos/usernames/IDs.

**Correccion recomendada:** Pasar `AbuseSubject` desde contexto autenticado del host, prohibir `AllowAll` en factory de produccion, convertir detalles a codigos sin valor crudo y usar HMAC/secret rotation o tokens opacos para correlacion.

**Verificacion:** Tests de produccion que rechacen 3+ requests del mismo sujeto si max=2, fallen con `DefaultActionValidator()` sin authorizer real y aseguren que eventos no contienen `alice@example.com` ni input original.

### H8 - Debug y release comparten `applicationId` y proyecto Firebase

**Severidad:** Media/Alta  
**Area:** Android, Firebase, entornos  
**Evidencia:** `androidApp/build.gradle.kts` usa `applicationId = "com.mindos.app"` sin suffix debug. `src/debug/...BuildVariantFirebaseAppCheckInstaller.kt` instala `DebugAppCheckProviderFactory`. `google-services.json` solo registra `com.mindos.app`.

**Impacto:** Debug puede apuntar al backend de produccion si el debug token esta registrado. Aumenta riesgo de datos de prueba en prod y abuso con APK debug.

**Correccion recomendada:** `applicationIdSuffix = ".debug"`, Firebase app separada para debug/staging y `google-services.json` por flavor/buildType.

**Verificacion:** `assembleDebug` produce `com.mindos.app.debug`; debug token no funciona contra app/proyecto de produccion.

## Hallazgos medios

### M1 - `profile_visits` permite writes directos que saltan controles del callable

`firestore.rules` permite create/update si schema e identidad del viewer son validos, pero no replica rate limit, App Check, existencia/legibilidad del perfil ni bloqueo mutuo de `recordProfileVisit`. Cerrar writes directos o replicar checks de existencia, bloqueo y visibilidad.

### M2 - `sendFollowRequest` permite targets publicos o inexistentes

`followUser` consulta privacidad del target, pero `sendFollowRequest` crea solicitud y actualiza contador sin exigir que el target exista y sea privado. Rechazar publico con `failed-precondition` e inexistente con `not-found`.

### M3 - Admin wallet sin MFA/IP enforce real por defecto

`ADMIN_SECURITY_CONFIG.require2FA=false` y `requireIpAllowlist=false`; `adminFunctions` no pasa IP. Para operaciones economicas, exigir MFA reciente e IP allowlist/proxy confiable por defecto en prod.

### M4 - Queries limitan antes de ordenar

`GlobalFeedRepositoryProvider.android.kt` y notificaciones en `FirebaseSocialInteractionRepository.kt` hacen `.limit()` y ordenan localmente. Firestore puede devolver un subconjunto arbitrario. Usar `orderBy(... DESCENDING)` antes de `limit` con indices.

### M5 - Firebase `.await()` sin timeouts en feed/media

`GlobalFeedRepositoryProvider.android.kt` usa `.get().await()`, batch `.await()`, Storage upload/download/delete y profile get sin wrappers. Aplicar `withFirestoreReadTimeout`, `withFirestoreWriteTimeout(BatchTimeout)`, `withStorageUploadTimeout` y `withStorageReadTimeout`.

### M6 - Audio privado de DM queda en disco tras envio

`DMAudioRecorder.android.kt` e iOS guardan en `dm_audio`; el flujo de `DMChatScreen` usa bytes y no necesita conservar el archivo. Borrar tras envio exitoso o usar cache/no-backup con limpieza.

### M7 - File picker lee archivos completos antes de validar

Android usa `readBytes()` e iOS carga `NSData` completo. Un archivo grande puede causar OOM antes de que el use case lo rechace. Validar tamaño por descriptor/metadata antes de materializar bytes y quitar la opcion "Archivo" si no esta soportada.

### M8 - Storage rules no estan cubiertas por CI ejecutable

`package.json` excluye Storage rules de `test:rules` por limitacion de `request.app`. Esto deja ownership/MIME/tamano/App Check sin regresion automatizada. Anadir tests estaticos/negativos viables y un modo emulator-only que no relaje produccion.

### M9 - Rules tests bloqueados por entorno y Firebase CLI no esta pineado

`npm run test:rules` fallo porque el Firebase CLI exige JDK 21+. Ademas workflows instalan `firebase-tools` global sin version. Fijar JDK 21+ y `firebase-tools` como devDependency exacta.

### M10 - Logs backend incluyen identificadores personales directos

`accountDeletion.ts`, `posts.ts`, `socialGraph.ts` y `rateLimiter.ts` loguean UID/postId/pares follow/path. Usar logs estructurados con correlationId y hashes/HMAC no reversibles; no interpolar UID/path completo.

### M11 - Documentacion privacy/Data Safety inconsistente

Docs de go/no-go y Data Safety no coinciden con el estado real de account deletion y Analytics. Actualizar politica, Play Data Safety y App Store privacy tras decidir Advertising ID/Analytics y account deletion real.

### M12 - No hay `FLAG_SECURE` ni proteccion equivalente de screenshots

`MainActivity.kt` no activa `FLAG_SECURE`; no se encontro uso. Para DMs, wallet, auth y pantallas sensibles, usar proteccion contextual Android y equivalente iOS para capture/inactive scene.

### M13 - iOS CocoaPods no reproducible

`iosApp/Podfile` no fija versiones Firebase y `Podfile.lock` esta ignorado. Commit del lockfile y `pod install --deployment` en CI.

### M14 - Webhook sandbox publico si se despliega con flag true

`SANDBOX_PAYMENTS_ENABLED=true` permite aceptar eventos sandbox sin secreto/firma/rate limit por IP. Exigir secreto/header sandbox y guardrail que bloquee sandbox en proyecto prod.

### M15 - Account deletion sin reauth reciente

`deleteAccount` exige Auth/App Check/rate limit, pero no valida `auth_time` reciente ni nonce de reautenticacion. Para accion irreversible, requerir reauth reciente.

### M16 - iOS picker de perfil puede perder delegate y video no calcula duracion

`ProfileMediaPlatform.ios.kt` asigna delegate local sin retencion fuerte y crea video con `durationMillis=null`, que la validacion puede rechazar. Retener delegate activo y calcular duracion con AVAsset o deshabilitar video iOS.

### M17 - Navegacion rechazada se reporta como exito en `mindos/`

`REJECT_WHEN_FULL` devuelve `false`, pero `navigate()` traduce no-delegacion a `NavigationOutcome.Success`; el executor audita `EXECUTED`. Devolver `Rejected("stack_full")` y mutar shadow stack solo tras exito o rollback.

### M18 - Idempotencia se salta si `sessionId` falta

`DefaultActionExecutor` crea `IdempotencyKey` solo si `context.sessionId` no esta blank. En produccion, requerir session namespace o default seguro; no bypass silencioso.

### M19 - Metadata de correlacion sin forma ni longitud

`requestId`, `sessionId` y `localeTag` solo validan non-blank en varios modelos/logs. Validar longitud, charset y control chars antes de logs/auditoria.

### M20 - Actions Android instrumented/iOS son manuales o smoke-only

`android-instrumented.yml` es `workflow_dispatch` y `SmokeInstrumentedTest` solo valida package/linkage. `ios-build.yml` es manual y solo compila framework. Activar flows criticos en PR o mantener fuera de release scope.

## Hallazgos bajos / higiene

### L1 - Firebase API keys committeadas requieren restricciones externas

Las API keys en `google-services.json` y `GoogleService-Info.plist` no son secretos por si mismas, pero deben estar restringidas por package+SHA/bundle ID y APIs permitidas. Esto no es verificable desde repo.

### L2 - Artefactos generados y referencias antiguas

`androidApp/release/output-metadata.json` esta trackeado y declara `com.mindos.app`. Hay scratch/log files como `CHAT1VS1.kt` y `out_lh.txt` con referencias antiguas. Desversionar artefactos y limpiar restos o moverlos a `tools/` documentado.

### L3 - Secret scan no es repo-wide

`functions/scripts/scan-secrets.js` solo cubre `functions/src`, `firebase.json`, `firestore.rules`, `storage.rules`. Escanear `git ls-files` con allowlist explicita para Firebase public config.

### L4 - Cobertura, visual regression y performance gates ausentes

Docs declaran coverage/visual regression/performance como no configurados o diferidos. Anadir Kover/Jacoco, screenshots, Macrobenchmark/Baseline Profile y budgets de startup/jank.

### L5 - Privacy overlay puede filtrar executor

`PrivacyScreenOverlay.android.kt` crea executor antes de comprobar permiso y solo lo cierra en la rama con permiso. Crear executor solo si se usa o cerrarlo siempre con `DisposableEffect`.

### L6 - Waveform de feed es sintetica

`GlobalFeedComposerStateHolder.kt` genera waveform sintetica para audio real. Usar amplitud real o mostrar UI neutra sin waveform.

### L7 - Worktree sucio reduce reproducibilidad

La auditoria se hizo sobre un estado con cambios no commiteados. Antes de release, congelar branch/commit limpio y reejecutar gates desde clean clone.

## Controles correctos observados

- `AndroidManifest.xml` usa `allowBackup=false`, `networkSecurityConfig` y solo una activity launcher exportada.
- `network_security_config.xml` deshabilita cleartext y limita trust anchors a sistema.
- Android release instala Play Integrity App Check; debug usa Debug App Check.
- Release Android tiene minify/shrink habilitados.
- `google-services.json` y `GoogleService-Info.plist` apuntan al mismo Firebase project (`mindos-8452f`) y package/bundle `com.mindos.app`.
- Backend Functions pasa typecheck y 713 tests en el entorno local.
- `:shared:desktopTest`, `:androidApp:assembleDebug` y `mindos productionReady` pasan.
- Firestore rules ya incluyen limites `request.query.limit` en varias colecciones sensibles, segun aprendizajes previos.

## Orden recomendado de remediacion

1. **Privacidad/PII:** H1, H3, M10, M11, M15.
2. **Backend integrity:** H2, M1, M2, M3, M14.
3. **Release gate:** H5, M8, M9, M20, L3, L4.
4. **Mobile production readiness:** H4, H8, M6, M7, M12, M13, M16.
5. **Router `mindos/`:** H7, M17, M18, M19.
6. **Arquitectura/UX:** H6, M4, M5, L2, L5, L6.

## Criterio de no-go

No publicar produccion hasta cerrar al menos H1-H8 y M1-M3/M8-M11/M15. Los hallazgos bajos pueden esperar, pero deben entrar en el plan antes de una release publica porque afectan reproducibilidad, privacidad operativa y calidad de deteccion.
