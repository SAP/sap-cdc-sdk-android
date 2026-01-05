package com.sap.cdc.android.mrz.parser

/**
 * Utility for validating MRZ checksums per ICAO Doc 9303 standard.
 * 
 * The ICAO 9303 checksum algorithm uses weighted modulo 10:
 * - Weights cycle through [7, 3, 1]
 * - Each character has a numeric value (0-35)
 * - Sum all (character_value × weight) and take modulo 10
 * 
 * Character values:
 * - '<' (filler) = 0
 * - '0'-'9' = 0-9
 * - 'A'-'Z' = 10-35 (A=10, B=11, C=12, ..., Z=35)
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303</a>
 */
object ChecksumValidator {
    
    private val WEIGHTS = intArrayOf(7, 3, 1)
    
    /**
     * Result of checksum validation with detailed information.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val calculated: Int,
        val expected: Int
    )
    
    /**
     * Validate a checksum against MRZ data.
     * 
     * Example:
     * ```kotlin
     * val result = validateChecksum("L898902C3", 6)
     * // result.isValid = true, calculated = 6, expected = 6
     * ```
     * 
     * @param data The data string to validate (excluding the checksum digit itself)
     * @param expectedChecksum The checksum digit from the MRZ
     * @return ValidationResult with calculation details
     */
    fun validateChecksum(data: String, expectedChecksum: Int): ValidationResult {
        val calculated = calculateChecksum(data)
        return ValidationResult(
            isValid = calculated == expectedChecksum,
            calculated = calculated,
            expected = expectedChecksum
        )
    }
    
    /**
     * Calculate checksum for given data string.
     * 
     * Algorithm:
     * 1. For each character at position i:
     *    - Get character value (0-35)
     *    - Multiply by weight[i % 3]
     * 2. Sum all products
     * 3. Return sum % 10
     * 
     * @param data The data string to calculate checksum for
     * @return Calculated checksum digit (0-9)
     */
    fun calculateChecksum(data: String): Int {
        val sum = data.mapIndexed { index, char ->
            charValue(char) * WEIGHTS[index % WEIGHTS.size]
        }.sum()
        return sum % 10
    }
    
    /**
     * Convert an MRZ character to its numeric value.
     * 
     * Mapping:
     * - '<' (filler) → 0
     * - '0'-'9' (digits) → 0-9
     * - 'A'-'Z' (letters) → 10-35
     * - Other characters → 0 (treated as filler)
     * 
     * @param char The character to convert
     * @return Numeric value (0-35)
     */
    private fun charValue(char: Char): Int = when (char) {
        '<' -> 0
        in '0'..'9' -> char.digitToInt()
        in 'A'..'Z' -> char.code - 55  // A=65-55=10, B=66-55=11, ..., Z=90-55=35
        else -> 0  // Invalid characters treated as filler
    }
    
    /**
     * Validate multiple checksums at once.
     * Useful for TD1 which has 3 checksums, or TD3 which has 4.
     * 
     * @param checksums Map of field names to (data, expectedChecksum) pairs
     * @return Map of field names to ValidationResult
     */
    fun validateMultipleChecksums(
        checksums: Map<String, Pair<String, Int>>
    ): Map<String, ValidationResult> {
        return checksums.mapValues { (_, pair) ->
            validateChecksum(pair.first, pair.second)
        }
    }
}
