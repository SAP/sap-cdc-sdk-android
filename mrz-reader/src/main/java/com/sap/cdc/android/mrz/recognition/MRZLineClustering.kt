// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.recognition

import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Groups individual MRZ line candidates into 2-line or 3-line MRZ groups.
 * 
 * This implements the clustering logic from the improved snippet that:
 * - Finds vertically stacked lines (TD2, TD3: 2 lines; TD1: 3 lines)
 * - Checks alignment (left/right edges should be close)
 * - Scores groups based on consistency
 */
internal object MRZLineClustering {
    
    /**
     * Build MRZ groups from line candidates.
     * 
     * @param candidates List of MRZ line candidates sorted by score
     * @return List of MRZ groups, scored and sorted
     */
    fun buildGroups(candidates: List<MRZLineCandidate>): List<MRZGroup> {
        val groups = mutableListOf<MRZGroup>()
        
        android.util.Log.d("MRZLineClustering", "Building groups from ${candidates.size} candidates")
        candidates.forEachIndexed { idx, candidate ->
            android.util.Log.d("MRZLineClustering", "  Candidate $idx: bounds=${candidate.bounds}, centerY=${candidate.bounds.centerY()}")
        }
        
        // Try all combinations (O(n²) is fine with small N after filtering)
        for (i in candidates.indices) {
            for (j in i + 1 until candidates.size) {
                val a = candidates[i]
                val b = candidates[j]
                
                val stackResult = isStacked(a.bounds, b.bounds)
                android.util.Log.d("MRZLineClustering", "Testing pair [$i,$j]: stacked=$stackResult")
                
                if (!stackResult) continue
                
                // Create 2-line group
                val group2 = MRZGroup(
                    lines = listOf(a, b),
                    bounds = union(a.bounds, b.bounds)
                )
                groups.add(group2)
                
                // Try to extend to 3-line group (TD1)
                for (k in j + 1 until candidates.size) {
                    val c = candidates[k]
                    if (!isStacked(b.bounds, c.bounds)) continue
                    if (!isAligned(a.bounds, b.bounds, c.bounds)) continue
                    
                    val group3 = MRZGroup(
                        lines = listOf(a, b, c),
                        bounds = union(union(a.bounds, b.bounds), c.bounds)
                    )
                    groups.add(group3)
                }
            }
        }
        
        // Score and sort groups
        return groups.map { scoreGroup(it) }.sortedByDescending { it.score }
    }
    
    /**
     * Check if two lines are vertically stacked.
     */
    private fun isStacked(a: Rect, b: Rect): Boolean {
        // Lines should be close in X (horizontal alignment)
        val xClose = abs(a.centerX() - b.centerX()) < max(a.width(), b.width()) * 0.25
        
        // b should be below a
        val below = b.centerY() > a.centerY()
        
        // Vertical gap should be reasonable (not too far apart)
        val yGap = abs(b.top - a.bottom)
        val yClose = yGap < max(a.height(), b.height()) * 2.2
        
        return xClose && below && yClose
    }
    
    /**
     * Check if three lines are aligned (for TD1 format).
     */
    private fun isAligned(a: Rect, b: Rect, c: Rect): Boolean {
        val lefts = listOf(a.left, b.left, c.left)
        val rights = listOf(a.right, b.right, c.right)
        
        val leftVariance = lefts.maxOrNull()!! - lefts.minOrNull()!!
        val rightVariance = rights.maxOrNull()!! - rights.minOrNull()!!
        
        val maxWidth = max(a.width(), max(b.width(), c.width()))
        
        // Allow 25% variance in alignment
        return leftVariance < maxWidth * 0.25 && rightVariance < maxWidth * 0.25
    }
    
    /**
     * Score a group based on consistency metrics.
     */
    private fun scoreGroup(group: MRZGroup): MRZGroup {
        val sorted = group.lines.sortedBy { it.bounds.centerY() }
        
        // Height consistency
        val heights = sorted.map { it.bounds.height() }
        val avgHeight = heights.average().coerceAtLeast(1.0)
        val heightVariance = heights.sumOf { abs(it - avgHeight) } / avgHeight
        
        // Vertical gap consistency
        val gaps = sorted.zipWithNext { p, n -> max(0, n.bounds.top - p.bounds.bottom) }
        val avgGap = if (gaps.isNotEmpty()) gaps.average() else 0.0
        val gapPenalty = min(1.0, avgGap / (avgHeight * 1.5))
        
        // Alignment quality
        val alignPenalty = calculateAlignmentPenalty(sorted.map { it.bounds })
        
        // Base score from line scores
        val baseScore = sorted.sumOf { it.score }
        
        // Final score with penalties
        val finalScore = baseScore - (heightVariance * 0.8) - (gapPenalty * 0.5) - (alignPenalty * 0.8)
        
        return group.copy(score = finalScore, lines = sorted)
    }
    
    /**
     * Calculate alignment penalty for a set of bounds.
     */
    private fun calculateAlignmentPenalty(bounds: List<Rect>): Double {
        if (bounds.size < 2) return 0.0
        
        val lefts = bounds.map { it.left }
        val rights = bounds.map { it.right }
        
        val leftVar = (lefts.maxOrNull()!! - lefts.minOrNull()!!).toDouble()
        val rightVar = (rights.maxOrNull()!! - rights.minOrNull()!!).toDouble()
        
        val maxWidth = bounds.maxOf { it.width() }.toDouble().coerceAtLeast(1.0)
        
        return (leftVar / maxWidth) + (rightVar / maxWidth)
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