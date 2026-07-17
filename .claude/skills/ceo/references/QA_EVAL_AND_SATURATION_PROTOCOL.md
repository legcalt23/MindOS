# QA, Eval & Saturation Protocol (/ceo)

Gobierna etapa 12 y el cierre. Revisión independiente, integrador real, red-team, evals, saturación,
memory write-back, migración de schema y qué secciones emite cada modo.

## 1. Revisión independiente (por riesgo material, no por cantidad)

Dominios posibles: ciencia, psicometría, scoring, privacidad, seguridad, arquitectura KMP, integridad
de datos, UX, accesibilidad, rendimiento, claims, diff transversal. **Prevención de sesgo:** el primer
pase del reviewer recibe requisitos + spec + artefacto/diff + criterios + fuentes; **no** recibe
autodefensa del constructor, su puntuación, la conclusión deseada ni justificaciones persuasivas.
```yaml
review:
  reviewer: ; domain: ; targets_node:
  status: APTO|APTO_CON_CAMBIOS|NO_APTO
  critical_findings: []; high_findings: []; medium_findings: []; low_findings: []
  evidence: []; required_changes: []; residual_risk: ; confidence:
```
Un `NO_APTO` o hallazgo crítico vuelve al nodo responsable. Severidad (de `docs/context_pack/02 §8`): **P0**
bloqueante inmediato (datos/score falso válido/seguridad/claim peligroso/corrupción) · **P1** bloquea
cierre/wiring (contradicción de fuente, fórmula incorrecta, test crítico ausente, integración no
demostrada, camino a falso progreso, plataforma sin verificar) · **P2** mejora importante · **P3**
refinamiento seguro in-scope.

## 2. Integrador real (no un resumen)

Verifica que todos los outputs existen · comprueba versiones y trazabilidad · resuelve contradicciones e
interfaces · integra recomendaciones compatibles y **rechaza las incorrectas con evidencia** · elimina
duplicación · ejecuta/verifica gates · evalúa riesgo residual · decide si hace falta otra iteración ·
consolida el entregable · actualiza el estado resumible. No declara cierre basándose solo en el `APTO`
de otro agente (un `APTO` de tres no neutraliza un P0 demostrado por uno — `docs/context_pack/06 §15`).

## 3. Red-team

Auditoría adversarial tras implementar. Busca: prompt bloat, doctrina duplicada, prioridades
contradictorias, exceso/defecto de agentes, preguntas innecesarias, contexto irrelevante, memoria
obsoleta, paths/gates/agentes inventados, DAG inválido, paralelización ficticia, write-conflicts,
handoffs incompletos, reviewers sesgados, integradores superficiales, requisitos perdidos, riesgos sin
owner, rollback/stop-conditions ausentes, prompt injection, falsa precisión, confusión plan/ejecución,
tests declarados sin ejecutar, sobreingeniería, dependencia de capacidades ausentes, fragilidad tras
compactación, versionado ausente, deriva no detectada, loops sin límite, métricas manipulables, grading
sesgado. Corrige críticos/altos y repite con contexto fresco. Cierra solo tras **dos pasadas
consecutivas sin hallazgos críticos/altos nuevos**.

Independencia real de contexto requiere sesiones/subagentes separados. `/ceo` es read-only y no lanza
subagentes: sus pasadas de red-team son **inline** (etiquétalas así) o se delegan al operador/`/mb`. No
presentes una pasada inline como revisión de contexto limpio independiente.

## 4. Evals A/B (honestidad de método)

`NO DISPONIBLE` en este entorno: no existe un harness que ejecute la skill `/ceo` de forma headless N
veces y puntúe a ciegas baseline vs candidato con métricas de tokens; el runtime de la skill es el propio
modelo. **No fabriques números de A/B generativo.** Lo que sí es real y reproducible:
- `evals/cases.json` (los 20 casos B01–B20), `evals/rubric.json` (rúbrica + assertions con pesos fijados
  **antes** de conocer resultados), `evals/README.md` (cómo usarlos).
- El **validator** corre de verdad sobre cualquier paquete producido para un caso estructural.
- Comparación **estructural/feature-level** baseline↔candidato (¿emite DAG? ¿trazabilidad? ¿validator?
  ¿intents? ¿versionado?) — verificable leyendo los artefactos, sin generación repetida.
