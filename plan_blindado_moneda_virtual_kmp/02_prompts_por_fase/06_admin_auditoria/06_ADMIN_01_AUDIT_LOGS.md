# Admin/Auditoría 01 — Audit logs inmutables

## Objetivo

Asegurar trazabilidad.

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

Integra AuditLogService en spend, credit, purchase verify, refund skeleton, freeze y admin actions.

## Alcance permitido

Servicio/integración/tests.

## Fuera de alcance

Panel visual completo.

## Requisitos de seguridad

No permitir modificación de audit logs desde la app. No loguear secretos.

## Tests mínimos

Tests de audit log por operación.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
