# S5 · R3 · PASO 7 — Plan de reconciliación documental + reseal del ledger

> **⚠️ SUPERSEDED-BY-OUTCOME (2026-07-22, reaudit Paso 7).** Este plan es HISTÓRICO/proceso (ver
> `CURRENT_AUTHORITY_INDEX §2`), NO autoridad de contenido. Su guardarraíl §0.2 **DISPARÓ**: el tracker
> `scratchpad/S5_R3_lens_integration.md` NO se conserva en el árbol, así que las frases de este plan que dirigen
> escribir «6/6 + 7/7 APTO gate-passed» (cabecera, T1 §7, T2 gate_result, T4, DoD) NO se cumplieron y NO se
> afirmaron: en TODOS los docs de autoridad/evidencia «7/7 APTO» quedó `MISSING_PRIMARY_EVIDENCE`. Lo que SÍ
> domina y está probado de primera mano (triple-verificado) es la **falsación de la R2** (`VERIFIED_PRIMARY`).
> Resultado real entregado: **R3 CANDIDATA** a auditoría externa. Leer este plan con ese override en mente.

> **Qué es esto.** El plan ejecutable del **Paso 7** de la reapertura externa R2→R3 de
> `ADR_FOCUS_TEMPORAL_QUALITY`. Paso 7 = **cerrar la reconciliación documental** (05/09/register/status/plan) y
> **resellar el ledger** para que la R3 candidata quede entregable a la siguiente auditoría de ChatGPT.
> **NO** ejecuta implementación (contrato steps 5–14). **NO** auto-congela. Techo honesto al terminar =
> **R3 CANDIDATA** (gate interno pasado), no `FROZEN_FOR_S5_R3`.
>
> **Estado de partida asumido** (verificar en §0 antes de tocar nada): la R3 ya está escrita en el ADR
> (`§R3-0..R3-22` sobre el divisor `# HISTORIA — R2 (SUPERSEDED)`); gate de entrada §8 **6/6** + gate de salida
> **7/7 APTO** con correcciones aplicadas (evidencia en `scratchpad/S5_R3_lens_integration.md`). Lo único que
> `S5_REOPEN_STATUS.md` todavía dice STALE es «gate BLOCKED por límite de sesión» (§0.4/§0.5 + cabecera): eso lo
> corrige este Paso 7.

---

## 0. Guardarraíles (leer antes de cada edición)

Reglas que dominan todas las tareas de abajo. Violar una = rehacer.

1. **Nunca «done / CABLEADO / persisted» en pasado para código PENDIENTE.** La R2 falló justo por esto. Todo lo
   de steps 5–14 (marker→proxy, gate `S_full`-o-`Absent` retirado, revocación cableada, thresholds persistidos,
   bins de tercio, `TemporalCapture` requerido, retención) es `IMPLEMENTATION = PENDING`. Usar
   `DECISION_STATUS` / `IMPLEMENTATION_STATUS` / `VALIDATION_STATUS` separados (patrón §R3-15 del ADR).
2. **Honestidad de gate.** Antes de escribir «7/7 APTO» en cualquier doc, **abrir `scratchpad/S5_R3_lens_integration.md`
   y confirmar el marcador «GATE DE SALIDA COMPLETO: 7/7 APTO … CORRECCIONES APLICADAS ✓»**. Si esa evidencia NO
   está, **parar**: mantener el texto BLOCKED y reportar el prerequisito exacto. No se afirma un gate sin su registro.
3. **Ledger = single-writer (sesión principal), append-only.** No borrar filas. No reutilizar IDs. `SUPERSEDED`
   apunta al ID nuevo. `MITIGATED`/`BLOCKED` ≠ `CLOSED`. Cerrar exige evidencia + gate, no narrativa. Sin `|`
   literales dentro de una celda (usar `abs(x)` u otra notación). Sin PII/secretos. `.gitattributes` ya fija
   `MINDBOOST_AUDIT_LEDGER.md text eol=lf` (MB-LEDGER-0007) — no cambiar EOL.
4. **Solo DOCS.** Paso 7 no toca `.kt`. Los cambios de código de steps 5–14 no son parte de esto.
5. **No re-congelar.** DoD de Paso 7 = reconciliación cerrada + ledger resellado + R3 devuelta para auditoría
   externa. `FROZEN_FOR_S5_R3` NO se emite en esta invocación (contrato §13: «Devuelve la R3 para una nueva
   auditoría de ChatGPT»).
