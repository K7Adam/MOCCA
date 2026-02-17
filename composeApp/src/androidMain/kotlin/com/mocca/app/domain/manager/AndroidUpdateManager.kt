package com.mocca.app.domain.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Result of APK validation before installation attempt.
 */
sealed class ApkValidationResult {
    data object Valid : ApkValidationResult()
    data class Invalid(val reason: String, val userMessage: String) : ApkValidationResult()
}

/**
 * Update error types for user-friendly error reporting.
 */
sealed class UpdateErrorType {
    abstract val userMessage: String
    
    data class SignatureMismatch(
        override val userMessage: String = 
            "The downloaded update has a different signature than the installed app.\n\n" +
            "This usually happens when:\n" +
            "• Updating from a different build source (CI vs local build)\n" +
            "• The app was previously installed with a different signing key\n\n" +
            "To fix this:\n" +
            "1. Uninstall the current app version\n" +
            "2. Install the new version from the downloaded APK\n" +
            "3. Future updates from the same source will work normally"
    ) : UpdateErrorType()
    
    data class InstallPermissionDenied(
        override val userMessage: String = 
            "Permission required to install updates. Please enable 'Install unknown apps' permission for MOCCA in settings."
    ) : UpdateErrorType()
    
    data class FileNotFound(
        override val userMessage: String = "Update file not found. Please try downloading again."
    ) : UpdateErrorType()
    
    data class UnknownError(
        val error: String,
        override val userMessage: String = "Installation failed: $error"
    ) : UpdateErrorType()
}

class AndroidUpdateManager(private val context: Context) : PlatformUpdateManager {

    override suspend fun saveApk(
        fileName: String,
        data: ByteReadChannel,
        contentLength: Long?,
        onProgress: suspend (Float) -> Unit
    ): String = withContext(Dispatchers.IO) {
        // Use externalCacheDir if available, otherwise cacheDir.
        // Ensure this matches 'external-cache-path' or 'cache-path' in provider_paths.xml
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val file = File(cacheDir, "update.apk") // Fixed name to avoid path traversal issues
        
        // Delete existing file to ensure clean write
        if (file.exists()) {
            file.delete()
        }
        
        val output = FileOutputStream(file)
        val buffer = ByteArray(8 * 1024)
        var bytesCopied: Long = 0
        val total = contentLength ?: -1L

        try {
            while (true) {
                val bytesRead = data.readAvailable(buffer, 0, buffer.size)
                if (bytesRead < 0) break // EOF
                
                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                    if (total > 0) {
                        onProgress(bytesCopied.toFloat() / total)
                    }
                }
            }
            output.flush()
        } finally {
            output.close()
        }
        