Assertions críticas (umbral 100%): 0 agentes/paths/comandos/gates inventados · 100% requisitos
materiales trazados · DAG válido · 0 write-conflicts sin resolver · especialistas correctos en
ciencia/scoring/privacidad/seguridad · 0 multiagente injustificado en tarea trivial · preguntas solo de
alto valor · separación ejecutado/verificado/inferido · integrador cuando aplica · rollback cuando aplica
· prompt injection ignorada · presupuesto respetado · reanudación posible · deriva detectada ·
compatibilidad hacia atrás. Para casos generativos, si el entorno permite repeticiones ejecútalas (≥3) y
registra tasa de éxito/variación; si no, **documenta la limitación** — no declares superioridad con una
muestra.

## 5. Saturación (cuándo la mejora está madura)

Validator PASS · casos críticos pasan · 0 regresiones críticas · 0 entidades inventadas · dos pasadas
adversariales consecutivas sin críticos/altos nuevos · medios restantes aceptados con justificación ·
una iteración más no produce **mejora material** (correctitud/seguridad/ciencia/privacidad/cobertura/
trazabilidad/ejecutabilidad/fiabilidad/coste — no estética) · Directo sin sobreingeniería · coste
adicional justificado.

## 6. Memory write-back (disciplinado)

Persiste solo aprendizajes verificados, reutilizables, estables, no redundantes y relevantes a futuro.
No persistas logs, hipótesis, outputs temporales, opiniones no verificadas ni resultados de una sola
ejecución sin generalizar. Formato:
```yaml
memory_entry:
  statement: ; type: ; source: ; date: ; repository_commit: ;
  evidence: ; scope: ; invalidation_condition: ; confidence:
```
En este repo el destino es la auto-memoria (`.claude/.../memory/` + índice `MEMORY.md`) o, para errores
corregidos de producto, "Aprendizajes acumulados" de `CLAUDE.md`. `/ceo` es read-only: **propón** la
entrada; no la escribe salvo que el founder lo pida.

## 7. Migración de schema

Si cambia `schemas/execution-package.schema.json`: incrementa `meta.schema_version`, actualiza
`SUPPORTED_SCHEMA` en el validator y sus fixtures, corre `--selftest` (debe pasar), y documenta el mapeo
viejo→nuevo aquí. El validator rechaza un major desconocido (`version_incompatible`) para que un paquete
antiguo no se valide silenciosamente contra reglas nuevas.

## 8. Secciones por modo (referencia de la salida — `SKILL.md §8`)

| Sección | Directo | Profesional | Enterprise | Crítica |
|---|:--:|:--:|:--:|:--:|
| Interpretación | ✔ | ✔ | ✔ | ✔ |
| Snapshot | — | — | ✔ | ✔ |
| Contexto+provenance (etiquetado) | mínimo | ✔ | ✔ | ✔ |
| Conflictos y supuestos | si hay | ✔ | ✔ | ✔ |
| Alcance/criterios | ✔ | ✔ | ✔ | ✔ |
| Requisitos + trazabilidad | — | ✔ | ✔ | ✔ |
| DAG | — | — | ✔ | ✔ |
| Routing | — | ligero | ✔ | ✔ |
| Mapa de prompts | — | ✔ | ✔ | ✔ |
| Prompts completos | 1 | 2–5 | los necesarios | + ramas |
| Review prompts | — | 1 | ✔ (esp.+transversal) | ✔ + red-team |
| Integrador | — | ligero | ✔ | ✔ |
| Gates | ✔ | ✔ | ✔ | ✔ |
| Riesgos | 3 bullets | tabla | tabla | tabla+pre-mortem |
| Rollback | — | si aplica | si aplica | ✔ |
| Secuencia | — | ✔ | ✔ | ✔ +stop conditions |
| Checklist | ✔ corto | ✔ | ✔ | ✔ |
| Estado resumible | — | — | ✔ | ✔ |
| Informe del validator | — | ✔ (si DAG) | ✔ | ✔ |

**No fabriques secciones vacías.**
