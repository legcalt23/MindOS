# MindBoost — Exercise Quality Contract (v1, extraído de E1–E8)

> **Propósito.** Que ningún ejercicio (de los ~121 planificados) reinvente la honestidad de medición ni
> herede una grieta. Este contrato es la lista de comprobación OBLIGATORIA de "listo de verdad" para
> cualquier ejercicio nuevo, extraída de lo aprendido construyendo Velocidad Mental (E1–E8). Se lee junto a
> `CLAUDE.md` §2 (rigor charter) y §5 (persistencia). **Regla madre:** un ejercicio "compila y es jugable" NO
> es un ejercicio "listo"; hay cuatro niveles de readiness (§0) y cada uno tiene un gate distinto.

## §0 — Los cuatro niveles de readiness (NUNCA colapsarlos en "cerrado")

| Nivel | Significado | Gate mínimo |
|---|---|---|
| **L1 · Módulo local entrenable** | El ejercicio se juega end-to-end en el dispositivo, mide honestamente y da feedback. | Dominio puro + captura viva + scorer honesto + persistencia local cifrada + tests + copy-safe. |
| **L2 · Listo para perfil/calibración** | Su señal puede alimentar el perfil de bloque / baseline sin mentir. | L1 + banco validado + timebase autoritativo (si RT-crítico) + anti-gaming con cobertura declarada + elegibilidad `profile/calibration` computada + wiring real al agregador. |
| **L3 · Listo para MVP** | Cumple los requisitos del PRD para envío. | L2 + Firestore sync + persistencia completa de campos PRD + recovery ante process-death + Android+iOS runtime QA + analytics. |
| **L4 · Listo para ranking/competición** | Comparable entre usuarios con integridad estricta. | L3 + `rankingEligible` (timebase CLEAN + confianza) + validación física §15 + antifraude reforzado. |

**Nunca** escribir "cerrado" a secas. Escribir "cerrado a L1", "cerrado a L2", etc. Un ejercicio a L1 con
L3 abierto es HONESTO; un ejercicio que dice "cerrado" con Firestore/perfil abiertos es DEUDA ESCONDIDA.

## §1 — Domain model puro (commonMain, sin plataforma)
Modelos `@Serializable` con `schemaVersion`. Cero `Clock.System.now()` (inyectar `EpochClock`). "Desconocido" =
`null`, jamás `0L`/`""`/`-1`. **Ref E8:** `GoNoGoStimulus`, `GoNoGoResponse`, `GoNoGoOutcome`.

## §2 — Trial/event model auditable
Cada resultado por trial es una función PURA de (estímulo × respuesta), con TODAS las clases de resultado
explícitas (correcto/omisión/comisión/anticipación/tardío…), no un booleano `isCorrect` que esconda el modo
de fallo. **Ref E8:** `GoNoGoOutcome.resolve` (7 resoluciones). **Anti-patrón:** forzar el paradigma a un
`isCorrect` RT-tap cuando "no responder" es lo correcto (NO-GO) → mentiría.

## §3 — Summary model persistible
Resumen agregado `@Serializable` con `summaryVersion` (esquema) ⊥ `metricsVersion` (fórmulas). Cada métrica es
`Measured(v)` **o** `Unavailable(reason)` — NUNCA un 0 fabricado. **Ref E8:** `GoNoGoTelemetrySummaryV1`,
`GoNoGoSignal`.

## §4 — Official run adapter (o modelo oficial compatible)
Si el paradigma NO cabe en `MeasurementRun`/`TrialRecord` (RT-tap con `responseUptimeNanos` real), producir un
**run-equivalente neutral** que lleve omisiones/comisiones EXPLÍCITAS; nunca fabricar timestamps ni invertir
`isCorrect`. **PERO** (aprendizaje R3): un adapter SIN consumidor no es integración. Marcarlo
`contributesToOfficialProfile` derivado de un interruptor único (`PROFILE_WIRING_ENABLED`) con criterio EXACTO
documentado para ponerlo en `true`. **Ref E8:** `GoNoGoOfficialRunAdapter`. **Deuda sistémica abierta:** no
existe todavía un `CognitiveSessionRun` unificado que un historial oficial consuma para paradigmas no-RT-tap
(ver §21).

