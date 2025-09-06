package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface IPendingRegistrationViewModel {

    fun resolve(
        map: MutableMap<String, String>,
        regToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
    }
}

// Mock preview class for the PendingRegistrationViewModel
class PendingRegistrationViewModelPreview : IPendingRegistrationViewModel

class PendingRegistrationViewModel(
    context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context),
    IPendingRegistrationViewModel {

    /**
     * Resolve pending registration interruption with provided missing fields for registration.
     */
    override fun resolve(
        map: MutableMap<String, String>,
        regToken: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            val jsonMap = mutableMapOf<String, JsonPrimitive>()
            map.forEach { (key, value) ->
                // Removing "profile.+" prefix for value... This is dynamic and should not be taken
                // as a best practice form.
                jsonMap[key.substring(key.lastIndexOf(".") + 1)] = JsonPrimitive(value)
            }

            authenticationFlowDelegate.cdc.resolvePendingRegistration(
                missingFieldsSerialized = mutableMapOf("profile" to JsonObject(jsonMap).toString()),
                regToken = regToken,
                authCallbacks = authCallbacks
            )

        }
    }

}