package com.sap.cdc.android.mrz.model

/**
 * Represents the different Machine Readable Zone (MRZ) format types as defined by 
 * ICAO 9303 standard.
 * 
 * Each format specifies the number of lines and characters per line in the MRZ,
 * which determines how the data should be parsed.
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303 Part 3</a>
 */
enum class MRZFormat(
    val lines: Int,
    val charsPerLine: Int,
    val description: String
) {
    /**
     * TD1 Format - Identity Cards
     * - 3 lines, 30 characters each
     * - Total: 90 characters
     * - Commonly used for national ID cards
     * 
     * Structure:
     * - Line 1: Document code, issuing state, document number
     * - Line 2: Date of birth, sex, expiration date, nationality, optional data
     * - Line 3: Name (surname << given names)
     */
    TD1(
        lines = 3,
        charsPerLine = 30,
        description = "ID cards (3 lines, 30 chars each)"
    ),
    
    /**
     * TD2 Format - Some Passports and Travel Documents
     * - 2 lines, 36 characters each
     * - Total: 72 characters
     * - Less common passport format
     * 
     * Structure:
     * - Line 1: Document code, issuing state, name
     * - Line 2: Document number, nationality, date of birth, sex, expiration date, optional data
     */
    TD2(
        lines = 2,
        charsPerLine = 36,
        description = "Some passports (2 lines, 36 chars each)"
    ),
    
    /**
     * TD3 Format - Standard Passports
     * - 2 lines, 44 characters each
     * - Total: 88 characters
     * - Most common international passport format
     * 
     * Structure:
     * - Line 1: Document code, issuing state, name (surname << given names)
     * - Line 2: Document number, nationality, date of birth, sex, expiration date, personal number
     */
    TD3(
        lines = 2,
        charsPerLine = 44,
        description = "Standard passports (2 lines, 44 chars each)"
    ),
    
    /**
     * MRVA Format - Visa Type A
     * - 2 lines, 44 characters each
     * - Total: 88 characters
     * - Full-page visa format
     * 
     * Structure:
     * - Similar to TD3 but for visa documents
     */
    MRVA(
        lines = 2,
        charsPerLine = 44,
        description = "Visa type A (2 lines, 44 chars each)"
    ),
    
    /**
     * MRVB Format - Visa Type B
     * - 2 lines, 36 characters each
     * - Total: 72 characters
     * - Visa sticker format
     * 
     * Structure:
     * - Similar to TD2 but for visa documents
     */
    MRVB(
        lines = 2,
        charsPerLine = 36,
        description = "Visa type B (2 lines, 36 chars each)"
    );
    
    companion object {
        /**
         * Detect MRZ format from the number of lines and characters.
         * 
         * @param lines Number of MRZ lines detected
         * @param firstLineLength Length of the first MRZ line
         * @return Corresponding MRZFormat or null if no match
         */
        fun detect(lines: Int, firstLineLength: Int): MRZFormat? {
            return values().find { 
                it.lines == lines && it.charsPerLine == firstLineLength 
            }
        }
        
        /**
         * Check if the given line count and length represent a valid MRZ format.
         * 
         * @param lines Number of lines
         * @param charsPerLine Number of characters per line
         * @return true if valid MRZ format, false otherwise
         */
        fun isValidFormat(lines: Int, charsPerLine: Int): Boolean {
            return detect(lines, charsPerLine) != null
        }
    }
}
