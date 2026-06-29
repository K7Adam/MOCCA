package com.mocca.app.data.repository

import com.mocca.app.data.security.SecureTokenStorage

/** No-op secure storage that returns plaintext as-is. */
internal object NoOpFakeSecureTokenStorage : SecureTokenStorage {
    override fun encrypt(plaintext: String): String = plaintext
    override fun decrypt(ciphertext: String): String = ciphertext
    override fun isEncrypted(data: String?): Boolean = false
    override fun clearKey() { /* No-op */ }
}

/** Fake secure storage that reverses the string as "encryption." */
internal class ReversingFakeSecureTokenStorage : SecureTokenStorage {
    override fun encrypt(plaintext: String): String = "ENC:" + plaintext.reversed()
    override fun decrypt(ciphertext: String): String =
        if (ciphertext.startsWith("ENC:")) ciphertext.removePrefix("ENC:").reversed() else ciphertext
    override fun isEncrypted(data: String?): Boolean = data != null && data.startsWith("ENC:")
    override fun clearKey() { /* No-op */ }
}

/** Fake that always fails decryption — simulates corrupted key. */
internal class FailingDecryptSecureTokenStorage : SecureTokenStorage {
    override fun encrypt(plaintext: String): String = "ENC:" + plaintext.reversed()
    override fun decrypt(ciphertext: String): String = throw RuntimeException("Decryption failed")
    override fun isEncrypted(data: String?): Boolean = data != null && data.startsWith("ENC:")
    override fun clearKey() { /* No-op */ }
}
