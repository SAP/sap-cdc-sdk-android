// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.bitsnbytes.feature.camera

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sap.cdc.android.mrz.MRZProcessorConfig
import com.sap.cdc.android.mrz.MRZReader
import com.sap.cdc.android.mrz.model.MRZResult
import com.sap.cdc.bitsnbytes.apptheme.AppTheme

/**
 * Test activity demonstrating MRZ scanning with MRZReader.
 * 
 * This demonstrates the simplified MRZReader approach where:
 * - The client (this app) manages CameraX setup
 * - The MRZ SDK provides MRZReader with start/stop controls
 * - Client attaches SDK's analyzer to CameraX
 * - Client designs 100% of UI in Compose
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
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // State
    var mrzResult by remember { mutableStateOf<MRZResult>(MRZResult.Scanning()) }
    var isScanning by remember { mutableStateOf(false) }
    var detectedBounds by remember { mutableStateOf<com.sap.cdc.android.mrz.model.MRZDetectionInfo.NormalizedBounds?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    
    // MRZReader reference - will be initialized below
    var mrzReader: MRZReader? = null
    
    // MRZReader - SDK provides this with start/stop controls
    mrzReader = remember {
        MRZReader.create(
            context, 
            MRZProcessorConfig(
                debugMode = true,
                stabilityFramesRequired = 2
            ),
            lifecycleOwner
        ) { result ->
            mrzResult = result
            
            // Update detected bounds for visual feedback
            detectedBounds = when (result) {
                is MRZResult.Success -> result.detectionInfo?.getNormalizedBounds()
                is MRZResult.Scanning -> result.detectionInfo?.getNormalizedBounds()
                else -> null
            }
            
            // Log success and failure
            when (result) {
                is MRZResult.Success -> {
                    Log.i("MRZCamera", "✅ MRZ SCAN SUCCESS")
                    Log.i("MRZCamera", "Name: ${result.data.fullName}")
                    Log.i("MRZCamera", "Document: ${result.data.documentNumber}")
                    Log.i("MRZCamera", "Nationality: ${result.data.nationality}")
                    Log.i("MRZCamera", "Valid: ${result.data.isValid}")
                    
                    // Stop scanning
                    isScanning = false
                    
                    // Navigate to results activity
                    val intent = Intent(context, MRZResultActivity::class.java).apply {
                        putExtra(MRZResultActivity.EXTRA_DOCUMENT_TYPE, result.data.documentType.name)
                        putExtra(MRZResultActivity.EXTRA_COUNTRY_CODE, result.data.countryCode)
                        putExtra(MRZResultActivity.EXTRA_SURNAME, result.data.surname)
                        putExtra(MRZResultActivity.EXTRA_GIVEN_NAMES, result.data.givenNames)
                        putExtra(MRZResultActivity.EXTRA_DOCUMENT_NUMBER, result.data.documentNumber)
                        putExtra(MRZResultActivity.EXTRA_NATIONALITY, result.data.nationality)
                        putExtra(MRZResultActivity.EXTRA_DATE_OF_BIRTH, result.data.dateOfBirth)
                        putExtra(MRZResultActivity.EXTRA_SEX, result.data.sex.name)
                        putExtra(MRZResultActivity.EXTRA_EXPIRATION_DATE, result.data.expirationDate)
                        putExtra(MRZResultActivity.EXTRA_PERSONAL_NUMBER, result.data.personalNumber)
                        putExtra(MRZResultActivity.EXTRA_IS_VALID, result.data.isValid)
                    }
                    context.startActivity(intent)
                }
                is MRZResult.Error -> {
                    Log.e("MRZCamera", "❌ MRZ SCAN FAILED: ${result.message}")
                }
                is MRZResult.Scanning -> {
                    // Scanning state - no logging needed
                }
            }
        }
    }
    
    // CameraX state - managed by client
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    
    // Toggle flashlight function
    fun toggleTorch() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isTorchOn = !isTorchOn
                cam.cameraControl.enableTorch(isTorchOn)
                Log.d("MRZCamera", "Torch ${if (isTorchOn) "ON" else "OFF"}")
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "MRZ Scanner",
                        style = AppTheme.typography.topBar
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colorScheme.background,
                    titleContentColor = AppTheme.colorScheme.primary
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
                    // Camera preview with MRZReader analyzer
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                
                                cameraProviderFuture.addListener({
                                    val provider = cameraProviderFuture.get()
                                    cameraProvider = provider
                                    
                                    // Setup preview
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    
                                    // Setup image analysis with MRZReader's analyzer
                                    val imageAnalyzer = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            // Use MRZReader's analyzer
                                            mrzReader?.let { reader ->
                                                analysis.setAnalyzer(
                                                    ContextCompat.getMainExecutor(ctx),
                                                    reader.getImageAnalyzer()
                                                )
                                            }
                                        }
                                    
                                    // Bind to lifecycle
                                    try {
                                        provider.unbindAll()
                                        camera = provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
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
                    
                    // MRZ Bounding Box Overlay with detected bounds - clickable to toggle torch
                    MRZBoundingBoxOverlay(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { toggleTorch() },
                        detectedBounds = detectedBounds
                    )
                    
                    // White container with button to reduce scan surface
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = AppTheme.colorScheme.background,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Start/Stop button - styled to match app theme
                            Button(
                                onClick = {
                                    mrzReader?.let { reader ->
                                        if (isScanning) {
                                            reader.stop()
                                            isScanning = false
                                        } else {
                                            reader.start()
                                            isScanning = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colorScheme.primary,
                                    contentColor = AppTheme.colorScheme.background
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = if (isScanning) "Stop Scanning" else "Start Scanning"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            mrzReader?.release()
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
