# /ceo evals

Casos (`cases.json`) + rúbrica (`rubric.json`) para comparar una versión candidata de `/ceo` contra la
baseline y evitar regresiones. Método honesto según el entorno.

## Qué es ejecutable de verdad vs qué no

- **`EJECUTABLE`** — el **structural linter** sobre cualquier paquete producido:
  `node ../scripts/validate-ceo-package.mjs <paquete>.json`. Los casos `kind: structural` (B01, B07,
  B09, B11–B14, B16–B19) se juzgan mayormente así + inspección del paquete.
- **`NO DISPONIBLE`** — un harness que ejecute la skill `/ceo` de forma **headless N veces** y puntúe a
  ciegas baseline vs candidato con métricas de tokens/tiempo. El runtime de una skill es el propio
  modelo; no hay proceso que la corra sin un humano/loop. **No inventes números de A/B generativo.**
- **`VERIFICADO ESTÁTICAMENTE`** — comparación feature-level baseline↔candidato leyendo los artefactos:
  ¿intents? ¿versionado (`ceo_contract`)? ¿enlaza `PROMPT_ENGINEERING_PROTOCOL.md`? ¿DAG + validator?
  ¿trust/conflict? ¿requirement compiler? ¿resumibilidad? Cada "sí/no" es verificable abriendo los files.

## Cómo correr una evaluación

1. Fija rúbrica y pesos **antes** (`rubric.json` ya está congelado). No la cambies tras ver resultados
   salvo error objetivo (obliga a repetir baseline y candidato).
2. Para cada caso, produce el paquete `/ceo` (candidato). Materialízalo a JSON en el scratchpad si tiene
   DAG y córrele el validator.
3. `kind: structural`: comprueba `must`/`must_not` de `cases.json` + validator PASS. Una sola pasada
   basta si el resultado es determinista y validado por script.
4. `kind: generative`: si el entorno permite, ≥3 repeticiones; registra tasa de éxito y variación. Si no,
   **documenta la limitación**; no declares superioridad con una muestra.
5. Grading independiente cuando sea posible (autor ≠ grader, contexto limpio, orden aleatorio, sin
   revelar cuál es baseline/candidato).
6. Umbrales de cierre en `rubric.json` (invariantes de seguridad y validez estructural = 100%).

## Assertions críticas (deben ser 100%)

Ver `rubric.json → critical_assertions_must_be_100pct`. Las computables por el linter
(`hallucinated_*`, `cycles`, `write_conflicts`, `unresolved_inputs`, `untraced_requirement`,
`unverifiable_gates`) se leen directo de su informe `--json`.
