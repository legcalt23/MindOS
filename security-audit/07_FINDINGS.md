# Hallazgos consolidados

Convenciones: `CONFIRMED_SECURITY_DEFECT` significa que el defecto se demostro en fuente o prueba local, no que se haya explotado contra produccion. `LIKELY` requiere validacion dinamica adicional. Cuando no existe una ruta remota cuantificable, no se fuerza un CVSS artificial.

## [MOLVEX-SEC-001] Firebase Auth provisional es alcanzable desde la variante Android release

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P0
- Confianza: Alta
- CVSS y vector: No calculado; es un incumplimiento bloqueante de arquitectura/release, sin endpoint VPS desplegado que permita puntuar explotabilidad remota responsable
- CWE: CWE-1188, CWE-16
- OWASP MASVS/MASTG: MASVS-AUTH, MASVS-RESILIENCE
- OWASP ASVS: V2 Authentication, V14 Configuration
- OWASP API: API2:2023 Broken Authentication
- Plataforma: Android; paridad iOS pendiente
- Entorno: Release
- Componentes afectados: wiring de sesion y login
- Archivos y lineas: `androidApp/src/main/java/com/mindos/app/MainActivity.kt:28,49-58`; `shared/src/commonMain/kotlin/com/mindos/app/core/session/FirebaseSessionRepository.kt`; `docs/context_pack/05_TECHNICAL_CURRENT_STATE.md:447-484`; `docs/context_pack/09_DECISION_LOG.md` decisiones IDN-003/IDN-008
- Introducido en commit, si puede determinarse: UNKNOWN; historial causal no determinado
- Actor atacante: usuario de una build publica o actor que obtenga el artefacto
- Preconditions: build release distribuida con la configuracion actual
- Limite de confianza atravesado: DEV/ALPHA -> PRODUCTION; app -> proveedor de identidad
- Datos o activos afectados: identidad, sesiones, cuentas y autoridad de backend

### Resumen

La ruta release instancia `FirebaseSessionRepository()` sin un gate mecanico `DEV_AUTH_ONLY`, mientras la autoridad de producto reserva Firebase sintetico a desarrollo y exige VPS para lanzamiento.

### Causa raiz

La politica de entorno vive en documentacion y no en el grafo de build ni en el wiring de release.

### Ruta de ataque

Publicar la variante actual -> abrir login -> autenticar mediante el proveedor provisional -> obtener una sesion no perteneciente a la arquitectura de identidad aprobada para produccion.

### Evidencia

Busqueda dirigida no encuentra `DEV_AUTH_ONLY` en codigo; `MainActivity` selecciona Firebase para toda ejecucion que no use el harness de preview.

### Reproduccion segura

Ejecutar `:androidApp:processReleaseMainManifest` y revisar el wiring compilado de `MainActivity`; no se requiere conectar a Firebase ni crear cuentas.

### Resultado observado

No existe una variante release que falle cerrado ante la ausencia del proveedor VPS.

### Impacto tecnico

Mezcla de entornos, contratos de sesion divergentes y posibilidad de publicar autenticacion temporal.

### Impacto para el usuario y el negocio

Identidades no migrables, recuperacion/revocacion no aprobadas y riesgo de tener que invalidar cuentas tras el lanzamiento.

### Analisis de falsos positivos

No es solo codigo muerto: la clase se construye desde el entrypoint Android. No se afirma bypass de Firebase Auth.

### Correccion recomendada

Crear variantes tipadas de identidad; hacer que release solo compile con el adaptador VPS y falle la configuracion si falta. Mantener Firebase sintetico exclusivamente en una variante interna no publicable.

### Parche o archivos modificados

No aplicado. Propuesta en `patches/0001-release-auth-fail-closed.diff`.

### Prueba de regresion

Test de wiring que inspeccione release y falle si referencia `FirebaseSessionRepository`; test negativo de tarea Gradle sin configuracion VPS.

### Riesgo residual

La seguridad real de VPS, migracion y revocacion permanece `NOT_VERIFIED` hasta disponer del backend.

### Estado de revalidacion

ABIERTO; gate release `NO_APTO`.

