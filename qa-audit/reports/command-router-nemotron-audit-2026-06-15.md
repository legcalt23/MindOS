# Auditoria MindVerse - Command Router + Nemotron Speech

Fecha: 2026-06-15  
Rama auditada: `codex/auditoria-command-router`

## 1. Resumen ejecutivo

**Estado probable:** parcialmente funcional como foundation headless aislada; **no integrado y no
verificable como funcionalidad de producto**. La integracion Nemotron/NVIDIA NeMo Speech no existe
en el codigo revisado.

**Riesgo general:** critico para declarar la funcionalidad lista para produccion.

La arquitectura textual canonica tiene buenas propiedades: contratos tipados, separacion
router/ejecucion, validacion, errores tipados, correlacion por `requestId`, confirmaciones ligadas y
proteccion contra replay. Sin embargo, el flujo de voz actual usa mocks, no esta conectado a la app
raiz, no compila para iOS y contiene huecos que permiten gates verdes sin probar el flujo real.

Top 5 problemas:

1. No existe cliente Nemotron/NeMo, red, credenciales, modelo ni respuesta real.
2. La app Android/iOS no consume los modulos de voz/runtime; los modulos relevantes no estan
   trackeados en el estado auditado.
3. El supuesto E2E de voz sustituye el router real por un `RecordingRouter`; el transcript de
   ejemplo `abre ajustes` no esta registrado en el router canonico.
4. `sessionId` y `source` se pierden antes del matcher; el `AbuseGuard` requerido en produccion
   recibe `AbuseSubject.Anonymous` y el rate limiting queda inoperante.
5. El rechazo de confirmacion por voz borra estado local sin consumir el token canonico.

Correcciones prioritarias:

1. Integrar los modulos en un clean checkout y en la composition root real de Android.
2. Implementar un adaptador ASR/TTS real mediante backend broker; nunca incluir secretos NVIDIA en
   el cliente movil.
3. Crear un E2E que use `CommandRouterImpl`, proveedor real o sandbox y navegacion/UI real.
4. Propagar contexto de abuso/source hasta `UnderstandingContext` y probar rate limiting.
5. Revocar siempre el token canonico ante cualquier rechazo/cambio/cancelacion de confirmacion.

## 2. Arquitectura actual inferida

### Evidencia directa

El stack textual canonico es:

```text
TextCommandGateway
  -> KmpCommandRouter / CommandRouterImpl
  -> RouterActionExecutionAdapter
  -> CommandSafetyPolicy / ConfirmationStore
  -> ActionExecutor
  -> NavigationCoordinator
```

El stack de voz aislado es:

```text
SpeechInputProvider
  -> ASRBackend
  -> TranscriptPolicy
  -> TextCommandGateway
  -> ResponseTextResolver
  -> TTSBackend
  -> AudioOutputProvider
```

`mind-speech` declara explicitamente que no contiene Android, Apple, NVIDIA, red, credenciales,
UI ni proveedor real. La app raiz solo incluye `:shared`, `:androidApp` y `:desktopApp`. Los modulos
`mind-speech`, `mind-command-runtime`, `mind-app-runtime` y `mind-android-voice` viven en el build
independiente `mindos/`.

### Flujo real de producto

```text
Android/iOS app -> NO WIRING hacia MindCommandRuntime/VoiceCommandOrchestrator
```

### Flujo real verificable en tests

```text
FakeSpeechInputProvider
  -> MockASRBackend
  -> DefaultTranscriptPolicy
  -> RecordingRouter o gateway fake
  -> RecordingExecutor
  -> MockTTSBackend
  -> FakeAudioOutputProvider
```

### Inferencia

Un APK puede compilar correctamente sin contener la funcionalidad de comandos de voz. Eso ocurre
en el estado auditado: `:androidApp:assembleDebug` pasa, pero el build raiz no depende del runtime.

## 3. Hallazgos criticos

