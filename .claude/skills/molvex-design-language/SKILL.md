---
name: molvex-design-language
description: >
  The canonical Molvex / MindBoost visual language — how EVERY screen, component, and
  redesign must look so it reads as "ours", not generic AI output. Use whenever designing,
  building, redesigning, or reviewing ANY UI: a screen (pantalla), a Compose component, a
  layout, colors, typography, glow/glass, icons, charts, motion, or a mockup. Trigger on
  "design", "diseño", "UI", "pantalla", "rediseña", "componente", "que no se vea genérico",
  "que no se vea IA". Load BEFORE writing UI so output reproduces the established look
  instead of regressing to a generic mean.
---

# Molvex — Lenguaje de Diseño (contrato de reproducción)

> **Propósito.** Cuando pides una UI "desde cero", cualquier IA devuelve su **media
> estadística** (gradiente morado, Inter, cards planas, todo centrado) — eso es lo que la
> gente reconoce como "se ve hecho con IA". No es un problema de la IA; es la **ausencia de
> una restricción fuerte y persistente**. Este documento ES esa restricción. Con él, cada
> pantalla nueva hereda TU lenguaje en vez de reinventar uno genérico.
>
> **El look de Molvex ya es distintivo** (lienzo cósmico oscuro, orbes neón por-dominio,
> radar con glow, píldoras doradas, glass). El trabajo NO es inventar un estilo — es (1)
> **codificar** el que ya tienes y (2) **matar sistemáticamente** los "tells" genéricos.

## 0. Protocolo de reproducción (esto es lo que arregla el "se ve IA")

Antes de construir/rediseñar CUALQUIER pantalla, en este orden:

1. **NUNCA empieces desde cero.** Abre la pantalla existente más parecida en
   `features/**/presentation/ui/**` y la galería (`references/GALLERY.md`). Reproduce desde
   ahí; hereda su estructura, densidad y acentos.
2. **Usa SOLO tokens reales.** Todos los colores salen de
   `shared/src/commonMain/kotlin/com/mindos/app/design/AppColors.kt` (+ `MindOSTheme`).
   Prohibido `Color(0xFF…)` fuera de esos dos archivos (regla del repo, ya vigente).
3. **Aplica el ADN visual (§2)** y al menos **un "signature move" (§3)** por pantalla. Una
   pantalla sin ningún elemento firma = genérica.
4. **Pasa el kill-list anti-genérico (§4)** antes de dar nada por hecho. Es un gate, no una
   sugerencia.
5. **Verifica contra la captura de referencia** más cercana (§5). Si no se parece a la
   familia, no está lista.

## 1. Diagnóstico honesto (por qué un amigo dijo "se ve IA")

Tu núcleo es bueno y **ownable**. Lo que dispara la sensación de "IA" son **tells
concretos**, no el estilo base. Visibles en tus capturas actuales:

- **Emoji como iconos** (🎨 🎯 🟢 en el perfil) → el tell #1. Sustituir por el set de iconos
  de línea neón (§3.7).
- **Chrome de mockup** ("9:41", barra de estado) filtrándose como si fuera parte del diseño.
- **Rellenos placeholder rayados** ("foto"/"GIF") presentados como estado final.
- **Densidad sin jerarquía** (perfil): demasiados elementos del mismo peso compitiendo.
- Momentos de **gradiente/sombra genéricos** en vez de glow con color de marca.

Ninguno afecta al ADN — se arreglan con §4. El ADN (§2) es lo que hay que blindar y repetir.

## 2. ADN visual (lo que hace que un pixel sea "Molvex")

1. **Lienzo cósmico oscuro.** Fondo casi negro (`AppColors.DeepBlack #020504` /
   `Background #050505` / `ProfileCanvas #0A0A0B`), con gradiente vertical/radial sutil y,
   en pantallas hero, un **starfield** tenue y **blooms de aurora** con color
   (`FocusAuroraA` teal `#0E5E4E`, `FocusAuroraB` violeta `#1E2A66`). Nunca gris plano.
