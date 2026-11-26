package com.yasminhosam.brandora.feature.business.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Top-level delegate so every injection shares one DataStore instance per process.
private val Context.businessDataStore: DataStore<Preferences> by preferencesDataStore(name = "business")

class DataStoreCurrentBusinessStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrentBusinessStore {

    override val businessId: Flow<String?> = context.businessDataStore.data
        .map { it[KEY_BUSINESS_ID] }

    override suspend fun setBusinessId(id: String, ownerId: String?) {
        context.businessDataStore.edit {
            it[KEY_BUSINESS_ID] = id
            if (ownerId != null) it[KEY_BUSINESS_OWNER_ID] = ownerId
            else it.remove(KEY_BUSINESS_OWNER_ID)
        }
    }

    override suspend fun clearUnlessOwnedBy(userId: String) {
        context.businessDataStore.edit { prefs ->
            // No business, or one created by someone else (a legacy entry with no recorded
            // owner counts as someone else's): drop it so no account inherits another's.
            if (prefs[KEY_BUSINESS_ID] != null && prefs[KEY_BUSINESS_OWNER_ID] != userId) {
                prefs.remove(KEY_BUSINESS_ID)
                prefs.remove(KEY_BUSINESS_OWNER_ID)
            }
        }
    }

    private companion object {
        val KEY_BUSINESS_ID = stringPreferencesKey("business_id")
        val KEY_BUSINESS_OWNER_ID = stringPreferencesKey("business_owner_id")
    }
}
