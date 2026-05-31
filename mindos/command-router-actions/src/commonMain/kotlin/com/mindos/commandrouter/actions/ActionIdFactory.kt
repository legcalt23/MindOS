package com.mindos.commandrouter.actions

fun interface ActionIdFactory {
    fun create(): ActionId
}

fun interface TimestampProvider {
    fun nowMillis(): Long
}
