package com.mocca.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mocca.app.ui.App
import com.mocca.app.ui.theme.TerminalColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
}
