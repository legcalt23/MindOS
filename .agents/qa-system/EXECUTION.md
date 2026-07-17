# Máquina de ejecución `/qa`

## Índice

1. Principios de ejecución
2. Estado de una ejecución
3. Fases obligatorias
4. Ciclo de reparación
5. Finalización

## 1. Principios de ejecución

- Ejecutar una fase solo cuando sus prerrequisitos estén satisfechos.
- Registrar inicio, fin y resultado de cada fase en UTC.
- No marcar `SKIPPED` por conveniencia. Solo usar `NOT_APPLICABLE` con una justificación ligada al alcance; una capacidad aplicable pero inaccesible es `FAILED`.
- Separar observación, inferencia y conclusión. Una inferencia debe estar etiquetada y confirmarse antes del veredicto.
- Mantener un único escritor. Los investigadores paralelos no editan archivos ni comparten un directorio de salida mutable.
- No usar el contexto de una prueba anterior como sustituto de una ejecución actual.

## 2. Estado de una ejecución

Crear `.qa/runs/<YYYYMMDDTHHMMSSZ>_<sha8>/` sin reutilizar nombres. Mantener durante la ejecución:

```text
run_id
started_at_utc
coordinator_model
repository_root
branch
head_before
baseline_commit
handoff_source
dirty_state_before
affected_scope
risk_class
phase_status
fix_iterations
head_after
dirty_state_after
verdict
```

Crear `.qa/current-run` únicamente como puntero al `run_id`; eliminarlo al cerrar. Si ya existe, comprobar si corresponde a un proceso activo. No borrar un lock activo. Un lock huérfano debe preservarse como evidencia y provocar `FAIL — CONCURRENT_OR_STALE_RUN` hasta revisión humana.

## 3. Fases obligatorias

### S0 — Preflight e integridad

**Entrada:** workspace abierto.  
**Acciones:** verificar Gemini/Antigravity, raíz, contratos QA, Git, permisos no destructivos, herramientas y estado inicial.  
**Salida:** `environment.md`, `repository-before.txt`, `phase-S0.md`.  
**Fallo:** no continuar si no puede preservarse el estado del usuario.

### S1 — Handoff y baseline

Aplicar `HANDOFF_AND_IMPACT.md`. Identificar exactamente qué entregó Claude y contra qué commit se compara. Un baseline ambiguo es fallo, no permiso para probar todo de forma superficial.

**Salida:** `handoff.md`, `changed-files.txt`, `baseline.diff`.

### S2 — Impacto, riesgo y trazabilidad

Construir una tabla con una fila por requisito o riesgo:

```text
ID | requisito | archivos/símbolos | riesgo | prueba | entorno | evidencia esperada | estado
```

Incluir impactos directos y transitivos: llamadas, modelos, serialización, almacenamiento, navegación, targets KMP, UI, analytics y tests. Clasificar `CRITICAL`, `HIGH`, `MEDIUM` o `LOW` conforme a `HANDOFF_AND_IMPACT.md`.

**Salida:** `traceability.md`, `impact-map.md`.

### S3 — Descubrimiento y línea base

Aplicar `BUILD_AND_TEST.md` para descubrir tareas reales. Ejecutar compilación y pruebas mínimas antes de editar. Diferenciar:

- Fallo preexistente fuera del alcance.
- Regresión provocada por el handoff.
- Bloqueo de entorno.

No corregir un fallo no relacionado. Documentarlo y continuar solo si no invalida las pruebas afectadas; de lo contrario, `FAIL`.

**Salida:** `baseline/commands.log`, `baseline/test-results/`, `phase-S3.md`.

### S4 — Verificación automatizada focal

Ejecutar tests unitarios, integración, persistencia y contrato correspondientes al mapa. Añadir tests faltantes solo cuando expresen requisitos ya aprobados. Primero crear una prueba que falle por el defecto; luego corregir.

**Salida:** resultados brutos, cobertura disponible, lista de casos y exit codes.

### S5 — Scoring y perfil cognitivo

Aplicar íntegramente `SCORING.md` si el cambio toca o puede alcanzar: ejercicios, respuestas, tiempos, dificultad, sesión, serialización, perfil, historial, plan, UI de resultados, calibración, rangos, analytics o sincronización. Ante duda, es aplicable.

### S6 — Persistencia, conectividad y recuperación

Aplicar `OFFLINE_SYNC.md`. El smoke offline esencial se ejecuta en toda funcionalidad que pueda coincidir con una sesión activa. La matriz completa se ejecuta para cambios en datos, red, repositorios, lifecycle o perfil.

### S7 — Dispositivo y recorrido visual

Aplicar `DEVICE_VISUAL.md`. Instalar el build exacto probado, limpiar únicamente datos de la app de prueba cuando el escenario lo requiera y recorrer el flujo afectado. Capturar evidencia antes y después de cualquier reparación visual.

### S8 — Observabilidad y rendimiento

Aplicar `OBSERVABILITY_PERFORMANCE.md` sobre el mismo build y los mismos recorridos. Correlacionar timestamps de UI, Logcat, Crashlytics y medición. Un log sin correlación temporal no demuestra el origen.

### S9 — Reparación controlada

Aplicar `FIX_POLICY.md`. Ordenar defectos por severidad y dependencia. Reparar un grupo causal pequeño cada vez. No mezclar refactors cosméticos.

### S10 — Regresión total afectada

Después de la última edición:

1. Recalcular el diff completo.
2. Repetir compilación desde artefactos limpios cuando sea viable.
3. Repetir suites afectadas.
4. Repetir scoring aplicable.
5. Repetir cold start y recorrido visual.
6. Repetir offline/reconexión.
7. Volver a revisar logs y rendimiento.
8. Confirmar que no aparecieron archivos o cambios fuera de alcance.

Los resultados anteriores a la última edición no pueden cerrar puertas finales salvo pruebas totalmente independientes del código modificado.

### S11 — Puertas e informe

Aplicar `GATES_AND_REPORT.md`. Validar que cada afirmación del informe enlaza a evidencia existente y que los hashes corresponden al build final. Emitir el veredicto binario.

## 4. Ciclo de reparación

Por cada defecto:

```text
OBSERVAR → REPRODUCIR → AISLAR CAUSA → CLASIFICAR AUTORIDAD
→ AÑADIR/IDENTIFICAR PRUEBA → PARCHE MÍNIMO → PRUEBA FOCAL
→ REGRESIÓN AFECTADA → ACTUALIZAR EVIDENCIA
```

Reglas:

- Máximo cinco intentos distintos sobre la misma causa sin nueva evidencia. El sexto intento requiere detenerse con `FAIL — REPAIR_LOOP_EXHAUSTED`.
- Revertir únicamente el parche propio que empeore el resultado, preservando cambios previos del usuario. No usar comandos destructivos.
- Si dos reparaciones fallan por causas diferentes, no acumular parches especulativos.
- Si el defecto requiere decisión de arquitectura/seguridad/producto, detener esa rama de trabajo y documentar el handoff necesario.

## 5. Finalización

Antes de cerrar:

- Guardar `final.diff` y hashes de archivos modificados.
- Comparar estado inicial/final y atribuir cada cambio.
- Cerrar/eliminar el puntero `.qa/current-run` solo después de persistir el informe.
- No alterar informes de ejecuciones anteriores.
- No realizar commit o push.
