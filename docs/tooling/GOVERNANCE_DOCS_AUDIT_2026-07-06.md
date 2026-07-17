# Auditoría y reconstrucción de los docs de gobernanza — 2026-07-06

Tarea solo-docs sobre `CLAUDE.md` y `AGENTS.md` (raíz). Objetivo: máxima señal por token,
exactitud factual contra el repo real, cero pérdida del ledger de aprendizajes, contrato del
bucle de aprendizaje intacto. **No se tocó código; no se corrieron gates Gradle** (desktopTest /
detekt / kover no aplican a un cambio solo-docs y gastarían el daemon del IDE sin aportar).

Backups para rollback:

- `docs/tooling/CLAUDE.md.bak-2026-07-06`
- `docs/tooling/AGENTS.md.bak-2026-07-06`

---

## Fase 0 — Hallazgos de auditoría

### a) Duplicación entre archivos (estado previo)

| Bloque | CLAUDE.md (antes) | AGENTS.md (antes) |
|---|---|---|
| Protocolo del bucle de aprendizaje (Regla núcleo → Referencia rápida) | líneas 154–257 | líneas 17–120 (byte-idéntico) |
| Doctrina de producto "Mindverse / Mindboost — … Project Memory/Instructions" | cola, líneas 3558–3608 | cola, líneas 2431–2510 (casi idéntica; AGENTS añadía dominios canónicos, conceptos backend y quality gates) |
| "Mindverse Enterprise … Memory/Instructions" | cola, líneas 3612–3659 | cola, líneas 2514–2561 (idéntica salvo título) |
| ~60 entradas del ledger repetidas en ambos archivos (2026-05-26 → 2026-06-20) | sí | sí |

Además CLAUDE.md duplicaba la regla Graphify (sección larga al inicio + sección `## graphify` al
final).

### b) Entradas legacy de otra herramienta en AGENTS.md (7)

Reubicadas al `## Apéndice — aprendizajes de otras herramientas (revisar / posible retirada)`,
**verbatim**, marcadas para decisión del usuario:

1. 2026-06-25 — write_to_file restringe ArtifactMetadata (tool de Antigravity/Gemini)
2. 2026-06-21 — replay omitía validación de transcripts (`.antigravity-only/scripts/anti_cli.py`)
3. 2026-06-21 — `datetime.utcnow()` deprecado (`anti_runner.py`, `scratch/`)
4. 2026-06-21 — llaves de diccionarios Python (`anti_agent_registry.py` — no existe en el repo)
5. 2026-06-21 — validador de secciones vacías (`verify_subagents.py`, `.antigravity-only/plans/`)
6. 2026-06-21 — `$PSScriptRoot` tras `param` (`scripts/security/*.ps1`, paquete Codex)
7. 2026-06-21 — PowerShell reserva `$Host` (`scripts/security/egress-allowlist-check.ps1`)

Nota: `.antigravity-only/`, `scratch/` y `scripts/security/` SÍ existen en el repo; las entradas
describen otro runtime, no el código KMP. Las dos de PowerShell contienen lecciones de shell
reutilizables — señalado en la cabecera del apéndice.

### c) Afirmaciones desactualizadas detectadas y corregidas

- **Caveat de hooks Graphify obsoleto** (CLAUDE.md antiguo, "Automatización y hooks"): decía que
  los recordatorios eran no-op silencioso por el stub `python3`. Reemplazado por el estado real
  post-fix 2026-07-06 (lógica consolidada en `.claude/hooks/mindos-guard.cjs` vía Node,
  advisory, verificada en vivo — el propio ledger 2026-07-06 lo documenta).
- La doctrina de la cola usaba lexemas del léxico prohibido en disclaimers negados; reescrita en
  §1 con lenguaje seguro y remitiendo la autoridad al `ForbiddenLexicon.kt` + scanners.

### d) Conteo de entradas `### YYYY-MM-DD` (no-pérdida)

| Archivo | Antes | Después | Δ |
|---|---|---|---|
| CLAUDE.md | 125 | 125 | 0 |
| AGENTS.md | 110 | 110 (103 en ledger + 7 en apéndice) | 0 |

---

## Cambios aplicados (Fases 1–3)

### CLAUDE.md (3669 → 3623 líneas)

- Front-matter nuevo (§1–§6): identidad + doctrina de producto (incluye dominios cognitivos
  canónicos, conceptos canónicos y quality gates que antes solo estaban en AGENTS.md); rigor
  charter; mapa REAL del repo con tabla de tareas Gradle verificadas; reglas de entorno;
  persistencia y datos; navegación de contexto (Graphify condensado + toolkit + agent-browser +
  docs clave).
- Cola duplicada eliminada (las dos secciones "Project Memory" + `## graphify` final): su
  contenido quedó fusionado en §1–§2 y §6.
- Protocolo del bucle de aprendizaje y ledger completo: **byte-idénticos** al backup
  (verificado por comparación de la porción completa).

### AGENTS.md (2600 → 2484 líneas)

