package com.example.mindos.features.social.presentation.media

sealed class MediaError {
    data object NetworkUnavailable : MediaError()
    data object NetworkTimeout : MediaError()
    data object NotFound : MediaError()
    data object PermissionDenied : MediaError()
    data object UnsupportedFormat : MediaError()
    data object CorruptFile : MediaError()
    data object PlaybackError : MediaError()
    data object DecodingImageError : MediaError()
    data object RenderingPdfError : MediaError()
    data object InsufficientMemory : MediaError()
    data object ConnectionError : MediaError()
    data object InvalidUrl : MediaError()
    data class Unknown(val message: String? = null) : MediaError()

    fun getUiMessage(): String = when (this) {
        NetworkUnavailable -> "Sin conexion a internet"
        NetworkTimeout -> "Tiempo de espera agotado"
        NotFound -> "El archivo no esta disponible"
        PermissionDenied -> "Acceso denegado al archivo"
        UnsupportedFormat -> "Formato de archivo no compatible"
        CorruptFile -> "El archivo parece estar danado"
        PlaybackError -> "Error durante la reproduccion"
        DecodingImageError -> "No se pudo procesar la imagen"
        RenderingPdfError -> "Error al renderizar el documento PDF"
        InsufficientMemory -> "Memoria insuficiente para cargar el medio"
        ConnectionError -> "Error de conexion con el servidor"
        InvalidUrl -> "La direccion del archivo no es valida"
        is Unknown -> message ?: "Ha ocurrido un error inesperado"
    }

    fun getActionSuggestion(): String = when (this) {
        NetworkUnavailable, NetworkTimeout, ConnectionError -> "Revisa tu conexion e intentalo de nuevo"
        NotFound, InvalidUrl -> "El recurso podria haber sido eliminado o la URL es incorrecta"
        PermissionDenied -> "Verifica los permisos de tu cuenta"
        UnsupportedFormat -> "Usa un formato estandar"
        CorruptFile, DecodingImageError, RenderingPdfError -> "El archivo original podria ser ilegible"
        PlaybackError -> "Prueba a reiniciar la reproduccion"
        InsufficientMemory -> "Cierra otras aplicaciones para liberar memoria"
        is Unknown -> "Contacta con soporte si el problema persiste"
    }
}
