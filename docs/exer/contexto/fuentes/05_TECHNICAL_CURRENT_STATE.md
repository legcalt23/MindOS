# MindBoost — Technical Current State & Target Architecture

**Versión:** 1.0.0  
**Fecha de snapshot:** 2026-07-10  
**Owner:** MindBoost Engineering  
**Estado:** LIVING DOCUMENT  
**Regla:** este archivo describe lo conocido; el repositorio y los gates reproducibles deciden lo que realmente existe

---

## 1. Propósito

Este documento evita que una IA mezcle:

- Decisiones confirmadas.
- Arquitectura objetivo.
- Implementación declarada por Claude.
- Implementación verificada directamente.
- Trabajo pendiente.

Toda fase debe actualizar este snapshot si cambia arquitectura, datos, scoring, seguridad, build, release o deuda crítica.

---

## 2. Convención de evidencia técnica

Cada afirmación relevante debe usar una etiqueta:

| Etiqueta | Significado |
|---|---|
| `CEO_APPROVED` | Decisión de producto o arquitectura aprobada. |
| `REPO_VERIFIED` | Comprobada directamente en código/configuración actual. |
| `TEST_VERIFIED` | Gate reproducido con comando y resultado verificable. |
| `CLAIMED_BY_CLAUDE` | Declarada en un cierre, aún no comprobada de forma independiente. |
| `TARGET` | Arquitectura deseada, no necesariamente implementada. |
| `PROVISIONAL` | Solución temporal permitida bajo condiciones explícitas. |
| `UNKNOWN` | Falta inspección o decisión. |
| `BLOCKED` | No debe activarse/cablearse hasta cerrar prerequisitos. |

No elevar `CLAIMED_BY_CLAUDE` a `REPO_VERIFIED` por repetición textual.

---

## 3. Resumen ejecutivo

| Área | Estado conocido | Confianza |
|---|---|---|
| Madurez | MVP parcial, poco refinado pero considerado bastante sólido por el CEO. | `CEO_APPROVED` |
| Plataformas | Android + iOS. | `CEO_APPROVED` |
| Stack cliente | Kotlin Multiplatform + Compose Multiplatform. | PRD confirmado; verificar versiones y cobertura real. |
| Plataformas mínimas | Android 8/API 26 e iOS 15. | `CEO_APPROVED` |
| Persistencia actual/objetivo próximo | Firebase/Firestore con funcionamiento offline. | PRD; estado real `UNKNOWN`. |
| Auth durante desarrollo | Firebase Auth mediante email sintético/interno. | `PROVISIONAL` |
| Auth de lanzamiento | VPS propio; username + password; email/teléfono opcionales. | `CEO_APPROVED`, diseño detallado diferido. |
| IA Elite | API externa, solo suscriptores; ningún uso de IA en Free. | `CEO_APPROVED` |
| Catálogo | Objetivo de al menos 120 ejercicios completos, 15 por bloque. | `TARGET` hasta auditar catálogo. |
| Presupuesto adicional | 0 € de desarrollo; infraestructura y APIs deben tener control de coste. | `CEO_APPROVED` |
| Equipo humano | CEO único apoyado por Claude, ChatGPT y agentes especializados. | `CEO_APPROVED` |
| Release | Alpha privada ~20 adultos → beta abierta en stores. | `CEO_APPROVED` |

### 3.1 Declaración de honestidad

ChatGPT ha recibido el PRD y un cierre de Slice 6, no un snapshot completo del repositorio en este paquete. Por ello:

- No se afirma que todos los módulos, pantallas o tests existan.
- No se afirma que el build completo esté verde hoy.
- No se afirma que Firestore, auth, sync, seguridad o iOS estén listos.
- El siguiente agente con acceso al repo debe ejecutar la auditoría de bootstrap de la sección 22.

---

## 4. Stack aprobado

### 4.1 Cliente

