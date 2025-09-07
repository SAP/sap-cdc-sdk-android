package com.sap.cdc.bitsnbytes.extensions

import com.sap.cdc.android.sdk.feature.auth.flow.OTPContext
import com.sap.cdc.android.sdk.feature.auth.flow.RegistrationContext
import com.sap.cdc.android.sdk.feature.auth.flow.TwoFactorContext
import kotlinx.serialization.json.Json

fun TwoFactorContext.toJson(): String {
    return Json.encodeToString(this)
}

fun RegistrationContext.toJson(): String {
    return Json.encodeToString(this)
}

fun OTPContext.toJson(): String {
    return Json.encodeToString(this)
}