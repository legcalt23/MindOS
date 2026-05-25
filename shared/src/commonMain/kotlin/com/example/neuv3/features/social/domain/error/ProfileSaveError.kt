package com.example.neuv3.features.social.domain.error

import com.example.neuv3.features.social.domain.valueobject.UserId

sealed interface ProfileSaveError : DomainError {
    val attemptId: String
    val phase: String
    val operation: String
    val userId: UserId?
    val path: String?
    val causeMessage: String?
}

data class ProfileSaveNotAuthenticated(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val path: String? = null,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val userId: UserId? = null
    override val code: String = "profile_save_not_authenticated"
    override val message: String =
        "Tu sesion no esta lista. Vuelve a iniciar sesion antes de guardar."
}

data class ProfileSaveAppCheckFailed(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_app_check_failed"
    override val message: String =
        "Firebase no pudo verificar esta instalacion antes de guardar. Revisa App Check en pruebas o Play Integrity en builds firmadas."
}

data class ProfileSavePermissionDenied(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_permission_denied"
    override val message: String =
        "No tienes permisos para guardar este perfil. Comprueba la cuenta activa."
}

data class ProfileSaveNetworkUnavailable(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_network_unavailable"
    override val message: String =
        "No se pudo conectar para guardar. Revisa la red e intentalo de nuevo."
}

data class ProfileSaveTimeout(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_timeout"
    override val message: String =
        "El guardado ha tardado demasiado. Revisa la conexion e intentalo otra vez."
}

data class ProfileSaveValidationError(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String? = null,
    override val causeMessage: String?,
) : ProfileSaveError {
    override val code: String = "profile_save_validation_error"
    override val message: String =
        causeMessage ?: "Hay datos del perfil que no se pueden guardar."
}

data class ProfileSavePartialRemoteFailure(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    val completedOperations: List<String>,
    val failedOperation: String,
    val failedCode: String,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_partial_remote_failure"
    override val message: String =
        "No se pudo completar el guardado. Los cambios no se confirmaron y se intento limpiar lo subido."
}

data class ProfileSaveUnknownRemoteFailure(
    override val attemptId: String,
    override val phase: String,
    override val operation: String,
    override val userId: UserId?,
    override val path: String?,
    override val causeMessage: String? = null,
) : ProfileSaveError {
    override val code: String = "profile_save_unknown_remote_failure"
    override val message: String =
        "No se pudo guardar el perfil por un error remoto inesperado."
}

class ProfileSaveException(
    val saveError: ProfileSaveError,
    cause: Throwable? = null,
) : RuntimeException(saveError.message, cause)
