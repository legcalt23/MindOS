# Instrucciones generales para exprimir Cloud Code al máximo

## Regla principal

Ejecuta una sola tarea por prompt. Cada prompt de este paquete está diseñado para producir un cambio revisable, testeable y revertible.

## Cómo usar los prompts

1. Carga el repositorio completo.
2. Antes de implementar, pide al agente que inspeccione el repo.
3. Ejecuta `00_PREPARACION_01_AUDITAR_REPO.md`.
4. Sigue el orden numérico.
5. Después de cada prompt:
   - Ejecuta tests.
   - Revisa diff.
   - Verifica que no haya secretos.
   - Haz commit pequeño.
6. No avances de fase si la anterior no está verde.

## Modo de trabajo recomendado

Usa este patrón para cada prompt:

```text
Lee este prompt completo.
Inspecciona el repositorio.
Haz únicamente la tarea indicada.
No implementes tareas futuras.
Mantén compatibilidad con el código existente.
Añade tests.
Ejecuta tests relevantes.
Devuelve resumen de cambios, archivos tocados, comandos ejecutados y riesgos pendientes.
```

## Restricciones obligatorias para el agente

- No usar saldo local como fuente de verdad.
- No permitir que el cliente envíe saldo final.
- No implementar pagos reales todavía.
- No guardar secretos reales.
- No añadir endpoints admin inseguros.
- No saltarse idempotencia.
- No saltarse auditoría.
- No usar mocks en producción salvo `sandbox_payment_provider`.
- No ocultar errores de tests.
- No dejar TODOs críticos sin registrar.

## Convención de commits

```text
phase/<numero>-<descripcion>
```

Ejemplos:

```text
phase/01-db-ledger-schema
phase/02-wallet-spend-endpoint
phase/04-kmp-money-screen
```

## Definición de terminado por tarea

Una tarea está terminada si:

- Compila.
- Tiene tests.
- No rompe tests existentes.
- Tiene migración si toca DB.
- Tiene logs estructurados si toca backend.
- Tiene auditoría si toca operación económica.
- Tiene idempotencia si toca operación económica.
- Está documentada en el README técnico del módulo.
