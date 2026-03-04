package com.mocca.app.ui.screens.onboarding

import com.mocca.app.api.NetworkConfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.DiscoveredServer
import com.mocca.app.ui.components.modern.MoccaButton
import com.mocca.app.ui.components.modern.MoccaInput
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@Composable
internal fun SelectServerStep(
    servers: List<DiscoveredServer>,
    selectedServer: DiscoveredServer?,
    error: String?,
    onServerSelected: (DiscoveredServer) -> Unit,
    onScanQr: () -> Unit,
    onManualConnect: (host: String, port: Int, username: String, password: String, useHttps: Boolean) -> Unit,
    onRetry: () -> Unit
) {
    var showManualEntry by remember { mutableStateOf(true) } // Expanded by default for dev
    // TEMPORARY PREFILLS
    var manualHost by remember { mutableStateOf(NetworkConfig.DEFAULT_HOST_IP) }
    var manualPort by remember { mutableStateOf(NetworkConfig.OPENCODE_SERVER_PORT.toString()) }
    var manualUsername by remember { mutableStateOf(NetworkConfig.DEFAULT_USERNAME) }
    var manualPassword by remember { mutableStateOf(NetworkConfig.DEFAULT_PASSWORD) }
    var useHttps by remember { mutableStateOf(false) }
    
    // Removed: Auto-detect Tailscale (.ts.net) and set HTTPS + port 443
    // Tailscale encrypts its overlay network, so HTTP on 4242 works safely without TLS.
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Server",
            style = AppTypography.headlineSmall,
            color = AppColors.white
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        
        Text(
            text = "Choose an OpenCode server to connect",
            style = AppTypography.bodyMedium,
            color = AppColors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        if (servers.isNotEmpty()) {
            // Server list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                modifier = Modifier.weight(1f)
            ) {
                items(servers, key = { it.baseUrl }) { server ->
                    ServerListItem(
                        server = server,
                        isSelected = server.baseUrl == selectedServer?.baseUrl,
                        onClick = { onServerSelected(server) }
                    )
                }
            }
        } else {
            // No servers found
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    
                    Text(
                        text = "No servers found",
                        style = AppTypography.bodyMedium,
                        color = AppColors.textSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    
                    Text(
                        text = "Scan again",
                        style = AppTypography.bodyMedium,
                        color = AppColors.accent,
                        modifier = Modifier.clickable(onClick = onRetry)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // QR scan button
        MoccaButton(
            text = "Scan QR Code",
            onClick = onScanQr,
            icon = Icons.Default.QrCodeScanner,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Manual entry toggle
        if (!showManualEntry) {
            Text(
                text = "Enter server manually",
                style = AppTypography.bodyMedium,
                color = AppColors.textSecondary,
                modifier = Modifier
                    .clickable { showManualEntry = true }
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            // Manual entry form — Host / Port / Username / Password
            MoccaInput(
                value = manualHost,
                onValueChange = { manualHost = it },
                label = "Host",
                placeholder = NetworkConfig.DEFAULT_HOST_IP
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualPort,
                onValueChange = { manualPort = it },
                label = "Port",
                placeholder = "4242"
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualUsername,
                onValueChange = { manualUsername = it },
                label = "Username",
                placeholder = NetworkConfig.DEFAULT_USERNAME
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaInput(
                value = manualPassword,
                onValueChange = { manualPassword = it },
                label = "Password",
                placeholder = "Leave empty if none",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            // HTTPS toggle (auto-detected for Tailscale)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use HTTPS",
                    style = AppTypography.bodyMedium,
                    color = AppColors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useHttps,
                    onCheckedChange = { 
                        useHttps = it
                        if (it && manualPort == "4242") {
                            manualPort = "443"
                        } else if (!it && manualPort == "443") {
                            manualPort = "4242"
                        }
                    }
                )
            }
            
            // Show effective URL preview
            val effectiveProtocol = if (useHttps) "https" else "http"
            val effectivePort = manualPort.toIntOrNull() ?: 4242
            Text(
                text = "$effectiveProtocol://${manualHost.trim()}:$effectivePort",
                style = AppTypography.bodySmall,
                color = AppColors.accent,
                modifier = Modifier.padding(vertical = AppSpacing.sm)
            )
            
            Spacer(modifier = Modifier.height(AppSpacing.md))
            
            MoccaButton(
                text = "Connect",
                onClick = {
                    onManualConnect(
                        manualHost.trim(),
                        manualPort.trim().toIntOrNull() ?: 4242,
                        manualUsername.trim().ifBlank { NetworkConfig.DEFAULT_USERNAME },
                        manualPassword,
                        useHttps
                    )
                },
                enabled = manualHost.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
