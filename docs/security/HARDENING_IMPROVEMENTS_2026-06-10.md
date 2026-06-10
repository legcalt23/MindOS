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

> **✅ Fase P0 ejecutada (2026-06-10):** P0.1 `7ec5bdd`→`208ea35`, P0.2 `22c754b`, P0.3 (este commit).
> Backend `766/766` tests, typecheck OK, `npm audit` 0 vulns. Decisiones D-P0.1/D-P0.2 abajo.

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

## Decisiones de seguridad registradas (ejecución Fase P0)

### D-P0.1 — `privacyLogging` NO se hace fail-closed (decisión deliberada)
Al implementar P0.1 se evaluó aplicar el mismo *fail-closed* (throw si falta secreto) a
`privacyLogging.logSafeRef`. **Rechazado:** `logSafeRef` se invoca en casi todos los logs de las
Cloud Functions; lanzar ahí convertiría un secreto ausente en un **fallo de disponibilidad** (cada
log crashea la función), un riesgo peor que el que mitiga. A diferencia de `deletionPseudonym` (que
**persiste** pseudónimos GDPR), `logSafeRef` solo redacta para Cloud Logging y su fallback es un
SHA-256 con sal constante (one-way, no reversible trivialmente para ids de alta entropía).
**Resolución:** `deletionPseudonym` → fail-closed en prod (implementado). `logSafeRef` → mantiene el
fallback; el secreto SHOULD estar configurado en prod, verificable por checklist de despliegue, no por
crash en runtime. Estado: aceptado.

### D-P0.2 — uuid: `overrides` en vez de `npm audit fix --force`
Las 11 vulns (8 functions + 3 root) eran un único CVE moderate (`uuid <11.1.1`, bounds-check en
v3/v5/v6 con `buf` — path **no usado** por las libs de Google, que usan v4 sin buf). `npm audit fix
--force` **degradaba firebase-tools 15.19.0 → 13.13.3** (rompe M9/JDK21). Ningún `firebase-admin`
(≤13.10.0) mueve a uuid parcheado → sin fix upstream de parent. **Resolución:** `overrides: { uuid:
"^11.1.1" }` en root y functions. Verificado: firebase-admin carga + uuid v4 OK + 757/757 tests +
`audit:deps` moderate=0; firebase-tools CLI carga (sigue 15.19.0). **Residual:** el efecto sobre el
emulador de firebase-tools (`test:rules`) no se validó localmente (requiere JDK21) → lo valida CI.
Dependabot añadido para retirar el pin cuando upstream publique tree limpio. Estado: aceptado.

### D-P1.2 — Validación real de firma de webhook: DIFERIDA (gated por producto)
Decisión tomada durante la ejecución de la Fase P1. La validación criptográfica real de los webhooks
de provider (Google Play / Apple / Stripe) **no se implementa en esta fase**. Motivos:
1. El endpoint está hoy **fail-closed** (403 para providers reales) — el estado seguro. Implementar la
   firma lo convertiría en una ruta que **acepta eventos reales**, paso que depende de la **decisión de
   producto de habilitar pagos reales** (no tomada).
2. Requiere secretos/material por provider no disponibles en este entorno (clave pública JWT de Google
   Play / cadena JWS x5c de Apple / secret HMAC de Stripe) y verificación provider-específica que solo
   puede testearse contra esos secretos reales.
3. Sin (1) y (2), implementarlo sería código muerto o, peor, una ruta de aceptación de eventos no
   verificados en la práctica.

**Prerrequisitos pendientes para cuando se active** (no implementados, para no introducir scaffolding
de fase futura): preservar el **raw body** de la request (hoy se usa `req.body` parseado) para el HMAC,
y replay protection por **timestamp+nonce** además del dedup por `eventId` ya existente. Estado:
diferido a la fase de "pagos reales".

### D-P1.3 — Router `mindos/`: parcialmente implementado
- **AllowAllAuthorizer (M24) → IMPLEMENTADO.** `ActionExecutionRuntimeConfig` (producción) ahora
  rechaza un `DefaultActionValidator` que use `AllowAllAuthorizer` (`DefaultActionValidator.authorizesAll`
  + check en `productionWiringFailures()`). Cero ripple en tests (el default cómodo sigue en
  `forTests/forDevelopment`). Verificado con `ProductionAuthorizerGuardTest`.
- **TraceRedactor FNV-32 sin clave (M22) → DIFERIDO.** Un hash sin clave no se arregla ensanchando bits
  (inputs de baja cardinalidad son fuerza-bruteables). El fix real exige una **clave secreta inyectada
  desde la plataforma** (commonMain KMP no accede a env), lo que requiere un diseño de key-injection a
  través del runtime config del router. No se hackea una sal hardcodeada (seguiría siendo keyless).
- **MatchTrace explain-mode (M25) → DIFERIDO.** Requiere analizar la superficie de exposición real
  (¿llega a respuestas de API?) antes de gatear; pendiente de esa verificación.

### D-P1.4 — Storage XSS inline / signed URLs: residual bajo + rearquitectura diferida
Análisis al ejecutar P1: los tipos MIME **ejecutables inline** (text/html, image/svg+xml, xhtml) **ya
están excluidos** del allowlist de attachments; los permitidos como File (pdf, text/plain,
application/json) **no ejecutan script inline** en navegadores modernos (con nosniff). El riesgo XSS
real es bajo/mitigado. La parte grande (signed URLs de vida corta vs download tokens) conecta con H1,
**ya direccionado** (URLs resueltas en lectura desde `storagePath`, no persistidas). Forzar
`Content-Disposition: attachment` indiscriminado **rompería** el render inline de imágenes/vídeo, así
que la variante correcta (disposition por-tipo solo en File) es un cambio **cross-platform matizado**
diferido a una pasada dedicada de storage. Sin cambio de código en esta fase.

## Recomendación de orden
P0.1 → P0.2 → P0.3 (todo S, alto impacto/coste bajo) → P1.1 → P1.3 → P1.4 → P1.2 (cuando se acerquen
pagos reales) → P2. Cada uno con su test (TDD) y verificación de gates.
