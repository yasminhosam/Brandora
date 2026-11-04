package com.yasminhosam.brandora.feature.auth.data.local

import android.content.SharedPreferences
import com.yasminhosam.brandora.core.crypto.KeystoreCipher
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The session is stored as one encrypted blob (Base64 of Keystore ciphertext) in a private
 * SharedPreferences file that is excluded from backups. The in-memory [StateFlow] mirrors
 * the disk state and is loaded synchronously in init, so an interceptor can read
 * [session].value the moment the first request goes out.
 *
 * A blob that no longer decrypts (Keystore key lost after a device restore, or tampering)
 * is treated as "no session" and removed - never a crash.
 */
class EncryptedSessionStore @Inject constructor(
    private val preferences: SharedPreferences,
    private val cipher: KeystoreCipher,
    private val json: Json
) : SessionStore {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    init {
        _session.value = load()
    }

    override suspend fun save(session: AuthSession) {
        val encoded = Base64.getEncoder().encodeToString(
            cipher.encrypt(json.encodeToString(AuthSession.serializer(), session).toByteArray())
        )
        // commit(), not apply(): the tokens must be durable before the caller navigates or
        // retries a request, and the blob is a few hundred bytes.
        preferences.edit().putString(KEY_SESSION, encoded).commit()
        _session.value = session
    }

    override suspend fun clear() {
        preferences.edit().remove(KEY_SESSION).commit()
        _session.value = null
    }

    private fun load(): AuthSession? {
        val encoded = preferences.getString(KEY_SESSION, null) ?: return null
        val decrypted = try {
            cipher.decrypt(Base64.getDecoder().decode(encoded))
        } catch (e: IllegalArgumentException) {
            null // not valid Base64: corrupt storage
        } ?: run {
            removeCorruptedEntry()
            return null
        }
        return try {
            json.decodeFromString(AuthSession.serializer(), String(decrypted))
        } catch (e: SerializationException) {
            removeCorruptedEntry()
            null
        }
    }

    private fun removeCorruptedEntry() {
        preferences.edit().remove(KEY_SESSION).commit()
    }

    private companion object {
        const val KEY_SESSION = "session_blob"
    }
}
