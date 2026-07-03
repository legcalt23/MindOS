# Test Account & Inputs Notes

> **Seguridad:** las credenciales NO se imprimen, NO se copian a logs/informes, NO se commitean.
> Pasar siempre por variable de entorno o archivo local git-ignored. Redactar cualquier token.

## Inputs necesarios para fases en vivo

| Variable | Para qué | Estado |
|----------|----------|--------|
| `APP_TEST_EMAIL` | Login real (auth-agent) | **falta** |
| `APP_TEST_PASSWORD` | Login real (auth-agent) | **falta** |
| `BASE_URL` / `API_ENV` | Entorno backend de prueba | **falta** (nota: Auth es Firebase, no BASE_URL custom; aplica a Functions/env) |
| Emulador / `ANDROID_DEVICE_ID` | android-agent (install/logcat) | **disponible** ✓ |
| macOS / Xcode / `IOS_SIMULATOR_NAME` | ios-agent | **no disponible** (Windows) |
| Feature flags | Rutas condicionales | por determinar |

## Confirmaciones de seguridad antes de cualquier login

1. Confirmar que el entorno **NO es producción** (proyecto Firebase de prueba/dev).
2. Usar cuenta de prueba dedicada (no datos reales de usuarios).
3. Logging sanitizado: capturar solo errores redactados, nunca tokens ni passwords.
4. No subir secretos ni `google-services.json`/`GoogleService-Info.plist` de producción a la auditoría.

## Flujos de login a cubrir (cuando haya credenciales)

Login válido · inválido · email vacío · password vacío · email mal formado · password incorrecta ·
red caída · timeout · sesión expirada · logout · reapertura con sesión persistida · back tras login ·
deep link a pantalla protegida sin/con sesión.

> Hoy: auth-agent en modo **estático** (revisar `FirebaseSessionRepository`, `AuthDiagnostics`, `AuthScreen`,
> verificación de email, timeouts de auth). Login en vivo **diferido** hasta recibir credenciales de prueba.
