# SECURITY_REMEDIATION_ALTO_03_STORAGE_FLOOD.md

## Objetivo
Mitigar el riesgo de abuso y *flood* (subidas masivas) de Storage por parte de usuarios autenticados que pretendan inflar la factura de GCP o realizar un ataque de denegación de cartera (Economic Denial of Service - EDoS).

## Cambios Implementados

### 1. Cloud Functions (`enforceStorageQuotas`)
Se ha añadido un trigger en `Storage.object().onFinalize` y `onDelete` (dentro de `functions/src/index.ts`).
- **Ledger Server-side**: Se mantiene un conteo exacto de los bytes y archivos de cada usuario en `storage_usage/{uid}`.
- **Rate Limiting**: Se aplica un límite por ventana de tiempo (ej. 20 archivos por minuto).
- **Límite Absoluto**: Se impide superar los límites de almacenamiento total (ej. 500 MB) o de cantidad total de archivos (ej. 2000 archivos).
- Si el usuario viola estos límites:
  1. El archivo que provocó la violación se **borra inmediatamente**.
  2. El usuario es marcado como `isSuspended: true` en el ledger, de forma que todos los futuros archivos que intente subir serán bloqueados y purgados.
  3. (Opcional) El documento del usuario en `users/{uid}` recibe un flag `isStorageSuspended: true`.

### 2. App Check en Storage Rules
Se ha incorporado el control `isAppChecked()` a la regla `isOwner` en `storage.rules`. De esta manera, cualquier operación de creación, actualización o lectura privada requiere un *token de App Check válido*, impidiendo accesos mediante cURL, Postman o scripts bot.

```javascript
function isAppChecked() {
  return request.app != null;
}
```

### 3. Ajuste de Tipos y Tamaños (Centralización)
En `storage.rules`, ya existen funciones que validan extensiones permitidas (`.jpg`, `.mp4`, `.pdf`, etc.) y sus respectivos tamaños máximos por módulo (ej. `maxProfileImageBytes(moduleId)`).

---

## Tareas Manuales a realizar en GCP/Firebase Console

Puesto que algunas protecciones de infraestructura no pueden configurarse de forma declarativa, se DEBEN aplicar los siguientes controles manuales:

### A. Configuración de Firebase App Check
1. Accede a **Firebase Console > App Check**.
2. Registra las aplicaciones:
   - Para Android: usa **Play Integrity**.
   - Para iOS: usa **DeviceCheck** o **App Attest**.
   - Para Web: usa **reCAPTCHA Enterprise**.
3. Activa la opción de **Enforcement (Obligatoriedad)** para **Firebase Storage**.
   *(Nota: Prueba primero sin Enforcement para asegurar que tus usuarios legítimos tienen tokens válidos).*

### B. Configurar Budget Alerts y Kill-Switches
1. Accede a **GCP Console > Billing (Facturación) > Budgets & alerts**.
2. Crea un **Presupuesto (Budget)** asociado a este proyecto Firebase.
3. Establece reglas de alerta (ej. al 50%, 90% y 100% de 50€).
4. Opcionalmente, configura un **Cloud Pub/Sub** asociado a la alerta del 100% que lance una Cloud Function de Kill-Switch para desactivar el billing del proyecto temporalmente si supera el presupuesto de forma anómala en un ataque DDoS.

### C. Desplegar y Configurar
```bash
# Desplegar reglas de Storage
firebase deploy --only storage

# Desplegar Cloud Functions de mitigación
firebase deploy --only functions
```
