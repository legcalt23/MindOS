# Infra 03 — CI de seguridad

## Objetivo

Automatizar verificación.

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

Añade pipeline CI para build, tests, migrations check, dependency scanning, secret scanning y lint si el stack lo permite.

## Alcance permitido

CI config.

## Fuera de alcance

Deploy producción.

## Requisitos de seguridad

Fail build ante secretos o tests rotos.

## Tests mínimos

Validar sintaxis pipeline si posible.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
