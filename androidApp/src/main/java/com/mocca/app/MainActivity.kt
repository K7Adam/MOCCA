package com.mocca.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.App
import com.mocca.app.ui.theme.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val configRepository: ConfigRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable true edge-to-edge with transparent bars
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppColors.background.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppColors.background.toArgb())
        )
        
        // Handle deep link on cold start
        handleIntent(intent)
        
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
