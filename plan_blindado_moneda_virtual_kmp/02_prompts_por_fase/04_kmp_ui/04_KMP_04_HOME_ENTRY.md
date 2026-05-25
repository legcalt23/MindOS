# KMP 04 — Entrada desde Home

## Objetivo

Añadir acceso a Moneda.

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

Añade un apartado/botón/tarjeta en Home llamado `Moneda` que navegue a la nueva pantalla.

## Alcance permitido

Navegación y UI.

## Fuera de alcance

Cambios backend.

## Requisitos de seguridad

No mostrar saldo como autoritativo si está cacheado; refrescar al entrar.

## Tests mínimos

Tests navegación si existen.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
