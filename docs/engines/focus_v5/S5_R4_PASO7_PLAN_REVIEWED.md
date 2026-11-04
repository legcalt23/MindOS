# S5 R4 · PASO 7 — RED-TEAM DE LA R3 + PLAN R4 (borrador candidato, NO ejecutado)

> Mandato: «RED-TEAM FINAL Y RECONSTRUCCIÓN R4 DEL PASO 7» (CEO, 2026-07-22). Esta invocación fue
> READ-ONLY sobre el repo; el ÚNICO archivo escrito es este borrador. `EXECUTE_STEP_7 = DENIED`.
> Ubicación elegida: `docs/engines/focus_v5/` (durable, no-canónica — donde viven los planes
> candidatos: `S5_EXECUTION_PROPOSAL`, `S5_R3_PASO7_PLAN`) en cumplimiento de P0-R4-12; el
> scratchpad Temp queda descartado como base autorizable.
> PLAN_SHA256 de este archivo: se reporta FUERA de él (un archivo no contiene su propio hash).
> Ejecución futura SOLO con: `EJECUTAR_PASO7_R4 PLAN_SHA256=<64hex> CUSTODY_DECISION=<id> RUN_ID=<uuid-no-usado>`.
>
> Etiquetas de evidencia: `VERIFIED_PRIMARY` (bytes/output primario de esta invocación),
> `VERIFIED_DERIVED`, `STATIC_SEARCH_ONLY`, `INFERENCE`, `PROPOSAL`, `UNKNOWN`,
> `MISSING_PRIMARY_EVIDENCE`, `CONTRADICTED`.

---

## 1. VEREDICTO EJECUTIVO SOBRE LA R3

**`NO_GO_TO_EXECUTE` — coincide con el auditor externo: requiere esta R4.** La R3 (mi propio
artefacto) acertó en el diagnóstico central (scopes mezclados, gate no ligado, diseño≠implementación,
custodia rota) pero su PROTOCOLO conservaba defectos capaces de fabricar certeza:

- **P0 confirmados en la R3** (adjudicación completa en §3): cadena de evidencia con circularidad
  posible (A.1/A.2), retro-binding epistémicamente inválido (A.3/A.4), gate de allowlist IMPOSIBLE
  sobre el pre-state real (B.13 — el árbol tiene 13 tracked-M + ~20 untracked de código ANTES de P7),
  sin transacción global (B.17), autorización replayable (B.21), packet que prometía «commit/tree
  exactos» para bytes M/U fuera de HEAD (E.41), y tricotomía de custodia incompleta (C.25).
- **Hallazgo NUEVO de esta re-derivación (S-01, P0): la R3 infra-midió la brecha de custodia.**
  La verificación completa de las 16 entradas del manifiesto contra HEAD da **3 MISMATCH, no 1**:
  `GATE_CATALOG_V5.md`, `EXERCISE_PORTFOLIO_V5.md` e `INPUTS_SHA256.txt` (el propio ancla de
  procedencia de inputs). Los tres se commitearon UNA sola vez (`15cb12a`, el commit del freeze) y
  los bytes sellados **no existen en el object store de git** (búsqueda exhaustiva por tamaño +
  sha256 de candidatos): `RECOVERY = NOT_FOUND`. `LEGACY_MANIFEST_VALIDATES_TREE = NO` queda
  MEDIDO, no narrado.
- **P0-R4-12 re-adjudicado PARTIAL con suerte:** el PC se apagó tras entregar la R3; TODO el
  scratchpad sobrevivió al reinicio (R3 re-hasheada byte-idéntica `25dfe9ff…`). La pérdida no
  ocurrió; el riesgo estructural era real. Esta R4 vive en ruta durable.
- **Próxima acción permitida:** CEO decide D-P7-A (recomendación: **Opción D**, §7) y emite el
  mandato ligado a hash+custodia+run-id. Nada de este documento está ejecutado.

---

## 2. SOURCE & EVIDENCE MANIFEST RE-DERIVADO (2026-07-22, post-reinicio)

Capturado con comandos read-only esta invocación. `VERIFIED_PRIMARY` salvo nota.

**Estado git:** rama `product/focus2-v5` (no protegida ✓), HEAD
`8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5`, upstream `+0 -0`, stash VACÍO. Dirty-tree: 13
tracked-M (3 docs: 05, 09, register · 8 código propio S5 · 2 iosMain ajenos) + untracked: 4 docs
S5 + ~20 archivos de código `core/timing`/`focus2` + 2 planes (R3 en Temp, este R4). Idéntico al
pre-shutdown → las lecturas de código de la invocación anterior siguen ligadas a este árbol
(`VERIFIED_DERIVED`).

| Artefacto | SHA-256 (hoy) | Bytes | Supervivencia / delta |
|---|---|---|---|
| `scratchpad/S5_R3_PASO7_PLAN_REVIEWED.md` (Temp) | `25dfe9ff59124630a2bccc9c6a11269ccf68f0fe1717b0dbe7969c0347b85e49` | 65 402 | **SOBREVIVIÓ** al reinicio, byte-idéntico |
| `scratchpad/S5_R3_lens_integration.md` (Temp) | `ee8761a090a02d74042f012f559cd6ad676c5f2a86dc23e689be1aa4a17254d9` | 29 339 | SOBREVIVIÓ; sigue efímero (copiar a archive en R4-2) |
| `ADR_FOCUS_TEMPORAL_QUALITY.md` | `d6941396ac941db2ee88cc44daaab9f2fa5d01d239e1ebd9bdc4d2c928e7e64e` | 93 870 | sin cambio; hash pre-corrección sigue NO REGISTRADO |
| `S5_REOPEN_STATUS.md` | `502f8ff8b94069bbedfdf8996f9322cd7e89af2e67b04832291a13f99ec3a1ed` | 49 557 | sin cambio; §0.4/§0.5 STALE |
| `S5_R3_PASO7_PLAN.md` (plan viejo) | `6f6b9354a00f817093b90fc35ab2807dafa4366fc496b64cbf282d8a4444fdee` | 20 298 | evidencia, intacto |
| `05_TECHNICAL_CURRENT_STATE.md` | `cca93e829d0632b5256cc1ca6c1066a79f43c2807a430eb645b5b5c9f22f30ec` | 30 042 | sin cambio |
| `09_DECISION_LOG.md` | `5c9d2005a23fc7601bc4f4c5727ea8b624e166c789eb1a9492130c14da0a219a` | 86 018 | sin cambio |
| `ADR_REGISTER_V5.md` (worktree) | `8320d28f73a2881363c1374fb41416a26746bbee58f8a0a9da14f7568cca4972` | 17 584 | sin cambio; HEAD=manifest `d77a9df9…` |
| `MINDBOOST_AUDIT_LEDGER.md` | `265e4b7eb1fffcd4ad6a02b1dbf1e2bf715624c96d5799a5fd0bcf08ca867b45` · sello §0 `8526b509…` (`:85`) | 353 989 | tracked LIMPIO, sin cambio |

**Verificación COMPLETA del manifiesto V5 contra HEAD (16/16 entradas, nueva):**

