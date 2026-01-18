// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.bitsnbytes.feature.camera

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sap.cdc.android.mrz.MRZImageProcessor
import com.sap.cdc.android.mrz.MRZProcessorConfig
import com.sap.cdc.android.mrz.model.MRZResult
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import kotlinx.coroutines.launch

/**
 * Test activity demonstrating MRZ scanning with client-provided CameraX implementation.
 * 
 * This demonstrates the new interface-based approach where:
 * - The client (this app) manages its own CameraX setup
 * - The MRZ SDK provides only the processing interface (MRZImageProcessor)
 * - Client passes ImageProxy frames to the processor for analysis
 * 
 * This is the recommended integration pattern for the MRZ reader module.
 */
class CameraTestActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme {
                CameraTestScreen()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraTestScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // MRZ Processor - This is what the SDK provides
    val mrzProcessor = remember { 
        MRZImageProcessor.create(context, MRZProcessorConfig.DEBUG)
    }
    
    // State
    var mrzResult by remember { mutableStateOf<MRZResult>(MRZResult.Scanning) }
    var frameCount by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // CameraX state - managed by client
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MRZ Scanner - Interface Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera permission check
            if (!cameraPermissionState.status.isGranted) {
                PermissionRequestContent(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            } else {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Client manages their own CameraX setup
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                // Initialize CameraX - Client's responsibility
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                
                                cameraProviderFuture.addListener({
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider
                                    
                                    // Setup preview
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    
                                    // Setup image analysis - Client creates this
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(
                                                ContextCompat.getMainExecutor(ctx)
                                            ) { imageProxy ->
                                                frameCount++
                                                
                                                // Only process every 10th frame to avoid overload
                                                if (!isProcessing && frameCount % 10 == 0) {
                                                    isProcessing = true
                                                    scope.launch {
                                                        // Use SDK's processor interface
                                                        val result = mrzProcessor.processImage(imageProxy)
                                                        mrzResult = result
                                                        isProcessing = false
                                                        imageProxy.close()
                                                    }
                                                } else {
                                                    imageProxy.close()
                                                }
                                            }
                                        }
                                    
                                    // Bind to lifecycle
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    
                                    try {
                                        provider.unbindAll()
                                        camera = provider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalyzer
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // MRZ Alignment Guide Overlay
                    MRZAlignmentGuide(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // Instructions
                    MRZInstructions(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                    )
                    
                    // Result overlay
                    MRZResultOverlay(
                        result = mrzResult,
                        frameCount = frameCount,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
                
                // Controls
                MRZControls(
                    result = mrzResult,
                    onReset = { 
                        mrzResult = MRZResult.Scanning
                        frameCount = 0
                    }
                )
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mrzProcessor.release()
            cameraProvider?.unbindAll()
        }
    }
}

@Composable
fun PermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This demo requires camera access to scan MRZ data from passports and ID cards.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun MRZResultOverlay(
    result: MRZResult,
    frameCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "MRZ Scanner Status",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            when (result) {
                is MRZResult.Scanning -> {
                    Text(
                        text = "🔍 Scanning...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Point camera at MRZ area",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is MRZResult.Success -> {
                    Text(
                        text = "✅ MRZ Detected!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Name: ${result.data.fullName}", style = MaterialTheme.typography.bodySmall)
                    Text("Document: ${result.data.documentNumber}", style = MaterialTheme.typography.bodySmall)
                    Text("Nationality: ${result.data.nationality}", style = MaterialTheme.typography.bodySmall)
                    Text("Valid: ${if (result.data.isValid) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall)
                }
                is MRZResult.Error -> {
                    Text(
                        text = "❌ Error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Frames: $frameCount",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun MRZControls(
    result: MRZResult,
    onReset: () -> Unit
) {
    if (result is MRZResult.Success || result is MRZResult.Error) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan Again")
                }
            }
        }
    }
}

/**
 * MRZ Alignment Guide Overlay
 * Displays a visual guide box to help users align the document's MRZ area
 */
@Composable
fun MRZAlignmentGuide(
    modifier: Modifier = Modifier
) {
    // Guide box dimensions optimized for TD1 (ID cards) - 3 lines
    // Can be adjusted based on detected document type
    val guideWidth = 320.dp
    val guideHeight = 100.dp
    val cornerRadius = 12.dp
    
    Box(
        modifier = modifier
            .width(guideWidth)
            .height(guideHeight)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val cornerRadiusPx = cornerRadius.toPx()
            
            // Semi-transparent overlay outside the guide box
            val overlayColor = Color.Black.copy(alpha = 0.5f)
            
            // Draw the guide box with rounded corners
            val guideColor = Color.Green.copy(alpha = 0.8f)
            val strokeWidth = 4.dp.toPx()
            
            // Dashed line effect
            val dashPattern = floatArrayOf(20f, 10f)
            val pathEffect = PathEffect.dashPathEffect(dashPattern, 0f)
            
            // Draw rounded rectangle guide
            drawRoundRect(
                color = guideColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = pathEffect
                )
            )
            
            // Draw corner markers for better visibility
            val markerLength = 30f
            val markerWidth = 6f
            val markerColor = Color.Green
            
            // Top-left corner
            drawLine(
                color = markerColor,
                start = Offset(0f, cornerRadiusPx),
                end = Offset(0f, cornerRadiusPx + markerLength),
                strokeWidth = markerWidth
            )
            drawLine(
                color = markerColor,
                start = Offset(cornerRadiusPx, 0f),
                end = Offset(cornerRadiusPx + markerLength, 0f),
                strokeWidth = markerWidth
            )
            
            // Top-right corner
            drawLine(
                color = markerColor,
                start = Offset(width, cornerRadiusPx),
                end = Offset(width, cornerRadiusPx + markerLength),
                strokeWidth = markerWidth
            )
            drawLine(
                color = markerColor,
                start = Offset(width - cornerRadiusPx, 0f),
                end = Offset(width - cornerRadiusPx - markerLength, 0f),
                strokeWidth = markerWidth
            )
            
            // Bottom-left corner
            drawLine(
                color = markerColor,
                start = Offset(0f, height - cornerRadiusPx),
                end = Offset(0f, height - cornerRadiusPx - markerLength),
                strokeWidth = markerWidth
            )
            drawLine(
                color = markerColor,
                start = Offset(cornerRadiusPx, height),
                end = Offset(cornerRadiusPx + markerLength, height),
                strokeWidth = markerWidth
            )
            
            // Bottom-right corner
            drawLine(
                color = markerColor,
                start = Offset(width, height - cornerRadiusPx),
                end = Offset(width, height - cornerRadiusPx - markerLength),
                strokeWidth = markerWidth
            )
            drawLine(
                color = markerColor,
                start = Offset(width - cornerRadiusPx, height),
                end = Offset(width - cornerRadiusPx - markerLength, height),
                strokeWidth = markerWidth
            )
            
            // Draw line guides to show MRZ text lines (3 lines for TD1)
            val lineSpacing = height / 4
            val lineColor = Color.Yellow.copy(alpha = 0.7f)
            val lineStrokeWidth = 2.dp.toPx()
            
            for (i in 1..2) {
                val y = lineSpacing * i
                drawLine(
                    color = lineColor,
                    start = Offset(20f, y),
                    end = Offset(width - 20f, y),
                    strokeWidth = lineStrokeWidth
                )
            }
        }
    }
}

/**
 * Instructions overlay to guide the user
 */
@Composable
fun MRZInstructions(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Align MRZ within the guide box",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Hold document flat and steady\n• Ensure good lighting\n• Fit all 3 lines for ID cards (TD1)\n• Fit all 2 lines for passports (TD3)",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
        }
    }
}
