package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailRegistrationViewModel {

    fun register(
        credentials: Credentials,
        name: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub
    }
}

// Mock preview class for the EmailRegisterViewModel
class EmailRegistrationViewModelPreview : IEmailRegistrationViewModel

class EmailRegistrationViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailRegistrationViewModel {

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
            flowDelegate.register(
                credentials,
                authCallbacks,
                mutableMapOf("profile" to profileObject.toString())
            )
        }
    }
}