- Kotlin Multiplatform.
- Compose Multiplatform.
- Lógica compartida en `commonMain` siempre que no dependa del dispositivo.
- Adaptadores `expect/actual` o interfaces inyectadas para capacidades de plataforma.
- Android 8/API 26 o superior.
- iOS 15 o superior.

### 4.2 Cloud durante MVP/beta

- Firestore como persistencia cloud próxima, no como única fuente de funcionamiento.
- Caché/base local para uso offline.
- Sincronización idempotente y tolerante a reconexión.
- Reglas estrictas para evitar acceso cruzado.

### 4.3 Autenticación

- Desarrollo: Firebase Auth con email sintético derivado internamente.
- Lanzamiento: servicio propio en VPS con username + password.
- Email y teléfono opcionales, solo si el usuario decide vincularlos.
- `loginUsername` privado, estable e inmutable salvo migración administrada.
- `displayName` público/semi-público separado y editable.

### 4.4 Inteligencia artificial

- Solo plan Elite de pago.
- API externa con gateway server-side.
- Ninguna clave de proveedor en cliente.
- Free usa planes y feedback deterministas preconstruidos.
- Memoria Elite estructurada, visible, editable y borrable.

---

## 5. Principios arquitectónicos

1. **Offline first:** entrenar no depende de red.
2. **Common-first:** motor, reglas y scoring puro en código compartido.
3. **Domain isolation:** UI, datos y plataforma no contienen verdad cognitiva duplicada.
4. **Append/recompute:** conservar eventos o evidencia suficiente para recalcular proyecciones versionadas.
5. **Idempotency:** reintentar nunca duplica una sesión.
6. **Version everything:** ejercicio, contenido, fórmula, schema, plan y normas.
7. **Fail closed:** si integridad o comparabilidad fallan, entrenar puede continuar pero evidencia oficial no.
8. **Privacy by design:** minimizar, separar finalidades y preferir local.
9. **No hidden AI:** nada del plan Free depende de generación remota.
10. **Cross-platform parity:** no declarar terminada una slice móvil con una plataforma sin resolver.
11. **Observability without surveillance:** medir salud del sistema con el mínimo dato personal.
12. **Reversible migration:** auth, scoring y schema necesitan rollback/compatibilidad.

---

## 6. Arquitectura objetivo por fronteras

```text
┌─────────────────────────────────────────────────────────────┐
│                         Compose UI                          │
│ catálogo · sesión · dashboard · onboarding · ajustes       │
└────────────────────────────┬────────────────────────────────┘
                             │ intents / state
┌────────────────────────────▼────────────────────────────────┐
│                    Application / Use Cases                  │
│ planes · orquestación sesión · perfil · sync · entitlement │
└───────────────┬──────────────────────────────┬──────────────┘
                │                              │
┌───────────────▼────────────────┐  ┌──────────▼──────────────┐
│      Cognitive Domain          │  │       Data Domain       │
│ state machines · generators    │  │ repos · schemas · queue │
│ metrics · eligibility · score  │  │ projections · consent   │
└───────────────┬────────────────┘  └──────────┬──────────────┘
                │                              │
┌───────────────▼──────────────────────────────▼──────────────┐
│                   Platform Adapters                        │
│ clock · storage · network · auth · audio · haptics · sensor│
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│             Firestore / VPS Auth / Elite AI Gateway        │
└─────────────────────────────────────────────────────────────┘
```

### 6.1 Dependencias permitidas

- UI depende de application/domain mediante interfaces.
- Domain no depende de Compose, Firebase ni APIs de plataforma.
- Data implementa repositorios definidos hacia dentro.
- Firebase y VPS quedan tras adaptadores.
- El scoring longitudinal no depende de texto visible.
- Analytics consume eventos aprobados; nunca decide lógica cognitiva.

### 6.2 Dependencias prohibidas

