# Molvex Home Variant

Variante de pantalla inicial en Compose Multiplatform basada en la referencia adjunta.
El archivo Kotlin es autocontenido y vive en `commonMain`; no reemplaza ni conecta la
home actual.

## Integración en el proyecto

1. Copia `MolvexHomeVariant.kt` a `shared/src/commonMain/kotlin/com/mindos/app/uivariantes/`.
2. Copia las dos imágenes de `assets/` a recursos de Compose del módulo compartido.
3. Pasa los `Painter` de esos recursos al composable y envuélvelo con `MindOSTheme`.

```kotlin
MolvexHomeVariant(
    mountainPainter = mountainPainter,
    brainPainter = brainPainter,
    displayName = "Usuario",
    onStartMission = { /* conectar al flujo real de entrenamiento */ },
    onOpenProfile = { /* abrir el perfil */ },
)
```

Requiere las dependencias Compose Multiplatform que ya usa `:shared`, incluido
Material Icons Extended. Los botones, búsqueda, selector de duración y selección de
capacidad mantienen estado local de muestra; la misión solo llama al callback recibido.

## Recursos

- `assets/molvex-landscape.png`: recorte del paisaje de la imagen de referencia.
- `assets/molvex-brain.png`: recorte de la ilustración central de esa misma referencia.
