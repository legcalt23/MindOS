# MindBoost · Lenguaje v6 — Checklist de producción

Qué está listo y qué falta antes de lanzar el módulo como "entrenador de lenguaje".
Basado en los Launch Gates de v6 (§17) y la realidad de la implementación actual (Fases 1–5).

## Listo (núcleo offline)

- [x] Dominio completo, serializable y testeable (`@Serializable`, round-trip probado).
- [x] Diagnóstico → evidencia → claims gobernados: cadena mínima v6 verificada end-to-end (`LanguageCycleEndToEndTest`).
- [x] Scoring cerrado con gates §5.6 (fatiga/repetido/confusor/baja confianza → no actualiza θ fuerte).
- [x] Reparación MVP (14 patrones), detección por frecuencia **cruda** (no deduplicada).
- [x] Promoción de evidencia event-sourced: paquetes de peso 0 no promocionan nunca.
- [x] ClaimEngine: bloquea claims sin evidencia; copy prohibido (§11.6) imposible de emitir.
- [x] Privacidad: consentimiento granular, `deleteUserData` por usuario, persistencia cifrada (`SecureKeyValueStore`), evaluador remoto consent-gated, texto libre nunca en el contrato de dominio (`responseRef` opaco).
- [x] Calidad: ~145 tests verdes, `:shared:detekt` limpio (ratchet respetado).

## Bloqueantes para producción (necesitan backend / datos / validación)

- [ ] **Evaluador real** (§9): reemplazar `LocalStubEvaluatorClient` por el Evaluator OS (jueces, rúbricas ancladas, caps, bias sentinel real, eval harness). El stub NUNCA certifica evidencia E2+.
- [ ] **Calibración psicométrica** (§5.3): θ es placeholder (0.0); recalibrar umbrales (SE, fiabilidad, DIF) con datos reales. IRT/CAT.
- [ ] **Cola de revisión humana** (§9.6) operativa con SLA y triage.
- [ ] **DIF real** (§5.3/§8.6): `FairnessGate` aplica la política, pero el cálculo de DIF necesita cohortes.
- [ ] **Validación del cuerpo del ítem** (§8.2): `ContentValidator` valida metadatos; falta el Content Compiler con `RawTask` (clave ambigua, distractores no diagnósticos).
- [ ] **Banco de contenido** con los presupuestos mínimos por módulo (§7.3) revisado por expertos y piloto.
- [ ] **Sync** (§12.6): outbox/ledger a backend (hoy local-first sin sincronización remota).
- [ ] **Evidencia longitudinal** E4/E5 con retest 7/30/90 días reales.

## UI (diferido por diseño — Fase 4 entregó modelos, no pantallas)

- [ ] `MindBoostLanguageScreen` (Compose) consumiendo los builders de `presentation/`.
- [ ] Registro del track Lenguaje en `MindBoostHubScreen`.
- [ ] Verificación visual (preview) y accesibilidad.

## Gates no negociables antes de mostrar un claim (recordatorio §1.4 / §3.3)

1. ¿La evidencia soporta el estado? (ClaimEngine).
2. ¿El material fue nuevo / no confundido? (gates §5.6 + bank separation).
3. ¿Hay incertidumbre que mostrar? (siempre que calibre).
4. ¿El copy está aprobado? (ClaimEngine, nunca la UI).
