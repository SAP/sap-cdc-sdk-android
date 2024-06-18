package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */

abstract class ACredentialsRegistrationViewModel(context: Context) : ViewModelBase(context) {

    abstract fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    )
}

class CredentialsRegistrationViewModel(context: Context) :
    ACredentialsRegistrationViewModel(context) {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.register(email, password)
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                //TODO: Add error handling.
                onFailed(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }

}

class CredentialsRegistrationViewModelPreview(context: Context) :
    ACredentialsRegistrationViewModel(context) {

    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        // Stub.
    }

}