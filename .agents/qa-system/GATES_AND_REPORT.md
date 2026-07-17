# Puertas binarias, evidencia e informe final

## Índice

1. Directorio de ejecución
2. Severidad
3. Puertas
4. Regla de veredicto
5. Índice de evidencia
6. Plantilla de QA_REPORT
7. Estado para la siguiente ejecución

## 1. Directorio de ejecución

Crear sin sobrescribir:

```text
.qa/runs/<run_id>/
├── QA_REPORT.md
├── evidence-index.md
├── environment.md
├── repository-before.txt
├── repository-after.txt
├── handoff.md
├── impact-map.md
├── traceability.md
├── commands.log
├── baseline.diff
├── final.diff
├── build/
├── tests/
├── scoring/
├── sync/
├── device/
├── logs/
└── performance/
```

Usar artefactos nativos cuando existan. No convertir evidencia bruta en solo resúmenes. Redactar secretos/PII conservando utilidad forense.

## 2. Severidad

- `BLOCKER`: impide compilar, instalar o completar el flujo.
- `CRITICAL`: pérdida/corrupción/duplicado de datos, score/rango falso, crash/ANR, acceso cruzado, entitlement incorrecto, privacidad o seguridad.
- `MAJOR`: comportamiento funcional incorrecto, offline roto, UI esencial inutilizable, regresión fuerte de performance/accesibilidad.
- `MINOR`: defecto acotado que no altera resultado, persistencia ni flujo esencial.
- `INFO`: observación sin incumplimiento.

Todo hallazgo debe incluir ID estable `MBQA-####`, severidad, requisito, reproducción, esperado/observado, causa, estado, parche y evidencia.

## 3. Puertas

Evaluar todas. Estados permitidos: `PASS`, `FAIL`, `NOT_APPLICABLE`. `NOT_APPLICABLE` exige justificación y evidencia de alcance.

| ID | Puerta | Condición de paso |
|---|---|---|
| G00 | Integridad del sistema QA | Gemini/Antigravity verificado; contratos presentes; ejecución aislada. |
| G01 | Handoff y baseline | Feature, baseline, diff y working tree inequívocos. |
| G02 | Fuente de verdad | Requisitos/arquitectura/scoring sin conflictos materiales. |
| G03 | Trazabilidad | Cada riesgo aplicable tiene prueba y cada prueba tiene evidencia. |
| G04 | Compilación | Todos los módulos/targets/variantes aplicables compilan. |
| G05 | Unitarias | Suites afectadas pasan con bordes y repeticiones según riesgo. |
| G06 | Integración/contratos | Persistencia, serialización, backend/sandbox y consumidores pasan. |
| G07 | Scoring | Fórmulas, guards, propiedades, calibración, rangos, idempotencia y versiones pasan. |
| G08 | Offline/resync | Pérdida de red, process death y sync exactly-once pasan. |
| G09 | Dispositivo/E2E | Build final instalado; flujos afectados y consecuencias persistidas pasan. |
| G10 | Visual/accesibilidad | Contrato MindBoost, tamaños/estados/semantics aplicables pasan. |
| G11 | Logcat/Crashlytics | Sin crash, ANR, errores relevantes, tormentas o datos sensibles. |
| G12 | Rendimiento | Presupuestos oficiales/baseline/provisionales aplicables pasan. |
| G13 | Regresión final | Pasada posterior al último parche completa y limpia. |
| G14 | Disciplina del diff | Solo cambios autorizados, mínimos y atribuibles. |
| G15 | Integridad de evidencia | Artefactos existen, correlacionan con build final y son reproducibles. |

Aplicabilidad mínima:

- G00–G05, G09, G11, G13–G15: siempre.
- G06: siempre que exista código ejecutable con estado/datos/dependencias; casi toda feature MindBoost.
- G07: aplicar ante cualquier ruta que alcance sesión/perfil/resultados/scoring; ante duda, aplicar.
- G08: aplicar siempre como smoke para entrenamiento; matriz completa para cambios de datos/red/lifecycle.
- G10: aplicar si existe cualquier UI o cambio que pueda alterar layout/estado.
- G12: aplicar a HIGH/CRITICAL y a cambios UI, lifecycle, datos, build o rendimiento.

