// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.model

import android.graphics.Rect

/**
 * Information about detected MRZ text regions from ML Kit OCR.
 * 
 * This can be used to provide visual feedback about where MRZ text
 * was detected in the camera frame.
 * 
 * @property bounds Bounding box of detected text (in image coordinates)
 * @property imageWidth Width of the analyzed image
 * @property imageHeight Height of the analyzed image
 * @property confidence Optional confidence score (0.0-1.0)
 */
data class MRZDetectionInfo(
    val bounds: Rect,
    val imageWidth: Int,
    val imageHeight: Int,
    val confidence: Float? = null
) {
    /**
     * Normalized bounding box data (0.0-1.0 coordinates) for use in UI overlays.
     */
    data class NormalizedBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
    
    /**
     * Get normalized bounds (0.0-1.0 coordinates) for use in UI overlays.
     */
    fun getNormalizedBounds(): NormalizedBounds {
        return NormalizedBounds(
            left = bounds.left.toFloat() / imageWidth.toFloat(),
            top = bounds.top.toFloat() / imageHeight.toFloat(),
            right = bounds.right.toFloat() / imageWidth.toFloat(),
            bottom = bounds.bottom.toFloat() / imageHeight.toFloat()
        )
    }
}
