# Plantilla · Revisión y comprobaciones

## Identidad de revisión

- Revisión R1/R2/R3 y objetivo; artefacto/versión/hash; brief; fuentes.
- Autor/revisor reales; `independent_agent` / `same_agent_sequential` / `not_performed`.
- Alcance inspeccionado y no inspeccionado; comandos/entorno si se ejecutaron.

## Matriz de criterios

| Criterio | Evidencia inspeccionada | Resultado | Límite | Hallazgo/acción |
|---|---|---|---|---|

Cubrir mecanismo, práctica, progresión/dosis, personalización, medición, transferencia, móvil, accesibilidad, datos, motor, canon y completitud.

## Casos de falsación

| Caso | Input/supuesto | Interpretación esperada | Método | Resultado real | Gate | Estado |
|---|---|---|---|---|---|---|

Métodos: análisis de mesa, simulación sintética, prueba existente ejecutada, revisión humana futura. No confundir esperado y observado. Si no se ha implementado el ejercicio, las pruebas técnicas pertenecen al plan futuro.

## Hallazgos

| ID | Afirmación/contrato | Evidencia/contraejemplo | Consecuencia | Severidad | Uso afectado | Propuesta | Decisión necesaria | Disposición |
|---|---|---|---|---|---|---|---|---|

## Cierre de hallazgos

Para cada corrección: diferencia exacta, verificación pertinente, resultado y riesgo restante. No marcar `FIXED` por recomendar una solución. `ACCEPTED_RISK` requiere quién lo aceptó y barrera que conserva el uso permitido; no es un defecto arreglado.

## Veredicto

- Completitud documental, disposición del diseño y preparación por uso.
- P0/P1 restantes y por qué afectan o no al alcance aceptable.
- Decisiones pendientes del CEO; evidencia faltante; revisiones no realizadas.
- Condición de reentrada y siguiente gate.
