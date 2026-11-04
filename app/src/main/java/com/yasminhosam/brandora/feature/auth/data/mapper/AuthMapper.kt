package com.yasminhosam.brandora.feature.auth.data.mapper

import com.yasminhosam.brandora.feature.auth.data.remote.SessionDto
import com.yasminhosam.brandora.feature.auth.data.remote.UserDto
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser

internal fun UserDto.toDomain(): AuthUser = AuthUser(
    id = id,
    email = email,
    name = name
)

internal fun SessionDto.toDomain(): AuthSession = AuthSession(
    user = user.toDomain(),
    accessToken = accessToken,
    refreshToken = refreshToken,
    tokenType = tokenType
)
