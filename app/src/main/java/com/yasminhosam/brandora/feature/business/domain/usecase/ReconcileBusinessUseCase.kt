package com.yasminhosam.brandora.feature.business.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface BusinessSyncResult {
    data object HasBusiness : BusinessSyncResult
    data object NoBusiness : BusinessSyncResult
    data class Error(val error: ApiResult.Error) : BusinessSyncResult
}

/**
 * Syncs owned businesses with the server on startup or post-login/register:
 * - Server returns businesses: if stored ID is among them, keep it; else save the first one. Returns [BusinessSyncResult.HasBusiness].
 * - Server returns empty list: confirms no business exists. Returns [BusinessSyncResult.NoBusiness].
 * - Server error / offline: if stored ID exists, use it ([BusinessSyncResult.HasBusiness]); else return [BusinessSyncResult.Error].
 */
class ReconcileBusinessUseCase @Inject constructor(
    private val repository: BusinessRepository,
    private val currentBusinessStore: CurrentBusinessStore
) {
    suspend operator fun invoke(userId: String): BusinessSyncResult {
        val storedId = currentBusinessStore.businessId.first()
        return when (val result = repository.getBusinesses()) {
            is ApiResult.Success -> {
                val businesses = result.data
                if (businesses.isNotEmpty()) {
                    val matchingId = businesses.firstOrNull { it.id == storedId }?.id ?: businesses.first().id
                    currentBusinessStore.setBusinessId(matchingId, userId)
                    BusinessSyncResult.HasBusiness
                } else {
                    BusinessSyncResult.NoBusiness
                }
            }
            is ApiResult.Error -> {
                if (storedId != null) {
                    BusinessSyncResult.HasBusiness
                } else {
                    BusinessSyncResult.Error(result)
                }
            }
        }
    }
}
