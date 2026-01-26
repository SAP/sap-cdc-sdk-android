// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.recognition

import android.graphics.Rect
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Simplified line clustering that accepts valid MRZ line combinations
 * without overly strict bounding box validation.
 * 
 * Key improvement: If we have 2-3 lines that look like valid MRZ based on their
 * text content (length, character set), we accept them as a group regardless of
 * whether OCR provided accurate bounding boxes.
 */
internal object MRZLineClusteringSimplified {
    
    private const val TAG = "MRZLineClusteringSimplified"
    
    /**
     * Build MRZ groups from line candidates.
     * Strategy: Try text-based grouping first, fall back to position-based if needed.
     */
    fun buildGroups(candidates: List<MRZLineCandidate>): List<MRZGroup> {
        Log.d(TAG, "Building groups from ${candidates.size} candidates")
        
        if (candidates.isEmpty()) return emptyList()
        
        val groups = mutableListOf<MRZGroup>()
        
        // Strategy 1: Simple sequential grouping for 2-3 lines
        // If we have exactly 2 candidates with appropriate lengths, group them
        if (candidates.size == 2) {
            val group = createGroupFromCandidates(candidates)
            if (group != null) {
                Log.d(TAG, "Created 2-line group from all candidates")
                groups.add(group)
            }
        }
        
        // If we have 3 candidates, try as a 3-line group
        if (candidates.size == 3) {
            val group = createGroupFromCandidates(candidates)
            if (group != null) {
                Log.d(TAG, "Created 3-line group from all candidates")
                groups.add(group)
            }
        }
        
        // Strategy 2: Try pairwise combinations for larger sets
        if (candidates.size > 3) {
            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    val pair = listOf(candidates[i], candidates[j])
                    val group = createGroupFromCandidates(pair)
                    if (group != null) {
                        Log.d(TAG, "Created 2-line group from pair [$i,$j]")
                        groups.add(group)
                    }
                    
                    // Try 3-line combinations
                    for (k in j + 1 until candidates.size) {
                        val triple = listOf(candidates[i], candidates[j], candidates[k])
                        val group3 = createGroupFromCandidates(triple)
                        if (group3 != null) {
                            Log.d(TAG, "Created 3-line group from triple [$i,$j,$k]")
                            groups.add(group3)
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Created ${groups.size} total groups")
        
        // Score and sort by quality
        return groups.map { scoreGroup(it) }.sortedByDescending { it.score }
    }
    
    /**
     * Create a group from candidates if they form a valid MRZ pattern.
     */
    private fun createGroupFromCandidates(candidates: List<MRZLineCandidate>): MRZGroup? {
        if (candidates.size !in 2..3) return null
        
        // Sort by Y position (top to bottom)
        val sorted = candidates.sortedBy { it.bounds.centerY() }
        
        // Check if line lengths suggest a valid MRZ format
        val lengths = sorted.map { it.text.length }
        
        val isValid = when (sorted.size) {
            2 -> {
                // TD2 (~36 chars) or TD3 (~44 chars)
                // Accept if at least one line is in valid range
                // This handles OCR truncation issues
                val hasValidLength = lengths.any { it in 34..46 }
                val avg = lengths.average()
                hasValidLength && avg >= 28.0  // Lenient: at least avg 28 chars
            }
            3 -> {
                // TD1 (~30 chars per line)
                lengths.all { it in 26..33 }  // More lenient range
            }
            else -> false
        }
        
        if (!isValid) {
            Log.d(TAG, "Rejected group: lengths=$lengths don't match expected formats")
            return null
        }
        
        // Create union bounds
        val unionBounds = sorted.map { it.bounds }.reduce { acc, rect -> union(acc, rect) }
        
        return MRZGroup(
            lines = sorted,
            bounds = unionBounds,
            score = sorted.sumOf { it.score }
        )
    }
    
    /**
     * Score a group based on line quality and consistency.
     */
    private fun scoreGroup(group: MRZGroup): MRZGroup {
        val sorted = group.lines.sortedBy { it.bounds.centerY() }
        
        // Base score from line scores
        val baseScore = sorted.sumOf { it.score }
        
        // Bonus for consistent line lengths
        val lengths = sorted.map { it.text.length }
        val avgLength = lengths.average()
        val lengthVariance = lengths.sumOf { abs(it - avgLength) } / avgLength
        val lengthBonus = max(0.0, 1.0 - lengthVariance)
        
        // Bonus for proper Y-ordering
        val yOrdered = sorted.zipWithNext().all { (a, b) -> b.bounds.centerY() > a.bounds.centerY() }
        val orderBonus = if (yOrdered) 0.5 else 0.0
        
        val finalScore = baseScore + lengthBonus + orderBonus
        
        Log.d(TAG, "Group scored: base=$baseScore, length=$lengthBonus, order=$orderBonus, final=$finalScore")
        
        return group.copy(score = finalScore, lines = sorted)
    }
    
    /**
     * Union of two rectangles.
     */
    private fun union(a: Rect, b: Rect): Rect {
        return Rect(
            min(a.left, b.left),
            min(a.top, b.top),
            max(a.right, b.right),
            max(a.bottom, b.bottom)
        )
    }
}