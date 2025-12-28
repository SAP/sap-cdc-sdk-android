package com.sap.cdc.android.sdk.feature.provider.passkey

interface IPasskeysAuthenticationProvider {

    suspend fun createPasskey(requestJson: String): String?

    suspend fun getPasskey(requestJson: String): String?

}