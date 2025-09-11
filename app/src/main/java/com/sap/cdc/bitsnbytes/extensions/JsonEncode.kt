package com.sap.cdc.bitsnbytes.extensions

import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.OTPContext
import com.sap.cdc.android.sdk.feature.RegistrationContext
import com.sap.cdc.android.sdk.feature.TwoFactorContext
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

fun LinkingContext.toJson(): String {
    return Json.encodeToString(this)
}