| Resultado | Entradas |
|---|---|
| OK (13) | ADR_REGISTER, BASELINE_EVIDENCE, FOCUS_ENGINE_MASTER_PLAN, REDTEAM_TEST_MATRIX, REQUIREMENTS_MATRIX, REVIEWS, agent_outputs/01..06, tools/freeze_v5.cjs |
| **MISMATCH (3)** | `EXERCISE_PORTFOLIO_V5.md` (head=`cc1f684e…` ≠ sellado `dba3d15c…`/16 693 B) · `GATE_CATALOG_V5.md` (head=`8c4adeec…`/10 485 B ≠ sellado `02090ca6…`/9 249 B) · `INPUTS_SHA256.txt` (head=`8d5a0fda…` ≠ sellado `8f5f0609…`/2 220 B) |

**Recuperación forense (ejecutada, read-only):** `git log --all` de los 3 → un ÚNICO commit
(`15cb12a`) en toda la historia. Object store completo barrido por tamaño: 0 blobs de 9 249 B,
0 de 16 693 B; 2 blobs de 2 220 B examinados y EXCLUIDOS por sha256 (`65d1fbb7…`, `4391a0c4…`).
**Los bytes sellados de los 3 archivos son irrecuperables desde git: `NOT_FOUND`.**

**Evidencia primaria del gate R3:** outputs de lente = ficheros `.output` de sesión + transcript
`.jsonl` (fuera del repo) + tracker (Temp). `MISSING_PRIMARY_EVIDENCE` para: prompts exactos por
lente, input-hash por lente, hash pre-corrección del ADR. Parcialmente recuperable del transcript
(R4-2 lo adjudica). Los `agent_outputs/01..06` in-repo son era S1–S4, no R3 (`VERIFIED_PRIMARY`).

**Conversación adjunta (lado auditor):** termina en R0; el mandato duplicado que cita = `UNKNOWN`
desde el repo (evidencia secundaria del CEO; se acepta como reportado). Cierres S0–S5 = en repo
(ledger §4, status §7, 09 §20.2–20.5), no en el chat.

---

## 3. ADJUDICACIÓN DE LAS 64 HIPÓTESIS

Formato: Veredicto · evidencia · severidad · cambio en R4 (gate). «CONF» = CONFIRMED.

### A. Evidencia, hash y procedencia (1–12)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 1 | **CONF** | R3 P7-3 hasheaba el snapshot pero P7-4.5/P7-5 le añadían inventario/dedupe después («El inventario completo va al snapshot») → citas stale posibles. P0. R4: `IMMUTABLE_EVIDENCE_CAPSULE` finalizado UNA vez tras terminar inventario+dedupe (R4-4); cualquier reapertura invalida y repite downstream. Gate: capsule hash inmutable en el journal |
| 2 | **CONF** | R3 P7-6.4 permitía handoff como «sección final del snapshot» → autorreferencia posible. P0. R4: `FINAL_HANDOFF_MANIFEST` = archivo SEPARADO, creado al final, sin su propio hash (§10). Gate: check estructural en R4-7 |
| 3 | **CONF** | Sin hash pre-corrección ni outputs primarios durables (§2), un diff-audit HOY es una auditoría NUEVA, no un binding retroactivo. P0. R4: gate histórico queda `HISTORICAL_INTERNAL_AUDIT=UNBOUND` para siempre; la nueva se llama `INTERNAL_REAUDIT` y liga al hash final (R4-2/R4-3) |
| 4 | **CONF** | R3 P7-1 permitía re-verificar «solo esa» corrección tras editar → los demás checks quedaban ligados al hash viejo. P0. R4-3: tras la ÚLTIMA edición, re-audit COMPLETO sobre el hash final; después el ADR es read-only (freeze lógico). Gate: hash del ADR en el journal no cambia post-reaudit |
| 5 | **CONF** | «12 sitios» era impreciso: 11 ediciones físicas + C-R3-002 = decisión futura sin diff. P1. R4: `CORRECTION_MANIFEST` con 12 filas y oráculo PROPIO por fila (11 diff-checks + 1 decision-recorded-check) |
| 6 | **CONF** | Verificar correcciones ≠ verificar las 14 filas §R3-15 contra código. P1. R4-3 los separa: `REAUDIT-A` (correcciones) y `REAUDIT-B` (truth-table completa 14/14, no 8/14) |
| 7 | **CONF** | No existe fingerprint capturado EN el momento de la corrida 4008 → vínculo indemostrable. P1. R4: esa corrida = `HISTORICAL_PASS_UNBOUND`; solo un run futuro con `run-id+commit+fingerprint+comandos+outputs` capturados podrá ser BOUND. P7 sigue doc-only, sin Gradle |
| 8 | **CONF (afinado con medición)** | «Evidencia histórica del sello S1–S4» sobreafirmaba: el manifiesto NUNCA validó el árbol commiteado (3/16 rotos de nacimiento, §2). R4 adopta: `LEGACY_MANIFEST_PRESERVED=YES`, `LEGACY_MANIFEST_VALIDATES_TREE=NO`, con tabla por-archivo (13 OK / 3 MISMATCH) — lo que SÍ demuestra: 13 entradas coinciden con HEAD |
| 9 | **CONF + EJECUTADA** | Recuperación intentada ANTES de declarar pérdida: object store barrido por tamaño; catálogo 0 candidatos, portfolio 0, inputs 2 candidatos excluidos por sha256. Resultado: `NOT_FOUND` ×3 (§2). El `PROVENANCE_INCIDENT` de R4-1 la incorpora como hecho cerrado |
| 10 | **CONF** | «Hash + ubicación Temp» no preserva bytes. P1. R4-2: bytes del tracker (y de la R3) COPIADOS a `PRIMARY_EVIDENCE_ARCHIVE` in-repo tras secret/PII scan. La supervivencia de hoy fue suerte (S-02), no custodia |
| 11 | **CONF** | Falta procedencia por lente (prompt, input-hash, modelo, permisos, stop-reason). `MISSING_PRIMARY_EVIDENCE` parcial: el transcript `.jsonl` conserva outputs verbatim — R4-2 extrae lo extraíble, etiqueta lo perdido, y NO lo re-narra |
| 12 | **CONF** | Tokens con hash embebido (`BOUND@hash`) mal diseño. P2. R4: campos separados `status · artifact_sha256 · audit_run_id · scope · timestamp · reviewer` en capsule/companion |

