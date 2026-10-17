package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.validateMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

class ConfirmMappingUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    fun validate(columns: List<MappedColumn>): MappingValidationResult = validateMapping(columns)

    suspend operator fun invoke(importId: String, columns: List<MappedColumn>): ApiResult<Unit> {
        if (importId.isBlank() || validate(columns) !is MappingValidationResult.Valid) {
            return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        }
        return repository.confirmMapping(importId, columns).map { }
    }
}
