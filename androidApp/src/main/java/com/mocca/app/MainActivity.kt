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
import androidx.core.content.ContextCompat
import com.mocca.app.bridge.connection.BridgePairingIntentStore
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.data.repository.SharedContentBus
import com.mocca.app.domain.model.Resource
import com.mocca.app.ui.App
import com.mocca.app.ui.theme.AppColors
import io.github.aakira.napier.Napier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val configRepository: ConfigRepository by inject()
    private val bridgePairingIntentStore: BridgePairingIntentStore by inject()
    
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
        // Mocha dark background (#19120D) — hardcoded because onCreate is not composable
        val mochaDarkBg = android.graphics.Color.parseColor("#19120D")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(mochaDarkBg),
            navigationBarStyle = SystemBarStyle.dark(mochaDarkBg),
        )
        
        // Handle deep link on cold start
        handleIntent(intent)
        
        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val hasPermission = ContextCompat.checkSelfPermission(this, permission)
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
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
        // Handle share-sheet (ACTION_SEND with text)
        if (intent?.action == Intent.ACTION_SEND) {
            handleShareIntent(intent)
            return
        }

        val uri = intent?.data
        if (uri != null && uri.scheme == "mocca") {
            when (uri.host) {
                "bridge" -> handleBridgePairing(uri.toString())
                "oauth" -> handleOAuthCallback(uri)
            }
        }
    }

    private fun handleShareIntent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
        if (!sharedText.isNullOrBlank()) {
            Napier.i("[MainActivity] Received shared text (${sharedText.length} chars)")
            SharedContentBus.publish(sharedText)
            Toast.makeText(this, "Content added to chat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBridgePairing(uriString: String) {
        bridgePairingIntentStore.submit(uriString)
        Toast.makeText(this, "Connecting MOCCA CLI...", Toast.LENGTH_SHORT).show()
    }

    private fun handleOAuthCallback(uri: android.net.Uri) {
        // mocca://oauth?code=...&state=...&provider=...
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val provider = uri.getQueryParameter("provider")

        if (code != null && state != null && provider != null) {
            lifecycleScope.launch {
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