- `commonMain` importando APIs Android/iOS directas.
- UI calculando score o calibración.
- Firestore model convertido en modelo de dominio universal.
- Auth ID usado como display name.
- XP/racha alimentando perfil cognitivo.
- Respuesta de IA escribiendo directamente score, rango o memoria sin validación.
- Last-write-wins sobre historial de sesiones.

---

## 7. Módulos lógicos objetivo

Los nombres físicos pueden diferir; las fronteras deben existir.

```text
mindboost-core
  ids · time · result · versioning · feature flags

cognitive-engine
  exercise contracts · state machines · generators · outcomes

measurement
  metrics · quality · eligibility · calibration · estimators

profile
  block projections · growth · coverage · freshness · global

personalization
  deterministic plan · occupational mapping · adaptation

catalog
  exercise metadata · dossiers · validation states · routing

data
  local repositories · sync queue · Firestore adapters · migrations

identity
  dev Firebase auth · future VPS adapter · recovery · session security

analytics
  service telemetry · optional product analytics · experiments

elite-ai
  entitlement · minimization · gateway · quota · structured memory

ui
  onboarding · training · assessment · dashboard · settings
```

Evitar un rediseño por capas horizontales gigantes. Migrar mediante slices verticales funcionales.

---

## 8. Modelo de ejecución de ejercicio

Contrato objetivo:

```kotlin
data class ExerciseDefinition(...)
data class ExerciseConfig(...)
data class ExerciseState(...)
sealed interface ExerciseAction
sealed interface ExerciseEvent

interface ExerciseEngine {
    fun start(config: ExerciseConfig, seed: Seed): Transition
    fun reduce(state: ExerciseState, action: ExerciseAction): Transition
}

data class Transition(
    val state: ExerciseState,
    val events: List<ExerciseEvent>,
)
```

Propiedades:

- Determinista para config + seed + acciones.
- Reloj inyectado.
- Sin IO.
- Serialización versionada si hay recuperación.
- Golden tests y property-based tests.

---

## 9. Pipeline de sesión y perfil

```text
ExerciseEngine
→ Trial/Session Events
→ CognitiveSessionRun sellado
→ Quality & Eligibility
→ Ingestor por bloque
→ Estimador versionado
→ ModuleContribution
→ Profile Aggregator
→ Block Projection
→ Global/Trend/Feedback
```

### 9.1 Invariantes

- `null` significa ausencia; nunca coalescer a `0`.
- `TRAINING_ONLY` no actualiza perfil oficial.
- Score preliminar no se convierte en oficial por atravesar capas.
- El ingestor no decide que un módulo está físicamente validado.
- El estimador condiciona por dificultad o rechaza mezcla no comparable.
- Perfil y global conservan cobertura, error, frescura y versiones.

### 9.2 Wiring

Cada conexión nueva necesita test end-to-end que demuestre:

- Dato válido llega una vez.
- Dato inválido no llega.
- Duplicado no cambia estado.
- Ausencia no disminuye score.
- Reordenación de sync no cambia proyección.
- Versiones incompatibles no se mezclan.

---

## 10. Slice 6 recibida: CognitiveSessionRun Ingestor

Fuente: `SYSTEM1_SLICE_6_INGESTOR_CONTRACT.md` aportado por el CEO.

### 10.1 Lo declarado

`CLAIMED_BY_CLAUDE`:

- Existe `CognitiveSessionIngestor` puro y no cableado.
- Convierte historial de `CognitiveSessionRun` en contribución/calibración por bloque.
- Excluye sesiones no válidas y propósitos no elegibles.
- Usa `null` para ausencia.
- Usa `normalizedScore` como moneda.
- Reutiliza `CalibrationPolicy`: 5 sesiones válidas y 3 días.
- Mantiene `preliminary = true`.
- Tiene 21 tests.
- Compilación desktop/Android y detekt quedaron verdes.
- Exit gate `/mb`: efficacy, psychometrician y diff auditor aprobaron.

