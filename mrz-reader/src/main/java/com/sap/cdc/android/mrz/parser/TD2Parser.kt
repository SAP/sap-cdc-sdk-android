package com.sap.cdc.android.mrz.parser

import com.sap.cdc.android.mrz.model.DocumentType
import com.sap.cdc.android.mrz.model.Gender
import com.sap.cdc.android.mrz.model.MRZData
import com.sap.cdc.android.mrz.model.MRZFormat

/**
 * Parser implementation for TD2 format (Official Travel Documents).
 * 
 * TD2 Format Specification (ICAO 9303 Part 6):
 * - 2 lines, 36 characters each
 * - Total: 72 characters
 * 
 * Line 1: I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<
 *         │ │ │└────────────────────────────┘
 *         │ │ │    Full Name (Surname<<GivenNames)
 *         │ │ Country
 *         │ Filler
 *         Document Type
 * 
 * Line 2: D231458907UTO7408122F1204159<<<<<<<6
 *         └────────┘│└─┘└────┘│└────┘│└────┘│
 *           Doc#   │Nat  DOB  │Expiry│ Opt  │
 *              Chk │    Chk    │    Chk Data Composite
 * 
 * Checksums validated:
 * 1. Document number (line 2, position 10)
 * 2. Date of birth (line 2, position 20)
 * 3. Expiration date (line 2, position 28)
 * 4. Composite (line 2, position 36)
 * 
 * @see <a href="https://www.icao.int/publications/Documents/9303_p6_cons_en.pdf">ICAO 9303 Part 6</a>
 */
class TD2Parser : MRZParser {
    
    override val supportedFormat: MRZFormat = MRZFormat.TD2
    
    override fun parse(lines: List<String>): ParseResult {
        val errors = mutableListOf<ParseError>()
        
        // Step 1: Validate format
        val formatErrors = validateFormat(lines)
        if (formatErrors.isNotEmpty()) {
            return ParseResult.Failure(formatErrors)
        }
        
        val line1 = lines[0]
        val line2 = lines[1]
        
        // Step 2: Extract fields with error tracking
        
        // Line 1 fields
        val documentTypeCode = extractOrError(line1, 0..0, "documentType", errors)
        val countryCode = extractOrError(line1, 2..4, "countryCode", errors)?.replace("<", "")
        val nameSection = extractOrError(line1, 5..35, "name", errors)
        val (surname, givenNames) = if (nameSection != null) {
            FieldExtractor.parseName(nameSection)
        } else {
            Pair("", "")
        }
        
        // Line 2 fields
        val documentNumber = extractOrError(line2, 0..8, "documentNumber", errors)?.replace("<", "")
        val docChecksum = FieldExtractor.extractChecksum(line2, 9, "documentNumber")
        val nationality = extractOrError(line2, 10..12, "nationality", errors)?.replace("<", "")
        val dobRaw = extractOrError(line2, 13..18, "dateOfBirth", errors)
        val dobChecksum = FieldExtractor.extractChecksum(line2, 19, "dateOfBirth")
        val genderCode = extractOrError(line2, 20..20, "gender", errors)
        val expiryRaw = extractOrError(line2, 21..26, "expirationDate", errors)
        val expiryChecksum = FieldExtractor.extractChecksum(line2, 27, "expirationDate")
        val optionalData = extractOrError(line2, 28..34, "optionalData", errors)?.replace("<", "")
        val compositeChecksum = FieldExtractor.extractChecksum(line2, 35, "composite")
        
        // Step 3: Validate checksums
        var allChecksumsValid = true
        
        // Document number checksum
        if (documentNumber != null && docChecksum != null) {
            val validation = ChecksumValidator.validateChecksum(
                line2.substring(0, 9),
                docChecksum
            )
            if (!validation.isValid) {
                errors.add(
                    ParseError.ChecksumFailed(
                        field = "documentNumber",
                        data = line2.substring(0, 9),
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
        
        // Date of birth checksum
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
        
        // Expiration date checksum
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
        
        // Composite checksum (validates line 2, positions 0-9 + 13-19 + 21-35)
        if (compositeChecksum != null) {
            val compositeData = line2.substring(0, 10) + 
                               line2.substring(13, 20) + 
                               line2.substring(21, 35)
            val validation = ChecksumValidator.validateChecksum(compositeData, compositeChecksum)
            if (!validation.isValid) {
                errors.add(
                    ParseError.ChecksumFailed(
                        field = "composite",
                        data = compositeData,
                        expected = compositeChecksum,
                        calculated = validation.calculated
                    )
                )
                allChecksumsValid = false
            }
        } else {
            errors.add(ParseError.MissingRequiredField("composite checksum"))
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
                    personalNumber = optionalData?.takeIf { it.isNotEmpty() },
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
        if (lines.size != 2) {
            errors.add(
                ParseError.InvalidLength(
                    expected = 2,
                    actual = lines.size,
                    lineNumber = 0
                )
            )
            return errors
        }
        
        // Check each line
        lines.forEachIndexed { index, line ->
            // Check length
            if (line.length != 36) {
                errors.add(
                    ParseError.InvalidLength(
                        expected = 36,
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
