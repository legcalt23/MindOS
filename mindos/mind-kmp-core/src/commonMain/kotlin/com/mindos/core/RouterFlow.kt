package com.mindos.core

import com.mindos.contracts.KmpCommandRouter
import com.mindos.contracts.RouterRequestDto
import com.mindos.contracts.RouterResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun KmpCommandRouter.routeAll(requests: Flow<RouterRequestDto>): Flow<RouterResultDto> =
    requests.map { route(it) }
