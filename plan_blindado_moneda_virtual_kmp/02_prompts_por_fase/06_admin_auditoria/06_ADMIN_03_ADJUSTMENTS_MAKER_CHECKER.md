# Admin 03 — Ajustes con maker/checker

## Objetivo

Permitir ajustes sin abrir puerta crítica.

## Contexto obligatorio

Estás implementando un sistema de moneda virtual blindado para una app Kotlin Multiplatform con backend autoritativo.

Reglas absolutas:

- El cliente nunca es fuente de verdad.
- No aceptes saldo final desde cliente.
- No implementes pagos reales todavía.
- Usa TPV sandbox/simulado.
- Toda operación económica debe ser idempotente.
- Toda operación económica debe ser auditada.
- Ledger append-only.
- Tests obligatorios.

## Tarea única

Implementa flujo admin adjustment: request pending, approve by different admin, execute creando ledger transaction. Para importes bajos puede ejecutarse según config, pero siempre auditado.

## Alcance permitido

Endpoints admin, servicio, tests.

## Fuera de alcance

UI admin compleja.

## Requisitos de seguridad

Nunca editar snapshot directamente sin ledger. Justificación obligatoria. Admin solicitante no puede aprobar.

## Tests mínimos

Tests maker/checker, ajuste ejecutado, intento autoaprobar rechazado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
