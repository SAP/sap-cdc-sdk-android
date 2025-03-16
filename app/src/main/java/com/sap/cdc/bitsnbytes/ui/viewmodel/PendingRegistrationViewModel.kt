package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface IPendingRegistrationViewModel {

    fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) { }
}

// Mock preview class for the PendingRegistrationViewModel
class PendingRegistrationViewModelPreview : IPendingRegistrationViewModel

class PendingRegistrationViewModel(context: Context) : BaseViewModel(context),
    IPendingRegistrationViewModel {

    /**
     * Resolve pending registration interruption with provided missing fields for registration.
     */
    override fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val jsonMap = mutableMapOf<String, JsonPrimitive>()
            map.forEach { (key, value) ->
                // Removing "profile.+" prefix for value... This is dynamic and should not be taken
                // as a best practice form.
                jsonMap[key.substring(key.lastIndexOf(".") + 1)] = JsonPrimitive(value)
            }
            val authResponse = identityService.resolvePendingRegistrationWithMissingFields(
                "profile", JsonObject(jsonMap).toString(), regToken,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

}