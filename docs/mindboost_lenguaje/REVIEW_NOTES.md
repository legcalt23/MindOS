# MindBoost · Lenguaje v6 — Notas de revisión (Fase 5)

Revisión de arquitectura, deuda y nombres del feature `language`. Honesta: solo se listan hallazgos
reales; no se inventan problemas ni refactors especulativos.

## Hallazgos y acciones

1. **`ReviewTrigger` no consumido → tipado.** El enum `ReviewTrigger` (§9.6) existía pero
   `TransferMission.humanReviewTriggers` usaba `List<String>` sin tipo. **Corregido**: el campo ahora
   es `List<ReviewTrigger>` (type-safety + DRY; elimina superficie no consumida).

## Superficie de API intencional (no es deuda)

- `EvaluatorMode.HYBRID` / `EvaluatorMode.REMOTE`: solo `LOCAL` se usa hoy (el stub offline). HYBRID/
  REMOTE son contrato para futuras implementaciones remotas del `EvaluatorClient`; no se eliminan
  porque romperían ese contrato (la implementación real es backend/diferida).
- `HumanReviewStatus.{IN_REVIEW, COMPLETED, ESCALATED}`: estados del ciclo de revisión humana que la
  cola real (§9.6, backend) producirá; el stub solo emite `NOT_REQUIRED`/`PENDING`.
- `BankType.{GOLD_SET, RED_TEAM}`: bancos que por diseño no computan progreso (§8.3); referenciados
  por la política de promoción (`EvidenceEngine`) para devolver "sin evidencia".

## Arquitectura — veredicto

- Separación de responsabilidades por paquete correcta; sin ciclos de dependencia. `transfer` →
  `evaluator` (por `ReviewTrigger`) es unidireccional y justificado (la misión enruta al evaluador).
- Sin código muerto detectable: `:shared:detekt` limpio (cubre `UnusedParameter`/`UnusedPrivateMember`/
  `UnreachableCode`).
- Nombres consistentes con `focus`/`memory`/`speed` (`*Engine`/`Default*`/`*UiModel`/`*Builder`).
- Tiempo siempre inyectado por epoch millis; ningún `Clock.System.now()` en dominio/presentación.

## Placeholders deliberados (documentados, con contrato)

| Placeholder | Por qué | Desbloqueo |
|-------------|---------|-----------|
| θ (thetaMean = 0.0) en `ProfileProjector` | IRT real necesita datos/backend | Calibración IRT/CAT |
| `LocalStubEvaluatorClient` | §9.7 degradación offline; nunca certifica E2+ | Evaluator OS real |
| `FairnessGate` DIF | aplica política dada una cifra; no calcula DIF | Datos de cohorte |
| `ContentValidator` (solo metadatos) | el cuerpo del ítem necesita `RawTask` | Content Compiler |

Ver [COVERAGE_MATRIX.md](COVERAGE_MATRIX.md) y [PRODUCTION_CHECKLIST.md](PRODUCTION_CHECKLIST.md).
