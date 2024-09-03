package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelProfile {

    fun firstName(): String = ""

    fun lastName(): String = ""

    fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()) {}

    fun setAccountInfo(success: () -> Unit, onFailed: (CDCError) -> Unit) {}

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {}
}


class ViewModelProfile(context: Context) : ViewModelBase(context), IViewModelProfile {

    init {
        // Request account information on viewModel initialization.
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
            if (authResponse.toDisplayError() != null) {
                // Error in account request.
                //TODO: Add error handling.
                return@launch
            }
            // Deserialize account data.
            val account = json.decodeFromString<AccountEntity>(authResponse.asJsonString()!!)

            // Update UI stateful parameters.
            firstName = account.profile.firstName
            lastName = account.profile.lastName
        }
    }

    override fun setAccountInfo(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        val profileObject =
            json.encodeToJsonElement(mutableMapOf("firstName" to firstName, "lastName" to lastName))
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
            val authResponse = identityService.setAccountInfo(parameters)
            if (authResponse.toDisplayError() != null) {
                // Error in account request.
                onFailed(authResponse.toDisplayError()!!)
                return@launch
            }
            // Deserialize account data.
            val account = json.decodeFromString<AccountEntity>(authResponse.asJsonString()!!)

            // Update UI stateful parameters.
            firstName = account.profile.firstName
            lastName = account.profile.lastName

            success()
        }
    }

    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            if (authResponse.isError()) {
                // Error in account request.
                onFailed(authResponse.toCDCError())
                return@launch
            }
            success()
        }
    }

    fun splitName(name: String): Pair<String?, String?> {
        val names = name.trim().split(Regex("\\s+"))
        return names.firstOrNull() to names.lastOrNull()
    }

}

class ViewModelProfilePreview() : IViewModelProfile