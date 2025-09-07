package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailRegisterViewModel {

    fun register(
        credentials: Credentials,
        name: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub
    }
}

// Mock preview class for the EmailRegisterViewModel
class EmailRegisterViewModelPreview : IEmailRegisterViewModel

class EmailRegisterViewModel(
    context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailRegisterViewModel {

    /**
     * Register new account using credentials (email,password)
     * Additional profile fields are included to set profile.firstName & profile.lastName fields.
     */
    override fun register(
        credentials: Credentials,
        name: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            val namePair = name.splitFullName()
            val profileObject =
                json.encodeToJsonElement(
                    mutableMapOf(
                        "firstName" to namePair.first,
                        "lastName" to namePair.second
                    )
                )
            authenticationFlowDelegate.register(
                credentials,
                authCallbacks,
                mutableMapOf("profile" to profileObject.toString())
            )
        }
    }
}