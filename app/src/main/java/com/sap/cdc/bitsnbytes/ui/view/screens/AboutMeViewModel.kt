package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

interface IAboutMeViewModel {

    val flowDelegate: AuthenticationFlowDelegate?
        get() = null

    fun setAccountInfo(
        newName: String,
        alias: String? = null,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }
}

class AboutMeViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context),
    IAboutMeViewModel {

    override fun setAccountInfo(
        newName: String,
        alias: String?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val newName = newName.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf("firstName" to newName.first, "lastName" to newName.second)
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())

        // Update alias (custom custom identifier) if provided
        if (alias != null) {
            val customIdentifierObject =
                json.encodeToJsonElement(
                    mutableMapOf("nationalId" to alias)
                )
            parameters["customIdentifiers"] = customIdentifierObject.toString()
        }
        viewModelScope.launch {
            flowDelegate.setAccountInfo(parameters = parameters, authCallbacks = authCallbacks)
        }
    }
}

// Mocked preview class for AboutMeViewModel
class AboutMeViewModelPreview : IAboutMeViewModel