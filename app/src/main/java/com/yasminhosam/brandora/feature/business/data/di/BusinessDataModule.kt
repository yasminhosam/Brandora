package com.yasminhosam.brandora.feature.business.data.di

import com.yasminhosam.brandora.feature.business.data.local.DataStoreCurrentBusinessStore
import com.yasminhosam.brandora.feature.business.data.repository.BusinessRepositoryImpl
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BusinessDataModule {

    @Binds
    abstract fun bindBusinessRepository(
        implementation: BusinessRepositoryImpl
    ): BusinessRepository

    @Binds
    abstract fun bindCurrentBusinessStore(
        implementation: DataStoreCurrentBusinessStore
    ): CurrentBusinessStore
}