### 10.2 Estado correcto en este paquete

- Contrato: `CLAIMED_BY_CLAUDE`.
- Wiring a perfil vivo: `BLOCKED` y explícitamente no realizado.
- Evidencia reproducida por ChatGPT sobre repo actual: `UNKNOWN`.

### 10.3 Prerequisitos antes de cablear

Los siguientes follow-ups del propio cierre son bloqueantes de wiring oficial:

1. Reemplazar `latest normalizedScore` por estimador robusto de ventana/modelo.
2. Reemplazar confianza basada en CV por error/estabilidad apropiados a escala fija.
3. Reemplazar trend first-vs-last por pendiente/modelo con deadband basado en error.
4. Sustituir bucket UTC por política local/intervalo mínimo aprobada.
5. Introducir señal externa de validación física del módulo antes de `preliminary=false`.
6. Test end-to-end de `null` como ausencia a través del agregador.
7. Verificar invarianza/ajuste por dificultad antes de mezclar scores.
8. Añadir la regla aprobada de al menos 48 horas entre primera y última evidencia.

Hasta resolverlos, no usar ese ingestor para score oficial, rango o claims.

### 10.4 Actualización Slice 6-R3 (2026-07-11)

Fuente: `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R3_REOPEN.md` (+ Decision Log §20.1). El
ingestor `profile/domain/ingestion/` que describía §10.1 fue **BORRADO** (6-R2) y reemplazado por un contrato
**evidence-only** en `core.session.cognitive.evidence`; 6-R3 lo reescribió a **facts-only** bajo la autoridad de
`docs/context_pack/`. Etiqueta global: `CLAIMED_BY_CLAUDE` (los gates Gradle no fueron reproducidos por el agente;
el IDE posee el daemon del worktree) + wiring `BLOCKED`.

- **Contrato facts-only (LANDED):** `ExerciseScaleEvidence` emite ahora solo 4 hechos (`lineage`,
  `observationCount`, `distinctDayCount`, `spanMs: Long?`). ELIMINADOS: `orderedNormalizedScores` (input del
  estimador — máxima prioridad), `cappedObservationCount`, `observationCountProgress`/`distinctDayProgress`,
  `EvidenceQuantityMaturity`, `FreshnessStatus` y los conteos `RankReadiness.{rankSessionCount, distinctDays,
  spanMs, acceleratingSessionCount}`. La comparabilidad pasó a un sealed `ComparabilityNotEstablished`(default) /
  `Incompatible` / `Bridged(version)` (esta última inalcanzable/fail-closed). RT-criticidad: el hardcode
  `{SPEED}` se sustituye por `RtCriticalityRegistry` versionado; metadata ausente ⇒ UNKNOWN ⇒ fail-closed.
  `TRAINING_FREE` completo ⇒ `TRAINING_ONLY`. Barrera anti-reintroducción: extensión del `evidenceBoundaryScanner`
  (Tier-1 tipos `Double`/`Float`/arrays + Tier-2 literales retirados) + fixture de mutación en desktopTest.
- **Fix P0 `CognitiveSessionRunStore` (6R3-P0C-01, revisado APTO):** `subject` obligatorio en construcción del
  store seguro + claves scopeadas por sujeto (`crun:<subject>:…`) + exclusión del `clearNamespace()` masivo del
  eraser (borrado scopeado por `deleteAll()`), corrigiendo el commingling cross-account y el wipe colateral.
- **Wiring: sigue `BLOCKED`.** Nada cableado a perfil vivo / Global Score / Growth Index / rango / UI / Firestore /
  SyncQueue / analytics / cloud. Precondiciones W1–W5 (sellar `SessionEligibility`; purga en sign-out; decisión de
  producto sobre `TRAINING_FREE`; registro ejercicio∈bloque; `CalibrationBlueprint` + equating) + ADRs **ABIERTOS**
  `ADR-SCI-003/004/005/006/007/008` (§19 Decision Log). El contrato se mantiene estrictamente por debajo de esos
  ADRs: emite hechos, no estimaciones.
