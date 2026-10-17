package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.initialColumns
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

class GetMappingUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    suspend operator fun invoke(importId: String): ApiResult<List<MappedColumn>> {
        if (importId.isBlank()) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return repository.getMapping(importId).map { initialColumns(it.columns, it.savedMapping) }
    }
}