## 4. Regla de veredicto

Emitir `PASS` solo si:

1. Todas las puertas aplicables son `PASS`.
2. No queda ningún BLOCKER, CRITICAL o MAJOR abierto.
3. Ningún MINOR contradice un criterio explícito de aceptación.
4. No existe decisión pendiente que afecte corrección.
5. El último parche fue incluido en la regresión final.
6. El informe y evidencia corresponden al mismo commit/diff/binario.

En cualquier otro caso emitir `FAIL`. No existe `PARTIAL PASS`, `PASS WITH CAVEATS` ni “suficientemente bueno”. Los bloqueos se expresan como causas de `FAIL`.

## 5. Índice de evidencia

`evidence-index.md` debe tener una fila por artefacto:

```text
Evidence ID | Gate | Requirement/Test | Timestamp UTC | Environment | Command/Action | Exit/Observed | Path | SHA-256 | Redacted
```

Asignar IDs `EV-0001...`. Los informes enlazan IDs, no descripciones vagas. Verificar existencia y hash antes de cerrar.

`commands.log` debe ser append-only dentro de la ejecución y contener:

```text
timestamp_utc | cwd | sanitized_command | exit_code | duration_ms | output_artifact
```

## 6. Plantilla de `QA_REPORT.md`

```markdown
# MindBoost QA Report — <run_id>

## VERDICT: PASS|FAIL

### Identidad
- Run ID:
- Inicio/fin UTC:
- Coordinador/modelo:
- Rama:
- Baseline commit:
- Commit probado:
- Working tree/diff hash:
- Artefactos/binarios y SHA-256:

### Handoff probado
- Feature:
- Fuente del handoff:
- Requisitos PRD/arquitectura:
- Alcance directo/transitivo:
- Riesgo máximo:

### Resumen ejecutivo
- Qué se verificó:
- Qué se corrigió:
- Qué permanece:
- Puertas: <pass>/<aplicables>.

### Puertas
| Gate | Applicable | Result | Evidence | Notes |
|---|---|---|---|---|

### Matriz de entornos
| Plataforma/dispositivo | OS/API | Variante/hash | Escenarios | Resultado | Evidencia |
|---|---|---|---|---|---|

### Pruebas
| Test suite/case | Repeticiones | Resultado | Duración | Evidencia |
|---|---:|---|---:|---|

### Scoring
| Invariante/vector | Resultado | Evidencia |
|---|---|---|

### Offline y sincronización
| Escenario | Local | Cloud | Duplicados | Resultado | Evidencia |
|---|---|---|---:|---|---|

### Visual, accesibilidad y performance
| Comprobación | Baseline/presupuesto | Observado | Resultado | Evidencia |
|---|---|---|---|---|

### Hallazgos y reparaciones
| ID | Severidad | Estado | Causa raíz | Archivos | Prueba regresión | Evidencia |
|---|---|---|---|---|---|---|

### Cambios realizados por QA
- Diff resumido:
- Confirmación de límites de autoridad:
- Archivos no relacionados preservados:

### Bloqueos/decisiones requeridas
- Ninguno, o decisión exacta + owner recomendado + evidencia.

### Reproducción
- Prerrequisitos:
- Comandos:
- Orden de journeys:
- Ubicación de evidencia bruta:

### Declaración de integridad
No se declara ejecutada ninguna prueba sin artefacto verificable. El veredicto corresponde al build y diff identificados arriba.
```

No omitir secciones; usar “No aplicable — razón” cuando proceda.

## 7. Estado para la siguiente ejecución

Solo después de `PASS`, actualizar atómicamente `.qa/state/last-pass.json`:

```json
{
  "run_id": "...",
  "tested_commit": "full-sha",
  "baseline_commit": "full-sha",
  "branch": "...",
  "final_diff_sha256": "...",
  "completed_at_utc": "...",
  "report": ".qa/runs/<run_id>/QA_REPORT.md"
}
```

En `FAIL`, no avanzar `tested_commit`. Crear `.qa/state/last-fail.json` con la misma identidad, causas y acción mínima de desbloqueo. Nunca usar un `FAIL` como baseline aprobado.
