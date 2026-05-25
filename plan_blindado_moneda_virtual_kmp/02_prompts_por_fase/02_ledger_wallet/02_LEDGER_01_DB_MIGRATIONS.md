# Ledger 01 — Migraciones DB

## Objetivo

Crear el modelo persistente del sistema económico.

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

Añade migraciones para wallets, ledger_transactions, ledger_entries, balance_snapshots, idempotency_keys, purchases, payment_provider_events, fraud_signals, audit_logs, admin_actions y outbox_events.

## Alcance permitido

Solo migraciones DB y tests de migración si existen.

## Fuera de alcance

Servicios, endpoints o UI.

## Requisitos de seguridad

Incluye PK, FK, unique constraints, checks e índices. Ledger append-only. Purchase token hash único.

## Tests mínimos

Tests de migración/constraints si el stack lo permite.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