## §5 — Local persistence (cifrada, offline-first)
`SecureKeyValueStore` (Android AES-256-GCM + Keystore; desktop in-memory; iOS Keychain). Solo RESÚMENES, nunca
trials crudos de alto volumen. Idempotente por `userId::sessionId`. **Persistir el resultado SCORED completo**
(índice + categoría + versión + integridad + elegibilidad + conteos crudos + distribución RT), no solo rates:
"reconstruible" debe ser verdad, no aspiración. **Ref E8:** `ExerciseTelemetryStore` (namespace `mindboost_speed`,
prefijo `tele:`), `GoNoGoTelemetrySummaryV1 v1.2.0`.

## §6 — Cloud sync path
Todo record nace `syncStatus = LOCAL_ONLY` con los estados de sync ya modelados (contrato forward). El schema de
Firestore + un **outbox/journal durable local** (write-ahead → mostrar resultado → sync async → recovery al
abrir) es requisito de **L3**, no de L1. Documentar el schema aunque no se implemente. **Deuda sistémica
abierta** para TODO el bloque Velocidad (ver §21).

## §7 — Account deletion coverage
Al añadir un namespace por-usuario, actualizar EN EL MISMO CAMBIO el eraser de `AppGraph` (+ test de cobertura)
y `functions/src/accountDeletion.ts`. Fail-closed. **Ref E8:** cubierto por el clear namespace-wide de
`mindboost_speed` (verificado en `AppGraph`).

## §8 — Profile contribution policy
Gate de lectura (no una bandera persistida) como autoridad de elegibilidad. Debe gatear por: banco validado
(`BankIntegrity.blocksProfileEligibility()`), timebase autoritativo si RT-crítico, anti-gaming con cobertura
adecuada, muestra mínima (`MinSampleGate`). `dataSufficiency` (tamaño) ⊥ `confidence` (estabilidad): jamás
derivar una de la otra. **Ref E8:** hoy DIFERIDA por diseño (§15 físico abierto), `profileEligible` computado
pero `PROFILE_WIRING_ENABLED=false`.

## §9 — Calibration eligibility
Elegibilidad de calibración/baseline es MÁS estricta que la de perfil: exige integridad `CLEAN` + confianza
suficiente. Debe ser un booleano COMPUTADO en el score, no una decisión de UI. **Ref E8:**
`GoNoGoScore.calibrationEligible`. El baseline de 5 sesiones válidas del PRD se construye SOLO con sesiones
`calibrationEligible`.

## §10 — Data-quality / anomaly flags
Diagnostics puros por corrida (patrones sospechosos / medición no fiable) SIN acusar al usuario. La integridad
de temporización es ESCALONADA (`CLEAN`/`MINOR`/`COMPROMISED`), no binaria: un evento aislado de plataforma no
anula toda la sesión (penalización proporcional); sistemático sí (cap fuerte). **El gate de data-quality NO
puede depender de disciplina humana** → el scorer DEBE exigir los diagnostics (parámetro obligatorio, no
`= null`). Input perdido (tap sin timestamp) se CUENTA (`missingOnsetTaps`), nunca se fabrica un RT=0 ni se
oculta. **Ref E8:** `GoNoGoScorer.score(metrics, diagnostics)` (diagnostics requerido), `TimingIntegrityLevel`,
`GoNoGoDiagnostics.missingOnsetTaps`.

## §11 — Anti-degeneracy tests
Toda estrategia degenerada (tap-all, tap-none, adivinar-rápido, robótico, RT imposible, ráfagas de error) DEBE
puntuar bajo, probado con tests. La velocidad NUNCA compensa la precisión. Property/adversarial tests con barridos
(prob. de objetivo, dificultad, distribuciones de RT). **Ref E8:** `GoNoGoScoreTest` (caps + integridad + elegibilidad).

## §12 — Lifecycle policy
Nav-away / back a mitad de corrida: abandonar SIN persistir (una corrida incompleta no es dato). Una corrida
COMPLETADA debe sobrevivir al `dispose` del scope de pantalla (`withContext(NonCancellable)`). **Background del SO
durante un trial temporizado** puede corromper la medición: requiere un hook de lifecycle que invalide el trial y
marque anomalía de sesión → device QA obligatorio (§14). No añadir el hook sin call-site vivo (evitar control
muerto). **Ref E8:** `abort()`, `persist()` NonCancellable; background = device-QA pendiente.

