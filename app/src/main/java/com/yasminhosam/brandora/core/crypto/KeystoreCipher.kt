package com.yasminhosam.brandora.core.crypto

/**
 * Minimal authenticated-encryption seam for the session store. Kept as an interface so the
 * store can be unit-tested with a fake on the JVM (the AndroidKeystoreCipher needs a device).
 */
interface KeystoreCipher {

    /** Encrypts [plainText], returning an IV-prefixed ciphertext blob. */
    fun encrypt(plainText: ByteArray): ByteArray

    /**
     * Decrypts a blob produced by [encrypt]. Returns null when the payload cannot be
     * decrypted: Keystore key lost (device restore, OEM wipe) or the data is corrupt.
     * Callers must treat null as "no data" and clear the stored value, never crash.
     */
    fun decrypt(payload: ByteArray): ByteArray?
}
