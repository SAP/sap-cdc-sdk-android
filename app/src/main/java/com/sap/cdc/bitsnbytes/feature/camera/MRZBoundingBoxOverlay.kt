// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.bitsnbytes.feature.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Overlay component that displays a green bounding box for MRZ scanning.
 * 
 * This creates a visual guide similar to document scanning apps, showing:
 * - A semi-transparent dark overlay with a cutout
 * - A green rounded rectangle border in the cutout area
 * - Corner brackets for enhanced visual guidance
 * 
 * @param modifier Modifier for the overlay
 * @param detectedBounds Optional detected text bounds from ML Kit (normalized 0-1)
 */
@Composable
fun MRZBoundingBoxOverlay(
    modifier: Modifier = Modifier,
    detectedBounds: com.sap.cdc.android.mrz.model.MRZDetectionInfo.NormalizedBounds? = null
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { 4.dp.toPx() }
    val cornerLength = with(density) { 32.dp.toPx() }
    val cornerRadius = with(density) { 16.dp.toPx() }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Define the scanning area (centered, landscape-oriented rectangle)
        // Typical MRZ is about 88mm wide, make the box proportionally sized
        val boxWidth = canvasWidth * 0.85f
        val boxHeight = canvasHeight * 0.35f
        val boxLeft = (canvasWidth - boxWidth) / 2f
        val boxTop = (canvasHeight - boxHeight) / 2f
        
        val scanRect = Rect(
            left = boxLeft,
            top = boxTop,
            right = boxLeft + boxWidth,
            bottom = boxTop + boxHeight
        )
        
        // Draw semi-transparent overlay with cutout
        val overlayPath = Path().apply {
            // Outer rectangle (full screen)
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            // Inner rectangle (scanning area) - subtract this to create cutout
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = scanRect,
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }
        
        drawPath(
            path = overlayPath,
            color = Color.Black.copy(alpha = 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        
        // Draw green bounding box - clean solid border
        val borderColor = Color(0xFF4CAF50) // Green color matching the reference image
        
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(scanRect.left, scanRect.top),
            size = Size(scanRect.width, scanRect.height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Draw enhanced corner brackets for visual guidance
        val cornerStrokeWidth = strokeWidth * 1.8f
        val cornerColor = borderColor.copy(alpha = 1f)
        
        // Simple L-shaped corners (cleaner than arcs)
        // Top-left
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.left, scanRect.top + cornerLength),
            end = Offset(scanRect.left, scanRect.top),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.left, scanRect.top),
            end = Offset(scanRect.left + cornerLength, scanRect.top),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Top-right
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.right - cornerLength, scanRect.top),
            end = Offset(scanRect.right, scanRect.top),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.right, scanRect.top),
            end = Offset(scanRect.right, scanRect.top + cornerLength),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Bottom-left
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.left, scanRect.bottom - cornerLength),
            end = Offset(scanRect.left, scanRect.bottom),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.left, scanRect.bottom),
            end = Offset(scanRect.left + cornerLength, scanRect.bottom),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // Bottom-right
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.right - cornerLength, scanRect.bottom),
            end = Offset(scanRect.right, scanRect.bottom),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(scanRect.right, scanRect.bottom - cornerLength),
            end = Offset(scanRect.right, scanRect.bottom),
            strokeWidth = cornerStrokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        // If ML Kit detected bounds, draw a highlight
        detectedBounds?.let { bounds ->
            // Convert normalized coordinates (0-1) to canvas coordinates
            val detectedLeft = bounds.left * canvasWidth
            val detectedTop = bounds.top * canvasHeight
            val detectedRight = bounds.right * canvasWidth
            val detectedBottom = bounds.bottom * canvasHeight
            
            // Draw dashed rectangle around detected text
            drawRoundRect(
                color = Color(0xFF8BC34A), // Lighter green for detection feedback
                topLeft = Offset(detectedLeft, detectedTop),
                size = Size(detectedRight - detectedLeft, detectedBottom - detectedTop),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }
    }
}
