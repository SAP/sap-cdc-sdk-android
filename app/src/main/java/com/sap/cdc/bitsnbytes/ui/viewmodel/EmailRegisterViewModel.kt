package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailRegisterViewModel {
    fun register(
        email: String,
        password: String,
        name: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }
}

// Mock preview class for the EmailRegisterViewModel
class EmailRegisterViewModelPreview:  IEmailRegisterViewModel

class EmailRegisterViewModel(context: Context) : BaseViewModel(context), IEmailRegisterViewModel {

    /**
     * Register new account using credentials (email,password)
     * Additional profile fields are included to set profile.firstName & profile.lastName fields.
     */
    override fun register(
        email: String,
        password: String,
        name: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
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
            val authResponse = identityService.register(email, password, profileObject.toString())
            // Check response state for flow success/error/continuation.
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