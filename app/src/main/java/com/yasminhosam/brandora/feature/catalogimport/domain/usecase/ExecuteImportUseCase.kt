package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteOutcome
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

/**
 * Runs the import and applies the backend's recovery contract. Execute is synchronous, so a
 * conflict, a 500 or a timeout does NOT prove nothing was imported — instead of re-POSTing we
 * re-read the persisted job status. Only a true 404 is surfaced as an error.
 */
class ExecuteImportUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    suspend operator fun invoke(importId: String): ApiResult<ExecuteOutcome> {
        if (importId.isBlank()) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return when (val result = repository.execute(importId)) {
            is ApiResult.Success -> ApiResult.Success(ExecuteOutcome.Performed(result.data))
            is ApiResult.Error -> recover(importId, result)
        }
    }

    private suspend fun recover(importId: String, error: ApiResult.Error): ApiResult<ExecuteOutcome> =
        if (error.errorCode in RECOVERABLE_CODES || error.type in RECOVERABLE_TYPES) {
            when (val status = repository.getImportStatus(importId)) {
                is ApiResult.Success -> ApiResult.Success(ExecuteOutcome.Recovered(status.data))
                is ApiResult.Error -> status
            }
        } else {
            error
        }

    private companion object {
        val RECOVERABLE_CODES = setOf(
            ImportErrorCodes.IMPORT_NOT_READY,
            ImportErrorCodes.IMPORT_EXECUTION_ALREADY_STARTED,
            ImportErrorCodes.IMPORT_EXECUTION_FAILED
        )

        // A timeout or dropped connection still may have committed rows server-side.
        val RECOVERABLE_TYPES = setOf(ApiErrorType.TIMEOUT, ApiErrorType.NETWORK)
    }
}
