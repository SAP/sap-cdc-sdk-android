package com.sap.cdc.bitsnbytes.extensions

import com.sap.cdc.android.sdk.feature.auth.flow.TwoFactorContext
import kotlinx.serialization.json.Json

fun TwoFactorContext.toJson(): String {
    return Json.encodeToString(this)
}