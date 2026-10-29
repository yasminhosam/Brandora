package com.yasminhosam.brandora.core.network

import okhttp3.HttpUrl

/**
 * The public /auth routes: they never carry an Authorization header, and a 401 from them
 * (e.g. wrong credentials on login) must never trigger a token refresh.
 */
internal val PUBLIC_AUTH_PATHS: Set<String> = setOf(
    ApiEndpoints.AUTH_REGISTER,
    ApiEndpoints.AUTH_LOGIN,
    ApiEndpoints.AUTH_REFRESH,
    ApiEndpoints.AUTH_LOGOUT,
    ApiEndpoints.AUTH_FORGOT_PASSWORD,
    ApiEndpoints.AUTH_RESET_PASSWORD
)

/**
 * suffix-match so a base URL with a path prefix (e.g. https://host/api/) still matches;
 * the backend contract has no other route ending in these segments.
 */
internal fun HttpUrl.isPublicAuthPath(): Boolean =
    PUBLIC_AUTH_PATHS.any { encodedPath.endsWith(it) }
