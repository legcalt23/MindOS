# MindBoost — Hardening Backlog

Fecha: 2026-06-20

## P0 — bloquea lanzamiento

- [ ] Ejecutar gates completos y archivar logs para el commit exacto de release: tests KMP, Detekt, compilación Android, dependency/secret checks y release evidence.
- [ ] Confirmar que el artefacto publicable es Android-only; excluir iOS/Desktop del release hasta cerrar MB-R-004.
- [ ] Ejecutar canary Crashlytics en proyecto no productivo: mensaje y causa sintéticos no deben aparecer; revisar ACL, retención y exportaciones.
- [ ] Revisar y limpiar `validation/` con una política aprobada; conservar solo evidencia sintética necesaria y cifrada.

## P1 — antes de automatización o integración MindVerse

- [ ] Reducir herramientas de cada agente al mínimo: lectura por defecto; Bash/escritura solo para roles de implementación.
- [ ] Añadir check CI que rechace permisos locales `Bash(npm run *)`, intérpretes wildcard y agentes con Bash sin la boundary obligatoria.
- [ ] Añadir un test Android del adapter Crashlytics o extraerlo a una unidad testeable para confirmar la excepción final sin `cause`.
- [ ] Añadir inventario/TTL automatizado para exports debug y evidencias QA.
- [ ] Definir clasificación de datos MindBoost: identidad, cognición, diagnóstico técnico, analytics y contenido de IA.
- [ ] Documentar ROPA/DPIA y contratos con terceros antes de enviar datos cognitivos a analytics o modelos.

## P2 — hardening continuo

- [ ] Secret scanning pre-commit/CI y revisión periódica del historial.
- [ ] SBOM por release, verificación de lockfiles y revisión de acciones/plugins fijados por digest.
- [ ] Tests emulator para toda nueva colección/callable MindBoost: auth, ownership, IDOR, payload limits, App Check y rate limiting.
- [ ] Fuzz/property tests de parsers, serialización y límites de scoring.
- [ ] Runbook de incidente para exposición de datos cognitivos y telemetría.
- [ ] Revisión trimestral de IAM, Crashlytics, Storage, Functions y evidencia de borrado.
- [ ] Red-team local de prompts/agentes con corpus de inyección indirecta y canarios de secretos.

## Criterio de cierre

Una tarea solo se cierra con cambio revisable, test reproducible, comando y resultado, evidencia archivada sin PII real y owner. Los gates manuales no se consideran cerrados por inspección de código.

