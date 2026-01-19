package com.mocca.app.domain.provider

import android.content.Context

class AndroidAppVersionProvider(private val context: Context) : AppVersionProvider {
    override fun getVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
