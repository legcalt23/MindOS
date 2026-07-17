# Ejemplo — Enterprise (B03): ejercicio cognitivo nuevo que actualiza el perfil

**Entrada:** `/ceo crea un ejercicio nuevo de memoria de trabajo que actualice el perfil`

**Modo:** Enterprise (science=3, scoring=3 → eje sensible fuerza Enterprise mínimo; total 20). DAG
completo, especialistas mínimos, integrador, gate de salida, validator.

El paquete completo y **machine-checkable** está en [`new-exercise.package.json`](new-exercise.package.json).
Pasa el linter con 0 errores / 0 warnings:

```
$ node .claude/skills/ceo/scripts/validate-ceo-package.mjs examples/enterprise/new-exercise.package.json
validation:
  status: PASS
  metrics: graph_nodes: 7 · requirements_traced: 2/2 · hallucinated_entities: 0 · write_conflicts: 0 · unverifiable_gates: 0
```

## Forma que enseña
- **DAG de 7 nodos** con gate de entrada científico → arquitectura → (motor ‖ UI en `parallel_group: build`
  con write_sets disjuntos) → tests → integración → gate de salida. `engine` y `ui` son paralelos porque
  dependen ambos de `arch`, **no** entre sí (si dependieran, el linter marcaría `invalid_parallel`).
- **Trazabilidad**: R1 (jugable) y R2 (scoring honesto null!=0 + anti-gaming) trazan a `engine` y su gate
  `gate_out`. Un requisito material sin nodo → `untraced_requirement`.
- **Routing real**: solo agentes existentes (`efficacy-guardian`, `psychometrician`, `kmp-architect`,
  `cognitive-engine-dev`, `compose-ui-dev`, `test-engineer`, `diff-auditor`). Inventar uno → `hallucinated_agent`.
- **Rigor charter**: R2 fija `null != 0`, contribución solo vía `ScoreResult` sellado y anti-gaming con
  red-team; el gate de salida lo verifica sobre el diff final.
- **Integrador real** (`integrate`, owner `main-session`) + **estado resumible** (`next_node: gate_in`).
- **Ownership**: cada `write_set` es disjunto (memory/domain vs memory/presentation vs desktopTest/memory).

## Secuencia
`gate_in` → `arch` → {`engine` ‖ `ui`} → `tests` → `integrate` → `gate_out`. Ejecuta la sesión principal
vía `/mb` (nombrando los agentes del paquete). Revalida deriva (`git status`) antes de `engine`.
