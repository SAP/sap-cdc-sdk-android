package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.bitsnbytes.extensions.parseRequiredMissingFieldsForRegistration
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailRegistrationViewModel {

    // Form field states
    var name: String
    var email: String
    var password: String
    var confirmPassword: String
    var passwordVisible: Boolean
    var registerError: String
    var loading: Boolean

    fun register(
        credentials: Credentials,
        name: String,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub
    }
}

// Mock preview class for the EmailRegisterViewModel
class EmailRegistrationViewModelPreview : IEmailRegistrationViewModel {
    override var name: String by mutableStateOf("")
    override var email: String by mutableStateOf("")
    override var password: String by mutableStateOf("")
    override var confirmPassword: String by mutableStateOf("")
    override var passwordVisible: Boolean by mutableStateOf(false)
    override var registerError: String by mutableStateOf("")
    override var loading: Boolean by mutableStateOf(false)
}

class EmailRegistrationViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailRegistrationViewModel {

    // Form field states - these will persist across navigation
    override var name: String by mutableStateOf("")
    override var email: String by mutableStateOf("")
    override var password: String by mutableStateOf("")
    override var confirmPassword: String by mutableStateOf("")
    override var passwordVisible: Boolean by mutableStateOf(false)
    override var registerError: String by mutableStateOf("")
    override var loading: Boolean by mutableStateOf(false)

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
                mutableMapOf("profile" to profileObject.toString()),
            )
            {
                doOnPendingRegistrationAndOverride { registrationContext ->
                    val parsedMissingRequiredFieldsFromErrorDetails =
                        registrationContext.originatingError?.details?.parseRequiredMissingFieldsForRegistration()
                    registrationContext.copy(missingRequiredFields = parsedMissingRequiredFieldsFromErrorDetails)
                }

                authCallbacks()

            }
        }
    }
}