## [MOLVEX-SEC-002] La callable de borrado confirma exito antes de ejecutar la purga

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P0
- Confianza: Alta
- CVSS y vector: No calculado; defecto de privacidad y semantica asincrona, no vulnerabilidad de red puntuable sin supuestos adicionales
- CWE: CWE-459, CWE-672
- OWASP MASVS/MASTG: MASVS-PRIVACY, MASVS-STORAGE
- OWASP ASVS: V8 Data Protection
- OWASP API: API1:2023, API3:2023
- Plataforma: Android/iOS/backend
- Entorno: Todos los que usen la callable
- Componentes afectados: account deletion, cliente de sesion, cola de Cloud Tasks
- Archivos y lineas: `functions/src/accountDeletion.ts:509-528,542-546`; `functions/src/accountDeletion.test.ts:478-503`; `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: no necesario; usuario que ejerce borrado o fallo operativo posterior
- Preconditions: callable acepta la solicitud y la cola aun no ha completado el worker
- Limite de confianza atravesado: app -> callable -> worker asincrono
- Datos o activos afectados: datos personales, credenciales, estado de borrado y confianza regulatoria

### Resumen

`deleteAccount` encola el trabajo y devuelve `{deleted: true}`. El cliente interpreta finalizacion, cierra sesion y puede retirar su marcador recuperable aunque el worker falle despues.

### Causa raiz

Un booleano terminal representa dos estados distintos: solicitud duradera y purga verificada.

### Ruta de ataque

Solicitar borrado -> recibir `deleted=true` -> interrumpir/fallar worker -> conservar datos remotos sin una señal cliente fiable de pendiente o fallo.

### Evidencia

El test de worker demuestra que la ejecucion real es posterior; la callable construye el resultado terminal inmediatamente tras encolar.

### Reproduccion segura

Usar los mocks existentes: invocar callable, comprobar cero operaciones de purga antes de ejecutar manualmente `processAccountDeletion`.

### Resultado observado

La respuesta afirma borrado con purga aun no iniciada.

### Impacto tecnico

Estado imposible de reconciliar y riesgo de borrado parcial silencioso.

### Impacto para el usuario y el negocio

El usuario recibe una confirmacion materialmente incorrecta; riesgo de incumplir expectativas de GDPR/LOPDGDD y requisitos de stores.

### Analisis de falsos positivos

La cola duradera reduce perdida, pero no convierte una aceptacion en finalizacion. El nombre del campo y el flujo cliente son terminales.

### Correccion recomendada

Devolver `accepted/pending` con `jobId`; exponer estado autenticado y monotono; solo declarar `ERASED` despues de verificaciones por componente y Auth.

### Parche o archivos modificados

No aplicado; contrato propuesto en `patches/README.md`.

### Prueba de regresion

Callable nunca devuelve terminal; worker fallido conserva `RETRY_REQUIRED`; cliente no elimina el journal hasta `ERASED` verificado.

### Riesgo residual

Cloud Tasks/IAM y comportamiento real de Auth no se validaron en proyecto cloud.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-016] Dos gates Linux invocan un wrapper Gradle sin bit ejecutable

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta
- CVSS y vector: No calculado; afecta la ejecucion del control CI, no una interfaz remota de la app
- CWE: CWE-693, CWE-754
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration; OWASP SCVS
- OWASP API: No aplica
- Plataforma: CI Linux
- Entorno: command-router y release
- Componentes afectados: wrapper incluido `mindos/gradlew` y workflows
- Archivos y lineas: modo Git de `mindos/gradlew` = `100644`; `.github/workflows/command-router-voice.yml:64`; `.github/workflows/release.yml:112`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: no necesario; regresion de repositorio/checkout Linux
- Preconditions: runner intenta ejecutar `./gradlew` desde `mindos`
- Limite de confianza atravesado: repositorio -> runner CI -> decision de release
- Datos o activos afectados: integridad de gates command/voice y release

### Resumen

El wrapper raiz es `100755`, pero la copia byte-identica bajo `mindos/` esta rastreada como `100644`; dos jobs Linux la invocan directamente.

### Causa raiz

La duplicacion del wrapper no conserva su metadata ejecutable y no existe un gate que compare modos.

### Ruta de ataque

Checkout Linux -> entrar en `mindos` -> ejecutar `./gradlew productionReady` -> permiso denegado antes de evaluar el control.

### Evidencia

`git ls-files -s gradlew mindos/gradlew` devuelve `100755` y `100644`; ambos workflows usan la segunda ruta por `working-directory`.

### Reproduccion segura

En checkout Linux limpio, ejecutar `test -x mindos/gradlew` o inspeccionar el mode del indice; no ejecutar build.

### Resultado observado

El indice Git no autoriza ejecucion directa de `mindos/gradlew`.

### Impacto tecnico

Gate no ejecutado y posible bloqueo del pipeline. No se observo `continue-on-error`, por lo que el comportamiento esperado es fail-closed/DoS de CI, no release silencioso.

### Impacto para el usuario y el negocio

Releases detenidos o presion operativa para omitir un control critico.

### Analisis de falsos positivos

Un runner podria invocarlo con `bash gradlew` o restaurar permisos, pero los workflows actuales usan `./gradlew` y no contienen ese paso.

### Correccion recomendada

Versionar `mindos/gradlew` como `100755`, evitar wrappers duplicados cuando sea viable y añadir `test -x`/mode gate.

### Parche o archivos modificados

No aplicado; cambiar mode debe hacerse en una tarea DevSecOps revisada.

### Prueba de regresion

`git ls-files -s mindos/gradlew` empieza por `100755` y el job `productionReady` se ejecuta en Ubuntu.

### Riesgo residual

El gate puede fallar posteriormente por Java/Gradle/dependencias; eso debe registrarse por separado.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-003] No existe una autoridad unica de revocacion para toda la persistencia local

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P0
- Confianza: Alta
- CVSS y vector: No calculado; requiere control local, writers concurrentes o restos previos
- CWE: CWE-862, CWE-672, CWE-459
- OWASP MASVS/MASTG: MASVS-STORAGE, MASVS-PRIVACY, MASVS-AUTH
- OWASP ASVS: V4 Access Control, V8 Data Protection
- OWASP API: No aplica directamente
- Plataforma: KMP; impacto principal Android, iOS bloqueado
- Entorno: Todos
- Componentes afectados: sesiones, runs, Decisiones, payload journal y cuarentena
- Archivos y lineas: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/data/SecureDecisionSessionRepository.kt`; `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/payload/SecurePayloadJournalStore.kt`; `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/SecureSessionRecordStore.kt:196-199`; ledger `decisiones-resurrection`, `journal-resurrection-retention`, `read-gating`, `quarantine`, `unified-authority`
- Introducido en commit, si puede determinarse: deuda preexistente, commit no atribuido
- Actor atacante: usuario con almacenamiento local/control del lifecycle; tambien carreras legitimas de proceso
- Preconditions: datos residuales, write atrasado, lectura con revocacion o entrada en cuarentena
- Limite de confianza atravesado: identidad A/B -> almacenamiento local; writer -> autoridad de borrado
- Datos o activos afectados: historial cognitivo, evidencia, consentimiento y datos borrados

### Resumen

Los stores aplican revocacion de forma desigual; algunos writers y readers no la consultan y la cuarentena no es enumerable por sujeto.

### Causa raiz

La autoridad de lifecycle esta distribuida por repositorio en vez de ser una dependencia obligatoria de todo acceso sujeto-especifico.

### Ruta de ataque

Borrar/revocar sujeto -> ejecutar writer atrasado o leer fila residual -> resucitar o exponer datos que debian permanecer inaccesibles.

### Evidencia

Los invariantes abiertos del ledger fueron rederivados en fuente actual; los comentarios de `SecureSessionRecordStore` reconocen el residual de cuarentena.

### Reproduccion segura

Tests desktop A/B con store en memoria: intercalar `deleteAll` con `record/enqueue`; inyectar entrada corrupta, cuarentenarla y ejecutar borrado por sujeto.

### Resultado observado

Existen rutas sin guard y filas que el enumerador de borrado no puede ver.

### Impacto tecnico

Resurreccion, acceso post-revocacion y borrado incompleto.

### Impacto para el usuario y el negocio

Datos de una cuenta eliminada o anterior pueden persistir y quedar disponibles en el dispositivo.

### Analisis de falsos positivos

No depende de activar Focus2: Decisiones y el journal/storage comun forman parte del codigo actual. La alcanzabilidad UI de cada subruta varia y esta documentada.

### Correccion recomendada

Unificar una autoridad triestado fail-closed para todos los reads/writes; tombstone primero; API de cuarentena por sujeto; censo mecanico de callers.

### Parche o archivos modificados

No aplicado; deduplicado contra hallazgos abiertos del ledger.

### Prueba de regresion

Matriz A/B, process-death, writer atrasado, estado de autoridad no disponible y cuarentena por sujeto.

### Riesgo residual

Restauracion de backup y Keychain real requieren dispositivos.

### Estado de revalidacion

ABIERTO; no se cerraron hallazgos heredados.

## [MOLVEX-SEC-004] El borrado local no es total, aislado por sujeto ni durable en todas las plataformas

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P1
- Confianza: Alta
- CVSS y vector: No calculado
- CWE: CWE-459, CWE-703
- OWASP MASVS/MASTG: MASVS-STORAGE, MASVS-PRIVACY
- OWASP ASVS: V8 Data Protection
- OWASP API: No aplica
- Plataforma: Android/iOS/KMP
- Entorno: Todos
- Componentes afectados: `AccountDeletionCoordinator`, secure store Android/iOS
- Archivos y lineas: `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`; `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`; `shared/src/iosMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.ios.kt`; ledger `erase-foreach-abort`, `resumable-deletion`, `MB-FOCUS-0058`, `MB-FOCUS-0059`, `MB-FOCUS-0014`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: no necesario; fallo de I/O, process death o cambio de cuenta
- Preconditions: fallo temprano, `apply()` no persistido, indice Keychain corrupto o namespace compartido
- Limite de confianza atravesado: coordinador comun -> adaptador de almacenamiento de plataforma
- Datos o activos afectados: datos locales de A/B y estado de borrado

