# KMP 02 — Estado de pantalla Moneda

## Objetivo

Gestionar estado UI seguro.

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

Implementa ViewModel/state holder para cargar saldo, iniciar compra sandbox 1/10/100 y refrescar balance tras verify.

## Alcance permitido

ViewModel/state, tests.

## Fuera de alcance

Cambiar backend.

## Requisitos de seguridad

No actualizar saldo local como definitivo hasta respuesta backend. Manejar desincronización.

## Tests mínimos

Tests de success/error/loading/retry.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
