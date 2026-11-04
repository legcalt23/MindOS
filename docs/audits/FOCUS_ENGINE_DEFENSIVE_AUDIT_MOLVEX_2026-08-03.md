# Auditoria defensiva del motor Enfoque (Molvex)

Fecha: 2026-08-03  
Repositorio: `C:\Users\curro\AndroidStudioProjects\MindOS`  
Branch: `product/focus2-v5`  
HEAD observado: `8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5`  
Estado: worktree modificado y con artefactos Focus V5 sin seguimiento  
Clase de revision: `AI_ASSISTED_DEFENSIVE_REVIEW`, no auditoria humana externa

## 1. Veredicto ejecutivo

**NO APTO para montar, liberar ni presentar Focus V5/R3 como motor auditado.**

El riesgo maximo observado es **ALTO**. Focus2 sigue sin mount productivo, lo que reduce la
exposicion inmediata del nuevo runtime, pero no elimina los defectos transversales de almacenamiento,
borrado y supply chain. La autoridad vigente ya declara `R3 CANDIDATA`, `MOUNT=BLOCKED`,
`EXTERNAL_HUMAN_AUDIT=NOT_OBTAINED` y 5/7 lentes en FAIL
([CURRENT_AUTHORITY_INDEX.md](../engines/focus_v5/CURRENT_AUTHORITY_INDEX.md)). Esta revision confirma
el bloqueo y agrega evidencia defensiva nueva.

Hallazgos materialmente nuevos: **8**. Hallazgos previos relevantes que siguen abiertos: **al menos 9**.
No se cierra ningun hallazgo. La bateria Kotlin/Gradle actual queda **NOT_TESTED (TIMEOUT)**.

La ruta critica es:

1. hacer durable y verificable el almacenamiento/borrado;
2. impedir resurreccion y borrado cruzado por sujeto;
3. endurecer restore e identidad criptografica;
4. versionar el arbol exacto y cerrar la supply chain;
5. ejecutar Android, Desktop, iOS y hardware sobre el mismo SHA;
6. obtener las revisiones humanas que exige la propia autoridad antes del mount.

## 2. Conflicto de politica y alcance real

El prompt Molvex solicita una auditoria integral cientifica, psicometrica, UX, competitiva, de producto y
estrategia. `AGENTS.md` limita este entorno a ciberseguridad. Por tanto:

- AppSec, privacidad tecnica, integridad, DevSecOps, supply chain y threat modeling: **AUDITADOS**.
- Ciencia cognitiva, psicometria, UX general, benchmark competitivo, monetizacion y estrategia:
  **POLICY_CONFLICT / NOT_AUDITED**.
- Accesibilidad: solo se reviso cuando afecta privacidad, integridad o validez de la medicion.
- No se modifico codigo, no se instalo software, no se contacto produccion y no se usaron credenciales.

El resultado no puede llamarse auditoria Molvex completa. Es una auditoria defensiva parcial y trazable.

## 3. Metodo y etiquetas de evidencia

- `VERIFICADO_ACTUAL`: inspeccion directa de los bytes del worktree actual.
- `EJECUTADO_ACTUAL`: comando ejecutado en esta revision con resultado observable.
- `HISTORICO`: resultado registrado anteriormente; no prueba los bytes actuales.
- `INFERIDO`: consecuencia razonable cuya explotacion no se ejecuto.
- `CONTRADICHO`: afirmacion incompatible con otra evidencia de mayor autoridad.
- `NOT_TESTED`: no ejecutado, no disponible o sin resultado util.

Se revisaron la autoridad Focus V5, el ledger, las rutas Focus legacy/Focus2, secure storage Android/iOS,
consentimiento, lifecycle, restore, borrado de cuenta, reglas Firestore, telemetria, Gradle/CocoaPods/npm y
CI. Tres revisiones independientes cubrieron AppSec/MASVS, threat modeling y supply chain; la integracion y
el ledger permanecen bajo un unico escritor.

## 4. Mapa de superficie y confianza