### Resumen

El coordinador aborta tras el primer error; Android confirma operaciones asincronas con `apply()`; iOS puede mapear corrupcion a ausencia; existen clears globales sin prueba completa A/B.

### Causa raiz

El contrato comun solo modela ausencia de excepcion, no durabilidad ni verificacion por componente/sujeto.

### Ruta de ataque

Provocar/experimentar fallo de store -> omitir componentes posteriores; o matar proceso tras confirmacion -> conservar datos; o borrar B y afectar namespace de A.

### Evidencia

Inspeccion source-to-sink y hallazgos existentes `MB-FOCUS-0058/0059`; no se necesitó I/O destructivo.

### Reproduccion segura

Fakes que fallan en el primer eraser y canarios fisicos A/B; fault injection de persistencia en emulador/simulador de prueba.

### Resultado observado

El estado reportado no prueba erasure total ni durable.

### Impacto tecnico

Residuo, borrado de otra cuenta y reintentos ambiguos.

### Impacto para el usuario y el negocio

Perdida de datos de A o retencion no solicitada de B.

### Analisis de falsos positivos

La semantica de `apply()` y el fail-fast estan en codigo; el resultado fisico de Keychain queda `NOT_VERIFIED` por falta de macOS.

### Correccion recomendada

Estado durable por componente, best-effort total con agregacion tipada, commit sincronico verificable y aislamiento fisico por sujeto.

### Parche o archivos modificados

No aplicado; depende del rediseño coordinado con SEC-002/003.

### Prueba de regresion

Kill-process, full-storage, Keychain corrupto, A/B conservation y reanudacion monotona.

### Riesgo residual

Los sistemas operativos pueden conservar backups/caches fuera del store hasta verificar artefactos y politicas reales.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-005] Inventario, derechos y alcance de borrado remoto son incompletos

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P1
- Confianza: Media-alta
- CVSS y vector: No calculado; riesgo regulatorio/privacidad
- CWE: CWE-459, CWE-359
- OWASP MASVS/MASTG: MASVS-PRIVACY
- OWASP ASVS: V8 Data Protection
- OWASP API: API3:2023
- Plataforma: Backend, Android/iOS
- Entorno: Todos
- Componentes afectados: account deletion, inventario de datos, export/correction
- Archivos y lineas: `functions/src/accountDeletion.ts:345-483`; colecciones `voice_consents`, `storage_usage`, subcolecciones marketplace y jobs; `docs/security/DATA_INVENTORY.md`; `docs/release/DATA_SAFETY_AND_PRIVACY_LABELS.md`; ledger `MB-FOCUS-0062`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: no necesario; ejercicio de derechos o evolucion de schema
- Preconditions: usuario con datos en colecciones no incluidas o futuras subcolecciones
- Limite de confianza atravesado: app -> Firebase/Storage -> procesadores/retencion
- Datos o activos afectados: consentimiento, uso, medios, perfil, transacciones y metadatos

### Resumen

El purgador enumera muchas colecciones, pero no existe un registro autoritativo completo ni prueba de exportacion, correccion y borrado para todas las clases y subcolecciones.

### Causa raiz

El modelo de datos y el lifecycle de privacidad evolucionan sin un catalogo ejecutable que obligue a registrar cada nueva persistencia.

### Ruta de ataque

Crear dato en una coleccion fuera del catalogo -> borrar cuenta -> el purgador termina sin inspeccionar ese dato.

### Evidencia

Comparacion entre inventario, reglas, Functions y lista de eliminacion; Firestore no borra subcolecciones al borrar el padre.

### Reproduccion segura

Sembrar canarios sinteticos en Emulator Suite para cada ruta y comprobar cero residuos tras el worker.

### Resultado observado

No hay test exhaustivo generado desde un catalogo unico ni flujo E2E de acceso/export/correccion.

### Impacto tecnico

Datos huerfanos y cobertura de lifecycle no demostrable.

### Impacto para el usuario y el negocio

Riesgo de respuestas incompletas a derechos y declaraciones de privacidad inexactas.

### Analisis de falsos positivos

Algunas rutas estan inertes o futuras; se incluyen como bloqueantes pre-activacion, no como fuga activa confirmada.

### Correccion recomendada

Catalogo versionado de datos/retencion/owner/eraser/exporter; test generado que falle al introducir persistencia no registrada.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Canary por coleccion/subcoleccion/prefijo Storage, export y correccion; verificacion cero residuo.

### Riesgo residual

Backups administrados y procesadores externos requieren evidencia operacional.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-006] Medios sensibles sobreviven al lifecycle de sesion o cuenta

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: HIGH
- Prioridad Molvex: P1
- Confianza: Alta para archivos iOS/Android DM; Media para claves URL en Media3
- CVSS y vector: No calculado; requiere acceso al dispositivo o backup/cache
- CWE: CWE-922, CWE-459
- OWASP MASVS/MASTG: MASVS-STORAGE, MASVS-PRIVACY
- OWASP ASVS: V8 Data Protection
- OWASP API: No aplica
- Plataforma: Android/iOS
- Entorno: Todos
- Componentes afectados: grabacion DM, picker temporal, cache Media3
- Archivos y lineas: `shared/src/iosMain/kotlin/com/mindos/app/features/dm/presentation/media/DMAudioRecorder.ios.kt:61-62,113`; `shared/src/androidMain/kotlin/com/mindos/app/features/dm/presentation/media/DMAudioRecorder.android.kt:56-60,169`; `shared/src/androidMain/kotlin/com/mindos/app/features/social/presentation/media/MediaPreloadManager.android.kt:38-64`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: poseedor del dispositivo, app con capacidad forense/root o usuario posterior del mismo perfil
- Preconditions: audio/medio procesado y cleanup no ejecutado; acceso local suficiente
- Limite de confianza atravesado: UI/DM -> filesystem/cache de plataforma
- Datos o activos afectados: voz, nombres/path, URLs de descarga con token y contenido social

### Resumen

iOS escribe audio DM en `Documents/dm_audio` sin borrado terminal; Android retiene temporales hasta TTL/lifecycle; Media3 usa URLs de descarga como URI/cache key y no hay borrado ligado a logout.

### Causa raiz

La propiedad del archivo/cache no esta ligada a una sesion, mensaje o sujeto con cleanup verificable.

### Ruta de ataque

Grabar/preload -> completar o abandonar flujo -> logout/borrar cuenta -> inspeccionar directorio/cache -> recuperar residuo o identificadores de URL.

### Evidencia

Source-to-sink desde creacion hasta ausencia de eraser de cuenta; reglas de backup Android excluyen `dm_audio` pero no eliminan el dato local.