- **NUEVO P1 `SessionRecordStore`/`SecureSessionRecordStore` (`mindboost_sessions`):** mismo defecto cross-account
  (sin sujeto en la clave `session_record:$sessionId`) en las 8 DIs de módulo; `SessionRecoveryScanner.recover()`
  MUTA filas de cuentas ajenas a ABANDONED en cada track-mount (escritura cross-account viva). Severidad P1
  (payload = solo ciclo de vida, sin score/cognitivo). **Disposición PENDIENTE decisión CEO** (privacy-security
  recomienda slice propio, owner `data-sync-dev`). Ver Decision Log S6R3-003.
- **Deuda de documentación de código (registrada, no editada en este pase):** `MindBoostStorageNamespaces.kt:22-24`
  llama a `Sessions` "Per-user" en su KDoc, lo cual es **INEXACTO** hasta que se resuelva el P1 anterior — corregir
  ese KDoc en el mismo cambio que arregle `SessionRecordStore`.
- **iOS:** NOT VERIFIED en Windows (P1 de paridad, honesto — no un falso "sin P0/P1").

---

## 11. Persistencia local y Firestore

### 11.1 Fuente local

Objetivo:

- La sesión se sella localmente antes de confirmar al usuario.
- Una cola durable sincroniza después.
- La app reconstruye plan e historial básico sin red.
- Esquemas locales tienen migraciones.

Tecnología concreta local: `UNKNOWN`; inspeccionar repositorio antes de decidir cambiarla.

### 11.2 Identificadores

Cada sesión necesita:

- `sessionRunId` globalmente único.
- `userId`/subject interno pseudónimo.
- `exerciseId` y versión.
- `schemaVersion`.
- Inicio/fin.
- Seed/item family según finalidad.
- Idempotency key.
- Estado de integridad/sync.

### 11.3 Sync

Máquina sugerida:

```text
LOCAL_PENDING
→ UPLOADING
→ ACKNOWLEDGED
→ VERIFIED
→ PROJECTED

fallos: RETRYABLE | PERMANENT_REJECTED | CONFLICT_REVIEW
```

- Backoff con jitter.
- Reintentos idempotentes.
- Dedupe server-side.
- Server timestamp no sustituye el tiempo monotónico de ensayo.
- Sesión offline permanece provisional hasta integridad.

### 11.4 Firestore

Antes de beta:

- Reglas con default deny.
- Tests de emulator para acceso cruzado.
- Separación de colecciones por finalidad.
- Índices y límites de coste.
- No guardar secretos ni datos locales declarados.
- Política de retención/borrado.
- Exportación y cascada de eliminación.

---

## 12. Autenticación: transición controlada

### 12.1 Estado provisional de desarrollo

Se permite Firebase Auth con email sintético únicamente si:

- El dominio/formato está reservado al entorno.
- Nunca se intenta enviar correo real.
- La UI no promete recuperación por email.
- No se reutiliza en producción.
- Se marca `DEV_AUTH_ONLY`.
- Los datos son de desarrollo/alpha controlada.

### 12.2 Kill-switch de release

La open beta no puede publicarse si cualquiera es cierto:

- `DEV_AUTH_ONLY = true`.
- Se genera email falso en build de release.
- No existe migración de cuentas.
- No existe recuperación segura.
- `loginUsername` se expone públicamente.
- Sesiones/tokens no tienen revocación y expiración adecuadas.

### 12.3 Auth objetivo, alcance actual

Decisiones cerradas:

- Username + password sin email obligatorio.
- Email/teléfono vinculables voluntariamente.
- Recovery codes.
- Enlace/PIN de recuperación a canal previamente verificado.
- Passkeys como evolución futura recomendable.
- No usar preguntas personales como factor de recuperación.