2. **Sistema de acento por-dominio.** Cada dominio cognitivo "posee" un tono; la UI se tiñe
   al dominio activo (o al color favorito del usuario vía `FavoriteColor`). Verde=éxito/marca
   (`NeuGreen #13ECA4`), teal=velocidad/foco (`FocusSignal #21F0A8`/`ComposerTeal #28F0C6`),
   violeta=abstracción/memoria (`NeuViolet #8F5CFF`/`FocusLure #7C6CFF`), cyan=edición/foco
   (`NeuCyan #00E5FF`), azul=vídeo (`ComposerBlue #2E9DFF`), oro=premium/CTA
   (`NeuGold #E7E0CF`/`TargetGold #E9FF9B`).
3. **Glass sobre oscuro.** Cards translúcidas oscuras, **borde 1px** de luz baja
   (`Stroke #252525`/`StrokeDialog #26344A`), esquinas generosas (16–24px), profundidad con
   sombra suave — NO sombra stock; glow con color cuando el elemento es activo/marca.
4. **Glow neón, no drop-shadow.** El brillo exterior con el color del acento es la textura de
   Molvex. Orbes = esfera con **gradiente radial** (highlight arriba-izq, oscuro abajo) +
   halo del propio tono.
5. **Píldoras doradas para CTA.** El oro cálido contra el frío oscuro es firma
   (`+ Añadir`, `NEU EQUIPO`, marcador `media rango`). El dorado señala "acción/premium".
6. **Data-viz premium honesto.** Radar/spider con trazo neón + relleno suave + glow; tracks
   de progreso con gradiente y **marcador de referencia dorado** ("media rango"). Ligado al
   charter de medición honesta (`null ≠ 0`, "orientativo", nunca claims clínicos/IQ).
7. **Iconografía de línea neón + la marca infinito/MB.** Trazo fino luminoso, nunca emoji.
8. **Sistema de rango/insignias.** Emblemas hexagonales, chevrons de rango, badges de
   recompensa con color por tipo.

## 3. Recetas de componentes firma (reproducir, no reinventar)

- **3.1 Cosmic canvas**: `Box` fondo `Brush.verticalGradient(DeepBlack→Background)` + capa de
  estrellas (puntos blancos low-alpha, seed estable por pantalla) + 1–2 blooms de aurora con
  `radialGradient` del acento a `alpha ~0.15`.
- **3.2 Domain orb**: círculo con `radialGradient` (highlight→sombra del tono) + halo exterior
  (`drawCircle` blur/soft del tono, alpha ~0.4). Anillos de órbita finos low-alpha entre orbes.
- **3.3 Glass card**: superficie oscura translúcida, `RoundedCornerShape(20.dp)`, `border 1.dp`
  color de acento/`Stroke` a baja alpha, padding generoso, jerarquía tipográfica clara.
- **3.4 Gold pill CTA**: `RoundedCornerShape(50)`, relleno/borde oro (`NeuGold`/`TargetGold`),
  texto oscuro (`OnBright #06130F`) o dorado; reservado a la acción primaria.
- **3.5 Neon radar**: ejes por sub-capacidad, trazo del acento con glow, relleno del polígono a
  baja alpha, grid oscuro. Etiquetas cortas.
- **3.6 Metric track**: barra con gradiente del acento + thumb + marcador vertical dorado de
  referencia ("media rango"). Valor en pts. Lenguaje honesto/orientativo.
- **3.7 Neon-outline icon**: SVG/vector de línea, trazo del acento con glow sutil. **Cero
  emoji** en UI de producto (reglas de copy del repo + este ADN).
- **3.8 Rank emblem**: hexágono con chevrons, color por rango; badges circulares con glow.

## 4. Kill-list anti-genérico (GATE — todo debe pasar)

Marca ✅ cada uno antes de entregar una pantalla:

- ☐ **Cero emoji como icono.** Todo icono es vector de línea neón.
- ☐ **Cero `Color(0xFF…)`** fuera de `AppColors.kt`/`MindOSTheme.kt`. Todo tono es un token.
- ☐ **Cero gradiente morado por defecto / Inter-Roboto por defecto / componente Material sin
  estilar.** Nada que la IA ponga "por inercia".
- ☐ **Glow con color, no drop-shadow gris.** Los elementos activos/marca brillan con su tono.
- ☐ **Al menos un signature move (§3)** presente y protagonista.
- ☐ **Jerarquía clara**: no todo pesa igual; un foco por pantalla. (Arregla la densidad tipo
  perfil.)
- ☐ **Ritmo de espaciado** de la escala `AppSpacing`, no 8px uniforme en todo.
- ☐ **Sin chrome de mockup** ("9:41"/barra de estado) ni placeholders rayados como final.
- ☐ **Fondo cósmico**, nunca gris/blanco plano (salvo light-mode aprobado con AA verificado).
- ☐ **Copy honesto/orientativo**; sin claims clínicos/IQ/transferencia lejana (charter).
- ☐ **Contraste AA** (texto ≥4.5:1) sobre el oscuro; glow no sustituye contraste de texto.

Si algún ítem falla, la pantalla **todavía se verá "IA"**. Corrige antes de cerrar.

## 5. Verificación de fidelidad

1. Pon la pantalla nueva al lado de la captura de referencia más cercana
   (`references/GALLERY.md`). ¿Se lee como la MISMA app? Si no, ajusta canvas/acento/glow/glass.
2. Ejecuta el kill-list (§4) entero.
3. Preview real (dev server / emulador) — el glow, el radar y las esferas deben verse como en
   la referencia, no como aproximaciones planas.

## 6. Cómo crece este sistema (mantenerlo vivo)

Cada vez que produzcas una pantalla que te encante, **guárdala como referencia**: añade el PNG
a `references/` y una entrada en `GALLERY.md` (qué signature moves usa, qué acento). Cuando
cambie un token o nazca un patrón firma, actualiza §2/§3 aquí. El sistema mejora cuantas más
verdades tuyas capture — no al revés.

## 7. Relación con el resto del toolkit

- **Tokens** (fuente de verdad de valores): `design/AppColors.kt`, `AppSpacing`, `AppShapes`,
  `AppElevation`, `AppAnimation`, `FavoriteColor`, `MindOSTheme`, `NeuColorScheme`.
- **Teoría de tokens** (referencia conceptual, agnóstica): `references/token-theory/` — la
  arquitectura 3-capas (primitive→semantic→component) y cómo ya vive en los tokens Kotlin del repo.
- **`ui-ux-pro-max`** (skill instalado): úsalo como **suelo** de buenas prácticas UX/a11y/stack
  (Compose) — reglas genéricas correctas. Este documento es el **techo** de identidad Molvex.
  Orden: primero reproduce el ADN Molvex (§2–§3), luego valida UX con `ui-ux-pro-max`.
- **Data-viz** (radar, metric-tracks, cualquier chart): carga la skill **`dataviz`** (bundled,
  ya disponible) para paleta categórica/sequential/diverging + legends/tooltips accesibles, y
  APLICA encima el ADN Molvex (§2.6): trazo neón del acento + glow + marcador dorado "media
  rango". `dataviz` = corrección técnica del chart; §2.6 = que se vea Molvex.
- **Agentes**: `motion-experience` (movimiento/feel), `compose-ui-dev` (implementación Compose),
  `frontend-design` (calidad de front). Todos deben cargar este skill antes de diseñar.

> **Portabilidad.** Este mismo documento sirve pegado como brief de estilo en cualquier
> herramienta IA (Claude.ai, v0, Lovable…): copia §0, §2, §3 y §4 al inicio del prompt y exige
> "reproduce este lenguaje, no diseñes desde cero". Fuera del repo, sustituye los tokens por sus
> valores hex (§2 los lista).