6. **Leer el ancla vivo antes de editar.** Cada tarea abajo dice «leer X:líneas». Las líneas cambian; localizar
   el ancla real con `Grep`/`Read` justo antes del `Edit`.

---

## 1. Preflight (orden fijo, antes de editar)

- [ ] `git status --porcelain` — confirmar que el working tree = el del baseline de §1.3 de `S5_REOPEN_STATUS`
      (HEAD `8a06b9a`, rama `product/focus2-v5`; ajenos `DMListScreenIcon.ios.kt`/`AppMediaViewer.ios.kt` NO
      staged; código S5 propio intacto). Si difiere, reconciliar contra `scratchpad/S5_R3_baseline_status.txt`.
- [ ] Abrir `scratchpad/S5_R3_lens_integration.md` → confirmar gate 6/6 entrada + 7/7 salida APTO + correcciones
      aplicadas (guardarraíl §0.2). **Anotar los veredictos por lente** (se citan literalmente en 09 y en el ledger).
- [ ] `git rev-parse HEAD` y guardar (va en el checkpoint del ledger y en el evento).
- [ ] Backup del ledger antes de escribirlo: `git diff -- MINDBOOST_AUDIT_LEDGER.md > scratchpad/ledger_pre_R3.patch`
      (o copia del archivo). Recuperación garantizada si el reseal se corrompe.

---

## 2. Matriz de tareas documentales (T1–T6)

| # | Archivo | Acción | Ancla a leer primero | Estado de salida |
|---|---|---|---|---|
| T1 | `docs/engines/focus_v5/S5_REOPEN_STATUS.md` | Reemplazar texto STALE «BLOCKED por límite de sesión» por «gate 6/6 + 7/7 APTO, correcciones aplicadas»; append al log §7 | cabecera líneas 9–11 · §0.4 · §0.5 · §7 (fin) | R3 CANDIDATA, gate PASADO |
| T2 | `docs/context_pack/09_DECISION_LOG.md` | Nueva entrada de decisión R3 (falsación + D-R3-01..04 + resultado de gate) | §20.5 (última entrada Focus V5) + formato §22 | entrada `PROVISIONAL`/`REOPENED_AFTER_EXTERNAL_AUDIT` |
| T3 | `docs/context_pack/05_TECHNICAL_CURRENT_STATE.md` | Reflejar R3: IMPLEMENTATION=PENDING para los ítems de steps 5–14; retirar cualquier «cableado/persistido» | secciones focus2/temporal (grep `focus2`, `temporal`, `FROZEN_FOR_S5`) | estado técnico honesto |
| T4 | `docs/engines/focus_v5/ADR_REGISTER_V5.md` | Nota-puente en la fila del ADR temporal: `FROZEN_FOR_S5_R2` → `REOPENED_AFTER_EXTERNAL_AUDIT / R3 CANDIDATA` | fila `ADR_FOCUS_TEMPORAL_QUALITY` | fila actualizada + aviso diff-auditor |
| T5 | `FOCUS_ENGINE_MASTER_PLAN_V5` + `GATE_CATALOG_V5` | Escanear y corregir «vertical / export entregado / commit real / onset real» y estados inflados | grep de esos términos | sin claims falsos (o «no hits» registrado) |
| T6 | `MINDBOOST_AUDIT_LEDGER.md` | Reseal (findings + checkpoint + evento + SHA-256 + validador) | §2/§3/§4/§0 (ver §3 de este plan) | `LEDGER OK` del validador |

Orn de ejecución: **T1 → T2 → T3 → T4 → T5 → T6**. El ledger (T6) va **último** porque su §0 sella count+hash sobre
el estado final del propio ledger (no de los otros docs) — pero conviene tener T1–T5 cerrados para que el evento
del ledger los pueda citar como evidencia.

---

### T1 · `S5_REOPEN_STATUS.md` — retirar el STALE «BLOCKED»

**Por qué:** el gate ya corrió y pasó; §0.4/§0.5 y la cabecera aún dicen «BLOQUEADOS por límite de sesión». Es la
mentira de estado que el propio contrato prohíbe (en dirección inversa: aquí infra-declara un gate que SÍ pasó).

**Ediciones concretas:**