Sin canal verificado, recovery code o dispositivo/passkey confiable no existe recuperación automática segura. La UX debe advertir el riesgo; soporte no puede saltarse autenticación mediante conocimiento personal.

Detalles VPS — protocolo, framework, almacenamiento de credenciales, rate limits, despliegue y operación — quedan diferidos a un ADR de identidad antes de implementar. Deben seguir estándares actuales y auditoría de seguridad.

---

## 13. Privacidad y seguridad

### 13.1 Clasificación de datos

| Clase | Ejemplos | Política |
|---|---|---|
| Identidad | username, hash, canales vinculados | Backend de identidad, acceso mínimo. |
| Perfil cognitivo | scores, estimaciones, historial | Sensible por impacto; privado por defecto. |
| Eventos de sesión | respuestas, RT, dificultad | Minimizar y separar finalidad. |
| Contexto local | sueño, estrés, energía, fatiga | Local por defecto. |
| Servicio/seguridad | errores, fraude, uptime | Necesario, retención limitada. |
| Analytics opcional | uso para mejorar producto | Consentimiento separado y revocable. |
| Investigación | datasets y outcomes | Consentimiento específico/protocolo. |
| Memoria Elite | objetivos y preferencias estructuradas | Visible, editable, borrable. |

### 13.2 Controles mínimos

- TLS en tránsito.
- Cifrado en reposo del proveedor y local apropiado.
- Secretos solo server-side/CI secrets.
- Logs sin tokens, contraseñas, respuestas sensibles ni prompts completos.
- Autorización por subject, no solo autenticación.
- Rate limiting.
- Rotación y revocación.
- Backups con borrado documentado.
- Export/delete end-to-end.
- Región UE cuando sea viable y contratos de encargado.

### 13.3 Consentimiento

Separar:

- Tratamiento necesario para prestar y proteger el servicio.
- Analytics opcional.
- Investigación opcional.
- IA Elite y transferencia a proveedor.
- Permisos de dispositivo.

Pseudonimizar no convierte automáticamente los datos en anónimos.

---

## 14. Elite AI Gateway

Arquitectura objetivo:

```text
App
→ entitlement + user confirmation
→ MindBoost AI Gateway
→ minimizer / policy / redaction
→ external model provider
→ schema validator
→ allowed-action mapper
→ user preview
→ accepted plan/memory update
```

Requisitos:

- Solo suscripción activa.
- Cuota dinámica y circuit breaker por coste.
- Nunca exponer API key.
- Enviar el mínimo dato.
- No enviar sueño/estrés/fatiga locales sin consentimiento específico.
- Salida estructurada validada.
- La IA reorganiza componentes validados; no inventa ejercicios o fórmulas oficiales.
- El usuario confirma cambios del plan.
- Fallback determinista si falla proveedor.
- Registro de versión de prompt/modelo sin almacenar contenido innecesario.
- Margen unitario positivo.
- Proveedor y modelo detrás de una interfaz estable; ninguna lógica de producto depende del formato propietario directo.
- Benchmark/eval versionado con casos de personalización, seguridad, schema, coste y regresión antes de cambiar modelo o prompt crítico.

---

## 15. Entornos y feature flags

Entornos mínimos:

```text
LOCAL
DEV
ALPHA
BETA
PRODUCTION
```

Flags críticas:

- `DEV_AUTH_ONLY`.
- `OFFICIAL_SCORING_ENABLED` por bloque/versión.
- `PUBLIC_RANKING_ENABLED` — false durante beta.
- `WORLD_CLASS_ENABLED` — false hasta normas representativas.
- `ELITE_AI_ENABLED`.
- `EXPERIMENT_ID` y variante.
- `SENSOR_EXERCISE_ENABLED`.
- `DATA_EXPORT_ENABLED`.
- `ACCOUNT_DELETION_ENABLED`.

Reglas:

