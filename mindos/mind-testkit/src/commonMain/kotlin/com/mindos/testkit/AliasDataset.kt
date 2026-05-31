package com.mindos.testkit

/**
 * ES/EN alias dataset stored separately from command patterns. Aliases map a localized phrase to an
 * intent that needs no entity (settings, messages/notifications), so each resolves standalone and is
 * testable end-to-end. Localized parametric verbs (e.g. "perfil {user}") are a registry concern, not
 * standalone aliases.
 */
object AliasDataset {

    val rows: List<AliasRow> = listOf(
        // open_settings
        AliasRow("settings", "en", "open_settings"),
        AliasRow("preferences", "en", "open_settings"),
        AliasRow("open settings", "en", "open_settings"),
        AliasRow("ajustes", "es", "open_settings"),
        AliasRow("configuracion", "es", "open_settings"),
        AliasRow("preferencias", "es", "open_settings"),
        // open_messages (a.k.a. notifications / chats inbox)
        AliasRow("messages", "en", "open_messages"),
        AliasRow("inbox", "en", "open_messages"),
        AliasRow("notifications", "en", "open_messages"),
        AliasRow("chats", "en", "open_messages"),
        AliasRow("mensajes", "es", "open_messages"),
        AliasRow("bandeja", "es", "open_messages"),
        AliasRow("notificaciones", "es", "open_messages"),
    )

    /** Aliases for a single locale. */
    fun forLocale(locale: String): List<AliasRow> = rows.filter { it.locale == locale }
}
