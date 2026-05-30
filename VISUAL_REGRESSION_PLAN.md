# Visual Regression Plan — MindOS

> Fecha de creacion: 2026-05-27
> Stack: Kotlin Multiplatform + Compose Multiplatform 1.10.3
> Targets: Android (API 29+), iOS (15.0+), Desktop (JVM/Skia)

---

## Tabla de contenidos

1. [Estado actual del proyecto](#1-estado-actual-del-proyecto)
2. [Limitaciones del stack](#2-limitaciones-del-stack)
3. [Herramientas recomendadas](#3-herramientas-recomendadas)
4. [Inventario de pantallas](#4-inventario-de-pantallas)
5. [Inventario de componentes compartidos](#5-inventario-de-componentes-compartidos)
6. [Auditoria de valores magicos](#6-auditoria-de-valores-magicos)
7. [Flujo de captura — Android](#7-flujo-de-captura--android)
8. [Flujo de captura — Desktop (baseline comun)](#8-flujo-de-captura--desktop-baseline-comun)
9. [Flujo de captura — iOS](#9-flujo-de-captura--ios)
10. [Flujo de comparacion](#10-flujo-de-comparacion)
11. [Umbrales de tolerancia](#11-umbrales-de-tolerancia)
12. [Diferencias nativas inevitables](#12-diferencias-nativas-inevitables)
13. [Como revisar diffs](#13-como-revisar-diffs)
14. [Como actualizar snapshots](#14-como-actualizar-snapshots)
15. [Integracion en CI](#15-integracion-en-ci)
16. [Snapshots obligatorios — pantallas](#16-snapshots-obligatorios--pantallas)
17. [Snapshots obligatorios — componentes](#17-snapshots-obligatorios--componentes)
18. [Checklist visual por pantalla](#18-checklist-visual-por-pantalla)
19. [Plan de eliminacion de valores magicos](#19-plan-de-eliminacion-de-valores-magicos)
20. [Proceso manual para revision visual](#20-proceso-manual-para-revision-visual)

---

## 1. Estado actual del proyecto

### Design system centralizado

| Archivo | Contenido | Estado |
|---|---|---|
| `AppColors.kt` | 40+ tokens de color (brand, neutral, semantic, state) | Existe |
| `AppSpacing.kt` | Escala 4dp (2→40dp) + tokens semanticos | Existe |
| `AppShapes.kt` | Radios (4→999dp) + shapes prebuilt | Existe |
| `AppElevation.kt` | 5 niveles (0→16dp) | Existe |
| `AppAnimation.kt` | 4 duraciones + stagger delay | Existe |
| `AppStrings.kt` | ~80 strings centralizados en espanol | Existe |
| `MindOSTheme.kt` | Material3 dark color scheme + Typography | Existe |
| `StateComponents.kt` | Loading, Empty, Error, Offline screens | Existe |

### Deuda visual pendiente

| Tipo de literal | Ocurrencias en `features/` | Archivos afectados |
|---|---|---|
| `Color(0xFF...)` | **205** | 23 archivos |
| `N.dp` sueltos | **790** | 36 archivos |
| `N.sp` sueltos | **109** | 22 archivos |
| `RoundedCornerShape(N)` | **160** | 24 archivos |

### Tokens duplicados (deben migrarse a AppColors)

| Archivo | Duplicados |
|---|---|
| `SearchVisualTokens.kt` | `NeuBlack`, `NeuWhite`, `NeuGreen`, `NeuMist`, `TargetGold` — todos ya existen en `AppColors` |
| `ViewerDesign.kt` | `Black`, `White`, `Green`, `Mist`, `Accent` + spacing/typography locales |
| `CajoneraScreen.kt` (lineas ~1097-1108) | Paleta `Background`, `FrameTop`, `DrawerTop`, etc. — 12 colores propios |
| `MindOSIntroConfig.Palette` | 8 colores cosmicos — aceptables como excepcion artistica |
| `ProfileEditorComponents.kt` | 25+ colores inline para editor de ChromaCore |

### Tests existentes

- **desktopTest**: 3 smoke tests (AuthScreen, ElementSelectionScreen, Navigation).
- **CI**: GitHub Actions con `desktopTest`, `assembleDebug`, `detekt`.
- **Screenshot testing**: No existe.
- **`@Preview` composables**: Solo 1 (en `GlobalFeedComposerScreen`).

---

## 2. Limitaciones del stack

### Compose Multiplatform + KMP

| Limitacion | Impacto |
|---|---|
| Compose Multiplatform renderiza via Skia en Desktop, via Android Canvas en Android, via UIKit bridge en iOS | Los screenshots NO son identicos entre plataformas — por diseno. No se puede exigir paridad pixel-a-pixel entre Android y Desktop. |
| Paparazzi (Google) no soporta KMP `commonMain` | No se puede usar Paparazzi directamente sobre composables compartidos |
| Roborazzi requiere Android Gradle Plugin y Robolectric | Funciona para el target `androidApp`, no para `shared/commonMain` directamente |
| iOS se compila via CocoaPods framework estatico | No hay directorio `iosApp/` en el repo. Screenshot testing iOS requiere Xcode + macOS |
| CI actual corre en `ubuntu-latest` | No puede ejecutar tests iOS (requiere `macos-latest`) ni Android instrumented tests (requiere emulador) |
| Solo 1 `@Preview` composable existe | No hay infraestructura de previews para capturar |

### Conclusion practica

**No es posible hacer visual regression automatizado cross-platform con una sola herramienta.**

La estrategia realista es:
1. **Desktop (JVM/Skia)**: Capturas automatizadas via `captureToImage()` en `desktopTest` — es la forma mas rapida y barata de detectar regresiones en el codigo compartido de Compose.
2. **Android**: Roborazzi en un modulo de test Android para capturas con renderizado nativo Android.
3. **iOS**: Proceso manual documentado hasta que exista `iosApp/` con Swift Snapshot Testing.
4. **Comparacion cross-platform**: Manual con diff de imagenes.

---

## 3. Herramientas recomendadas

### Tier 1 — Implementar ahora

| Herramienta | Plataforma | Razon |
|---|---|---|
| **Compose `captureToImage()` en desktopTest** | Desktop/JVM | Ya tenemos Compose UI test infra en `desktopTest`. Solo hay que anadir `captureToImage()` + comparacion con imagen de referencia. Coste cero en dependencias. |
| **ImageMagick `compare`** | CI (ubuntu) | Para diff de imagenes en CI. `apt-get install imagemagick`. |

### Tier 2 — Implementar cuando el proyecto madure

| Herramienta | Plataforma | Razon |
|---|---|---|
| **Roborazzi** (`io.github.takahirom.roborazzi`) | Android | Screenshot testing JVM sin emulador. Requiere anadir plugin + dependencias al modulo Android. |
| **Swift Snapshot Testing** (`pointfreeco/swift-snapshot-testing`) | iOS | Cuando exista `iosApp/`. Captura UIKit views renderizadas desde el framework Shared. |

### Tier 3 — Futuro

| Herramienta | Uso |
|---|---|
| **Percy / Chromatic** | Si se anade Compose for Web. |
| **Maestro** | E2E visual en dispositivo real / emulador. |
| **Firebase Test Lab** | Capturas en dispositivos reales Android (matrix testing). |

---

## 4. Inventario de pantallas

Derivado de `AppRoute.kt` y archivos `*Screen.kt`:

| # | Pantalla | Archivo | Ruta (AppRoute) |
|---|---|---|---|
| 1 | Intro / Splash | `MindOSIntroScreen.kt` | (pre-auth) |
| 2 | Auth | `AuthScreen.kt` | `Auth` |
| 3 | Element Selection | `ElementSelectionScreen.kt` | `ElementSelection` |
| 4 | Home | `HomeScreen.kt` | `Main` |
| 5 | Profile | `ProfileScreen.kt` | (dentro de Main) |
| 6 | Profile Entry | `ProfileEntryScreen.kt` | (dentro de Main) |
| 7 | Profile Cover | `ProfileCoverScreen.kt` | (dentro de Main) |
| 8 | Profile Settings | `ProfileSettingsScreen.kt` | `Settings` variant |
| 9 | Privacy Settings | `PrivacySettingsScreen.kt` | (dentro de Settings) |
| 10 | Settings | `SettingsScreen.kt` | `Settings` |
| 11 | Global Feed | `GlobalFeedScreen.kt` | `GlobalFeed` |
| 12 | Feed Composer | `GlobalFeedComposerScreen.kt` | (overlay/dialog) |
| 13 | DM List | `DMListScreen.kt` | `DMList` |
| 14 | DM Chat | `DMChatScreen.kt` | `DMChat` |
| 15 | DM Chat User Info | `WavesChatUserInfoScreen.kt` | `DMChatUserInfo` |
| 16 | Cajonera | `CajoneraScreen.kt` | `DMCajonera` |
| 17 | Username Search | `UsernameSearchScreen.kt` | (dentro de Main) |
| 18 | Notifications | `NotificationsScreen.kt` | (dentro de Main) |
| 19 | Social Graph | `SocialGraphScreen.kt` | (dentro de Profile) |
| 20 | Follow Requests | `FollowRequestsScreen.kt` | (dentro de Social) |
| 21 | Money / Wallet | `MoneyScreen.kt` | `Moneda` |
| 22 | Media Viewer | `ViewerShell.kt` / `AppMediaViewer.kt` | `MediaViewer` |

**Total: 22 pantallas.**

---

## 5. Inventario de componentes compartidos

### Componentes en `design/components/`

| Componente | Archivo | Usa tokens centralizados |
|---|---|---|
| `LoadingScreen` | `StateComponents.kt` | Si (AppColors, AppSpacing) |
| `EmptyScreen` | `StateComponents.kt` | Si |
| `ErrorScreen` | `StateComponents.kt` | Si |
| `OfflineScreen` | `StateComponents.kt` | Si |

### Componentes en `core/ui/`

| Componente | Archivo |
|---|---|
| `ElementColor` | `ElementColor.kt` |
| `FireBackGestureHandler` | `gesture/FireBackGestureHandler.kt` |
| `ShadowBackGestureDefaults` | `gesture/ShadowBackGestureDefaults.kt` |
| `UiState` | `UiState.kt` |
| `UserFacingError` | `UserFacingError.kt` |

### Componentes de feature reutilizados

| Componente | Archivo | Notas |
|---|---|---|
| SearchSharedComponents | `SearchSharedComponents.kt` | Radar UI, input, results |
| UsernameRadarMode | `UsernameRadarMode.kt` | Animacion de radar |
| ZeroUiSearchMode | `ZeroUiSearchMode.kt` | Modo de busqueda minimalista |
| ProfileVisualComponents | `ProfileVisualComponents.kt` | Avatares, badges, counters |
| ProfileEditorComponents | `ProfileEditorComponents.kt` | Inputs de edicion ChromaCore |
| ProfileInlineEditComponents | `ProfileInlineEditComponents.kt` | Edicion inline |
| ProfileStoriesCarousel3D | `ProfileStoriesCarousel3D.kt` | Carrusel 3D |
| ProfileMediaCanvas | `ProfileMediaCanvas.kt` | Canvas de media |
| ViewerComponents | `ViewerComponents.kt` | Controles del visor de media |
| ChromaCorePanel | `ChromaCorePanel.kt` | Panel de color ChromaCore |
| FeedAttachmentPicker | `FeedAttachmentPicker.kt` | Selector de adjuntos |
| OrbComponent | `OrbComponent.kt` | Orbe animado (element selection) |
| ElementalButton | `ElementalButton.kt` | Boton elemental tematico |
| SkyLoreText | `SkyLoreText.kt` | Texto animado |
| SanctuaryBackground | `SanctuaryBackground.kt` | Fondo animado |

---

## 6. Auditoria de valores magicos

### Resumen cuantitativo

```
features/ (codigo de produccion, excluyendo design/)
  Color(0xFF...)        → 205 ocurrencias en 23 archivos
  N.dp sueltos          → 790 ocurrencias en 36 archivos
  N.sp sueltos          → 109 ocurrencias en 22 archivos
  RoundedCornerShape(N) → 160 ocurrencias en 24 archivos
  ─────────────────────────────────────────────────────
  TOTAL                 → 1264 valores magicos
```

### Top 10 archivos con mas valores magicos

| Archivo | Color | dp | sp | Shape | Total |
|---|---|---|---|---|---|
| `ProfileScreen.kt` | 0 | 80 | 11 | 21 | 112 |
| `CajoneraScreen.kt` | 26 | 73 | 0 | 4 | 103 |
| `GlobalFeedScreen.kt` | 8 | 79 | 2 | 22 | 111 |
| `GlobalFeedComposerScreen.kt` | 21 | 59 | 3 | 13 | 96 |
| `ProfileEditorComponents.kt` | 25 | 37 | 0 | 11 | 73 |
| `MoneyScreen.kt` | 6 | 54 | 12 | 12 | 84 |
| `DMChatScreen.kt` | 0 | 46 | 11 | 9 | 66 |
| `ProfileCoverScreen.kt` | 9 | 32 | 3 | 11 | 55 |
| `HomeScreen.kt` | 0 | 24 | 4 | 4 | 32 |
| `DMListScreen.kt` | 3 | 31 | 9 | 1 | 44 |

### Tokens duplicados que DEBEN eliminarse

**Prioridad 1 — Duplicados exactos de AppColors:**

```
SearchVisualTokens.kt:
  NeuBlack  = Color(0xFF020504)  → AppColors.DeepBlack
  NeuWhite  = Color(0xFFF4FFF9)  → AppColors.OnBackground
  NeuGreen  = Color(0xFF13ECA4)  → AppColors.NeuGreen
  NeuMist   = Color(0xFFA7CFC1)  → AppColors.Mist
  TargetGold= Color(0xFFE9FF9B)  → AppColors.TargetGold

ViewerDesign.kt:
  Black  = Color(0xFF020504)  → AppColors.DeepBlack
  White  = Color(0xFFF4FFF9)  → AppColors.OnBackground
  Green  = Color(0xFF13ECA4)  → AppColors.NeuGreen
  Mist   = Color(0xFFA7CFC1)  → AppColors.Mist
  Accent = Color(0xFF8F5CFF)  → AppColors.NeuViolet
```

**Prioridad 2 — Paletas locales que necesitan migracion parcial:**

- `CajoneraScreen.kt` lineas 1097-1108: 12 colores de cajones fisicos. Candidatos a `AppColors.Cajonera.*` o un sub-object dedicado.
- `ProfileEditorComponents.kt`: 25+ colores de ChromaCore editor. Candidatos a `AppColors.ChromaEditor.*`.

**Excepcion documentada:**

- `MindOSIntroConfig.Palette`: 8 colores cosmicos para la animacion de intro. Son unicos a esa pantalla y no se reutilizan. Se permiten como excepcion artistica, pero deben documentarse.

---

## 7. Flujo de captura — Android

### Herramienta: Roborazzi (Tier 2)

**Cuando implementar**: Cuando se invierta en hardening visual. No es requisito para v1.

**Setup** (futuro):

```kotlin
// androidApp/build.gradle.kts
plugins {
    id("io.github.takahirom.roborazzi") version "1.x"
}

dependencies {
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.x")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.x")
    testImplementation("org.robolectric:robolectric:4.x")
}
```

**Captura**:

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileScreenRoborazziTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun profileScreen_default_state() {
        composeRule.setContent {
            MindOSTheme {
                ProfileScreen(/* fake state */)
            }
        }
        composeRule.onRoot().captureRoboImage(
            "src/test/snapshots/ProfileScreen_default.png"
        )
    }
}
```

**Verificacion**: `./gradlew :androidApp:verifyRoborazziDebug`
**Actualizacion**: `./gradlew :androidApp:recordRoborazziDebug`

### Proceso manual (ahora)

1. Compilar: `./gradlew :androidApp:installDebug`
2. Ejecutar en emulador con configuracion fija:
   - Dispositivo: Pixel 7 API 34
   - Resolucion: 1080x2400
   - Density: 420dpi
   - Tema: Dark (siempre)
   - Idioma: es-ES
   - Escala de fuente: 1.0x
   - Escala de animacion: 1.0x
3. Capturar via `adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png`
4. Nombrar: `snapshots/android/{pantalla}_{estado}_{fecha}.png`

---

## 8. Flujo de captura — Desktop (baseline comun)

### Herramienta: Compose `captureToImage()` en desktopTest

**Esta es la herramienta primaria para visual regression del codigo compartido.**

El target Desktop/JVM renderiza via Skia (el mismo motor que Compose Multiplatform usa internamente). Las capturas no son identicas a Android, pero detectan regresiones en layout, color, y composicion.

**Setup**: Ya tenemos la infra en `desktopTest`. Solo hay que anadir tests de captura.

**Patron de test**:

```kotlin
// shared/src/desktopTest/kotlin/.../visual/AuthScreenVisualTest.kt
@OptIn(ExperimentalTestApi::class)
class AuthScreenVisualTest {

    @Test
    fun authScreen_signIn_mode() = runComposeUiTest {
        setContent {
            MindOSTheme {
                AuthScreen(
                    sessionRepository = PreviewSessionRepository(isAuthenticated = false),
                    onAuthenticated = {},
                )
            }
        }

        val image = onRoot().captureToImage()
        val actual = image.toPixelMap()

        // Comparar con referencia guardada en resources
        val reference = loadReferenceImage("AuthScreen_signIn.png")
        assertVisualMatch(actual, reference, threshold = 0.005)
    }
}
```

**Utilidades necesarias** (a crear en `desktopTest`):

```kotlin
// shared/src/desktopTest/kotlin/.../visual/VisualTestUtils.kt
object VisualTestUtils {
    fun loadReferenceImage(name: String): BufferedImage { ... }
    fun saveSnapshot(image: ImageBitmap, name: String) { ... }
    fun assertVisualMatch(
        actual: PixelMap,
        reference: BufferedImage,
        threshold: Double = 0.005 // 0.5% de pixeles diferentes
    ) { ... }
    fun generateDiffImage(
        actual: BufferedImage,
        reference: BufferedImage,
    ): BufferedImage { ... }
}
```

**Directorio de referencias**: `shared/src/desktopTest/resources/snapshots/`

**Ejecucion**:
- Grabar: `./gradlew :shared:desktopTest -Dupdate.snapshots=true`
- Verificar: `./gradlew :shared:desktopTest`

**Ventajas**:
- Corre en CI sin emulador (ubuntu-latest).
- Detecta regresiones en colors, layout, spacing.
- Rapido (~2s por test).

**Limitaciones**:
- Renderizado Skia != renderizado Android Canvas.
- Fuentes pueden diferir ligeramente (sistema vs Android).
- No detecta problemas especificos de plataforma (status bar, navigation bar, notch).

---

## 9. Flujo de captura — iOS

### Estado actual

No existe directorio `iosApp/` en el repositorio. El framework `Shared` se exporta via CocoaPods para integracion futura con un proyecto Xcode.

### Cuando exista iosApp/

**Herramienta**: Swift Snapshot Testing (pointfreeco/swift-snapshot-testing)

```swift
// iosApp/iosAppTests/ProfileScreenSnapshotTest.swift
import XCTest
import SnapshotTesting
@testable import iosApp
import Shared

class ProfileScreenSnapshotTest: XCTestCase {
    func testProfileScreen_defaultState() {
        let vc = UIHostingController(
            rootView: ComposeView { /* ProfileScreen wrapper */ }
        )
        vc.view.frame = CGRect(x: 0, y: 0, width: 393, height: 852) // iPhone 15
        assertSnapshot(matching: vc, as: .image(precision: 0.995))
    }
}
```

**Dispositivo de referencia**: iPhone 15 (393x852pt, @3x)

### Proceso manual (ahora, si hay acceso a Mac)

1. Compilar framework: `./gradlew :shared:podPublishDebug` (en macOS)
2. Abrir proyecto Xcode que integre el framework
3. Ejecutar en simulador iPhone 15 (iOS 17)
4. Capturar: `xcrun simctl io booted screenshot snapshots/ios/{pantalla}_{estado}.png`
5. Nombrar consistentemente con Android

---

## 10. Flujo de comparacion

### Comparacion intra-plataforma (regression testing)

Detecta: cambios no intencionados entre commits en la MISMA plataforma.

```
referencia (commit N) vs actual (commit N+1) → mismo dispositivo/renderizador
```

**Metodo automatizado (Desktop)**:
```kotlin
fun assertVisualMatch(actual: PixelMap, reference: BufferedImage, threshold: Double) {
    var mismatchCount = 0
    val totalPixels = actual.width * actual.height

    for (y in 0 until actual.height) {
        for (x in 0 until actual.width) {
            val actualPixel = actual[x, y]
            val refPixel = Color(reference.getRGB(x, y))
            if (!colorsMatch(actualPixel, refPixel, perChannelTolerance = 3)) {
                mismatchCount++
            }
        }
    }

    val mismatchRatio = mismatchCount.toDouble() / totalPixels
    assertTrue(
        "Visual mismatch: ${(mismatchRatio * 100).format(3)}% pixels differ (threshold: ${threshold * 100}%)",
        mismatchRatio <= threshold
    )
}

fun colorsMatch(a: Color, b: Color, perChannelTolerance: Int): Boolean {
    return abs((a.red * 255).toInt() - (b.red * 255).toInt()) <= perChannelTolerance &&
           abs((a.green * 255).toInt() - (b.green * 255).toInt()) <= perChannelTolerance &&
           abs((a.blue * 255).toInt() - (b.blue * 255).toInt()) <= perChannelTolerance &&
           abs((a.alpha * 255).toInt() - (b.alpha * 255).toInt()) <= perChannelTolerance
}
```

**Metodo manual (ImageMagick)**:
```bash
# Generar diff visual
compare -metric AE -fuzz 2% \
  snapshots/android/ProfileScreen_default_baseline.png \
  snapshots/android/ProfileScreen_default_current.png \
  snapshots/android/ProfileScreen_default_diff.png

# El numero devuelto es la cantidad de pixeles diferentes
# Si > 0.5% del total, es una regresion
```

### Comparacion cross-platform (paridad visual)

Detecta: diferencias significativas entre Android, Desktop, e iOS para la misma pantalla.

**Esto NO puede automatizarse de forma fiable** porque:
- Diferentes motores de renderizado (Android Canvas vs Skia vs Core Graphics)
- Diferentes fuentes del sistema
- Diferentes densidades de pixel
- Diferentes chrome de sistema (status bar, navigation bar)

**Proceso manual**:
1. Capturar la misma pantalla en Android, Desktop e iOS
2. Recortar chrome del sistema (status bar, nav bar)
3. Escalar a la misma resolucion
4. Colocar lado a lado en un visor (ej: Beyond Compare, Kaleidoscope)
5. Verificar layout, colores, proporciones
6. Documentar diferencias aceptadas

---

## 11. Umbrales de tolerancia

### Regression testing intra-plataforma

| Nivel | Umbral (% pixeles diferentes) | Uso |
|---|---|---|
| **Estricto** | 0.1% | Componentes con layout estatico (Loading, Empty, Error, Auth) |
| **Normal** | 0.5% | Pantallas con contenido dinamico pero layout fijo (Profile, Settings, DM List) |
| **Relajado** | 2.0% | Pantallas con animaciones o contenido altamente dinamico (Intro, Radar, Cajonera) |
| **Solo layout** | 5.0% | Componentes con gradientes o sombras que varian entre ejecuciones |

### Tolerancia por canal de color

| Parametro | Valor |
|---|---|
| Per-channel tolerance (R/G/B/A) | 3/255 (~1.2%) |
| Anti-aliasing tolerance | 5/255 (~2.0%) |

### Tolerancia cross-platform

No se define un umbral numerico. La comparacion cross-platform es cualitativa:

- **Layout**: Misma jerarquia visual, mismas proporciones. Diferencias de +-2dp aceptables por redondeo de densidad.
- **Color**: Identico (mismo codigo Compose, mismo valor hex). Diferencias solo por color management del SO.
- **Tipografia**: Metricas similares pero no identicas. La fuente por defecto difiere entre plataformas.
- **Bordes y sombras**: Pueden variar. El anti-aliasing de Skia != Android Canvas != Core Graphics.

---

## 12. Diferencias nativas inevitables

Estas diferencias existen por diseno y NO son bugs:

| Diferencia | Android | iOS | Desktop |
|---|---|---|---|
| **Fuente del sistema** | Roboto | SF Pro | Varia (sistema) |
| **Renderizado de texto** | Hinting subpixel Android | Core Text | Skia text shaping |
| **Status bar** | Material You, iconos adaptables | Estilo iOS fijo | No existe |
| **Navigation bar** | Gesture bar / 3-button | Home indicator | No existe |
| **Scroll overscroll** | Glow effect (pre-12) / stretch | Bounce elastico | Sin efecto |
| **Ripple / press feedback** | Material ripple | UIKit highlight | Compose ripple |
| **Teclado** | GBoard / Android IME | iOS keyboard | No aplica |
| **Safe areas / notch** | WindowInsets | SafeAreaInsets | No aplica |
| **Antialiasing de shapes** | Android Canvas AA | Core Graphics AA | Skia AA |
| **Color space** | sRGB (mayoria) | Display P3 (iPhones recientes) | sRGB |
| **Densidad** | Variable (mdpi→xxxhdpi) | Fija por modelo (@2x, @3x) | Escalable |
| **Corner smoothing** | Estandar (circular) | Squircle (iOS continuo) | Estandar |

### Decisiones de diseno por estas diferencias

1. **No intentar replicar squircle iOS en Android.** RoundedCornerShape es correcto para ambos.
2. **No personalizar fuentes** por ahora. `FontFamily.Default` usa la fuente del sistema en cada plataforma. Aceptar la diferencia.
3. **Los screenshots de referencia son per-platform.** Nunca comparar un screenshot Android con uno Desktop como test automatizado.

---

## 13. Como revisar diffs

### En CI (automatizado)

1. El job de CI ejecuta los tests de snapshot en Desktop.
2. Si un test falla, genera una imagen diff y la sube como artefacto.
3. El revisor descarga el artefacto y examina:
   - `{TestName}_reference.png` — lo esperado
   - `{TestName}_actual.png` — lo renderizado
   - `{TestName}_diff.png` — pixeles diferentes en rojo

### En revision de PR (manual)

Checklist para el revisor:

```
[ ] Abrir los 3 archivos (reference, actual, diff)
[ ] Si el diff es solo anti-aliasing (lineas de 1px en bordes) → ACEPTAR
[ ] Si el diff muestra cambio de color → VERIFICAR que el cambio es intencional
[ ] Si el diff muestra cambio de layout → VERIFICAR que es intencional
[ ] Si el cambio es intencional → actualizar snapshot de referencia
[ ] Si el cambio NO es intencional → rechazar el PR
```

### Herramientas de visualizacion recomendadas

| Herramienta | Plataforma | Uso |
|---|---|---|
| Beyond Compare | Windows/Mac/Linux | Side-by-side con pixel zoom |
| Kaleidoscope | Mac | Diff visual nativo |
| ImageMagick `compare` | CLI | Genera diff automaticamente |
| GIMP (modo diferencia) | Cualquiera | Capa diferencia entre dos imagenes |

---

## 14. Como actualizar snapshots

### Cuando actualizar

1. **Cambio intencional de UI**: nuevo color, nuevo layout, nuevo componente.
2. **Actualizacion de Compose Multiplatform**: puede cambiar renderizado de Skia.
3. **Cambio de fuente o tema**: afecta a todos los snapshots.

### Procedimiento

```bash
# 1. Verificar que el cambio visual es correcto en la app real
#    (ejecutar en emulador / dispositivo y revisar manualmente)

# 2. Actualizar snapshots de Desktop
./gradlew :shared:desktopTest -Dupdate.snapshots=true

# 3. Verificar que los snapshots actualizados son correctos
git diff --stat shared/src/desktopTest/resources/snapshots/

# 4. Revisar cada imagen cambiada visualmente
#    (abrir en visor de imagenes, comparar con la version anterior)

# 5. Commit de los snapshots junto con el cambio de codigo
git add shared/src/desktopTest/resources/snapshots/
git commit -m "update visual snapshots for [descripcion del cambio]"
```

### Reglas

- **NUNCA** hacer `update.snapshots=true` sin haber verificado visualmente el cambio primero.
- **SIEMPRE** commitear los snapshots en el mismo PR que el cambio de UI.
- **NUNCA** actualizar snapshots en un PR que no cambia UI. Si el test falla sin cambio de UI, es una regresion real.
- En un update de Compose Multiplatform, actualizar TODOS los snapshots en un commit dedicado con mensaje explicito.

---

## 15. Integracion en CI

### Fase 1 — Ahora (Desktop snapshot tests)

Anadir al workflow existente `kotlin-ci.yml`:

```yaml
  # ────────────────────────────────────────────────────────────────────────
  # Visual regression — Desktop snapshot tests
  # ────────────────────────────────────────────────────────────────────────
  visual-regression:
    name: Shared — visual snapshots
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          persist-credentials: false
          lfs: true  # snapshots PNG pueden estar en LFS

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}

      - name: Install ImageMagick
        run: sudo apt-get install -y imagemagick

      - name: Run visual snapshot tests
        run: ./gradlew :shared:desktopTest --tests "*.visual.*" --no-daemon --stacktrace

      - name: Upload snapshot diffs on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: snapshot-diffs
          path: shared/build/reports/visual-diffs/
          retention-days: 14
```

### Fase 2 — Futura (Roborazzi Android)

```yaml
  android-visual-regression:
    name: Android — Roborazzi snapshots
    runs-on: ubuntu-latest
    timeout-minutes: 25

    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      - name: Verify Roborazzi snapshots
        run: ./gradlew :androidApp:verifyRoborazziDebug --no-daemon --stacktrace
      - name: Upload diffs on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: roborazzi-diffs
          path: androidApp/build/outputs/roborazzi/
          retention-days: 14
```

### Almacenamiento de snapshots en Git

| Opcion | Pros | Contras | Recomendacion |
|---|---|---|---|
| Git directo | Simple, diff en PR | Repo crece rapido | Si < 50 imagenes |
| Git LFS | Repo ligero, imagenes en LFS | Setup extra, coste LFS | Si > 50 imagenes |
| Artefacto externo (S3) | Repo minimo | Complejidad, latencia | Solo si necesario |

**Recomendacion para MindOS**: Git directo mientras haya menos de 50 snapshots. Migrar a Git LFS cuando se supere.

---

## 16. Snapshots obligatorios — pantallas

Pantallas que DEBEN tener snapshots automatizados porque representan flujos criticos o tienen alta complejidad visual:

| Pantalla | Prioridad | Estados a capturar |
|---|---|---|
| **AuthScreen** | P0 | `signIn_empty`, `signIn_filled`, `signUp_empty`, `signUp_error` |
| **HomeScreen** | P0 | `default`, `search_active`, `notifications_badge` |
| **ProfileScreen** | P0 | `own_profile`, `other_profile`, `edit_mode`, `blocked_user` |
| **DMListScreen** | P0 | `empty`, `with_conversations`, `unread_badge` |
| **DMChatScreen** | P0 | `empty_chat`, `with_messages`, `recording_audio` |
| **GlobalFeedScreen** | P1 | `empty`, `with_posts`, `loading` |
| **NotificationsScreen** | P1 | `empty`, `with_notifications` |
| **SettingsScreen** | P1 | `default` |
| **UsernameSearchScreen** | P1 | `idle`, `scanning`, `results` |
| **MoneyScreen** | P2 | `default`, `transaction_history` |
| **ElementSelectionScreen** | P2 | `default` |
| **CajoneraScreen** | P2 | `open`, `closed` |

**P0**: Implementar en Fase 1 (Desktop tests).
**P1**: Implementar despues de P0.
**P2**: Implementar cuando se estabilicen las pantallas.

---

## 17. Snapshots obligatorios — componentes

Componentes que DEBEN tener snapshots individuales:

| Componente | Prioridad | Variantes a capturar |
|---|---|---|
| **LoadingScreen** | P0 | `default` |
| **EmptyScreen** | P0 | `default`, `custom_message` |
| **ErrorScreen** | P0 | `retryable`, `non_retryable` |
| **OfflineScreen** | P0 | `default` |
| **ProfileVisualComponents** (avatar) | P0 | `small`, `medium`, `large`, `with_badge` |
| **ElementalButton** | P1 | Cada elemento (earth, water, fire, air, etc.) |
| **SearchSharedComponents** | P1 | `input_empty`, `input_filled`, `result_item` |
| **FeedAttachmentPicker** | P1 | `collapsed`, `expanded` |
| **ViewerComponents** (controles media) | P2 | `playing`, `paused` |
| **ChromaCorePanel** | P2 | `default`, `editing` |

---

## 18. Checklist visual por pantalla

Cada pantalla debe verificar estos criterios antes de marcarla como visualmente completa:

### Checklist generico (aplicar a todas)

```
COLORES
[ ] No contiene Color(0xFF...) literales — usa AppColors o MaterialTheme.colorScheme
[ ] Fondo principal usa AppColors.Background o MaterialTheme.colorScheme.background
[ ] Texto primario usa AppColors.OnBackground o AppColors.TextPrimary
[ ] Texto secundario usa AppColors.Mist o AppColors.TextMuted
[ ] Colores de acento usan AppColors.NeuGreen o AppColors.NeuViolet
[ ] Colores de error usan AppColors.Error
[ ] No hay colores duplicados con valores identicos a AppColors

SPACING
[ ] Padding horizontal de pantalla usa AppSpacing.ScreenHorizontal (20dp)
[ ] Gaps entre secciones usan AppSpacing.SectionGap (24dp)
[ ] Padding de cards usa AppSpacing.CardPadding (16dp)
[ ] Espaciados menores usan tokens de escala (XXS→XXXXL)
[ ] No hay dp literales que coincidan con valores de AppSpacing

SHAPES
[ ] Bordes de cards usan AppShapes.Card
[ ] Bordes de botones usan AppShapes.Button
[ ] Bordes de inputs usan AppShapes.Input
[ ] No hay RoundedCornerShape(N.dp) que coincida con valores de AppShapes

TIPOGRAFIA
[ ] Titulos usan MaterialTheme.typography.headlineMedium o titleMedium
[ ] Cuerpo usa MaterialTheme.typography.bodyLarge o bodyMedium
[ ] Labels usan MaterialTheme.typography.labelLarge
[ ] No hay TextStyle locales con fontSize/fontWeight que repliquen NeuTypography

ELEVACION
[ ] Cards usan AppElevation.Low
[ ] Dialogs usan AppElevation.High
[ ] No hay elevation literales que coincidan con AppElevation

COMPONENTES COMPARTIDOS
[ ] Estado de carga usa LoadingScreen (no CircularProgressIndicator suelto)
[ ] Estado vacio usa EmptyScreen
[ ] Estado de error usa ErrorScreen
[ ] Estado offline usa OfflineScreen

STRINGS
[ ] Acciones comunes usan AppStrings (Cancel, Confirm, Save, etc.)
[ ] Textos de accesibilidad usan AppStrings.A11y*
```

### Checklists especificos por pantalla

**AuthScreen**:
```
[ ] Logo/branding centrado
[ ] Campos email y contrasena con AppShapes.Input
[ ] Boton primario con AppColors.NeuGreen + AppColors.OnBright
[ ] Toggle sign-in/sign-up con transicion suave
[ ] Mensaje de error con AppColors.Error
```

**HomeScreen**:
```
[ ] Bottom navigation con iconos correctos
[ ] Badge de notificaciones con AppColors.Error
[ ] Transiciones entre tabs fluidas
[ ] Top bar con altura AppSpacing.TopBarHeight
```

**ProfileScreen**:
```
[ ] Avatar con tamano AppSpacing.AvatarSizeLarge
[ ] Contadores (followers, following, posts) alineados
[ ] Boton Follow/Unfollow con estados correctos
[ ] Cover area proporcional
[ ] Edit mode visualmente diferenciado
```

**DMChatScreen**:
```
[ ] Burbujas de mensaje con shapes consistentes
[ ] Input de mensaje en la parte inferior
[ ] Indicador de grabacion con AppColors.RecordingRed
[ ] Timestamps con AppColors.TextMuted
```

**GlobalFeedScreen**:
```
[ ] Cards de post con AppShapes.Card
[ ] Botones de interaccion (like, comment, share) alineados
[ ] Compositor accesible
[ ] Separadores con AppColors.Stroke
```

---

## 19. Plan de eliminacion de valores magicos

### Fase 1 — Tokens duplicados (1-2 horas)

1. Eliminar `SearchVisualTokens.kt` — reemplazar imports por `AppColors.*`
2. Eliminar colores de `ViewerDesign.kt` — reemplazar por `AppColors.*`; mover spacing/typography a `AppSpacing`/`MindOSTheme`
3. Migrar paleta de `CajoneraScreen.kt` (lineas 1097-1108) a `AppColors` como sub-tokens

### Fase 2 — Colores inline (4-8 horas)

Archivos en orden de prioridad (mayor impacto primero):

1. `ProfileEditorComponents.kt` — 25 colores → migrar a `AppColors.ChromaEditor.*`
2. `GlobalFeedComposerScreen.kt` — 21 colores
3. `CajoneraScreen.kt` — 26 colores (restantes, no la paleta)
4. `ProfileCoverScreen.kt` — 9 colores
5. `WavesChatUserInfoScreen.kt` — 9 colores
6. Resto de archivos (8+ con menos de 8 colores cada uno)

### Fase 3 — Spacing y shapes (8-16 horas)

- Reemplazar dp literales que coincidan con tokens existentes
- Anadir nuevos tokens si hay valores recurrentes no cubiertos (ej: 22.dp, 42.dp)
- Reemplazar RoundedCornerShape literales por AppShapes

### Fase 4 — Typography inline (2-4 horas)

- Reemplazar sp literales por referencias a `MaterialTheme.typography.*`
- Anadir estilos adicionales a `NeuTypography` si faltan tamanos recurrentes

**Estimacion total: 15-30 horas de trabajo.**

---

## 20. Proceso manual para revision visual

Para pantallas que no tienen snapshots automatizados o para verificacion cross-platform, usar este proceso manual estricto:

### Antes de cada release

```
DISPOSITIVOS DE REFERENCIA:
  Android: Pixel 7 emulador, API 34, 1080x2400, Dark mode, es-ES, font scale 1.0x
  iOS:     iPhone 15 simulador, iOS 17, Dark mode, es-ES
  Desktop: 1280x800 ventana, tema oscuro del sistema

PARA CADA PANTALLA DEL INVENTARIO (seccion 4):

1. CAPTURA
   [ ] Abrir pantalla en cada plataforma disponible
   [ ] Capturar estado por defecto
   [ ] Capturar estado con datos
   [ ] Capturar estado vacio
   [ ] Capturar estado de error (si aplica)
   [ ] Guardar en snapshots/{plataforma}/{pantalla}_{estado}_{fecha}.png

2. REVISION
   [ ] Layout: elementos en posicion correcta
   [ ] Colores: coinciden con tokens del design system
   [ ] Tipografia: tamanos y pesos correctos
   [ ] Spacing: margenes y paddings visualmente correctos
   [ ] Interacciones: botones, inputs responden correctamente
   [ ] Accesibilidad: contraste suficiente, touch targets >= 48dp
   [ ] Scroll: contenido largo se scrollea correctamente
   [ ] Safe areas: contenido no se oculta bajo status/nav bar

3. COMPARACION CROSS-PLATFORM (si aplica)
   [ ] Abrir side-by-side Android vs Desktop
   [ ] Verificar que layout es equivalente (no identico)
   [ ] Documentar diferencias en VISUAL_REGRESSION_LOG.md

4. APROBACION
   [ ] Firmado por: [nombre del revisor]
   [ ] Fecha: [YYYY-MM-DD]
   [ ] Pantallas aprobadas: [lista]
   [ ] Pantallas con issues: [lista + descripcion]
```

### Frecuencia

| Evento | Revision requerida |
|---|---|
| PR que toca archivos en `design/` | Todas las pantallas afectadas |
| PR que toca una pantalla | Solo esa pantalla |
| Update de Compose Multiplatform | Todas las pantallas |
| Antes de release | Todas las pantallas |
| PR que toca `MindOSTheme.kt` | Todas las pantallas |

---

## Apendice A — Estructura de directorios propuesta

```
MindOS/
  shared/
    src/
      desktopTest/
        kotlin/.../visual/           ← tests de snapshot
          AuthScreenVisualTest.kt
          HomeScreenVisualTest.kt
          ProfileScreenVisualTest.kt
          ComponentVisualTests.kt
          VisualTestUtils.kt          ← utilidades de comparacion
        resources/
          snapshots/                   ← imagenes de referencia
            AuthScreen_signIn.png
            AuthScreen_signUp.png
            HomeScreen_default.png
            ...
  snapshots/                           ← capturas manuales (no en tests)
    android/
    ios/
    desktop/
    diffs/
```

## Apendice B — Comando rapido de captura manual

```bash
#!/bin/bash
# capture-android.sh — Captura todas las pantallas en el emulador actual
DEVICE="Pixel_7_API_34"
OUT="snapshots/android"
DATE=$(date +%Y-%m-%d)

mkdir -p "$OUT"

# Listar pantallas a capturar (navegar manualmente a cada una)
SCREENS=(
  "auth_signIn"
  "home_default"
  "profile_own"
  "dmlist_default"
  "dmchat_default"
  "feed_default"
  "notifications_default"
  "settings_default"
  "search_idle"
)

for screen in "${SCREENS[@]}"; do
  echo "Navega a: $screen — Presiona ENTER cuando este listo"
  read -r
  adb shell screencap -p "/sdcard/${screen}.png"
  adb pull "/sdcard/${screen}.png" "${OUT}/${screen}_${DATE}.png"
  adb shell rm "/sdcard/${screen}.png"
  echo "Capturado: ${OUT}/${screen}_${DATE}.png"
done
```

## Apendice C — Glosario

| Termino | Definicion en este documento |
|---|---|
| **Snapshot** | Imagen PNG de referencia de una pantalla o componente en un estado especifico |
| **Diff** | Imagen que resalta los pixeles diferentes entre referencia y actual |
| **Threshold** | Porcentaje maximo de pixeles diferentes permitido antes de fallar el test |
| **Per-channel tolerance** | Diferencia maxima permitida en cada canal RGBA de un pixel individual |
| **Baseline** | Conjunto completo de snapshots de referencia para una plataforma |
| **Valores magicos** | Literales (`Color(0xFF...)`, `N.dp`, `N.sp`, `RoundedCornerShape(N)`) fuera de los archivos de tokens centralizados |
| **Token** | Valor semantico nombrado definido en `AppColors`, `AppSpacing`, `AppShapes`, etc. |
| **Regression intra-plataforma** | Cambio visual no intencionado entre commits en la misma plataforma |
| **Paridad cross-platform** | Equivalencia visual (no identidad) entre Android, iOS y Desktop |