- **Cabecera (líneas ~9–11), bloque ` ``` `:** cambiar
  *«Gate §8 (6 lentes read-only) + gate de salida (7 lentes) BLOQUEADOS por límite de sesión … R3 = CANDIDATA, NO
  congelada.»* → *«Gate §8 (6 lentes read-only) COMPLETO + gate de salida (7 lentes) COMPLETO, 7/7 APTO,
  correcciones aplicadas al ADR. R3 = CANDIDATA (gate interno pasado), pendiente de auditoría externa; NO
  congelada.»*
- **§0.4** (líneas ~64–73): sustituir la narrativa «murieron por límite de sesión / prerequisito = reset» por el
  resultado real: los 6 agentes de entrada y las 7 lentes de salida **corrieron y devolvieron APTO** (citar la
  tabla de veredictos del tracker); la R3 no se congela porque el **contrato §13 exige devolverla a auditoría
  externa**, no por bloqueo de sesión. Renombrar el encabezado §0.4 a «Estado del gate §8/salida: COMPLETO — por
  qué la R3 sigue CANDIDATA (auditoría externa pendiente)».
- **§0.5** (líneas ~75–87): la checklist de reanudación pasa de «post-reset» a «post-gate»: marcar 1–4 como
  **hechos** (entrada + integración + R3 escrita + salida 7/7); dejar **5 (docs + ledger reseal)** como *este
  Paso 7 en curso*; y añadir **6. Devolver R3 a auditoría ChatGPT** como paso terminal de la invocación.
- **§7 Registro de progreso** (fin, ~línea 394): append una línea con fecha `2026-07-21`:
  *«Gate de salida R3 COMPLETO (7/7 APTO: efficacy · psychometrician · assessment · kmp-architect ·
  mobile-performance · privacy-security · diff-auditor); 8 correcciones no-bloqueantes aplicadas al ADR; ninguna
  falsación reproducible de la R3. Paso 7 (reconciliación docs + ledger) EN CURSO. R3 = CANDIDATA para auditoría
  externa; NO congelada.»*

**Verificación T1:** grep `BLOQUEAD|BLOCKED|límite de sesión|reset 22:50` en el archivo → 0 hits en §0/cabecera
(salvo dentro de «# HISTORIA»/citas si las hubiera).

---

### T2 · `09_DECISION_LOG.md` — entrada de decisión R3

**Por qué:** el decision log es la fuente canónica de decisiones; D-R3-01..04 y la falsación de la R2 deben quedar
registradas con su formato (§22).

**Contenido de la entrada** (nueva subsección tras §20.5, p. ej. `§20.6`), en el formato del doc:

- **Título:** «ADR-FOCUS-TEMPORAL — Reapertura externa R2→R3 (falsación de la R2)».
- **Estado:** `REOPENED_AFTER_EXTERNAL_AUDIT` · R3 = `PROVISIONAL`/`CANDIDATA` (no APPROVED).
- **Contexto/gatillo:** auditoría externa (ChatGPT) sobre la R2; rol confirmado = Claude implementador.
- **Falsación probada (domina):** el gate R2 §6.1 `S_full`-o-`Absent` con `mg-1` es **no-monótono**: con el scorer
  real (`DescriptiveScorer.dPrime` + `Probit`, `Z_95=1.959963984540054`, loglinear Hautus), sobre 30H/10M/10FA/30CR,
  D1 (excluir 5 misses) → `Absent`, pero D2 (excluir 5 misses + 15 hits, admisible: señal=20=`MinSampleGate.DPRIME`,
  ruido=40, exclusión 25% < 50%) → `Present(S_full)`. Más exclusión técnica ABRE el gate que menos exclusión
  cerraba. `CI-lower(S_full)>0` hace la elegibilidad dependiente del rendimiento (sesgo de selección longitudinal).
  **El mecanismo se retira como gate de elegibilidad.**
- **Decisiones cerradas** (copiar D-R3-01..04 de `S5_REOPEN_STATUS §0.2`): OI de la elegibilidad temporal · S5 no
  certifica accuracy/RT (shadow-only, `TRAINING_ONLY`/`NotCaptured`, `OFFICIAL_ELIGIBLE` inconstructible, S6 monta
  sin desbloquear medición) · sin «commit real» (`FRAME_ISSUED_PROXY`) · retención coherente con drain-not-adopt
  (purga-antes-de-leer, sin TTL wall-clock).
- **Resultado de gate:** entrada 6/6 + salida 7/7 APTO; sin falsación reproducible de la R3; correcciones
  aplicadas. Evidencia: `S5_REOPEN_STATUS §0` + `scratchpad/S5_R3_lens_integration.md`.
- **Alcance:** doc-only; ninguna implementación dependiente avanza; techo = CANDIDATA hasta auditoría externa.

**Nota:** si 09 tiene el ADR temporal en §20.5 como `PROVISIONAL` (Paso 2 R1), **no borrar** esa entrada; la nueva
la SUPERSEDE por referencia («§20.5 SUPERSEDED por §20.6 tras falsación externa»).

---

### T3 · `05_TECHNICAL_CURRENT_STATE.md` — estado técnico honesto

**Por qué:** describe el estado actual del repo; debe decir que la capa temporal es `CONTRACT_INERT_NO_WIRING`
(dominio probado en harness `desktopTest`; actuals solo compile-verified) y que los ítems de steps 5–14 son
`IMPLEMENTATION = PENDING`.

**Ediciones:**

- Grep `focus2`, `temporal`, `FROZEN_FOR_S5`, `cableado`, `persistid` en el archivo.
- Donde diga o insinúe que el vertical temporal está montado/cableado/persistiendo perfil: corregir a
  **construido pero NO montado** (focus2 fuera del paquete solo en `MindBoostStorageNamespaces` + `AppGraph`
  eraser; `mindboost_focus2` vacío en runtime; barrera = no-mount + test de bytecode
  `Focus2DependencyBoundaryTest`).
- Listar como PENDING (no hechos): marker→proxy no-gating, retiro del gate `S_full`-o-`Absent`, revocación
  subject-scoped cableada (GAP-1/GAP-2), thresholds/model versions persistidos, bins de tercio, `TemporalCapture`
  requerido en el sellado, retención.
- **Regresión conocida a comprobar** (hallazgo S5R-F2, §7 del status): §3/§4.1 de este archivo ya se corrigió de
  «Android 8/API 26» a API 29 — verificar que sigue en API 29 y no ha vuelto a driftar.

---

### T4 · `ADR_REGISTER_V5.md` — nota-puente

**Por qué:** el register es el índice de ADRs; la fila del ADR temporal debe reflejar la reapertura.

**Edición:** en la fila `ADR_FOCUS_TEMPORAL_QUALITY`:
- Estado: `FROZEN_FOR_S5_R2` → `REOPENED_AFTER_EXTERNAL_AUDIT · R3 CANDIDATA`.
- Nota: «R2 FALSADA (Absent→Present, contraejemplo reproducido con el scorer real); R3 candidata gate-passed
  (6/6+7/7 APTO), pendiente auditoría externa. Ver `ADR_FOCUS_TEMPORAL_QUALITY §R3-*` + `S5_REOPEN_STATUS §0`.»
- **AVISO diff-auditor:** el ADR es UNTRACKED (git status). Anotar que el register apunta a un artefacto no
  versionado todavía (se versiona al cierre real de S5, no en esta invocación).

---

### T5 · Plan + GATE_CATALOG — barrido de claims falsos

**Por qué:** contrato R3-AUD-12 exige que ningún doc afirme «vertical entregado / export entregado / commit real /
onset real / desbloquea rt−D por API / exactitud por NANOS_NATIVE / estado inflado».

**Procedimiento:**
- `Grep` en `FOCUS_ENGINE_MASTER_PLAN_V5*` y `GATE_CATALOG_V5*` (y cualquier `docs/engines/focus_v5/*.md` de plan):
  `commit real`, `onset real`, `vertical`, `export`, `NANOS_NATIVE`, `rt.?D`, `desbloquea`, `entregad`,
  `FROZEN_FOR_S5(?!_R)`.
- Cada hit: reformular a lenguaje epistémico honesto (`FRAME_ISSUED_PROXY`; export = «diferido con owner+gate»;
  T-S5 = `CONDITIONAL`/`REOPENED`, no PASS).
- Si **no hay hits**, registrarlo explícitamente en el status §7 («T5: barrido plan/GATE_CATALOG = 0 claims
  falsos») — un barrido silencioso se lee como «no revisado».

---

## 3. T6 · Reseal del ledger — sub-protocolo ordenado (la parte delicada)

Estructura real del ledger (verificada): §0 status + `Canonical SHA-256` (self-excluding) · §1 gobernanza · §2
findings register (fila = ID en backticks, ≥12 celdas) · §3 event history (append-only, filas con timestamp) · §4
task checkpoints · §5 evidencia de cierre · §6 supersession map · §7 open gates · §8 checklist+convención de hash ·
§9 ID policy. Hash actual sellado: `8526b509…ad60cf9`. Validador: `node scripts/validate-audit-ledger.mjs`.

**Ejecutar en este orden EXACTO (protocolo del CLAUDE.md raíz):**

1. **Leer el ledger entero** (o al menos §0, tail de §2, tail de §3, tail de §4, §6). Reconciliar el delta: ¿qué
   findings de focus2 ya existen? (§6.1 del status cita `MB-FOCUS-0026/0028/0035/0042` OPEN). **Localizar el
   siguiente ID libre** de la serie `MB-FOCUS-####` leyendo el máximo actual en §2 — **no reutilizar**.
2. **Añadir findings NUEVOS en §2** (solo hechos durables, no las 4 decisiones — esas van a 09). Candidatos:
   - `MB-FOCUS-####` **F-R3-FALSIFICATION** — «Gate R2 §6.1 `S_full`-o-`Absent`/`mg-1` no-monótono: exclusión
     técnica creciente abre el gate (Absent→Present); elegibilidad dependiente del rendimiento vía
     `CI-lower(S_full)>0`. Mecanismo retirado como gate.» Estado `OPEN` (el fix — retirar el gate — es
     IMPLEMENTATION PENDING). Evidencia: contraejemplo reproducido (`DescriptiveScorer`+`Probit`), doble lente.
   - `MB-FOCUS-####` **F-R3-STATUS-INFLATION** — «R2 afirmó done/CABLEADO/persistido para código DESIGNED_NOT_RUN
     (marker gatea L1, revocación no cableada, versiones no persistidas, `seal()` params opcionales). R3 separa
     DECISION/IMPLEMENTATION/VALIDATION.» Estado `OPEN`.
   - `MB-FOCUS-####` **GAP-1/GAP-2 revocación** — «`SecureFocusSessionStore`/`SecureFocusConsentStore` sin cablear
     a `SubjectRevocationAuthority`; carrera `checkpoint()`-tras-`clearNamespace()`. Cierre S5 exige cablear o
     CONDITIONAL con owner+gate.» Estado `OPEN` (o `BLOCKED` si se prefiere marcar dependencia).
   - `MB-FOCUS-####` **F2 borrado colateral** + **PRIV-D1 retención sin TTL** si no están ya como filas.
   > Si alguno YA existe como fila OPEN, **actualizar esa fila** (no crear duplicado): añadir la evidencia R3 a su
   > celda de notas y dejar el estado como está (OPEN sigue OPEN; no cerrar sin gate).
3. **Actualizar findings afectados:** si el ADR temporal tenía una fila de finding en estado que la R2 marcó
   MITIGATED/CLOSED indebidamente, `REOPEN` esa fila (prev→REOPENED) apuntando a la falsación. Verificar el
   invariante register↔event del validador (la última materialización del evento debe concordar con el estado del
   register).
4. **Append un evento en §3** (una fila, formato existente):
   `| 2026-07-21T…Z | TASK-2026-07-21-FOCUS-V5-S5-R3-RECON | \`MB-FOCUS-####\`(los nuevos) | OPEN | OPEN | — |
   R3: R2 falsada (Absent→Present con scorer real); gate 6/6+7/7 APTO; D-R3-01..04 cerradas en 09 §20.6; docs
   05/09/register/status/plan reconciliados; capa temporal CONTRACT_INERT_NO_WIRING | S5_REOPEN_STATUS §0 +
   scratchpad/S5_R3_lens_integration.md | — | primary session |`
   (Sin `|` literales dentro de las celdas; sin PII.)
5. **Append un checkpoint en §4** (una fila por task): `TASK-2026-07-21-FOCUS-V5-S5-R3-RECON` con HEAD `8a06b9a`,
   gates: sin re-run de Gradle (Paso 7 es doc-only; citar el run 2 verde del baseline 3965/0/0/0 como estado del
   árbol), resultado = «docs reconciliados + R3 candidata; NO congelada».
6. **NO cerrar nada como CLOSED** salvo que tenga evidencia + gate real. La R3 es candidata: sus findings quedan
   OPEN/REOPENED.
7. **Recomputar §0 (count + SHA) el ÚLTIMO.** Método robusto en Windows (evita `grep`/`sha256sum`):
   - Poner un placeholder en la línea `Canonical SHA-256 …: <PENDING>` (o dejar el hash viejo).
   - Actualizar el **count** de findings en §0 al número real de filas de §2.
   - `node scripts/validate-audit-ledger.mjs` → imprimirá `hash mismatch: stamped=… actual=<HASH_REAL>`.
   - Pegar `<HASH_REAL>` en la línea `Canonical SHA-256`.
   - Re-ejecutar `node scripts/validate-audit-ledger.mjs` → debe imprimir **`LEDGER OK — N findings, hash matches
     (…)`**. Ese OK es el gate de T6.
8. **Verificar que ningún finding previo desapareció** (diff contra `scratchpad/ledger_pre_R3.patch`: solo
   adiciones, cero borrados de filas).

**Comando de gate T6 (único):**
```bash
node scripts/validate-audit-ledger.mjs
```

---

## 4. Definition of Done (Paso 7)

Paso 7 está COMPLETO cuando **todas**:

- [ ] T1: `S5_REOPEN_STATUS` sin texto STALE «BLOCKED»; §0.4/§0.5/§7 reflejan gate 6/6+7/7 APTO (evidencia
      confirmada en el tracker).
- [ ] T2: entrada R3 en `09_DECISION_LOG §20.6` (falsación + D-R3-01..04 + resultado de gate).
- [ ] T3: `05_TECHNICAL_CURRENT_STATE` marca la capa temporal como no-montada + steps 5–14 como PENDING; API 29 intacto.
- [ ] T4: fila del ADR en `ADR_REGISTER_V5` = `REOPENED_AFTER_EXTERNAL_AUDIT / R3 CANDIDATA` + nota-puente + aviso UNTRACKED.
- [ ] T5: plan + GATE_CATALOG sin claims falsos (o «0 hits» registrado en §7 del status).
- [ ] T6: ledger resellado; `node scripts/validate-audit-ledger.mjs` → `LEDGER OK`; solo adiciones (cero borrados).
- [ ] **Ningún doc dice `FROZEN_FOR_S5_R3`.** El estado entregado es **R3 CANDIDATA**.

**Al cerrar la DoD:** producir el **cierre §13 (15 partes)** del contrato y **devolver la R3 candidata + sus
pruebas/falsaciones + esta reconciliación** para la siguiente auditoría de ChatGPT. **No** continuar con
implementación (steps 5–14) — es otra invocación, dependiente de que la auditoría externa apruebe la R3.

---

## 5. Riesgos / notas

- **Riesgo A — afirmar el gate sin evidencia.** Si el tracker no confirma 7/7 APTO, T1 NO se ejecuta: se mantiene
  BLOCKED y se reporta. (Guardarraíl §0.2.)
- **Riesgo B — hash irreproducible.** El validador computa el hash self-excluding sobre el archivo con EOL LF; el
  `.gitattributes` ya lo fija. Si el validador da mismatch tras pegar el hash, el archivo se materializó con CRLF
  → renormalizar (`git add --renormalize MINDBOOST_AUDIT_LEDGER.md` o reescribir con LF) y recomputar. (Learning
  2026-07-20.)
- **Riesgo C — duplicar un finding.** Antes de crear `MB-FOCUS-####`, grep el ID y el título en §2/§6: si ya
  existe, actualizar la fila, no duplicar.
- **Riesgo D — `|` en celda de tabla del ledger.** Cualquier fórmula con valor absoluto va como `abs(x)`, nunca
  `|x|` (rompe el troceo por `|` del validador). (Learning 2026-07-20.)
- **Riesgo E — tocar código.** Si aparece la tentación de «arreglar» un `.kt` (p. ej. retirar el gate
  `S_full`-o-`Absent`), NO: eso es step 5–14, fuera de Paso 7 y fuera de esta invocación.
- **ADR UNTRACKED:** `ADR_FOCUS_TEMPORAL_QUALITY.md`, `S5_REOPEN_STATUS.md`, `S5_EXECUTION_PROPOSAL.md` y este plan
  son untracked. No se ofrece commit/PR (regla de entorno). Se reporta el working-tree al terminar.

---

## 6. Resumen de una línea

Paso 7 = cerrar 5 docs (status/09/05/register/plan) para que digan la verdad (R2 falsada, R3 candidata, steps
5–14 PENDING) + resellar el ledger con validador verde, **sin congelar y sin tocar código**, y devolver la R3 a la
auditoría externa.
