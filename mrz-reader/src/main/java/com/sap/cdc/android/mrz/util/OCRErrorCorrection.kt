// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.android.mrz.util

import android.util.Log

/**
 * OCR error correction utility for MRZ text.
 * 
 * Implements industry-standard character confusion corrections based on:
 * - Visual similarity of characters
 * - Field-specific context (letters vs digits)
 * - Common OCR engine mistakes
 * 
 * This is a standard feature in professional MRZ readers (Regula, Scandit, Anyline)
 * that significantly improves accuracy for documents with poor print quality.
 */
object OCRErrorCorrection {
    
    private const val TAG = "OCRErrorCorrection"
    
    /**
     * Common OCR character confusions.
     * Maps misread characters to their likely correct values.
     */
    private val LETTER_CONFUSIONS = mapOf(
        '0' to 'O',  // Zero to letter O
        '1' to 'I',  // One to letter I
        '5' to 'S',  // Five to letter S
        '8' to 'B',  // Eight to letter B
        '6' to 'G',  // Six to letter G
        '2' to 'Z',  // Two to letter Z (less common)
    )
    
    private val DIGIT_CONFUSIONS = mapOf(
        'O' to '0',  // Letter O to zero
        'I' to '1',  // Letter I to one
        'S' to '5',  // Letter S to five
        'B' to '8',  // Letter B to eight
        'G' to '6',  // Letter G to six
        'Z' to '2',  // Letter Z to two (less common)
        'D' to '0',  // Letter D to zero (less common)
        'Q' to '0',  // Letter Q to zero (less common)
    )
    
    /**
     * Correct OCR errors in MRZ lines based on expected format.
     * 
     * @param lines The MRZ lines to correct
     * @param format The detected MRZ format
     * @return Corrected lines with logging of changes made
     */
    fun correctLines(lines: List<String>, format: com.sap.cdc.android.mrz.model.MRZFormat): List<String> {
        return when (format) {
            com.sap.cdc.android.mrz.model.MRZFormat.TD1 -> correctTD1(lines)
            com.sap.cdc.android.mrz.model.MRZFormat.TD2 -> correctTD2(lines)
            com.sap.cdc.android.mrz.model.MRZFormat.TD3 -> correctTD3(lines)
            else -> lines
        }
    }
    
    /**
     * Correct TD3 format (2 lines × 44 chars).
     * 
     * Line 1: P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<
     *         └─┘ └────────────────────────────────────┘
     *         Type  Name (all letters)
     *         
     * Line 2: L898902C36UTO7408122F1204159ZE184226B<<<<<10
     *         └────────┘└─┘└────┘ └────┘ └────────────┘└┘
     *         Doc# (mix) Nat DOB   Expiry Personal# (mix) Chk
     */
    private fun correctTD3(lines: List<String>): List<String> {
        if (lines.size != 2) return lines
        
        val line1 = lines[0]
        val line2 = lines[1]
        
        // Line 1 corrections
        val correctedLine1 = buildString {
            for (i in line1.indices) {
                val char = line1[i]
                when (i) {
                    in 0..1 -> append(char)  // Document type code (mixed)
                    2 -> append(if (char.isDigit()) DIGIT_CONFUSIONS[char] ?: char else char)  // Should be '<'
                    in 3..4 -> append(correctToLetter(char))  // Country code (letters)
                    else -> append(correctToLetter(char))  // Name section (letters + <)
                }
            }
        }
        
        // Line 2 corrections
        val correctedLine2 = buildString {
            for (i in line2.indices) {
                val char = line2[i]
                when (i) {
                    in 0..8 -> append(char)  // Document number (mixed - alphanumeric, keep as-is)
                    9 -> append(correctToDigit(char))  // Checksum (digit)
                    in 10..12 -> append(correctToLetter(char))  // Nationality (letters)
                    in 13..18 -> append(correctToDigit(char))  // Date of birth (digits)
                    19 -> append(correctToDigit(char))  // Checksum (digit)
                    20 -> append(char)  // Sex (M/F/<, leave as-is)
                    in 21..26 -> append(correctToDigit(char))  // Expiration date (digits)
                    27 -> append(correctToDigit(char))  // Checksum (digit)
                    in 28..41 -> append(char)  // Personal number (mixed, keep as-is)
                    42 -> append(correctToDigit(char))  // Checksum (digit)
                    43 -> append(correctToDigit(char))  // Composite checksum (digit)
                    else -> append(char)
                }
            }
        }
        
        logCorrections("TD3", line1, correctedLine1, line2, correctedLine2)
        
        return listOf(correctedLine1, correctedLine2)
    }
    
