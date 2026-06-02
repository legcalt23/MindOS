# Firebase API key restrictions

**Fecha:** 2026-06-02  
**Alcance:** `androidApp/google-services.json`, `androidApp/src/debug/google-services.json`,
`shared/src/iosMain/resources/GoogleService-Info.plist`.

Las API keys de Firebase son identificadores publicos de cliente, no secretos. Aun asi, antes de
release deben estar restringidas en Google Cloud Console para reducir abuso desde apps no
autorizadas.

## Android release

- Key: la asociada a `com.mindos.app` en el proyecto Firebase de produccion.
- Application restriction: **Android apps**.
- Package name permitido: `com.mindos.app`.
- SHA permitidos: SHA-1/SHA-256 del keystore de release real.
- APIs permitidas: solo las APIs Firebase/Google realmente usadas por la app.
- Bloquear uso desde `com.example.*` y desde `com.mindos.app.debug`.

## Android debug

- Key: la asociada a `androidApp/src/debug/google-services.json`.
- Application restriction: **Android apps**.
- Package name permitido: `com.mindos.app.debug`.
- SHA permitidos: debug keystore local/CI, nunca el keystore de release.
- Proyecto recomendado: Firebase no productivo.

## iOS

iOS no esta habilitado para produccion. Cuando se desbloquee:

- Application restriction: **iOS apps**.
- Bundle ID permitido: el bundle real registrado para iOS.
- APIs permitidas: solo Firebase Auth, Firestore, Storage, Functions, App Check, Crashlytics y
  Analytics si se decide mantenerlo.
- Verificar App Check antes de permitir writes de perfil o Storage.

## Gate de release

Antes de publicar Android:

- [ ] Release key restringida a `com.mindos.app` + SHA del keystore de release.
- [ ] Debug key restringida a `com.mindos.app.debug` + SHA debug.
- [ ] APIs permitidas revisadas.
- [ ] App Check en modo enforcement para Auth/Firestore/Storage/Functions donde aplique.
- [ ] Captura o evidencia de consola adjunta al checklist interno de release.

Este gate es externo al repositorio; CI no puede verificarlo sin acceso a Google Cloud/Firebase
Console.
