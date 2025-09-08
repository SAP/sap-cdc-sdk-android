package com.sap.cdc.bitsnbytes.cdc

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.provider.IPasskeysAuthenticationProvider
import java.lang.ref.WeakReference

class PasskeysAuthenticationProvider(
    private val weakActivity: WeakReference<ComponentActivity>? = null
) : IPasskeysAuthenticationProvider {

    companion object {

        const val LOG_TAG = "PasskeysAuthenticationProvider"
    }

    private val credentialManager by lazy(LazyThreadSafetyMode.PUBLICATION) {
        weakActivity?.get()?.let {
            CredentialManager.create(it)
        }
    }

    @SuppressLint("PublicKeyCredential")
    override suspend fun createPasskey(requestJson: String): String? {
        if (weakActivity?.get() == null) {
            return null
        }
        try {
            val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
                // Contains the request in JSON format. Uses the standard WebAuthn
                // web JSON spec.
                requestJson = requestJson,
                // Defines whether you prefer to use only immediately available credentials,
                // not hybrid credentials, to fulfill this request. This value is false
                // by default.
                preferImmediatelyAvailableCredentials = false,
            )
            // Use the createCredential method to create the passkey.
            val result: CreateCredentialResponse? = credentialManager?.createCredential(
                request = createPublicKeyCredentialRequest,
                context = weakActivity.get()!!,
            )
            return if (result is CreatePublicKeyCredentialResponse) {
                result.registrationResponseJson
            } else {
                null
            }
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, e.message ?: "Error creating passkey")
            return null
        }
    }

    override suspend fun getPasskey(requestJson: String): String? {
        if (weakActivity?.get() == null) {
            return null
        }
        try {
            // Get passkeys from the user's public key credential provider.
            val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                requestJson = requestJson,
            )

            val getCredRequest = GetCredentialRequest(
                listOf(getPublicKeyCredentialOption)
            )

            val result: GetCredentialResponse? = credentialManager?.getCredential(
                // Use an activity-based context to avoid undefined system UI
                // launching behavior.
                context = weakActivity.get()!!,
                request = getCredRequest
            )

            val credential = result?.credential
            return if (credential is PublicKeyCredential) {
                credential.authenticationResponseJson
            } else {
                null
            }
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, e.message ?: "Error getting passkey")
            return null
        }
    }

}