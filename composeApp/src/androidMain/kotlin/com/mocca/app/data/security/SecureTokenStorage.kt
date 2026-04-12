package com.mocca.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.aakira.napier.Napier
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of SecureTokenStorage using Android Keystore.
 * 
 * This class provides hardware-backed encryption for sensitive authentication tokens,
 * ensuring they are protected even if the device is compromised.
 * 
 * SECURITY FEATURES:
 * - Uses Android Keystore (hardware-backed when available)
 * - AES-256 encryption with GCM mode
 * - Random IV for each encryption operation
 * - Base64 encoding for database storage
 * - Automatic key generation and management
 */
class SecureTokenStorageImpl(context: Context) : SecureTokenStorage {
    
    companion object {
        private const val TAG = "SecureTokenStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "mocca_auth_token_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val AES_KEY_SIZE = 256

        /** Prefix added to all encrypted values for reliable identification. */
        const val ENCRYPTION_PREFIX = "ENC:"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    init {
        // Generate key if it doesn't exist
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }
    
    /**
     * Generate a new AES key in the Android Keystore.
     * The key is hardware-backed when the device supports it.
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            
            Napier.i("$TAG: Generated new AES-$AES_KEY_SIZE key in Android Keystore")
        } catch (e: Exception) {
            Napier.e("$TAG: Failed to generate key", e)
            throw e
        }
    }
    
    /**
     * Get the secret key from the Keystore.
     */
    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Secret key not found in Keystore")
    }
    
    /**
     * Encrypt a plaintext token for secure storage.
     * 
     * @param plaintext The token to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext + auth tag)
     */
    override fun encrypt(plaintext: String): String {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV + ciphertext
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            
            // Base64 encode with prefix for reliable identification
            ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            Napier.e("$TAG: Encryption failed", e)
            throw e
        }
    }
    
    /**
     * Decrypt an encrypted token.
     * 
     * Accepts both the new ENC:-prefixed format and legacy raw encrypted payloads.
     *
     * @param ciphertext Base64-encoded encrypted data (IV + ciphertext + auth tag)
     * @return The decrypted plaintext token
     */
    override fun decrypt(ciphertext: String): String {
        return try {
            // Strip encryption prefix if present
            val payload = if (ciphertext.startsWith(ENCRYPTION_PREFIX)) {
                ciphertext.substring(ENCRYPTION_PREFIX.length)
            } else {
                ciphertext
            }
            
            val combined = Base64.getDecoder().decode(payload)
            
            // Extract IV and ciphertext
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val plaintext = cipher.doFinal(encrypted)
            String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            Napier.e("$TAG: Decryption failed", e)
            throw e
        }
    }
    
    /**
     * Check if a string was encrypted by this implementation.
     * Uses the ENC: prefix for deterministic identification.
     */
    override fun isEncrypted(data: String?): Boolean {
        if (data.isNullOrEmpty()) return false
        return data.startsWith(ENCRYPTION_PREFIX)
    }
    
    /**
     * Clear the stored key (for logout/reset scenarios).
     * This will invalidate all encrypted tokens.
     */
    override fun clearKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
                Napier.i("$TAG: Deleted encryption key from Keystore")
            }
            // Generate new key for future use
            generateKey()
        } catch (e: Exception) {
            Napier.e("$TAG: Failed to clear key", e)
        }
    }
}