### B. Ejecutabilidad, concurrencia y rollback (13–24)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 13 | **CONF** | Defecto real de MI R3: P7-6.2 exigía «git status SOLO allowlist; cero .kt» sobre un árbol que YA trae 13 M + ~20 U de código (§2) → gate imposible o incentivo a borrar trabajo ajeno. P0. R4-7: el gate es **DELTA vs `PRE_STATE_MANIFEST`** (bytes/modos idénticos para todo lo no-allowlist), nunca el estado absoluto |
| 14 | **CONF** | ídem — delta-only, codificado en R4-0/R4-7 |
| 15 | **CONF** | `git diff --check` no cubre untracked ni hunk-level. P1. R4-7: validación por-deliverable + comparación byte-a-byte del no-allowlist + allowlist HUNK-level para ADR/register (solo secciones autorizadas) |
| 16 | **CONF** | `Get-Process` no excluye writers no-JVM. P1. R4-0: lock file atómico (`p7.lock` con owner+run-id+TTL en el repo-root de trabajo, creación exclusiva) + optimistic concurrency: re-hash de precondiciones ANTES de promover (R4-6) |
| 17 | **CONF** | Sin transacción global: P7-4 podía dejar 4 docs editados si P7-5 fallaba. P0. R4: TODO se edita en STAGING (copias), se valida el conjunto, y se PROMUEVE atómicamente con journal de dos fases + rollback global (§10) |
| 18 | **CONF** | `git checkout --` destruye cambios previos (05/09/register ya traían cambios de fases anteriores). P1. R4: rollback SOLO desde backups byte-for-byte de R4-0; git checkout prohibido como vía de rollback |
| 19 | **CONF** | Staging + journal + precondition-hashes + promoción de conjunto + recovery post-crash: adoptados (R4-6, §10) |
| 20 | **CONF** | Verificado HOY: rama `product/focus2-v5` (no main/master), upstream +0-0, stash vacío. R4-0 lo re-verifica en ejecución + aborta si HEAD cambia mid-run (`CONFLICT_DETECTED`) |
| 21 | **CONF** | `EJECUTAR_PASO7_REVISADO` era replayable y no ligaba plan/decisión/run. P0. Sustituido por el mandato parametrizado (cabecera). El duplicado del chat = `UNKNOWN` desde repo, aceptado como demostración del riesgo |
| 22 | **CONF** | Formato mínimo adoptado: `PLAN_SHA256 + CUSTODY_DECISION + RUN_ID`; R4-0 verifica run-id no usado (registro de run-ids en el journal del archive) |
| 23 | **CONF** | P0 hallado mid-transacción → primero `STOP_REPORT` separado (archivo en evidence/), NUNCA escritura parcial de ledger dentro de una transacción abortada. Registrarlo en el ledger = ejecución autorizada posterior |
| 24 | **CONF** | Oráculos duales PowerShell/Bash por cada verificación (§11 tabla); mismos inputs, mismo criterio de pass |

### C. Autoridad, estados y legacy bundle (25–32)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 25 | **CONF** | A/B/C incompleta. Opción D evaluada y **RECOMENDADA** (§7); no se inventa una E: D + la forense YA ejecutada cubre el espacio |
| 26 | **CONF** | Con S-01 (3/16, no 1/16) la etiqueta «sello S1–S4» es insostenible. Clasificación adoptada de la hipótesis 8 |
| 27 | **CONF** | Enterrar la supersesión de T-S5 en 09 deja autoridad stale legible. P0. R4-5 crea `CURRENT_AUTHORITY_INDEX` (§10) que TODO entrypoint debe leer; los legacy quedan preservados pero marcados NO VIGENTES por mecanismo, no por convención |
| 28 | **CONF** | Índice de autoridad o V5.1: ambos en D (índice ahora, línea V5-S5 al cierre real con autorización separada) |
| 29 | **PARTIAL** | La R3 en scratchpad SOBREVIVIÓ al reinicio (byte-idéntica, §2) — la pérdida no ocurrió; la crítica estructural es válida. Esta R4 ya vive en ruta durable; R4-2 archiva los bytes de R3+tracker in-repo |
| 30 | **CONF** | El modelo §5 de la R3 mezclaba ejes sin máquina de transiciones. R4 §6: ejes con autoridad de transición, transiciones permitidas e invalidadores |
| 31 | **CONF** | «7/7» sustituido por MATRIZ DE OBLIGACIONES conjuntivas por scope con blockers dominantes (§6/§11); ninguna fracción agregada |
| 32 | **CONF** | Campos separados: `severity · exposure(cuándo daña: CURRENT/PRE_MOUNT/PRE_MEASUREMENT/PRE_CLAIM/PRE_RELEASE) · affected_gate · evidence_confidence · user_harm` (aplicado en §12 y en la matriz ledger) |

### D. Ledger y coherencia documental (33–40)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 33 | **CONF** | «~10–14» era anchoring. Eliminado: el conteo SALE del dedupe, no lo precede |
| 34 | **CONF** | Fusiones predecididas (p. ej. B1+B2+B3) pasan a PROPUESTAS que el dedupe debe PROBAR (invariante+owner+fix+gate+evidencia iguales); en duda, NO fusionar |
| 35 | **CONF** | `validate-audit-ledger.mjs` = necesario, no suficiente (sintaxis+sello; no verdad/cobertura). R4-6 añade el comparador semántico |
| 36 | **CONF** | Comparador pre/post = SCRIPT ARTEFACTO hasheado en evidence/ (no efímero): IDs 1×, transición⇒evento, row-hash de filas históricas, columnas allowlist, EOL LF, schema, referencias a EVIDENCE_ID existentes, conteos §0 |
| 37 | **CONF** | Citas `ruta:línea` frágiles → `EVIDENCE_ID + artifact_sha256 + anchor estable` (línea solo como ayuda). Aplicado en capsule/companion |
| 38 | **CONF** | Barrido léxico = DISCOVERY. La cobertura real la da el `CLAIM_REGISTRY` del capsule: todo claim vivo con estado/autoridad/evidencia/consumidor; el barrido solo alimenta candidatos |
| 39 | **CONF** | Igualdad cross-doc literal mal definida → los docs son PROYECCIONES del capsule (fuente estructurada); la validación comprueba que ninguna proyección EXCEDE su fuente |
| 40 | **CONF** | Si falla el ledger tras editar docs → restore de TODO el conjunto P7 desde backups y verificación byte-a-byte, o no se promueve nada (transacción, R4-6) |

### E. Packet y auditoría externa (41–50)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 41 | **CONF** | «Commit/tree exactos» era falso con 13 M + ~20 U fuera de HEAD. P0. R4-7: `base HEAD + overlay determinista` |
| 42 | **CONF** | Overlay = `git diff --binary` + TODOS los untracked (bytes+modos+rutas) + manifest sha256 de miembros + script de reconstrucción (§10) |
| 43 | **CONF** | Prueba de reconstrucción OBLIGATORIA en dir temporal limpio: aplicar overlay sobre HEAD → re-hash → root manifest idéntico; sin ella el packet NO está listo |
| 44 | **CONF** | Handoff separado sin su propio hash; root digest reportado fuera de banda (chat/registro CEO; opcional tag firmado futuro con autorización) |
| 45 | **CONF** | Contraejemplos como fixtures machine-readable en el capsule companion: inputs {H,M,FA,CR}, constantes (`LOGLINEAR 0.5/1.0`, `Z_95=1.959963984540054`), fórmula id, outputs esperados a 10 decimales, política de redondeo, y DOS cálculos independientes ya existentes (psicómetra base A, assessment base B) referenciados por EVIDENCE_ID |
| 46 | **CONF** | Un solo «auditor externo» no colapsa scopes. Packet por 5 scopes con cualificación/criterios/veredicto separados (§11) |
| 47 | **CONF** | Agente-mismo-sistema = independencia de contexto, NO material. Declarado en cada fila de la matriz de gate; la independencia material la aporta el auditor externo |
| 48 | **CONF** | Packet incluye: FAILs (§7 observer), GAPs, evidencia perdida (`MISSING_PRIMARY_EVIDENCE`), conflictos, límites y preguntas de falsación + criterios de aceptación PREDECLARADOS por scope (§11) |
| 49 | **CONF** | Política de invalidación: cualquier edición del ADR/scorer/thresholds/OS-target/intended-use ⇒ veredicto expirado; revalidación dirigida con impact assessment (§11) |
| 50 | **CONF** | Secret/PII scan ANTES de copiar transcript/outputs al archive (R4-2, oráculo en §11) |

### F. Ciencia cognitiva, usuario y empresa (51–64)

