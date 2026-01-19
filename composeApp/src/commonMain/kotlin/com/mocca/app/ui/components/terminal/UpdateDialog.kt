package com.mocca.app.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocca.app.domain.model.UpdateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    progress: Float,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .border(1.dp, Color(0xFF00FF00), RoundedCornerShape(0.dp))
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = ">> UPDATE_DETECTED [${updateInfo.version}]",
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Release Notes
            Text(
                text = updateInfo.releaseNotes,
                color = Color(0xFFCCCCCC),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Size
            Text(
                text = "SIZE: ${updateInfo.size / 1024 / 1024} MB",
                color = Color(0xFF888888),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isDownloading) {
                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DOWNLOADING... [${(progress * 100).toInt()}%]",
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF333333))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color(0xFF00FF00))
                        )
                    }
                }
            } else {
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TerminalButton(
                        text = "LATER",
                        onClick = onDismiss,
                        backgroundColor = Color(0xFF333333),
                        textColor = Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TerminalButton(
                        text = "INSTALL_NOW",
                        onClick = onUpdate,
                        backgroundColor = Color(0xFF00FF00),
                        textColor = Color.Black
                    )
                }
            }
        }
    }
}
