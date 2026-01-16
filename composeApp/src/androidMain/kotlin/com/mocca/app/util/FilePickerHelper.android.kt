package com.mocca.app.util

import android.util.Base64

/**
 * Android implementation of base64 encoding.
 */
actual fun bytesToBase64(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