| ID | Severidad | Componente | Evidencia | Impacto | Recomendacion | Test de cierre |
|---|---|---|---|---|---|---|
| CR-01 | Critico | Nemotron/NeMo | `mind-speech` solo contiene contratos y mocks; docs declaran ausencia de NVIDIA/red/proveedor | No puede recibirse una transcripcion real de Nemotron | Adaptador remoto mediante backend broker y sandbox del proveedor | Audio real -> request backend -> transcript final real |
| CR-02 | Critico | App wiring/release | Build raiz no incluye ningun modulo `mind-*`; modulos de voz/runtime no trackeados | Clean checkout/CI/APK no contienen la feature | Trackear, incluir build o publicar artefactos, y cablear composition root | Test APK que resuelva y abra la UI de voz |
| H-01 | Alto | E2E/routing | `fullHeadlessSuccess` usa `RecordingRouter` que devuelve exito; fixture usa `abre ajustes`/`es-US`, registro usa `ajustes`/`es` | Falso verde: speech y router real pueden no conectar | Test de integracion con `CommandRouterImpl`; corregir catalogo y fallback BCP-47 | `abre ajustes` real produce `OpenSettings` |
| H-02 | Alto | Seguridad/rate limiting | Gateway descarta `sessionId/source`; matcher construye contexto sin `abuseSubject`; default es anonymous | `AbuseGuard` de produccion queda efectivamente inerte | Propagar sujeto opaco y source hasta matcher | Burst por sesion termina en deny |
| H-03 | Alto | Confirmacion | `confirmByVoice(accepted=false)` y cambio de accion limpian `pending` sin `cancelConfirmation()` | Token aun valido hasta TTL; posible ejecucion tardia/replay externo | Toda salida terminal no ejecutada debe consumir/revocar token | Voz rechazada -> confirm posterior = replay/not found |
| H-04 | Alto | Timeouts | Solo ASR/TTS tienen timeout en orchestrator; capture, routing/execution y output dependen del adapter | Cuelgue indefinido y UI bloqueada | Timeouts por etapa en el limite canonico | Fake infinito por etapa termina tipado |
| H-05 | Alto | Android UX/safety | UI muestra `Processing` antes de iniciar `runOnce`; boton cancelar sigue activo durante routing/TTS | Puede mostrar cancelacion despues de que el comando ya se ejecuto | Estado por etapa y cancelar solo pre-dispatch | Cancel post-dispatch no afirma reversion |
| H-06 | Alto | KMP/iOS | Convencion `mindos.kmp-library` solo declara `jvm()`; iOS app esta bloqueada | Sin compilacion ni ejecucion iOS del runtime | Targets iOS y adapters AVAudioSession reales | `iosSimulatorArm64Test` + prueba fisica |
| M-01 | Medio | Latencia/audio | Captura Android llena siempre el buffer de hasta 10 s; sin stop por release, VAD ni streaming | Latencia y coste altos; no hay primer parcial | Sesion de captura start/stop + VAD/streaming | Comando corto no espera 10 s |
| M-02 | Medio | Formato audio | `audioSampleRateHz` no se aplica; orchestrator no valida formato contra provider/modelo | ASR puede recibir encoding/rate incompatible | Validacion estricta y conversion explicita | Formato incorrecto = INVALID_INPUT |
| M-03 | Medio | Observabilidad | Sink speech default NoOp; no sink productivo; faltan eventos de permiso/request/final/UI | No se puede demostrar operacion real | Sink estructurado allowlisted y trazas por etapa | Una sesion genera cadena completa |
| M-04 | Medio | Lifecycle/audio focus | No lifecycle, audio focus, interrupciones ni tests instrumentados | Grabacion/reproduccion fragil en background/llamadas | Adapter lifecycle-aware y audio focus | onPause/interruption libera recursos |
| M-05 | Medio | Privacidad | No consentimiento/retencion/deletion para voz externa; Info.plist solo menciona notas de voz | Riesgo legal/privacidad al activar proveedor | Consentimiento explicito y politica de datos | Sin consentimiento no sale audio |

## 4. Auditoria del Command Router

### Fortalezas con evidencia

- Desacoplado de UI y plataforma; contratos y logica viven en `commonMain`.
- Router y executor estan separados; el router no ejecuta ni navega.
- Registro declarativo pequeno, errores tipados, deteccion de ambiguedad y validacion DTO.
- Seguridad fail-closed para acciones no clasificadas.
- Confirmacion ligada a token/request/session/action/TTL y proteccion de replay.
- Idempotencia antes de navegacion cuando se inyecta `IdempotencyStore`.
- Tests unitarios buenos para rutas, fallos, correlacion, seguridad y confirmacion.

### Riesgos

- El catalogo default sigue hardcoded y es demasiado limitado para ASR natural.
- El lookup de alias exige locale exacto o `und`; `es-US` no cae a `es`.
- El gateway no tiene timeout para router/executor.
- El contexto de abuso no atraviesa el contrato publico.
- No existe `commandId`/`intentId` independiente y estable de extremo a extremo; se usa
  `requestId` + `actionType`.

### Rediseño recomendado

Mantener el stack canonico. Extender el contexto, no crear otro router de voz:

```kotlin
data class RouterRequestContext(
    val requestId: String,
    val localeTag: String,
    val source: CommandSource,
    val abuseSubject: AbuseSubject,
)
```

El `abuseSubject` debe contener identificadores opacos, no email/UID bruto. Añadir fallback
BCP-47 (`es-MX` -> `es` -> `und`) y corpus golden con frases ASR reales.

## 5. Auditoria de Nemotron Speech

**Veredicto:** no integrado.

No hay:

