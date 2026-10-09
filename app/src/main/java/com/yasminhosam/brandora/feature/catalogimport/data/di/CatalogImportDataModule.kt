package com.yasminhosam.brandora.feature.catalogimport.data.di

import com.yasminhosam.brandora.feature.catalogimport.data.file.UriExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.data.repository.ExcelImportRepositoryImpl
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Only public types are bound here: an internal type in a Hilt provider signature breaks
 * Dagger's generated callers through Kotlin name mangling, so the Retrofit service is built
 * inside the data source instead.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogImportDataModule {

    @Binds
    abstract fun bindExcelImportRepository(
        implementation: ExcelImportRepositoryImpl
    ): ExcelImportRepository

    @Binds
    abstract fun bindExcelFileFactory(
        implementation: UriExcelFileFactory
    ): ExcelFileFactory
}
