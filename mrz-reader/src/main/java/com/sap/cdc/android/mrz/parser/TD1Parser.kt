package com.sap.cdc.android.mrz.parser

import com.sap.cdc.android.mrz.model.DocumentType
import com.sap.cdc.android.mrz.model.Gender
import com.sap.cdc.android.mrz.model.MRZData
import com.sap.cdc.android.mrz.model.MRZFormat

/**
 * Parser implementation for TD1 format (ID Cards).
 * 
 * TD1 Format Specification:
 * - 3 lines, 30 characters each
 * - Total: 90 characters
 * 
 * Line 1: I<UTOD23145890<<<<<<<<<<<<<
 *         │ │ │└─────────┘│
 *         │ │ │   Doc#    Checksum
 *         │ │ Country
 *         │ Filler
 *         Document Type
 * 
 * Line 2: 7408122F1204159UTO<<<<<<<<<<<
 *         └────┘│└┘└────┘│└─┘
 *          DOB  │Sx Expiry Nationality
 *           Chk │    Chk
 * 
 * Line 3: ERIKSSON<<ANNA<MARIA<<<<<<<<<<
 *         └──────┘  └──────────────────┘
 *         Surname   Given names
 * 
 * Checksums validated:
 * 1. Document number (line 1, position 14)
 * 2. Date of birth (line 2, position 6)
 * 3. Expiration date (line 2, position 14)
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p3_cons_en.pdf">ICAO 9303 Part 3</a>
 */
class TD1Parser : MRZParser {
    
    override val supportedFormat: MRZFormat = MRZFormat.TD1
    
