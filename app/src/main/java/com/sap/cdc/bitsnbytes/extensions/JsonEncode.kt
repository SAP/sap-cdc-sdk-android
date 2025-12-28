package com.sap.cdc.bitsnbytes.extensions

import android.net.Uri
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.OTPContext
import com.sap.cdc.android.sdk.feature.RegistrationContext
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import kotlinx.serialization.json.Json

fun TwoFactorContext.toJson(): String {
    val jsonString = Json.encodeToString(this)
    return Uri.encode(jsonString)
}

fun RegistrationContext.toJson(): String {
    val jsonString = Json.encodeToString(this)
    return Uri.encode(jsonString)
}

fun OTPContext.toJson(): String {
    val jsonString = Json.encodeToString(this)
    return Uri.encode(jsonString)
}

fun LinkingContext.toJson(): String {
    val jsonString = Json.encodeToString(this)
    return Uri.encode(jsonString)
}
