package com.mocca.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography
import io.github.aakira.napier.Napier
import java.util.concurrent.Executors

/**
 * QR code scanner composable with CameraX and ML Kit.
 * 
 * Features:
 * - Camera preview with QR detection overlay
 * - Permission handling
 * - Animated scanning indicator
 * - Success/error callbacks
 */
@Composable
fun QrCodeScanner(
    onQrCodeDetected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var isScanning by remember { mutableStateOf(true) }
    var detectedCode by remember { mutableStateOf<String?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            !hasCameraPermission -> {
                // Permission denied state
                PermissionDeniedView(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onDismiss = onDismiss
                )
            }
            
            detectedCode != null -> {
                // Success state
                QrSuccessView(
                    code = detectedCode!!,
                    onConfirm = {
                        onQrCodeDetected(detectedCode!!)
                    },
                    onScanAgain = {
                        detectedCode = null
                        isScanning = true
                    },
                    onDismiss = onDismiss
                )
            }
            
            else -> {
                // Scanning state
                QrScannerView(
                    onQrCodeDetected = { code ->
                        detectedCode = code
                        isScanning = false
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun QrScannerView(
    onQrCodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .apply {
                            // setTargetResolution is deprecated but setResolutionSelector requires
                            // ResolutionSelector with AspectRatioStrategy which is more complex
                            // Using @Suppress for simplicity while maintaining functionality
                            @Suppress("DEPRECATION")
                            setTargetResolution(Size(1280, 720))
                        }
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { code ->
                                                Napier.d("QR Code detected: $code")
                                                onQrCodeDetected(code)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Napier.e("QR scanning failed", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Napier.e("Camera binding failed", e)
                    }
                    
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Scanning overlay
        ScanningOverlay()
        
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.screenPaddingHorizontal)
                .padding(top = AppSpacing.screenPaddingTop)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AppColors.white
                )
            }
        }
    }
}

@Composable
private fun ScanningOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Scanner frame
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(AppShapes.card)
                .border(
                    width = 2.dp,
                    color = AppColors.accent,
                    shape = AppShapes.card
                )
        ) {
            // Corner indicators
            ScanningCorners()
            
            // Animated scanning line
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ScanningLine()
            }
        }
        
        // Instructions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = AppColors.white,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Text(
                text = "Point camera at QR code",
                color = AppColors.white,
                style = AppTypography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "on your OpenCode terminal",
                color = AppColors.textSecondary,
                style = AppTypography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanningCorners() {
    // Draw corner brackets
    Box(modifier = Modifier.fillMaxSize()) {
        // Top-left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(24.dp)
                .padding(4.dp)
                .border(
                    width = 3.dp,
                    color = AppColors.accent,
                    shape = AppShapes.extraSmall
                )
        )
        // Top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .padding(4.dp)
                .border(
                    width = 3.dp,
                    color = AppColors.accent,
                    shape = AppShapes.extraSmall
                )
        )
        // Bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(24.dp)
                .padding(4.dp)
                .border(
                    width = 3.dp,
                    color = AppColors.accent,
                    shape = AppShapes.extraSmall
                )
        )
        // Bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .padding(4.dp)
                .border(
                    width = 3.dp,
                    color = AppColors.accent,
                    shape = AppShapes.extraSmall
                )
        )
    }
}

@Composable
private fun ScanningLine() {
    // Animated horizontal scanning line
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(AppColors.accent)
    )
}

@Composable
private fun PermissionDeniedView(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(AppSpacing.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppSpacing.xxl * 2))
        
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = AppColors.textSecondary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        Text(
            text = "Camera permission required",
            color = AppColors.white,
            style = AppTypography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        Text(
            text = "QR code scanning needs camera access to connect to your OpenCode server",
            color = AppColors.textSecondary,
            style = AppTypography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        // Grant permission button
        Box(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(AppColors.accent)
                .clickable(onClick = onRequestPermission)
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
        ) {
            Text(
                text = "Grant Permission",
                color = AppColors.background,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // Cancel button
        Text(
            text = "Cancel",
            color = AppColors.textSecondary,
            style = AppTypography.bodyMedium,
            modifier = Modifier.clickable(onClick = onDismiss)
        )
    }
}

@Composable
private fun QrSuccessView(
    code: String,
    onConfirm: () -> Unit,
    onScanAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .padding(AppSpacing.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppSpacing.xxl * 2))
        
        // Success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(AppColors.statusOnline),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = AppColors.background,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        Text(
            text = "QR Code Detected!",
            color = AppColors.white,
            style = AppTypography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Show truncated code
        val displayCode = if (code.length > 50) code.take(50) + "..." else code
        Text(
            text = displayCode,
            color = AppColors.accent,
            style = AppTypography.bodySmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.xxl))
        
        // Connect button
        Box(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(AppColors.accent)
                .clickable(onClick = onConfirm)
                .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md)
        ) {
            Text(
                text = "Connect",
                color = AppColors.background,
                style = AppTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // Scan again
        Text(
            text = "Scan Again",
            color = AppColors.textSecondary,
            style = AppTypography.bodyMedium,
            modifier = Modifier.clickable(onClick = onScanAgain)
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.md))
        
        // Cancel
        Text(
            text = "Cancel",
            color = AppColors.textSecondary,
            style = AppTypography.bodySmall,
            modifier = Modifier.clickable(onClick = onDismiss)
        )
    }
}