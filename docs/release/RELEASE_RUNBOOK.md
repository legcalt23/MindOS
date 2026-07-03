# Release runbook — rollout escalonado, monitoreo y rollback (T8.2)

**Fecha:** 2026-05-29 · **Alcance:** Android (iOS aplazado, ver `IOS_STATUS.md`).

Procedimiento operativo para un lanzamiento controlado una vez superado el GO/NO-GO
(`PRODUCTION_GO_NO_GO.md`). El pipeline de publicación es `.github/workflows/release.yml`.

Antes de solicitar aprobación del entorno `production`, cada owner actualiza
`RELEASE_EVIDENCE.json` con `status: approved`, una referencia de evidencia y `verifiedAt`.
Ejecutar `npm run test:release-gates` y `npm run check:release-go`; no editar el workflow ni el
script para saltar un bloqueo. La firma release se valida aparte y `bundleRelease` falla si falta
cualquier propiedad de keystore.

---

## 1. Pre-vuelo (antes de pulsar "release")
- [ ] GO/NO-GO en verde (todos los bloqueadores rojos cerrados).
- [ ] `versionName` = tag semántico (`vX.Y.Z`); `versionCode` lo fija CI (run number).
- [ ] Secrets configurados: `MINDOS_KEYSTORE_B64` (+ passwords/alias), `PLAY_SERVICE_ACCOUNT_JSON`.
- [ ] API keys de Firebase restringidas (ver `../security/FIREBASE_API_KEY_RESTRICTIONS.md`).
- [ ] Política de privacidad publicada y Play Data Safety rellenado.
- [ ] `android-instrumented.yml` verde en flujos críticos o evidencia manual equivalente.
- [ ] Quality gates revisados (ver `QUALITY_GATES.md`).
- [ ] Crashlytics + Analytics + Performance verificados en una build interna real (dispositivo).
- [ ] Backend desplegado y `backend-ci.yml` (Functions + reglas) en verde.

## 2. Rollout escalonado (track `production`)
Subir con `release.yml` (dispatch, track=production) e ir aumentando `userFraction`:

| Etapa | Fracción | Permanencia mínima | Criterio para avanzar |
|---|---|---|---|
| 1 | **5%** | 24 h | crash-free ≥ 99.5% · ANR < 0.47% · sin pico de errores de Functions |
| 2 | **20%** | 24–48 h | métricas estables vs. etapa 1 |
| 3 | **50%** | 48 h | sin regresiones; reseñas/soporte sin incidencias graves |
| 4 | **100%** | — | todo verde |

Antes de production, pasar por **internal → closed (beta)** con testers reales (incluye el flujo de
compra IAP en sandbox y el **borrado de cuenta** end-to-end).

## 3. Señales a monitorear (primeras 24–72 h)
- **Crashlytics:** crash-free users/sessions (umbral **≥ 99.5%**), nuevos crashes por versión,
  velocity. Segmentar por `app_version` y `current_route` (custom key ya emitida).
- **ANRs** (Play Vitals): < 0.47% (umbral de "bad behavior" de Play).
- **Functions** (`functions_get_logs` / Cloud Monitoring): tasa de error y latencia de
  `verifyPurchase`/`initiatePurchase`/`deleteAccount`; alertar si error-rate > 2%.
- **Reglas/seguridad:** picos de `PERMISSION_DENIED` (señal de desfase regla↔cliente).
- **Wallet/ledger:** que `reconciliationService` siga cuadrando tras ventas reales.
- **Negocio:** funnel de registro→verificación email, compra IAP, retención D1.

## 4. Criterios de ROLLBACK / HALT
Detener o revertir el rollout si en la cohorte desplegada:
- crash-free **< 99.0%**, o un crash nuevo afecta a **> 0.5%** de sesiones, o
- ANR **> 0.47%**, o
- error-rate de cualquier callable de wallet **> 5%** o doble-cargo detectado, o
- fallo de `deleteAccount` (PII no purgada / Auth no borrado), o
- incidente de seguridad (fuga de datos, regla permisiva).

## 5. Procedimiento de rollback (Play no permite "des-publicar" un APK ya instalado)
1. **Halt staged rollout** en Play Console (detiene la expansión; los ya actualizados se quedan).
2. **Forward-fix**: subir una nueva `versionCode` corregida y reanudar el rollout escalonado desde 5%.
   - Para fallos del lado servidor (Functions/reglas): **revertir el deploy de Functions/rules**
     (no requiere update de app) — es la palanca más rápida.
   - **Kill switches (Remote Config):** desactivar la feature afectada al instante sin update:
     `privacy_overlay_enabled`, `wallet_enabled`, `waves_enabled`.
3. Comunicar a soporte; abrir post-mortem.

## 6. Post-release
- Vigilar 72 h; cerrar el rollout al 100% solo con métricas estables.
- Etiquetar el commit liberado; registrar versionCode↔tag.
- Revisar reseñas y Play Vitals semanalmente.
