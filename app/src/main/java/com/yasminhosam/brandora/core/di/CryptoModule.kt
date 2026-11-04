package com.yasminhosam.brandora.core.di

import com.yasminhosam.brandora.core.crypto.AndroidKeystoreCipher
import com.yasminhosam.brandora.core.crypto.KeystoreCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides @Singleton
    fun provideKeystoreCipher(): KeystoreCipher = AndroidKeystoreCipher()
}