| # | Veredicto | Evidencia y cambio en R4 |
|---|---|---|
| 51 | **CONF** | `Construct validity=COVERED(docs)` era overclaim mío. Reclasificado: `DOCUMENTED` ≠ `VALIDATED`; matriz §12 separa docs/impl/validación/evidencia humana |
| 52 | **CONF** | «Observabilidad sin PII» = `STATIC_SEARCH_ONLY` (grep+schema); auditoría runtime de sinks reales = gate PRE_RELEASE (§12) |
| 53 | **CONF** | Lexicon/copy-scanner gobierna CLAIMS, no clasificación regulatoria. Intended-use analysis + counsel/DPO = fila propia PRE_CLAIM con owner CEO/legal (§12); el modelo NO concluye la clasificación |
| 54 | **CONF** | Fiabilidad/SEM/practice/alternate-forms/responsiveness/normas/invariancia añadidos al registro (§12, filas 3–7) — eran el MISSING #2 de la R3, ahora desglosados |
| 55 | **CONF** | «Usuario promedio» → población objetivo/exclusiones/idiomas/dispositivos como fila PRE_CLAIM (§12 fila 1) |
| 56 | **CONF** | 4 claims distintos (tarea entrenada, constructo, transferencia, vida diaria) con evidencia distinta — ya doctrina del charter; ahora filas separadas con gate propio (§12 fila 9) |
| 57 | **CONF** | Roadmap de eficacia (prerregistro, control activo, outcomes primarios, multiplicidad, attrition, nulos) = `RESEARCH_ROADMAP` con owner (§12 fila 9) |
| 58 | **PARTIAL** | El repo YA separa propósito por autoridad única (`ProfileMeasurementEligibility`, verificado en el authority map S5-02) — la FRONTERA existe; la verificación EMPÍRICA de no-contaminación (adaptación→comparabilidad) está pendiente = gate PRE_MEASUREMENT (§12 fila 10) |
| 59 | **CONF** | Change-control/bridging por versión de motor/thresholds/OS/device/idioma/input = política PRE_RELEASE (§12 fila 11) |
| 60 | **CONF** | DPIA (proxies asistivos ya identificados), export (gap de TODO el repo, F-R3), backups/logs, menores, multi-device → owners/gates en §12 filas 12–13; la determinación DPIA formal se remite a DPO/asesoría (no la concluye el modelo) |
| 61 | **CONF** | = H-18 de la R3, coherente: mutex in-process + tombstone ≠ prueba cross-process; `NOT_PROVEN` hasta primitiva persistente atómica O precondición single-process declarada+forzada+testeada. Gate de montaje F-R3-A ampliado |
| 62 | **CONF** | Hoy la barrera es NO-MOUNT estructural (sin flag) — correcto para S5; PRE_MOUNT exige flag/kill-switch + rollback + observabilidad sin PII + incident response + device matrix ANTES de shadow/S6 (§12 fila 14, precondición NUEVA de S6) |
| 63 | **CONF** | Riesgos de usuario (fatiga, frustración, compulsión, fotosensibilidad, menores) = registro con owner product-ux/qa-safety, sin implementar en P7 (§12 fila 15) |
| 64 | **CONF** | Clasificación adoptada para TODO gap fuera de P7: `CURRENT_BLOCKER / PRE_MOUNT / PRE_MEASUREMENT / PRE_CLAIM / PRE_RELEASE / RESEARCH_ROADMAP` + owner + evidencia de cierre + user harm evitado |

---

## 4. HALLAZGOS ADICIONALES DE ESTA INVOCACIÓN

| ID | Sev | Hallazgo | Evidencia |
|---|---|---|---|
| S-01 | **P0** | **La brecha del bundle es 3/16, no 1/16, y la R3 la infra-midió** (solo verifiqué 3 archivos + manifest; el barrido completo era obligatorio). `EXERCISE_PORTFOLIO_V5.md` e `INPUTS_SHA256.txt` también rotos de nacimiento (mismo commit único `15cb12a`); bytes sellados `NOT_FOUND` en el object store (búsqueda por tamaño + sha256). Lección aplicada: en R4 ningún «intacto» se declara sin barrido completo | `VERIFIED_PRIMARY` §2 |
| S-02 | P1 | Scratchpad sobrevivió al reinicio: R3 (`25dfe9ff…`) y tracker (`ee8761a0…`) byte-idénticos. Convierte P0-R4-12/C.29 en PARTIAL: riesgo real, daño no materializado. R4-2 archiva los bytes in-repo igualmente | `VERIFIED_PRIMARY` |
| S-03 | P1 | `INPUTS_SHA256.txt` roto = el ANCLA de procedencia de los inputs del bundle es a su vez inverificable → el incidente de procedencia alcanza también a la cadena de inputs del plan V5, no solo a 2 docs de contenido | `VERIFIED_PRIMARY` |
| S-04 | P2 | focus2 no tiene feature flag (barrera = no-mount estructural). Correcto en S5; convierte «flag/kill-switch ANTES de shadow» (F.62) en precondición NUEVA y explícita de S6 que ningún doc listaba como tal | `VERIFIED_DERIVED` (data-flow map S5-09) |
| S-05 | P2 | El mandato duplicado en el chat adjunto: sin rastro en repo (`UNKNOWN`); aceptado como reporte del CEO. Refuerza B.21 (idempotencia por RUN_ID) sin necesitar prueba local | `UNKNOWN` |

---

## 5. AUTHORITY MATRIX POR TIPO DE CLAIM

| Tipo de claim | Fuente que MANDA | El resto | Ante discrepancia |
|---|---|---|---|
| Intended use / claims de producto | CEO + charter + ForbiddenLexicon | docs proyectan; código nunca amplía claims | gana el más restrictivo; discrepancia = STOP |
| Ley / privacidad | normativa + counsel/DPO (humanos) | el modelo solo REGISTRA riesgos y remite | nunca concluye el modelo; fila abierta con owner legal |
| Requisito de producto | PRD + decisiones CEO (09) | plan/gates derivan | 09 más reciente supersede; registrar supersesión en el índice |
| Decisión aprobada | 09_DECISION_LOG (+ ledger para findings) | status narra, no decide | entrada 09 con fecha gana; sin entrada = no decidido |
| Contrato de diseño | ADR vigente según `CURRENT_AUTHORITY_INDEX` | R2/R1 = historia | ADR CANDIDATA no es APPROVED: lo dice el eje AUDIT |
| **Implementación real** | **EL CÓDIGO** (bytes en el árbol identificado) | docs DESCRIBEN; un doc contra código = `CONTRADICTED` del doc | ningún doc puede declarar implementado lo que el código refuta |
| Validación / test evidence | runs con run-id+árbol+outputs capturados | claims sin captura = `UNBOUND`/`STALE` | grep/compile/unit ≠ evidencia física; cada nivel etiquetado |
| Release / mount | GATE_CATALOG vigente vía índice + ledger blockers | — | un P0 abierto domina cualquier agregado de APTOs |

---

## 6. STATE MACHINE NAMESPACED (valores literales, autoridad, transiciones, invalidadores)