### Reproduccion segura

Usar archivos canario sin contenido real en dispositivo/simulador, completar/cancelar/logout y enumerar solo rutas del sandbox.

### Resultado observado

No existe una postcondicion comun de cero medios por sujeto.

### Impacto tecnico

Retencion de voz y tokens de URL en almacenamiento recuperable.

### Impacto para el usuario y el negocio

Exposicion de comunicaciones y discrepancia con borrado/retencion declarados.

### Analisis de falsos positivos

La exclusión de backup mitiga copia cloud Android, no acceso local. La composicion exacta de cache key Media3 se marca `LIKELY` hasta inspeccion runtime.

### Correccion recomendada

Directorios de cache con proteccion adecuada, owner metadata, borrado en success/cancel/error/logout/account deletion y cache key opaca sin query tokens.

### Parche o archivos modificados

No aplicado; plan en `patches/README.md`.

### Prueba de regresion

Canarios por todos los finales de flujo y cambio A/B; inspeccion de keys sin URL/token.

### Riesgo residual

Copias del receptor y caches del sistema quedan fuera del control de la app.

### Estado de revalidacion

ABIERTO; runtime movil no ejecutado.

## [MOLVEX-SEC-007] Logs locales exponen rutas, UID y mensajes de excepcion

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta
- CVSS y vector: No calculado; canal local dependiente de acceso a logs
- CWE: CWE-532, CWE-209
- OWASP MASVS/MASTG: MASVS-PRIVACY
- OWASP ASVS: V7 Error Handling and Logging, V8 Data Protection
- OWASP API: No aplica
- Plataforma: Android/iOS
- Entorno: Release incluido
- Componentes afectados: media repository, logging de plataforma
- Archivos y lineas: `shared/src/commonMain/kotlin/com/mindos/app/features/profile/card/data/FirebaseProfileCardMediaRepository.kt:45-52,79-86`; `shared/src/commonMain/kotlin/com/mindos/app/core/logging/SafeDebugLogging.kt:23-31`; `shared/src/androidMain/kotlin/com/mindos/app/core/logging/SafeDebugLogging.android.kt:15-21`; `shared/src/iosMain/kotlin/com/mindos/app/core/logging/SafeDebugLogging.ios.kt:8-11`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: app/operador con acceso a logs o artefacto de diagnostico
- Preconditions: operacion de media falla
- Limite de confianza atravesado: SDK/exception -> log de plataforma
- Datos o activos afectados: UID, storage path, nombres y mensajes del SDK

### Resumen

La capa comun construye errores con path/UID y Android conserva hasta 100 caracteres del mensaje en release; iOS imprime mensajes y rutas sin un gate equivalente.

### Causa raiz

La minimizacion se aplica de forma incompleta despues de que datos sensibles ya se incorporaron al mensaje.

### Ruta de ataque

Forzar fallo local/SDK -> capturar log -> correlacionar usuario y objeto.

### Evidencia

Flujo source-to-sink confirmado; backend remoto usa helpers redacted, por lo que no se extiende el finding a Functions.

### Reproduccion segura

Inyectar una excepcion canario con UID/path sinteticos y capturar log release local.

### Resultado observado

La inspeccion estatica confirma que el texto dinamico cruza la frontera source-to-sink. No se ejecuto captura runtime ni se afirma observacion en dispositivo.

### Impacto tecnico

Divulgacion secundaria y aumento del valor forense de logs.

### Impacto para el usuario y el negocio

Correlacion de identidad y actividad; soporte puede recopilar datos no necesarios.

### Analisis de falsos positivos

No se afirma acceso remoto a logcat. El defecto es que el dato se emite, aun si el SO limita lectores.

### Correccion recomendada

Eventos allowlisted sin `Throwable.message`, UID ni path; hash efimero solo si es imprescindible; paridad Android/iOS.

### Parche o archivos modificados

No aplicado. Propuesta en `patches/0002-platform-log-redaction.diff`.

### Prueba de regresion

Canarios que fallen si mensaje, causa, UID o path aparecen en cualquier sink release.

### Riesgo residual

Logs de SDK de terceros requieren captura dinamica.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-008] Telemetria y control de privacidad no estan gobernados por consentimiento/finalidad

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Media-alta
- CVSS y vector: No calculado; privacidad/configuracion
- CWE: CWE-359, CWE-284
- OWASP MASVS/MASTG: MASVS-PRIVACY
- OWASP ASVS: V8 Data Protection
- OWASP API: No aplica
- Plataforma: Android/iOS
- Entorno: Release
- Componentes afectados: Analytics, Performance, Crashlytics, privacy overlay
- Archivos y lineas: `androidApp/build.gradle.kts:164`; manifest fusionado release; `iosApp/Podfile:34`; `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/**`; `shared/src/iosMain/kotlin/com/mindos/app/features/privacy/PrivacyScreenOverlay.ios.kt`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: no aplica; configuracion/SDK de telemetria
- Preconditions: release con SDK activo y usuario que no ha consentido una finalidad opcional
- Limite de confianza atravesado: app -> procesador de telemetria
- Datos o activos afectados: identificadores de instalacion, rendimiento, crashes y uso

### Resumen

Los SDK de Analytics/Performance/Crashlytics estan incluidos, pero no se encontro una autoridad de consentimiento por finalidad que gobierne su activacion; el control visible de privacidad es una superposicion de pantalla y en iOS es no-op.

### Causa raiz

Se mezclan privacidad visual, consentimiento y telemetria bajo un estado ambiguo sin contrato persistente multiplataforma.

### Ruta de ataque

Instalar/abrir release -> inicializar SDK -> emitir datos antes de una decision o pese a desactivar un control que el usuario interpreta como privacidad.

### Evidencia

Dependencias/permisos presentes y ausencia de llamadas de consentimiento/collection control en el censo dirigido.

### Reproduccion segura

Build local con endpoints bloqueados y proxy de prueba; alternar preferencias y comprobar que ningun request se intenta antes de consentimiento.

### Resultado observado

No existe prueba de red que demuestre la propiedad anterior; iOS no persiste/aplica el overlay.

### Impacto tecnico

Procesamiento no gobernado y divergencia Android/iOS.

### Impacto para el usuario y el negocio

Declaraciones de privacidad potencialmente inexactas y consentimiento no efectivo.

### Analisis de falsos positivos

Incluir SDK no prueba por si solo que envie eventos; por ello la transmision real queda `NOT_VERIFIED`. El defecto confirmado es la ausencia del control y de evidencia.

### Correccion recomendada

Consent authority por finalidad, default-off donde corresponda, inicializacion tardia, revocacion efectiva, Data Safety/Privacy Manifest sincronizados y test de red.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Matriz fresh install/accept/deny/revoke en ambas plataformas con canario de red y estado persistente.

### Riesgo residual

