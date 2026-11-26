package com.yasminhosam.brandora.feature.products.data.di

import com.yasminhosam.brandora.feature.products.data.ProductImageFiles
import com.yasminhosam.brandora.feature.products.data.page.FetchAllFilterPageSource
import com.yasminhosam.brandora.feature.products.data.repository.ProductsRepositoryImpl
import com.yasminhosam.brandora.feature.products.domain.ProductImageReader
import com.yasminhosam.brandora.feature.products.domain.ProductPageSource
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProductsDataModule {
    @Binds
    @Singleton
    abstract fun repository(implementation: ProductsRepositoryImpl): ProductsRepository

    @Binds abstract fun imageReader(implementation: ProductImageFiles): ProductImageReader

    /**
     * Client-side category filtering: the backend GET /products currently accepts only limit/offset
     * (verified against backend main on 2026-11-07). When `?categoryId=` lands server-side, rebind
     * this to ServerCategoryParamPageSource — screens stay untouched.
     */
    @Binds abstract fun pageSource(implementation: FetchAllFilterPageSource): ProductPageSource
}
