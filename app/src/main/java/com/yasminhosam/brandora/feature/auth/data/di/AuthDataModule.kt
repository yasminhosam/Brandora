package com.yasminhosam.brandora.feature.auth.data.di

import android.content.Context
import android.content.SharedPreferences
import com.yasminhosam.brandora.feature.auth.data.local.EncryptedSessionStore
import com.yasminhosam.brandora.feature.auth.data.repository.AuthRepositoryImpl
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthDataModule {

    @Binds
    @Singleton
    abstract fun bindSessionStore(implementation: EncryptedSessionStore): SessionStore

    @Binds
    abstract fun bindAuthRepository(implementation: AuthRepositoryImpl): AuthRepository

    companion object {

        /** Own file so backup rules can exclude it; tokens must never leave the device. */
        @Provides @Singleton
        fun provideAuthPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences(AUTH_PREFERENCES_FILE, Context.MODE_PRIVATE)
    }
}

// Referenced from backup_rules.xml / data_extraction_rules.xml - keep in sync.
internal const val AUTH_PREFERENCES_FILE = "auth_session"