Comportamiento interno de SDK y consola Firebase permanece `NOT_VERIFIED`.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-009] `loadHomeFeed` permite amplificacion de lecturas sin cuota explicita

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta
- CVSS y vector: CVSS:4.0/AV:N/AC:L/AT:N/PR:L/UI:N/VC:N/VI:N/VA:L/SC:N/SI:N/SA:N, puntuacion no fijada por falta de capacidad/cuotas reales
- CWE: CWE-770
- OWASP MASVS/MASTG: MASVS-NETWORK
- OWASP ASVS: V13 API and Web Service
- OWASP API: API4:2023 Unrestricted Resource Consumption
- Plataforma: Backend
- Entorno: Firebase Functions
- Componentes afectados: home feed y rate limiter
- Archivos y lineas: `functions/src/homeFeed.ts:3,182-204`; `functions/src/lib/rateLimiter.ts`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: usuario autenticado
- Preconditions: sesion valida y App Check aceptado
- Limite de confianza atravesado: cliente -> callable -> Firestore
- Datos o activos afectados: disponibilidad, cuota y coste

### Resumen

Una invocacion realiza consulta de follows y multiples consultas de posts, pero no llama a un limitador explicito.

### Causa raiz

El endpoint de lectura quedo fuera del registro de acciones limitadas.

### Ruta de ataque

Repetir llamadas autenticadas de bajo coste cliente -> multiplicar operaciones Firestore/backend.

### Evidencia

Source review y ausencia de `checkRateLimit*` en `homeFeed.ts`.

### Reproduccion segura

Unit test con contadores de mocks para demostrar fan-out y que la segunda llamada no se rechaza; no hacer carga.

### Resultado observado

Cada llamada completa ejecuta el fan-out permitido.

### Impacto tecnico

Cost amplification y degradacion localizada.

### Impacto para el usuario y el negocio

Coste inesperado y feed lento/no disponible.

### Analisis de falsos positivos

Cuotas globales de Firebase pueden limitar daño, pero no sustituyen una cuota por sujeto/accion. No se hizo stress test.

### Correccion recomendada

Registro fail-closed de acciones, limite por UID/IP razonable, cache acotada y presupuesto maximo de lecturas por respuesta.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Test del limitador, accion desconocida denegada y presupuesto de queries fijo.

### Riesgo residual

Los limites deben calibrarse para no bloquear trafico legitimo.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-010] El pseudonimo de borrado reutiliza secretos de logging sin separacion de dominio

- Estado: DEFENSE_IN_DEPTH
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta en fuente; explotabilidad no demostrada
- CVSS y vector: No aplica responsablemente
- CWE: CWE-323, CWE-321
- OWASP MASVS/MASTG: MASVS-CRYPTO
- OWASP ASVS: V6 Cryptography
- OWASP API: No aplica
- Plataforma: Backend
- Entorno: Produccion futura
- Componentes afectados: pseudonimizacion de account deletion
- Archivos y lineas: `functions/src/accountDeletion.ts:59-79,345-346`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: operador/servicio con acceso a uno de los secretos o correlaciones
- Preconditions: fallback a secreto de logging y acceso a material/contexto relacionado
- Limite de confianza atravesado: secretos de observabilidad -> identidad retenida
- Datos o activos afectados: pseudonimos de registros financieros y jobs de borrado

### Resumen

La clave HMAC dedicada puede caer a secretos de redaccion/logging; no se observa version/purpose binding en el derivado persistido.

### Causa raiz

Compatibilidad operativa priorizada sobre separacion criptografica de proposito y rotacion.

### Ruta de ataque

Comprometer/reutilizar secreto de logging -> derivar/correlacionar identificadores de borrado bajo el mismo material.

### Evidencia

Cadena de fallback explicita en fuente; produccion falla cerrado solo si ningun secreto existe.

### Reproduccion segura

Test unitario con secretos sinteticos iguales/diferentes y dominios versionados.

### Resultado observado

La misma clave puede servir a finalidades distintas.

### Impacto tecnico

Amplia el blast radius de una clave y dificulta rotacion/migracion.

### Impacto para el usuario y el negocio

Debilita la pseudonimizacion de registros retenidos.

### Analisis de falsos positivos

HMAC sigue siendo una primitiva adecuada; no se encontro preimagen ni secreto expuesto. Se clasifica defensa en profundidad.

### Correccion recomendada

Secreto dedicado en Secret Manager/KMS, derivacion con contexto/version y plan de rotacion dual-read.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Produccion rechaza fallback; dominios distintos producen salidas no correlacionables; migracion versionada.

### Riesgo residual

IAM, rotacion y residencia del secreto quedan `NOT_VERIFIED` en cloud.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-011] Gradle carece de locks y verificacion criptografica del grafo

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Alta
- CVSS y vector: No calculado; riesgo de supply chain dependiente de compromiso externo
- CWE: CWE-494, CWE-829
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration
- OWASP API: No aplica
- Plataforma: KMP/Android/Desktop
- Entorno: Build/CI/release
- Componentes afectados: Gradle dependencies/plugins
- Archivos y lineas: `settings.gradle.kts`; `build.gradle.kts`; `gradle/libs.versions.toml`; ausencia de `gradle/verification-metadata.xml` y lockfiles; ledger `MB-SUPPLY-0001`
- Introducido en commit, si puede determinarse: preexistente
- Actor atacante: repositorio/dependencia/plugin comprometido
- Preconditions: resolucion de artefacto alterado o transitive drift
- Limite de confianza atravesado: CI/desarrollador -> repositorios de artefactos
- Datos o activos afectados: codigo ejecutado en build, binarios, firma y release

### Resumen

Las versiones directas estan fijadas, pero los transitivos no estan bloqueados ni verificados por checksum/firma en modo estricto.

### Causa raiz

No se cerro el grafo reproducible como control de release.

### Ruta de ataque

Artefacto transitivo sustituido/actualizado -> resolucion CI -> codigo malicioso en build o app.

### Evidencia

Inventario local y ausencia de metadata/locks rastreados.

### Reproduccion segura

Ejecutar verificacion estatica de archivos requeridos; no alterar repositorios ni descargar dependencias.

### Resultado observado

El build confia en el repositorio y cache sin una allowlist criptografica del proyecto.

### Impacto tecnico

Build no reproducible ni verificable frente a sustitucion.

### Impacto para el usuario y el negocio

Compromiso potencial del artefacto firmado y dificultad forense.

### Analisis de falsos positivos

HTTPS y pins de versiones reducen riesgo, pero no prueban identidad del transitive recibido.

### Correccion recomendada

Generar/revisar locks y verification metadata, strict mode, repositorios allowlisted y CI limpio.

### Parche o archivos modificados

No aplicado; requiere resolucion online aprobada y revision humana de hashes.

### Prueba de regresion

Mutar un checksum en fixture/cache aislado y comprobar fallo; regeneracion de SBOM byte-estable.

### Riesgo residual