| Eje | Valores literales | Autoridad de transición | Invalidador |
|---|---|---|---|
| `ADR_STATUS` | `DRAFT` → `CANDIDATE_FOR_EXTERNAL_REVIEW` → (`APPROVED_EXTERNAL` ∨ `REJECTED_WITH_FINDINGS`) → `FROZEN_FOR_S5_R4` ∨ `REOPENED_AFTER_EXTERNAL_AUDIT`; terminal histórico `SUPERSEDED` | auditoría externa (APPROVED/REJECTED); CEO (FROZEN); nunca la sesión sola | cualquier edición del ADR tras CANDIDATE re-entra a DRAFT del delta |
| `INTERNAL_AUDIT` | `NOT_REVIEWED` · `HISTORICAL_INTERNAL_AUDIT_UNBOUND` (fijo, irrecuperable) · `INTERNAL_REAUDIT_PASS` (campos: run_id, artifact_sha256, scope) · `NEEDS_REVIEW` · `FALSIFIED` | sesión ejecuta; el PASS solo existe con run+hash capturados | edición del artefacto ⇒ PASS expira |
| `IMPLEMENTATION` (por claim) | `NOT_STARTED` · `DESIGNED_NOT_RUN` · `PENDING` · `PARTIAL` · `CONFORME_UNMOUNTED` · `CONTRADICTED` · `MOUNTED` | solo el CÓDIGO la determina; docs proyectan | diff en los archivos del claim ⇒ re-derivar |
| `VALIDATION` | `NOT_RUN` · `STATIC_ONLY` · `UNIT_SIM` · `DEVICE_MANUAL` · `PHYSICAL_BENCH` | el run que la produce (con captura) | cambio de código/OS/device ⇒ impact assessment |
| `EVIDENCE_STATE` (dominio charter §7) | `TRAINING_ONLY` · `PROVISIONAL` · `OFFICIAL_ELIGIBLE` (inconstructible) · `INVALIDATED` (+ veredicto `NotCaptured`) | el motor en runtime; renombrado de `PROVISIONAL` = decisión de código PENDIENTE (C-R3-002) | — |
| `MOUNT` | `BLOCKED` · `SHADOW_ONLY` · `ALLOWED` | gate S6 (F-R3-A/C/D/E + flag S-04) | cualquier blocker P0 nuevo ⇒ BLOCKED |
| `RELEASE` | escalera GATE_CATALOG vigente (`NOT_READY` → `TRAINING_READY` → `MEASUREMENT_CANDIDATE` → …) | gates T-* vigentes según índice | T-S5 no-PASS ⇒ NOT_READY |
| `EXECUTION` (terminales P7, excluyentes) | `COMPLETED` · `BLOCKED_DECISION` · `STOPPED_NEEDS_REVIEW` · `CONFLICT_DETECTED` · `ABORTED_ROLLED_BACK` · `PARTIAL_RECOVERY_REQUIRED` | el journal de la ejecución | **solo `COMPLETED` satisface la DoD; STOP nunca es DONE** |

Estados actuales (2026-07-22): ADR=`CANDIDATE_FOR_EXTERNAL_REVIEW` · INTERNAL_AUDIT=
`HISTORICAL_INTERNAL_AUDIT_UNBOUND` · IMPLEMENTATION=mixto (§R3-15: 3 CONTRADICTED, 6 PENDING,
resto CONFORME_UNMOUNTED) · VALIDATION=UNIT_SIM(dominio)/STATIC_ONLY(plataforma)/NOT_RUN(físico) ·
MOUNT=`BLOCKED` · RELEASE=`NOT_READY` · EXECUTION=n/a (P7 no ejecutado).

---

## 7. DECISIÓN DE CUSTODIA — COMPARACIÓN A/B/C/D Y RECOMENDACIÓN

| Criterio | A (register vivo + manifest «histórico») | B (revert + addendum) | C (regenerar manifest S4) | **D (procedencia versionada + cuarentena)** |
|---|---|---|---|---|
| Honestidad histórica | media (llama «sello» a lo que nunca selló — refutado por S-01) | media (reescribe worktree del Paso 2) | nula (destruye reproducibilidad) | **máxima** (declara VALIDATES_TREE=NO medido + incidente formal) |
| Recuperabilidad | no intenta forense | no | no | **forense EJECUTADA: NOT_FOUND ×3 documentado** |
| Riesgo de drift futuro | alto (sin índice de autoridad) | alto (autoridad bifurcada) | alto | **bajo (CURRENT_AUTHORITY_INDEX como entrypoint obligatorio)** |
| Discoverability de autoridad vigente | baja (supersesión enterrada en 09) | baja | baja | **alta (índice por claim-type)** |
| Coste | bajo | medio | bajo | medio (un incidente + un índice + snapshot del register en el bundle nuevo) |
| Auditoría externa | difícil | difícil | imposible | **directa (incidente + tabla 13/3 + NOT_FOUND reproducibles)** |

**RECOMENDACIÓN: Opción D**, con dos concreciones ya resueltas por esta invocación: (i) el paso
forense de D está EJECUTADO (resultados en §2 — solo falta transcribirlos al `PROVENANCE_INCIDENT`
en R4-1); (ii) el register: su edición worktree del Paso 2 se CONSERVA (no se revierte — historia
real) y el register queda declarado documento VIVO en el índice; el bundle V5-S5 futuro incluirá un
SNAPSHOT del register, nunca el vivo. No se propone una Opción E: D + forense hecha cubre el espacio;
añadir capas sería teatro de procedencia.

---

## 8. PLAN R4 COMPLETO (ejecutable por otra sesión; requiere el mandato parametrizado)

Prohibiciones en TODA fase: editar código/build; Gradle; `freeze_v5.cjs` en dir real; montar;
congelar; `git add/commit/push/stash/checkout/reset` (git solo LECTURA); tocar archivos fuera de
allowlist; presentar STOP como DONE. Cada fase declara: precondición → operación → oráculo →
fallo/rollback → postcondición.

### R4-0 · Preflight, lock y pre-state (sin ediciones)
- Verificar el mandato: `PLAN_SHA256` == hash de ESTE archivo re-computado; `CUSTODY_DECISION`
  presente; `RUN_ID` no usado (buscar en `docs/engines/focus_v5/evidence/` journals previos).
- Rama ≠ main/master; HEAD capturado; upstream delta; stash listado. Lock: crear
  `docs/engines/focus_v5/evidence/p7_run_<RUN_ID>.lock` por creación EXCLUSIVA (falla si existe)
  con owner+timestamp; si existe → `CONFLICT_DETECTED`.
- `PRE_STATE_MANIFEST` (JSON, §10): `git status --porcelain=v2 -z` COMPLETO expandido (tracked
  M/A/D/R/C + untracked TODOS, incl. dirs `focus2/domain/evidence/`), por entrada: path, estado,
  modo, bytes, sha256. Incluye los 2 ajenos iOS (se preservan idénticos).
- Backups byte-for-byte de TODOS los allowlisted editables (05, 09, register, status, ADR, ledger)
  → `evidence/backup_<RUN_ID>/` + sha256 por archivo + **restore PROBADO** (restaurar a temp,
  comparar hash).
- Oráculo: manifest generado + lock propio + backups verificados. Fallo → liberar lock, terminal
  `CONFLICT_DETECTED` o `BLOCKED_DECISION`. Postcondición: cero bytes del repo cambiados aún.

### R4-1 · Incidente de procedencia + custodia (staging)
- Redactar `PROVENANCE_INCIDENT` (sección del capsule): timeline (freeze `15cb12a` 2026-07-21 →
  3/16 rotos de nacimiento → register mutado en worktree Paso 2), tabla 16 entradas OK/MISMATCH,
  forense NOT_FOUND ×3 (comandos+outputs de §2 transcritos), alcance (INPUTS = ancla de inputs
  también inválida, S-03), causa raíz `UNKNOWN` (edición post-generación pre-commit; no
  reconstruible), y límites de lo afirmable (13 entradas SÍ coinciden con HEAD).
