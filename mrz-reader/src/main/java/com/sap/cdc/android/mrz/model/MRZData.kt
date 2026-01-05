package com.sap.cdc.android.mrz.model

/**
 * Represents parsed and validated Machine Readable Zone (MRZ) data from a travel document.
 * 
 * This is the primary data structure returned after successfully scanning and parsing
 * an MRZ. All fields are extracted and validated according to ICAO 9303 standards.
 * 
 * @property documentType The type of document (passport, ID card, visa)
 * @property countryCode ISO 3166-1 alpha-3 country code of the issuing state
 * @property surname The surname(s) or primary identifier from the document
 * @property givenNames The given name(s) or secondary identifier from the document
 * @property documentNumber The document number (passport number, ID number, etc.)
 * @property nationality ISO 3166-1 alpha-3 country code of nationality
 * @property dateOfBirth Date of birth in YYYY-MM-DD format
 * @property sex The sex/gender of the document holder
 * @property expirationDate Document expiration date in YYYY-MM-DD format
 * @property personalNumber Optional personal number or additional identifier (TD3 format only)
 * @property isValid Indicates if all checksums passed validation
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303 Part 3</a>
 */
data class MRZData(
    val documentType: DocumentType,
    val countryCode: String,
    val surname: String,
    val givenNames: String,
    val documentNumber: String,
    val nationality: String,
    val dateOfBirth: String,
    val sex: Gender,
    val expirationDate: String,
    val personalNumber: String? = null,
    val isValid: Boolean
) {
    /**
     * Returns the full name combining given names and surname.
     */
    val fullName: String
        get() = if (givenNames.isNotEmpty()) {
            "$givenNames $surname"
        } else {
            surname
        }
    
    /**
     * Checks if the document has expired based on current date.
     * Note: This is a simple date comparison and doesn't account for grace periods.
     * 
     * @return true if the document has expired, false otherwise
     */
    fun isExpired(): Boolean {
        return try {
            val expiryDate = parseDate(expirationDate)
            val currentDate = parseDate(getCurrentDate())
            expiryDate < currentDate
        } catch (e: Exception) {
            false // Assume not expired if we can't parse the date
        }
    }
    
    /**
     * Calculates the approximate age based on date of birth.
     * 
     * @return Age in years, or null if date cannot be parsed
     */
    fun getAge(): Int? {
        return try {
            val birthDate = parseDate(dateOfBirth)
            val currentDate = parseDate(getCurrentDate())
            val years = (currentDate - birthDate) / (365.25 * 24 * 60 * 60 * 1000)
            years.toInt()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Provides a human-readable summary of the MRZ data.
     */
    override fun toString(): String {
        return buildString {
            appendLine("MRZ Data:")
            appendLine("  Document Type: ${documentType.name}")
            appendLine("  Country: $countryCode")
            appendLine("  Name: $fullName")
            appendLine("  Document Number: $documentNumber")
            appendLine("  Nationality: $nationality")
            appendLine("  Date of Birth: $dateOfBirth")
            appendLine("  Sex: ${sex.name}")
            appendLine("  Expiration: $expirationDate")
            personalNumber?.let { appendLine("  Personal Number: $it") }
            appendLine("  Valid: $isValid")
        }
    }
    
    private fun parseDate(dateString: String): Long {
        // Simple date parsing - assumes YYYY-MM-DD format
        val parts = dateString.split("-")
        if (parts.size != 3) throw IllegalArgumentException("Invalid date format")
        
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        
        // Rough calculation - not accounting for leap years, etc.
        return ((year - 1970) * 365.25 * 24 * 60 * 60 * 1000 +
                (month - 1) * 30.44 * 24 * 60 * 60 * 1000 +
                day * 24 * 60 * 60 * 1000).toLong()
    }
    
    private fun getCurrentDate(): String {
        // This would typically use proper date/time APIs
        // Placeholder for current date in YYYY-MM-DD format
        return "2026-01-04" // TODO: Replace with actual date retrieval
    }
}
