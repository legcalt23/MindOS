# GATE_CATALOG_V5 — Catálogo único de gates del motor de Enfoque

```
DOCUMENT_STATUS = DRAFT · EXECUTION_AUTHORIZATION = NOT_GRANTED
BASE_SNAPSHOT = 27849450 · CANON = context_pack 2.0.2
AUTORIDAD ÚNICA DE: los DOS vocabularios de gates (científico V0–V8 y técnico T-*) + semántica de readiness
de 5 campos. Regla de completitud: TODO código de gate usado en cualquier artefacto V5 está definido AQUÍ;
un código no definido en este archivo es un defecto de bundle (check mecánico pre-freeze).
Estado de TODOS los gates hoy: DESIGNED_NOT_RUN (ninguno verde por planificación; NOT_RUN nunca se reporta verde).
```

## 0. Semántica

Estados de gate: `PASS / FAIL / BLOCKED / NOT_RUN / NOT_APPLICABLE`. Un gate solo se cierra con la evidencia
de SU nivel, registrada (comando+output o dataset+análisis).

**Regla iOS (P1-15, cerrada):** `BLOCKED_IOS` en la verificación de plataforma **impide**: marcar un
ejercicio `TRAINING_READY`, iniciar alpha externa (S6) y cualquier lanzamiento. Vía de desbloqueo a 0 €:
`ios-build.yml` (macos-14, compile-only — solo cubre drift de compilación) + Mac/simulador para tests y timing.
Compile-verde NO es paridad funcional: cada nivel de evidencia iOS se declara por separado
(COMPILE / UNIT_SIM / MANUAL_DEVICE).

### Cierre de readiness en 5 campos (obligatorio en todo informe; heredado de V4, ENDOSADO)

| Campo | Dominio | Juzga |
|---|---|---|
| `PLAN_ARTIFACT_VERDICT` | READY_TO_PLAN_SLICES / READY_WITH_BLOCKERS / NOT_READY | el documento |
| `IMPLEMENTATION_READINESS` | READY / NOT_READY(+razones) | el repo |
| `EXECUTION_AUTHORIZATION` | GRANTED(alcance) / NOT_GRANTED | solo el CEO |
| `PLATFORM_VERIFICATION` | por plataforma: GREEN(evidencia) / NOT_RUN / BLOCKED_IOS / BLOCKED_REPO_BASELINE | ejecución real |
| `NEXT_ALLOWED_ACTION` | una única acción | lo único permitido después |

`READY_WITH_BLOCKERS` NUNCA implica GRANTED ni autoriza wiring.

**CAVEAT-CANON (ex C-02):** los criterios científicos que provienen del research pack están anclados al
Context Pack 2.0.0 (superseded, no byte-recuperable). Antes de promover cualquier claim > C0, re-verificar los
criterios contra el canon 2.0.2 vigente. Aplica a V0, V3, V4–V6.

## 1. Familia científica V0–V8 (semántica verbatim del pack doc 09 §2 — sin debilitar)

| Gate | Pregunta | Claim máximo que habilita |
|---|---|---|
| V0 Especificación | ¿mecanismo y contratos coherentes? | "Diseñado para practicar…" |
| V1 Técnica | ¿se ejecuta/reconstruye igual y falla cerrado? | ningún claim cognitivo |
| V2 Humana/alfa | ¿se entiende, tolera y usa? | ningún claim de eficacia/psicometría |
| V3 Medición/beta | ¿fiable, comparable, estructuralmente válida? | score provisional interno |
| V4 Novel form | ¿supera familiaridad de ítems? | claim de formas nuevas específico |
| V5 Near transfer | ¿transfiere a tarea independiente cercana? | claim estrecho near-transfer |
| V6 Everyday/far | ¿cambia demanda real? | claim contextual específico |
| V7 Normas/ranking | ¿compara justamente a población? | percentiles/ranking |
| V8 Post-release | ¿se mantiene bajo drift? | mantener o retirar claim |

Referencias de precondición se escriben **"requiere V_n PASSED"** — jamás un pseudo-código `G-Vn`.
Los códigos de V4 `G-A`, `G-DR`, `G-S0`, `G-S2`, `G-SCI-00N`, `G-V1/2/3` quedan **RETIRADOS**; su reemplazo
exacto: G-A→`T-PLAN` · G-DR→`T-DR` · G-S0→`T-S0` · G-S2→`T-ADV` (SIN mapeo a V3 — prohibido) ·
G-SCI-00N→`T-SCI-n` · G-V1/2/3 (huérfanos)→"requiere V1/V2/V3 PASSED".