- Aplicar `CUSTODY_DECISION` (si D): redactar `CURRENT_AUTHORITY_INDEX.md` (staging) — por
  claim-type: artefacto vigente, versión, y lista de artefactos históricos/superseded/INVALID
  (manifest V5 = `LEGACY_MANIFEST_PRESERVED, VALIDATES_TREE=NO`; T-S5 criterio export =
  SUPERSEDED por contrato CEO + §R3-15).
- Oráculo: incidente cita solo hechos con EVIDENCE_ID. Fallo → `BLOCKED_DECISION` si la custodia
  elegida resulta inaplicable.

### R4-2 · Evidencia primaria: archivar o declarar perdida (staging)
- Crear `evidence/R3/`: bytes EXACTOS de `S5_R3_PASO7_PLAN_REVIEWED.md`, tracker, y extractos
  verbatim del transcript `.jsonl` con los outputs de las 13 lentes SI son recuperables —
  **tras secret/PII scan** (oráculo: scan limpio; hallazgo ⇒ redacción mínima marcada, nunca
  silenciosa). INDEX con EVIDENCE_ID, origen, sha256, limitación por ítem.
- Adjudicar: gate histórico = `HISTORICAL_INTERNAL_AUDIT_UNBOUND` PERMANENTE (hipótesis 3);
  prompts/input-hashes por lente = `MISSING_PRIMARY_EVIDENCE` si el transcript no los conserva.
- Definir el `INTERNAL_REAUDIT` (se ejecuta en R4-3): 1 agente diff-auditor read-only, scope =
  (a) 12 filas del CORRECTION_MANIFEST y (b) truth-table 14/14 de §R3-15 contra código, sobre el
  hash VIVO del ADR; prompt+input-hash+output CAPTURADOS al archive esta vez.

### R4-3 · Correcciones ADR en staging + reaudit completo + freeze lógico
- Clasificar cada corrección pendiente: EDITORIAL (aplicable) vs NORMATIVA (⇒
  `STOPPED_NEEDS_REVIEW`, no se aplica sin auditoría). Editar SOLO en la copia staging del ADR;
  hunk-allowlist = secciones §R3-* citadas por el manifest de correcciones.
- Ejecutar `INTERNAL_REAUDIT` (A+B) sobre el hash del ADR staging FINAL. Si pide un cambio: se
  aplica, y el reaudit COMPLETO se repite sobre el nuevo hash (hipótesis 4). Al PASS: el ADR queda
  read-only lógico (su hash entra al journal; cualquier edición posterior ⇒ todo downstream
  inválido).
- Oráculo: `INTERNAL_REAUDIT_PASS{run_id, sha256(ADR_final), scope A+B}` con output archivado.

### R4-4 · Capsule inmutable (una sola finalización)
- Completar EN STAGING: claim registry + inventario semántico clasificado (motor .NET regex con
  lookahead O doble pasada; cada hit = {FALSE_CURRENT_CLAIM, HISTORICAL_SUPERSEDED, NEGATION,
  VALID_SCOPED_CLAIM, UNKNOWN→resolver}), CORRECTION_MANIFEST final (12 filas + oráculo por fila),
  FINAL_GATE_EVIDENCE_MATRIX (una fila final por lente; gate histórico UNBOUND + reaudit nuevo),
  matriz finding→ledger con dedupe PROBADO fila a fila (leer texto real `MB-FOCUS-0021..0046`;
  sin conteo esperado), fixtures machine-readable de los contraejemplos, PROVENANCE_INCIDENT,
  PRE_STATE hash.
- Finalizar `IMMUTABLE_EVIDENCE_CAPSULE` (MD + companion JSON) UNA vez → sha256. No contiene
  hashes de docs/ledger/handoff (downstream). Reapertura ⇒ repetir R4-5..R4-7.

### R4-5 · Docs + ledger como PROYECCIONES del capsule (staging)
- Generar en staging: status (retira SOLO el blocker stale «límite de sesión»; conserva blockers
  reales scopeados; números derivados del capsule), 09 §20.6 (falsación con AMBOS contraejemplos
  por EVIDENCE_ID; «el gate falsado nunca estuvo shipped»; D-R3-01..04; CANDIDATE≠APPROVED;
  supersesión T-S5-export; custodia elegida; C-R3-002 pendiente), 05 (truth-table claim-by-claim
  desde §R3-15 verificado 14/14; sin PENDING para lo implementado ni viceversa), register (SOLO si
  custodia lo permite: fila 24 + nota de custodia, hunk-allowlist), CURRENT_AUTHORITY_INDEX, y el
  delta del ledger (filas nuevas con IDs desde el primer libre REAL, transiciones con evento 1:1
  homogéneo, checkpoint con HEAD+fingerprint, §0 count+sello recomputado con el algoritmo del
  validador replicado DIRECTAMENTE — jamás vía su mensaje de error).
- Oráculo staging: validador del ledger (sobre la copia staging) exit 0; comparador semántico
  pre/post verde; proyección-no-excede-fuente verde; secret/PII scan de todo lo nuevo.

### R4-6 · Recheck optimista + promoción transaccional
- Journal fase-1 `PREPARED`: lista {path, pre_sha (debe == PRE_STATE), staged_sha}.
- Re-hash de TODOS los pre_sha contra el árbol vivo: cualquier diferencia ⇒ `CONFLICT_DETECTED`,
  cero promoción.
- Promoción: copiar staged→vivo en orden (docs → ledger → índice), re-hash post, journal fase-2
  `PROMOTED`. Validador del ledger sobre el VIVO exit 0.
- Fallo en CUALQUIER punto ⇒ restaurar TODO el conjunto desde `backup_<RUN_ID>`, verificar
  byte-a-byte, journal `ROLLED_BACK`, terminal `ABORTED_ROLLED_BACK`. Crash: al reanudar, el
  journal decide — `PREPARED` sin promover = nada que restaurar; promoción parcial = restore
  completo; si el restore no puede CONFIRMARSE byte-a-byte ⇒ `PARTIAL_RECOVERY_REQUIRED` con lista
  exacta de bytes.

### R4-7 · Verificación delta, handoff y packet reproducible
- **Delta-only:** todo path NO-allowlist compara sha256+modo contra PRE_STATE — IDÉNTICO (código,
  ajenos iOS, bundle legacy intactos). Allowlist: diff esperado por deliverable.
- `FINAL_HANDOFF_MANIFEST` (archivo separado, al final): hashes finales de ADR/status/05/09/
  register/índice/capsule/ledger(archivo+sello §0)/plan-R4/journal + blockers supervivientes +
  estados §6. No incluye su propio hash.
- Overlay: `git diff --binary HEAD` + archivo con TODOS los untracked (bytes+modos) + manifest de
  miembros + script de reconstrucción. **Prueba de reconstrucción ejecutada** en dir temporal:
  HEAD limpio + overlay ⇒ root manifest idéntico. Root digest del paquete reportado FUERA de banda.
- Liberar lock. Terminal `COMPLETED` solo si TODO lo anterior pasó.

### R4-8 · Cierre
- Emitir exactamente UN terminal de §6/EXECUTION + resumen honesto + siguiente acción exacta
  (entregar packet a auditoría externa por scopes). `FROZEN_FOR_S5_R3/R4`, montaje e implementación
  (steps 5–14) siguen PROHIBIDOS.

---

## 9. TABLA R3→R4 (defecto → cambio → oráculo nuevo)

