package com.sap.cdc.android.sdk.session

import android.util.Base64
import com.sap.cdc.android.sdk.session.api.Signing
import com.sap.cdc.android.sdk.session.api.SigningSpec
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
class SigningUnitTest {

    init {
        mockStatic(Base64::class.java)
        `when`(Base64.encode(any(), anyInt())).thenAnswer { invocation ->
            java.util.Base64.getEncoder().encode(invocation.arguments[0] as ByteArray)
        }
        `when`(Base64.decode(anyString(), anyInt())).thenAnswer { invocation ->
            java.util.Base64.getMimeDecoder().decode(invocation.arguments[0] as String)
        }
        `when`(Base64.encodeToString(any(), anyInt())).thenAnswer { invocation ->
            String(java.util.Base64.getEncoder().encode(invocation.arguments[0] as ByteArray))
        }
    }

    private val signing = Signing()
    private val spec = SigningSpec(
        "op+xZdZ3jOMibrlrGoezpNNfsYY=",
        "https://accounts.us1.gigya.com/accounts.getAccountInfo",
        "POST",
        mutableMapOf(
            "apiKey" to "4_mL-YkAEegR9vzt6QvHWI5Q",
            "extraProfileFields" to "languages,phones,education",
            "format" to "json",
            "gmid" to "gmid.ver4.AcbH7A0KVA.Q6Ij-kPt3IEXbZ4LJ-n_ibZnCXJp1EPaW0wRmdKQhZkhUnXJ9aWxM3z3eowb9m_F.B-ezLKE6shBATyGNUSv87rc99fVcys4vdmRZCQA8aJJY8AzXJq0p4YYOCw902Ux11-lOsfuSdFwabq4cfZzcyA.sc3",
            "httpStatusCodes" to "false",
            "include" to "data,profile,emails,missing-required-fields",
            "nonce" to "1697483952341_797277454",
            "oauth_token" to "st2.s.AcbHLjJu-g.18Qif9fKPlYPLbBAJOzeVmv7RGJg3oUcK_mam_z2nzFccyWAGxSAiHpzTmO79USIPiZzzRRxe-V4YfVvgAzgjJFNt75aF_TVpAV0ITiShl0SMHKketS3RdCQK--byGLe.usxdcqO1T7KVKC5eIDvPab8lkGX8Gst96z9wx6M7WiWq2C7i2Ap5JCS0YAEmV-NMKJGY1WXv0MJDg0dZijk6DA.sc3",
            "sdk" to "Android_7.0.5",
            "targetEnv" to "mobile",
            "timestamp" to "1697483955",
            "ucid" to "McPEXONSfw-6K30E9zGrXg"
        )
    )

    companion object {
        const val VALID_SIGNATURE_VALUE = "X5YlxX4C3rnf-orJXUvMDQNjZpQ="
    }

    @Test
    fun newSignature_isCorrect() {
        val newSignature = signing.newSignature(spec)
        println("New generated signature: $newSignature")
        assertEquals(VALID_SIGNATURE_VALUE, newSignature)
    }
}