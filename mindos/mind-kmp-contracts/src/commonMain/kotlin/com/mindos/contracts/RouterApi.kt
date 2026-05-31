package com.mindos.contracts

interface KmpCommandRouter {
    suspend fun route(request: RouterRequestDto): RouterResultDto
}
