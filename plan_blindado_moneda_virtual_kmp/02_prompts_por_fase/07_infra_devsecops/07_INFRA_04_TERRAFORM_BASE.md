# Infra 04 — Terraform base

## Objetivo

Preparar IaC para producción.

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

Crea módulo Terraform base agnóstico o cloud elegido para API, DB PostgreSQL gestionado, Redis, queue, KMS/secrets, WAF y networking privado. Si no hay cloud elegido, crea blueprint documentado y módulos placeholder no destructivos.

## Alcance permitido

Terraform/docs.

## Fuera de alcance

Aplicar infra real.

## Requisitos de seguridad

No usar credenciales reales. Variables sensibles marcadas sensitive.

## Tests mínimos

terraform fmt/validate si posible.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