- Cliente NVIDIA/NeMo/Riva/Nemotron.
- Endpoint, transporte, autenticacion, modelo o version de modelo.
- Adaptador de streaming, VAD, diarizacion o parciales.
- Evidencia de audio enviado o transcript final recibido.
- Gestion de errores HTTP/gRPC, reintentos, reconexion o cuotas.
- Broker backend para proteger credenciales.

Lo existente define buenos ports tipados (`ASRBackend`, `TTSBackend`, `SpeechInputProvider`,
`AudioOutputProvider`) y evita logs de audio/transcript, pero no demuestra integracion.

## 6. Auditoria KMP

- `commonMain`: contratos speech, policy, orchestrator, router y runtime estan bien ubicados.
- Android: captura PCM y output liberan recursos en `finally`; permisos se comprueban.
- iOS: no existe adapter voice ni target de compilacion en el build `mindos/`.
- No se requiere `expect/actual` en el diseño actual porque se usan ports inyectados; esto es
  correcto. El problema es que faltan implementaciones/composition roots por plataforma.
- `AndroidPushToTalkViewModel` recibe un `CoroutineScope` externo y no es lifecycle-aware por si
  mismo; la seguridad depende totalmente del caller inexistente.

## 7. Flujo end-to-end validado o fallido

| Etapa | Estado | Señal existente | Señal minima requerida |
|---|---|---|---|
| Permiso microfono | Parcial Android | Check + tests unitarios fake | Dialogo real y resultado instrumentado |
| Captura real | Parcial Android aislado | `AudioRecord` implementation | Test fisico con buffer no vacio |
| Envio Nemotron | Fallido/ausente | Ninguna | requestId del backend/proveedor |
| Transcript final real | Fallido/ausente | Solo mock | evento final con backend/model |
| Normalizacion/policy | Validado headless | Tests JVM | Corpus ASR real |
| Router real tras ASR | No validado | E2E usa router fake | Test con `CommandRouterImpl` |
| Handler/accion | Validado con fakes | RecordingExecutor | Navegacion real en app |
| UI | No integrada | Panel aislado | Estado visible en APK |
| Trazabilidad completa | Parcial | requestId/sessionId en mocks | Traza productiva por sesion |

## 8. Matriz de pruebas recomendada

| Nivel | Casos bloqueantes |
|---|---|
| Unit | Locale `es-US -> es`; `abre ajustes`; formato audio invalido; timeout capture/router/executor/output; revocacion tras voz rechazada; abuso por sesion |
| Integration | `FakeSpeechInput -> MockASR -> DefaultTranscriptPolicy -> CommandRouterImpl -> gateway -> executor`; no `RecordingRouter` permisivo |
| Provider contract | Sandbox Nemotron: auth, modelo, PCM 16 kHz mono, timeout, 429, 5xx, transcript vacio/parcial/final |
| Android unit | Lifecycle controller, cancel pre/post dispatch, audio focus, interrupcion, background |
| Android instrumented | Permiso concedido/denegado, AudioRecord real, red offline, navegacion/UI real |
| iOS | AVAudioSession, permiso, interrupcion, cancelacion, memoria, output |
| E2E | Usuario habla -> transcript real -> router real -> accion real -> UI -> traza completa |
| Security | Token nunca en cliente/log; rate limit efectivo; confirmacion rechazada no reutilizable |

## 9. Observabilidad recomendada

Eventos obligatorios:

```text
voice.session.started
mic.permission.requested / granted / denied
audio.capture.started / stopped
audio.buffer.created
speech.request.sent
speech.partial.received
speech.final.received
speech.error
transcript.policy.accepted / rejected
command.route.matched / failed
command.handler.started / success / error
ui.state.updated
voice.session.finished
```

Campos allowlisted: `requestId`, `sessionId` opaco, `platform`, `locale`, `timestamp`,
`durationMs`, `errorCode`, `provider`, `model`, `networkClass`, `result`, `actionType`.

No registrar input "sanitizado": para voz potencialmente sensible usar solo longitud/bucket,
confidence bucket y action type tras matching. Nunca audio, transcript, entity values, response
text, email, UID bruto, headers o respuestas del proveedor.

## 10. Seguridad y privacidad

**Clasificacion de datos:** audio y transcript = `HIGHLY_SENSITIVE`.

Fortalezas:

- No se encontraron secretos Nemotron/NVIDIA en cliente porque no existe proveedor.
- Audio Android permanece en memoria y se limpian buffers temporales.
- Transcript/audio no se incluyen en speech telemetry.
- Transporte Android/iOS general prohibe cleartext.

Bloqueos:

- Rate limiting canonico inoperante por perdida de contexto.
- Rechazo de confirmacion no siempre revoca token.
- No existe consentimiento ni politica de retencion para voz externa.
- TLS, pinning, auth, cuotas, App Check/attestation y proteccion backend no son verificables.
- `SpeechTelemetryEvent.toString()` incluye `requestId/sessionId`; deben ser opacos y pasar por
  redactor antes de cualquier sink.

