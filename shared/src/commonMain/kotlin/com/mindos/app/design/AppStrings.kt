package com.mindos.app.design

/**
 * Centralized UI string constants for MindOS.
 *
 * This is NOT a full i18n solution — it is a centralization layer that:
 * 1. Ensures the same text appears on iOS and Android
 * 2. Makes future i18n migration straightforward (replace constants with lookups)
 * 3. Prevents typos and inconsistencies across screens
 *
 * Rule: NO bare string literals in Compose @Composable functions.
 * Exception: strings that are purely structural (e.g. accessibility test IDs).
 */
object AppStrings {

    // ── Common actions ──────────────────────────────────────────────────
    const val Back = "Volver"
    const val Cancel = "Cancelar"
    const val Confirm = "Confirmar"
    const val Save = "Guardar"
    const val Delete = "Eliminar"
    const val Edit = "Editar"
    const val Retry = "Reintentar"
    const val Close = "Cerrar"
    const val Search = "Buscar"
    const val Send = "Enviar"
    const val Done = "Hecho"
    const val Next = "Siguiente"
    const val Loading = "Cargando..."
    const val SignOut = "Cerrar sesion"

    // ── Auth ─────────────────────────────────────────────────────────────
    const val SignIn = "Iniciar sesion"
    const val SignUp = "Crear cuenta"
    const val Email = "Correo electronico"
    const val Password = "Contrasena"
    const val DisplayNameLabel = "Nombre"
    const val AlreadyHaveAccount = "Ya tengo cuenta"
    const val CreateNewAccount = "Crear cuenta nueva"

    // ── States ──────────────────────────────────────────────────────────
    const val EmptyStateTitle = "Sin contenido"
    const val EmptyStateMessage = "No hay nada que mostrar aqui."
    const val ErrorStateTitle = "Error"
    const val OfflineTitle = "Sin conexion"
    const val OfflineMessage = "Verifica tu conexion a internet e intentalo de nuevo."

    // ── Profile ─────────────────────────────────────────────────────────
    const val ProfileTitle = "Perfil"
    const val EditProfile = "Editar perfil"
    const val Followers = "Seguidores"
    const val Following = "Siguiendo"
    const val Posts = "Publicaciones"
    const val Follow = "Seguir"
    const val Unfollow = "Dejar de seguir"
    const val Block = "Bloquear"
    const val Unblock = "Desbloquear"
    const val FollowRequestSent = "Solicitud enviada"

    // ── DM ───────────────────────────────────────────────────────────────
    const val Messages = "Mensajes"
    const val NewMessage = "Nuevo mensaje"
    const val TypeMessage = "Escribe un mensaje..."

    // ── Settings ────────────────────────────────────────────────────────
    const val Settings = "Ajustes"
    const val PrivacySettings = "Privacidad"

    // ── Feed ────────────────────────────────────────────────────────────
    const val Feed = "Feed"
    const val NewPost = "Nueva publicacion"

    // ── Photo upload ────────────────────────────────────────────────────
    /** Visible label for the blocking/inline photo upload loader. */
    const val PhotoUploadInProgress = "Subiendo foto..."

    /** Screen-reader description announced while a photo is uploading. */
    const val A11yPhotoUploadLoading = "Subiendo foto"

    // ── Personal identity lab ───────────────────────────────────────────
    const val PersonalIdentityLabTitle = "Identidad personal - Lab interno"
    const val PersonalIdentityInternalStatus =
        "Feature disponible solo para desarrollo; no esta conectada a navegacion publica."
    const val PersonalIdentitySignatureSection = "Firma"
    const val PersonalIdentityHandwritingSection = "Letra"
    const val PersonalIdentitySignatureTitle = "Firma personalizada"
    const val PersonalIdentitySignatureDescription =
        "Dibuja con dedo, raton o stylus. Los trazos se guardan como vectores normalizados."
    const val PersonalIdentitySignaturePrivacyNotice =
        "Firma visual, no legal. Solo permanece en memoria mientras este lab siga abierto."
    const val PersonalIdentityUndo = "Deshacer"
    const val PersonalIdentityClear = "Limpiar"
    const val PersonalIdentitySavedSignatures = "Firmas guardadas en esta sesion"
    const val PersonalIdentityNoSavedSignatures =
        "Todavia no hay firmas guardadas. La persistencia real queda para una fase posterior."
    const val PersonalIdentitySignatureReady =
        "Lista para una futura superposicion visual o exportacion."
    const val PersonalIdentitySignatureNamePrefix = "Firma"
    const val PersonalIdentityHandwritingTitle = "Fuente personalizada"
    const val PersonalIdentityHandwritingDescription =
        "Laboratorio de plantilla y preview. No genera una fuente TTF u OTF real todavia."
    const val PersonalIdentityPrepareTemplate = "Preparar plantilla"
    const val PersonalIdentityImportImage = "Importar imagen"
    const val PersonalIdentityPreviewInputLabel = "Texto de prueba"
    const val PersonalIdentityPreviewTitle = "Preview actual"
    const val PersonalIdentityFontPending = "Estado: modelo preparado; fuente real aun no generada."
    const val PersonalIdentityCanonicalTemplate = "Plantilla canonica"
    const val PersonalIdentityTemplateRowPrefix = "Fila"
    const val PersonalIdentityHandwritingProfileName = "Mi letra experimental"
    const val PersonalIdentityHandwritingPreview = "MindVerse con mi letra, \u00D1 y \u00F1 incluidas"

    // ── Accessibility ───────────────────────────────────────────────────
    const val A11yBackButton = "Volver atras"
    const val A11yCloseButton = "Cerrar"
    const val A11ySettingsButton = "Abrir ajustes"
    const val A11ySearchButton = "Abrir busqueda"
    const val A11yProfileImage = "Imagen de perfil"
    const val A11yLoading = "Cargando contenido"
    const val A11yError = "Ha ocurrido un error"
}
