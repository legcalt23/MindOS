# 01. Mapa del Sistema: Motor de Enfoque de Molvex (Focus v5)

## Pipeline End-to-End
1. **Onboarding / Objetivo:** Selección de metas atencionales (atención sostenida, selectiva, inhibición).
2. **Catálogo & Selección:** Catalogo unificado en `FocusSessionDescriptor` (shared/commonMain).
3. **Runtime & State Machine:** Reductor determinista `FocusSessionReducer` impulsado por ticks monotónicos y eventos de estímulo/respuesta.
4. **Sonda de Temporización:** `WindowedTemporalProbe` (KMP) captura jitter, frame drops y tasa de refresco (HzBand).
5. **Elegibilidad de Evidencia:** `FocusTemporalEligibilityAdversarialOracle` dictamina si la sesión es APPLICABLE para score oficial o degradada a TRAINING_ONLY.
6. **Persistencia & Ingestión:** Guardado local offline-first con idempotency key.
7. **Estimador & Perfil:** Separación estricta entre Performance Score (0-100) y Growth Index longitudinal con incertidumbre.