## §13 — Copy/claims scanner
Todo copy (incluidos comentarios y disclaimers en el alcance escaneado) libre del léxico prohibido, incluso
negado. Correr la VARIANTE del source-set tocado (`…Scanner` / `…ScannerCommonTest` / `…ScannerDesktopTest`).
Describir la CONDUCTA de la corrida, no etiquetar a la persona. **Ref E8:** `GoNoGoCopyTest`, scanners verdes.

## §14 — Device QA requirements
La lógica pura en JVM NO cubre: temporización real en dispositivo, UX táctil, jank/frame timing, background,
permisos. Todo ejercicio RT-crítico exige una pasada de QA instrumentada Android **y** iOS antes de L3/L4.
Listar explícitamente qué NO está probado. **Ref E8:** Android runtime / iOS NO probados (declarado).

## §15 — Migration/versioning policy
Añadir un campo a un `@Serializable` persistido ⇒ default para compat de decode. Bumpear `summaryVersion` cuando
cambia el esquema; `metricsVersion` cuando cambian las fórmulas; `scoreVersion` cuando cambia un peso/fórmula del
score. Ejes independientes. **Ref E8:** `GoNoGoTelemetrySummaryV1 1.1.0-mc9d-r→1.2.0-mc9d-r3`, `GONOGO_SCORE_VERSION 1.0.0-mc9d→1.1.0-mc9d-r3`.

## §16 — AI Coach data exposure policy
El AI Coach consume SOLO campos allowlisted del summary/run oficial (rates, flags, score preliminar), nunca
trials crudos ni PII. Ningún sink remoto acepta `Throwable` crudos. Contrato doc-only hasta que exista el sink.
**Ref E8:** campos derivables de `OfficialExerciseRunAdapterResult`.

## §17 — Ranking eligibility policy
`rankingEligible` = la barra más estricta: timebase `CLEAN` + confianza + patrón no-CALIBRATING + §15 físico. Es un
booleano computado; el pipeline de ranking (fuera de alcance hoy) lo consumirá. **Ref E8:** `GoNoGoScore.rankingEligible`.

## §18 — Privacy minimization
Minimizar recolección. Namespaces por-usuario. Sin identificadores estables en exports debug. Logs PII-free.
Aislamiento por usuario probado (nunca cruzar usuarios en lecturas). **Ref E8:** `ExerciseTelemetryStore.forSession`
exige `userId`, clave exacta; test de aislamiento.

## §19 — Golden fixtures
Casos input→output conocidos para las fórmulas (score, métricas, transiciones), con tolerancia para Doubles.
**Ref E8:** tests de score con índices esperados acotados.

## §20 — Property-based / adversarial tests
Además de goldens, generar entradas variadas y verificar invariantes (score ∈ [0,1]∪{null}; degenerado ⇒ bajo;
integridad comprometida ⇒ inelegible). **Ref E8:** tests de tiers de integridad + elegibilidad + tap-all/none.

## §21 — Deuda SISTÉMICA transversal (bloquea L3/MVP del BLOQUE, no L1 por ejercicio)
Esto NO es backlog genérico: son requisitos explícitos del PRD, sin owner-code todavía, que bloquean el MVP:
1. **`CognitiveSessionRun` unificado**: un modelo/almacén de historial oficial que acepte tanto RT-tap
   (`MeasurementRun`) como paradigmas no-RT-tap (Go/No-Go, futuros) SIN mentir. Hoy el run-equivalente de E8 no
   tiene consumidor. **Owner: kmp-architect + data-sync-dev.**
2. **Firestore sync + outbox durable local** para todos los summaries/runs. **Owner: data-sync-dev.**
3. **Process-death recovery** (write-ahead journal). **Owner: data-sync-dev.**
4. **Lifecycle/background guard** instrumentado (Android LifecycleObserver + iOS scenePhase). **Owner: kmp-architect.**
5. **Analytics sink** (`exercise_started/completed/abandoned`, `score_updated`, `session_save_failed`). **Owner: data-analytics.**
6. **Android+iOS runtime QA harness** para ejercicios RT-críticos. **Owner: qa-safety.**
7. **Perfil de bloque + global + calibración de 5 sesiones** que consuma la elegibilidad. **Owner: assessment-diagnostics.**

Ninguno de estos se implementa por-ejercicio: se resuelven una vez, a nivel de plataforma, y TODOS los ejercicios
los heredan. Un ejercicio a L1/L2 con estos abiertos es honesto; declararlos "cerrados" es la grieta que este
contrato existe para prevenir.