```text
Persona / accesibilidad
        |
        v
FocusTracerScreen
        |
        v
FocusTracerStateHolder (proceso cliente no confiable)
        | seal / checkpoint / restore
        v
Runtime + reducer determinista ----> tallies ----> DescriptiveScorer
        |                                               |
        v                                               v
Secure KV Android/iOS                         FocusCompletion.Scored
                                                        |
                                      profile/sync/result ledger: AUSENTES
```

Separacion material:

- **Focus legacy** esta montado en el producto.
- **Focus2/V5** tiene runtime, almacenamiento y UI, pero no se encontraron callers productivos de
  `FocusTracerScreen`, `FocusTracerStateHolder`, `FocusTemporalEligibility.evaluate` ni
  `FocusEvidenceStatePolicy.evidenceOf`.
- No existe una ruta durable y autorizada
  `completion -> resultado sellado -> profile gate -> persistencia/sync -> UI`.
- El score es client-side. No debe gobernar recompensas, rankings, decisiones economicas ni estado remoto
  sin verificacion server-side e idempotencia.

Datos sensibles observados: UID/session ID, consentimiento, seed y especificacion sellada, respuestas
agregadas, omisiones, anticipaciones, RT agregado, pausas, calidad temporal, modalidad, indicadores de
accesibilidad y score futuro. Se clasifican como datos conductuales/cognitivos; algunos indicadores de
accesibilidad pueden actuar como proxy de discapacidad.

## 5. Hallazgos nuevos priorizados

| ID | Severidad | Estado | Hallazgo | Evidencia principal | Cierre exigido |
|---|---:|---|---|---|---|
| `MB-FOCUS-0058` | Alta / P1 | OPEN | Android devuelve exito antes de persistencia real. `put`, `remove`, cuarentena y `clearNamespace` usan `SharedPreferences.apply()`, pero capas superiores interpretan ausencia de excepcion como `Stored`/borrado confirmado. Process death o fallo de disco puede perder un seal, checkpoint o marcador de borrado. | `SecureKeyValueStore.android.kt:75-107`; `SecureFocusSessionStore.kt:106`; `AccountDeletionCoordinator.kt:163` | Primitiva durable/confirmable; propagar fallo tipado; pruebas de kill-process y almacenamiento lleno. |
| `MB-FOCUS-0059` | Alta / P1 | OPEN | iOS puede convertir un item Keychain no UTF-8 en ausencia. `copyMatchingValue()` devuelve `null`; si afecta al indice reservado, `readIndex()` produce conjunto vacio y `clearNamespace()` puede terminar sin intentar borrar datos reales. | `IosKeychainBackend.kt:138-146`; `IndexedSecureKeyValueStore.kt:103-126` | Distinguir `not found` de decode/corrupcion; fallo cerrado; prueba Keychain real con indice no UTF-8 y canarios. |
| `MB-FOCUS-0060` | Media / P2 | OPEN | AES-GCM no vincula ciphertext con namespace/clave/usuario/tipo/revision mediante AAD. Consentimiento tampoco valida `record.userId/scope` contra la clave solicitada. Un blob valido del mismo namespace puede trasplantarse y quedar mal atribuido. | `SecureKeyValueStore.android.kt:139-166`; `FocusDataConsent.kt:190-206`; `FocusSessionRestore.kt:94-112` | Envelope versionado + AAD; validar identidad logica tras decode; tests de swap entre sujetos, scopes, sesiones y revisiones. |
| `MB-FOCUS-0061` | Alta / P1 | OPEN | Restore valida identidad basica y algun total, pero no cierra toda la aritmetica temporal: componentes negativos, buckets desconocidos/duplicados, cuotas, sumas y coherencia de frames/razones pueden atravesar el restore y sesgar elegibilidad. | `FocusSessionRestore.kt:123-140`; schema temporal v2 del worktree | Validador exhaustivo, limites y aritmetica segura; property/fuzz sobre snapshots corruptos antes de scorer/elegibilidad. |
| `MB-FOCUS-0062` | Alta / P1 antes de sync | OPEN | El gateway legado modela `users/{uid}/sessions/{sid}`, pero el borrado backend elimina `users/{uid}` sin borrar `sessions`. Firestore no borra subcolecciones al eliminar el padre. Las reglas actuales bloquean el sync, por lo que el defecto es latente y debe cerrarse antes de abrirlo. | `accountDeletion.ts:362-459`; gateway Focus; `firestore.rules` sin match de sessions | Borrado recursivo/batcheado server-side, inventario de subcolecciones y canarios de erasure; mantener reglas deny hasta cerrar. |
| `MB-SUPPLY-0001` | Alta / P1 | OPEN | Gradle no tiene dependency lockfiles ni `verification-metadata.xml`; el release firmado resuelve plugins y transitivas sin una raiz de integridad reproducible. | `settings.gradle.kts`; `gradle/libs.versions.toml`; `.github/workflows/release.yml:136-161`; inventario tracked = 0 locks/metadata | Locks revisados + verificacion estricta de checksums/firmas en root e included builds; CI limpio y SBOM por target. |
| `MB-SUPPLY-0002` | Alta / P1 iOS | OPEN | `iosApp/Podfile` declara siete pods Firebase sin version y no existe `Podfile.lock`; el workflow iOS no se activa por `iosApp/**` ni compila el app/workspace real. | `iosApp/Podfile:28-34`; `iosApp/README.md:46-48`; `.github/workflows/ios-build.yml:14-27,80-95` | Versionar `Podfile.lock`, `pod install --deployment`, fijar herramienta, ampliar triggers y compilar app/simulator/device. |
| `MB-FOCUS-0063` | Alta / P1 | OPEN | Un paquete previo en `audit-output` afirma auditoria completa, PASS general, seguridad/privacidad PASS y build verde, pero su snapshot usa `commit: HEAD` y deja `passing/failing/unrun` vacios. Contradice la autoridad R3 candidata, 5/7 FAIL, P0/P1 abiertos y el portfolio de 18. Es falsa seguridad operativa. | `audit-output/focus-engine-audit-20260803-2037/00_*:11-36`; `snapshot.yaml:5,26-28`; `CURRENT_AUTHORITY_INDEX.md:25-32` | Marcar artefacto `REPORTED/CONTRADICTED`, prohibirlo como certificado y exigir snapshot SHA, manifest, gates y revision independiente. |