- Defaults de release conservadores.
- Flags server-side firmadas o protegidas.
- No usar flags para omitir migraciones.
- Auditar quién/qué versión cambió una flag.
- Testear combinaciones críticas.

---

## 16. Analytics y observabilidad

### 16.1 Dos rutas

**Necesaria para servicio/seguridad:** salud, errores, sync, abuso, coste. Base y retención documentadas.

**Opcional para producto/investigación:** funnels, engagement, experimentos, outcomes. Consentimiento separado.

### 16.2 Eventos de producto

Usar nombres versionados y propiedades minimizadas. Ningún evento genérico debe contener payload cognitivo arbitrario.

### 16.3 SLO/SLI por definir

Antes de beta fijar:

- Crash-free sessions.
- ANR/hangs.
- Sync success y latencia.
- Pérdida/duplicación de sesiones.
- Tiempo de inicio.
- Jank en ejercicios temporales.
- Disponibilidad auth/VPS.
- Errores Elite y coste por usuario.
- Tasa de borrado/export completado.

Los números objetivo son `UNKNOWN` hasta medir baseline y fijar ADR.

---

## 17. Rendimiento móvil

Requisitos:

- Perfilar gama baja Android e iPhone mínimo soportado.
- Evitar allocaciones en loops temporales.
- Precalcular/precargar recursos críticos.
- Separar animación de timing cognitivo.
- Medir frame pacing, no solo FPS promedio.
- Controlar batería, audio y sensores.
- Reducir payloads y lecturas Firestore.
- No bloquear main thread con scoring, generación o sync.

Una tarea de RT degrada a training-only si jank/latencia compromete su interpretación.

---

## 18. Calidad, CI y gates

Pipeline mínimo antes de merge/release:

1. Compilación common/desktop usada para tests.
2. Unit y property tests.
3. Tests de scoring golden.
4. Android compile/test.
5. iOS framework/build/test.
6. Static analysis y format.
7. Tests Firestore emulator cuando aplique.
8. Schema/migration tests.
9. Dependency/secret scan.
10. UI/accessibility smoke tests.
11. Catálogo/dossier consistency check.
12. Diff auditor + efficacy gate.

Los comandos exactos deben vivir en repo y actualizarse aquí. Hoy son `UNKNOWN`, salvo los declarados en el cierre de Slice 6, que no se han reproducido en este paquete.

### 18.1 Prohibición de “green parcial”

Una task test verde no implica build verde. El cierre debe enumerar:

- Comandos exactos.
- Exit code.
- Scope.
- Tests skipped/quarantined.
- Plataforma no ejecutada.

---

## 19. Release engineering

### 19.1 Alpha

- Builds identificables y revocables.
- Crash reporting mínimo.
- Canal de feedback.
- Datos de prueba separados.
- Consentimiento comprensible.
- Rollback/kill switch.

### 19.2 Open beta

Bloqueada hasta:

- Auth temporal eliminada/migrada.
- Android/iOS paridad.
- Export/delete funcionales.
- Política y consentimiento vigentes.
- Crash/sync gates superados.
- Score/ranks etiquetados correctamente.
- No ranking público oficial.
- Al menos ruta offline completa para 8 bloques.
- Store disclosures coherentes.

### 19.3 Producción estable

Se decide por gates, no por fecha. Requiere evidencia de beta, operación sostenible y no P0/P1.

---

## 20. Riesgos técnicos principales

