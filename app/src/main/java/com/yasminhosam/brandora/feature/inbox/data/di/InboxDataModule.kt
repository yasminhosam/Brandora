package com.yasminhosam.brandora.feature.inbox.data.di

import com.yasminhosam.brandora.feature.inbox.data.repository.InboxRepositoryImpl
import com.yasminhosam.brandora.feature.inbox.domain.repository.InboxRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InboxDataModule {

    @Binds
    abstract fun bindInboxRepository(
        implementation: InboxRepositoryImpl
    ): InboxRepository
}