Arquitectura segura del proveedor:

```text
Mobile -> backend propio autenticado/attested -> NVIDIA/NeMo
```

El backend debe guardar el secreto, aplicar rate limit, minimizar retencion, emitir IDs de
correlacion y devolver errores tipados. No usar API key NVIDIA persistente en Android/iOS.

## 11. Performance

KPIs bloqueantes:

- Tiempo a primer parcial: p50 < 700 ms, p95 < 1.5 s.
- Tiempo a transcript final: p95 < 3 s tras fin de habla.
- Routing: p95 < 20 ms.
- Voz a comando ejecutado: p95 < 4 s para comandos simples.
- Fallback < 10% en corpus soportado; timeout < 2%; error por plataforma visible.

Cuellos actuales:

- Batch Android llena hasta 10 s antes de ASR.
- Streaming esta prohibido por config.
- Sin VAD, stop por release, backpressure, reconexion ni parciales.
- Varias copias defensivas de PCM aumentan memoria transitoria; aceptable para batch corto, no para
  streaming.

## 12. Plan de correccion

### Fixes inmediatos

1. Trackear/integrar modulos y cablear Android con flags apagados por defecto.
2. Corregir revocacion de confirmacion y propagacion de `AbuseSubject`.
3. Sustituir el falso E2E por router real y corregir locale/catalogo.
4. Añadir timeouts canonicos por etapa.

### Refactor necesario

1. Captura controlable por start/stop y estado por etapa.
2. Backend broker Nemotron con contrato tipado.
3. Targets iOS y adapters de audio/session.
4. Sink productivo privacy-safe.

### Tests obligatorios

Los casos de la seccion 8 son gates de release, especialmente proveedor sandbox, Android
instrumentado, iOS y E2E real.

### Mejoras futuras

Streaming/VAD, parciales, barge-in seguro, fallback offline y evaluacion continua de corpus.

## 13. Codigo recomendado

Reutilizar los ports actuales; no duplicarlos. Añadir un limite con timeout completo:

```kotlin
suspend fun runBounded(session: VoiceSession): VoiceCommandOutcome =
    withTimeout(config.sessionTimeoutMs) { orchestrator.runOnce(session) }
```

Propagar contexto de abuso al router:

```kotlin
RouterRequestDto(
    requestId = request.requestId,
    input = request.input,
    localeTag = request.localeTag,
    source = request.source,
    abuseSubject = request.abuseSubject,
)
```

Revocar rechazo de confirmacion:

```kotlin
if (!accepted || currentActionType != binding.actionType) {
    cancelCanonical(binding)
    return
}
```

Test de integracion minimo correcto:

```kotlin
val gateway = gateway(router = CommandRouterImpl(), executor = recordingExecutor)
val outcome = orchestrator(gateway, transcript = "ajustes", locale = "es").runOnce(session)
assertIs<VoiceCommandOutcome.Completed>(outcome)
assertIs<MindAction.OpenSettings>(recordingExecutor.actions.single())
```

Para Nemotron, implementar `ASRBackend` en la capa data/platform contra un backend propio. El
adapter debe validar formato, enviar IDs opacos, mapear HTTP/gRPC a `BackendFailureReason` y no
retener audio.

## 14. Criterios de aceptacion

No declarar la integracion funcional hasta cumplir todos:

- Clean checkout y CI incluyen todos los modulos y tests.
- APK Android expone el flujo real; iOS compila y ejecuta su adapter.
- Audio real se captura y se detiene por accion del usuario/VAD.
- Backend propio recibe audio real y devuelve transcript final de Nemotron/NeMo.
- Provider/model/request ID aparecen en telemetria privacy-safe.
- Transcript real pasa policy y router canonico, sin router fake.
- Handler/NavigationCoordinator ejecuta una accion real y UI refleja resultado.
- `requestId/sessionId` correlacionan todas las etapas.
- Rate limiting funciona por sujeto opaco.
- Rechazo/cancelacion invalida confirmacion canonica.
- Timeouts y cancelacion funcionan en todas las etapas.
- Tests unitarios, integration, provider sandbox, Android instrumented, iOS y E2E pasan.
- Consentimiento, retencion, borrado y disclosure del proveedor estan aprobados.

## Verificacion ejecutada

- `mindos/gradlew.bat productionReady --console=plain`: **PASS**, 176 tareas.
- `gradlew.bat :androidApp:assembleDebug --console=plain`: **PASS**, pero sin dependencia de los
  modulos auditados.
- Suites focales: **102 tests, 0 failures, 0 errors, 0 skipped**.

Los gates verdes demuestran calidad del foundation aislado, no integracion Nemotron ni
funcionalidad E2E de producto.
