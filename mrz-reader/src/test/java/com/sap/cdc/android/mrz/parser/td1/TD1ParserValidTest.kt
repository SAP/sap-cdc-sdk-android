package com.sap.cdc.android.mrz.parser.td1

import com.sap.cdc.android.mrz.model.DocumentType
import com.sap.cdc.android.mrz.model.Gender
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD1Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD1Parser with valid MRZ examples.
 * 
 * Tests successful parsing scenarios with real-world TD1 format data.
 */
class TD1ParserValidTest {
    
    private lateinit var parser: TD1Parser
    
    @Before
    fun setup() {
        parser = TD1Parser()
    }
    
    @Test
    fun `parse valid TD1 ID card from ICAO example`() {
        // Based on ICAO 9303 TD1 example
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val success = result as ParseResult.Success
        val data = success.data
        
        assertEquals(DocumentType.ID_CARD, data.documentType)
        assertEquals("UTO", data.countryCode)
        assertEquals("ERIKSSON", data.surname)
        assertEquals("ANNA MARIA", data.givenNames)
        assertEquals("D23145890", data.documentNumber)
        assertEquals("UTO", data.nationality)
        assertEquals("1974-08-12", data.dateOfBirth)
        assertEquals(Gender.FEMALE, data.sex)
        assertEquals("2012-04-15", data.expirationDate)
        assertNull(data.personalNumber)  // TD1 doesn't have personal number
        assertTrue(data.isValid)  // All checksums should be valid
    }
    
    @Test
    fun `parse TD1 with male gender`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122M1204159UTO<<<<<<<<<<<<",
            "SMITH<<JOHN<<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals(Gender.MALE, data.sex)
        assertEquals("SMITH", data.surname)
        assertEquals("JOHN", data.givenNames)
    }
    
    @Test
    fun `parse TD1 with unspecified gender`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122<1204159UTO<<<<<<<<<<<<",
            "DOE<<<<<<<<<<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals(Gender.UNSPECIFIED, data.sex)
        assertEquals("DOE", data.surname)
        assertEquals("", data.givenNames)  // No given names
    }
    
    @Test
    fun `parse TD1 with name containing spaces`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "VON<DER<BERG<<MARIA<ANNA<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        // Single < within names become spaces
        assertEquals("VON DER BERG", data.surname)
        assertEquals("MARIA ANNA", data.givenNames)
        assertEquals("MARIA ANNA VON DER BERG", data.fullName)
    }
    
    @Test
    fun `parse TD1 with only surname no given names`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<<<<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("ERIKSSON", data.surname)
        assertEquals("", data.givenNames)
        assertEquals("ERIKSSON", data.fullName)
    }
    
    @Test
    fun `parse TD1 with document number containing fillers`() {
        val lines = listOf(
            "I<UTOA1234<<<<8<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "SMITH<<JANE<<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        // Fillers should be removed from document number
        assertEquals("A1234", data.documentNumber)
    }
    
    @Test
    fun `parse TD1 with different country codes`() {
        val lines = listOf(
            "I<USAD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159USA<<<<<<<<<<<<",
            "JOHNSON<<MARY<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("USA", data.countryCode)
        assertEquals("USA", data.nationality)
    }
    
    @Test
    fun `parse TD1 with century boundary date - 2000s`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "2506300F3012316UTO<<<<<<<<<<<<",
            "YOUNG<<ALICE<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        // Year 25 should be 2025 (< 30 rule)
        // Year 30 should be 1930 (>= 30 rule)
        assertEquals("2025-06-30", data.dateOfBirth)
        assertEquals("1930-12-31", data.expirationDate)
    }
    
    @Test
    fun `parse TD1 with century boundary date - 1900s`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "8501019M2512314UTO<<<<<<<<<<<<",
            "ELDER<<BOB<<<<<<<<<<<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        // Year 85 should be 1985 (>= 30 rule)
        assertEquals("1985-01-01", data.dateOfBirth)
        assertEquals("2025-12-31", data.expirationDate)
    }
    
    @Test
    fun `full name property combines names correctly`() {
        val lines = listOf(
            "I<UTOD231458907<<<<<<<<<<<<<<<",
            "7408122F1204159UTO<<<<<<<<<<<<",
            "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("ANNA MARIA ERIKSSON", data.fullName)
    }
}
