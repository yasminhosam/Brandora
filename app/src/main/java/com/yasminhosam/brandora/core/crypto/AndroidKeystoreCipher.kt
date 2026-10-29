package com.yasminhosam.brandora.core.crypto

import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * AES/GCM/NoPadding with a non-exportable key held in the Android Keystore, so the session
 * blob at rest is unreadable without the device. A fresh random IV is generated per
 * encryption (required by GCM) and prefixed to the ciphertext; the 128-bit tag provides
 * integrity, so a tampered blob fails to decrypt and is treated as "no session".
 */
class AndroidKeystoreCipher : KeystoreCipher {

    override fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv                       // random 12 bytes from the Keystore
        val cipherText = cipher.doFinal(plainText)
        return iv + cipherText
    }

    override fun decrypt(payload: ByteArray): ByteArray? {
        if (payload.size <= IV_BYTES) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            // Key lost or alias gone: init throws, caught as GeneralSecurityException below.
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, payload, 0, IV_BYTES))
            cipher.doFinal(payload, IV_BYTES, payload.size - IV_BYTES)
        } catch (e: GeneralSecurityException) {
            null
        }
    }

    /** Creates the key on first use; the alias never leaves the Keystore. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "brandora_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
