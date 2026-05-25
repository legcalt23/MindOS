# Testing 03 — API abuse tests

## Objetivo

Probar abuso de endpoints.

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

Añade tests para replay, idempotency conflict, userId tampering, campos extra, webhook spoofing, admin unauthorized, rate limit.

## Alcance permitido

Tests seguridad.

## Fuera de alcance

Pentest externo.

## Requisitos de seguridad

No deben depender de secretos reales.

## Tests mínimos

Ejecutar suite de seguridad.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