### Hallazgo transversal ya existente

`erase-foreach-abort` ya registra que `AccountDeletionCoordinator.eraseUserData` aborta el `forEach` tras
el primer namespace que falla. No se crea un ID duplicado. La correccion debe intentar todos los namespaces,
acumular errores y mantener el borrado pendiente hasta verificar el conjunto completo.

## 6. Hallazgos existentes que siguen bloqueando

| ID | Riesgo actual | Estado observado |
|---|---|---|
| `MB-FOCUS-0052` | Revocacion no cableada a writes Focus2 y borrado cruzado | OPEN P0; confirmado |
| `MB-FOCUS-0014` | Borrar cuenta A elimina consentimiento/sesiones de B en dispositivo compartido | OPEN; confirmado |
| `MB-FOCUS-0035` | Un segundo holder borra el buffer vivo del primero | OPEN; `start()` hace scan+discard ciego |
| `MB-FOCUS-0027` | Omisiones/anticipaciones quedan fuera del denominador sin techo | OPEN; seleccion favorable posible |
| `MB-FOCUS-0009` | Journal limitado por cantidad, no por edad | OPEN; writer aun no montado |
| `MB-FOCUS-0048` | Contrato temporal parcialmente `DESIGNED_NOT_RUN` | OPEN; sin wiring productivo |
| `MB-FOCUS-0050` | Autoridad e inputs actuales no anclados a HEAD | OPEN; confirmado por git status |
| `MB-FOCUS-0051` | Fuga historica de respuesta por accesibilidad | Mitigada en bytes actuales con descripcion answer-blind, pero sigue OPEN y no fue ejecutada |
| `MB-FOCUS-0026/0028/0042` | Modalidad, recovery global y generalizacion UI/restore incompletas | OPEN; mount blocker |

