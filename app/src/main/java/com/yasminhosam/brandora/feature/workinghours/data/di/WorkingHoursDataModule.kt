package com.yasminhosam.brandora.feature.workinghours.data.di

import com.yasminhosam.brandora.feature.workinghours.data.repository.WorkingHoursRepositoryImpl
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingHoursRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkingHoursDataModule {

    @Binds
    abstract fun bindWorkingHoursRepository(
        implementation: WorkingHoursRepositoryImpl
    ): WorkingHoursRepository
}