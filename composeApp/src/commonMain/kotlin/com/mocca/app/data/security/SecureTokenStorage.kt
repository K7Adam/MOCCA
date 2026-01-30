package com.mocca.app.data.security

/**
 * Interface for secure token storage.
 * 
 * This interface provides encryption/decryption capabilities for sensitive
 * authentication tokens. The actual implementation is platform-specific:
 * - Android: Uses Android Keystore with AES-256-GCM
 * - Other platforms: No-op implementation (returns plaintext)
 */
interface SecureTokenStorage {
    /**
     * Encrypt a plaintext token for secure storage.
     * 
     * @param plaintext The token to encrypt
     * @return Encrypted data (format depends on implementation)
     */
    fun encrypt(plaintext: String): String
    
    /**
     * Decrypt an encrypted token.
     * 
     * @param ciphertext The encrypted data
     * @return The decrypted plaintext token
     */
    fun decrypt(ciphertext: String): String
    
    /**
     * Check if a string appears to be encrypted.
     * This is a heuristic check to determine if data needs decryption.
     * 
     * @param data The string to check
     * @return true if the data appears to be encrypted
     */
    fun isEncrypted(data: String?): Boolean
    
    /**
     * Clear the stored encryption key.
     * This will invalidate all encrypted tokens.
     */
    fun clearKey()
}

/**
 * No-op implementation for platforms without secure storage.
 * This implementation simply returns plaintext without encryption.
 */
object NoOpSecureTokenStorage : SecureTokenStorage {
    override fun encrypt(plaintext: String): String = plaintext
    override fun decrypt(ciphertext: String): String = ciphertext
    override fun isEncrypted(data: String?): Boolean = false
    override fun clearKey() { /* No-op */ }
}
