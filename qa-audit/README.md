# QA Audit — MindOS / Mindverse (KMP)

Carpeta de trabajo de la **auditoría técnica multiagente** sobre la app Kotlin Multiplatform.

## Cómo está organizado

```
qa-audit/
  README.md                 ← este archivo
  audit-state.md            ← estado vivo (actualizar tras cada fase)
  function-inventory.md     ← inventario funcional (pantallas/flujos)
  platform-matrix.md        ← matriz plataforma × agente × feasibilidad
  test-account-notes.md     ← qué inputs faltan / qué hay disponible
  agents/
    brain.md                ← CEREBRO CENTRAL: roster de agentes + plan + lotes
  logs/                     ← salidas largas de comandos (no pegar en chat)
  evidence/                 ← evidencias por finding-id
  reports/
    findings.raw.md         ← hallazgos crudos por agente
    findings.deduped.md     ← deduplicados + severidad
    final-report.md         ← informe maestro (plantilla)
    regression-plan.md      ← plan de tests de regresión
  repro/                    ← repros mínimas
```

## Reglas de contexto (anti-explosión de tokens)

- El cerebro central solo recibe **resúmenes** (`AGENT_RESULT` ≤ 12 líneas) + rutas de archivos.
- Logs largos → `logs/`. Evidencias → `evidence/<finding-id>/`. **Nunca** pegar archivos enteros en chat.
- Cada fase debe re-pasar el **budget gate** de `CLAUDE.md`:
  `node .claude/scripts/budget-gate.mjs --task "..." --remaining N`
  y respetar su decisión (`allow | plan_only | split | deny`) **exactamente**.

## Estado de esta auditoría

Fase actual: **scaffold + plan** (única fase autorizada con el presupuesto actual ~14%).
Las fases de build, revisión estática profunda y pruebas en vivo están **pendientes de re-gate**.
Ver `audit-state.md` y `agents/brain.md`.
