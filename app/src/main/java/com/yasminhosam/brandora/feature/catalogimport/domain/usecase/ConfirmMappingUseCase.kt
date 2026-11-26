package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.normalizeMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.validateMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

class ConfirmMappingUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    fun validate(columns: List<MappedColumn>): MappingValidationResult =
        validateMapping(normalizeMapping(columns))

    suspend operator fun invoke(importId: String, columns: List<MappedColumn>): ApiResult<Unit> {
        // Trim custom keys once: the normalized list both gates the save and reaches the server.
        val normalized = normalizeMapping(columns)
        if (importId.isBlank() || validate(normalized) !is MappingValidationResult.Valid) {
            return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        }
        return repository.confirmMapping(importId, normalized).map { }
    }
}
