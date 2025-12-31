package com.sap.cdc.android.sdk.feature.logout

import android.webkit.CookieManager
import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_LOGOUT
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService

class AuthLogoutFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthLogoutFlow"
    }

    /**
     * Initiate logout flow.
     *
     * @see [accounts.logout](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41376ba570b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    suspend fun logout(callbacks: AuthCallbacks) {
        CIAMDebuggable.log(LOG_TAG, "logout")
        val response =
            AuthenticationApi(coreClient, sessionService).send(EP_ACCOUNTS_LOGOUT)

        // Success case
        if (!response.isError() || response.errorCode() == 403005 || response.errorCode() == 403007) {
            CIAMDebuggable.log(LOG_TAG, "logout: success")
            // Invalidate session if the response does not contain any errors.
            // If an "Unauthorized user" (403005, 403007) error is received, the session is already invalidated in the backend or
            // permission was denied to invalidate it from server side. In both cases, we need to clear the local session as well.
            sessionService.invalidateSession()
            clearCookies()

            val authSuccess = createAuthSuccess(response)
            callbacks.onSuccess?.invoke(authSuccess)
        } else {
            // Error case
            val authError = createAuthError(response)
            callbacks.onError?.invoke(authError)
        }
    }

    private fun clearCookies() {
        CIAMDebuggable.log(LOG_TAG, "clearCookies")
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()
    }
}