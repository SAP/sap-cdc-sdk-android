package com.sap.cdc.android.sdk.feature.screensets

class WebBridgeJSConfig private constructor(
    val obfuscate: Boolean = true
) {

    data class Builder(
        var obfuscate: Boolean = true
    ) {
        fun obfuscate(obfuscate: Boolean) = apply { this.obfuscate = obfuscate }

        fun build(): WebBridgeJSConfig = WebBridgeJSConfig(
            obfuscate
        )
    }
}



