package com.yasminhosam.brandora.core.di

import javax.inject.Qualifier

/**
 * Marks the Retrofit/OkHttp pair used for the public /auth routes (login, refresh, ...).
 * Separate from the main client on purpose: short timeouts (a hung refresh must not pin an
 * authenticator thread), and no AuthInterceptor/TokenAuthenticator, so a public 401 can
 * never trigger a refresh loop.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit
