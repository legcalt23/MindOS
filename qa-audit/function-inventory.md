# Function Inventory (inicial)

Derivado de recon estático ligero (glob de `*Screen.kt` en `commonMain` + arquitectura de `CLAUDE.md`).
**No** es exhaustivo: los agentes navigation/ui lo completarán con botones, diálogos, deep links y estados.

Paquete base: `com.mindos.app`. Cada pantalla aún sin probar → `not_tested`.

## Áreas y pantallas detectadas (37)

| Área | Pantallas | Precondición típica | Plataformas | test_status |
|------|-----------|---------------------|-------------|-------------|
| auth | `AuthScreen` | ninguna | common | not_tested |
| intro | `MindOSIntroScreen` | ninguna | common | not_tested |
| home | `HomeScreen` | login | common | not_tested |
| social/profile | `ProfileScreen`, `ProfileEntryScreen`, `ProfileCoverScreen`, `ProfileSettingsScreen`, `PrivacySettingsScreen` | login | common | not_tested |
| social/feed | `GlobalFeedScreen`, `GlobalFeedComposerScreen` | login | common | not_tested |
| social/graph | `SocialGraphScreen`, `FollowRequestsScreen` | login | common | not_tested |
| social/notif | `NotificationsScreen` | login | common | not_tested |
| profile (nuevo) | `MindOSProfileScreen` | login | common | not_tested |
| dm / waves | `DMListScreen`, `DMChatScreen`, `CajoneraScreen`, `WavesChatUserInfoScreen` | login + participante | common | not_tested |
| search | `UsernameSearchScreen` | login | common | not_tested |
| wallet | `MoneyScreen` | login | common | not_tested |
| element_selection | `ElementSelectionScreen` | login | common | not_tested |
| privacy | `SettingsScreen` | login | common | not_tested |
| mindboost/hub | `MindBoostHubScreen` | login | common | not_tested |
| mindboost/focus (Enfoque) | `FocusRootScreen`, `FocusHomeScreen`, `FocusSessionScreen`, `FocusSummaryScreen` | login | common | not_tested |
| mindboost/memory | `MemoryRootScreen`, `MemoryHomeScreen`, `MemorySessionScreen` | login | common | not_tested |
| mindboost/speed (Velocidad Mental) | `SpeedRootScreen`, `SpeedExerciseScreen`, `SpeedDashboardScreen`, `E1DestelloScreen`, `E3CifradoScreen`, `E6MultiviaScreen`, `E7CompuertaScreen` | login | common | not_tested |

## Capacidades transversales a verificar (de CLAUDE.md)

- **Navegación**: back stack propio (`AppRouteStack`), back físico Android (`PlatformBackHandler`), rebinding por auth.
- **Sesión/Auth**: email/password Firebase, verificación de email, account-enumeration mitigado, logout limpia estado.
- **Permisos**: `POST_NOTIFICATIONS` (Android 13+) contextual; Camera/Mic/Photos (media).
- **Persistencia cifrada**: `SecureKeyValueStore` (AES-256-GCM + Android Keystore) para datos cognitivos MindBoost.
- **Red/timeouts**: `FirestoreDefaults` (Auth/Firestore/Storage/Functions). Límites `.limit()` espejo de `firestore.rules`.
- **Wallet**: Cloud Functions callables (getBalance/getTransactions/initiate/verifyPurchase) con idempotencia.
- **Observabilidad**: `ObservabilityHub` + Crashlytics (breadcrumbs/custom keys, PII-free).

## Estado por función (leyenda)

`tested_ok | failed | blocked | not_applicable | not_reachable | needs_credentials | needs_backend_data | needs_platform_device`