| P0 auditor | Defecto R3 | Cambio R4 | Oráculo |
|---|---|---|---|
| 01 | snapshot mutable tras hashear | capsule finalizado 1× (R4-4) | hash en journal, inmutable |
| 02 | handoff dentro del snapshot | archivo separado sin auto-hash (R4-7) | check estructural |
| 03 | retro-binding del gate | `HISTORICAL…UNBOUND` fijo + `INTERNAL_REAUDIT` nuevo (R4-2/3) | reaudit con run_id+hash |
| 04 | reverificar «solo esa» | reaudit COMPLETO sobre hash final + ADR read-only (R4-3) | hash estable post-PASS |
| 05 | «12 sitios» ambiguo | 12 filas con oráculo propio (11 diffs + 1 decisión) | por-fila |
| 06 | dos scopes fusionados | REAUDIT-A (correcciones) + REAUDIT-B (14/14 vs código) | dos PASS separados |
| 07 | vigencia de tests inferida | `HISTORICAL_PASS_UNBOUND`; binding solo con captura futura | run-id+fingerprint |
| 08 | «sello S1–S4» | `LEGACY_MANIFEST_VALIDATES_TREE=NO` + tabla 13/3 | incidente medido |
| 09 | evidencia efímera | archive in-repo con bytes + PII scan (R4-2) | INDEX hasheado |
| 10 | valores hardcodeados | todo re-derivado en R4-0; capsule como única fuente | PRE_STATE manifest |
| 11 | «cero borrados» | invariantes semánticos + comparador artefacto (R4-5/6) | script hasheado verde |
| 12 | backup=git diff | backups byte-for-byte + restore probado (R4-0) | hash del restore |
| 13 | allowlist absoluta imposible | **delta vs PRE_STATE** (R4-7) | no-allowlist byte-idéntico |
| +S-01 | brecha infra-medida | barrido 16/16 SIEMPRE; jamás «intacto» sin medición completa | tabla completa en capsule |

---

## 10. ESPECIFICACIÓN DE ARTEFACTOS (esquemas mínimos, orden acíclico)

Orden de creación/hash: `PRE_STATE_MANIFEST` → `PRIMARY_EVIDENCE_ARCHIVE` → `INTERNAL_REAUDIT`
outputs → `IMMUTABLE_EVIDENCE_CAPSULE` (+companion) → docs/ledger staging → promoción (journal) →
`FINAL_HANDOFF_MANIFEST` → overlay/archive → root digest FUERA de banda. Ninguno contiene su propio
hash; ninguno upstream contiene hashes downstream.

- **PRE_STATE_MANIFEST** (`evidence/p7_run_<RUN_ID>.prestate.json`): `{run_id, ts_utc, branch,
  head, upstream_ab, entries:[{path, git_state, mode, bytes, sha256}], generator:"<script+ver>",
  hash_alg:"sha256(bytes crudos)"}` — todos los tracked-changed Y untracked, sin lista manual.
- **PRIMARY_EVIDENCE_ARCHIVE** (`evidence/R3/…` + `INDEX.md`): por ítem `{EVIDENCE_ID, tipo,
  origen(ruta original), sha256, bytes, limitación, pii_scan:PASS}`.
- **IMMUTABLE_EVIDENCE_CAPSULE** (`S5_R3_EVIDENCE_CAPSULE.md` + `capsule.json`): gate matrix,
  correction manifest, claim registry + inventario clasificado, matriz ledger con dedupe probado,
  fixtures de contraejemplos `{formula_id:"dprime_hautus_loglinear", constants:{c:0.5, t:1.0,
  z95:1.959963984540054}, cases:[{id, H,M,FA,CR, d_prime, se, ci_lower}] , rounding:"IEEE754
  double, sin redondeo intermedio"}`, provenance incident, prestate_sha. Campos de estado SIEMPRE
  `{status, artifact_sha256, audit_run_id, scope, ts, reviewer}`.
- **TRANSACTION_JOURNAL** (`evidence/p7_run_<RUN_ID>.journal.json`): `{run_id, phases:[{name,
  files:[{path, pre_sha, staged_sha, promoted_sha}], status: PREPARED→PROMOTED (o ROLLED_BACK),
  ts}]}`. Recovery: PREPARED-sin-PROMOTED ⇒ nada vivo cambió; parcial ⇒ restore total verificado.
- **FINAL_HANDOFF_MANIFEST** (`S5_R3_P7_HANDOFF.md`): lista de hashes finales + blockers
  supervivientes + estados; SIN su propio hash; root digest del overlay se reporta fuera.
- **OVERLAY** (`evidence/p7_run_<RUN_ID>_overlay/`): `head.txt`, `tracked.patch`
  (`git diff --binary HEAD`), `untracked/` (bytes+rutas+modos), `members.sha256`,
  `reconstruct.(ps1+sh)`; prueba de reconstrucción obligatoria antes de declarar el packet listo.

---

## 11. EXTERNAL AUDIT PACKET — POR SCOPES, CRITERIOS PREDECLARADOS

Contenido común: overlay reproducible + capsule + handoff + este plan + preguntas de falsación +
`MISSING_PRIMARY_EVIDENCE` declarado + política de invalidación (cualquier edición del ADR/scorer/
thresholds/OS-target/intended-use ⇒ veredicto expirado; revalidación dirigida con impact
assessment). Cada scope emite veredicto SEPARADO `{APPROVED, REJECTED_WITH_FINDINGS}` sobre
`{artifact_sha256, scope, criterios}`; ninguna aprobación colapsa scopes ajenos; un P0 de cualquier
scope domina.

| Scope | Cualificación esperada | Criterios de aceptación PREDECLARADOS (mínimos) |
|---|---|---|
| Psicometría / ciencia | psicometrista (SDT, MNAR, selección) | contraejemplos reproducidos desde fixtures; OI definida sobre denominadores delivered; ninguna ruta outcome→elegibilidad sin cubrir (incl. omisiones/abandono); política MNAR explícita |
| Timing móvil | ingeniería Android/iOS de render/input | taxonomía proxy-vs-photon correcta; L1/B1–B6 reconocidos como vivos; modelo de cadencia 6-hechos; ningún claim de exactitud sin banco físico |
| Seguridad / concurrencia | seguridad de sistemas | contrato de revocación `NOT_PROVEN` cross-process aceptado como gate de montaje; primitiva o precondición exigida; 9 interleavings como aceptación |
| Privacidad / legal | privacy + counsel/DPO | minimización verificada contra bytes; DPIA con owner humano; export/derechos con owner+gate; ninguna conclusión legal del modelo aceptada como final |
| Accesibilidad / fairness | a11y + medición | exclusión-sin-castigo verificada; invariancia declarada NO probada; plan de equating como PRE_MEASUREMENT |

Independencia: declarar por auditor conflicto de interés, acceso previo, prompts/contexto recibido
y relación con la redacción del ADR. Un agente del mismo sistema = independencia de contexto, se
etiqueta como tal, nunca como externa.

---

## 12. CEO ASSURANCE MATRIX AMPLIADA (docs ≠ implementación ≠ validación ≠ evidencia humana)

Clasificación: CB=CURRENT_BLOCKER · PM=PRE_MOUNT · PMe=PRE_MEASUREMENT · PC=PRE_CLAIM ·
PR=PRE_RELEASE · RR=RESEARCH_ROADMAP.

