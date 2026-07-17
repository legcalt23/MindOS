# Copy de onboarding y retención (claim-safe)

> Pasa por [`../CLAIMS_SAFETY.md`](../CLAIMS_SAFETY.md) antes de usar. Racional en el teardown §A5/§A7/§A10.
>
> **PROPUESTA / tarea de producto separada:** cablear esta copy a `AppStrings.kt` / al holder de onboarding es
> trabajo de RUNTIME, no de este sistema de docs. Dispara el `mindboostCopySafetyScanner` + Detekt ratchet y
> debe hacerse como fase pequeña con sus gates (`:shared:desktopTest`, `:shared:detekt`,
> `mindboostCopySafetyScanner…`). Enrutar a `mindboost-growth-retention` + skill `review-cognitive-claims`.

## Onboarding — activación = primera lectura de línea base (A5)

**Definición de "aha":** el usuario **calibra su línea base y ve su primera lectura de progreso**. Todo el
primer minuto empuja a ese único hito (regla "un objetivo por sesión").

| Momento | Copy |
|---|---|
| Primer arranque | `Antes de entrenar, calibramos tu línea base. Son unos minutos y es tu punto de partida personal — sin nota y sin compararte con nadie.` |
| Barra de progreso (goal-gradient) | `Vas por el 60% de tu calibración. Un paso más y desbloqueas tu primer perfil orientativo.` |
| Momento aha (primera lectura) | `Ya tienes tu primera medición. A partir de aquí, cada sesión mide tu progreso frente a ti mismo.` |
| Empty state del perfil (null≠0) | `Aún estás CALIBRANDO. Cuando haya datos suficientes verás tu perfil orientativo — nunca un número inventado.` |

**Checklist de primera semana (3 ítems, con celebración):** ① Calibra tu línea base · ② Completa tu primera
sesión de foco · ③ Vuelve mañana y empieza tu racha.

## Retención proactiva ligada a la métrica longitudinal (A10)

**Health-score simple (0-100)** con señales: días sin sesión, racha rota, calibración incompleta.
**Push** (permiso `POST_NOTIFICATIONS` ya declarado en el manifest):

| Disparador | Copy |
|---|---|
| Racha en riesgo (1 día sin sesión) | `Tu línea base te espera 👀 3 minutos hoy mantienen tu racha y tu progreso medible.` |
| Calibración incompleta a las 48 h | `Te falta poco para tu primera lectura. Termina tu calibración y ve tu punto de partida.` |
| Vuelta tras 7 días | `Bienvenido de vuelta. Tu progreso sigue guardado — retoma justo donde lo dejaste.` |

**Fail-closed de honestidad:** ningún aviso afirma mejoras de capacidad ni usa culpa.

## Psicología ética aplicada (A7)

Goal-gradient (barra de calibración) · efecto dotación/IKEA ("tu línea base", "tu perfil que has construido") ·
Zeigarnik (bucle abierto: "te queda 1 sesión para cerrar tu semana") · peak-end (cierre con tu mejor momento +
próximo paso) · pratfall = honestidad ("no somos magia: si no hay datos, decimos CALIBRANDO").
**Descartado:** escasez/urgencia falsa, decoy manipulador, defaults oscuros.
