package com.sap.cdc.android.sdk.auth.provider

interface IPasskeysAuthenticationProvider {

    suspend fun createPasskey(requestJson: String): String?

    suspend fun getPasskey(requestJson: String): String?

}