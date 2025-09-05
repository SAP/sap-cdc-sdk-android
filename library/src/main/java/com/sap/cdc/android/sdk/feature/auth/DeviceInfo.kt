package com.sap.cdc.android.sdk.feature.auth

import android.os.Build
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val platform: String? = "android",
    val os: String? = Build.VERSION.RELEASE,
    val man: String? = Build.MANUFACTURER,
    var pushToken: String? = null
)

