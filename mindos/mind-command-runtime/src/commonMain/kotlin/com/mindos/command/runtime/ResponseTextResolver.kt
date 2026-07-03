package com.mindos.command.runtime

import com.mindos.contracts.AmbiguousFailureDto
import com.mindos.contracts.NoMatchFailureDto

fun interface ResponseTextResolver {
    fun resolve(result: CommandDispatchResult, localeTag: String): ResponseText
}

class DefaultResponseTextResolver : ResponseTextResolver {
    override fun resolve(result: CommandDispatchResult, localeTag: String): ResponseText {
        val spanish = localeTag.lowercase().startsWith("es")
        val text = when (result) {
            is CommandDispatchResult.Executed ->
                if (spanish) "Accion completada." else "Action completed."
            is CommandDispatchResult.NeedsConfirmation ->
                if (spanish) "Se necesita confirmacion para continuar." else "Confirmation is required to continue."
            is CommandDispatchResult.RoutingFailed -> when (result.failure.reason) {
                is AmbiguousFailureDto ->
                    if (spanish) "El comando es ambiguo. Prueba a ser mas especifico." else
                        "The command is ambiguous. Try being more specific."
                is NoMatchFailureDto ->
                    if (spanish) "No he reconocido ese comando." else "I did not recognize that command."
                else ->
                    if (spanish) "No se puede procesar ese comando." else "That command cannot be processed."
            }
            is CommandDispatchResult.ExecutionFailed ->
                if (spanish) "No se pudo completar la accion." else "The action could not be completed."
            is CommandDispatchResult.Rejected -> when (result.reason) {
                CommandRejectionReason.RuntimeDisabled ->
                    if (spanish) "Los comandos no estan disponibles." else "Commands are not available."
                is CommandRejectionReason.SafetyDenied ->
                    if (spanish) "Ese comando fue denegado por seguridad." else
                        "That command was denied for safety."
                else ->
                    if (spanish) "El comando fue rechazado." else "The command was rejected."
            }
            is CommandDispatchResult.DryRun ->
                if (spanish) "El comando es valido, pero no se ejecuto." else
                    "The command is valid, but it was not executed."
        }
        return ResponseText(text)
    }
}
