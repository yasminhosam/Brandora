package com.yasminhosam.brandora.feature.business.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The business the whole catalog/import flow belongs to. Emits the persisted id once known;
 * null before the first creation. The stored id is the source of truth - no startup GET.
 */
interface CurrentBusinessStore {
    val businessId: Flow<String?>
    suspend fun setBusinessId(id: String)
}