| Riesgo | Consecuencia | Mitigación |
|---|---|---|
| 120 ejercicios con contratos divergentes | Bugs y scores incomparables | Framework + dossiers + tests generados. |
| Fórmulas duplicadas | Doble verdad | Autoridades únicas y versionadas. |
| RT dependiente del dispositivo | Falso progreso | Quality gate, class/bridge o exclusión. |
| Sync offline duplicado | Perfil corrupto | IDs idempotentes + append/recompute. |
| Auth temporal llega a beta | Cuenta irrecuperable/riesgo | Release kill-switch. |
| Firestore permisivo | Fuga cruzada | Default deny + emulator tests. |
| API IA costosa | Margen negativo | Gateway, cuota dinámica, fallback. |
| Código generado por agentes sin wiring | Feature fantasma | Slice vertical + test E2E. |
| Cambios de scoring rompen historia | Confianza perdida | Versionado + dual scoring + migración. |
| Paridad declarada sin iOS real | Beta defectuosa | Gate build/test por plataforma. |

---

## 21. Unknowns que no deben rellenarse por intuición

El primer agente con repo completo debe resolver y documentar:

- Versiones de Kotlin, Compose, Gradle y SDK.
- Module graph real.
- Estructura local DB/cache.
- Estado de Firestore y security rules.
- Cobertura y estado de los 120 ejercicios.
- Estado real de iOS y firma/build.
- CI/CD y gestión de secretos.
- Crash/analytics SDKs.
- Consent model actual.
- Esquemas y migraciones.
- Algoritmo de scoring por bloque.
- Integración real del profile ingestor.
- Tests skipped o inestables.
- Licencias de assets/dependencias.
- Accesibilidad actual.
- Retención y borrado.
- Coste actual de infraestructura.

Resolver `UNKNOWN` requiere evidencia, no texto.

---

## 22. Auditoría de bootstrap obligatoria

Al iniciar la próxima macrofase:

1. Leer `AGENTS.md`, README, Gradle settings y catálogo de versiones.
2. Ejecutar `git status` y preservar cambios del CEO.
3. Generar `rg --files` por módulos relevantes.
4. Dibujar dependencias reales.
5. Localizar auth, Firestore, local storage, scoring, profile, analytics y AI.
6. Contar ejercicios realmente cableados por bloque/estado.
7. Ejecutar gates baseline exactos.
8. Auditar `TODO`, mocks, placeholders y feature flags.
9. Auditar secretos y reglas de acceso sin exponer valores.
10. Comparar repo con este documento.
11. Registrar desviaciones como P0–P3/ADR.
12. Actualizar tabla de estado con commit SHA y fecha.

Salida requerida:

```yaml
snapshot:
  commit: ""
  dirtyWorktree: true|false
  buildCommands: []
  passing: []
  failing: []
  unrun: []
  platformsVerified: []
  exerciseCountByBlock: {}
  criticalUnknowns: []
  blockers: []
```

---

## 23. Plantilla de actualización de snapshot

```markdown
### YYYY-MM-DD — <fase/slice>

- Commit/base:
- Archivos/módulos tocados:
- Cambios de arquitectura:
- Contratos añadidos/modificados:
- Migraciones:
- Flags:
- Gates ejecutados:
- Plataformas verificadas:
- Riesgos abiertos:
- Unknowns resueltos:
- Decisiones/ADR:
- Próximo wiring permitido:
```

---

## 24. Definition of Done técnica

Una slice técnica termina cuando:

- Cruza verticalmente motor/datos/UI cuando su objetivo lo requiere.
- No deja la feature sin wiring ni rutas fantasma.
- Mantiene fronteras y autoridades únicas.
- Incluye migraciones y compatibilidad.
- Pasa tests proporcionales, Android e iOS cuando aplica.
- Funciona offline y sincroniza de forma idempotente.
- Respeta privacidad, seguridad y entitlement.
- Actualiza documentos y flags.
- No quedan P0/P1.
- Toda mejora segura, relevante e in-scope está implementada; lo demás tiene owner, razón y gate.

---

## 25. Regla final

El estado técnico de MindBoost solo es tan fiable como la evidencia reproducible más reciente.

Un resumen convincente no sustituye al diff, el wiring, los tests ni la ejecución en ambas plataformas. Este documento debe volverse más preciso con cada fase y nunca más optimista que el repositorio.
