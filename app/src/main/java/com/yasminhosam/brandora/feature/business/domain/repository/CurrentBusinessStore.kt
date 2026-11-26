package com.yasminhosam.brandora.feature.business.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The business the whole catalog/import flow belongs to. Emits the persisted id once known;
 * null before the first creation. The stored id is the source of truth - no startup GET.
 *
 * The id is stored together with the user that created it: the next login keeps the business
 * only when the same user returns ([clearUnlessOwnedBy]), so a session expiry never orphans
 * the same user's catalog and a different account never inherits it.
 */
interface CurrentBusinessStore {
    val businessId: Flow<String?>

    /** Persists the business and the user that created it; [ownerId] is null only when unknown. */
    suspend fun setBusinessId(id: String, ownerId: String?)

    /**
     * Clears the stored business unless it was created by [userId]. A legacy entry with no
     * recorded owner counts as "not theirs" and is dropped, so no account inherits another's.
     */
    suspend fun clearUnlessOwnedBy(userId: String)
}