## 2. Familia técnica T-* (gates de slice; NUNCA se mapean a un V-gate)

**Prohibición central (P1-4):** un gate técnico jamás equivale a un escalón científico. En particular
`T-ADV` (suite adversarial sintética) autoriza EXPOSICIÓN/wiring técnico; V3 es evidencia EMPÍRICA humana.
Conflarlos permitiría reclamar validez desde tests sintéticos.

| Código | Qué juzga | PASS exige | FAIL ⇒ |
|---|---|---|---|
| `T-PLAN` | el artefacto de plan (5 campos: PLAN_ARTIFACT_VERDICT) | bundle V5 congelado + manifiesto + última revisión posterior al último cambio | REOPEN del plan |
| `T-DR` | design review (CEO); precede a TODA ejecución | aprobación expresa del CEO sobre el bundle exacto (hash) | nada se implementa |
| `T-BASE` | baseline reproducible del repo (R0-exit) | outputs reales de los 5 comandos Gradle (CEO los ejecuta) + worktree conocido | slices bloqueadas; estado BLOCKED_REPO_BASELINE |
| `T-S0` | contención del score v1 — **precondición de EXPOSICIÓN de cualquier superficie Focus a usuarios** | flag OFF ocultando las 3 superficies + ruta arquetipo cortada + historial intacto + test-oráculo ROJO documentando el porqué + rollback probado (flag ON restaura) | ninguna exposición; alpha bloqueada |
| `T-S1` | contratos y ADRs de S1 | ADR-FOCUS-PURPOSE/-EVIDENCE/-DATA/-SESSION-STATE/-MIGRATION decididos y registrados en 09 §22; grieta `mayContributeToProfile` cerrada; namespace+eraser+coverage-test especificados | S2+ no arranca |
| `T-ADV` | **suite adversarial anti-gaming — BLOQUEANTE** | oráculo A1–A6 verde para el scorer descriptivo del tracer (masher/waiter/side-bias/cherry-picker/lower-is-better + mitad discriminante A4) | NO-GO: ningún número se expone; el tracer sigue sin feedback numérico |
| `T-S3` / `T-S4` | vertical del Tracer A / B completo | tutorial+práctica+sesión+cierre+offline+pause/resume/exit+process-death+a11y (48dp/44pt)+feedback C0 neutral+frontera de dependencias verde+eventos locales | el tracer no se declara INTERNAL_TECHNICAL_TRACER |
| `T-S5` | V1 técnico + paridad | Android GREEN con evidencia + iOS ≥ UNIT_SIM + datos/lifecycle/migración/export-delete + presupuestos de rendimiento + red-team técnico | BLOCKED_IOS o NOT_READY; TRAINING_READY imposible |
| `T-S7` | escalado del catálogo | por ejercicio nuevo: dossier + generador determinista + T-ADV propio + frontera verde | el ejercicio no entra al bloque |
| `T-S8` | measurement/shadow | journal solo si ADR-FOCUS-DATA lo aprobó; scorers rivales SOLO en sombra; simulación+property/golden verdes ANTES de tocar datos humanos | measurement no arranca |
| `T-SCI-n` | la barrera MECÁNICA del requisito científico FOC-SCI-n existe y falla cerrado | test/scanner específico verde | wiring del requisito bloqueado |
| `T-REL` | release hardening (S10) | estabilidad+observabilidad+rollback+soporte+claims permitidos auditados | sin release |

**Semántica de `T-DR` [R6-V5 P2-5]:** T-DR es POR SLICE — cada slice requiere aprobación expresa y puntual del
CEO sobre SU alcance antes de ejecutarla; la aprobación del plan completo (T-PLAN→T-DR inicial) autoriza
únicamente R0. Ninguna aprobación se generaliza a la slice siguiente (coherente con GOV-013).

**Aclaración de `T-S1` [R6-V5 P1-6]:** su condición de PASS es que los contratos ratificados estén
**IMPLEMENTADOS en código (tipos + wiring de namespace/eraser en AppGraph + coverage-test), sin UI** — no solo
especificados. El eraser toca infraestructura compartida (AppGraph): su diff se revisa con privacy-security.

### 2.1 Hitos y decisiones NO-gate (definidos aquí para completitud de vocabulario [R6-V5 P0-2])

Tokens que la matriz de requisitos usa en celdas de gate y que NO son gates de este catálogo sino HITOS de
estudio o decisiones: `DPIA` (evaluación de impacto — con ADR-FOCUS-DATA/ADR-DATA-002) · `Protocolo etico` y
`Threat model` (artefactos de S6/S11, producidos en su slice) · `R-24` (hito de retención/seguimiento del pack
para estudios longitudinales — S11) · `Presupuesto/equipamiento (hand-off founder)` e `inventario de
dispositivos` (inputs del CEO, no gates) · `Etica/DPO + consent` (= decisión propietario+DPO de
ADR-FOCUS-DATA + consentimiento por finalidad) · referencias `FOC-*-nnn` en celdas de gate = DEPENDENCIA de
otro requisito, no un gate. Ninguno de estos tokens puede usarse como exit-gate único de una slice.

## 3. Estados de madurez de un ejercicio (vocabulario de catálogo)

`SPEC` → `INTERNAL_TECHNICAL_TRACER` (máximo alcanzable antes de paridad; interno, sin usuarios externos) →
`TRAINING_READY` (exige T-S5 PASS con iOS no-BLOCKED) → `MEASUREMENT_CANDIDATE` (exige V3 en curso) →
`OFFICIAL_MEASURE` (exige V3 PASSED + ADR-FOCUS-SCORING decidido). `RESEARCH_ONLY` y `RETIRED` son terminales
laterales. La exposición EXTERNA de cualquier estado exige V2 en curso o superado (S6); C0 copy siempre.

### 3.1 Estado de CONJUNTO (eje distinto — añadido 2026-07-20, S2)

`PORTFOLIO_APPROVED` — **portfolio y tracers ratificados por el CEO con revisión científica registrada;
NINGÚN ejercicio implementado ni jugable.**

Este estado NO pertenece a la escalera de §3 y no se compara con ella: la escalera describe la madurez de UN
EJERCICIO, y esto describe la ratificación de un CONJUNTO. Son ejes ortogonales. Un portfolio puede estar
`PORTFOLIO_APPROVED` mientras sus 18 ejercicios siguen todos en `SPEC` — que es exactamente el estado al
cerrar S2.

**Se añade como corrección de un defecto de bundle**, no como vocabulario nuevo por conveniencia: el plan
(`FOCUS_ENGINE_MASTER_PLAN_V5.md:254`) declaraba que S2 alcanza `PORTFOLIO_APPROVED`, pero el token no estaba
definido en ninguna parte (una sola ocurrencia en todo el repo, la del propio plan). Por la regla de
completitud de §0 de este catálogo, un estado referenciado y no definido es un defecto. Registrado en
`09_DECISION_LOG.md` §20.4.1 como `FIX-FOCUS-S2-PORTFOLIO-APPROVED`.

**Overclaim a evitar:** alcanzar `PORTFOLIO_APPROVED` no implica NADA sobre implementación. Confundirlo con
`TRAINING_READY` sería afirmar que hay algo jugable cuando no lo hay.

## 4. Tabla gate → slice (resumen; el detalle vive en el plan §5)

| Slice | Entry gates | Exit gates |
|---|---|---|
| R0 | — | `T-BASE` |
| S0 | `T-BASE` | `T-S0` (spec completa; ejecución tras T-DR) |
| S1 | `T-S0` spec + `T-DR` | `T-S1` |
| S2 | `T-S1` | revisión científica (efficacy) + selección de tracers registrada |
| S3 | S2 + `T-DR` | `T-S3` + `T-ADV` |
| S4 | `T-S3` | `T-S4` + `T-ADV`(B) |
| S5 | `T-S4` | `T-S5` (= V1 técnico) |
| S6 | `T-S5` + reclutamiento CEO | V2 (evidencia humana formativa) |
| S7 | `T-S5` | `T-S7` por ejercicio |
| S8 | `T-S1`(DATA) + `T-S5` | `T-S8` |
| S9 | `T-S8` + datos repetidos reales | V3 + ADR-FOCUS-SCORING propuesto |
| S10 | V2 + `T-S5` | `T-REL` |
| S11 | V3 | V4–V8 (programas separados, preregistrados) |