    override fun parse(lines: List<String>): ParseResult {
        val errors = mutableListOf<ParseError>()
        
        // Step 1: Validate format
        val formatErrors = validateFormat(lines)
        if (formatErrors.isNotEmpty()) {
            return ParseResult.Failure(formatErrors)
        }
        
        val line1 = lines[0]
        val line2 = lines[1]
        val line3 = lines[2]
        
        // Step 2: Extract fields with error tracking
        
        // Line 1 fields
        val documentTypeCode = extractOrError(line1, 0..0, "documentType", errors)
        val countryCode = extractOrError(line1, 2..4, "countryCode", errors)?.replace("<", "")
        val documentNumber = extractOrError(line1, 5..13, "documentNumber", errors)?.replace("<", "")
        val docChecksum = FieldExtractor.extractChecksum(line1, 14, "documentNumber")
        
        // Line 2 fields
        val dobRaw = extractOrError(line2, 0..5, "dateOfBirth", errors)
        val dobChecksum = FieldExtractor.extractChecksum(line2, 6, "dateOfBirth")
        val genderCode = extractOrError(line2, 7..7, "gender", errors)
        val expiryRaw = extractOrError(line2, 8..13, "expirationDate", errors)
        val expiryChecksum = FieldExtractor.extractChecksum(line2, 14, "expirationDate")
        val nationality = extractOrError(line2, 15..17, "nationality", errors)?.replace("<", "")
        
        // Line 3 fields
        val nameSection = extractOrError(line3, 0..29, "name", errors)
        val (surname, givenNames) = if (nameSection != null) {
            FieldExtractor.parseName(nameSection)
        } else {
            Pair("", "")
        }
        
        // Step 3: Validate checksums
        var allChecksumsValid = true
        
        if (documentNumber != null && docChecksum != null) {
            val validation = ChecksumValidator.validateChecksum(
                line1.substring(5, 14), 
                docChecksum
            )
            if (!validation.isValid) {
                errors.add(
                    ParseError.ChecksumFailed(
                        field = "documentNumber",
                        data = line1.substring(5, 14),
                        expected = docChecksum,
                        calculated = validation.calculated
                    )
                )
                allChecksumsValid = false
            }
        } else {
            errors.add(ParseError.MissingRequiredField("documentNumber or its checksum"))
            allChecksumsValid = false
        }
        
        if (dobRaw != null && dobChecksum != null) {
            val validation = ChecksumValidator.validateChecksum(dobRaw, dobChecksum)
            if (!validation.isValid) {
                errors.add(
                    ParseError.ChecksumFailed(
                        field = "dateOfBirth",
                        data = dobRaw,
                        expected = dobChecksum,
                        calculated = validation.calculated
                    )
                )
                allChecksumsValid = false
            }
        } else {
            errors.add(ParseError.MissingRequiredField("dateOfBirth or its checksum"))
            allChecksumsValid = false
        }
        
        if (expiryRaw != null && expiryChecksum != null) {
            val validation = ChecksumValidator.validateChecksum(expiryRaw, expiryChecksum)
            if (!validation.isValid) {
                errors.add(
                    ParseError.ChecksumFailed(
                        field = "expirationDate",
                        data = expiryRaw,
                        expected = expiryChecksum,
                        calculated = validation.calculated
                    )
                )
                allChecksumsValid = false
            }
        } else {
            errors.add(ParseError.MissingRequiredField("expirationDate or its checksum"))
            allChecksumsValid = false
        }
        
        // Step 4: Parse dates
        val dateOfBirth = if (dobRaw != null) {
            when (val result = DateParser.parseDate(dobRaw)) {
                is DateParser.DateParseResult.Success -> result.formatted
                is DateParser.DateParseResult.Failure -> {
                    errors.add(ParseError.InvalidDate(dobRaw, "dateOfBirth", result.reason))
                    null
                }
            }
        } else null
        
        val expirationDate = if (expiryRaw != null) {
            when (val result = DateParser.parseDate(expiryRaw)) {
                is DateParser.DateParseResult.Success -> result.formatted
                is DateParser.DateParseResult.Failure -> {
                    errors.add(ParseError.InvalidDate(expiryRaw, "expirationDate", result.reason))
                    null
                }
            }
        } else null
        
        // Step 5: Parse enums
        val documentType = documentTypeCode?.let { DocumentType.fromCode(it) } ?: DocumentType.UNKNOWN
        val gender = genderCode?.let { Gender.fromCode(it) } ?: Gender.UNSPECIFIED
        
        // Step 6: Check if we have all required fields
        if (documentNumber == null || countryCode == null || nationality == null ||
            dateOfBirth == null || expirationDate == null || surname.isEmpty()) {
            
            if (errors.isEmpty()) {
                errors.add(ParseError.MissingRequiredField("One or more required fields"))
            }
            return ParseResult.Failure(errors)
        }
        
        // Step 7: Return result
        return if (errors.isEmpty()) {
            ParseResult.Success(
                MRZData(
                    documentType = documentType,
                    countryCode = countryCode,
                    surname = surname,
                    givenNames = givenNames,
                    documentNumber = documentNumber,
                    nationality = nationality,
                    dateOfBirth = dateOfBirth,
                    sex = gender,
                    expirationDate = expirationDate,
                    personalNumber = null,  // TD1 format doesn't have personal number
                    isValid = allChecksumsValid
                )
            )
        } else {
            ParseResult.Failure(errors)
        }
    }
    
    override fun validateFormat(lines: List<String>): List<ParseError> {
        val errors = mutableListOf<ParseError>()
        
        // Check line count
        if (lines.size != 3) {
            errors.add(
                ParseError.InvalidLength(
                    expected = 3,
                    actual = lines.size,
                    lineNumber = 0
                )
            )
            return errors
        }
        
        // Check each line
        lines.forEachIndexed { index, line ->
            // Check length
            if (line.length != 30) {
                errors.add(
                    ParseError.InvalidLength(
                        expected = 30,
                        actual = line.length,
                        lineNumber = index + 1
                    )
                )
            }
            
            // Check character set
            val invalidChars = line.filter { char ->
                !char.isUpperCase() && !char.isDigit() && char != '<'
            }.toSet()
            
            if (invalidChars.isNotEmpty()) {
                errors.add(
                    ParseError.InvalidCharacters(
                        invalidChars = invalidChars,
                        lineNumber = index + 1
                    )
                )
            }
        }
        
        return errors
    }
    
    /**
     * Helper function to extract field and track errors.
     */
    private fun extractOrError(
        line: String,
        range: IntRange,
        fieldName: String,
        errors: MutableList<ParseError>
    ): String? {
        return when (val result = FieldExtractor.extractField(line, range, fieldName)) {
            is FieldExtractor.ExtractionResult.Success -> result.value
            is FieldExtractor.ExtractionResult.Failure -> {
                errors.add(result.error)
                null
            }
        }
    }
}
