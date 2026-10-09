package com.yasminhosam.brandora.core.di

import com.yasminhosam.brandora.BuildConfig
import com.yasminhosam.brandora.core.network.ApiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


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
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        // Keep this level at BASIC or lower: BODY would dump whole Excel files into logcat.
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        //   .addInterceptor(AuthInterceptor(sessionStore))
        // Fail fast while debugging: short timeouts surface an unreachable backend in seconds.
        // Release keeps patient values for slow networks and large multipart uploads.
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(if (BuildConfig.DEBUG) 30L else 120L, TimeUnit.SECONDS)
        .readTimeout(if (BuildConfig.DEBUG) 30L else 120L, TimeUnit.SECONDS)
        .build()


    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}