# S5_R3_EVIDENCE_CAPSULE — Focus V5 · Paso 7 (evidencia inmutable de la reconciliación R3)

```
ROLE: EVIDENCE (no es canon paralelo; las transiciones de estado viven en MINDBOOST_AUDIT_LEDGER.md)
GENERATED_BY: TASK-2026-07-22-FOCUS-V5-S5-R3-PASO7 · HEAD 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5
CEILING: R3 CANDIDATA (NO FROZEN_FOR_S5_R3) · MOUNT BLOCKED · EXTERNAL_AUDIT PENDING
```

> Cápsula que fija los HECHOS de Paso 7 para que el ledger, los docs y la auditoría externa citen una
> misma fuente. Companion machine-readable: `S5_R3_EVIDENCE_CAPSULE.json`. La cápsula PROYECTA hechos;
> no excede lo que los docs fuente afirman (proyección-no-excede).

---

## 1. Claim registry (afirmación → evidencia → etiqueta)

| Claim | Evidencia | Etiqueta |
|---|---|---|
| La R2 (`S_full`-o-`Absent`/`mg-1`) es no-monótona con el scorer real | contraejemplo §4 (30H/10M/10FA/30CR; D1→Absent, D2→Present) | `VERIFIED_PRIMARY` (reproducido con la fórmula exacta) |
| Elegibilidad temporal debe ser OUTCOME-INDEPENDENT (D-R3-01) | `S5_REOPEN_STATUS §0.2` | `VERIFIED_DERIVED` (decisión registrada) |
| El vertical temporal es `CONTRACT_INERT_NO_WIRING` | focus2 fuera del paquete; `mindboost_focus2` vacío; barrera no-mount + `Focus2DependencyBoundaryTest` | `VERIFIED_PRIMARY` (S3/S4 checkpoints) |
| Bundle V5: 13/16 fieles, 2 birth-defects, INPUTS recuperable en worktree | verificación 16/16 §5 | `VERIFIED_PRIMARY` (medición directa) |
| Gate 6/6 (entrada) + 7/7 (salida) APTO de la R3 | **`MISSING_PRIMARY_EVIDENCE`** — el tracker `scratchpad/S5_R3_lens_integration.md` NO está presente (ver §2/nota honesta) | `NOT_VERIFIED_IN_PASO7` |
| iOS de la capa temporal | `BLOCKED_IOS` (sin macOS en este host) | `NOT_CHECKED` |
| Físico / device matrix | `NOT_RUN` (S6, diseño futuro) | `NOT_RUN` |

**Nota de honestidad (crítica).** El plan original de Paso 7 (guardarraíl §0.2) exige, ANTES de escribir
"7/7 APTO", confirmar el marcador en `scratchpad/S5_R3_lens_integration.md`. Ese tracker **no está presente
en este árbol** (`scratchpad` es efímero y no sobrevivió). Por tanto Paso 7 **NO afirma "7/7 APTO" como
hecho verificado**: lo registra como `MISSING_PRIMARY_EVIDENCE`. Lo que SÍ está probado de primera mano es
la **falsación de la R2** (§4) — que DOMINA cualquier APTO — y la separación DECISION/IMPLEMENTATION/
VALIDATION. La R3 se entrega como **CANDIDATA a auditoría externa**, no como gate-passed-verificado. Esto es
más conservador que el plan y evita el defecto que el propio charter prohíbe (afirmar un gate sin su registro).

## 2. CORRECTION_MANIFEST (reconciliación documental; oráculo por fila)

