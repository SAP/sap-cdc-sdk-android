package com.sap.cdc.android.mrz.model

/**
 * Represents the gender/sex field in travel documents as per ICAO 9303 standard.
 * 
 * The gender is encoded as a single character in the MRZ, following international
 * conventions for travel documents.
 * 
 * @property code The single-character code used in MRZ to identify gender
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303 Part 3</a>
 */
enum class Gender(val code: String) {
    /**
     * Male (M)
     * - Indicates the document holder is male
     */
    MALE("M"),
    
    /**
     * Female (F)
     * - Indicates the document holder is female
     */
    FEMALE("F"),
    
    /**
     * Unspecified (<)
     * - Gender not specified or not applicable
     * - Represented by '<' character in MRZ
     * - Used when gender information is unavailable or not required
     */
    UNSPECIFIED("<");
    
    companion object {
        /**
         * Parse gender from MRZ code.
         * 
         * @param code The single character code from MRZ
         * @return Corresponding Gender or UNSPECIFIED if not recognized
         */
        fun fromCode(code: String): Gender {
            return values().find { it.code == code } ?: UNSPECIFIED
        }
    }
}