Un maintainer legitimo comprometido puede publicar una version cuyo hash se apruebe indebidamente.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-012] El grafo y artefacto iOS no estan bloqueados ni probados por CI

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Alta
- CVSS y vector: No calculado
- CWE: CWE-494, CWE-1104
- OWASP MASVS/MASTG: MASVS-CODE, MASVS-PLATFORM
- OWASP ASVS: V14 Configuration
- OWASP API: No aplica
- Plataforma: iOS
- Entorno: Build/CI/release
- Componentes afectados: CocoaPods, Xcode app, privacy manifest/entitlements
- Archivos y lineas: `iosApp/Podfile`; ausencia de `Podfile.lock`; `.github/workflows/ios-build.yml`; ledger `MB-SUPPLY-0002`, `MB-FOCUS-0046`
- Introducido en commit, si puede determinarse: preexistente
- Actor atacante: dependencia comprometida o regresion de plataforma
- Preconditions: resolver pods/build iOS fuera del gate actual
- Limite de confianza atravesado: CI -> CocoaPods/Xcode -> artefacto iOS
- Datos o activos afectados: app iOS, SDK Firebase, privacidad, firma

### Resumen

Siete pods Firebase no tienen versiones/lock; CI no observa todo `iosApp/**` ni construye/inspecciona la app real.

### Causa raiz

Se valida el framework KMP, no el producto iOS integrado.

### Ruta de ataque

Cambio en Pod/app/entitlement -> CI no ejecuta gate relevante -> artefacto divergente o dependencia inesperada llega a release.

### Evidencia

Inventario de Podfile/workflow y ausencia de lock/project build verificable en este host.

### Reproduccion segura

Test estatico de paths del workflow y archivos de lock; build en runner macOS aislado.

### Resultado observado

No hay evidencia de app/archive, firma, entitlements o Privacy Manifest final.

### Impacto tecnico

Paridad de controles desconocida y supply chain no reproducible.

### Impacto para el usuario y el negocio

Riesgo de release iOS con permisos, logs, almacenamiento o SDK diferentes a lo revisado.

### Analisis de falsos positivos

El entrypoint iOS esta bloqueado, lo que limita alcance actual; sigue siendo bloqueo de release, no exploit activo.

### Correccion recomendada

Lock de pods, fuente unica de versiones, CI por SHA que compile app simulator/device, archive y extraiga plist/entitlements/privacy manifest.

### Parche o archivos modificados

No aplicado por ausencia de macOS y resolucion CocoaPods autorizada.

### Prueba de regresion

Cambio en cualquier `iosApp/**` dispara CI; app y framework usan mismo SHA/versions; inspeccion de archive.

### Riesgo residual

Provisioning y configuracion App Store requieren cuenta/operador autorizado.

### Estado de revalidacion

ABIERTO; iOS `NOT_VERIFIED` dinamicamente.

## [MOLVEX-SEC-013] Release no genera SBOM, digest ni atestacion ligados al artefacto

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Alta
- CVSS y vector: No calculado
- CWE: CWE-494
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration; OWASP SCVS
- OWASP API: No aplica
- Plataforma: CI/CD, Android/iOS/backend
- Entorno: Release
- Componentes afectados: workflows y artefactos
- Archivos y lineas: `.github/workflows/release.yml`; `.github/workflows/kotlin-ci.yml`; `docs/release/RELEASE_EVIDENCE.json`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: colaborador/runner/registro comprometido
- Preconditions: artefacto construido/publicado sin cadena verificable
- Limite de confianza atravesado: source -> CI -> artefacto -> store
- Datos o activos afectados: integridad del release y capacidad de respuesta

### Resumen

Los actions estan fijados por SHA y con permisos reducidos, pero falta una atestacion que una commit, grafo, tests, SBOM, digest y firma al binario entregado.

### Causa raiz

El gate valida pasos, no la identidad final del artefacto.

### Ruta de ataque

Sustituir/reconstruir artefacto fuera del job validado -> presentar evidencia de otro build.

### Evidencia

No se encontraron SBOM ni provenance/attestation de release; el evidence gate actual permanece `NO_GO` por campos humanos pendientes.

### Reproduccion segura

Inspeccion estatica de workflows y artefactos rastreados.

### Resultado observado

No hay digest verificable de AAB/archive enlazado a evidencia.

### Impacto tecnico

Provenance insuficiente y respuesta a CVE lenta.

### Impacto para el usuario y el negocio

No puede demostrarse que el binario distribuido corresponde al codigo auditado.

### Analisis de falsos positivos

La fijacion 48/48 de Actions y `persist-credentials:false` son controles positivos, pero no cubren provenance del binario.

### Correccion recomendada

SBOM CycloneDX/SPDX por artefacto, digest SHA-256, atestacion SLSA compatible, firma y verificacion antes de publicar.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

El gate rechaza evidencia cuyo commit/digest/SBOM no coincide con el artefacto.

### Riesgo residual

La atestacion hereda la confianza del runner y de la identidad de firma.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-014] El parser del gate npm acepta evidencia vacia o de error

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta
- CVSS y vector: No calculado
- CWE: CWE-754, CWE-252
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration; OWASP SCVS
- OWASP API: No aplica
- Plataforma: CI/backend tooling
- Entorno: CI
- Componentes afectados: npm audit parser
- Archivos y lineas: `functions/package.json:8`; `functions/scripts/audit-threshold.js`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: fallo de registry/tool o manipulacion de su salida
- Preconditions: audit devuelve JSON vacio, incompleto o con campo de error
- Limite de confianza atravesado: CI -> npm advisory service -> decision de seguridad
- Datos o activos afectados: decision de release y cobertura de vulnerabilidades

### Resumen

El pipeline canaliza JSON al parser; entradas `{}`, vacias o con error no tienen un schema obligatorio que fuerce estado `UNKNOWN` bloqueante.

### Causa raiz

El parser interpreta ausencia del mapa `vulnerabilities` como ausencia de vulnerabilidades, sin distinguir una respuesta valida de una respuesta fallida.

### Ruta de ataque

Fallo/alteracion de audit -> JSON no confiable -> parser devuelve exito -> pipeline registra una ausencia de vulnerabilidades no demostrada.

### Evidencia

Revision y reproduccion independiente del parser: `{}` y un error de registry retornan codigo 0; una fixture High se rechaza. El audit offline dio cero advisories solo en cache y no prueba vigencia.

### Reproduccion segura

Fixtures de JSON vacio, truncado, error de registry y advisory High; ejecutar parser sin red.

### Resultado observado

`unknown` puede convertirse en exit 0.

### Impacto tecnico

Falsa ausencia de vulnerabilidades.

### Impacto para el usuario y el negocio

Release basado en evidencia incompleta.

### Analisis de falsos positivos

No se encontro paquete malicioso ni advisory actual; el finding trata el control fail-open/provenance, no una CVE concreta.

### Correccion recomendada

Parser estricto con schema/estado `UNKNOWN` bloqueante y preservacion explicita del fallo del comando.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Mutation tests para salida invalida, vacia y fallo de red.

### Riesgo residual