| # | Doc | Corrección | Oráculo (cómo se verifica) |
|---|---|---|---|
| C1 | `S5_REOPEN_STATUS.md` | cabecera + §0.4 dejan de decir "gates BLOCKED por límite de sesión"; declaran R3 CANDIDATA por auditoría externa pendiente (no por bloqueo) | grep `BLOQUEAD|límite de sesión|reset 22:50` = 0 hits en cabecera/§0.4 fuera de citas |
| C2 | `S5_REOPEN_STATUS.md §7` | append línea 2026-07-22: Paso 7 reconciliación en curso; falsación domina; "7/7 APTO" = MISSING_PRIMARY_EVIDENCE | §7 contiene la línea fechada 2026-07-22 |
| C3 | `09_DECISION_LOG.md` | nueva sección de decisión R3: falsación + D-R3-01..04 + D-P7-A(=D) + D-P7-B(=ADR_REGISTER+CLAUDE_MD+SCANNER); `SUPERSEDES` explícito | la sección existe con los 6 tokens de decisión |
| C4 | `05_TECHNICAL_CURRENT_STATE.md` | capa temporal = construida NO montada; steps 5–14 = IMPLEMENTATION PENDING; API 29 intacto | grep confirma no-mount + PENDING; `android-minSdk = "29"` sin drift |
| C5 | `ADR_REGISTER_V5.md` | fila temporal `FROZEN_FOR_S5` → `REOPENED_AFTER_EXTERNAL_AUDIT / R3 CANDIDATA`; + marcador del índice; aviso UNTRACKED del ADR | la fila y el marcador están presentes |
| C6 | `CLAUDE.md` | bloque de discoverability con el marcador del índice (D-P7-B) | `CLAUDE.md` contiene el marcador exacto |
| C7 | `CURRENT_AUTHORITY_INDEX.md` (nuevo) | índice de autoridad + disposición GATE_CATALOG + VOID_PROVENANCE | único `AUTHORITY_INDEX_ROLE: CURRENT`; scanner PASS |
| C8 | `PROVENANCE_INCIDENT_V5_BUNDLE.md` (nuevo) | forense 16/16 + 2 birth-defects + custodia D | tabla A/B/C + INPUTS recuperable |
| C9 | `MINDBOOST_AUDIT_LEDGER.md` | 2 findings (0047/0048) + evento + checkpoint + resello §0 | `node scripts/validate-audit-ledger.mjs` = LEDGER OK |
| C10 | plan + `GATE_CATALOG` barrido de claims falsos (T5) | NO se edita GATE_CATALOG (denylist/custodia D); el barrido se registra como resultado (§6) | resultado del barrido en el status §7 |
| C11 | scanner `validate-authority-entrypoints.mjs` (nuevo) | discoverability ejecutable (D-P7-B) | `--self-test` OK + run real PASS |
| C12 | `S5_R3_P7_HANDOFF.md` (nuevo) | handoff a auditoría externa con hashes finales | referencia hashes finales + blockers |

## 3. Gate matrix (evidencia = ledger checkpoints)

`T-BASE/T-S0/T-S1/T-ADV/T-S3/T-S4 = PASS` · `T-S5 = NOT MET (R3 en reapertura; iOS BLOCKED; físico NOT_RUN)`
· `T-S7/T-S8/T-SCI-n/T-REL = NOT_RUN` · `V0–V8 = DESIGNED_NOT_RUN`. `MOUNT = BLOCKED` (`MB-FOCUS-0026/0028/0035/0042`).

**Calificador (no leer los PASS aislados):** los PASS son al techo **INTERNAL_TECHNICAL_TRACER** (JVM + Android),
con **iOS NOT EXECUTED** en todos los checkpoints que los respaldan; `T-S5` (V1 técnico + paridad, que incluye
iOS ≥ UNIT_SIM + físico) es precisamente lo que NO se ha alcanzado. El PASS de un T-gate técnico NO es un escalón
científico (V0–V8 siguen `DESIGNED_NOT_RUN`, ningún claim > C0).

## 4. Contraejemplo de la falsación R2 (valores del scorer real, triple-verificados)

Fórmula exacta del scorer real (`focus2/domain/scoring/DescriptiveScorer.kt` + `Probit.zScore`, Acklam;
`Z_95 = 1.959963984540054`; corrección loglinear de Hautus: `+0.5` por conteo, `+1.0` por total).
`d′ = z((hits+0.5)/(signal+1)) − z((FA+0.5)/(noise+1))`. FA=10, CR=30 fijos (noise=40, F=10.5/41=0.256098).

| Escenario | Celda | H corregida | d′ (scorer real, 6 dp) | Gate R2 §6.1 |
|---|---|---|---|---|
| `S_full` | 30H/10M/10FA/30CR (signal 40) | 30.5/41 = 0.743902 | **1.310847** | referencia |
| `D1` (excl. 5 misses) | 30H/5M (signal 35) | 30.5/36 = 0.847222 | **1.680016** | **Absent** (1.680016 > 1.310847) |
| `D2` (excl. 5 misses + 15 hits) | 15H/5M (signal 20) | 15.5/21 = 0.738095 | **1.292908** | **Present(S_full)** (1.292908 ≤ 1.310847) |

D2 admisible: signal 20 = `MinSampleGate.DPRIME.nMinPerCell`; exclusión 20/80 = 25% < 50% backstop, no
diferencial. **Más exclusión técnica (D1→D2) ABRE el gate que menos exclusión cerraba** ⇒ `mg-1` FALSADO.
Margen D2→Present = 0.017939 (estrecho pero ~18× el ruido del probit ~1e-3, estable bajo todo redondeo).

