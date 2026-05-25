# Wallet 07 — Historial de transacciones

## Objetivo

Permitir consulta segura de movimientos.

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

Implementa `GET /wallet/transactions` paginado para el usuario autenticado.

## Alcance permitido

Endpoint, query, DTOs, tests.

## Fuera de alcance

Admin ledger view.

## Requisitos de seguridad

No exponer payloads internos, purchase tokens ni señales antifraude.

## Tests mínimos

Tests paginación, authz, orden.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