- Cabecera nueva: declara audiencia (agentes no-Claude) y deferencia explícita a CLAUDE.md §1–§6.
- Protocolo del bucle: intacto (byte-idéntico al de CLAUDE.md).
- Ledger: intacto salvo la reubicación de las 7 entradas legacy al apéndice final (verbatim).
- Cola: doctrina duplicada (Codex Project Instructions + Enterprise) sustituida por una nota de
  deferencia; los dominios/conceptos/quality-gates únicos se movieron a CLAUDE.md §1.
- Preservado verbatim: "Perfil privado de ciberseguridad de Codex" + "Paquete agentic de
  ciberseguridad exclusivo de Codex" (reglas específicas de agente, incl. marcadores
  `BEGIN/END CODEX_SECURITY_AGENTIC_PACKAGE`).

Reducción de duplicación de front-matter/doctrina: ~250 líneas netas entre ambos archivos
(CLAUDE −46 netas con front más rico; AGENTS −116), con el conocimiento fusionado, no borrado.

---

## Verificación (solo-docs)

Ejecutada con Node (fiel a bytes; los archivos son CRLF uniforme y así se conservaron):

1. **No-pérdida:** 125/125 y 110/110 entradas `### YYYY-MM-DD` (tabla arriba).
2. **Contrato del bucle:** bloque "Regla núcleo → Referencia rápida" **byte-idéntico** entre
   CLAUDE.md y AGENTS.md, e idéntico al backup.
3. **Léxico restringido** (`clinic|clínic|diagnos|IQ|terapéut|therapeut|médic|medical|ADHD|TDAH|
   demencia|dementia|gambling`, case-insensitive):
   - Prosa nueva (front-matter/cabeceras/apéndice): **0 hits**, con UNA excepción deliberada:
     CLAUDE.md línea 65 contiene el identificador literal del subagente existente
     `mindboost-assessment-diagnostics` (nombre de herramienta del harness; renombrarlo en el doc
     rompería la referencia). Decisión pendiente del usuario si quiere renombrar el agente.
   - Hits restantes: 22 en CLAUDE.md y 8 en AGENTS.md, TODOS dentro de entradas del ledger
     preservadas verbatim (citan literales de scanners/copys corregidos). Protegidos por el
     contrato de no-pérdida.
4. **Exactitud factual:** verificados contra el repo — módulos `:shared`/`:androidApp`/
   `:desktopApp` + `includeBuild("mindos")` (`settings.gradle.kts`); tareas
   `arenaTerminologyScanner`/`mindboostCopySafetyScanner` con variantes `CommonTest`/`DesktopTest`
   y `koverVerifyDesktop` (`shared/build.gradle.kts`); rutas citadas existen
   (`MindBoostHubScreen.kt`, `AppGraph.kt`, `ForbiddenLexicon.kt`, `mindos-guard.cjs`,
   `graphify-refresh.sh`, `docs/MindBoost_PRD_Completo.md`, `docs/architecture/`,
   `docs/mindboost_exercise_design/`, `docs/AI_WORKFLOW.md`, `docs/VIBE_KANBAN_SETUP.md`,
   `.claude/README.md`, `graphify-out/GRAPH_REPORT.md`).
5. **Enlaces markdown relativos:** 5 (CLAUDE) + 3 (AGENTS), todos resuelven a archivos existentes.

## Qué NO se tocó y por qué

- Ninguna entrada del ledger (contenido): contrato de no-pérdida.
- El paquete de ciberseguridad de Codex en AGENTS.md: reglas activas específicas de ese runtime.
- Las 7 entradas legacy NO se borraron (solo reubicadas): la poda es decisión del usuario.
- Código, gates Gradle, `graphify-out/`: fuera de alcance de una tarea solo-docs.

## Riesgos / decisiones pendientes del usuario

1. Poda (o traslado a `docs/`) de las 7 entradas legacy del apéndice de AGENTS.md.
2. El identificador `mindboost-assessment-diagnostics` en CLAUDE.md §1 (único lexema restringido
   fuera del ledger); renombrar el subagente sería un cambio de harness, no de docs.
3. ~60 entradas del ledger siguen duplicadas en ambos archivos (2026-05-26 → 2026-06-20): la
   deduplicación entre ledgers está PROHIBIDA por este encargo; si algún día se quiere un ledger
   único con referencias, es un encargo aparte.
4. PROPUESTA: refrescar `graphify update .` (AST-only) para que el grafo indexe la nueva
   estructura de encabezados de ambos .md — opcional para docs, no ejecutado aquí.

## Siguiente paso recomendado

Endurecer la gobernanza con un guard estático ligero (test Node o check en `mindos-guard.cjs`)
que valide en cada sesión: (a) el bloque del protocolo sigue byte-idéntico entre ambos archivos,
(b) el conteo de entradas nunca decrece, y (c) la prosa fuera del ledger no introduce lexemas del
`ForbiddenLexicon`. Hoy esa invariante solo se sostiene por disciplina.
