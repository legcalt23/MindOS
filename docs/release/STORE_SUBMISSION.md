# Store submission checklist — Google Play & App Store (T7.2)

**Fecha:** 2026-05-29
**Alcance:** Android-first (iOS aplazado, ver `IOS_STATUS.md`).

Reúne lo que se rellena en las consolas (no es código). Marca el estado real para no
auto-engañarse antes de subir.

---

## A. Bloqueadores que deben cerrarse ANTES de subir
- [ ] **Borrado de cuenta in-app** — `docs/security/ACCOUNT_DELETION_DECISION.md` (obligatorio ambos stores).
- [ ] **IAP de la moneda virtual** — `docs/security/IAP_COMPLIANCE_DECISION.md` (si se vende por dinero real).
- [ ] **Data Safety / Privacy Labels** — `docs/release/DATA_SAFETY_AND_PRIVACY_LABELS.md`.
- [ ] **Keystore de release** configurado en secrets de CI (T7.1 hecho a nivel de pipeline; falta el .jks real + secrets).
- [ ] **Política de privacidad publicada** (URL) y enlazada en ambas fichas.

## B. Identidad de la app (verificado en repo)
- applicationId: `com.mindos.app` · versionName: `1.0` · versionCode: dinámico (CI).
- minSdk 29 · targetSdk 36 · idioma: español (es) — ver `PHASE4_I18N_DECISION.md`.

## C. Metadata de ficha (rellenar — DRAFT)
| Campo | Google Play | App Store |
|---|---|---|
| Nombre | MindOS | MindOS |
| Subtítulo / breve | (≤30 car. Play short desc) | (≤30 car. subtitle) |
| Descripción | full description (≤4000) | description |
| Keywords | (n/a Play) | (≤100 car.) |
| Categoría | (definir: Social / Lifestyle) | (definir) |
| Email de soporte / URL | (requerido) | (requerido) |
| Política de privacidad URL | (requerido) | (requerido) |

> Texto de marketing/descripciones: pendiente de redacción de producto. Estructura lista para
> `fastlane/metadata` si se adopta fastlane.

## D. Screenshots / gráficos (assets — pendientes, requieren capturas reales)
- **Google Play:** icono 512×512, feature graphic 1024×500, ≥2 phone screenshots (16:9 o 9:16),
  (opcional tablet/7"/10").
- **App Store:** screenshots por tamaño de dispositivo requerido (6.7", 6.5", 5.5", iPad si aplica).
- **Estado:** NO generables en este entorno (sin dispositivo). Capturar desde la app real.

## E. Content rating / clasificación
- **Google Play (cuestionario IARC):** responder según contenido real:
  - UGC (posts, DM, perfiles) → declarar **contenido generado por usuarios** + moderación.
  - Mensajería directa entre usuarios → sí.
  - Compras digitales (coins) → sí (si I1=sí).
  - Sin violencia/apuestas/sustancias salvo lo anterior.
- **App Store age rating:** equivalente; UGC + comunicación entre usuarios suele situar 12+/17+
  según moderación. Declarar moderación de UGC (requisito de Apple 1.2 para apps con UGC:
  filtro, reporte, bloqueo, EULA). **Verificar que existe flujo de reporte/bloqueo** (hay `blocks/`
  en el modelo; confirmar UI de reporte de contenido — posible gap).

## F. Permisos a justificar en las fichas/revisión
- `CAMERA` — captura de media de perfil/posts **y** overlay de privacidad (cámara continua). Esta
  última requiere justificación clara; ver kill switch `privacy_overlay_enabled` (Fase 6).
- `RECORD_AUDIO` — notas de audio / audio en DM y posts.
- `POST_NOTIFICATIONS` — notificaciones (solicitar contextual, ya implementado).

## G. Riesgo de revisión señalado
- **UGC + DM sin flujo de reporte de contenido visible** podría incumplir Apple 1.2 / políticas de
  Play de UGC. Confirmar/implementar "reportar" además de "bloquear". (No verificado en esta fase —
  riesgo a revisar.)
