# Execution DAG & Validation Protocol (/ceo)

Gobierna etapas 8, 11 y 14. Todo plan Profesional+ se representa como un **DAG de nodos** conforme a
`schemas/execution-package.schema.json` y se valida con `scripts/validate-ceo-package.mjs` antes de
entregarse. El paquete es DATA; el linter no ejecuta nada de él.

## 1. Contrato de nodo

```yaml
task:
  id:                    # único
  title:
  type:                  # DISCOVERY|RESEARCH|DECISION|SPECIFICATION|IMPLEMENTATION|MIGRATION|TEST|REVIEW|RED_TEAM|INTEGRATION|GATE|DOCUMENTATION
  objective:             # obligatorio
  owner:                 # agente real | inline | main-session | founder
  domain:
  risk: low|medium|high|critical
  dependencies: []       # ids de nodos que deben completar antes
  entry_conditions: []
  input_artifacts: []    # producidos por una dependencia o listados en external_artifacts
  output_artifacts: []
  read_set: []
  write_set: []          # rutas que el nodo escribe (para serializar)
  context_capsule: {}    # ver Trust protocol §6
  decision_rights:       # autónomas | escalables | defaults
  tools: []
  commands: []           # se validan contra .claude/commands
  agent:                 # se valida contra .claude/agents
  model:                 # relativo (eficiente|alta capacidad); no hardcodear nombres volátiles
  effort: low|medium|high|xhigh|max
  max_turns:             # o exit_conditions, para acotar loops
  isolation: none|worktree
  parallel_group:        # nodos del mismo grupo corren a la vez → write_sets disjuntos
  acceptance_criteria: []# observables, obligatorio
  verification: []       # comandos/tests/inspecciones (obligatorio en GATE/TEST/REVIEW/RED_TEAM)
  evidence_required: []
  failure_policy:
  retry_policy: { max:, on: }
  rollback:              # obligatorio en Crítica si hay write_set y riesgo alto
  exit_conditions: []
  handoff_to:            # nodo consumidor | final
```

`external_artifacts: []` a nivel de paquete declara los inputs que vienen de fuera del DAG (la petición
del founder, el estado del repo, un asset). Un input que no produce ninguna dependencia ni está en
`external_artifacts` es un **error** (`unresolved_input`).

## 2. Validaciones obligatorias (las que impone el linter → error) 

Grafo acíclico · todo nodo con objetivo/owner/acceptance · ids únicos · dependencias existentes · cada
input con productor o external · cada output crítico consumido o `handoff_to: final` (si no, warning
`orphan_output`) · sin write-conflicts (dos nodos escriben la misma ruta sin orden ni relación de
dependencia, o comparten `parallel_group`) · sin paralelización con dependencia semántica · todo
requisito material trazado a un nodo o a estado `out_of_scope`/`deferred`/`blocked`/`done` · gates con
`verification`/`evidence_required` · `verification_gate` de un requisito apunta a un nodo GATE/TEST/
REVIEW · agentes/commands/paths existentes (no inventados) · sin placeholders en acceptance/verification
· sin operaciones destructivas en verification/rollback/tools · integrador presente si Enterprise+ tiene
>1 flujo terminal · `schema_version` compatible con el validator.

Warnings (no bloquean pero se reportan): `orphan_output`, `missing_rollback` (fuera de Crítica),
`no_turn_bound`, `node_no_exit`, `overengineered_direct`, `missing_skill_version`.

Correr el gate:
```
node .claude/skills/ceo/scripts/validate-ceo-package.mjs <paquete>.json        # humano
node .claude/skills/ceo/scripts/validate-ceo-package.mjs <paquete>.json --json # máquina
node .claude/skills/ceo/scripts/validate-ceo-package.mjs --selftest            # prueba del propio linter
```
`status: FAIL` ⇒ el paquete **no se entrega**. Reporta el informe con su estado real (`EJECUTADO Y
VERDE`/`FALLIDO`); nunca lo declares sin correrlo. Materializa el paquete a JSON en el scratchpad para
validarlo; no lo dejes en el repo salvo que el founder lo pida.

## 3. Deriva del repositorio (drift)

Antes de ejecutar un nodo que dependa del estado del repo, revalida commit, paths, símbolos,
precondiciones y working-tree relevante (`git rev-parse HEAD`, `git status`). Recuerda: el working-tree
puede tener cambios ajenos en paralelo (Vibe Kanban/n8n). Si hay deriva:
```yaml
drift:
  detected: true
  affected_nodes: []
  changed_assumptions: []
  required_action:      # revalidar precondiciones | recompilar nodo | escalar
```
No continúes ciegamente con un plan obsoleto.

## 4. Fallos y terminación

Taxonomía: `TRANSIENT` · `IMPLEMENTATION` · `PREEXISTING` · `ENVIRONMENT` (herramienta/capacidad
ausente) · `REQUIREMENT` (contradictorio) · `DECISION` (necesita humano) · `SECURITY` (obliga a parar) ·
`BUDGET`. Reintentos por defecto: transient ≤2 · implementación ≤3 ciclos · review `NO_APTO` ≤3 antes de
escalar · error estructural del paquete: corregir hasta PASS, ≤3 reconstrucciones. Superado el límite:
no declares éxito, entrega diagnóstico, continúa con nodos independientes, escala solo el bloqueo real.
Todo nodo delegado tiene `max_turns` o `exit_conditions` (sin loops indefinidos).

## 5. Resumibilidad (etapa 14)

Cada paquete incluye estado suficiente para reanudar en otra sesión sin la conversación entera:
```yaml
execution_state:
  package_id:
  schema_version:
  skill_version:
  repository_snapshot:   # commit
  completed_nodes: []
  active_nodes: []
  blocked_nodes: []
  produced_artifacts: []
  gate_results: []
  unresolved_decisions: []
  assumptions: []
  detected_drift: []
  next_node:
  resume_instructions:   # qué leer y por dónde seguir (no "git reset" ni destructivo)
```
Tras compactación o cambio de sesión, retomar = leer `execution_state`, revalidar deriva (§3), continuar
por `next_node`.