## 7. Threat model y abusos prioritarios

| Amenaza | Activo | Ruta | Impacto | Control requerido |
|---|---|---|---:|---|
| Score client-side tratado como autoridad | perfil/recompensas | cliente modificado fabrica completion/scalar | Alto | recomputo o envelope verificable server-side; trust tier local |
| Perdida entre score y persistencia | historial/perfil | proceso muere tras score o antes de purge | Alto | result envelope durable + outbox idempotente |
| Replay/duplicado/out-of-order | perfil/sync | reenvio de session ID/lineage | Alto | create-only + idempotency key + revision monotona |
| Segundo holder destruye sesion viva | evidencia | scan-discard sin lease | Alto | lease/owner epoch y single-instance por sujeto |
| Blob swapping | consentimiento/sesion | mover ciphertext bajo otra clave | Medio | AAD + identity binding + quarantine tipada |
| Restore de estado imposible | score/elegibilidad | JSON valido con aritmetica corrupta | Alto | validacion completa + fuzz/property |
| Omision/anticipacion selectiva | validez | excluir trials dificiles | Alto | politica de missingness y tests adversariales |
| Resurreccion tras borrado | privacidad | callback/writer activo repuebla namespace | Alto | tombstone de sujeto en cada read/write + cancelacion |
| Telemetria granular futura | privacidad | eventos por trial ganan IDs/timestamps/RT | Medio | schema allowlist agregado, sin trial/RT/seed/modalidad/ID estable |
| Procedencia no reproducible | release | build sobre WIP/untracked | Alto | commit/manifest aceptado + CI limpio sobre SHA exacto |

Abusos de red team defensivo que deben convertirse en tests:

1. dos holders arrancan con barrera y el segundo no puede tocar al primero;
2. kill-process en seal/checkpoint/result/purge nunca produce falsa evidencia admisible;
3. swap descriptor/snapshot/consentimiento entre UIDs y claves falla cerrado;
4. snapshots con negativos, overflow, buckets duplicados y sumas imposibles se rechazan;
5. logout/delete mientras un write esta bloqueado no resucita datos;
6. duplicado/retry/out-of-order produce exactamente una contribucion;
7. reemplazar una respuesta incorrecta por omision/anticipacion nunca mejora elegibilidad;
8. un evento remoto con respuesta, RT, seed, modalidad o ID estable rompe el gate de privacidad.

## 8. MASVS, privacidad y borrado

Evaluacion defensiva:

- `MASVS-STORAGE-1`: **PARCIAL/FAIL**. Cifrado local, backups Android deshabilitados y Keychain iOS son
  controles positivos; durabilidad, binding e indice iOS impiden aprobar el conjunto.
- `MASVS-STORAGE-2`: **PARCIAL**. No se encontro export, clipboard o red propia de Focus2 y los errores no
  incluyen payloads; el sink global y futuras rutas deben mantenerse bajo canarios.
- `MASVS-PRIVACY-1`: **PARCIAL/FAIL**. Hay minimizacion (agregados, no serie cruda) y consentimiento por
  proposito, pero retencion, revocacion, subject erasure y account isolation no cierran.
- `MASVS-AUTH/NETWORK`: **NOT_APPLICABLE hoy para Focus2** por ausencia de mount/sync; deben reabrirse al
  crear un limite remoto.

La normativa no se certifica aqui. Como criterio tecnico, los principios de minimizacion y limitacion de
conservacion del art. 5 RGPD, el derecho de supresion del art. 17 y la proteccion por diseno del art. 25
obligan a tratar resurreccion, borrado parcial y retencion indefinida como blockers, no como deuda cosmética.

Fuentes primarias:

