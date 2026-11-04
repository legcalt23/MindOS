# Plantilla · Memoria y checkpoint

## Snapshot

Run ID, fecha, objetivo, alcance, canon identificado, repo/commit/cambios locales, vault/índice/Graphify reales, versiones y permisos. Si falta acceso, `MEMORY_ACCESS_MISSING` y fuentes locales usadas.

## Ledger de cambios

| Objeto/ID | Fuente anterior/hash | Evidencia nueva/hash | Tipo factual/propuesta | Autorización | Cambio aplicado | Verificación | Reversión |
|---|---|---|---|---|---|---|---|

Separar planificado, aplicado y verificado. En fallo parcial, describir exactamente qué fuente/índice quedó pendiente. No afirmar «actualizado» a partir de una propuesta de delta.

## Registro semántico propuesto para un diseño

ID estable; tipo de objeto; título; versión; estado de decisión/diseño/ciencia; fuentes; decisiones de las que depende; requisitos del motor; relaciones a familias y constructos; claims; riesgos; revisión; fechas efectiva/de observación; objetos superseded y successor si corresponde.

Adaptar a la estructura existente. Crear nuevos tipos o reorganizar el vault requiere propuesta; no es una actualización factual automática.

## Checkpoint

- Entregables completos/parciales y rutas exactas.
- Módulos y fuentes leídos, hashes, autores de lecturas delegadas.
- Decisiones aprobadas, rechazadas, pendientes y preguntas ya respondidas.
- Alternativas descartadas con razón; no reabrir sin evidencia nueva.
- Hallazgos y estado, pruebas realmente ejecutadas y no ejecutadas.
- Próximo paso y dependencias; qué puede continuar sin respuesta del CEO.
- Cambios externos que obligarían a revisar: versiones, auditoría, evidencia, población, permisos.

## Comprobación al reanudar

Comparar hashes/versiones y decisiones relevantes. Releer lo cambiado y sus dependencias. Conservar trabajo válido; nunca inventar continuidad de una ejecución no disponible. Declarar qué estado se reconstruyó de evidencia y cuál no se pudo recuperar.
