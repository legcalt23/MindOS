# Example output (aterrizado en el repo real)

Este es el shape esperado de la respuesta de `/ceo`: no genérico, con módulos, tareas y gates
reales de MindOS. El artefacto principal es el bloque `Prompt perfecto para Claude Code`.

````md
# CEO Brief

## Diagnóstico
El usuario no pide "hacer bonito" el onboarding. Pide convertir el primer minuto en una demostración
seria de valor: baseline, personalización y una primera sensación de progreso HONESTA. El onboarding
debe terminar en la primera micro-sesión, no en una pantalla informativa, y debe respetar el hub
existente (`MindBoostHubScreen`) y el rigor charter (nada de progreso fabricado en el minuto 1: si no
hay datos, la UI dice "—", no un 0).

## Decisiones CEO aplicadas
- El onboarding termina iniciando una primera micro-sesión real, no en storytelling.
- Se prioriza baseline + plan diario sobre copy motivacional; el "progreso" del minuto 1 es honesto
  (CALIBRATING/"—", nunca un score inventado).
- Cero claims médicos/IQ (debe pasar `mindboostCopySafetyScanner`). Lógica en `commonMain`, UI Compose
  MP; verificación con los gates reales del repo.

## Prompt perfecto para Claude Code

```text
Actúa como CEO de MindBoost, Principal Product Engineer y Staff Kotlin Multiplatform Engineer.

Contexto de producto: MindBoost es el entrenador cognitivo (Android + iOS, KMP + Compose MP) dentro
de Mindverse. No es un juego casual: es un sistema medible de progreso — baseline, entrenamiento
adaptativo, feedback, progresión y hábito.

Contexto del repo (REAL): módulos :shared (commonMain/androidMain/desktopMain/iosMain; tests en
commonTest y desktopTest), :androidApp, :desktopApp; paquete com.mindos.app; features MindBoost en
shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/**; hub en
presentation/ui/MindBoostHubScreen.kt. Lógica y tests corren en el target desktop/JVM. iOS no compila
en Windows (verificación diferida).

Objetivo: que el usuario, en su primer minuto, complete una calibración de baseline mínima, entienda
su plan diario y llegue a una primera micro-sesión con fricción mínima — sintiendo un sistema que
mide y adapta, sin ninguna promesa clínica.

Antes de implementar:
1. Lee CLAUDE.md (incl. "Aprendizajes acumulados"), y el feature de onboarding/hub actual en
   shared/src/commonMain/kotlin/com/mindos/app/features/** (localiza la ruta real con Grep).
2. Resume hallazgos y plantea un plan por FASES PEQUEÑAS (dominio → estado → UI → tests).

Restricciones (rigor charter): score honesto null≠0 — en el minuto 1, cualquier dimensión sin datos
suficientes se muestra como CALIBRATING/"—", nunca 0. No cablees el perfil salvo por un ScoreResult
sellado con sus gates. Sin claims médicos/IQ (debe pasar mindboostCopySafetyScanner).

Restricciones técnicas: lógica compartida en commonMain; expect/actual solo para APIs de plataforma;
respeta estado/navegación/persistencia/DI existentes; Detekt es ratchet one-way (no regeneres el
baseline). Persistencia sensible vía SecureKeyValueStore cifrado, no Room.

Implementación requerida (por fases):
- Fase 1: modelo de dominio del flujo de onboarding + calibración baseline en commonMain, con tests
  input→output en desktopTest.
- Fase 2: state holder del onboarding (transiciones puras, testeables).
- Fase 3: UI Compose MP que termine iniciando la primera micro-sesión; copy sin claims.

Criterios de aceptación:
- Onboarding + baseline completables en <90s; primera micro-sesión arranca con fricción mínima.
- Ninguna dimensión muestra 0 fabricado: sin datos ⇒ "—".
- El copy pasa mindboostCopySafetyScanner.

Verificación (gates reales):
- :shared:desktopTest y :shared:detekt en verde; :androidApp:compileDebugKotlin si tocas androidMain.
- Si Android Studio posee el daemon Gradle en el worktree, NO ejecutes Gradle por CLI: verifica
  estáticamente y ENTRÉGAME los comandos de gate exactos.

Entrega final: resumen de archivos del working-tree (sin ofrecer commit/PR), decisiones, tradeoffs,
riesgos, y si corregiste un error real, la entrada en "Aprendizajes acumulados del proyecto" de
CLAUDE.md. Termina con el siguiente paso para reforzar el MVP.
```

## Criterios de éxito
- El usuario completa onboarding + baseline en <90s y entra a la primera micro-sesión.
- El "progreso" del minuto 1 es honesto (CALIBRATING/"—"), nunca un score inventado.
- El copy comunica entrenamiento y progreso sin claims médicos (pasa el scanner de copy).
- La implementación respeta la arquitectura KMP y los gates reales quedan verdes.

## Riesgos que el prompt evita
- Onboarding decorativo sin utilidad ni primera sesión.
- Progreso fabricado en el minuto 1 (0 en vez de "—") — viola el rigor charter.
- Claims exagerados de mejora mental / IQ (rotura del copy scanner).
- Lógica Android/iOS duplicada en vez de compartida en commonMain.
````
