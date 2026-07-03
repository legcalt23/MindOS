# security-agent (Lote B — pase enfocado)

## AGENT_RESULT

agent_id: `security-agent`
scope: `Estatico: datos faciales ML Kit, secretos hardcodeados, presencia fabricada, wiring de privacidad`
status: `partial` (pase enfocado en follow-ups de Lote A; api/tokens en profundidad pendiente)
confidence: `88`
files_examined_count: `2` (PrivacyScreenOverlay.android.kt, App.kt grep) + 3 greps
commands_run_count: `3` (greps)
evidence_files:
- `shared/src/androidMain/kotlin/com/mindos/app/features/privacy/PrivacyScreenOverlay.android.kt`

### ARCH-002 — RESUELTO (con matices)

`PrivacyScreenOverlay.android.kt` = overlay anti-shoulder-surfing: cámara **frontal** + ML Kit detecta
caras/ángulos de cabeza y oscurece la pantalla si detecta a alguien mirando (eulerY/Z >25°, o >1 cara,
o ausencia >1.5s). **Manejo del dato biométrico = correcto:**
- Detección **100% on-device** (ML Kit local), `STRATEGY_KEEP_ONLY_LATEST`.
- Solo se leen `headEulerAngleY/Z` + nº de caras → un float de protección. Classification/Landmark/Contour = NONE.
- `imageProxy.close()` siempre; `detector.close()` + `cameraProvider.unbindAll()` en `onDispose`.
- **Nada se persiste ni se transmite.** No hay almacenamiento ni upload de imágenes/embeddings.
- Es **opt-in**: gateado tras `privacyEnabledState` (App.kt:102), no activo por defecto.
→ ARCH-002 degradado a informativo. El riesgo residual es de **disclosure/consentimiento**, no de fuga de datos.

### findings

- id: `PRIV-001`
  title: `Permiso de CÁMARA frontal solicitado sin rationale al activar "pantalla de privacidad"`
  severity: `P3`
  category: `security`
  target: `android`
  reproducibility: `always`
  user_impact: `el usuario activa "privacidad" y de repente se le pide la cámara frontal sin explicar que se analizará su rostro en continuo`
  technical_impact: `LaunchedEffect(Unit){ launcher.launch(CAMERA) } sin diálogo de rationale; requiere disclosure en Play Data Safety + política de privacidad`
  evidence:
    - `PrivacyScreenOverlay.android.kt:69-73`
  recommended_next_action: `mostrar rationale antes de pedir CAMERA explicando el uso continuo de cámara frontal on-device; declarar en Data Safety`

- id: `PRIV-002`
  title: `Si se deniega la cámara (o falla el bind / no hay cámara frontal), la protección de privacidad falla en silencio`
  severity: `P3`
  category: `security`
  target: `android`
  reproducibility: `always`
  user_impact: `el usuario cree que la pantalla de privacidad le protege, pero con permiso denegado el overlay nunca se activa, sin aviso`
  technical_impact: `hasCameraPermission=false → bloque no se monta; bindToLifecycle catch(Exception) solo loggea. Sin feedback ni redirect a ajustes.`
  evidence:
    - `PrivacyScreenOverlay.android.kt:65-75, 126-131`
  recommended_next_action: `si el toggle está ON pero falta permiso/cámara, mostrar estado "protección inactiva" + acción para conceder permiso/ajustes`

### Verificaciones limpias

- **Secretos:** grep de `AIza…`, `sk_live/test`, claves PEM, `password="…"`, `apiKey="…"` en `*.kt` → **0 coincidencias**. (Heurístico; `scan:secrets` de functions queda para Lote backend.)
- **NAV-003 (presencia fabricada):** único `isOnline = true` está en `AppRouteRebinderTest.kt` (fixture). Producción limpia. **Cerrado.**

blocked_by:
- `Revisión profunda de almacenamiento de tokens de sesión, refresh 401/403 y manejo de errores de red en repos gitlive → pendiente (api-agent, Lote B restante).`

summary_for_brain: |
  ARCH-002 RESUELTO: overlay anti-mirones usa cámara frontal+ML Kit 100% on-device, sin persistir ni
  transmitir nada, opt-in tras privacyEnabledState. Riesgo residual = disclosure/consentimiento, no fuga.
  2 findings P3: PRIV-001 (pide CÁMARA sin rationale al activar), PRIV-002 (protección falla en silencio si
  se deniega permiso/cámara). Secretos limpio. NAV-003 cerrado (solo test fixture). Falta api/tokens en profundidad.
