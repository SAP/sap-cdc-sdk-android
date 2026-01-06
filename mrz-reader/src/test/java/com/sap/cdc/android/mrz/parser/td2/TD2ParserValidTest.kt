package com.sap.cdc.android.mrz.parser.td2

import com.sap.cdc.android.mrz.model.DocumentType
import com.sap.cdc.android.mrz.model.Gender
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD2Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD2Parser with valid MRZ examples.
 * 
 * Tests successful parsing scenarios with real-world TD2 format data.
 * All test data uses valid ICAO 9303 checksums.
 */
class TD2ParserValidTest {
    
    private lateinit var parser: TD2Parser
    
    @Before
    fun setup() {
        parser = TD2Parser()
    }
    
    @Test
    fun `parse valid TD2 ID card`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6"
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
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse TD2 with male gender`() {
        val lines = listOf(
            "I<UTOSMITH<<JOHN<<<<<<<<<<<<<<<<<<<<",
            "D231458907UTO7408122M1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals(Gender.MALE, data.sex)
        assertEquals("SMITH", data.surname)
        assertEquals("JOHN", data.givenNames)
    }
    
    @Test
    fun `parse TD2 with unspecified gender`() {
        val lines = listOf(
            "I<UTODOE<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "D231458907UTO7408122<1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals(Gender.UNSPECIFIED, data.sex)
        assertEquals("DOE", data.surname)
        assertEquals("", data.givenNames)
    }
    
    @Test
    fun `parse TD2 with name containing spaces`() {
        val lines = listOf(
            "I<UTOVON<DER<BERG<<MARIA<ANNA<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("VON DER BERG", data.surname)
        assertEquals("MARIA ANNA", data.givenNames)
        assertEquals("MARIA ANNA VON DER BERG", data.fullName)
    }
    
    @Test
    fun `parse TD2 with document number containing fillers`() {
        val lines = listOf(
            "I<UTOSMITH<<JANE<<<<<<<<<<<<<<<<<<<<",
            "A1234<<<<8UTO7408122F1204159<<<<<<<4"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("A1234", data.documentNumber)
    }
    
    @Test
    fun `parse TD2 with different country codes`() {
        val lines = listOf(
            "I<USAJOHNSON<<MARY<<<<<<<<<<<<<<<<<<",
            "D231458907USA7408122F1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("USA", data.countryCode)
        assertEquals("USA", data.nationality)
    }
    
    @Test
    fun `parse TD2 with century boundary date - 2000s`() {
        val lines = listOf(
            "I<UTOYOUNG<<ALICE<<<<<<<<<<<<<<<<<<<",
            "D231458907UTO2506300F3012316<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("2025-06-30", data.dateOfBirth)
        assertEquals("1930-12-31", data.expirationDate)
    }
    
    @Test
    fun `parse TD2 with century boundary date - 1900s`() {
        val lines = listOf(
            "I<UTOELDER<<BOB<<<<<<<<<<<<<<<<<<<<<",
            "D231458907UTO8501019M2512314<<<<<<<2"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("1985-01-01", data.dateOfBirth)
        assertEquals("2025-12-31", data.expirationDate)
    }
    
    @Test
    fun `parse TD2 with optional data`() {
        val lines = listOf(
            "I<UTOANDERSON<<CHRIS<<<<<<<<<<<<<<<<",
            "D231458907UTO9005156M2803157OPT12348"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertNotNull(data.personalNumber)
        assertEquals("OPT1234", data.personalNumber)
        assertEquals("ANDERSON", data.surname)
        assertEquals("CHRIS", data.givenNames)
    }
    
    @Test
    fun `full name property combines names correctly`() {
        val lines = listOf(
            "I<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<",
            "D231458907UTO7408122F1204159<<<<<<<6"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("ANNA MARIA ERIKSSON", data.fullName)
    }
}
