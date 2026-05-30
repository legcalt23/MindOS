# Producción — Checklist GO / NO-GO (T8.3)

**Fecha:** 2026-05-29 · **Alcance:** Android-first (iOS aplazado, `IOS_STATUS.md`).
Documento autoritativo único de salida a producción. Consolida fases 1–7 + B1.

---

## VEREDICTO ACTUAL: 🔴 **NO-GO**

Faltan bloqueadores rojos (abajo). El producto está cerca en Android (base sólida), pero **no se
debe publicar** hasta cerrarlos. El pipeline y el runbook ya están listos para cuando se resuelvan.

### Bloqueadores rojos abiertos (impiden publicar)
| # | Bloqueador | Estado | Dónde |
|---|---|---|---|
| R1 | **IAP de la moneda virtual** (dinero real, 100 coins=1 €) | ❌ no implementado (sandbox no es publicable) | `IAP_COMPLIANCE_DECISION.md` (B2) |
| R2 | **Política de privacidad publicada (URL)** + Data Safety/Privacy Labels rellenados en consola | ❌ pendiente (contenido listo) | `DATA_SAFETY_AND_PRIVACY_LABELS.md` |
| R3 | **Keystore de release real** generado, respaldado y secrets en CI | ❌ pendiente | `gradle.properties.example` |
| R4 | **Ficha de tienda**: screenshots, feature graphic, descripción, **content rating (IARC)** | ❌ pendiente (assets/consola) | `STORE_SUBMISSION.md` |
| R5 | **Flujo de "reportar contenido" UGC** (Apple 1.2 / políticas Play) | ⚠️ a verificar (existe block, falta report) | `STORE_SUBMISSION.md` §G |

---

## Listas por área (estado verificado)

### Seguridad y backend ✅ (mayormente)
- [x] Reglas Firestore/Storage endurecidas (límites de query, App Check) — validación en CI (`backend-ci.yml`, JDK 21)
- [x] App Check (Play Integrity) en Android
- [x] Network security config (HTTPS-only)
- [x] Backend server-authoritative (wallet/ledger), 713 tests jest
- [x] Secret scan en CI (Functions)
- [ ] **Primer run verde de `backend-ci.yml` confirmado en GitHub** (reglas en emulador)
- [ ] Test de integración de `deleteAccount` contra emulador (hoy mockeado)

### Cuenta / privacidad / legal
- [x] **Borrado de cuenta in-app** (B1: backend + Kotlin + UI, 12 tests) ✅
- [x] Verificación de email al registrar
- [ ] R2 política de privacidad publicada + labels en consola
- [ ] Re-autenticación antes de borrar cuenta (endurecimiento recomendado)

### Build & signing
- [x] `applicationId=com.mindos.app`, `versionCode` dinámico (CI), R8/ProGuard, `.aab`
- [x] Pipeline de release con rollout escalonado (`release.yml`, environment protegido)
- [ ] R3 keystore real + secrets (`MINDOS_KEYSTORE_B64`, passwords, `PLAY_SERVICE_ACCOUNT_JSON`)
- [ ] `applicationIdSuffix ".debug"` (requiere app Firebase debug — `PHASE3_NOTES.md`)

### Pagos
- [ ] R1 Google Play Billing (cliente) integrado → backend ya preparado (webhooks)
- [ ] Productos IAP creados en Play Console (tiers ≥ mínimo; catálogo I3)
- [ ] Consentimiento de renuncia al desistimiento (UE) en el flujo de compra

### Observabilidad
- [x] Crashlytics (release) + breadcrumbs/custom keys (route, auth)
- [x] Analytics + Performance Monitoring (SDK)
- [x] Remote Config kill switches (`privacy_overlay_enabled` cableado; `waves`/`wallet` definidos)
- [ ] Plugin firebase-perf (auto HTTP) — diferido por compat AGP 9 (`PHASE6_NOTES.md`)
- [ ] Alertas configuradas (crash-free, error-rate Functions) — `RELEASE_RUNBOOK.md`

### Testing
- [x] Kotlin `:shared:desktopTest` 330/0/0
- [x] Functions jest 713/713
- [x] Smoke UI desktop (nav, state components, borrado de cuenta)
- [ ] Tests instrumentados de flujos críticos (login/DM/wallet) — pipeline GMD listo (manual)
- [ ] Test de reglas verde en CI confirmado

### Store readiness
- [ ] R4 metadata + screenshots + content rating
- [ ] R5 flujo de reporte de contenido UGC
- [x] Permisos justificados (CAMERA/RECORD_AUDIO/POST_NOTIFICATIONS) — `STORE_SUBMISSION.md`

### iOS
- [ ] Aplazado de v1 (shell de preview no funcional) — `IOS_STATUS.md`. NO publicar iOS.

---

## Camino a GO
1. Cerrar **R2/R3/R4/R5** (mayormente ops/consola/legal + un flujo de reporte).
2. Implementar **R1 (IAP)** con I2–I4 decididos y productos creados.
3. Confirmar CI verde (reglas + instrumentados) y un test de integración de borrado.
4. Beta cerrada (internal→beta) con flujos reales (compra sandbox + borrado de cuenta).
5. Seguir `RELEASE_RUNBOOK.md`: rollout 5%→20%→50%→100% con los umbrales y rollback definidos.
