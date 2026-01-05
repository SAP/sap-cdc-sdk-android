package com.sap.cdc.android.mrz.model

/**
 * Represents the type of travel document as defined by ICAO 9303 standard.
 * 
 * The document type is encoded in the first character of the MRZ and indicates
 * the category of the travel document being scanned.
 * 
 * @property code The single-character code used in MRZ to identify this document type
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303 Part 3</a>
 */
enum class DocumentType(val code: String) {
    /**
     * Passport (Type P)
     * - Standard format: TD3 (2 lines, 44 characters each)
     * - Most common international travel document
     */
    PASSPORT("P"),
    
    /**
     * Identity Card (Type I)
     * - Standard format: TD1 (3 lines, 30 characters each)
     * - National identity cards with international travel capability
     */
    ID_CARD("I"),
    
    /**
     * Visa Type A (Type VA)
     * - Format: 2 lines, 44 characters each
     * - Visa that occupies full page
     */
    VISA_A("VA"),
    
    /**
     * Visa Type B (Type VB)
     * - Format: 2 lines, 36 characters each
     * - Visa sticker format
     */
    VISA_B("VB"),
    
    /**
     * Unknown or unsupported document type
     * - Used when the document type code doesn't match known types
     * - May indicate damaged MRZ or non-standard document
     */
    UNKNOWN("U");
    
    companion object {
        /**
         * Parse document type from MRZ code.
         * 
         * @param code The single character code from MRZ
         * @return Corresponding DocumentType or UNKNOWN if not recognized
         */
        fun fromCode(code: String): DocumentType {
            return values().find { it.code == code } ?: UNKNOWN
        }
    }
}
