# Backend 01 — Error model y logging estructurado

## Objetivo

Crear base segura de errores y trazabilidad.

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

Implementa modelo unificado de errores API, request/correlation ID, logging estructurado y sanitización de logs.

## Alcance permitido

Middleware/filtros, clases de error, tests de error response.

## Fuera de alcance

Wallet, ledger, pagos o UI.

## Requisitos de seguridad

No loguear tokens, secrets, purchase tokens completos ni PII innecesaria.

## Tests mínimos

Tests de error contract, request ID y sanitización.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
