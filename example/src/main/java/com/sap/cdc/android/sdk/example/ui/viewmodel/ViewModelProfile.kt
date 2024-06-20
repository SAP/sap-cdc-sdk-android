package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelProfile {

    fun firstName(): String = ""

    fun lastName(): String = ""

    fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()) {}
}


class ViewModelProfile(context: Context) : ViewModelBase(context), IViewModelProfile {

    init {
        getAccountInfo()
    }

    var lastName by mutableStateOf("")
        internal set

    var firstName by mutableStateOf("")
        internal set

    override fun firstName(): String = this.firstName

    override fun lastName(): String = this.lastName

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

class ViewModelProfilePreview() : IViewModelProfile