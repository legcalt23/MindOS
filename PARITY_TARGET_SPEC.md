# PARITY_TARGET_SPEC.md

**Date:** 2026-05-27
**Project:** MindOS KMP

---

## Definicion de paridad: que significa "misma app" en iOS y Android

Este documento establece los criterios objetivos de paridad entre plataformas. Cada dimension incluye objetivo, riesgo, criterio de aceptacion, metodo de verificacion, y diferencias nativas aceptables.

---

## 1. Paridad visual

**Objetivo:** Misma apariencia pixel-level en ambas plataformas, controlada por tokens compartidos.

**Riesgo:** Compose Multiplatform renderiza via Skiko en iOS (no UIKit nativo), lo que produce renderizado identico EXCEPTO en: system fonts, text rendering hinting, scroll physics, y system chrome (status bar, nav bar).

**Criterio de aceptacion:**
- Todos los colores provienen de `MindOSTheme` o tokens centralizados
- Tipografia usa fuentes del design system (no system defaults sin control)
- Spacing, radius, elevation usan tokens
- No Color() literals hardcodeados fuera de tokens
- Dark mode consistente en ambas plataformas

**Como se prueba:**
- Screenshot comparison (manual o Paparazzi/Roborazzi en Android, snapshot testing en desktop)
- Visual checklist por pantalla

**Diferencias nativas aceptables:**
- Subpixel text antialiasing (Skia vs CoreText)
- System font metrics ligeramente diferentes
- Status bar / navigation bar chrome nativo
- Keyboard appearance nativa

**Diferencias NO aceptables:**
- Colores diferentes para el mismo componente
- Espaciado diferente entre plataformas
- Tamano de fuente diferente
- Componentes que existen en una plataforma pero no en otra

---

## 2. Paridad funcional

**Objetivo:** Cada feature produce el mismo resultado dado el mismo input en ambas plataformas.

**Riesgo:** Logic bifurcation through expect/actual implementations may introduce different behavior.

**Criterio de aceptacion:**
- Business logic 100% en commonMain
- expect/actual solo para APIs de plataforma (filesystem, camera, audio, logging)
- Validaciones identicas (compartidas)
- Mismo flujo de estados (Loading -> Success/Error/Empty)

**Como se prueba:**
- Unit tests en commonTest ejecutados en Android y iOS JVM targets
- Manual QA script por pantalla

**Diferencias nativas aceptables:**
- Camera API implementation details
- Audio recording codec/format differences
- File picker UI nativa

**Diferencias NO aceptables:**
- Validacion que pasa en Android pero falla en iOS (o viceversa)
- Feature disponible en una plataforma pero no en otra (excepto camera-specific)
- Error message diferente para el mismo error

---

## 3. Paridad de navegacion

**Objetivo:** Mismo graph de navegacion, mismas transiciones logicas, mismo back behavior.

**Riesgo:** iOS swipe-back gesture and Android system back button behave differently at the OS level.

**Criterio de aceptacion:**
- Todas las rutas definidas en `AppRoute` sealed interface (compartido)
- Back behavior definido centralmente en `MindOSApp`
- No pantallas huerfanas
- No flujos rotos

**Como se prueba:**
- Desktop UI smoke tests (existing)
- Navigation flow diagram audit
- Manual QA script

**Diferencias nativas aceptables:**
- iOS edge swipe gesture vs Android back button/gesture
- System-level back handling (AndroidX OnBackPressedDispatcher vs iOS interactivePopGesture)

**Diferencias NO aceptables:**
- Route available on one platform but not the other
- Different destination after same user action
- Back behavior that navigates to different screens

---

## 4. Paridad de estados

**Objetivo:** Cada pantalla muestra los mismos estados (idle, loading, success, empty, error, offline) en ambas plataformas.

**Riesgo:** State holders instantiated per-platform could diverge if not shared.

**Criterio de aceptacion:**
- UiState model compartido en commonMain
- Todos los state holders en commonMain
- Loading, error, empty states renderizados por componentes compartidos

**Como se prueba:**
- Unit tests por state holder
- Manual QA with network off/slow/error scenarios

**Diferencias nativas aceptables:** None.

**Diferencias NO aceptables:**
- Loading spinner on one platform, blank screen on other
- Error shown on one platform, crash on other
- Empty state text different between platforms

---

## 5. Paridad de animaciones

**Objetivo:** Mismas animaciones y transiciones en ambas plataformas.

**Riesgo:** Compose animation APIs are shared, but frame timing may differ.

**Criterio de aceptacion:**
- Animaciones definidas en commonMain
- No platform-specific animation implementations unless required
- Animation durations use shared constants

**Como se prueba:**
- Visual comparison (manual)
- Animation token constants in design system

**Diferencias nativas aceptables:**
- Frame rate differences (60 vs 120 Hz)
- Scroll physics (overscroll bounce on iOS vs glow on Android)

**Diferencias NO aceptables:**
- Animation present on one platform but absent on other
- Different animation type (fade vs slide)

---

## 6. Paridad de formularios

**Objetivo:** Todos los formularios (auth, profile edit, search, publisher) behave identically.

**Criterio de aceptacion:**
- Same fields, same order, same validation
- Same error messages for same invalid input
- Same keyboard types
- Same submit behavior

**Como se prueba:**
- Unit tests for validators (SafeInput, value objects)
- Manual form testing

