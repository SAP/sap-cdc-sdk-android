package com.sap.cdc.android.sdk

import android.annotation.SuppressLint
import android.util.Base64
import com.sap.cdc.android.sdk.core.api.Signing
import com.sap.cdc.android.sdk.core.api.SigningSpec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@SuppressLint("CheckResult")
class SigningUnitTest {

    init {
        mockStatic(Base64::class.java)
        `when`(Base64.encode(any(), anyInt())).thenAnswer { invocation ->
            java.util.Base64.getUrlEncoder().encode(invocation.arguments[0] as ByteArray)
        }
        `when`(Base64.decode(anyString(), anyInt())).thenAnswer { invocation ->
            java.util.Base64.getMimeDecoder().decode(invocation.arguments[0] as String)
        }
        `when`(Base64.encodeToString(any(), anyInt())).thenAnswer { invocation ->
            String(java.util.Base64.getUrlEncoder().encode(invocation.arguments[0] as ByteArray))
        }
    }

    private val signing = Signing()
    private val spec = SigningSpec(
        "9uZBWx8PDUZQ4XPgk4t5N/hEnHM=",
        "https://accounts.us1.gigya.com/accounts.getAccountInfo",
        "POST",
        mutableMapOf(
            "apiKey" to "4_mL-YkAEegR9vzt6QvHWI5Q",
            "extraProfileFields" to "languages,phones,education",
            "format" to "json",
            "gmid" to "gmid.ver4.AtLt0wCRug.dnF-O6cRvaT9KvNWOMCrwbqAQnAy1WB9od4etn6QC3FBloqFm0r3gOdZWoT4D4aq.zG-Wvi5PZQ_uUqbjsi0XNnwNsPBciRlaDZwC_KSrHQYAOY2XLjmrYPmIArJBB1H9h4oI39Ic4Q5BJ-WOE8ZRSQ.sc3",
            "httpStatusCodes" to "false",
            "include" to "data,profile,emails,missing-required-fields",
            "nonce" to "1718648375377_653059349",
            "oauth_token" to "st2.s.AtLtNeMpVg.fZotUbj-qRTj1QfbMTJjsuIXMvuebpOpVfYR5vrBN-5DXDWImSj26e9a2eVlAYGVv3mJj18gorcLDUK-x4fQ6juVfR-a5x2Keixt7RKeu6n5U3NCf6AEEttS-Jd-BI8y.psjwXTXHHgCsud0UVowzRu4Y100Ja8Te3ri7jrkzpYXcoiToyqbo3YJA9AYPmTcR35oiOzLw8WIfOFcwkLzPOg.sc3",
            "sdk" to "Android_7.0.11",
            "targetEnv" to "mobile",
            "timestamp" to "1718648375",
            "ucid" to "WhVS3SvK1JFRArK-4KqpMw"
        )
    )

    companion object {
        const val VALID_SIGNATURE_VALUE = "4HH-d8okgzeU1BukwZIzEuEfsCo="
    }

    @Test
    fun newSignature_isCorrect() {
        println("normalizedUrl: https://accounts.us1.gigya.com/accounts.getAccountInfo")
        println("baseSignature: POST&https%3A%2F%2Faccounts.us1.gigya.com%2Faccounts.getAccountInfo&apiKey%3D4_mL-YkAEegR9vzt6QvHWI5Q%26extraProfileFields%3Dlanguages%252Cphones%252Ceducation%26format%3Djson%26gmid%3Dgmid.ver4.AtLt0wCRug.dnF-O6cRvaT9KvNWOMCrwbqAQnAy1WB9od4etn6QC3FBloqFm0r3gOdZWoT4D4aq.zG-Wvi5PZQ_uUqbjsi0XNnwNsPBciRlaDZwC_KSrHQYAOY2XLjmrYPmIArJBB1H9h4oI39Ic4Q5BJ-WOE8ZRSQ.sc3%26httpStatusCodes%3Dfalse%26include%3Ddata%252Cprofile%252Cemails%252Cmissing-required-fields%26nonce%3D1718648375377_653059349%26oauth_token%3Dst2.s.AtLtNeMpVg.fZotUbj-qRTj1QfbMTJjsuIXMvuebpOpVfYR5vrBN-5DXDWImSj26e9a2eVlAYGVv3mJj18gorcLDUK-x4fQ6juVfR-a5x2Keixt7RKeu6n5U3NCf6AEEttS-Jd-BI8y.psjwXTXHHgCsud0UVowzRu4Y100Ja8Te3ri7jrkzpYXcoiToyqbo3YJA9AYPmTcR35oiOzLw8WIfOFcwkLzPOg.sc3%26sdk%3DAndroid_7.0.11%26targetEnv%3Dmobile%26timestamp%3D1718648375%26ucid%3DWhVS3SvK1JFRArK-4KqpMw")

        val newSignature = signing.newSignature(spec)
        println("New generated signature: $newSignature")
        assertEquals(VALID_SIGNATURE_VALUE, newSignature)
    }
}
