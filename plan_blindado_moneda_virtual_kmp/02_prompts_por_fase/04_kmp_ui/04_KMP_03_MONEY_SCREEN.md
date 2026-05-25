# KMP 03 — Pantalla Moneda

## Objetivo

Crear la pantalla visible al usuario.

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

Añade pantalla nueva llamada `Moneda` con saldo actual, breakdown si existe y botones `Comprar 1 punto`, `Comprar 10 puntos`, `Comprar 100 puntos` usando sandbox purchase.

## Alcance permitido

UI Compose/KMP adaptada al stack.

## Fuera de alcance

Backend.

## Requisitos de seguridad

Mostrar claramente si la compra está en modo sandbox/no real si corresponde a entorno dev/beta.

## Tests mínimos

Tests UI/snapshot si existen.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
