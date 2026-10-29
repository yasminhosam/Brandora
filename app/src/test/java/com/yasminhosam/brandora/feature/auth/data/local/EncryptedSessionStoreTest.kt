package com.yasminhosam.brandora.feature.auth.data.local

import android.content.SharedPreferences
import com.yasminhosam.brandora.core.crypto.KeystoreCipher
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The store under test with a fake KeystoreCipher and an in-memory SharedPreferences, so
 * the encryption-persistence rules run on the JVM. The fake cipher is trivially reversible
 * but always produces a distinct ciphertext, which is enough to assert "not stored in
 * plaintext" and "corrupt/undecryptable blob == no session".
 */
class EncryptedSessionStoreTest {

    private class FakeCipher(var failDecrypt: Boolean = false) : KeystoreCipher {
        override fun encrypt(plainText: ByteArray): ByteArray =
            Base64.getEncoder().encode("enc:".toByteArray() + plainText)

        override fun decrypt(payload: ByteArray): ByteArray? {
            if (failDecrypt) return null
            val decoded = Base64.getDecoder().decode(payload)
            val prefix = "enc:".toByteArray()
            return decoded.takeIf { it.size >= prefix.size && it.copyOfRange(0, prefix.size).contentEquals(prefix) }
                ?.copyOfRange(prefix.size, decoded.size)
        }
    }

    private class FakePreferences : SharedPreferences {
        val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearFirst = false

            override fun putString(key: String?, value: String?) = apply { pending[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) =
                apply { pending[key!!] = values }
            override fun putInt(key: String?, value: Int) = apply { pending[key!!] = value }
            override fun putLong(key: String?, value: Long) = apply { pending[key!!] = value }
            override fun putFloat(key: String?, value: Float) = apply { pending[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { pending[key!!] = value }
            override fun remove(key: String?) = apply { pending[key!!] = null }
            override fun clear() = apply { clearFirst = true }
            override fun commit(): Boolean = applyChanges()
            override fun apply() { applyChanges() }

            private fun applyChanges(): Boolean {
                if (clearFirst) values.clear()
                pending.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                return true
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val cipher = FakeCipher()

    private fun store(prefs: FakePreferences = FakePreferences()) =
        EncryptedSessionStore(prefs, cipher, json)

    @Test
    fun `a saved session survives a store restart and is never plaintext on disk`() = runBlocking {
        val prefs = FakePreferences()
        store(prefs).save(SESSION)

        // "Restart": a fresh store instance over the same preferences file.
        val restored = store(prefs).session.value

        assertEquals(SESSION, restored)
        val raw = prefs.values["session_blob"] as String
        assertTrue("token stored in plaintext!", !raw.contains(SESSION.accessToken))
        assertTrue(!raw.contains(SESSION.refreshToken))
    }

    @Test
    fun `an empty file means no session`() {
        assertNull(store().session.value)
    }

    @Test
    fun `an undecryptable blob is treated as no session and removed`() {
        cipher.failDecrypt = true
        val prefs = FakePreferences().apply { values["session_blob"] = "aW52YWxpZA===" }

        val restored = store(prefs)

        assertNull(restored.session.value)
        assertTrue(prefs.values.isEmpty())
        cipher.failDecrypt = false
    }

    @Test
    fun `a corrupt blob that decrypts to non-JSON is removed`() {
        val prefs = FakePreferences().apply {
            values["session_blob"] =
                Base64.getEncoder().encodeToString(cipher.encrypt("not json at all".toByteArray()))
        }

        val restored = store(prefs)

        assertNull(restored.session.value)
        assertTrue(prefs.values.isEmpty())
    }

    @Test
    fun `clear removes the blob and empties the in-memory state`() = runBlocking {
        val prefs = FakePreferences()
        val store = store(prefs)
        store.save(SESSION)

        store.clear()

        assertNull(store.session.value)
        assertTrue(prefs.values.isEmpty())
    }

    @Test
    fun `clear on an empty store is safe`() = runBlocking {
        val store = store()
        store.clear()
        assertNull(store.session.value)
    }

    private companion object {
        val SESSION = AuthSession(
            user = AuthUser(id = "u1", email = "a@b.c", name = "A"),
            accessToken = "access-token-abc",
            refreshToken = "refresh-token-xyz"
        )
    }
}