| # | Dominio | Docs | Impl | Valid | Ev. humana | Clase | Owner |
|---|---|---|---|---|---|---|---|
| 1 | Intended use / población objetivo exacta | parcial (PRD) | n/a | NO | NO | PC | CEO+producto |
| 2 | Validez de contenido/constructo | dossiers | n/a | NO | NO | PMe | assessment |
| 3 | Test-retest / SEM / límites de acuerdo | NO | NO | NO | NO | PMe | assessment |
| 4 | Practice effects / alternate forms / leakage | NO | parcial (exposición/seed) | NO | NO | PMe | psychometrician |
| 5 | Responsiveness / reliable change | NO | NO | NO | NO | PMe | psychometrician |
| 6 | Invariancia / equating OS-device-input | diseño (§R3-9) | NO | NO | NO | PMe | assessment+CEO (físico) |
| 7 | Normas / calibración / drift | NO | NO | NO | NO | RR | assessment |
| 8 | MNAR / attrition / adherencia | estimado (capsule) | parcial | NO | NO | PMe | psychometrician |
| 9 | Eficacia y transferencia (4 claims separados) | doctrina charter | n/a | NO | NO | RR (RCT prerregistrado) | CEO+ciencia |
| 10 | Frontera personalización↔medición | autoridad única existe | SÍ (propósito) | empírica NO | NO | PMe | cognitive-engine |
| 11 | Change-control / bridging por versión | NO | versiones parciales | NO | NO | PR | kmp-architect |
| 12 | Privacidad: DPIA, export, backups, menores, multi-device | parcial | export NO existe | NO | NO | PC/PR | privacy+DPO |
| 13 | Seguridad: revocación cross-process, key lifecycle, reinstall | contrato diseñado | NO cableado | NO | n/a | PM | data-sync+security |
| 14 | Operación: flag/kill-switch, rollback, observabilidad, device matrix | NO (no-mount es la barrera) | NO flag | NO | n/a | PM (nueva precondición S6, S-04) | devops+mobile |
| 15 | Riesgos de usuario (fatiga, compulsión, fotosensibilidad, menores) | NO | n/a | NO | NO | PC | product-ux+qa-safety |
| 16 | Timing fit-for-purpose (fotón/touch) | taxonomía §R3-9 | proxy-only | físico NO | n/a | PMe | mobile-perf+CEO |
| 17 | Observabilidad sin PII | verificado estático | shipped | runtime NO | n/a | PR | privacy |
| 18 | Clasificación regulatoria / claims por intended use | lexicon activo | n/a | análisis legal NO | n/a | PC | CEO+counsel |

Regla dura: `documentado` jamás se proyecta como `validado`; esta tabla es la fuente y 05/09 solo
la citan.

---

## 13. SELF-RED-TEAM DE LA R4 (15 formas de falsa seguridad y su bloqueo)

1. **El reaudit interno lo ejecuta el mismo sistema** → se etiqueta `INTERNAL`, campos
   reviewer/limitación obligatorios; la externa es la única que aprueba el ADR.
2. **El capsule como «canon paralelo»** → rol EVIDENCIA declarado; transiciones solo en ledger;
   proyección-no-excede-fuente verificada.
3. **Truth-table copiada de §R3-15 sin re-verificar** → REAUDIT-B exige 14/14 contra código.
4. **Fixtures de contraejemplo con error de trascripción** → companion machine-readable + los DOS
   cálculos independientes por EVIDENCE_ID; discrepancia ⇒ STOP.
5. **PII colada al archive vía transcript** → scan obligatorio con oráculo; redacción marcada.
6. **El comparador semántico del ledger con bug que «pasa»** → es artefacto hasheado con
   self-test mínimo (caso rojo sintético: transición sin evento DEBE fallar).
7. **Dedupe que fusiona invariantes distintos** → prueba de equivalencia por fila; en duda no
   fusionar; racional en el capsule.
8. **Inventario semántico que blanquea un claim vivo como HISTORICAL** → todo HISTORICAL debe
   apuntar a su marcador de supersesión; sin marcador = FALSE_CURRENT_CLAIM.
9. **Promoción parcial por crash** → journal dos-fases + recovery definido + terminal
   `PARTIAL_RECOVERY_REQUIRED` si el restore no se confirma.
10. **Lock huérfano bloqueando para siempre** → TTL en el lock + override SOLO con decisión CEO
    explícita registrada.
11. **RUN_ID reutilizado** → verificación contra journals existentes en R4-0; colisión ⇒
    `CONFLICT_DETECTED`.
12. **El overlay reconstruye pero omite un untracked** → members.sha256 se genera DESDE el
    PRE_STATE manifest (misma lista), y la prueba de reconstrucción compara el root manifest
    completo, no una muestra.
13. **«Validador verde» leído como «ledger veraz»** → validador+comparador son sintaxis/estructura;
    la verdad la sostienen EVIDENCE_IDs; el packet lo declara.
14. **El índice de autoridad se desactualiza** → el índice lleva versión+fecha+hash y el handoff
    lo lista; toda decisión nueva en 09 DEBE tocar el índice (regla registrada en el propio índice).
15. **Este documento tratado como ejecutado o auto-autorizado** → banner `EXECUTE_STEP_7=DENIED`;
    terminales de §6 sin cambiar; el mandato exige hash de ESTE archivo, que el CEO fija fuera.

---

## 14. DECISIONES CEO REALMENTE BLOQUEANTES

**Única decisión: D-P7-A (custodia).** Recomendación primero: **Opción D** (§7) — procedencia
versionada + cuarentena del legado, con la forense YA ejecutada (NOT_FOUND ×3) y el register
conservado como documento vivo indexado. Trade-off honesto: cuesta un incidente formal y un índice
de autoridad; a cambio, ningún consumidor futuro puede leer autoridad stale y la auditoría externa
recibe hechos medidos. Alternativas A/B/C en §7 con sus costes.

*(No son decisiones: el formato de autorización — ya fijado por el mandato; la ubicación durable de
esta R4 — defaulted a `docs/engines/focus_v5/`, revocable por el CEO al autorizar; la supersesión
del criterio export de T-S5 — ya cerrada por el contrato CEO, solo se registra.)*

---

## 15. ESTADO RESUMIBLE

- **Qué pasó (2026-07-22):** red-team R4 del plan del Paso 7 bajo mandato CEO, read-only. Las 64
  hipótesis adjudicadas (§3: 61 CONFIRMED, 2 PARTIAL — 29 y 58 —, 1 UNKNOWN parcial — S-05; cero
  refutadas de plano). Hallazgo propio S-01: la brecha del bundle es **3/16** con bytes sellados
  `NOT_FOUND` (forense exhaustiva ejecutada). El scratchpad sobrevivió al reinicio (suerte);
  la R3 (`25dfe9ff…`) queda como evidencia.
- **Este archivo** = R4 candidata durable (`docs/engines/focus_v5/S5_R4_PASO7_PLAN_REVIEWED.md`),
  único escrito de la invocación. Nada ejecutado; canon/ledger/código intactos (verificable:
  git status idéntico al pre-shutdown, §2).
- **Siguiente acción exacta (no puede ser ejecutar la R3):** el CEO (1) decide **D-P7-A**
  (recomendada D), (2) fija el hash de esta R4 (reportado fuera de banda) y (3) emite:
  `EJECUTAR_PASO7_R4 PLAN_SHA256=<hash-de-este-archivo> CUSTODY_DECISION=<A,B,C,D> RUN_ID=<uuid-no-usado>`.
  La ejecución sigue R4-0→R4-8 y termina en UNO de los terminales de §6; después, packet a
  auditoría externa POR SCOPES (§11). `FROZEN_FOR_S5_R4`, montaje y steps 5–14 siguen prohibidos.
