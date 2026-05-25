# Auditoría final 03 — Release gate

## Objetivo

Decidir si está listo para beta.

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

Ejecuta checklist final de lanzamiento: tests, migrations, config, secrets, rate limits, sandbox flag, admin RBAC, audit logs, reconciliation, backups docs. Crea `docs/security/release-gate.md` con GO/NO-GO.

## Alcance permitido

Documentación y verificación.

## Fuera de alcance

Cambios funcionales grandes.

## Requisitos de seguridad

Si algo crítico falla, marcar NO-GO.

## Tests mínimos

Ejecutar todos los comandos disponibles.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
