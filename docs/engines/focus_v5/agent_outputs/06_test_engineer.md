# Orden 3 — test-engineer (2026-07-19) — falsación del borrador integrado

> Parte 1 (matriz de 47 escenarios) materializada e integrada en REDTEAM_TEST_MATRIX_V5.md (con los 11
> oráculos faltantes resueltos por la sesión /mb, marcados ★). Aquí se registra la Parte 2 (hallazgos) y su
> disposición. Veredicto del agente: "V5 corrige genuinamente casi todos los defectos estructurales de V4…
> pero no puede pasar T-PLAN tal como está" → REOPEN parcial acotado, aplicado íntegramente antes del freeze.

## Hallazgos y disposición

| ID | Hallazgo | Disposición |
|---|---|---|
| P0-1 | 3 artefactos citados no existían (REDTEAM/REVIEWS/MANIFEST) | RESUELTO: REDTEAM_TEST_MATRIX_V5.md escrito (la propia Parte 1 + oráculos); REVIEWS_V5.md existía ya (escrito tras lanzar al agente — su snapshot era anterior); SHA256_MANIFEST_V5.txt lo genera el freeze (tools/freeze_v5.cjs) y es la evidencia de T-PLAN |
| P0-2 | Tokens de gate del CSV fuera del catálogo (DPIA, Protocolo etico, Threat model, R-24, VAL-008, "P3 gate…", "hand-off"…) + tercera fuente de vocabulario | RESUELTO: GATE_CATALOG §2.1 "Hitos y decisiones NO-gate" define los tokens narrativos; CSV post-procesado ("P3 gate 2-metodos"→T-SCI-7; VAL-008→FOC-VAL-008 (dependencia); "ADR target band"→ADR-FOCUS-SCORING); regla: ningún hito no-gate puede ser exit-gate único |
| P0-3 | CSV mezcla vocabularios/diverge del plan §5 por fila | RESUELTO (precedencia declarada): plan §6 — las celdas del CSV heredan granularidad del pack; en divergencia ganan catálogo+plan §5 |
| P1-1 | Sin evidencia del pipeline de revisión | RESUELTO: REVIEWS_V5.md registra órdenes 1-3 y el diff-auditor (orden 4) corre ÚLTIMO sobre el manifiesto final |
| P1-2 | T-ADV podría fallar también en v2 (v1 también era "accuracy-based" sin chance-floor); sin prueba de concepto previa | RESUELTO: S2 tarea (5) — simulación JVM del scorer candidato (CON corrección de azar) contra los 5 perfiles ANTES de comprometer S3 |
| P1-3 | "alineado 1:1 con los 16" aritméticamente imposible | RESUELTO: ADR-SESSION-STATE precisado — el sealed cubre el SUBCONJUNTO de ciclo de vida; eventos de trial/infra no son estados |
| P1-4 | ADR-FOCUS-DATA sin protocolo de reapertura | RESUELTO: protocolo de reapertura añadido (regímenes por fila; (ii)→(i) purga+re-BLOCKED; (i)→(ii) solo prospectivo; re-DPIA) |
| P1-5 | §3 contradecía el degradado del ADR-CONSTRUCT | RESUELTO: §3 reescrito (5 = navegación; 3 candidatas; 2 nunca-subscore salvo evidencia nueva) |
| P1-6 | ¿Eraser especificado o implementado en S1? | RESUELTO: catálogo aclara T-S1 = IMPLEMENTADO (tipos+wiring+coverage-test, sin UI); diff con privacy-security |
| P2-1 | switch-access ausente | RESUELTO: REDTEAM #34 + portfolio regla 10 + S3 tarea (6) — modalidad secuencial obligatoria en choice, registrada como modalidad distinta |
| P2-2 | Fix C.1 (Hz real) sin tarea explícita | RESUELTO: S5 tarea (0) prerrequisito con test 60-vs-120 |
| P2-3 | ⚠re-scoring solo marcado en E07/E08 | RESUELTO: portfolio regla 9 — TODA fila reuse-v1 implica re-scoring; gobernantes v1 citados retirados |
| P2-4 | S11 sin los 15 campos | RESUELTO: S11 expandida a los 15 campos a nivel de programa (protocolo por estudio = entry-gate, no sustituto) |
| P2-5 | Ambigüedad T-DR (uno global vs por slice) | RESUELTO: catálogo — T-DR es POR SLICE; T-PLAN+T-DR inicial solo autoriza R0 (GOV-013) |
| P3-1 | "V5" ambiguo (plan vs gate) | RESUELTO: nota tipográfica en la cabecera del plan |
| P3-2 | Aritmética de cobertura del portfolio | control positivo (sin cambio) |
| ★×11 | 11 escenarios sin oráculo (11,13,16/40,18,19,22,25,26,33,42,43) | RESUELTOS en REDTEAM_TEST_MATRIX_V5.md con oráculo concreto cada uno |
| #25 | GAP sequenceNo asignado a S4 sin tarea real | RESUELTO: reasignado a S8 (journal) en ADR-FOCUS-DATA y REDTEAM |
| #35 | Cap de dosis solo para bloques largos | RESUELTO: REDTEAM #35 — cap general de EVIDENCIA diaria (interino, ADR-SCI-005); exceso→TRAINING_FREE (SES-008), jamás bloqueo de jugar |
| #36 | Falso positivo contra experto real | RESUELTO: T-ADV incluye perfil EXPERT además de HONEST (REDTEAM #36; S3 tarea 7) |
| #24/#31 | N/A no declarados (sync; permisos) | RESUELTOS: declarados NOT_APPLICABLE con condición de reapertura (REDTEAM #24/#31) |
| #26 | seed wall-clock (FocusGraph:131-140) sin slice asignada | RESUELTO: S3 tarea (9) — seed de SeedSource persistida pre-trial (golden) |
| #42 | 8 estados sin matriz de transiciones testeable | RESUELTO: REDTEAM #42 — tabla sellada (estado×evento) + property test exhaustivo, contrato S1 |

## Métricas del dictamen
47 escenarios · hallazgos: 3×P0, 6×P1, 5×P2, 2×P3 · TODOS con disposición registrada arriba · veredicto
post-corrección pendiente del diff-auditor (orden 4) sobre el bundle congelado.
