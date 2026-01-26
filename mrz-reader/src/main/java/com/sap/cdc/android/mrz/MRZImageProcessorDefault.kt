// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import com.sap.cdc.android.mrz.model.MRZFormat
import com.sap.cdc.android.mrz.model.MRZResult
import com.sap.cdc.android.mrz.parser.MRZParserFactory
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.recognition.EvaluatedCandidate
import com.sap.cdc.android.mrz.recognition.MRZGroup
import com.sap.cdc.android.mrz.recognition.MRZLineClusteringSimplified
import com.sap.cdc.android.mrz.recognition.MRZTextRecognizer

/**
 * Default MRZ image processor implementation with improved detection and validation.
 * 
 * Implements the advanced processing logic:
 * 1. Extract MRZ line candidates with scoring
 * 2. Cluster into 2/3-line groups
 * 3. Detect format for each group
 * 4. Parse and validate
 * 5. Return best candidate based on combined scores
 * 
 * @param context Android context
 * @param config Configuration
 */
internal class MRZImageProcessorDefault(
    private val context: Context,
    private var config: MRZProcessorConfig
) : IMRZImageProcessor {
    
    private val textRecognizer = MRZTextRecognizer(config)
    private var isReleased = false
    
    // Stability tracking
    private var lastStableKey: String? = null
    private var stableCount: Int = 0
    
    override suspend fun processImage(imageProxy: ImageProxy): MRZResult {
        if (isReleased) {
            return MRZResult.Error("Processor has been released")
        }
        
        return try {
            logDebug("========================================")
            logDebug("Processing ImageProxy")
            
            // Step 1: Extract line candidates with scoring
            val candidates = textRecognizer.recognizeWithCandidates(imageProxy)
            
            if (candidates.isEmpty()) {
                logDebug("No MRZ candidates found")
                resetStability()
                return MRZResult.Scanning()
            }
            
            logDebug("Found ${candidates.size} line candidates")
            
            // Step 2: Cluster into groups (using simplified clustering)
            val groups = MRZLineClusteringSimplified.buildGroups(candidates).take(8)
            
            logDebug("Clustering result: ${groups.size} groups formed from ${candidates.size} candidates")
            groups.forEachIndexed { idx, group ->
                logDebug("  Group $idx: ${group.lines.size} lines, score: ${group.score}")
                group.lines.forEach { line ->
                    logDebug("    Line: \"${line.text}\" at Y=${line.bounds.centerY()}")
                }
            }
            
            if (groups.isEmpty()) {
                logDebug("No valid MRZ groups formed - candidates not properly aligned/stacked")
                resetStability()
                return MRZResult.Scanning()
            }
            
            logDebug("Formed ${groups.size} MRZ groups")
            
            // Step 3: Evaluate each group
            val evaluated = evaluateGroups(groups)
            
            if (evaluated.isEmpty()) {
                logDebug("No valid MRZ found after evaluation")
                resetStability()
                return MRZResult.Scanning()
            }
            
            // Step 4: Check stability
            val best = evaluated.first()
            val stabilityKey = createStabilityKey(best.rawLines)
            
            if (lastStableKey == stabilityKey) {
                stableCount++
            } else {
                lastStableKey = stabilityKey
                stableCount = 1
            }
            
            logDebug("Stability: $stableCount/${config.stabilityFramesRequired} (key: $stabilityKey)")
            
            if (stableCount >= config.stabilityFramesRequired) {
                logDebug("MRZ stable! Parsing...")
                
                // Parse with factory
                return when (val parseResult = MRZParserFactory.detectAndParse(best.rawLines)) {
                    is ParseResult.Success -> {
                        logDebug("Successfully parsed MRZ!")
                        MRZResult.Success(parseResult.data)
                    }
                    is ParseResult.Failure -> {
                        val errorMsg = parseResult.errors.joinToString(", ") { it.toMessage() }
                        logDebug("Parse failed: $errorMsg - resetting and continuing scan")
                        // Reset stability and continue scanning with informative message
                        resetStability()
                        MRZResult.Scanning(
                            message = "Parse failed: $errorMsg. Adjust document and try again."
                        )
                    }
                }
            } else {
                logDebug("Not yet stable, continuing scan...")
                return MRZResult.Scanning()
            }
            
        } catch (e: Exception) {
            logDebug("Error processing image: ${e.message}")
            MRZResult.Error(
                message = "Failed to process image: ${e.message}",
                exception = e
            )
        }
    }
    
    override suspend fun processImage(bitmap: Bitmap): MRZResult {
        if (isReleased) {
            return MRZResult.Error("Processor has been released")
        }
        
        return try {
            logDebug("========================================")
            logDebug("Processing Bitmap")
            
            val candidates = textRecognizer.recognizeWithCandidates(bitmap)
            
            if (candidates.isEmpty()) {
                logDebug("No MRZ candidates found")
                resetStability()
                return MRZResult.Scanning()
            }
            
            val groups = MRZLineClusteringSimplified.buildGroups(candidates).take(8)
            
            if (groups.isEmpty()) {
                resetStability()
                return MRZResult.Scanning()
            }
            
            val evaluated = evaluateGroups(groups)
            
            if (evaluated.isEmpty()) {
                resetStability()
                return MRZResult.Scanning()
            }
            
            val best = evaluated.first()
            
            // For bitmap processing, skip stability and parse immediately
            return when (val parseResult = MRZParserFactory.detectAndParse(best.rawLines)) {
                is ParseResult.Success -> {
                    logDebug("Successfully parsed MRZ!")
                    MRZResult.Success(parseResult.data)
                }
                is ParseResult.Failure -> {
                    val errorMsg = parseResult.errors.joinToString(", ") { it.toMessage() }
                    logDebug("Parse failed: $errorMsg")
                    MRZResult.Error("Failed to parse MRZ: $errorMsg")
                }
            }
            
        } catch (e: Exception) {
            logDebug("Error processing bitmap: ${e.message}")
            MRZResult.Error(
                message = "Failed to process bitmap: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Evaluate groups and return sorted by score.
     * Only evaluates top N groups based on config for performance.
     */
    private fun evaluateGroups(groups: List<MRZGroup>): List<EvaluatedCandidate> {
        val evaluated = mutableListOf<EvaluatedCandidate>()
        
        // Only evaluate top N groups based on config
        val groupsToEvaluate = groups.take(config.maxGroupsToEvaluate)
        
        for (group in groupsToEvaluate) {
            // Sort lines by Y position (top to bottom)
            val normalizedLines = group.lines
                .sortedBy { it.bounds.centerY() }
                .map { it.text }
            
            // Detect format
            val format = detectFormat(normalizedLines) ?: continue
            
            logDebug("Group with ${normalizedLines.size} lines detected as ${format.name}")
            
            // Calculate final score (group score + format bonus)
            val formatBonus = when (format) {
                MRZFormat.TD3 -> 0.5  // Passports are most common
                MRZFormat.TD1 -> 0.3  // ID cards
                MRZFormat.TD2 -> 0.2  // Less common
                else -> 0.0
            }
            
            val finalScore = group.score + formatBonus
            
            evaluated.add(
                EvaluatedCandidate(
                    group = group,
                    format = format,
                    rawLines = normalizedLines,
                    finalScore = finalScore
                )
            )
        }
        
        return evaluated.sortedByDescending { it.finalScore }
    }
    
    /**
     * Detect MRZ format from line patterns.
     */
    private fun detectFormat(lines: List<String>): MRZFormat? {
        return when (lines.size) {
            3 -> {
                // TD1: 3 lines of 30 chars
                if (lines.all { it.length in 28..32 }) {
                    MRZFormat.TD1
                } else null
            }
            2 -> {
                // TD2: 2 lines of 36 chars
                // TD3: 2 lines of 44 chars
                val l0 = lines[0].length
                val l1 = lines[1].length
                val avg = (l0 + l1) / 2
                
                when {
                    avg >= 40 -> MRZFormat.TD3  // Passport (40+)
                    avg >= 30 -> MRZFormat.TD2  // Visa/ID (30-39)
                    else -> null
                }
            }
            else -> null
        }
    }
    
    /**
     * Create stability key from MRZ lines.
     * Uses very fuzzy matching - focuses only on document number and dates which are most stable.
     */
    private fun createStabilityKey(lines: List<String>): String {
        when (lines.size) {
            3 -> {
                // TD1 format: Extract stable fields from lines 1 and 2
                val line1 = lines[0]
                val line2 = lines[1]
                
                // Line 1: Document number (positions 5-13)
                val docNum = if (line1.length > 13) line1.substring(5, kotlin.math.min(14, line1.length)) else ""
                
                // Line 2: DOB (positions 0-5), Expiry (positions 8-13)
                val dob = if (line2.length > 5) line2.substring(0, kotlin.math.min(6, line2.length)) else ""
                val expiry = if (line2.length > 13) line2.substring(8, kotlin.math.min(14, line2.length)) else ""
                
                return "$docNum|$dob|$expiry"
            }
            2 -> {
                // TD2/TD3 format: Extract from line 2
                val line2 = lines[1]
                
                val docNum = if (line2.length > 8) line2.substring(0, kotlin.math.min(9, line2.length)) else ""
                val dob = if (line2.length > 18) line2.substring(13, kotlin.math.min(19, line2.length)) else ""
                val expiry = if (line2.length > 26) line2.substring(21, kotlin.math.min(27, line2.length)) else ""
                
                return "$docNum|$dob|$expiry"
            }
            else -> return lines.joinToString("|")
        }
    }
    
    /**
     * Reset stability tracking.
     */
    private fun resetStability() {
        lastStableKey = null
        stableCount = 0
    }
    
    override fun configure(config: MRZProcessorConfig) {
        this.config = config
        textRecognizer.updateConfig(config)
        logDebug("Configuration updated")
    }
    
    override fun getConfig(): MRZProcessorConfig {
        return config
    }
    
    override fun release() {
        if (!isReleased) {
            textRecognizer.release()
            isReleased = true
            logDebug("Processor released")
        }
    }
    
    private fun logDebug(message: String) {
        if (config.debugMode) {
            Log.d(TAG, message)
        }
    }
    
    companion object {
        private const val TAG = "MRZImageProcessorDefault"
    }
}