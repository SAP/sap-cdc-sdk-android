// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.recognition

import android.graphics.Rect

/**
 * Represents a single text line that could be part of an MRZ.
 * 
 * @property text Normalized MRZ text (uppercase, no spaces)
 * @property bounds Bounding box in image coordinates
 * @property score Quality score for this candidate
 */
internal data class MRZLineCandidate(
    val text: String,
    val bounds: Rect,
    val score: Double
)

/**
 * Represents a group of 2 or 3 MRZ lines that form a complete MRZ.
 * 
 * @property lines The MRZ line candidates in this group
 * @property bounds Union of all line bounds
 * @property score Overall quality score for this group
 */
internal data class MRZGroup(
    val lines: List<MRZLineCandidate>,
    val bounds: Rect,
    val score: Double = 0.0
)

/**
 * Result of evaluating a candidate MRZ group.
 * 
 * @property group The original MRZ group
 * @property format Detected MRZ format
 * @property rawLines The normalized MRZ lines
 * @property finalScore Combined score (group score + validation)
 */
internal data class EvaluatedCandidate(
    val group: MRZGroup,
    val format: com.sap.cdc.android.mrz.model.MRZFormat,
    val rawLines: List<String>,
    val finalScore: Double
)