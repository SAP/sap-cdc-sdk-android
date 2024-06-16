package com.sap.cdc.android.sdk.session

import com.sap.cdc.android.sdk.session.extensions.parseQueryStringParams
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class StringExtTest {

    @Test
    fun toQueryParamsMap_isCorrect() {
        val source = "param1=42&param2=54&param3=ololo&param4=aa"
        val result = source.parseQueryStringParams()

        assertEquals(4, result.size)
        val param1 = result["param1"]
        assertNotNull(param1)
        assertEquals("42", param1)

        val param2 = result["param2"]
        assertNotNull(param2)
        assertEquals("54", param2)

        val param3 = result["param3"]
        assertNotNull(param3)
        assertEquals("ololo", param3)

        val param4 = result["param4"]
        assertNotNull(param4)
        assertEquals("aa", param4)
    }
}