    /**
     * Correct TD2 format (2 lines × 36 chars).
     */
    private fun correctTD2(lines: List<String>): List<String> {
        if (lines.size != 2) return lines
        
        val line1 = lines[0]
        val line2 = lines[1]
        
        // Line 1: Similar to TD3 but 36 chars
        val correctedLine1 = buildString {
            for (i in line1.indices) {
                val char = line1[i]
                when (i) {
                    in 0..1 -> append(char)  // Document type
                    2 -> append(if (char.isDigit()) DIGIT_CONFUSIONS[char] ?: char else char)
                    in 3..4 -> append(correctToLetter(char))  // Country
                    else -> append(correctToLetter(char))  // Name
                }
            }
        }
        
        // Line 2: TD2 specific positions
        val correctedLine2 = buildString {
            for (i in line2.indices) {
                val char = line2[i]
                when (i) {
                    in 0..8 -> append(char)  // Document number (mixed)
                    9 -> append(correctToDigit(char))  // Checksum
                    in 10..12 -> append(correctToLetter(char))  // Nationality
                    in 13..18 -> append(correctToDigit(char))  // DOB
                    19 -> append(correctToDigit(char))  // Checksum
                    20 -> append(char)  // Sex
                    in 21..26 -> append(correctToDigit(char))  // Expiry
                    27 -> append(correctToDigit(char))  // Checksum
                    in 28..34 -> append(char)  // Optional data (mixed)
                    35 -> append(correctToDigit(char))  // Composite checksum
                    else -> append(char)
                }
            }
        }
        
        logCorrections("TD2", line1, correctedLine1, line2, correctedLine2)
        
        return listOf(correctedLine1, correctedLine2)
    }
    
    /**
     * Correct TD1 format (3 lines × 30 chars).
     */
    private fun correctTD1(lines: List<String>): List<String> {
        if (lines.size != 3) return lines
        
        val line1 = lines[0]
        val line2 = lines[1]
        val line3 = lines[2]
        
        // Line 1: I<UTOD231458907<<<<<<<<<<<<<<<
        val correctedLine1 = buildString {
            for (i in line1.indices) {
                val char = line1[i]
                when (i) {
                    in 0..1 -> append(char)  // Document type
                    2 -> append(if (char.isDigit()) DIGIT_CONFUSIONS[char] ?: char else char)
                    in 3..4 -> append(correctToLetter(char))  // Country
                    in 5..13 -> append(char)  // Document number (mixed)
                    14 -> append(correctToDigit(char))  // Checksum
                    else -> append(char)  // Optional data
                }
            }
        }
        
        // Line 2: 7408122F1204159UTO<<<<<<<<<<<6
        val correctedLine2 = buildString {
            for (i in line2.indices) {
                val char = line2[i]
                when (i) {
                    in 0..5 -> append(correctToDigit(char))  // DOB
                    6 -> append(correctToDigit(char))  // Checksum
                    7 -> append(char)  // Sex
                    in 8..13 -> append(correctToDigit(char))  // Expiry
                    14 -> append(correctToDigit(char))  // Checksum
                    in 15..17 -> append(correctToLetter(char))  // Nationality
                    in 18..28 -> append(char)  // Optional data (mixed)
                    29 -> append(correctToDigit(char))  // Composite checksum
                    else -> append(char)
                }
            }
        }
        
        // Line 3: ERIKSSON<<ANNA<MARIA<<<<<<<<<<
        val correctedLine3 = line3.map { correctToLetter(it) }.joinToString("")
        
        logCorrections("TD1", line1, correctedLine1, line2, correctedLine2, line3, correctedLine3)
        
        return listOf(correctedLine1, correctedLine2, correctedLine3)
    }
    
    /**
     * Correct a character to be a letter (A-Z or <).
     */
    private fun correctToLetter(char: Char): Char {
        return when {
            char in 'A'..'Z' || char == '<' -> char
            char.isDigit() -> LETTER_CONFUSIONS[char] ?: char
            else -> char
        }
    }
    
    /**
     * Correct a character to be a digit (0-9).
     */
    private fun correctToDigit(char: Char): Char {
        return when {
            char in '0'..'9' || char == '<' -> char
            char in 'A'..'Z' -> DIGIT_CONFUSIONS[char] ?: char
            else -> char
        }
    }
    
    /**
     * Log corrections made for debugging.
     */
    private fun logCorrections(format: String, vararg linePairs: String) {
        val changes = mutableListOf<String>()
        
        for (i in linePairs.indices step 2) {
            if (i + 1 >= linePairs.size) break
            
            val original = linePairs[i]
            val corrected = linePairs[i + 1]
            
            if (original != corrected) {
                val lineNum = (i / 2) + 1
                val diff = findDifferences(original, corrected)
                changes.add("Line $lineNum: $diff")
            }
        }
        
        if (changes.isNotEmpty()) {
            Log.d(TAG, "$format OCR corrections applied:")
            changes.forEach { Log.d(TAG, "  $it") }
        }
    }
    
    /**
     * Find character-by-character differences between two strings.
     */
    private fun findDifferences(original: String, corrected: String): String {
        val diffs = mutableListOf<String>()
        val maxLen = maxOf(original.length, corrected.length)
        
        for (i in 0 until maxLen) {
            val orig = original.getOrNull(i) ?: '?'
            val corr = corrected.getOrNull(i) ?: '?'
            if (orig != corr) {
                diffs.add("pos $i: '$orig'→'$corr'")
            }
        }
        
        return if (diffs.size <= 3) {
            diffs.joinToString(", ")
        } else {
            "${diffs.size} corrections"
        }
    }
}