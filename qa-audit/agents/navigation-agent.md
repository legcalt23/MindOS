# navigation-agent

## AGENT_RESULT

agent_id: `navigation-agent`
scope: `Estatico: glob de *Screen.kt (37) + modelo de navegacion de CLAUDE.md (AppRoute/AppRouteStack)`
status: `partial` (estatico; sin ejecutar la app)
confidence: `70`
files_examined_count: `glob de 37 pantallas` (+ contexto navegación de CLAUDE.md)
commands_run_count: `0`
evidence_files:
- `qa-audit/function-inventory.md`

### Modelo de navegación (de CLAUDE.md, sin lib externa)

- `AppRoute` (sealed) + `AppRouteStack` (respaldado por `SnapshotStateList`: push/pop/replace/replaceAll/popTo/rebindStackWithUser).
- `PlatformBackHandler` expect/actual: Android = `BackHandler`; **desktop/iOS = no-op**.
- `AppRouteRebinder.rebindWithUser` (14 tests), `AppRouteTelemetry.telemetryName` (PII-free).
- 37 pantallas en 11 áreas (auth, intro, home, social, dm/waves, search, wallet, profile, privacy, element_selection, mindboost focus/memory/speed).

### findings

- id: `NAV-001`
  title: `Back físico es no-op en desktop/iOS — pantallas profundas dependen de botón en pantalla`
  severity: `P3`
  category: `navigation`
  target: `desktop`
  reproducibility: `not_reproduced` (verificar en build desktop)
  user_impact: `en desktop, si una pantalla profunda no pinta back visible, el usuario queda atrapado`
  technical_impact: `PlatformBackHandler.desktop = no-op por diseño`
  recommended_next_action: `ui-agent: verificar que TODA pantalla pusheada tiene back affordance visible en desktop`

- id: `NAV-002`
  title: `Añadir un AppRoute exige actualizar rebinder + telemetryName + stack (acoplamiento latente)`
  severity: `P4`
  category: `navigation`
  target: `common`
  reproducibility: `always`
  technical_impact: `una ruta nueva sin actualizar los 3 sitios = rebinding incompleto o telemetría vacía`
  recommended_next_action: `usar when exhaustivo (sin else) sobre AppRoute para que el compilador fuerce los 3 updates`

- id: `NAV-003`
  title: `Verificar que no quede presencia "isOnline" fabricada en ninguna ruta`
  severity: `P3`
  category: `ui`
  target: `common`
  reproducibility: `not_reproduced`
  technical_impact: `CLAUDE.md corrigió isOnline=true fabricado; confirmar que default false se mantiene en todas las entradas`
  recommended_next_action: `grep de isOnline = true en features/dm + App.kt durante Lote B (state/ui)`

blocked_by:
- `Ejecución real diferida: deep links, botones sin acción, pantallas muertas y bucles requieren build + navegación en device/desktop (presupuesto + Lote C).`

summary_for_brain: |
  Navegación propia (AppRouteStack), sin lib. 37 pantallas/11 áreas inventariadas. 3 hipótesis estáticas:
  back no-op en desktop (P3, verificar affordance), acoplamiento al añadir rutas (P4), residual isOnline
  fabricado (P3, grep en Lote B). Mapa de navegación dinámico (deep links, dead ends) requiere ejecución
  real — diferido a Lote C con device.