La base de advisories necesita snapshot actual autorizado.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-017] `agent-browser` ejecuta bootstrap no atestado y exige un Node distinto al CI

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta en metadata; no se encontro paquete malicioso
- CVSS y vector: No calculado
- CWE: CWE-829, CWE-494
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration; OWASP SCVS
- OWASP API: No aplica
- Plataforma: developer/CI tooling
- Entorno: install/bootstrap
- Componentes afectados: dependencia raiz `agent-browser`
- Archivos y lineas: `package.json:29`; entrada `agent-browser` del `package-lock.json`
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: proveedor/paquete/binario comprometido
- Preconditions: ejecutar lifecycle de instalacion o fallback de descarga
- Limite de confianza atravesado: npm package -> ejecutable descargado -> host de build
- Datos o activos afectados: runner, workspace y evidencia de CI

### Resumen

La dependencia usa rango caret, tiene install script, declara Node 24+ mientras jobs raiz usan Node 20 y puede materializar un ejecutable sin checksum allowlisted por el proyecto.

### Causa raiz

Tooling interactivo se instalo en el grafo raiz sin una frontera de runtime/provenance propia.

### Ruta de ataque

`npm ci` ejecuta lifecycle -> script obtiene/ejecuta binario -> codigo de terceros opera con permisos del runner.

### Evidencia

Metadata del manifest/lock y revision estatica del paquete local; no se ejecuto el install script ni se uso red.

### Reproduccion segura

Inspeccionar `hasInstallScript`, `engines` y resolucion en lock; simular bootstrap offline con binario canario y checksum.

### Resultado observado

No existe checksum/provenance del ejecutable fijado por el repositorio y los runtimes declarados divergen.

### Impacto tecnico

Mayor superficie supply chain y builds inconsistentes.

### Impacto para el usuario y el negocio

Compromiso potencial del runner o gates que no reproducen el entorno local.

### Analisis de falsos positivos

No se encontro codigo malicioso ni sustitucion real. El finding es el control de procedencia/runtime ausente.

### Correccion recomendada

Aislar tooling, fijar version exacta, deshabilitar lifecycle por defecto, allowlist checksum/provenance del binario y alinear Node.

### Parche o archivos modificados

No aplicado.

### Prueba de regresion

Bootstrap offline solo acepta binario pre-verificado; CI usa runtime compatible y no ejecuta lifecycle inesperado.

### Riesgo residual

Un maintainer legitimo comprometido sigue requiriendo controles de actualizacion/review.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-015] El harness de preview puede seleccionarse mediante intent externo en `main`

- Estado: DEFENSE_IN_DEPTH
- Severidad: LOW
- Prioridad Molvex: P3
- Confianza: Alta
- CVSS y vector: No aplica; no se demostro bypass de datos/auth real
- CWE: CWE-489
- OWASP MASVS/MASTG: MASVS-RESILIENCE
- OWASP ASVS: V14 Configuration
- OWASP API: No aplica
- Plataforma: Android
- Entorno: Main/release
- Componentes afectados: MainActivity y auth preview
- Archivos y lineas: `androidApp/src/main/java/com/mindos/app/MainActivity.kt:49-58`; manifest fusionado release (launcher exportado)
- Introducido en commit, si puede determinarse: UNKNOWN
- Actor atacante: app local capaz de lanzar la activity con extras
- Preconditions: activity exportada y extra `use_preview_auth=true`
- Limite de confianza atravesado: intent externo -> wiring interno de preview
- Datos o activos afectados: superficie de UI/test; no datos reales demostrados

### Resumen

Un booleano controlado por intent selecciona repositorios/harness de preview compilados en el entrypoint principal.

### Causa raiz

El mecanismo de tests visuales se incluyo en `main` y se selecciona en runtime en vez de mediante source set/build type.

### Ruta de ataque

App local inicia launcher con extra -> actividad construye el grafo preview.

### Evidencia

Rama explicita en `MainActivity`; inspeccion del harness indica datos simulados, sin acceso Firebase real.

### Reproduccion segura

En emulador local: `am start` con el extra y observar UI preview, sin credenciales ni red.

### Resultado observado

El grafo alternativo es seleccionable externamente.

### Impacto tecnico

Superficie de debug innecesaria y posibles futuras regresiones si el harness adquiere capacidades.

### Impacto para el usuario y el negocio

Confusion de estado o capturas falsas; impacto actual bajo.

### Analisis de falsos positivos

No es auth bypass porque el harness actual es aislado. Se clasifica defensa en profundidad/P3.

### Correccion recomendada

Mover a `debug`/androidTest, o exigir componente no exportado y firma de test; eliminar el extra de release.

### Parche o archivos modificados

No aplicado. Propuesta en `patches/0003-preview-harness-debug-only.diff`.

### Prueba de regresion

El binario release no contiene la cadena/extra ni clases de harness.

### Riesgo residual

Otros hooks de debug deben someterse al mismo censo.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-018] El cifrado local no vincula ciphertext con su identidad logica

- Estado: LIKELY
- Severidad: MEDIUM
- Prioridad Molvex: P2
- Confianza: Alta en fuente; explotabilidad condicionada a control local privilegiado o bug de routing
- CVSS y vector: No calculado; el sandbox impide a una app ordinaria realizar el swap
- CWE: CWE-323, CWE-345
- OWASP MASVS/MASTG: MASVS-CRYPTO, MASVS-STORAGE
- OWASP ASVS: V6 Cryptography, V8 Data Protection
- OWASP API: No aplica
- Plataforma: Android primero; contrato KMP comun
- Entorno: Todos
- Componentes afectados: secure store, consentimientos, restore y payload journal
- Archivos y lineas: `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`; DTOs/restore comunes; ledger `MB-FOCUS-0060`
- Introducido en commit, si puede determinarse: preexistente; ya registrado 2026-08-03
- Actor atacante: actor con root/instrumentacion o componente con capacidad de mover ciphertext
- Preconditions: ciphertext AES-GCM valido bajo la misma clave de app y capacidad de trasplantarlo entre claves logicas
- Limite de confianza atravesado: almacenamiento fisico -> logical key/owner/type
- Datos o activos afectados: consentimiento, evidencia, sesiones y atribucion por sujeto

### Resumen

AES-GCM protege confidencialidad/integridad del blob, pero no usa AAD derivada de namespace, key, sujeto, tipo y version; algunos decoders no revalidan toda la identidad solicitada.

### Causa raiz

El envelope criptografico autentica bytes, no su contexto logico.

### Ruta de ataque

Copiar ciphertext valido A -> key B dentro del mismo dominio de clave -> descifrado valido -> DTO potencialmente atribuido al contexto B.

### Evidencia

Fuente actual confirma ausencia de AAD contextual; no se ejecuto swap en dispositivo. Es el mismo invariante que `MB-FOCUS-0060`, no un duplicado nuevo.

### Reproduccion segura

Store de prueba con dos sujetos/keys/scopes: mover ciphertext sin descifrar y exigir rechazo tipado.