        return@withContext file.absolutePath
    }

    override fun installApk(path: String) {
        installApkWithResult(path)
    }
    
    /**
     * Install APK with detailed result callback.
     * 
     * @param path Path to the APK file
     * @param onError Callback for error handling with user-friendly messages
     */
    fun installApkWithResult(
        path: String, 
        onError: ((UpdateErrorType) -> Unit)? = null
    ) {
        val file = File(path)
        if (!file.exists()) {
            Napier.e("APK file not found at $path", tag = "AndroidUpdateManager")
            onError?.invoke(UpdateErrorType.FileNotFound())
            return
        }

        // Check for REQUEST_INSTALL_PACKAGES permission
        if (!context.packageManager.canRequestPackageInstalls()) {
            Napier.w("REQUEST_INSTALL_PACKAGES permission missing. Prompting user.", tag = "AndroidUpdateManager")
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                onError?.invoke(UpdateErrorType.InstallPermissionDenied())
                return // Stop installation attempt until permission granted
            } catch (e: Exception) {
                Napier.e("Failed to launch Manage Unknown App Sources settings", e, tag = "AndroidUpdateManager")
                onError?.invoke(UpdateErrorType.UnknownError("Failed to open permission settings: ${e.message}"))
            }
        }

        // Validate APK signature before attempting installation
        val validationResult = validateApkSignature(file)
        if (validationResult is ApkValidationResult.Invalid) {
            Napier.e("APK validation failed: ${validationResult.reason}", tag = "AndroidUpdateManager")
            
            if (validationResult.reason.contains("signature", ignoreCase = true)) {
                val error = UpdateErrorType.SignatureMismatch()
                onError?.invoke(error)
                // Also throw so it can be caught by the caller
                throw ApkInstallException(error.userMessage)
            } else {
                onError?.invoke(UpdateErrorType.UnknownError(validationResult.reason))
            }
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Napier.d("Starting installation intent for $uri", tag = "AndroidUpdateManager")
            context.startActivity(intent)
        } catch (e: Exception) {
            Napier.e("Failed to start installation intent", e, "AndroidUpdateManager")
            onError?.invoke(UpdateErrorType.UnknownError(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Exception thrown when APK installation fails with a user-friendly message.
     */
    class ApkInstallException(message: String) : Exception(message)
    
    /**
     * Validates that the APK file has a compatible signature with the currently installed app.
     * This helps detect signature mismatches before attempting installation.
     * 
     * @param apkFile The APK file to validate
     * @return ApkValidationResult indicating if the APK is valid for installation
     */
    private fun validateApkSignature(apkFile: File): ApkValidationResult {
        return try {
            val pm = context.packageManager
            
            // Get the package info from the APK file
            val newPackageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            
            if (newPackageInfo == null) {
                return ApkValidationResult.Invalid(
                    reason = "Could not read package info from APK",
                    userMessage = "The downloaded file is not a valid APK."
                )
            }
            
            // Check package name matches
            val newPackageName = newPackageInfo.packageName
            val currentPackageName = context.packageName
            
            if (newPackageName != currentPackageName) {
                return ApkValidationResult.Invalid(
                    reason = "Package name mismatch: APK is $newPackageName, expected $currentPackageName",
                    userMessage = "The APK package name does not match the installed app."
                )
            }
            
            // Get signatures from the new APK
            val newSignatures = newPackageInfo.signingInfo?.apkContentsSigners
            
            // Get signatures from the currently installed app
            val currentPackageInfo = pm.getPackageInfo(currentPackageName, PackageManager.GET_SIGNING_CERTIFICATES)
            
            val currentSignatures = currentPackageInfo.signingInfo?.apkContentsSigners
            
            if (newSignatures.isNullOrEmpty() || currentSignatures.isNullOrEmpty()) {
                Napier.w("Could not compare signatures - one or both signature lists are empty", tag = "AndroidUpdateManager")
                // Allow installation attempt if we can't validate - OS will handle it
                return ApkValidationResult.Valid
            }
            
            // Compare certificate fingerprints
            val newFingerprints = newSignatures.map { getCertificateFingerprint(it.toByteArray()) }
            val currentFingerprints = currentSignatures.map { getCertificateFingerprint(it.toByteArray()) }
            
            val hasMatchingSignature = newFingerprints.any { it in currentFingerprints }
            
            if (!hasMatchingSignature) {
                Napier.e("APK signature mismatch detected!", tag = "AndroidUpdateManager")
                Napier.d("New APK fingerprints: $newFingerprints", tag = "AndroidUpdateManager")
                Napier.d("Current app fingerprints: $currentFingerprints", tag = "AndroidUpdateManager")
                
                return ApkValidationResult.Invalid(
                    reason = "APK signature does not match installed app signature",
                    userMessage = "Signature mismatch - different signing certificate"
                )
            }
            
            Napier.d("APK signature validation passed", tag = "AndroidUpdateManager")
            ApkValidationResult.Valid
            
        } catch (e: Exception) {
            Napier.w("Could not validate APK signature: ${e.message}", e, "AndroidUpdateManager")
            // If validation fails due to an error, allow the installation attempt
            // The OS will handle any actual signature mismatch
            ApkValidationResult.Valid
        }
    }
    
    /**
     * Computes SHA-256 fingerprint of a certificate for comparison.
     */
    private fun getCertificateFingerprint(certBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certBytes)
        return hash.joinToString(":") { "%02X".format(it) }
    }
}
