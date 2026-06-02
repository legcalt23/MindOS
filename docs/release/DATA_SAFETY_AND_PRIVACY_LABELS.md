# Data Safety (Google Play) & Privacy Nutrition Labels (App Store) — T7.3

**Fecha:** 2026-06-02
**Fuente:** derivado de `docs/security/DATA_INVENTORY.md` + SDKs integrados (Auth, Firestore,
Storage, Functions, Crashlytics, Analytics, Performance Monitoring, App Check, Remote Config).
**Uso:** rellenar la sección Data Safety de Play Console y las Privacy Nutrition Labels de App
Store Connect con esta tabla. NO es código — es la declaración obligatoria.

---

## 1. Datos recopilados (Google Play Data Safety)

| Tipo de dato | Recopilado | Compartido* | Propósito | Opcional/Req. | Fuente |
|---|---|---|---|---|---|
| Email | Sí | No | Autenticación, gestión de cuenta | Requerido | Firebase Auth |
| Nombre (displayName) | Sí | No | Funcionalidad de la app (perfil) | Requerido | profiles/profile_visuals |
| Identificadores de usuario (uid, handle) | Sí | No | Funcionalidad, antifraude | Requerido | Auth / Firestore |
| "Sobre mí" (bio, userphrase) | Sí | No | Funcionalidad (perfil) | Opcional | profiles |
| Fotos / vídeos | Sí | No | Funcionalidad (perfil, posts, DM) | Opcional | Storage privado, resuelto por reglas/App Check |
| Archivos de audio (notas de voz) | Sí | No | Funcionalidad (posts, DM) | Opcional | Storage privado, resuelto por reglas/App Check |
| Mensajes in-app (DM) | Sí | No | Funcionalidad (mensajería) | Opcional | dm_chats/messages |
| Otro contenido generado por el usuario (posts) | Sí | No | Funcionalidad (feed) | Opcional | posts |
| Acciones in-app (follows, observaciones, visitas, notif.) | Sí | No | Funcionalidad, analítica | Opcional | Firestore |
| Historial de compras / saldo (coins) | Sí | No | Funcionalidad (wallet) | Opcional | wallet (server-only)** |
| Registros de fallos (crash logs) | Sí | No | Diagnóstico | — | Crashlytics |
| Diagnóstico / rendimiento | Sí | No | Diagnóstico | — | Performance Monitoring |
| Otros IDs (installation ID / App Check) | Sí | No | Antifraude / seguridad / analítica | — | Firebase/Analytics |

\* "Compartido" en el sentido de Play (transferido a terceros para su propio uso): **No**. Google
(Firebase) actúa como **proveedor de servicios/procesador**, no como tercero al que se le cede para
uso propio → se declara como *recopilado y procesado*, no *compartido*.

\** Los **pagos reales** (cuando se integre IAP) los procesa el billing de la tienda; la app no
recopila números de tarjeta. Hoy el wallet es sandbox (ver `IAP_COMPLIANCE_DECISION.md`).

## 2. Prácticas de seguridad (Data Safety)
- [x] **Cifrado en tránsito** — HTTPS-only (`network_security_config.xml` prohíbe cleartext).
- [x] **App Check** — attestation de dispositivo (Play Integrity / DeviceCheck).
- [x] **El usuario puede solicitar el borrado de datos** — `deleteAccount` exige App Check,
      autenticación y `auth_time` reciente. Purga PII, borra media propia en Storage, elimina Auth
      al final y pseudonimiza registros financieros/auditables retenidos por obligación legal.
- [x] Datos sensibles no se escriben a logs (políticas de logging seguro en el repo).

## 3. Apple Privacy Nutrition Labels (App Store) — gated por reentrada de iOS

iOS está bloqueado explícitamente para producción en `MainViewController`; no publicar App Store
hasta tener Firebase Auth/App Check/Crashlytics/Storage/Functions cableados y compilados en macOS.

Equivalencia (cuando iOS entre en alcance):
- **Data Linked to You:** Email, Name, User ID, User Content (fotos/vídeos/audio/mensajes/posts),
  Identifiers, Purchases, Usage Data, Diagnostics.
- **Data Used to Track You:** evaluar Analytics. Si Analytics hace tracking cross-app/cross-site →
  declarar + **App Tracking Transparency (ATT)** con `NSUserTrackingUsageDescription` y prompt
  contextual. Si Analytics se limita a uso first-party y se desactiva la recolección de Advertising
  ID, puede declararse "no tracking". **Decisión pendiente** sobre configuración de Analytics.
- **Data Not Linked to You:** crash/diagnostics si se configuran sin identidad.

## 4. App Tracking Transparency (ATT) — iOS
- Si se mantiene Firebase Analytics con recolección de IDFA/advertising → **obligatorio** el prompt
  ATT antes de cualquier tracking, con copy contextual.
- Recomendación por defecto (privacy-first): **desactivar** recolección de Advertising ID en
  Analytics (`google_analytics_adid_collection_enabled=false`) para evitar ATT y simplificar las
  labels. Decisión de producto/marketing.

## 5. Acciones
1. Publicar la **política de privacidad** coherente con esta tabla y enlazarla en Play Console.
2. Decidir la configuración de **Analytics/Advertising ID** → determina ATT y "Used to Track You".
3. Rellenar Play Data Safety con esta tabla.
4. No rellenar App Store Privacy Labels como release-ready hasta desbloquear iOS.
