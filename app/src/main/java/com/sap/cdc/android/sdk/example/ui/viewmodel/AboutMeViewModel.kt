package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import com.sap.cdc.android.sdk.example.cdc.model.ProfileEntity
import com.sap.cdc.android.sdk.example.extensions.splitFullName
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

interface IAboutMeViewModel : IAccountViewModel {

    fun updateAccountInfoWith(name: String, success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }

}

/**
 * Preview mock view model.
 */
class AboutMeViewModelPreview : IAboutMeViewModel {

    override fun accountInfo(): AccountEntity = AccountEntity(
        uid = "1234",
        profile = ProfileEntity(firstName = "John", lastName = "Doe", email = "johndoe@gmail.com")
    )
}

class AboutMeViewModel(context: Context) : BaseViewModel(context), IAboutMeViewModel {

    /**
     * Update account information with new name.
     * Name parameter will be split to firstName & lastName to update profile fields.
     */
    override fun updateAccountInfoWith(
        name: String,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        val newName = name.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf("firstName" to newName.first, "lastName" to newName.second)
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
            val setAuthResponse = identityService.setAccountInfo(parameters)
            when (setAuthResponse.state()) {
                AuthState.SUCCESS -> {
                    getAccountInfo(success = success, onFailed = onFailed)
                }

                else -> onFailed(setAuthResponse.toDisplayError()!!)
            }
        }
    }

}