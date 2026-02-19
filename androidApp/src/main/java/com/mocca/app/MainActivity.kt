package com.mocca.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.App
import com.mocca.app.ui.theme.AppColors
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val configRepository: ConfigRepository by inject()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Napier.i("[MainActivity] Notification permission granted")
        } else {
            Napier.w("[MainActivity] Notification permission denied. Active sessions may not be visible in background.")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable true edge-to-edge with transparent bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppColors.background.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppColors.background.toArgb())
        )
        
        // Handle deep link on cold start
        handleIntent(intent)
        
        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        setContent {
            // Surface fills entire screen (edge-to-edge) with theme background
            Surface(modifier = Modifier.fillMaxSize(), color = AppColors.background) {
                // Content handles its own insets (status bars, ime, etc.)
                App()
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "mocca" && uri.host == "oauth") {
            // mocca://oauth?code=...&state=...&provider=...
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            val provider = uri.getQueryParameter("provider")
            
            if (code != null && state != null && provider != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@MainActivity, "Completing login...", Toast.LENGTH_SHORT).show()
                    val result = configRepository.completeOAuthFlow(provider, code, state)
                    when (result) {
                        is Resource.Success -> {
                            Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Error -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Login Failed: ${result.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
