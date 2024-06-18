package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
abstract class AMyProfileViewModel(context: Context) : ViewModelBase(context) {

    var lastName by mutableStateOf("")
        internal set

    var firstName by mutableStateOf("")
        internal set

    abstract fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf())
}


class MyProfileViewModel(context: Context) : AMyProfileViewModel(context) {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

    init {
        getAccountInfo()
    }

    override fun getAccountInfo(parameters: MutableMap<String, String>?) {
        viewModelScope.launch {
            val authResponse = identityService.getAccountInfo()
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                //TODO: Add error handling.
                return@launch
            }
            // Deserialize account data.
            val account = json.decodeFromString<AccountEntity>(authResponse.authenticationJson()!!)

            // Update UI stateful parameters.
            firstName = account.profile.firstName
            lastName = account.profile.lastName
        }
    }

}

class MyProfileViewModelPreview(context: Context) : AMyProfileViewModel(context) {

    init {
        firstName = "John"
        lastName = "Doe"
    }

    override fun getAccountInfo(parameters: MutableMap<String, String>?) {
        // Stub.
    }

}