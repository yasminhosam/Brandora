package com.yasminhosam.brandora.core.di

import com.yasminhosam.brandora.BuildConfig
import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.AuthInterceptor
import com.yasminhosam.brandora.core.network.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true   // backend can add fields without crashing the app
        coerceInputValues = true   // unknown enum value -> default instead of crash
        explicitNulls = false
    }

    @Provides @Singleton
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        // Auth first: the header must be on the request before it goes out, and the
        // authenticator handles 401s on the way back.
        .addInterceptor(authInterceptor)
        // Keep this level at BASIC or lower: BODY would dump whole Excel files into logcat.
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        // Fail fast while debugging: short timeouts surface an unreachable backend in seconds.
        // Release keeps patient values for slow networks and large multipart uploads.
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(if (BuildConfig.DEBUG) 30L else 120L, TimeUnit.SECONDS)
        .readTimeout(if (BuildConfig.DEBUG) 30L else 120L, TimeUnit.SECONDS)
        .authenticator(tokenAuthenticator)
        .build()

    /**
     * Dedicated client for the public /auth routes (login, refresh): short timeouts in both
     * build types, because a hung refresh would otherwise pin an OkHttp authenticator
     * thread. Deliberately has NO AuthInterceptor/TokenAuthenticator: these routes are
     * public, and a 401 from them must never trigger a refresh loop.
     */
    @Provides @Singleton
    @AuthRetrofit
    fun provideAuthOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(@AuthRetrofit client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
