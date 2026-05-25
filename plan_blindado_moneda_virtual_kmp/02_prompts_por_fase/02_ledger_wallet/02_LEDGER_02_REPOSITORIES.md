# Ledger 02 — Repositorios transaccionales

## Objetivo

Crear capa de acceso a datos segura.

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

Implementa repositorios para wallets, ledger, snapshots, idempotency, purchases, provider events, audit logs y outbox.

## Alcance permitido

Repositorios y tests unitarios/integración.

## Fuera de alcance

Endpoints públicos.

## Requisitos de seguridad

Debe soportar `findForUpdate` o equivalente para wallet/snapshot. No exponer métodos que editen ledger histórico.

## Tests mínimos

Tests de insert, unique violation, lock básico si posible.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
