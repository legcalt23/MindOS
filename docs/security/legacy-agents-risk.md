# Riesgo de los agentes legacy

## Inventario

Antes de este paquete existian 51 manifiestos en `.codex/agents/`. Parsean como TOML, pero no fueron diseñados bajo el RBAC, receipts, writer lock y segregacion definidos aqui. El inventario no implica aprobacion de seguridad.

## Decision

Los agentes legacy quedan denegados por defecto para workflows de ciberseguridad. El Orchestrator solo puede seleccionar uno si una persona autorizada crea una allowlist por tarea que documente:

- nombre exacto del manifiesto;
- necesidad que no cubre un agente nuevo;
- nivel de autonomia maximo;
- herramientas y rutas permitidas;
- prohibiciones de red, secretos y escritura;
- aprobador, fecha y expiracion;
- verificacion independiente requerida.

## Riesgos principales

- Instrucciones heredadas sin tratamiento uniforme de prompt injection.
- Permisos implicitos o herramientas no alineadas con el RBAC actual.
- Escrituras concurrentes o falta de handoff independiente.
- Delegacion recursiva y crecimiento no acotado del contexto.
- Mezcla de tareas de producto con el mandato defensivo.

## Migracion

La migracion debe ser agente por agente: revisar prompt, asignar RBAC, retirar escritura, incorporar receipt, probar fail-closed y obtener aprobacion humana. Hasta entonces no se cuentan como miembros activos del equipo de seguridad.

No se borran ni reescriben automaticamente porque pueden pertenecer a workflows existentes del usuario. La contencion se implementa en el Orchestrator y en la politica publica del repositorio.
