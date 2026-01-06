package com.sap.cdc.android.mrz.parser.td3

import com.sap.cdc.android.mrz.model.DocumentType
import com.sap.cdc.android.mrz.model.Gender
import com.sap.cdc.android.mrz.parser.ParseResult
import com.sap.cdc.android.mrz.parser.TD3Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TD3Parser with valid MRZ examples.
 * 
 * Tests standard passport format (TD3) parsing with correct ICAO data.
 */
class TD3ParserValidTest {
    
    private lateinit var parser: TD3Parser
    
    @Before
    fun setup() {
        parser = TD3Parser()
    }
    
    @Test
    fun `parse ICAO standard example - female passport`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals(DocumentType.PASSPORT, data.documentType)
        assertEquals("UTO", data.countryCode)
        assertEquals("ERIKSSON", data.surname)
        assertEquals("ANNA MARIA", data.givenNames)
        assertEquals("L898902C3", data.documentNumber)
        assertEquals("UTO", data.nationality)
        assertEquals("1974-08-12", data.dateOfBirth)
        assertEquals(Gender.FEMALE, data.sex)
        assertEquals("2012-04-15", data.expirationDate)
        assertEquals("ZE184226B", data.personalNumber)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with male gender`() {
        val lines = listOf(
            "P<UTOSMITH<<JOHN<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122M1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("SMITH", data.surname)
        assertEquals("JOHN", data.givenNames)
        assertEquals(Gender.MALE, data.sex)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with unspecified gender`() {
        val lines = listOf(
            "P<UTODOE<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122<1204159<<<<<<<<<<<<<<08"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("DOE", data.surname)
        assertEquals("", data.givenNames)
        assertEquals(Gender.UNSPECIFIED, data.sex)
        assertNull(data.personalNumber)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with complex surname`() {
        val lines = listOf(
            "P<UTOVON<DER<BERG<<MARIA<ANNA<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("VON DER BERG", data.surname)
        assertEquals("MARIA ANNA", data.givenNames)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with document number containing fillers`() {
        val lines = listOf(
            "P<UTOSMITH<<JANE<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "A1234<<<<8UTO7408122F1204159<<<<<<<<<<<<<<04"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("A1234", data.documentNumber)
        assertNull(data.personalNumber)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with different country code`() {
        val lines = listOf(
            "P<USAJOHNSON<<MARY<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "L898902C36USA7408122F1204159ZE184226B<<<<<10"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("USA", data.countryCode)
        assertEquals("USA", data.nationality)
        assertEquals("JOHNSON", data.surname)
        assertEquals("MARY", data.givenNames)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport with 1900s century boundary dates`() {
        val lines = listOf(
            "P<UTOELDER<<BOB<<<<<<<<<<<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO8501019M2512314ZE184226B<<<<<16"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("1985-01-01", data.dateOfBirth)
        assertEquals("2025-12-31", data.expirationDate)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `parse passport without personal number`() {
        val lines = listOf(
            "P<UTOANDERSON<<CHRIS<<<<<<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO9005156M2803157<<<<<<<<<<<<<<04"
        )
        
        val result = parser.parse(lines)
        
        assertTrue(result is ParseResult.Success)
        val data = (result as ParseResult.Success).data
        
        assertEquals("ANDERSON", data.surname)
        assertEquals("CHRIS", data.givenNames)
        assertNull(data.personalNumber)
        assertTrue(data.isValid)
    }
    
    @Test
    fun `validateFormat accepts correct TD3 format`() {
        val lines = listOf(
            "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            "L898902C36UTO7408122F1204159ZE184226B<<<<<10"
        )
        
        val errors = parser.validateFormat(lines)
        
        assertTrue(errors.isEmpty())
    }
}
