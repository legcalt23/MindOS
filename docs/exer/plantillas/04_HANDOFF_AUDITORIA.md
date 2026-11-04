# Plantilla · Entrada de la auditoría del motor

## Identidad y naturaleza

- Documento, fecha efectiva, autor/roles, URL/ruta y hash.
- `input_kind`: AUDIT_RESULTS / AUDIT_PROMPT / PARTIAL_NOTES.
- Repo/commit/branch/cambios locales, plataformas, alcance, pruebas, datos y fuentes.
- Qué se observó directamente, qué se reportó y qué no se verificó.

Si es `AUDIT_PROMPT`, no completar resultados como si existieran. Registrar requisitos esperados y pedir informe/handoff cuando sea necesario.

## Extracción estructurada

| Elemento | Estado actual observado/reportado | Recomendación de auditoría | Decisión CEO | Evidencia exacta | Impacto sobre ejercicios |
|---|---|---|---|---|---|

Cubrir constructos, catálogo, cobertura, familia, contenido, dificultad, adaptación, dosis, métricas, timing, exposición, elegibilidad, perfil, datos, accesibilidad, móvil, transferencia y validación.

## Capacidades y bloqueos

| Capability ID/versión | Contrato | Exists | Connected | Executed/tested | Interpretación validada | Fuente | Limitación |
|---|---|---|---|---|---|---|---|

No convertir las columnas en una cadena de implicaciones automáticas. Marcar UNKNOWN donde no haya evidencia.

## Diferencias con memoria

| Afirmación del grafo | Fuente/fecha | Evidencia nueva | Conflicto real o aparente | Actualización factual autorizada | Cambio propuesto | Decisión |
|---|---|---|---|---|---|---|

## Impacto en diseños

| Dossier/versión | Dependencia afectada | Consecuencia | Mantener/revisar/condicionar | Gate | Acción mínima |
|---|---|---|---|---|---|

## Handoff suficiente

Declarar qué puede diseñarse con confianza conceptual, qué solo como training-only, qué requiere decisión del CEO, qué precisa inspección de repo y qué requiere evidencia humana. Entregar unknowns con pregunta/evidencia necesaria y la siguiente acción acotada.