### Resultado observado

El contrato actual no expresa una propiedad mecanica que obligue a rechazar el trasplante.

### Impacto tecnico

Misatribucion o replay local bajo capacidades elevadas.

### Impacto para el usuario y el negocio

Integridad de consentimiento/evidencia debilitada en dispositivos comprometidos o ante bugs de routing.

### Analisis de falsos positivos

No es acceso remoto ni bypass del Keystore. Requiere capacidad local elevada; por eso se clasifica `LIKELY`/P2.

### Correccion recomendada

Envelope versionado con AAD `namespace + logicalKey + subject + type + schemaVersion` y validacion post-decode.

### Parche o archivos modificados

No aplicado; requiere migracion dual-read y rollback.

### Prueba de regresion

Matriz swap por sujeto/scope/session/revision debe fallar cerrado; legacy migra una sola vez.

### Riesgo residual

Root puede alterar tambien codigo/memoria; el control protege errores y manipulacion de datos en reposo, no un runtime totalmente comprometido.

### Estado de revalidacion

ABIERTO.

## [MOLVEX-SEC-019] Focus2 restore acepta estados temporales parcialmente validados

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Alta en fuente; ruta productiva inerte/no montada
- CVSS y vector: No calculado; no existe actualmente una ruta productiva activa
- CWE: CWE-20, CWE-190
- OWASP MASVS/MASTG: MASVS-STORAGE, MASVS-RESILIENCE
- OWASP ASVS: V5 Validation, V8 Data Protection
- OWASP API: No aplica
- Plataforma: KMP/Android/iOS futuros
- Entorno: Focus2 candidate
- Componentes afectados: restore temporal, scorer y evidence policy
- Archivos y lineas: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus2/domain/runtime/FocusSessionRestore.kt:94-140`; modelos/scorer relacionados; ledger `MB-FOCUS-0061`
- Introducido en commit, si puede determinarse: preexistente; ya registrado 2026-08-03
- Actor atacante: manipulador del almacenamiento local o estado corrupto/migrado
- Preconditions: Focus2 montado y snapshot malformado disponible al restore
- Limite de confianza atravesado: almacenamiento no confiable -> runtime/scorer/evidencia
- Datos o activos afectados: score, elegibilidad, evidencia y continuidad de sesion

### Resumen

Restore valida identidad y algunos totales, pero no prueba exhaustivamente negatividad, duplicados, allowlists, cuotas, upper bounds, razones/frames y overflow antes de consumir el estado.

### Causa raiz

El validador se diseño como coherencia parcial del writer legal, no como frontera hostil de deserializacion/migracion.

### Ruta de ataque

Snapshot corrupto con componentes negativos/duplicados/unknown o sumas incoherentes -> restore aceptado -> scorer/evidence recibe estado imposible.

### Evidencia

Revision de las comprobaciones existentes y tipos permitidos. No se ejecuto payload malformado y la feature sigue no montada.

### Reproduccion segura

Property/fuzz local con enteros limite, negativos, duplicados, enums desconocidos y totales inconsistentes; sin archivos reales.

### Resultado observado

La frontera carece de invariantes exhaustivos para rechazar todas esas clases antes del scoring.

### Impacto tecnico

Score/evidencia incorrectos, excepcion o corrupcion de continuidad si la ruta se activa.

### Impacto para el usuario y el negocio

Integridad de medicion y confianza del perfil; bloquea montar Focus2.

### Analisis de falsos positivos

El writer normal produce estados coherentes y la ruta esta inerte, lo que reduce reachability actual pero no elimina el defecto pre-activacion.

### Correccion recomendada

Validador exhaustivo, aritmetica segura, allowlists cerradas, unicidad y fail-closed antes de construir el runtime.

### Parche o archivos modificados

No aplicado; deduplicado contra `MB-FOCUS-0061`.

### Prueba de regresion

Corpus adversarial y property tests demuestran que ningun estado imposible alcanza scorer/evidence.

### Riesgo residual

La validez cientifica de estados coherentes es un gate distinto.

### Estado de revalidacion

ABIERTO; mantener Focus2 desmontado.

## [MOLVEX-SEC-020] El paquete `audit-output/` anterior produce una conclusion de seguridad no sustentada

- Estado: CONFIRMED_SECURITY_DEFECT
- Severidad: MEDIUM
- Prioridad Molvex: P1
- Confianza: Alta
- CVSS y vector: No aplica; finding de governance/evidence
- CWE: CWE-1026
- OWASP MASVS/MASTG: MASVS-CODE
- OWASP ASVS: V14 Configuration; OWASP SAMM Governance/Verification
- OWASP API: No aplica
- Plataforma: Governance/release
- Entorno: Auditoria y decision de release
- Componentes afectados: paquete anterior `audit-output/`
- Archivos y lineas: `audit-output/` snapshot/resultados; ledger `MB-FOCUS-0063`
- Introducido en commit, si puede determinarse: artefacto preexistente no anclado
- Actor atacante: no necesario; consumidor de evidencia incompleta
- Preconditions: usar el paquete anterior para aprobar una release
- Limite de confianza atravesado: evidencia generada -> decisor humano/release gate
- Datos o activos afectados: integridad del proceso de assurance y release

### Resumen

El paquete anterior declara auditoria completa/PASS pero registra commit como `HEAD`, omite contadores de ejecucion y contradice P0/P1 y el estado candidato/no montado del ledger.

### Causa raiz

El formato permite claims terminales sin identidad de snapshot, manifest, recibos ni reconciliacion con la autoridad vigente.

### Ruta de ataque

Presentar paquete verde -> decisor asume cobertura/ejecucion -> promover build con bloqueantes abiertos.

### Evidencia

Contradiccion directa entre artefacto, ledger y evidencia actual; ya registrada como `MB-FOCUS-0063`.

### Reproduccion segura

Ejecutar un validator que compare commit, contadores, findings abiertos y release verdict; el paquete anterior debe fallar.

### Resultado observado

Los campos no sostienen la conclusion terminal declarada.

### Impacto tecnico

Falsa assurance y perdida de trazabilidad.

### Impacto para el usuario y el negocio

Riesgo de aprobar un release inseguro o no verificable.

### Analisis de falsos positivos

El paquete puede servir como borrador historico; el defecto aparece cuando se usa como certificacion o autoridad actual.

### Correccion recomendada

Marcarlo `REPORTED/CONTRADICTED`, excluirlo de gates y exigir schema con SHA, manifest, recibos, limitaciones y clase de review.

### Parche o archivos modificados

No se altero el paquete ajeno; este dossier lo sustituye como informe actual, sujeto a su propia QA.

### Prueba de regresion

Gate de coherencia rechaza claim PASS con P0/P1 abiertos, commit no fijado o resultados vacios.

### Riesgo residual

Este dossier tambien es un worktree unanchored hasta commit/firma/CI, por lo que no se presenta como atestacion.

### Estado de revalidacion

ABIERTO; paquete anterior no apto para certificar.
