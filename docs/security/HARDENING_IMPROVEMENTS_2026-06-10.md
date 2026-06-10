# Mejoras de blindaje de seguridad — MindOS

**Fecha:** 2026-06-10
**Método:** revisión adversarial multi-agente (16 dimensiones de seguridad sobre el código real,
cada hallazgo con doble verificación: ¿es real? / ¿ya está mitigado?). 96 hallazgos brutos →
curados y re-verificados a mano contra el código. Base auditada: commit `7ec5bdd` + posteriores.

## Veredicto

MindOS está **fuertemente endurecido** — el conjunto bloqueante del roadmap está cerrado y los
controles núcleo (App Check, reglas Firestore/Storage, validación de inputs, fail-closed económico,
borrado PII durable, secretos vía env) son sólidos. **Nivel ~4/5.** No está al máximo: hay mejoras
concretas. Ninguna es un agujero crítico explotable en el flujo actual; son endurecimiento real y
prerequisitos para escalar (p. ej. pagos reales).

> Nota: ~la mitad de los 96 hallazgos se descartaron por ser falsos positivos o **ya mitigados**
> (ej.: `profile_visits` ya es `create/update/delete: if false`; IP allowlist vacía ya es deny-by-default;
> guard no-op de desktop es by-design; dedup de webhook ya cubre replay del mismo eventId).

---

## P0 — Hacer ya (real, bajo esfuerzo)

### P0.1 — `deletionPseudonym`: secreto de fallback hardcodeado, no fail-closed
`functions/src/accountDeletion.ts:42-49`. Si en prod no están `ACCOUNT_DELETION_HMAC_SECRET` /
`PII_LOG_HMAC_SECRET` / `LOG_REDACTION_SECRET`, cae a `'mindos-account-deletion-local-fallback'` →
el pseudónimo HMAC se vuelve **predecible/reversible**, anulando la pseudonimización GDPR de los
registros financieros retenidos.
**Fix:** en proyecto de producción, **lanzar** si no hay secreto (fail-closed); centralizar un único
accessor de secreto requerido. Mismo patrón para `privacyLogging`. **Esfuerzo: S · Confianza: alta.**

### P0.2 — Vulnerabilidades de dependencias
`functions/package.json` (firebase-admin → 8 moderate transitivas vía `uuid`) y `package.json`
(firebase-tools → 3 moderate). **Fix:** actualizar a versiones parcheadas; añadir Dependabot/`npm audit`
gate en CI (ya hay audit en release; extender a devDeps root). **Esfuerzo: S · Confianza: alta.**

### P0.3 — Validación numérica laxa en attachments
`functions/src/lib/validators.ts:271-286`. `sizeBytes` acepta `Infinity`/float; `durationMs/width/height`
solo comprueban `typeof === 'number'` → aceptan `NaN`, `Infinity`, negativos, decimales.
**Fix:** `Number.isFinite()` + `Number.isInteger()` + rangos (min/max) por campo. **Esfuerzo: S · Confianza: alta.**

---

## P1 — Endurecimiento de alto valor

### P1.1 — Rate limiting por IP en superficies no autenticadas
`functions/src/lib/rateLimiter.ts` es **por-UID**; los endpoints HTTP no autenticados no tienen ninguno:
webhooks (`webhooks.ts:201-221`), `verifyClientIntegrity` (`appCheck.ts:9-14`), y el login admin no
tiene lockout por fuerza bruta (`adminAuth.ts`). **Fix:** throttle por IP (contador Firestore keyed por IP,
reusando el patrón existente) + lockout de fallos de auth admin. **Esfuerzo: M · Confianza: alta (defensa en profundidad).**

### P1.2 — Validación real de firma de webhook (prerequisito de dinero real)
`functions/src/webhooks.ts`: la validación criptográfica de providers reales es un **stub fail-closed**
(hoy correcto: rechaza todo sin sandbox). Antes de habilitar pagos reales hace falta:
(a) implementar HMAC/JWS por provider, que exige **preservar el raw body** (hoy se usa `req.body` parseado),
y (b) **replay protection por timestamp+nonce** además del dedup por eventId. **Esfuerzo: M-L · Confianza: alta.**

### P1.3 — Router `mindos/`: redacción y defaults inseguros
- `TraceRedaction.kt`: FNV-1a 32-bit **sin secreto** → reversible para ids de baja entropía. **Fix:** HMAC
  truncado con secreto rotable (como el logging de backend).
- `DefaultActionValidator` usa `AllowAllAuthorizer` por defecto y `AbuseSubject.Anonymous` por defecto;
  hoy solo `forProduction` lo bloquea. **Fix:** hacer imposible construir el default inseguro (authorizer
  explícito obligatorio).
- `MatchTrace`/telemetría en modo explain registra input crudo (`DefaultActionMatcher.kt:288`). **Fix:**
  gate tras no-prod o codificar a tokens. **Esfuerzo: M · Confianza: alta.**

### P1.4 — Storage: XSS inline y bearer-URL de larga vida
`storage.rules`: se aceptan `text/plain`/`application/json` en attachments y las reglas confían en el
Content-Type **declarado** (no en los bytes) → si se sirven inline, XSS almacenado. **Fix:** forzar
`Content-Disposition: attachment` (o ruta solo-descarga) y usar **signed URLs de vida corta** en lugar de
download tokens persistentes (el token resuelto sigue siendo un bearer no protegido por App Check).
**Esfuerzo: M · Confianza: media-alta.**

---

## P2 — Defensa en profundidad

- **Admin auth** (`adminAuth.ts`): validar `X-Forwarded-For` solo desde proxy de confianza (no confiar en
  el header crudo para el allowlist); rechazar `mfa_verified_at` con timestamp futuro; clarificar guard de
  allowlist vacío. **S-M.**
- **App Check**: consumir-una-vez / ventana de replay para tokens; verificar que reglas Firestore/Storage
  enforzan App Check independientemente del layer de Functions en rutas sensibles. **M.**
- **KMP data-at-rest** (`SecureKeyValueStore.android.kt`): claves Keystore con **StrongBox** + binding de
  autenticación donde esté disponible; documentar/garantizar unicidad del nonce GCM; exclusión de backup
  explícita para prefs cifradas. **S-M.**
- **Secret scanner** (`scan-secrets.js`): detectar strings de alta entropía y fallbacks hardcodeados. **S.**
- **Reglas Firestore**: revisar el scope de lectura de `observations` y añadir bound de delta en
  `observersCount` (evitar inflado client-side por update). **S (verificación) + S.**

---

## Acción externa (no cerrable desde este entorno)
- **iOS** App Check/enforcement server-side de escrituras (`MainViewController.kt`) → requiere macOS+Xcode.
- Restricción de API keys, app Firebase debug separada → Google/Firebase Console.
- Habilitar pagos reales (P1.2) → decisión de producto + secretos de provider.

---

## Recomendación de orden
P0.1 → P0.2 → P0.3 (todo S, alto impacto/coste bajo) → P1.1 → P1.3 → P1.4 → P1.2 (cuando se acerquen
pagos reales) → P2. Cada uno con su test (TDD) y verificación de gates.