**Independencia real (NO "dos cálculos" de la misma fórmula):** el número domina y es `VERIFIED_PRIMARY`,
triple-verificado por TRES implementaciones de probit independientes que coinciden a ≥5 dp — (1) `Probit.zScore`
del repo, (2) Acklam en Node (sesión principal), (3) Cody-erf + refinamiento de Halley cotejado con Newton-sobre-Φ
(lente psicométrica del reaudit) — y por DOS bases independientes: 30/10/10/30 (arriba) y 60/20/20/60
(S_full = 1.329617), ambas produciendo la no-monotonía. Nota de precisión: rondas previas imprimieron
1.3107/1.6805/1.2933 (mal-redondeados); los valores canónicos son los de la tabla (ADR §R3-2 ya los llevaba a
plena precisión). Discrepancia entre implementaciones > 1e-2 ⇒ STOP (no ocurre).

## 5. Verificación 16/16 del bundle (detalle en `PROVENANCE_INCIDENT_V5_BUNDLE.md`)

13 OK · 3 MISMATCH = { GATE_CATALOG (birth), PORTFOLIO (birth), ADR_REGISTER (edición worktree legítima) }.
INPUTS worktree == sello (recuperable, EV-R42-03). Birth-defects irrecuperables = **2**
(`NOT_FOUND_IN_LOCAL_GIT_OBJECT_DATABASE`; CI/remoto/backups = `NOT_CHECKED`).

## 6. Barrido de claims falsos (T5) — resultado READ-ONLY (conteo real registrado)

**Resultado literal del barrido** (grep read-only 2026-07-22 sobre `FOCUS_ENGINE_MASTER_PLAN_V5.md` y
`GATE_CATALOG_V5.md`, términos `commit real|onset real|NANOS_NATIVE|desbloquea|entregad|export entregado|
vertical entregad|FROZEN_FOR_S5[^_R]`): **0 hits de claim falso** en ambos. Las 3 apariciones de "exactitud" en
el plan son la negación honesta ("precisión de timestamp ≠ exactitud") o descripción del scorer (exactitud/d′
por condición), no un claim inflado.

`GATE_CATALOG_V5.md` y `FOCUS_ENGINE_MASTER_PLAN_V5.md` están en DENYLIST (custodia D: no se editan) — razón
SEPARADA de "no contienen claims falsos" (el barrido de arriba es lo que prueba la ausencia; la denylist es por
qué no se tocan aunque la tuvieran). Los claims temporales inflados de la R2 viven en el **ADR temporal**
(editable, R3) y en el status, corregidos ahí; el plan/catálogo NO se tocan (su re-sellado es cierre real de S5).

**RESIDUO CONOCIDO (registrado, no un claim falso):** `GATE_CATALOG_V5.md` línea 9 dice "Estado de TODOS los
gates hoy: DESIGNED_NOT_RUN", heredado del snapshot congelado `BASE_SNAPSHOT=27849450` (anterior a S0–S4). Esto
INFRA-declara (contradice los PASS T-BASE/S0/S1/ADV/S3/S4 del ledger) — dirección opuesta a la inflación, así que
el grep de T5 no lo captura. Disposición: NO se edita el catálogo congelado (custodia D); el estado LIVE de cada
gate se lee en `CURRENT_AUTHORITY_INDEX.md §4` (que re-deriva del ledger). Flag añadido en el índice §4.2 y en
`S5_REOPEN_STATUS §7`.

## 7. Findings → ledger (matriz)

| Finding | Invariante | Estado | Owner | Gate de cierre |
|---|---|---|---|---|
| `MB-FOCUS-0047` | gate `S_full`-o-`Absent`/`mg-1` no-monótono; elegibilidad performance-dependent; retirado como gate | OPEN | psychometrician / cognitive-engine-dev | gate retirado en código + elegibilidad outcome-independent + test "más exclusión no invierte Absent→Present" |
| `MB-FOCUS-0048` | R2 declaró done/persistido para código `DESIGNED_NOT_RUN` (marker→proxy, revocación focus2 GAP-1/GAP-2, versiones/tertiles/`TemporalCapture`/retención); R3 separa DECISION/IMPLEMENTATION/VALIDATION | OPEN | cognitive-engine-dev / data-sync-dev | steps 5–14 implementan cada ítem con los 3 ejes separados y verificados |

Dedupe probado: 0047 (falsación del gate) y 0048 (inflación de estado + wiring pendiente) NO comparten
invariante+owner+fix+gate ⇒ IDs separados (no se fusionan). Ninguno reutiliza un ID (máx previo = 0046).

## 8. Pre-state + attestation

`RUN_ID = 196628b2-7fd1-4940-92b5-d4fb7693e735` · `HEAD = 8a06b9ac…` · `head_tree = 1598aecd…` · stash 0 ·
drift = 0 (targets == referencia 2026-07-22). Backups de los 7 canónicos existentes en
`${OPS}/backup` con restore PROBADO (hash idéntico). Ledger pre-Paso7 = `265e4b7e…` (353989 B), sello §0
`8526b509…`. Los outputs forenses (16/16, 0047/0048, contraejemplo) son `VERIFIED_PRIMARY` capturados en el
JSON companion.