**Diferencias nativas aceptables:**
- Keyboard appearance (native keyboard)
- Autocorrect/spell check behavior

**Diferencias NO aceptables:**
- Different validation rules
- Different error text
- Missing field on one platform

---

## 7. Paridad de validaciones

**Objetivo:** Todas las validaciones viven en commonMain y producen resultados identicos.

**Criterio de aceptacion:**
- SafeInput en commonMain
- Value objects (Handle, DisplayName, Bio, UserId) en commonMain
- DomainResult en commonMain
- No platform-specific validation

**Como se prueba:**
- commonTest unit tests (existing: SafeInputTest, value object tests)

**Diferencias nativas aceptables:** None.
**Diferencias NO aceptables:** Any validation that behaves differently.

---

## 8. Paridad de errores

**Objetivo:** Mismo error produce mismo mensaje en ambas plataformas.

**Criterio de aceptacion:**
- DomainError sealed hierarchy en commonMain
- Error-to-user-message mapping en commonMain
- Auth error mapping en commonMain (AuthDiagnostics exists)
- Network error mapping standardized

**Como se prueba:**
- Unit tests for error mapping
- Manual error injection testing

**Diferencias nativas aceptables:** None.
**Diferencias NO aceptables:** Different error text, missing error handling on one platform.

---

## 9. Paridad de permisos

**Objetivo:** Permission requests and handling produce same user experience.

**Criterio de aceptacion:**
- Camera, microphone, storage permissions requested consistently
- Permission denied state handled identically
- Runtime permission flow documented

**Como se prueba:**
- Manual QA on both platforms
- Permission denied scenario testing

**Diferencias nativas aceptables:**
- OS permission dialog appearance (native)
- Permission flow timing (Android runtime vs iOS first-use)

**Diferencias NO aceptables:**
- Feature works without permission on one platform but requires it on other
- Different behavior after permission denied

---

## 10. Paridad offline

**Objetivo:** Same behavior when network is unavailable.

**Criterio de aceptacion:**
- Firebase offline cache behaves consistently (Firestore has built-in offline)
- Error states for network failures are identical
- Retry mechanisms are shared

**Como se prueba:**
- Manual airplane mode testing
- Network error injection

**Diferencias nativas aceptables:** None.
**Diferencias NO aceptables:** Crash on one platform, graceful degradation on other.

---

## 11. Paridad de rendimiento

**Objetivo:** App feels equally responsive on both platforms.

**Criterio de aceptacion:**
- No visible jank on either platform
- List scrolling smooth
- Image loading with placeholders
- No main thread blocking

**Como se prueba:**
- Manual feel testing
- Android: Compose metrics, strict mode
- iOS: Instruments (if available)

**Diferencias nativas aceptables:**
- Absolute frame timing differences
- Memory model differences (ARC vs GC)

**Diferencias NO aceptables:**
- Visible jank on one platform but not other
- Significantly different load times

---

## 12. Paridad de accesibilidad

**Objetivo:** Screen readers navigate the app equivalently on both platforms.

**Criterio de aceptacion:**
- All interactive elements have contentDescription/semantics
- All icon-only buttons have labels
- Touch targets >= 48dp
- Headings marked in Compose semantics

**Como se prueba:**
- TalkBack (Android) and VoiceOver (iOS) manual testing
- Compose semantics tree inspection

**Diferencias nativas aceptables:**
- Screen reader voice/behavior (TalkBack vs VoiceOver)
- Focus order may differ slightly due to platform rendering

**Diferencias NO aceptables:**
- Interactive element without label on one platform
- Inaccessible flow on one platform

---

## 13. Paridad de localizacion

**Objetivo:** Same text content on both platforms for the same locale.

**Criterio de aceptacion:**
- Strings centralized in shared code
- Date/time formatting shared or abstracted
- Number formatting shared or abstracted

**Como se prueba:**
- String audit
- Locale switching test

**Diferencias nativas aceptables:**
- Locale-specific number/date formatting differences (minor)

**Diferencias NO aceptables:**
- Different button text
- Different error message text
- Missing translation on one platform

---

## 14. Paridad de analitica

**Objetivo:** Same analytics events fired on both platforms for same user actions.

**Criterio de aceptacion:**
- Analytics abstraction in commonMain
- Event names and parameters consistent

**Como se prueba:**
- Analytics event log comparison

**Diferencias nativas aceptables:** None in event names/parameters. Implementation may differ.

---

## 15. Paridad de crash reporting

**Objetivo:** Crashes on both platforms are captured and reported consistently.

**Criterio de aceptacion:**
- Crashlytics or equivalent configured for both
- Non-fatal error logging consistent

**Como se prueba:**
- Forced crash test
- Dashboard verification

**Diferencias nativas aceptables:**
- Stack trace format (JVM vs native)
- Crash report metadata

---

## 16. Paridad de release

**Objetivo:** Both platforms can be built, signed, and released with equivalent confidence.

**Criterio de aceptacion:**
- Android release build with R8/ProGuard
- iOS release build (archive)
- Signing configured for both
- Version management shared

**Como se prueba:**
- Release build smoke test
- Store upload dry run

**Diferencias nativas aceptables:**
- Build toolchain (Gradle vs Xcode)
- Store submission process (Play Console vs App Store Connect)

**Diferencias NO aceptables:**
- Release build fails on one platform
- Missing signing configuration