- [OWASP MASVS-STORAGE-1](https://mas.owasp.org/MASVS/controls/MASVS-STORAGE-1/)
- [OWASP MASVS-PRIVACY-1](https://mas.owasp.org/MASVS/controls/MASVS-PRIVACY-1/)
- [Android SharedPreferences.Editor](https://developer.android.com/reference/android/content/SharedPreferences.Editor)
- [Apple Keychain accessibility](https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly)
- [RGPD, texto oficial EUR-Lex](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- [Firestore: delete data](https://firebase.google.com/docs/firestore/manage-data/delete-data)

## 9. Supply chain y DevSecOps

Controles positivos verificados:

- repositorios limitados a Google, Maven Central y Plugin Portal;
- no `mavenLocal`, JitPack, HTTP, `flatDir`, `SNAPSHOT`, `latest.*`, `+` ni `changing=true`;
- Actions externas fijadas a SHA de 40 caracteres;
- wrappers Gradle 9.3.1 con SHA de distribucion y JARs byte-identicos;
- checkout sin persistir credenciales y permisos generales `contents: read`;
- firma release falla cerrada.

Deudas adicionales:

- no hay escaneo Gradle/CocoaPods, SBOM por target ni cobertura KEV/EPSS;
- `npx vibe-kanban` se ejecuta sin version/lock;
- gates locales pueden reutilizar `node_modules` mutable y `npm ci` admite lifecycle scripts;
- varios jobs declaran JDK 17 aunque el daemon requiere Java 21/Foojay;
- Focus2 comparte el classpath amplio de `:shared`, aumentando el radio de confianza.

Gradle distingue dependency locking (versiones resueltas reproducibles) de dependency verification
(integridad/autenticidad de artefactos). Ambos faltan y ambos son necesarios para un release defendible:
[locking](https://docs.gradle.org/current/userguide/dependency_locking.html) y
[verification](https://docs.gradle.org/current/userguide/dependency_verification.html).

## 10. Gates ejecutados en esta revision

| Gate | Resultado | Evidencia |
|---|---|---|
| `node --test scripts/validate-authority-entrypoints.test.mjs` | PASS | 19/19, 0 fail |
| `node scripts/validate-authority-entrypoints.mjs --json` | PASS | `ok: true`, 0 errors |
| `node scripts/pii-secret-scan.mjs --selftest` | PASS | v2.0.0, 15 reglas; no es un scan completo |
| `node scripts/validate-audit-ledger.mjs` antes de editar | PASS | 130 hallazgos; hash `ce2645009416...` |
| Wrapper `gradlew` Focus subset, offline | NOT_TESTED | fallo pre-project: wrapper intento obtener Gradle 9.3.1 con red bloqueada |
| Gradle 9.3.1 cacheado, Focus subset, offline/rerun | NOT_TESTED (TIMEOUT) | sin stdout ni progreso util durante >11 min; proceso iniciado por la auditoria detenido por PID; ningun otro Gradle activo |
| Android build/runtime | NOT_TESTED | no ejecutado tras timeout |
| iOS simulator/device/Keychain real | NOT_TESTED | host Windows, sin Xcode |
| Hardware refresh/process-death/accessibility | NOT_TESTED | laboratorio fisico no disponible |
| Firebase/emuladores/produccion | NOT_TESTED | no iniciados; produccion fuera de alcance |

Los resultados historicos de 4032 tests y 229 Focus2 no se atribuyen a este worktree. El timeout tampoco
demuestra fallo de tests: demuestra que esta revision no obtuvo una bateria ejecutada util.

## 11. Scorecard defensiva

No se calcula el total Molvex de 100 puntos porque 11 dimensiones no defensivas quedan fuera de politica.

| Dimension permitida | Puntuacion | Veredicto |
|---|---:|---|
| Gobierno de evidencia y procedencia | 2/10 | FAIL: WIP/untracked y certificado contradictorio |
| Arquitectura de confianza e integridad | 3/10 | FAIL: resultado durable/profile/sync ausentes |
| Datos, sync y borrado | 2/10 | FAIL: resurreccion, borrado cruzado/parcial y subcolecciones |
| Privacidad y seguridad | 4/10 | FAIL: controles locales positivos, lifecycle incompleto |
| Cobertura de tests de seguridad | 2/10 | FAIL: gate actual sin resultado; gaps de process death/iOS/hardware |
| Supply chain y release | 3/10 | FAIL: sin locks/verificacion/SBOM; CocoaPods sin lock |
| Red team defensivo | 4/10 | PARCIAL: amenazas conocidas, varios abusos sin test vivo |

Dimensiones A-E cientificas/producto, I UX general, L mantenibilidad general, M benchmark, N coste y las
partes no defensivas de O: `POLICY_CONFLICT / NOT_AUDITED`.

## 12. Plan de remediacion

### P0/P1 - antes de cualquier mount

1. Prohibir el uso del paquete `audit-output/focus-engine-audit-20260803-2037` como certificado.
2. Versionar los bytes exactos de Focus R3 y citar el commit en manifest, gates e informe.
3. Sustituir el contrato Android `apply() -> Stored` por persistencia verificable y fallo tipado.
4. Hacer subject-scoped y best-effort-total el borrado; tombstone obligatorio en todos los writes.
5. Corregir decode/indice Keychain fail-closed y probarlo en simulator/device.
6. Añadir AAD e identity binding a consentimiento, descriptor y snapshot.
7. Endurecer restore con validacion exhaustiva, limites, safe arithmetic y fuzz/property.
8. Crear result envelope durable, outbox e idempotencia antes de perfil/sync.
9. Cerrar `MB-FOCUS-0027/0053`: missingness, omisiones, anticipaciones y detector anti-gaming vivo.
10. Implantar Gradle locks + verification metadata y CocoaPods lock bajo revision humana.

### P2 - despues de los blockers estructurales

11. Cerrar modalidad/recovery/generalizacion UI antes de exponer Focus2.
12. Definir TTL/retencion del journal y mantener el writer sin mount hasta entonces.
13. Añadir SBOM por target, revision de vulnerabilidades y JDK 21 explicito en todos los jobs.
14. Fijar `vibe-kanban` y evitar instalaciones implícitas/lifecycle scripts no allowlisted.
15. Extraer progresivamente `focus2-core` para reducir dependencias y radio de confianza.

## 13. Criterios de aceptacion del re-audit

- [ ] Worktree limpio; todos los inputs versionados; informe y manifest citan SHA real.
- [ ] Un solo resultado durable por `(subject, sessionId, lineageDigest)` bajo retry/replay/out-of-order.
- [ ] Kill-process no pierde ni duplica seal, checkpoint, resultado o borrado.
- [ ] Cuenta A se borra sin afectar B y ningun writer revive A.
- [ ] Fallo de un namespace no impide intentar los restantes; retry conserva survivors.
- [ ] Keychain corrupto/no UTF-8 bloquea y reintenta; nunca simula ausencia.
- [ ] Swaps de blob entre UID/scope/session/revision se rechazan.
- [ ] Restore rechaza negativos, overflow, buckets duplicados, cuotas y sumas incoherentes.
- [ ] Omision/anticipacion no mejora score/elegibilidad y tiene politica ratificada.
- [ ] Telemetria no incluye respuesta, RT, seed, modalidad, trial index, timestamp granular o ID estable.
- [ ] Gradle verification estricta, locks y CocoaPods deployment pasan en checkout limpio.
- [ ] Android, Desktop, iOS simulator/device y hardware representativo ejecutan sobre el mismo SHA.
- [ ] Revision humana independiente y aprobaciones requeridas quedan registradas antes del mount.

## 14. Cierre y limitaciones

Estado de saturacion: **PARTIAL**. La superficie defensiva fue recorrida en dos pasadas y deduplicada contra
el ledger, pero la auditoria integral solicitada no es compatible con este entorno. Tampoco se obtuvieron
ejecuciones Kotlin, iOS o hardware actuales. Por ello no se emite `AUDIT_COMPLETE`, `SECURITY_APTO`,
`WORLD_CLASS` ni certificado equivalente.

Archivos de producto modificados por esta auditoria: **0**. Artefactos propios: este informe y la
actualizacion obligatoria del ledger.
