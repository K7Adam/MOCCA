package com.mocca.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.App
import com.mocca.app.ui.theme.TerminalColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val configRepository: ConfigRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link on cold start
        handleIntent(intent)
        
        setContent {
            // Surface fills entire screen (edge-to-edge) with theme background
            Surface(modifier = Modifier.fillMaxSize(), color = TerminalColors.background) {
                // Box with safeDrawingPadding ensures content doesn't overlap
                // with status bar, navigation bar, or display cutouts
                Box(modifier = Modifier.safeDrawingPadding()) {
                    App()
                }
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
